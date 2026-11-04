package com.tencent.kuiklybase.chat.compose.core

/** Encodes and parses `data:<mime>;base64,<payload>` without decoding payload bytes. */
public object AttachmentUriCodec {
    public data class ParsedDataUri(
        val mimeType: String,
        val base64Payload: String,
    )

    public fun toDataUri(mimeType: String, base64Payload: String): String {
        val mime = mimeType.trim().ifEmpty { "application/octet-stream" }
        val payload = base64Payload.trim()
        require(payload.isNotEmpty()) { "base64 payload must not be empty" }
        return "data:$mime;base64,$payload"
    }

    public fun parseDataUri(uri: String): ParsedDataUri? {
        val trimmed = uri.trim()
        if (!trimmed.startsWith("data:")) return null
        val body = trimmed.substring(5)
        val marker = ";base64,"
        val markerIndex = body.indexOf(marker)
        if (markerIndex < 0) return null
        val mimeType = body.substring(0, markerIndex).trim()
        if (mimeType.isEmpty()) return null
        val payload = body.substring(markerIndex + marker.length)
        if (payload.isEmpty()) return null
        return ParsedDataUri(mimeType = mimeType, base64Payload = payload)
    }
}
