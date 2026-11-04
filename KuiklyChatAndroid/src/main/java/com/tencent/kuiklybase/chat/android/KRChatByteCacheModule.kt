package com.tencent.kuiklybase.chat.android

import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import java.io.File
import org.json.JSONObject

/**
 * Android native implementation for Kuikly [CHAT_BYTE_CACHE_MODULE_NAME].
 *
 * Register in the host `registerExternalModule` block with [MODULE_NAME].
 */
class KRChatByteCacheModule : KuiklyRenderBaseModule() {
    private var store: AndroidChatFileByteStore? = null

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            METHOD_CONFIGURE -> configure(params)
            else -> super.call(method, params, callback)
        }
    }

    override fun call(method: String, params: Any?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            METHOD_CONFIGURE -> configure(extractConfigureParams(params))
            METHOD_READ_LOCAL_FILE -> {
                val path = (params as? Array<*>)?.getOrNull(0) as? String
                    ?: throw IllegalArgumentException("File path is required")
                readLocalFile(path)
            }
            else -> {
                val activeStore = store
                    ?: throw IllegalStateException("Chat byte cache is not configured")
                when (method) {
                    METHOD_GET -> {
                        val key = (params as? Array<*>)?.getOrNull(0) as? String
                            ?: throw IllegalArgumentException("Cache key is required")
                        activeStore.get(key)
                    }

                    METHOD_SET -> {
                        val args = params as? Array<*>
                            ?: throw IllegalArgumentException("Cache key and value are required")
                        val key = args.getOrNull(0) as? String
                            ?: throw IllegalArgumentException("Cache key is required")
                        val value = args.getOrNull(1) as? ByteArray
                            ?: throw IllegalArgumentException("Cache value is required")
                        activeStore.set(key, value)
                        null
                    }

                    METHOD_CONTAINS -> {
                        val key = (params as? Array<*>)?.getOrNull(0) as? String
                            ?: throw IllegalArgumentException("Cache key is required")
                        activeStore.contains(key)
                    }

                    METHOD_DELETE -> {
                        val key = (params as? Array<*>)?.getOrNull(0) as? String
                            ?: throw IllegalArgumentException("Cache key is required")
                        activeStore.delete(key)
                        null
                    }

                    METHOD_UPDATE_KEY -> {
                        val args = params as? Array<*>
                            ?: throw IllegalArgumentException("Cache keys are required")
                        val oldKey = args.getOrNull(0) as? String
                            ?: throw IllegalArgumentException("Old cache key is required")
                        val newKey = args.getOrNull(1) as? String
                            ?: throw IllegalArgumentException("New cache key is required")
                        activeStore.updateKey(oldKey, newKey)
                        null
                    }

                    METHOD_CLEAR -> {
                        activeStore.clear()
                        null
                    }

                    else -> super.call(method, params, callback)
                }
            }
        }
    }

    private fun configure(params: String?): Any? {
        val namespace = params?.let(::JSONObject)
            ?.optString("namespace")
            ?.takeIf(String::isNotBlank)
            ?: throw IllegalArgumentException("Cache namespace is required")
        val cacheRoot = context?.cacheDir
            ?: File(System.getProperty("java.io.tmpdir") ?: error("Cache directory is unavailable"))
        store = AndroidChatFileByteStore(namespace, cacheRoot)
        return null
    }

    private fun extractConfigureParams(params: Any?): String? {
        return when (params) {
            null -> null
            is String -> params
            is JSONObject -> params.toString()
            is Map<*, *> -> JSONObject(params).toString()
            else -> params.toString()
        }
    }

    private fun readLocalFile(source: String): ByteArray? {
        val path = source.trim().removePrefix("file://")
        val file = File(path)
        return file.takeIf(File::isFile)?.readBytes()
    }

    companion object {
        const val MODULE_NAME = CHAT_BYTE_CACHE_MODULE_NAME
    }
}
