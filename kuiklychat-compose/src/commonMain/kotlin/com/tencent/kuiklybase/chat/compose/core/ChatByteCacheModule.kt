package com.tencent.kuiklybase.chat.compose.core

import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * Kuikly Module bridge for persistent chat media byte storage.
 *
 * Register the Kotlin instance via [chatByteCacheExternalModules] and provide the matching
 * native implementation on each host platform (for example `KRChatByteCacheModule` on Android).
 */
public open class ChatByteCacheModule : Module() {
    private var configured = false

    override fun moduleName(): String = MODULE_NAME

    public open fun configure(namespace: String) {
        val params = JSONObject().apply {
            put("namespace", normalizeChatCacheNamespace(namespace))
        }
        syncToNativeMethod(METHOD_CONFIGURE, params, null)
        configured = true
    }

    public open fun getBytes(key: String): ByteArray? {
        ensureConfigured()
        val result = syncToNativeMethod(METHOD_GET, arrayOf<Any>(key), null)
        return result as? ByteArray
    }

    public open fun setBytes(key: String, value: ByteArray) {
        ensureConfigured()
        syncToNativeMethod(METHOD_SET, arrayOf<Any>(key, value), null)
    }

    public open fun containsKey(key: String): Boolean {
        ensureConfigured()
        return syncToNativeMethod(METHOD_CONTAINS, arrayOf<Any>(key), null) == true
    }

    public open fun deleteKey(key: String) {
        ensureConfigured()
        syncToNativeMethod(METHOD_DELETE, arrayOf<Any>(key), null)
    }

    public open fun updateKey(oldKey: String, newKey: String) {
        ensureConfigured()
        syncToNativeMethod(METHOD_UPDATE_KEY, arrayOf<Any>(oldKey, newKey), null)
    }

    public open fun clearAll() {
        ensureConfigured()
        syncToNativeMethod(METHOD_CLEAR, arrayOf<Any>(), null)
    }

    public open fun readLocalFile(source: String): ByteArray? {
        val result = syncToNativeMethod(METHOD_READ_LOCAL_FILE, arrayOf<Any>(source), null)
        return result as? ByteArray
    }

    private fun ensureConfigured() {
        check(configured) { "ChatByteCacheModule.configure(namespace) must be called first" }
    }

    public companion object {
        public const val MODULE_NAME: String = "KRChatByteCacheModule"

        public const val METHOD_CONFIGURE: String = "configure"
        public const val METHOD_GET: String = "get"
        public const val METHOD_SET: String = "set"
        public const val METHOD_CONTAINS: String = "contains"
        public const val METHOD_DELETE: String = "delete"
        public const val METHOD_UPDATE_KEY: String = "updateKey"
        public const val METHOD_CLEAR: String = "clear"
        public const val METHOD_READ_LOCAL_FILE: String = "readLocalFile"
    }
}
