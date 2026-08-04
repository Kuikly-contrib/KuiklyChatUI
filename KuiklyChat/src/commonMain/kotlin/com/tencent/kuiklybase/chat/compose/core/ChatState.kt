package com.tencent.kuiklybase.chat.compose.core

/** Immutable snapshot consumed by Compose UI. */
data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val isInitialLoading: Boolean = false,
    val isLoadingEarlier: Boolean = false,
    val hasMoreEarlier: Boolean = false,
    val isSending: Boolean = false,
    val error: ChatError? = null,
    val unreadCount: Int = 0,
    val lastMessageId: String? = null
)

data class ChatError(
    val message: String,
    val code: String = "unknown",
    val retryable: Boolean = true
)

sealed interface ChatEffect {
    data object ScrollToBottom : ChatEffect
    data class ShowError(val message: String) : ChatEffect
    data class CopyToClipboard(val text: String) : ChatEffect
}

sealed interface ChatIntent {
    data class SendText(val text: String) : ChatIntent
    data object StopGeneration : ChatIntent
    data class RetryMessage(val messageId: String) : ChatIntent
    data object LoadEarlier : ChatIntent
    data object Refresh : ChatIntent
    data object ClearError : ChatIntent
    data object ClearUnread : ChatIntent
    data class DeleteMessage(val messageId: String) : ChatIntent
    data class UpsertMessage(val message: ChatMessage) : ChatIntent
    data class ReplaceMessages(
        val messages: List<ChatMessage>,
        val hasMoreEarlier: Boolean? = null
    ) : ChatIntent
    data class SetListNearBottom(val nearBottom: Boolean) : ChatIntent
}

/** Events emitted by a host sender while a response is being generated. */
sealed interface ChatStreamEvent {
    data class TextDelta(val messageId: String? = null, val text: String) : ChatStreamEvent
    data class ReplaceMessage(val message: ChatMessage) : ChatStreamEvent
    data object Completed : ChatStreamEvent
    data class Failed(val error: ChatError) : ChatStreamEvent
}

data class ChatHistoryPage(
    val messages: List<ChatMessage>,
    val hasMoreEarlier: Boolean
)

fun interface ChatMessageSender {
    suspend fun send(message: ChatMessage): kotlinx.coroutines.flow.Flow<ChatStreamEvent>
}

fun interface ChatHistoryLoader {
    suspend fun loadEarlier(beforeMessageId: String?): ChatHistoryPage
}
