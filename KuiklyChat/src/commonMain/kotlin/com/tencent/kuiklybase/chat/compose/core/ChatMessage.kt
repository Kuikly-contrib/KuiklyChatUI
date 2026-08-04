package com.tencent.kuiklybase.chat.compose.core

/** Stable identity for a chat participant. */
data class ChatUser(
    val id: String,
    val name: String = "",
    val avatar: String = "",
    val metadata: Map<String, String> = emptyMap()
)

/** UI-independent content carried by a chat message. */
sealed interface MessagePart {
    data class Text(
        val value: String,
        val format: TextFormat = TextFormat.PLAIN
    ) : MessagePart

    /** Typed UI content kept at the adapter boundary, not exposed to AG-UI protocol code. */
    data class AgentCardContent(
        val card: com.tencent.kuiklybase.chat.agentcard.AgentCard
    ) : MessagePart

    data class Attachment(
        val id: String,
        val uri: String,
        val mimeType: String = "",
        val name: String = "",
        val sizeBytes: Long = 0L
    ) : MessagePart

    data class Custom(
        val type: String,
        val payload: Map<String, String> = emptyMap()
    ) : MessagePart
}

enum class TextFormat {
    PLAIN,
    MARKDOWN
}

enum class MessageStatus {
    SENDING,
    STREAMING,
    SENT,
    FAILED,
    CANCELLED
}

/** Shared message model used by the Compose SDK and the future Kuikly DSL renderer. */
data class ChatMessage(
    val id: String,
    val author: ChatUser,
    val parts: List<MessagePart>,
    val status: MessageStatus = MessageStatus.SENT,
    val createdAt: Long = 0L,
    val replyToId: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    val text: String
        get() = parts.filterIsInstance<MessagePart.Text>().joinToString(separator = "") { it.value }
}

enum class MessageGroupStatus {
    SINGLE,
    FIRST,
    MIDDLE,
    LAST
}

fun ChatMessage.groupStatus(
    previous: ChatMessage?,
    next: ChatMessage?,
    groupingWindowMillis: Long = DEFAULT_GROUPING_WINDOW_MILLIS
): MessageGroupStatus {
    val sameAsPrevious = previous != null && canGroupWith(previous, groupingWindowMillis)
    val sameAsNext = next != null && canGroupWith(next, groupingWindowMillis)
    return when {
        !sameAsPrevious && !sameAsNext -> MessageGroupStatus.SINGLE
        !sameAsPrevious -> MessageGroupStatus.FIRST
        !sameAsNext -> MessageGroupStatus.LAST
        else -> MessageGroupStatus.MIDDLE
    }
}

private fun ChatMessage.canGroupWith(other: ChatMessage, windowMillis: Long): Boolean {
    return author.id == other.author.id &&
        (createdAt == 0L || other.createdAt == 0L ||
            kotlin.math.abs(createdAt - other.createdAt) <= windowMillis)
}

const val DEFAULT_GROUPING_WINDOW_MILLIS: Long = 5 * 60 * 1000L
