package com.tencent.kuiklybase.chat.android

import java.io.File
import java.security.MessageDigest

internal const val CHAT_BYTE_CACHE_MODULE_NAME = "KRChatByteCacheModule"

internal const val METHOD_CONFIGURE = "configure"
internal const val METHOD_GET = "get"
internal const val METHOD_SET = "set"
internal const val METHOD_CONTAINS = "contains"
internal const val METHOD_DELETE = "delete"
internal const val METHOD_UPDATE_KEY = "updateKey"
internal const val METHOD_CLEAR = "clear"
internal const val METHOD_READ_LOCAL_FILE = "readLocalFile"

internal class ChatCacheMissException(key: String) :
    Exception("No cached media for key hash ${sha256Hex(key).take(12)}")

internal class AndroidChatFileByteStore(
    namespace: String,
    cacheRoot: File,
) {
    private val directory = File(cacheRoot, normalizeChatCacheNamespace(namespace))

    fun get(key: String): ByteArray? =
        cacheFile(key).takeIf(File::isFile)?.readBytes()

    fun set(key: String, value: ByteArray) {
        ensureDirectory()
        val target = cacheFile(key)
        val temporary = File.createTempFile("${target.name}.", ".tmp", directory)
        try {
            temporary.writeBytes(value)
            if (target.exists() && !target.delete()) {
                throw IllegalStateException("Unable to replace cached media")
            }
            if (!temporary.renameTo(target)) {
                temporary.copyTo(target, overwrite = true)
            }
        } finally {
            temporary.delete()
        }
    }

    fun contains(key: String): Boolean = cacheFile(key).isFile

    fun delete(key: String) {
        val file = cacheFile(key)
        if (file.exists() && !file.delete()) {
            throw IllegalStateException("Unable to delete cached media")
        }
    }

    fun updateKey(key: String, newKey: String) {
        ensureDirectory()
        val source = cacheFile(key)
        if (!source.isFile) throw ChatCacheMissException(key)
        val destination = cacheFile(newKey)
        if (destination.exists() && !destination.delete()) {
            throw IllegalStateException("Unable to replace cached media")
        }
        if (!source.renameTo(destination)) {
            source.copyTo(destination, overwrite = true)
            if (!source.delete()) throw IllegalStateException("Unable to remove old cache key")
        }
    }

    fun clear() {
        directory.listFiles()?.forEach { file ->
            if (file.isFile && !file.delete()) {
                throw IllegalStateException("Unable to clear cached media")
            }
        }
    }

    private fun cacheFile(key: String): File = File(directory, sha256Hex(key))

    private fun ensureDirectory() {
        if (!directory.isDirectory && !directory.mkdirs()) {
            throw IllegalStateException("Unable to create media cache directory")
        }
    }
}

private fun normalizeChatCacheNamespace(namespace: String): String {
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
    return normalized.ifEmpty { "kuikly_chat_media" }
}

private fun sha256Hex(value: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(value.toByteArray(Charsets.UTF_8))
    return hash.joinToString("") { byte -> "%02x".format(byte) }
}
