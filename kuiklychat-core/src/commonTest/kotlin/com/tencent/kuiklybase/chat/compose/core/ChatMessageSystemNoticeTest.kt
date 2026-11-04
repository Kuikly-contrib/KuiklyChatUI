package com.tencent.kuiklybase.chat.compose.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChatMessageSystemNoticeTest {
    @Test
    fun systemNoticeOrNullOnlyForSingleNoticePart() {
        val pure = ChatMessage(
            id = "s1",
            author = SYSTEM_CHAT_USER,
            parts = listOf(MessagePart.SystemNotice("joined")),
        )
        assertEquals("joined", pure.systemNoticeOrNull?.text)

        val mixed = ChatMessage(
            id = "m1",
            author = ChatUser("user"),
            parts = listOf(
                MessagePart.SystemNotice("notice"),
                MessagePart.Text("body"),
            ),
        )
        assertNull(mixed.systemNoticeOrNull)
        assertTrue(mixed.containsSystemNotice)
    }

    @Test
    fun systemNoticeBreaksGrouping() {
        val user = ChatUser("user", "You")
        val first = ChatMessage(
            id = "1",
            author = user,
            parts = listOf(MessagePart.Text("one")),
            createdAt = 1L,
        )
        val notice = ChatMessage(
            id = "sys",
            author = SYSTEM_CHAT_USER,
            parts = listOf(MessagePart.SystemNotice("Alice joined")),
            createdAt = 2L,
        )
        val second = ChatMessage(
            id = "2",
            author = user,
            parts = listOf(MessagePart.Text("two")),
            createdAt = 3L,
        )

        assertEquals(
            MessageGroupStatus.SINGLE,
            first.groupStatus(previous = null, next = notice),
        )
        assertEquals(
            MessageGroupStatus.SINGLE,
            notice.groupStatus(previous = first, next = second),
        )
        assertEquals(
            MessageGroupStatus.SINGLE,
            second.groupStatus(previous = notice, next = null),
        )
    }

    @Test
    fun countsAsUnreadSkipsSystemNotice() {
        val notice = ChatMessage(
            id = "s1",
            author = SYSTEM_CHAT_USER,
            parts = listOf(MessagePart.SystemNotice("tip")),
        )
        val text = ChatMessage(
            id = "t1",
            author = ChatUser("assistant"),
            parts = listOf(MessagePart.Text("hi")),
        )
        assertFalse(notice.countsAsUnread())
        assertTrue(text.countsAsUnread())
    }
}
