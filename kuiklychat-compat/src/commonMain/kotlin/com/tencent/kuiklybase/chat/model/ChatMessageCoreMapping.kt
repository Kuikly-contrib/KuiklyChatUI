package com.tencent.kuiklybase.chat.model

import com.tencent.kuiklybase.chat.compose.core.ChatMessage as CoreChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.compose.core.SYSTEM_CHAT_USER

/**
 * Author remap for Compose: session [ChatMessage] is already the canonical type.
 */
public fun ChatMessage.toCore(
    currentUserId: String,
    assistant: ChatUser = ChatUser("assistant", "Assistant"),
): CoreChatMessage {
    val isPlainSystemNotice = type == MessageType.SYSTEM &&
        content.isNotEmpty() &&
        extra["agUiKind"].isNullOrEmpty()
    val authorId = if (isSelf) currentUserId else senderId.ifEmpty { assistant.id }
    val author = when {
        isPlainSystemNotice -> SYSTEM_CHAT_USER
        isSelf -> ChatUser(currentUserId, senderName.ifEmpty { "You" }, senderAvatar)
        else -> assistant.copy(
            id = authorId,
            name = senderName.ifEmpty { assistant.name },
            avatar = senderAvatar.ifEmpty { assistant.avatar },
        )
    }
    return copy(
        author = author,
        parts = resolveParts(),
        status = status,
        createdAt = timestamp,
        metadata = extra,
        outgoing = author.id == currentUserId,
    )
}

/** Inverse of [toCore]: mark [outgoing] from [currentUserId]. */
public fun CoreChatMessage.toSessionMessage(currentUserId: String): ChatMessage {
    return copy(outgoing = author.id == currentUserId)
}

internal fun ChatMessage.withCanonicalParts(): ChatMessage {
    if (parts.isNotEmpty()) return this
    return copy(parts = legacyPartsFromType())
}

internal fun ChatMessage.withRebuiltParts(): ChatMessage = copy(parts = legacyPartsFromType())

internal fun ChatMessage.withUpdatedContent(
    content: String,
    status: MessageStatus = sessionStatus,
): ChatMessage {
    val updatedParts = rebuildPartsForContent(content)
    return copy(parts = updatedParts, status = status)
}

private fun ChatMessage.rebuildPartsForContent(content: String): List<MessagePart> {
    if (isStructuredAgUiCard()) {
        return listOf(
            MessagePart.Custom(
                type = extra["agentCardType"] ?: extra["agUiKind"] ?: extra[AgUiExtraKeys.KIND] ?: "custom",
                payload = extra + ("text" to content),
            ),
        )
    }
    if (type == MessageType.SYSTEM) {
        return if (content.isEmpty()) emptyList() else listOf(MessagePart.SystemNotice(content))
    }
    val nonText = parts.filterNot { it is MessagePart.Text }
    val textParts = parts.filterIsInstance<MessagePart.Text>()
    val newText = if (content.isBlank()) {
        emptyList()
    } else {
        val format = textParts.firstOrNull()?.format
            ?: com.tencent.kuiklybase.chat.compose.core.TextFormat.PLAIN
        listOf(MessagePart.Text(content, format))
    }
    return newText + nonText
}

internal fun sessionTypeFromParts(
    parts: List<MessagePart>,
    metadata: Map<String, String>,
): MessageType {
    val kind = metadata["agUiKind"]
    // Kuikly session historically stored these as SYSTEM + extra so default
    // bubbles stay grey; Compose still maps them to MessagePart.Custom.
    if (kind == "reasoning" || kind == "tool_result") return MessageType.SYSTEM
    if (!kind.isNullOrEmpty() && kind != "streaming") return MessageType.CUSTOM
    if (parts.size == 1 && parts.first() is MessagePart.SystemNotice) return MessageType.SYSTEM
    if (parts.any { it is MessagePart.Custom }) return MessageType.CUSTOM
    val atts = parts.filterIsInstance<MessagePart.Attachment>()
    val texts = parts.filterIsInstance<MessagePart.Text>()
    if (atts.isNotEmpty() && texts.isEmpty()) {
        val mime = atts.first().mimeType.lowercase()
        return when {
            mime.startsWith("video/") -> MessageType.VIDEO
            mime.startsWith("image/") || mime == "image/unknown" -> MessageType.IMAGE
            else -> MessageType.FILE
        }
    }
    return MessageType.TEXT
}
