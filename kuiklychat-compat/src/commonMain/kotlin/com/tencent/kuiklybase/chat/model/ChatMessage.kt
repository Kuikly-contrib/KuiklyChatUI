package com.tencent.kuiklybase.chat.model

import com.tencent.kuiklybase.chat.compose.core.ChatMessage as CoreChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.compose.core.QuotedPreview
import com.tencent.kuiklybase.chat.compose.core.SYSTEM_CHAT_USER
import com.tencent.kuiklybase.chat.compose.core.ReactionItem as CoreReactionItem

/** Single SDK message type: Compose core [CoreChatMessage] with ordered [MessagePart]. */
public typealias ChatMessage = CoreChatMessage

/** Session-facing alias of core [CoreReactionItem]. */
public typealias ReactionItem = CoreReactionItem

public val ChatMessage.isSelf: Boolean get() = outgoing
public val ChatMessage.senderName: String get() = author.name
public val ChatMessage.senderAvatar: String get() = author.avatar
public val ChatMessage.senderId: String get() = author.id
public val ChatMessage.timestamp: Long get() = createdAt
public val ChatMessage.extra: Map<String, String> get() = metadata

public val ChatMessage.content: String
    get() {
        parts.filterIsInstance<MessagePart.Text>().forEach { part ->
            if (part.value.isNotBlank()) return part.value
        }
        parts.filterIsInstance<MessagePart.SystemNotice>().firstOrNull()?.text?.let { return it }
        parts.filterIsInstance<MessagePart.Custom>().firstOrNull()?.payload?.get("text")
            ?.takeIf { it.isNotEmpty() }?.let { return it }
        parts.filterIsInstance<MessagePart.Attachment>().firstOrNull()?.uri?.let { return it }
        return text
    }

public val ChatMessage.type: MessageType
    get() = sessionTypeFromParts(parts, metadata)

public val ChatMessage.sessionStatus: MessageStatus
    get() = if (status == MessageStatus.CANCELLED) MessageStatus.FAILED else status

public val ChatMessage.isRead: Boolean get() = readBy.isNotEmpty()

public val ChatMessage.attachments: List<Attachment>
    get() = parts.filterIsInstance<MessagePart.Attachment>().map { it.toLegacyAttachment() }

public val ChatMessage.quotedMessage: ChatMessage?
    get() {
        val preview = quotedPreview ?: return null
        return sessionChatMessage(
            id = preview.id,
            content = preview.content,
            isSelf = false,
            senderName = preview.senderName,
        )
    }

/**
 * Kuikly-session constructor matching the old envelope invoke.
 * Payload lives only on ordered [MessagePart].
 */
public fun sessionChatMessage(
    id: String,
    content: String,
    isSelf: Boolean,
    type: MessageType = MessageType.TEXT,
    status: MessageStatus = MessageStatus.SENT,
    senderName: String = "",
    senderAvatar: String = "",
    timestamp: Long = 0L,
    extra: Map<String, String> = emptyMap(),
    senderId: String = "",
    reactions: List<ReactionItem> = emptyList(),
    threadCount: Int = 0,
    isEdited: Boolean = false,
    isDeleted: Boolean = false,
    isPinned: Boolean = false,
    readBy: List<String> = emptyList(),
    attachments: List<Attachment> = emptyList(),
    parts: List<MessagePart> = emptyList(),
    quotedMessage: ChatMessage? = null,
): ChatMessage {
    val resolvedParts = deriveParts(id, content, type, extra, attachments, parts)
    val authorId = senderId.ifEmpty { if (isSelf) "self" else "assistant" }
    val isPlainSystem = type == MessageType.SYSTEM &&
        content.isNotEmpty() &&
        extra["agUiKind"].isNullOrEmpty()
    val author = if (isPlainSystem) {
        SYSTEM_CHAT_USER
    } else {
        ChatUser(id = authorId, name = senderName, avatar = senderAvatar)
    }
    return CoreChatMessage(
        id = id,
        author = author,
        parts = resolvedParts,
        status = status,
        createdAt = timestamp,
        replyToId = quotedMessage?.id,
        metadata = extra,
        outgoing = isSelf,
        reactions = reactions,
        threadCount = threadCount,
        isEdited = isEdited,
        isDeleted = isDeleted,
        isPinned = isPinned,
        readBy = readBy,
        quotedPreview = quotedMessage?.let { quoted ->
            QuotedPreview(
                id = quoted.id,
                senderName = quoted.senderName,
                content = quoted.content,
            )
        },
    )
}

/**
 * 消息上下文（参考 Stream Chat 的 MessageItemState）
 */
public data class MessageContext(
    val message: ChatMessage,
    val previousMessage: ChatMessage? = null,
    val nextMessage: ChatMessage? = null,
    val index: Int = 0,
    val isFirstInGroup: Boolean = true,
    val isLastInGroup: Boolean = true
)

/**
 * 时间分组间隔阈值（毫秒）
 */
public const val DEFAULT_TIME_GROUP_INTERVAL: Long = 5 * 60 * 1000L
