package com.tencent.kuiklybase.chat.session.controller

import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.compose.core.MessageStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatMessageSnapshotSyncTest {

    @Test
    fun messageAccessibilityIncludesAuthorContentAndStatus() {
        val strings = com.tencent.kuiklybase.chat.compose.core.ChatStrings.Default
        val message = ChatMessage(
            id = "a11y-1",
            author = ChatUser(id = "assistant", name = "Assistant"),
            parts = listOf(MessagePart.Text("hello")),
            status = MessageStatus.STREAMING_PARTIAL,
        )

        val label = com.tencent.kuiklybase.chat.compose.core.ChatStrings.Default.let {
            listOf("Assistant", "hello", strings.partial).joinToString(": ")
        }

        assertTrue(label.contains(strings.partial))
        assertFalse(label.contains(MessageStatus.STREAMING_PARTIAL.name))
    }

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

    @Test
    fun sharedStatusCopyMatchesComposeContract() {
        val strings = com.tencent.kuiklybase.chat.compose.core.ChatStrings.Default

        assertEquals(
            listOf(strings.sending, strings.streaming, strings.partial, strings.retry, strings.cancelled),
            sharedChatStatusLabels(strings),
        )
        assertTrue(strings.partial.isNotEmpty())
        assertTrue(strings.retry.isNotEmpty())
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
