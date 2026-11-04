package com.tencent.kuiklybase.chat.compose.core

/** Stable identity for a chat participant. */
public data class ChatUser(
    val id: String,
    val name: String = "",
    val avatar: String = "",
    val metadata: Map<String, String> = emptyMap()
)

/** UI-independent content carried by a chat message. */
public sealed interface MessagePart {
    public data class Text(
        val value: String,
        val format: TextFormat = TextFormat.PLAIN
    ) : MessagePart

    /**
     * Typed Agent-card payload on the shared message model.
     *
     * Keep this part so Compose and Kuikly DSL share one [ChatMessage]. Do not add
     * more card-specific fields here. Mid-term the card can move to [Custom] or an
     * optional module so text-only hosts do not carry this type.
     */
    public data class AgentCardContent(
        val card: com.tencent.kuiklybase.chat.agentcard.AgentCard
    ) : MessagePart

    public data class Attachment(
        val id: String,
        val uri: String,
        val mimeType: String = "",
        val name: String = "",
        val sizeBytes: Long = 0L,
        /** UI preview only; falls back to [uri] when empty. */
        val previewUri: String = "",
    ) : MessagePart

    public data class Custom(
        val type: String,
        val payload: Map<String, String> = emptyMap()
    ) : MessagePart

    /** Visible centered notice (e.g. "user joined"), not a provider system prompt. */
    public data class SystemNotice(
        val text: String,
    ) : MessagePart
}

public enum class TextFormat {
    PLAIN,
    MARKDOWN
}

public enum class MessageStatus {
    SENDING,
    STREAMING,
    SENT,
    FAILED,
    CANCELLED
}

/** Emoji/reaction aggregate stored on the canonical message. */
public data class ReactionItem(
    val type: String,
    val count: Int = 1,
    val isOwnReaction: Boolean = false,
)

/** Snapshot of a replied-to message; [ChatMessage.replyToId] is the stable id. */
public data class QuotedPreview(
    val id: String,
    val senderName: String = "",
    val content: String = "",
)

/** Shared message model used by the Compose SDK and the Kuikly DSL renderer.
 *  Do not keep adding fields here; new payload goes into [MessagePart] or metadata.
 */
public data class ChatMessage(
    val id: String,
    val author: ChatUser,
    val parts: List<MessagePart>,
    val status: MessageStatus = MessageStatus.SENT,
    val createdAt: Long = 0L,
    val replyToId: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    /** True when this message was sent by the active user (Kuikly session). */
    val outgoing: Boolean = false,
    val reactions: List<ReactionItem> = emptyList(),
    val threadCount: Int = 0,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    val isPinned: Boolean = false,
    val readBy: List<String> = emptyList(),
    val quotedPreview: QuotedPreview? = null,
) {
    val text: String
        get() = parts.filterIsInstance<MessagePart.Text>().joinToString(separator = "") { it.value }
}

public enum class MessageGroupStatus {
    SINGLE,
    FIRST,
    MIDDLE,
    LAST
}

public fun ChatMessage.groupStatus(
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
    if (containsSystemNotice || other.containsSystemNotice) return false
    return author.id == other.author.id &&
        (createdAt == 0L || other.createdAt == 0L ||
            kotlin.math.abs(createdAt - other.createdAt) <= windowMillis)
}

public const val DEFAULT_GROUPING_WINDOW_MILLIS: Long = 5 * 60 * 1000L

/** Stable author id for adapter-mapped UI system notices. */
public val SYSTEM_CHAT_USER: ChatUser = ChatUser(id = "__system__", name = "")

public val ChatMessage.systemNoticeOrNull: MessagePart.SystemNotice?
    get() = parts.singleOrNull() as? MessagePart.SystemNotice

public val ChatMessage.containsSystemNotice: Boolean
    get() = parts.any { it is MessagePart.SystemNotice }

/** System notices are informational and should not bump unread badges. */
public fun ChatMessage.countsAsUnread(): Boolean = !containsSystemNotice
