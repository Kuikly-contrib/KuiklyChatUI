package com.tencent.kuiklybase.chat.compose.core

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Immutable snapshot consumed by Compose UI. */
public data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val isInitialLoading: Boolean = false,
    val isLoadingEarlier: Boolean = false,
    val hasMoreEarlier: Boolean = false,
    val isSending: Boolean = false,
    val error: ChatError? = null,
    val unreadCount: Int = 0,
    val lastMessageId: String? = null,
    /** Messages accepted by the controller but waiting for a connected sender. */
    val queuedMessages: List<ChatMessage> = emptyList(),
    val connectionState: ChatConnectionState = ChatConnectionState.CONNECTED,
    /** Assistant replies that received content before a stream stopped or failed. */
    val partialMessageIds: List<String> = emptyList(),
    val draftText: String = "",
    val draftAttachments: List<MessagePart.Attachment> = emptyList(),
)

public enum class ChatConnectionState {
    CONNECTED,
    DISCONNECTED,
}

/** Severity levels for host-injected structured chat logs. */
public enum class ChatLogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR,
}

/** Host hook for structured chat telemetry. Implementations must not log message text or attachment URIs. */
public fun interface ChatLogger {
    public fun log(
        level: ChatLogLevel,
        event: String,
        attributes: Map<String, String>,
        throwable: Throwable?,
    )
}

public data class ChatError(
    val message: String,
    val code: String = "unknown",
    val retryable: Boolean = true
)

public sealed interface ChatEffect {
    public data object ScrollToBottom : ChatEffect
    public data class ShowError(val message: String) : ChatEffect
    public data class CopyToClipboard(val text: String) : ChatEffect
}

public sealed interface ChatIntent {
    public data class SendText(val text: String) : ChatIntent
    public data class SendMessage(
        val text: String,
        val attachments: List<MessagePart.Attachment> = emptyList(),
    ) : ChatIntent
    public data object StopGeneration : ChatIntent
    public data class RetryMessage(val messageId: String) : ChatIntent
    public data object LoadEarlier : ChatIntent
    public data object Refresh : ChatIntent
    public data object ClearError : ChatIntent
    public data object ClearUnread : ChatIntent
    public data class DeleteMessage(val messageId: String) : ChatIntent
    public data class UpsertMessage(val message: ChatMessage) : ChatIntent
    public data class ReplaceMessages(
        val messages: List<ChatMessage>,
        val hasMoreEarlier: Boolean? = null
    ) : ChatIntent
    public data class SetListNearBottom(val nearBottom: Boolean) : ChatIntent
    public data class SetConnectionState(val state: ChatConnectionState) : ChatIntent
    public data class SetDraft(val text: String) : ChatIntent
    public data class SetDraftAttachments(val attachments: List<MessagePart.Attachment>) : ChatIntent
    public data object ClearDraft : ChatIntent
}

/** Events emitted by a host sender while a response is being generated. */
public sealed interface ChatStreamEvent {
    public data class TextDelta(
        val messageId: String? = null,
        val text: String,
        val format: TextFormat = TextFormat.PLAIN,
    ) : ChatStreamEvent
    public data class ReplaceMessage(val message: ChatMessage) : ChatStreamEvent
    public data object Completed : ChatStreamEvent
    public data class Failed(val error: ChatError) : ChatStreamEvent
}

public data class ChatHistoryPage(
    val messages: List<ChatMessage>,
    val hasMoreEarlier: Boolean
)

public fun interface ChatMessageSender {
    public suspend fun send(message: ChatMessage): kotlinx.coroutines.flow.Flow<ChatStreamEvent>
}

public fun interface ChatHistoryLoader {
    public suspend fun loadEarlier(beforeMessageId: String?): ChatHistoryPage
}

internal class ChatEffectEmitter {
    private val sink = MutableSharedFlow<ChatEffect>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val effects: Flow<ChatEffect> = sink.asSharedFlow()

    fun emit(effect: ChatEffect) {
        sink.tryEmit(effect)
    }
}
