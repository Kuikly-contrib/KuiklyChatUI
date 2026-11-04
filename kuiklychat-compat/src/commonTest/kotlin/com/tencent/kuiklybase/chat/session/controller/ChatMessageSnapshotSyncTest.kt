package com.tencent.kuiklybase.chat.session.controller

import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.compose.core.MessageStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatMessageSnapshotSyncTest {

    @Test
    fun replaceSameIdKeepsPosition() {
        val target = mutableListOf(message("a", "hi"), message("b", "old"))
        val changed = applyMessageSnapshot(
            target,
            listOf(message("a", "hi"), message("b", "new", MessageStatus.STREAMING)),
        )
        assertTrue(changed)
        assertEquals(listOf("a", "b"), target.map { it.id })
        assertEquals("new", target[1].text)
        assertEquals(MessageStatus.STREAMING, target[1].status)
    }

    @Test
    fun prependsHistoryWithoutReorderingExistingIds() {
        val target = mutableListOf(message("b", "now"))
        val changed = applyMessageSnapshot(
            target,
            listOf(message("a", "earlier"), message("b", "now")),
        )
        assertTrue(changed)
        assertEquals(listOf("a", "b"), target.map { it.id })
    }

    @Test
    fun removesMissingIds() {
        val target = mutableListOf(message("a", "1"), message("b", "2"), message("c", "3"))
        val changed = applyMessageSnapshot(target, listOf(message("c", "3")))
        assertTrue(changed)
        assertEquals(listOf("c"), target.map { it.id })
    }

    @Test
    fun unchangedSnapshotReportsNoChange() {
        val existing = message("a", "hi")
        val target = mutableListOf(existing)
        val changed = applyMessageSnapshot(target, listOf(existing))
        assertFalse(changed)
        assertEquals(1, target.size)
    }

    private fun message(
        id: String,
        text: String,
        status: MessageStatus = MessageStatus.SENT,
    ): ChatMessage = ChatMessage(
        id = id,
        author = ChatUser(id = "u", name = "U"),
        parts = listOf(MessagePart.Text(text)),
        status = status,
    )
}
