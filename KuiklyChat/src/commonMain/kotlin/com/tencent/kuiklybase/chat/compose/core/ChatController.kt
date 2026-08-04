package com.tencent.kuiklybase.chat.compose.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Single source of truth for the new Compose Chat UI SDK.
 * It owns message operations but does not render UI or know AG-UI protocol types.
 */
class ChatController(
    val currentUser: ChatUser,
    private val scope: CoroutineScope,
    private val sender: ChatMessageSender? = null,
    private val historyLoader: ChatHistoryLoader? = null,
    initialMessages: List<ChatMessage> = emptyList()
) {
    var state by mutableStateOf(
        ChatState(
            messages = initialMessages,
            hasMoreEarlier = historyLoader != null,
            lastMessageId = initialMessages.lastOrNull()?.id
        )
    )
        private set

    var effect by mutableStateOf<ChatEffect?>(null)
        private set

    private var requestJob: Job? = null
    private var activeAssistantId: String? = null
    private var pendingUserMessageId: String? = null
    private var lastUserPrompt: String? = null
    private var isListNearBottom = true
    private var localId = 0

    init {
        if (historyLoader != null && initialMessages.isEmpty()) {
            scope.launch { loadInitial() }
        }
    }

    fun dispatch(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.SendText -> sendText(intent.text)
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

    /** Called by the message list when sticky-bottom state changes. */
    fun setListNearBottom(nearBottom: Boolean) {
        isListNearBottom = nearBottom
        if (nearBottom && state.unreadCount > 0) {
            state = state.copy(unreadCount = 0)
        }
    }

    fun clearUnread() {
        if (state.unreadCount > 0) {
            state = state.copy(unreadCount = 0)
        }
    }

    /** Adds a local message and starts the injected sender when one is available. */
    fun sendText(rawText: String): Boolean {
        val text = rawText.trim()
        if (text.isEmpty() || state.isSending) return false

        val message = ChatMessage(
            id = nextLocalId("user"),
            author = currentUser,
            parts = listOf(MessagePart.Text(text)),
            status = MessageStatus.SENDING
        )
        lastUserPrompt = text
        pendingUserMessageId = message.id
        state = state.copy(
            messages = state.messages + message,
            isSending = sender != null,
            error = null,
            lastMessageId = message.id
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

    fun stopGeneration() {
        requestJob?.cancel()
        requestJob = null
        state = state.copy(isSending = false)
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
    }

    fun retryMessage(messageId: String) {
        if (state.isSending) return
        val failed = state.messages.firstOrNull { it.id == messageId } ?: return
        if (failed.status != MessageStatus.FAILED) return
        val retryText = lastUserPrompt ?: failed.text
        state = state.copy(messages = state.messages.filterNot { it.id == messageId }, error = null)
        if (failed.author.id != currentUser.id) {
            pendingUserMessageId?.let { userId ->
                state = state.copy(messages = state.messages.filterNot { it.id == userId })
            }
        }
        sendText(retryText)
    }

    fun refresh() {
        val loader = historyLoader ?: return
        if (state.isInitialLoading) return
        scope.launch { loadInitial(force = true) }
    }

    fun loadEarlier() {
        val loader = historyLoader ?: return
        if (state.isLoadingEarlier || !state.hasMoreEarlier) return
        val beforeId = state.messages.firstOrNull()?.id
        state = state.copy(isLoadingEarlier = true, error = null)
        scope.launch {
            try {
                val page = loader.loadEarlier(beforeId)
                val existingIds = state.messages.mapTo(HashSet()) { it.id }
                val newMessages = page.messages.filterNot { it.id in existingIds }
                state = state.copy(
                    messages = newMessages + state.messages,
                    hasMoreEarlier = page.hasMoreEarlier,
                    isLoadingEarlier = false
                )
            } catch (cancelled: CancellationException) {
                state = state.copy(isLoadingEarlier = false)
                throw cancelled
            } catch (error: Throwable) {
                fail(error.message ?: "Failed to load earlier messages", "history")
            }
        }
    }

    fun deleteMessage(messageId: String) {
        if (state.messages.none { it.id == messageId }) return
        state = state.copy(
            messages = state.messages.filterNot { it.id == messageId },
            lastMessageId = state.messages.lastOrNull { it.id != messageId }?.id
        )
    }

    fun clearError() {
        state = state.copy(error = null)
    }

    /** Applies an external protocol/session message without exposing protocol types to UI. */
    fun upsertExternalMessage(message: ChatMessage) {
        updateMessage(message)
        state = state.copy(
            isSending = message.status == MessageStatus.STREAMING || state.isSending,
            error = if (message.status == MessageStatus.FAILED) {
                state.error ?: ChatError("Message failed")
            } else {
                state.error
            }
        )
    }

    /** Replaces the visible snapshot after an external session emits a messages snapshot. */
    fun replaceExternalMessages(messages: List<ChatMessage>, hasMoreEarlier: Boolean? = null) {
        state = state.copy(
            messages = messages,
            hasMoreEarlier = hasMoreEarlier ?: state.hasMoreEarlier,
            lastMessageId = messages.lastOrNull()?.id,
            isSending = messages.any { it.status == MessageStatus.STREAMING }
        )
    }

    fun consumeEffect() {
        effect = null
    }

    fun dispose() {
        requestJob?.cancel()
        requestJob = null
        activeAssistantId = null
        pendingUserMessageId = null
    }

    private suspend fun loadInitial(force: Boolean = false) {
        val loader = historyLoader ?: return
        if (state.isInitialLoading) return
        if (!force && state.messages.isNotEmpty()) return
        state = state.copy(isInitialLoading = true, error = null)
        try {
            val page = loader.loadEarlier(beforeMessageId = null)
            state = state.copy(
                messages = page.messages,
                hasMoreEarlier = page.hasMoreEarlier,
                isInitialLoading = false,
                lastMessageId = page.messages.lastOrNull()?.id
            )
            emit(ChatEffect.ScrollToBottom)
        } catch (cancelled: CancellationException) {
            state = state.copy(isInitialLoading = false)
            throw cancelled
        } catch (error: Throwable) {
            state = state.copy(isInitialLoading = false)
            fail(error.message ?: "Failed to load messages", "history")
        }
    }

    private fun startRequest(userMessage: ChatMessage) {
        val messageSender = sender ?: return
        activeAssistantId = null
        requestJob = scope.launch {
            try {
                messageSender.send(userMessage).collect { event ->
                    when (event) {
                        is ChatStreamEvent.TextDelta -> applyTextDelta(event)
                        is ChatStreamEvent.ReplaceMessage -> updateMessage(event.message)
                        ChatStreamEvent.Completed -> completeLatestAssistant()
                        is ChatStreamEvent.Failed -> fail(event.error.message, event.error.code, event.error)
                    }
                }
                if (state.isSending) completeLatestAssistant()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                fail(error.message ?: "Failed to send message", "send")
            } finally {
                if (requestJob === kotlinx.coroutines.currentCoroutineContext()[Job]) {
                    requestJob = null
                    state = state.copy(isSending = false)
                }
            }
        }
    }

    private fun applyTextDelta(event: ChatStreamEvent.TextDelta) {
        val assistant = event.messageId?.let { id -> state.messages.firstOrNull { it.id == id } }
            ?: activeAssistantId?.let { id -> state.messages.firstOrNull { it.id == id } }
            ?: ChatMessage(
                id = event.messageId ?: nextLocalId("assistant"),
                author = ChatUser("assistant", "Assistant"),
                parts = emptyList(),
                status = MessageStatus.STREAMING
            )
        activeAssistantId = assistant.id
        val updated = assistant.copy(
            parts = listOf(MessagePart.Text(assistant.text + event.text)),
            status = MessageStatus.STREAMING
        )
        val isNewAssistant = state.messages.none { it.id == assistant.id }
        if (isNewAssistant) {
            state = state.copy(messages = state.messages + updated, lastMessageId = updated.id)
            maybeIncrementUnread()
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
        state = state.copy(isSending = false)
        emit(ChatEffect.ScrollToBottom)
    }

    private fun updateMessage(message: ChatMessage) {
        val index = state.messages.indexOfFirst { it.id == message.id }
        if (index < 0) {
            state = state.copy(messages = state.messages + message, lastMessageId = message.id)
            return
        }
        val messages = state.messages.toMutableList()
        messages[index] = message
        state = state.copy(messages = messages, lastMessageId = message.id)
    }

    private fun fail(message: String, code: String, error: ChatError = ChatError(message, code)) {
        val failedMessage = activeAssistantId?.let { assistantId ->
            state.messages.firstOrNull { it.id == assistantId }
        } ?: state.messages.lastOrNull { it.author.id == currentUser.id && it.status == MessageStatus.SENDING }
        failedMessage?.let { updateMessage(it.copy(status = MessageStatus.FAILED)) }
        activeAssistantId = null
        state = state.copy(isSending = false, error = error)
        emit(ChatEffect.ShowError(message))
    }

    private fun maybeIncrementUnread() {
        if (!isListNearBottom) {
            state = state.copy(unreadCount = state.unreadCount + 1)
        }
    }

    private fun emit(newEffect: ChatEffect) {
        effect = newEffect
    }

    private fun nextLocalId(prefix: String): String = "$prefix-${localId++}"
}
