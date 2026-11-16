package com.tencent.kuiklybase.chat.compose.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
/** Message state for one visible conversation. UI and protocol types stay outside. */
public class ChatController(
    public val currentUser: ChatUser,
    private val scope: CoroutineScope,
    internal val sender: ChatMessageSender? = null,
    internal val historyLoader: ChatHistoryLoader? = null,
    initialMessages: List<ChatMessage> = emptyList(),
    internal val assistantUser: ChatUser = ChatUser("assistant", "Assistant"),
    internal val clock: ChatClock = ChatClock.System,
    internal val logger: ChatLogger? = null
) {
    private val controllerJob = SupervisorJob()
    private val parentCompletionHandle = scope.coroutineContext[Job]?.invokeOnCompletion { controllerJob.cancel() }
    internal val controllerScope = CoroutineScope(scope.coroutineContext + controllerJob)
    internal val _state = MutableStateFlow(
        ChatState(
            messages = initialMessages,
            hasMoreEarlier = historyLoader != null,
            lastMessageId = initialMessages.lastOrNull()?.id
        )
    )
    public val stateFlow: StateFlow<ChatState> = _state.asStateFlow()
    public val state: ChatState get() = _state.value

    internal var requestJob: Job? = null
    internal var requestToken: Int = 0
    internal var historyJob: Job? = null
    internal var activeAssistantId: String? = null
    internal var pendingUserMessageId: String? = null
    internal val sendQueue = ArrayDeque<ChatMessage>()
    internal var lastUserPrompt: String? = null
    internal var lastUserSubmission: List<MessagePart>? = null
    internal var historyEpoch = 0L
    internal var internalRequestActive = false
    private val externalStreamingMessageIds = mutableSetOf<String>()
    internal val externalStreamingMessageIdSet = externalStreamingMessageIds
    internal var closed = false
    internal var isListNearBottom = true
    internal var localId = 0L
    private val effectEmitter = ChatEffectEmitter()
    public val effects: Flow<ChatEffect> = effectEmitter.effects

    internal fun log(level: ChatLogLevel, event: String, attributes: Map<String, String> = emptyMap(), throwable: Throwable? = null) {
        logger?.log(level, event, attributes, throwable)
    }

    init {
        if (historyLoader != null && initialMessages.isEmpty()) {
            launchHistory { loadInitial(historyEpoch) }
        }
    }

    public fun dispatch(intent: ChatIntent) {
        if (closed) return
        when (intent) {
            is ChatIntent.SendText -> sendText(intent.text)
            is ChatIntent.SendMessage -> sendMessage(intent.text, intent.attachments)
            ChatIntent.StopGeneration -> stopGeneration()
            is ChatIntent.RetryMessage -> retryMessage(intent.messageId)
            ChatIntent.LoadEarlier -> loadEarlier()
            ChatIntent.Refresh -> refresh()
            ChatIntent.ClearError -> clearError()
            is ChatIntent.DeleteMessage -> deleteMessage(intent.messageId)
            is ChatIntent.UpsertMessage -> upsertExternalMessage(intent.message)
            is ChatIntent.ReplaceMessages -> replaceExternalMessages(intent.messages, intent.hasMoreEarlier)
            is ChatIntent.SetListNearBottom -> setListNearBottom(intent.nearBottom)
            ChatIntent.ClearUnread -> clearUnread()
            is ChatIntent.SetConnectionState -> setConnectionState(intent.state)
            is ChatIntent.SetDraft -> setDraft(intent.text)
            is ChatIntent.SetDraftAttachments -> setDraftAttachments(intent.attachments)
            ChatIntent.ClearDraft -> clearDraft()
        }
    }

    public fun refresh(): Unit = refreshInternal()
    public fun loadEarlier(): Unit = loadEarlierInternal()
    /** Called by the message list when sticky-bottom state changes. */
    public fun setListNearBottom(nearBottom: Boolean) {
        isListNearBottom = nearBottom
        if (nearBottom && state.unreadCount > 0) {
            _state.value = state.copy(unreadCount = 0)
        }
    }

    public fun clearUnread() {
        if (state.unreadCount > 0) {
            _state.value = state.copy(unreadCount = 0)
        }
    }
    /** Adds a local message and starts the injected sender when one is available. */
    public fun sendText(rawText: String): Boolean = sendMessage(rawText, emptyList())

    public fun sendMessage(
        rawText: String,
        attachments: List<MessagePart.Attachment> = emptyList(),
    ): Boolean {
        if (closed) return false
        val text = rawText.trim()
        if (text.isEmpty() && attachments.isEmpty()) return false
        if (attachments.isNotEmpty() && !validateOutgoingAttachments(attachments)) return false

        val parts = buildList {
            if (text.isNotEmpty()) add(MessagePart.Text(text))
            addAll(attachments)
        }
        val message = ChatMessage(
            id = nextLocalId("user"),
            author = currentUser,
            parts = parts,
            status = MessageStatus.SENDING,
            createdAt = clock.nowMillis(),
            outgoing = true,
        )
        lastUserPrompt = text
        lastUserSubmission = parts
        val isQueued = sender != null &&
            (state.isSending || state.connectionState == ChatConnectionState.DISCONNECTED)
        if (isQueued) sendQueue.addLast(message)
        _state.value = state.copy(
            messages = state.messages + message,
            queuedMessages = if (isQueued) state.queuedMessages + message else state.queuedMessages,
            isSending = sender != null,
            error = null,
            lastMessageId = message.id,
            draftText = "",
            draftAttachments = emptyList(),
        )
        if (!isQueued) pendingUserMessageId = message.id
        maybeIncrementUnread()
        when {
            sender == null -> {
                updateMessage(message.copy(status = MessageStatus.SENT))
                emit(ChatEffect.ScrollToBottom)
            }
            isQueued -> startNextQueuedIfNeeded()
            else -> startRequest(message)
        }
        return true
    }

    public fun setConnectionState(next: ChatConnectionState) {
        if (closed || next == state.connectionState) return
        _state.value = state.copy(connectionState = next)
        if (next == ChatConnectionState.CONNECTED) startNextQueuedIfNeeded()
    }

    public fun setDraft(text: String) {
        if (closed || state.draftText == text) return
        _state.value = state.copy(draftText = text)
    }

    public fun setDraftAttachments(attachments: List<MessagePart.Attachment>) {
        if (closed || state.draftAttachments == attachments) return
        _state.value = state.copy(draftAttachments = attachments)
    }

    public fun clearDraft() {
        if (closed) return
        if (state.draftText.isEmpty() && state.draftAttachments.isEmpty()) return
        _state.value = state.copy(draftText = "", draftAttachments = emptyList())
    }

    internal fun startNextQueuedIfNeeded() {
        if (closed || sender == null || internalRequestActive) return
        if (state.connectionState != ChatConnectionState.CONNECTED) return
        val next = sendQueue.firstOrNull() ?: return
        sendQueue.removeFirst()
        _state.value = state.copy(queuedMessages = state.queuedMessages.filterNot { it.id == next.id })
        lastUserPrompt = next.text
        lastUserSubmission = next.parts
        pendingUserMessageId = next.id
        startRequest(next)
    }
    public fun stopGeneration() {
        if (closed) return
        requestToken += 1
        requestJob?.cancel()
        requestJob = null
        internalRequestActive = false
        _state.value = state.copy(isSending = false)
        val partialAssistant = activeAssistantId?.let { assistantId ->
            state.messages.firstOrNull { it.id == assistantId }
        }
        if (partialAssistant != null && partialAssistant.text.isNotEmpty()) {
            updateMessage(partialAssistant.copy(status = MessageStatus.STREAMING_PARTIAL))
            _state.value = state.copy(
                partialMessageIds = (state.partialMessageIds + partialAssistant.id).distinct(),
            )
        }
        activeAssistantId?.let { assistantId ->
            state.messages.firstOrNull { it.id == assistantId }?.let {
                if (it.text.isEmpty()) updateMessage(it.copy(status = MessageStatus.CANCELLED))
            }
        }
        pendingUserMessageId?.let { userId ->
            state.messages.firstOrNull { it.id == userId }?.let { userMessage ->
                if (userMessage.status == MessageStatus.SENDING) {
                    updateMessage(userMessage.copy(status = MessageStatus.CANCELLED))
                }
            }
        }
        activeAssistantId = null
        pendingUserMessageId = null
        updateSendingState()
        startNextQueuedIfNeeded()
    }
    public fun retryMessage(messageId: String) {
        if (state.isSending) return
        val failed = state.messages.firstOrNull { it.id == messageId } ?: return
        if (failed.status != MessageStatus.FAILED && failed.status != MessageStatus.STREAMING_PARTIAL) return
        val retryParts = resolveRetryParts(failed) ?: return
        val retryText = retryParts.filterIsInstance<MessagePart.Text>().joinToString("") { it.value }
        val retryAttachments = retryParts.filterIsInstance<MessagePart.Attachment>()
        _state.value = state.copy(messages = state.messages.filterNot { it.id == messageId }, error = null)
        sendQueue.removeAll { queued -> queued.author.id == currentUser.id && queued.parts == retryParts }
        _state.value = state.copy(
            messages = state.messages.filterNot { message ->
                message.author.id == currentUser.id &&
                    message.status == MessageStatus.SENDING &&
                    message.parts == retryParts &&
                    message.id != messageId
            },
            queuedMessages = state.queuedMessages.filterNot { queued ->
                queued.author.id == currentUser.id && queued.parts == retryParts
            },
        )
        if (failed.author.id != currentUser.id) {
            pendingUserMessageId?.let { userId ->
                _state.value = state.copy(messages = state.messages.filterNot { it.id == userId })
            }
        }
        _state.value = state.copy(partialMessageIds = state.partialMessageIds - messageId)
        _state.value = state.copy(connectionState = ChatConnectionState.CONNECTED)
        sendMessage(retryText, retryAttachments)
    }
    public fun deleteMessage(messageId: String) {
        if (state.messages.none { it.id == messageId }) return
        _state.value = state.copy(
            messages = state.messages.filterNot { it.id == messageId },
            lastMessageId = state.messages.lastOrNull { it.id != messageId }?.id
        )
    }

    public fun clearError() {
        _state.value = state.copy(error = null)
    }
    /** Applies an external protocol/session message without exposing protocol types to UI. */
    public fun upsertExternalMessage(message: ChatMessage) {
        if (closed) return
        if (message.status == MessageStatus.STREAMING) {
            externalStreamingMessageIds.add(message.id)
        } else {
            externalStreamingMessageIds.remove(message.id)
        }
        updateMessage(message)
        _state.value = state.copy(
            isSending = internalRequestActive || externalStreamingMessageIds.isNotEmpty(),
            error = if (message.status == MessageStatus.FAILED) {
                state.error ?: ChatError("Message failed")
            } else {
                state.error
            }
        )
    }

    /** Replaces the visible snapshot after an external session emits a messages snapshot. */
    public fun replaceExternalMessages(messages: List<ChatMessage>, hasMoreEarlier: Boolean? = null) {
        if (closed) return
        externalStreamingMessageIds.clear()
        messages.filter { it.status == MessageStatus.STREAMING }
            .mapTo(externalStreamingMessageIds) { it.id }
        _state.value = state.copy(
            messages = messages,
            hasMoreEarlier = hasMoreEarlier ?: state.hasMoreEarlier,
            lastMessageId = messages.lastOrNull()?.id,
            isSending = internalRequestActive || externalStreamingMessageIds.isNotEmpty()
        )
    }

    public fun dispose() {
        if (closed) return
        closed = true
        requestToken += 1
        historyEpoch += 1
        requestJob?.cancel()
        historyJob?.cancel()
        controllerJob.cancel()
        requestJob = null
        historyJob = null
        internalRequestActive = false
        externalStreamingMessageIds.clear()
        sendQueue.clear()
        activeAssistantId = null
        pendingUserMessageId = null
        _state.value = state.copy(isSending = false, isInitialLoading = false, isLoadingEarlier = false)
    }

    internal fun showConversationSnapshot(messages: List<ChatMessage>) {
        if (closed) return
        requestToken += 1
        requestJob?.cancel()
        requestJob = null
        cancelHistoryOperation()
        internalRequestActive = false
        externalStreamingMessageIds.clear()
        sendQueue.clear()
        activeAssistantId = null
        pendingUserMessageId = null
        _state.value = state.copy(
            messages = messages,
            isSending = false,
            error = null,
            lastMessageId = messages.lastOrNull()?.id,
            queuedMessages = emptyList(),
        )
    }

    internal fun updateMessage(message: ChatMessage) {
        val index = state.messages.indexOfFirst { it.id == message.id }
        if (index < 0) {
            val messages = state.messages + message
            _state.value = state.copy(messages = messages, lastMessageId = messages.lastOrNull()?.id)
            return
        }
        val messages = state.messages.toMutableList()
        messages[index] = message
        _state.value = state.copy(messages = messages, lastMessageId = messages.lastOrNull()?.id)
    }

    internal fun emit(newEffect: ChatEffect) {
        effectEmitter.emit(newEffect)
    }


    private fun resolveRetryParts(failed: ChatMessage): List<MessagePart>? {
        if (failed.author.id == currentUser.id) return failed.parts
        pendingUserMessageId
            ?.let { userId -> state.messages.firstOrNull { it.id == userId }?.parts }
            ?.let { return it }
        return lastUserSubmission
    }

}
