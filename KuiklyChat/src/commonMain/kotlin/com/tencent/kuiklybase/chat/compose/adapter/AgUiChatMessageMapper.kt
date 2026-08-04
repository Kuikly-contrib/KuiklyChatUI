package com.tencent.kuiklybase.chat.compose.adapter

import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.compose.core.MessageStatus
import com.tencent.kuiklybase.chat.model.ChatMessage as LegacyChatMessage
import com.tencent.kuiklybase.chat.model.MessageStatus as LegacyMessageStatus
import com.tencent.kuiklybase.chat.model.MessageType

/** Converts the legacy AG-UI/session model at the adapter boundary. */
fun LegacyChatMessage.toComposeChatMessage(
    currentUserId: String,
    assistant: ChatUser = ChatUser("assistant", "Assistant")
): ChatMessage {
    val authorId = if (isSelf) currentUserId else senderId.ifEmpty { assistant.id }
    val author = if (isSelf) {
        ChatUser(currentUserId, senderName.ifEmpty { "You" }, senderAvatar)
    } else {
        assistant.copy(
            id = authorId,
            name = senderName.ifEmpty { assistant.name },
            avatar = senderAvatar.ifEmpty { assistant.avatar }
        )
    }
    val parts = buildList {
        if (content.isNotEmpty()) {
            if (type == MessageType.CUSTOM || extra["agUiKind"] != null) {
                add(
                    MessagePart.Custom(
                        type = extra["agentCardType"] ?: extra["agUiKind"] ?: "custom",
                        payload = extra + ("text" to content)
                    )
                )
            } else {
                add(MessagePart.Text(content))
            }
        }
        addAll(attachments.mapIndexed { index, attachment ->
            MessagePart.Attachment(
                id = "$id-attachment-$index",
                uri = attachment.url,
                mimeType = attachment.mimeType,
                name = attachment.title,
                sizeBytes = attachment.fileSize
            )
        })
    }
    return ChatMessage(
        id = id,
        author = author,
        parts = parts,
        status = status.toComposeStatus(),
        createdAt = timestamp,
        metadata = extra
    )
}

private fun LegacyMessageStatus.toComposeStatus(): MessageStatus = when (this) {
    LegacyMessageStatus.SENDING -> MessageStatus.SENDING
    LegacyMessageStatus.SENT, LegacyMessageStatus.READ -> MessageStatus.SENT
    LegacyMessageStatus.FAILED -> MessageStatus.FAILED
}
