package com.tencent.kuiklybase.chat.compose.adapter

import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmAttachment
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmAttachmentKind

/** Maps provider [LlmAttachment] to Compose [MessagePart.Attachment] for host pickers. */
fun LlmAttachment.toComposeAttachment(): MessagePart.Attachment {
    val displayMimeType = mimeType.ifEmpty {
        when (kind) {
            LlmAttachmentKind.IMAGE -> "image/unknown"
            LlmAttachmentKind.AUDIO -> "audio/unknown"
            else -> "application/octet-stream"
        }
    }
    val sourceUri = when {
        url.isNotEmpty() -> url
        bytesBase64.isNotEmpty() -> AttachmentUriCodec.toDataUri(displayMimeType, bytesBase64)
        else -> ""
    }
    val previewUri = if (kind == LlmAttachmentKind.IMAGE && thumbnailBase64.isNotEmpty()) {
        AttachmentUriCodec.toDataUri(displayMimeType, thumbnailBase64)
    } else {
        ""
    }
    return MessagePart.Attachment(
        id = id,
        uri = sourceUri,
        mimeType = displayMimeType,
        name = name,
        sizeBytes = sizeBytes,
        previewUri = previewUri,
    )
}

internal fun MessagePart.Attachment.toLlmAttachment(): LlmAttachment {
    val parsedUri = AttachmentUriCodec.parseDataUri(uri.trim())
    val thumbnailFromPreview = previewUri.trim().let { preview ->
        AttachmentUriCodec.parseDataUri(preview)?.base64Payload.orEmpty()
    }
    if (parsedUri != null) {
        return LlmAttachment(
            id = id,
            kind = llmKindFromMime(mimeType.ifEmpty { parsedUri.mimeType }),
            name = name,
            mimeType = mimeType.ifEmpty { parsedUri.mimeType },
            url = "",
            bytesBase64 = parsedUri.base64Payload,
            thumbnailBase64 = thumbnailFromPreview,
            sizeBytes = sizeBytes,
        )
    }
    return LlmAttachment(
        id = id,
        kind = llmKindFromMime(mimeType),
        name = name,
        mimeType = mimeType,
        url = uri,
        bytesBase64 = "",
        thumbnailBase64 = thumbnailFromPreview,
        sizeBytes = sizeBytes,
    )
}

private fun llmKindFromMime(mimeType: String): LlmAttachmentKind = when {
    mimeType.startsWith("image/") -> LlmAttachmentKind.IMAGE
    mimeType.startsWith("audio/") -> LlmAttachmentKind.AUDIO
    else -> LlmAttachmentKind.FILE
}
