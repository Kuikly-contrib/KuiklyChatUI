package com.tencent.kuiklybase.chat.compose.core

import kotlinx.coroutines.flow.Flow

/** Identifies why an image is being loaded without leaking UI implementation types. */
sealed interface ChatMediaContext {
    val source: String

    data class Avatar(
        val user: ChatUser,
        override val source: String = user.avatar.trim(),
    ) : ChatMediaContext

    data class AttachmentPreview(
        val message: ChatMessage,
        val attachment: MessagePart.Attachment,
        override val source: String,
    ) : ChatMediaContext
}

/**
 * A host-resolved media request.
 *
 * [cacheKey] is intentionally independent from [headers]. Credentials must not be embedded in cache
 * keys or logs; hosts should add a stable account/tenant namespace when the same URL can resolve to
 * different bytes.
 */
data class ChatMediaRequest(
    val source: String,
    val cacheKey: String = source,
    val headers: Map<String, String> = emptyMap(),
    val mimeType: String? = null,
) {
    override fun toString(): String {
        return "ChatMediaRequest(source=<redacted>, cacheKey=<redacted>, " +
            "headerNames=${headers.keys}, mimeType=$mimeType)"
    }
}

/** Events emitted while a [ChatMediaLoader] resolves a source that the native image view can show. */
sealed interface ChatMediaLoadEvent {
    data class Progress(
        val receivedBytes: Long,
        val totalBytes: Long? = null,
    ) : ChatMediaLoadEvent {
        init {
            require(receivedBytes >= 0L)
            require(totalBytes == null || totalBytes >= 0L)
        }
    }

    data class Ready(
        val displaySource: String,
    ) : ChatMediaLoadEvent

    data class Failed(
        val message: String = "Media load failed",
    ) : ChatMediaLoadEvent
}

/**
 * Root-injectable media loading boundary for avatars and attachment previews.
 *
 * Implementations may authenticate, decrypt, sign, deduplicate, or cache a request, then emit a
 * URL, data URI, or local URI in [ChatMediaLoadEvent.Ready]. Collection cancellation should cancel
 * unnecessary work. [close] must be idempotent.
 */
interface ChatMediaLoader {
    fun load(request: ChatMediaRequest): Flow<ChatMediaLoadEvent>

    fun close() = Unit
}
