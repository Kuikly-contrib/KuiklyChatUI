package com.tencent.kuiklybase.chat.compose.core

import com.tencent.kuikly.core.pager.IPager

/**
 * [ChatByteCache] backed by [ChatByteCacheModule].
 *
 * The module instance is owned by the pager; [close] does not tear down native storage.
 */
class ModuleChatByteCache(
    private val module: ChatByteCacheModule,
) : ChatByteCache {
    override suspend fun get(key: String): ByteArray? = module.getBytes(key)?.copyOf()

    override suspend fun set(key: String, value: ByteArray) {
        module.setBytes(key, value.copyOf())
    }

    override suspend fun contains(key: String): Boolean = module.containsKey(key)

    override suspend fun delete(key: String) {
        module.deleteKey(key)
    }

    override suspend fun updateKey(key: String, newKey: String) {
        module.updateKey(key, newKey)
    }

    override suspend fun clear() {
        module.clearAll()
    }
}

/**
 * Creates a [ChatByteCache] backed by the registered [ChatByteCacheModule].
 *
 * Requires both Kotlin-side `chatByteCacheExternalModules()` and the matching native
 * `KRChatByteCacheModule` on the host.
 */
fun createChatByteCache(
    namespace: String,
    pager: IPager,
): ChatByteCache {
    val module = pager.getModule<ChatByteCacheModule>(ChatByteCacheModule.MODULE_NAME)
        ?: error(
            "ChatByteCacheModule is not registered. Merge chatByteCacheExternalModules() on the " +
                "Kotlin side and register KRChatByteCacheModule via KuiklyChatAndroid / " +
                "KuiklyChatIOS / KuiklyChatOhos.",
        )
    module.configure(namespace)
    return ModuleChatByteCache(module)
}
