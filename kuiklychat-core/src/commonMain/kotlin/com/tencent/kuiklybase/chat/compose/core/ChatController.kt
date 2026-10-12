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
    private val sender: ChatMessageSender? = null,
    internal val historyLoader: ChatHistoryLoader? = null,
    initialMessages: List<ChatMessage> = emptyList(),
    private val assistantUser: ChatUser = ChatUser("assistant", "Assistant"),
    private val clock: ChatClock = ChatClock.System
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
    internal var lastUserPrompt: String? = null
    internal var lastUserSubmission: List<MessagePart>? = null
    private var isListNearBottom = true
    private var localId = 0L
    internal var historyEpoch = 0L
    private var internalRequestActive = false
    private val externalStreamingMessageIds = mutableSetOf<String>()
    internal var closed = false
    private val effectEmitter = ChatEffectEmitter()
    public val effects: Flow<ChatEffect> = effectEmitter.effects
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
        if ((text.isEmpty() && attachments.isEmpty()) || state.isSending) return false
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
        pendingUserMessageId = message.id
        _state.value = state.copy(
            messages = state.messages + message,
            isSending = sender != null,
            error = null,
            lastMessageId = message.id,
        )
        maybeIncrementUnread()
        if (sender == null) {
            updateMessage(message.copy(status = MessageStatus.SENT))
            emit(ChatEffect.ScrollToBottom)
        } else {
            startRequest(message)
        }
        return true
    }

    public fun stopGeneration() {
        if (closed) return
        requestToken += 1
        requestJob?.cancel()
        requestJob = null
        internalRequestActive = false
        _state.value = state.copy(isSending = false)
        activeAssistantId?.let { assistantId ->
            state.messages.firstOrNull { it.id == assistantId }?.let {
                updateMessage(it.copy(status = MessageStatus.CANCELLED))
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
    }

    public fun retryMessage(messageId: String) {
        if (state.isSending) return
        val failed = state.messages.firstOrNull { it.id == messageId } ?: return
        if (failed.status != MessageStatus.FAILED) return
        val retryParts = resolveRetryParts(failed) ?: return
        val retryText = retryParts.filterIsInstance<MessagePart.Text>().joinToString("") { it.value }
        val retryAttachments = retryParts.filterIsInstance<MessagePart.Attachment>()
        _state.value = state.copy(messages = state.messages.filterNot { it.id == messageId }, error = null)
        if (failed.author.id != currentUser.id) {
            pendingUserMessageId?.let { userId ->
                _state.value = state.copy(messages = state.messages.filterNot { it.id == userId })
            }
        }
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
        activeAssistantId = null
        pendingUserMessageId = null
        _state.value = state.copy(
            messages = messages,
            isSending = false,
            error = null,
            lastMessageId = messages.lastOrNull()?.id,
        )
    }

    internal fun startRequest(userMessage: ChatMessage) {
        val messageSender = sender ?: return
        val token = ++requestToken
        activeAssistantId = null
        internalRequestActive = true
        val job = controllerScope.launch(start = CoroutineStart.LAZY) {
            try {
                messageSender.send(userMessage).collect { event ->
                    if (token != requestToken) return@collect
                    when (event) {
                        is ChatStreamEvent.TextDelta -> applyTextDelta(event)
                        is ChatStreamEvent.ReplaceMessage -> updateMessage(event.message)
                        ChatStreamEvent.Completed -> completeLatestAssistant()
                        is ChatStreamEvent.Failed -> fail(event.error.message, event.error.code, event.error)
                    }
                }
                if (token == requestToken && state.isSending) completeLatestAssistant()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (token == requestToken && !closed) {
                    fail(error.message ?: "Failed to send message", "send")
                }
            } finally {
                if (requestJob === currentCoroutineContext()[Job]) {
                    requestJob = null
                    internalRequestActive = false
                    updateSendingState()
                }
            }
        }
        requestJob = job
        job.start()
    }

    private fun applyTextDelta(event: ChatStreamEvent.TextDelta) {
        val assistant = event.messageId?.let { id -> state.messages.firstOrNull { it.id == id } }
            ?: activeAssistantId?.let { id -> state.messages.firstOrNull { it.id == id } }
            ?: ChatMessage(
                id = event.messageId ?: nextLocalId("assistant"),
                author = assistantUser,
                parts = emptyList(),
                status = MessageStatus.STREAMING,
                createdAt = clock.nowMillis()
            )
        activeAssistantId = assistant.id
        val textFormat =
            assistant.parts.filterIsInstance<MessagePart.Text>().firstOrNull()?.format
                ?: event.format
        val updated = assistant.copy(
            parts = appendTextDelta(assistant.parts, event.text, textFormat),
            status = MessageStatus.STREAMING
        )
        val isNewAssistant = state.messages.none { it.id == assistant.id }
        if (isNewAssistant) {
            _state.value = state.copy(messages = state.messages + updated, lastMessageId = updated.id)
            maybeIncrementUnread(updated)
        } else {
            updateMessage(updated)
        }
        emit(ChatEffect.ScrollToBottom)
    }

    private fun completeLatestAssistant() {
        activeAssistantId?.let { assistantId ->
            state.messages.firstOrNull { it.id == assistantId }?.let {
                updateMessage(it.copy(status = MessageStatus.SENT))
            }
        }
        pendingUserMessageId?.let { userId ->
            state.messages.firstOrNull { it.id == userId }?.let { userMessage ->
                if (userMessage.status == MessageStatus.SENDING) {
                    updateMessage(userMessage.copy(status = MessageStatus.SENT))
                }
            }
        }
        activeAssistantId = null
        pendingUserMessageId = null
        internalRequestActive = false
        updateSendingState()
        emit(ChatEffect.ScrollToBottom)
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

    private fun fail(message: String, code: String, error: ChatError = ChatError(message, code)) {
        val failedMessage = activeAssistantId?.let { assistantId ->
            state.messages.firstOrNull { it.id == assistantId }
        } ?: state.messages.lastOrNull { it.author.id == currentUser.id && it.status == MessageStatus.SENDING }
        failedMessage?.let { updateMessage(it.copy(status = MessageStatus.FAILED)) }
        activeAssistantId = null
        pendingUserMessageId = null
        internalRequestActive = false
        _state.value = state.copy(
            isSending = externalStreamingMessageIds.isNotEmpty(),
            error = error,
        )
        emit(ChatEffect.ShowError(message))
    }

    private fun updateSendingState() {
        _state.value = state.copy(
            isSending = internalRequestActive || externalStreamingMessageIds.isNotEmpty()
        )
    }

    private fun maybeIncrementUnread(message: ChatMessage? = null) {
        if (!isListNearBottom) {
            if (message != null && !message.countsAsUnread()) return
            _state.value = state.copy(unreadCount = state.unreadCount + 1)
        }
    }

    internal fun emit(newEffect: ChatEffect) {
        effectEmitter.emit(newEffect)
    }

    private fun nextLocalId(prefix: String): String {
        var candidate: String
        do {
            candidate = "$prefix-${localId++}"
        } while (state.messages.any { it.id == candidate })
        return candidate
    }

    private fun resolveRetryParts(failed: ChatMessage): List<MessagePart>? {
        if (failed.author.id == currentUser.id) return failed.parts
        pendingUserMessageId
            ?.let { userId -> state.messages.firstOrNull { it.id == userId }?.parts }
            ?.let { return it }
        return lastUserSubmission
    }

}
