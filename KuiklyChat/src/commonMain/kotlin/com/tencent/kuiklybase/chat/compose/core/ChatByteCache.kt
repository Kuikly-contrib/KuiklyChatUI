package com.tencent.kuiklybase.chat.compose.core

const val DEFAULT_CHAT_MEDIA_CACHE_NAMESPACE = "kuikly_chat_media"

/** Persistent or in-memory byte storage used by [ChatCrossCache]. */
interface ChatByteCache {
    suspend fun get(key: String): ByteArray?

    suspend fun set(key: String, value: ByteArray)

    suspend fun contains(key: String): Boolean = get(key) != null

    suspend fun delete(key: String)

    suspend fun updateKey(key: String, newKey: String)

    suspend fun clear()

    fun close() = Unit
}

class ChatCacheMissException(key: String) :
    Exception("No cached media for key hash ${sha256Hex(key).take(12)}")

/** Small deterministic cache useful for tests, previews, and hosts that do not want persistence. */
class MemoryChatByteCache : ChatByteCache {
    private val entries = mutableMapOf<String, ByteArray>()
    private var closed = false

    override suspend fun get(key: String): ByteArray? {
        if (closed) return null
        return entries[key]?.copyOf()
    }

    override suspend fun set(key: String, value: ByteArray) {
        check(!closed) { "Chat byte cache is closed" }
        entries[key] = value.copyOf()
    }

    override suspend fun contains(key: String): Boolean = !closed && entries.containsKey(key)

    override suspend fun delete(key: String) {
        if (!closed) entries.remove(key)
    }

    override suspend fun updateKey(key: String, newKey: String) {
        check(!closed) { "Chat byte cache is closed" }
        val value = entries.remove(key) ?: throw ChatCacheMissException(key)
        entries[newKey] = value
    }

    override suspend fun clear() {
        if (!closed) entries.clear()
    }

    override     fun close() {
        if (closed) return
        closed = true
        entries.clear()
    }
}

internal fun normalizeChatCacheNamespace(namespace: String): String {
    val normalized = namespace.trim()
        .map { character ->
            if (character.isLetterOrDigit() || character == '-' || character == '_' || character == '.') {
                character
            } else {
                '_'
            }
        }
        .joinToString("")
        .trim('.')
    return normalized.ifEmpty { DEFAULT_CHAT_MEDIA_CACHE_NAMESPACE }
}
