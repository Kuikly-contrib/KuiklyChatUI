package com.tencent.kuiklybase.chat.compose.core

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

public fun isNetworkMediaSource(source: String): Boolean {
    val normalized = source.trim().lowercase()
    return normalized.startsWith("http://") ||
        normalized.startsWith("https://") ||
        normalized.startsWith("blob:")
}

@OptIn(ExperimentalEncodingApi::class)
public fun decodeInlineMediaSource(source: String): ByteArray? {
    val trimmed = source.trim()
    if (trimmed.startsWith("data:")) {
        val separator = trimmed.indexOf(',')
        if (separator <= 0 || !trimmed.substring(0, separator).contains(";base64")) return null
        return Base64.Default.decode(trimmed.substring(separator + 1))
    }
    return try {
        Base64.Default.decode(trimmed)
    } catch (_: IllegalArgumentException) {
        null
    }
}

public fun isLocalMediaFileSource(source: String): Boolean {
    val trimmed = source.trim()
    return trimmed.startsWith("file://") || trimmed.startsWith("/")
}
