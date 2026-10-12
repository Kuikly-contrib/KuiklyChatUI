package com.tencent.kuiklybase.chat.compose.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChatConversationDirectoryTest {
    @Test
    fun openSavesTheLeavingConversationAndBumpsGeneration() {
        val directory = ChatConversationDirectory(MemoryChatConversationStore(), clock = ChatClock { 10L })
        val first = listOf(message("u1", "hello", outgoing = true))
        directory.seed(first)
        val next = directory.startNew(first)
        assertTrue(next.isEmpty())
        assertEquals(1, directory.generation)
        assertEquals(listOf("hello"), directory.conversations.map { it.title })

        val returned = directory.open(directory.conversations.single().id, emptyList())
        assertEquals("hello", returned.single().text)
        assertEquals(2, directory.generation)
    }

    @Test
    fun deleteActiveFallsBackToTheNextConversation() {
        val directory = ChatConversationDirectory(MemoryChatConversationStore(), clock = ChatClock { 10L })
        directory.seed(listOf(message("u1", "one", outgoing = true)))
        directory.startNew(listOf(message("u1", "one", outgoing = true)))
        directory.remember(listOf(message("u2", "two", outgoing = true)))
        val active = directory.activeConversationId
        val shown = directory.delete(active, listOf(message("u2", "two", outgoing = true)))
        assertEquals("one", shown.single().text)
        assertEquals(1, directory.conversations.size)
    }

    @Test
    fun freezeCancelsAnInFlightReply() {
        val frozen = freezeConversationMessages(
            listOf(message("a", "partial", status = MessageStatus.STREAMING)),
        )
        assertEquals(MessageStatus.CANCELLED, frozen.single().status)
        assertEquals("partial", frozen.single().text)
    }

    private fun message(
        id: String,
        text: String,
        outgoing: Boolean = false,
        status: MessageStatus = MessageStatus.SENT,
    ): ChatMessage = ChatMessage(
        id = id,
        author = ChatUser(id = if (outgoing) "me" else "assistant", name = ""),
        parts = listOf(MessagePart.Text(text)),
        status = status,
        outgoing = outgoing,
    )
}
