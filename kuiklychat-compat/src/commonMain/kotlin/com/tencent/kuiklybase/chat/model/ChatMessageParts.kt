package com.tencent.kuiklybase.chat.model

import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.compose.core.TextFormat

/**
 * 解析有效内容：显式 [ChatMessage.parts] 优先，否则从 content / type / attachments 推导。
 * Kuikly DSL 与 Compose adapter 共用同一套 [MessagePart]，不再经过 ChatContentBlock。
 */
internal fun ChatMessage.resolveParts(): List<MessagePart> {
    if (parts.isNotEmpty()) return parts
    return deriveParts(id, content, type, extra, attachments, emptyList())
}

internal fun deriveParts(
    id: String,
    content: String,
    type: MessageType,
    extra: Map<String, String>,
    attachments: List<Attachment>,
    existingParts: List<MessagePart>,
): List<MessagePart> {
    if (existingParts.isNotEmpty()) return existingParts
    return legacyPartsFromFields(id, content, type, extra, attachments)
}

internal fun ChatMessage.isStructuredAgUiCard(): Boolean {
    val kind = extra["agUiKind"]
    return !kind.isNullOrEmpty() && kind != "streaming"
}

private fun isStructuredAgUiKind(extra: Map<String, String>): Boolean {
    val kind = extra["agUiKind"]
    return !kind.isNullOrEmpty() && kind != "streaming"
}

internal fun buildMessageParts(
    messageId: String,
    text: String,
    attachments: List<Attachment> = emptyList(),
): List<MessagePart> {
    val result = mutableListOf<MessagePart>()
    if (text.isNotBlank()) {
        result += MessagePart.Text(text, TextFormat.PLAIN)
    }
    attachments.forEachIndexed { index, attachment ->
        result += attachment.toMessagePart(messageId, index)
    }
    return result
}

public fun ChatMessage.primaryTextContent(): String {
    resolveParts().filterIsInstance<MessagePart.Text>().forEach { part ->
        if (part.value.isNotBlank()) return part.value
    }
    return content
}

internal fun ChatMessage.legacyPartsFromType(): List<MessagePart> =
    legacyPartsFromFields(id, content, type, extra, attachments)

internal fun legacyPartsFromFields(
    id: String,
    content: String,
    type: MessageType,
    extra: Map<String, String>,
    attachments: List<Attachment>,
): List<MessagePart> {
    if (isStructuredAgUiKind(extra)) {
        return listOf(
            MessagePart.Custom(
                type = extra["agentCardType"] ?: extra["agUiKind"] ?: extra[AgUiExtraKeys.KIND] ?: "custom",
                payload = extra + ("text" to content),
            ),
        )
    }
    return when (type) {
        MessageType.TEXT -> buildMessageParts(id, content, attachments)
        MessageType.SYSTEM -> {
            if (content.isEmpty()) emptyList() else listOf(MessagePart.SystemNotice(content))
        }
        MessageType.CUSTOM -> {
            if (content.isEmpty() && extra.isEmpty()) {
                emptyList()
            } else {
                listOf(
                    MessagePart.Custom(
                        type = extra["agentCardType"]
                            ?: extra["agUiKind"]
                            ?: extra[AgUiExtraKeys.KIND]
                            ?: "custom",
                        payload = extra + ("text" to content),
                    ),
                )
            }
        }
        MessageType.IMAGE, MessageType.VIDEO -> {
            if (attachments.isNotEmpty()) {
                prependCaption(content, attachments.mapIndexed { index, attachment ->
                    attachment.toMessagePart(id, index)
                })
            } else if (content.isNotBlank()) {
                listOf(
                    MessagePart.Attachment(
                        id = "$id-image-0",
                        uri = content,
                        mimeType = if (type == MessageType.VIDEO) "video/unknown" else "image/unknown",
                    ),
                )
            } else {
                emptyList()
            }
        }
        MessageType.FILE -> {
            if (attachments.isNotEmpty()) {
                prependCaption(content, attachments.mapIndexed { index, attachment ->
                    attachment.toMessagePart(id, index)
                })
            } else {
                val fileUrl = extra["fileUrl"].orEmpty().ifEmpty { content }
                if (fileUrl.isEmpty()) {
                    emptyList()
                } else {
                    listOf(
                        MessagePart.Attachment(
                            id = "$id-file-0",
                            uri = fileUrl,
                            mimeType = extra["mimeType"].orEmpty(),
                            name = extra["fileName"].orEmpty().ifEmpty { content },
                            sizeBytes = extra["fileSize"]?.toLongOrNull() ?: 0L,
                        ),
                    )
                }
            }
        }
        else -> emptyList()
    }
}

private fun prependCaption(
    content: String,
    attachments: List<MessagePart.Attachment>,
): List<MessagePart> {
    if (content.isEmpty() || attachments.isEmpty()) return attachments
    val attachmentUris = attachments.map { it.uri }
    val attachmentNames = attachments.map { it.name }
    if (content in attachmentUris || content in attachmentNames) return attachments
    return listOf(MessagePart.Text(content)) + attachments
}

internal fun Attachment.toMessagePart(messageId: String, index: Int): MessagePart.Attachment {
    val resolvedMime = when {
        mimeType.isNotEmpty() -> mimeType
        type == AttachmentType.IMAGE -> "image/unknown"
        else -> mimeType
    }
    return MessagePart.Attachment(
        id = "$messageId-attachment-$index",
        uri = url,
        mimeType = resolvedMime,
        name = title,
        sizeBytes = fileSize,
        previewUri = thumbnailUrl,
    )
}

internal fun MessagePart.Attachment.toLegacyAttachment(): Attachment {
    val attachmentType = when {
        isImagePart() -> AttachmentType.IMAGE
        mimeType.lowercase().startsWith("video/") -> AttachmentType.VIDEO
        else -> AttachmentType.FILE
    }
    return Attachment(
        type = attachmentType,
        url = uri,
        title = name,
        mimeType = mimeType,
        fileSize = sizeBytes,
        thumbnailUrl = previewUri,
    )
}

internal fun MessagePart.Attachment.isImagePart(): Boolean {
    val mime = mimeType.lowercase()
    return mime.startsWith("image/") || mime == "image/unknown"
}
