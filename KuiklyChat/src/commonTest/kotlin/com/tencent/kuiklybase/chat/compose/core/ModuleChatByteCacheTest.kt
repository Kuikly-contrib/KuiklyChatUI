package com.tencent.kuiklybase.chat.compose.core

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ModuleChatByteCacheTest {
    @Test
    fun externalModulesExposeByteCacheModule() {
        val modules = chatByteCacheExternalModules()
        assertEquals(ChatByteCacheModule.MODULE_NAME, modules.keys.single())
        assertTrue(modules.values.single() is ChatByteCacheModule)
    }

    @Test
    fun moduleBackedStoreDelegatesToConfiguredBridge() = runTest {
        val bridge = RecordingChatByteCacheModule()
        val store = ModuleChatByteCache(bridge)
        bridge.configure("tenant_media")

        store.set("avatar", byteArrayOf(1, 2))
        assertTrue(store.contains("avatar"))
        assertContentEquals(byteArrayOf(1, 2), store.get("avatar"))

        store.updateKey("avatar", "avatar-v2")
        assertFalse(store.contains("avatar"))
        assertContentEquals(byteArrayOf(1, 2), store.get("avatar-v2"))

        store.clear()
        assertFalse(store.contains("avatar-v2"))
        assertEquals("tenant_media", bridge.configuredNamespace)
    }

    private class RecordingChatByteCacheModule : ChatByteCacheModule() {
        private val entries = mutableMapOf<String, ByteArray>()
        var configuredNamespace: String? = null
            private set

        override fun configure(namespace: String) {
            configuredNamespace = namespace
        }

        override fun getBytes(key: String): ByteArray? = entries[key]?.copyOf()

        override fun setBytes(key: String, value: ByteArray) {
            entries[key] = value.copyOf()
        }

        override fun containsKey(key: String): Boolean = entries.containsKey(key)

        override fun deleteKey(key: String) {
            entries.remove(key)
        }

        override fun updateKey(oldKey: String, newKey: String) {
            val value = entries.remove(oldKey) ?: throw ChatCacheMissException(oldKey)
            entries[newKey] = value
        }

        override fun clearAll() {
            entries.clear()
        }
    }
}
