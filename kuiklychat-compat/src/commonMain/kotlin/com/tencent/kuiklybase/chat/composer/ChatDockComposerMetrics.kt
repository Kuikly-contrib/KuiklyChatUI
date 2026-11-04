package com.tencent.kuiklybase.chat.composer

import com.tencent.kuiklybase.chat.compose.core.AttachmentUriCodec
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmAttachment
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmAttachmentKind

internal const val DOCK_COLLAPSED_HEIGHT = 60f
internal const val DOCK_EXPANDED_HEIGHT = 112f
internal const val DOCK_TOOLBAR_HEIGHT = 48f
internal const val DOCK_ATTACHMENT_STRIP_HEIGHT = 82f
internal const val DOCK_ATTACHMENT_TEXT_GAP = 8f
internal const val DOCK_INPUT_LINE_HEIGHT = 23f
internal const val DOCK_MAX_INPUT_LINES = 3
internal const val DOCK_MULTILINE_TEXT_TOP = 12f
internal const val DOCK_TEXT_TOOLBAR_GAP = 4f
internal const val DOCK_COMPACT_TOOLBAR_HEIGHT = 44f
internal const val DOCK_COMPACT_TOOLBAR_BOTTOM = 8f
internal const val DOCK_KEYBOARD_CLEARANCE = 20f
internal const val DOCK_MAX_NAVIGATION_INSET = 48f
internal const val DOCK_BOTTOM_GAP = 4f

/**
 * Distance from the screen bottom to the dock.
 * Keyboard height replaces the navigation-bar inset, and a lagged safe-area value
 * that still includes the IME is subtracted so the dock is not pushed twice.
 */
internal fun estimateComposerLineCount(
    text: String,
    maxLines: Int,
    charsPerLine: Int = 20,
): Int {
    if (text.isEmpty()) return 1
    val perLine = charsPerLine.coerceAtLeast(8)
    val lines = text.split('\n').sumOf { line ->
        if (line.isEmpty()) 1 else (line.length + perLine - 1) / perLine
    }
    return lines.coerceIn(1, maxLines.coerceAtLeast(1))
}

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

internal fun composerPanelHeight(
    expanded: Boolean,
    lineCount: Int,
    hasAttachments: Boolean,
    textAboveIcons: Boolean = false,
): Float {
    val lines = lineCount.coerceIn(1, DOCK_MAX_INPUT_LINES)
    val extraLines = (lines - 1).coerceAtLeast(0)
    if (hasAttachments) {
        val textHeight = 15f + DOCK_INPUT_LINE_HEIGHT * lines
        return DOCK_ATTACHMENT_STRIP_HEIGHT + DOCK_ATTACHMENT_TEXT_GAP + textHeight +
            DOCK_TEXT_TOOLBAR_GAP + DOCK_TOOLBAR_HEIGHT + DOCK_COMPACT_TOOLBAR_BOTTOM
    }
    if (!expanded && (textAboveIcons || lines > 1)) {
        val textHeight = 15f + DOCK_INPUT_LINE_HEIGHT * lines
        return DOCK_MULTILINE_TEXT_TOP + textHeight + DOCK_TEXT_TOOLBAR_GAP +
            DOCK_COMPACT_TOOLBAR_HEIGHT + DOCK_COMPACT_TOOLBAR_BOTTOM
    }
    val base = if (expanded) DOCK_EXPANDED_HEIGHT else DOCK_COLLAPSED_HEIGHT
    return base + DOCK_INPUT_LINE_HEIGHT * extraLines
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
