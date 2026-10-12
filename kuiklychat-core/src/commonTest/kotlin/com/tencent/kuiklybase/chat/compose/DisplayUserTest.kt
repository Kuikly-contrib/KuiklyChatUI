package com.tencent.kuiklybase.chat.compose

import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.core.resolveDisplayUser
import kotlin.test.Test
import kotlin.test.assertEquals

class DisplayUserTest {
    @Test
    fun resolveDisplayUserPrefersCachedNameAndAvatar() {
        val embedded = ChatUser(id = "u1", name = "Embedded", avatar = "embedded://a")
        val cached = ChatUser(id = "u1", name = "Cached", avatar = "https://cdn/a.png")
        val merged = resolveDisplayUser("u1", embedded, cached)
        assertEquals("Cached", merged.name)
        assertEquals("https://cdn/a.png", merged.avatar)
    }

    @Test
    fun resolveDisplayUserFallsBackToEmbeddedWhenCachedEmpty() {
        val embedded = ChatUser(id = "u1", name = "Embedded", avatar = "embedded://a")
        val cached = ChatUser(id = "u1", name = "", avatar = "")
        val merged = resolveDisplayUser("u1", embedded, cached)
        assertEquals("Embedded", merged.name)
        assertEquals("embedded://a", merged.avatar)
    }

    @Test
    fun resolveDisplayUserMergesMetadataWithCachedOverride() {
        val embedded = ChatUser(id = "u1", metadata = mapOf("role" to "guest", "tier" to "1"))
        val cached = ChatUser(id = "u1", metadata = mapOf("role" to "admin", "extra" to "x"))
        val merged = resolveDisplayUser("u1", embedded, cached)
        assertEquals("admin", merged.metadata["role"])
        assertEquals("1", merged.metadata["tier"])
        assertEquals("x", merged.metadata["extra"])
    }
}
