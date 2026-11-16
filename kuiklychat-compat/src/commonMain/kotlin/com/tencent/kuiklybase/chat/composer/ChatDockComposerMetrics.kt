package com.tencent.kuiklybase.chat.composer

import com.tencent.kuiklybase.chat.compose.core.AttachmentUriCodec
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmAttachment
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmAttachmentKind

internal const val DOCK_ACTION_SIZE = 44f
internal const val DOCK_VERTICAL_PADDING = 8f
internal const val DOCK_INPUT_LINE_HEIGHT = 23f
internal const val DOCK_MAX_INPUT_LINES = 3
internal const val DOCK_KEYBOARD_CLEARANCE = 10f
internal const val DOCK_MAX_NAVIGATION_INSET = 48f
internal const val DOCK_BOTTOM_GAP = 4f

internal fun composerInputLineHeight(fontSize: Float): Float =
    maxOf(DOCK_INPUT_LINE_HEIGHT, fontSize + 8f)

internal fun composerMaxInputHeight(fontSize: Float): Float =
    maxOf(DOCK_ACTION_SIZE, composerInputLineHeight(fontSize) * DOCK_MAX_INPUT_LINES)

/** Keyboard height replaces the navigation inset without counting a lagged IME inset twice. */
internal fun composerDockBottom(keyboardHeight: Float, safeAreaBottom: Float): Float {
    val navigationBarInset = safeAreaBottom.coerceIn(0f, DOCK_MAX_NAVIGATION_INSET)
    val inset = if (keyboardHeight <= 0.5f) {
        navigationBarInset
    } else {
        val duplicatedInset = (safeAreaBottom - navigationBarInset).coerceAtLeast(0f)
        (keyboardHeight - duplicatedInset).coerceAtLeast(0f) + DOCK_KEYBOARD_CLEARANCE
    }
    return inset + DOCK_BOTTOM_GAP
}

internal fun LlmAttachment.toMessageAttachment(): MessagePart.Attachment {
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
    val previewUri = when {
        localPreviewUri.isNotEmpty() -> localPreviewUri
        kind == LlmAttachmentKind.IMAGE && thumbnailBase64.isNotEmpty() ->
            AttachmentUriCodec.toDataUri(displayMimeType, thumbnailBase64)
        else -> ""
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
