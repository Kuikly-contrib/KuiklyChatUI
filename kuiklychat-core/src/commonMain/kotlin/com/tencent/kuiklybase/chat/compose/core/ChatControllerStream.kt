package com.tencent.kuiklybase.chat.compose.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch

/** Request and stream handling for ChatController. */
internal fun ChatController.startRequest(userMessage: ChatMessage) {
    val messageSender = sender ?: return
    val token = ++requestToken
    activeAssistantId = null
    internalRequestActive = true
    var sawFirstToken = false
    log(ChatLogLevel.INFO, "send_started", mapOf("message_id" to userMessage.id))
    val job = controllerScope.launch(start = CoroutineStart.LAZY) {
        try {
            messageSender.send(userMessage).collect { event ->
                if (token != requestToken) return@collect
                when (event) {
                    is ChatStreamEvent.TextDelta -> sawFirstToken = applyTextDelta(event, sawFirstToken)
                    is ChatStreamEvent.ReplaceMessage -> event.message.let { message ->
                        if (message.author.id != currentUser.id &&
                            (message.status == MessageStatus.STREAMING || message.status == MessageStatus.SENDING)
                        ) activeAssistantId = message.id
                        updateMessage(message)
                    }
                    ChatStreamEvent.Completed -> completeLatestAssistant()
                    is ChatStreamEvent.Failed -> {
                        log(ChatLogLevel.WARN, "send_failed", mapOf("message_id" to userMessage.id), null)
                        fail(event.error.message, event.error.code, event.error)
                    }
                }
            }
            if (token == requestToken && state.isSending) completeLatestAssistant()
        } catch (cancelled: CancellationException) {
            log(ChatLogLevel.WARN, "stream_cancelled", mapOf("message_id" to userMessage.id), cancelled)
            throw cancelled
        } catch (error: Throwable) {
            if (token == requestToken && !closed) {
                log(ChatLogLevel.ERROR, "send_failed", mapOf("message_id" to userMessage.id), error)
                fail(error.message ?: "Failed to send message", "send")
            }
        } finally {
            if (requestJob === currentCoroutineContext()[Job]) {
                requestJob = null
                internalRequestActive = false
                updateSendingState()
                startNextQueuedIfNeeded()
            }
        }
    }
    requestJob = job
    job.start()
}

private fun ChatController.applyTextDelta(event: ChatStreamEvent.TextDelta, sawFirstToken: Boolean = false): Boolean {
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
    val textFormat = assistant.parts.filterIsInstance<MessagePart.Text>().firstOrNull()?.format
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
    val firstToken = !sawFirstToken
    if (firstToken) {
        log(ChatLogLevel.INFO, "stream_first_token", emptyMap())
    }
    emit(ChatEffect.ScrollToBottom)
    return firstToken
}

private fun ChatController.completeLatestAssistant() {
    val completedAssistantId = activeAssistantId
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
    log(ChatLogLevel.INFO, "send_completed", mapOf("message_id" to completedAssistantId.orEmpty()))
    _state.value = state.copy(
        partialMessageIds = state.partialMessageIds - completedAssistantId.orEmpty(),
    )
    updateSendingState()
    startNextQueuedIfNeeded()
    emit(ChatEffect.ScrollToBottom)
}

private fun ChatController.fail(message: String, code: String, error: ChatError = ChatError(message, code)) {
    val failedMessage = activeAssistantId?.let { assistantId ->
        state.messages.firstOrNull { it.id == assistantId }
    } ?: state.messages.lastOrNull { it.author.id == currentUser.id && it.status == MessageStatus.SENDING }
    val queuedUserMessage = pendingUserMessageId?.let { userId ->
        state.messages.firstOrNull { it.id == userId }
    }?.takeIf { it.status == MessageStatus.SENDING }
    failedMessage?.let { failed ->
        if (failed.author.id != currentUser.id && failed.text.isNotEmpty()) {
            updateMessage(failed.copy(status = MessageStatus.STREAMING_PARTIAL))
            _state.value = state.copy(
                partialMessageIds = (state.partialMessageIds + failed.id).distinct(),
            )
        } else {
            updateMessage(failed.copy(status = MessageStatus.FAILED))
        }
    }
    if (error.retryable && sender != null && queuedUserMessage != null) {
        queuedUserMessage?.let { userMessage ->
            sendQueue.addFirst(userMessage)
            _state.value = state.copy(
                queuedMessages = listOf(userMessage) + state.queuedMessages,
                connectionState = ChatConnectionState.DISCONNECTED,
            )
        }
    } else {
        startNextQueuedIfNeeded()
    }
    internalRequestActive = false
    activeAssistantId = null
    pendingUserMessageId = null
    _state.value = state.copy(
        isSending = externalStreamingMessageIdSet.isNotEmpty(),
        error = error,
    )
    emit(ChatEffect.ShowError(message))
}

internal fun ChatController.updateSendingState() {
    _state.value = state.copy(
        isSending = internalRequestActive || externalStreamingMessageIdSet.isNotEmpty()
    )
}

internal fun ChatController.nextLocalId(prefix: String): String {
    var candidate: String
    do {
        candidate = "$prefix-${localId++}"
    } while (state.messages.any { it.id == candidate })
    return candidate
}

internal fun ChatController.maybeIncrementUnread(message: ChatMessage? = null) {
    if (!isListNearBottom) {
        if (message != null && !message.countsAsUnread()) return
        _state.value = state.copy(unreadCount = state.unreadCount + 1)
    }
}
