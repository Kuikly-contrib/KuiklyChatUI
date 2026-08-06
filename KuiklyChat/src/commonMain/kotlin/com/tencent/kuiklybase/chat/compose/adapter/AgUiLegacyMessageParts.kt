package com.tencent.kuiklybase.chat.compose.adapter

import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.model.Attachment
import com.tencent.kuiklybase.chat.model.AttachmentType
import com.tencent.kuiklybase.chat.model.ChatMessage as LegacyChatMessage
import com.tencent.kuiklybase.chat.model.MessageType

internal fun LegacyChatMessage.buildStandardComposeParts(): List<MessagePart> = when (type) {
    MessageType.IMAGE -> buildImageComposeParts()
    MessageType.FILE -> buildFileComposeParts()
    else -> buildTextAndAttachmentParts()
}

private fun LegacyChatMessage.buildImageComposeParts(): List<MessagePart> {
    val attachments = if (attachments.isNotEmpty()) {
        attachments.mapIndexed { index, attachment -> attachment.toComposePart(id, index) }
    } else if (content.isNotEmpty()) {
        listOf(
            MessagePart.Attachment(
                id = "$id-image-0",
                uri = content,
                mimeType = "image/unknown",
            )
        )
    } else {
        emptyList()
    }
    return prependCaptionIfNeeded(attachments)
}

private fun LegacyChatMessage.buildFileComposeParts(): List<MessagePart> {
    val attachments = if (attachments.isNotEmpty()) {
        attachments.mapIndexed { index, attachment -> attachment.toComposePart(id, index) }
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
                )
            )
        }
    }
    return prependCaptionIfNeeded(attachments)
}

private fun LegacyChatMessage.buildTextAndAttachmentParts(): List<MessagePart> {
    return buildList {
        if (content.isNotEmpty()) {
            add(MessagePart.Text(content))
        }
        addAll(attachments.mapIndexed { index, attachment -> attachment.toComposePart(id, index) })
    }
}

private fun LegacyChatMessage.prependCaptionIfNeeded(
    attachments: List<MessagePart.Attachment>,
): List<MessagePart> {
    if (content.isEmpty() || attachments.isEmpty()) return attachments
    val attachmentUris = attachments.map { it.uri }
    val attachmentNames = attachments.map { it.name }
    if (content in attachmentUris || content in attachmentNames) return attachments
    return listOf(MessagePart.Text(content)) + attachments
}

private fun Attachment.toComposePart(messageId: String, index: Int): MessagePart.Attachment {
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
