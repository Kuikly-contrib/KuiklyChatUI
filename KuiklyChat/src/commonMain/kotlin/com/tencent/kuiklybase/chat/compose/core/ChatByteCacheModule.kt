package com.tencent.kuiklybase.chat.compose.core

import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * Kuikly Module bridge for persistent chat media byte storage.
 *
 * Register the Kotlin instance via [chatByteCacheExternalModules] and provide the matching
 * native implementation on each host platform (for example `KRChatByteCacheModule` on Android).
 */
open class ChatByteCacheModule : Module() {
    private var configured = false

    override fun moduleName(): String = MODULE_NAME

    open fun configure(namespace: String) {
        val params = JSONObject().apply {
            put("namespace", normalizeChatCacheNamespace(namespace))
        }
        syncToNativeMethod(METHOD_CONFIGURE, params, null)
        configured = true
    }

    open fun getBytes(key: String): ByteArray? {
        ensureConfigured()
        val result = syncToNativeMethod(METHOD_GET, arrayOf<Any>(key), null)
        return result as? ByteArray
    }

    open fun setBytes(key: String, value: ByteArray) {
        ensureConfigured()
        syncToNativeMethod(METHOD_SET, arrayOf<Any>(key, value), null)
    }

    open fun containsKey(key: String): Boolean {
        ensureConfigured()
        return syncToNativeMethod(METHOD_CONTAINS, arrayOf<Any>(key), null) == true
    }

    open fun deleteKey(key: String) {
        ensureConfigured()
        syncToNativeMethod(METHOD_DELETE, arrayOf<Any>(key), null)
    }

    open fun updateKey(oldKey: String, newKey: String) {
        ensureConfigured()
        syncToNativeMethod(METHOD_UPDATE_KEY, arrayOf<Any>(oldKey, newKey), null)
    }

    open fun clearAll() {
        ensureConfigured()
        syncToNativeMethod(METHOD_CLEAR, arrayOf<Any>(), null)
    }

    open fun readLocalFile(source: String): ByteArray? {
        val result = syncToNativeMethod(METHOD_READ_LOCAL_FILE, arrayOf<Any>(source), null)
        return result as? ByteArray
    }

    private fun ensureConfigured() {
        check(configured) { "ChatByteCacheModule.configure(namespace) must be called first" }
    }

    companion object {
        const val MODULE_NAME = "KRChatByteCacheModule"

        const val METHOD_CONFIGURE = "configure"
        const val METHOD_GET = "get"
        const val METHOD_SET = "set"
        const val METHOD_CONTAINS = "contains"
        const val METHOD_DELETE = "delete"
        const val METHOD_UPDATE_KEY = "updateKey"
        const val METHOD_CLEAR = "clear"
        const val METHOD_READ_LOCAL_FILE = "readLocalFile"
    }
}
