package com.tencent.kuiklybase.chat.compose.ui

import com.tencent.kuiklybase.chat.compose.adapter.AttachmentUriCodec
import com.tencent.kuiklybase.chat.compose.core.MessagePart

fun isImageMimeType(mimeType: String): Boolean = mimeType.startsWith("image/")

fun isDisplayableImageSource(source: String): Boolean {
    val trimmed = source.trim()
    if (trimmed.isEmpty()) return false
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return true
    if (trimmed.startsWith("file://") || trimmed.startsWith("content://")) return true
    val dataUri = AttachmentUriCodec.parseDataUri(trimmed)
    return dataUri != null && dataUri.mimeType.startsWith("image/")
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return ""
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024
        unitIndex++
    }
    return if (unitIndex == 0) {
        "${bytes} B"
    } else {
        val display = when {
            value >= 100 -> value.toInt().toString()
            value % 1.0 == 0.0 -> value.toInt().toString()
            else -> {
                val tenths = (value * 10).toInt()
                "${tenths / 10}.${tenths % 10}"
            }
        }
        "$display ${units[unitIndex]}"
    }
}

fun attachmentDisplaySource(attachment: MessagePart.Attachment): String {
    val preview = attachment.previewUri.trim()
    if (preview.isNotEmpty()) return preview
    return attachment.uri.trim()
}
