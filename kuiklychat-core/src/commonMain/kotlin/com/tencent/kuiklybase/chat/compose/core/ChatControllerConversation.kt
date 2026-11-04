package com.tencent.kuiklybase.chat.compose.core

/**
 * Drops the in-flight reply, then shows [messages] as the visible conversation.
 * Events from the cancelled request are ignored.
 */
public fun ChatController.showConversation(messages: List<ChatMessage>) {
    showConversationSnapshot(messages)
}

/** Sends the preceding user message again and replaces this assistant reply. */
public fun ChatController.regenerateReply(messageId: String) {
    if (state.isSending) return
    val index = state.messages.indexOfFirst { it.id == messageId }
    if (index < 0) return
    val target = state.messages[index]
    if (target.author.id == currentUser.id) return
    if (target.status == MessageStatus.STREAMING || target.status == MessageStatus.SENDING) return
    val prompt = state.messages.take(index).lastOrNull { it.author.id == currentUser.id } ?: return
    lastUserSubmission = prompt.parts
    lastUserPrompt = prompt.text
    pendingUserMessageId = prompt.id
    _state.value = state.copy(
        messages = state.messages.filterNot { it.id == messageId },
        error = null,
    )
    startRequest(prompt)
}
