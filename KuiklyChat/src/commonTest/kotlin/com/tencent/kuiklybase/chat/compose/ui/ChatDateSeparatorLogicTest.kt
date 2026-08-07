package com.tencent.kuiklybase.chat.compose.ui

import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatStrings
import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.core.LocalChatDateTimeFormatter
import com.tencent.kuiklybase.chat.compose.core.MessageGroupStatus
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.compose.core.MessageStatus
import com.tencent.kuiklybase.chat.compose.core.groupStatus
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChatDateSeparatorLogicTest {
    private val strings = ChatStrings.Default
    private val formatter = LocalChatDateTimeFormatter.withTimeZone("UTC")
    private val config = ChatScreenConfig.Default.copy(
        showDateSeparators = true,
        dateTimeFormatter = formatter,
    )

    @Test
    fun resolveDateSeparatorLabel_disabledWhenConfigOff() {
        val current = messageAt(2024, 6, 15, 10, 0)
        assertNull(
            resolveDateSeparatorLabel(
                previous = null,
                current = current,
                config = config.copy(showDateSeparators = false),
                strings = strings,
                nowMillis = instantUtc(2024, 6, 15, 12, 0),
            ),
        )
    }

    @Test
    fun resolveDateSeparatorLabel_showsForFirstMessage() {
        val current = messageAt(2024, 6, 15, 10, 0)
        assertEquals(
            "今天 10:00",
            resolveDateSeparatorLabel(
                previous = null,
                current = current,
                config = config,
                strings = strings,
                nowMillis = instantUtc(2024, 6, 15, 12, 0),
            ),
        )
    }

    @Test
    fun resolveDateSeparatorLabel_nullOnSameDay() {
        val previous = messageAt(2024, 6, 15, 9, 0)
        val current = messageAt(2024, 6, 15, 10, 0)
        assertNull(
            resolveDateSeparatorLabel(
                previous = previous,
                current = current,
                config = config,
                strings = strings,
                nowMillis = instantUtc(2024, 6, 15, 12, 0),
            ),
        )
    }

    @Test
    fun breaksMessageGroupingAtDateBoundary_splitsSameAuthorGroup() {
        val previous = messageAt(2024, 6, 14, 23, 59)
        val current = messageAt(2024, 6, 15, 0, 1)
        val now = instantUtc(2024, 6, 15, 12, 0)
        assertTrue(
            breaksMessageGroupingAtDateBoundary(previous, current, config, strings, now),
        )
        val group = current.groupStatus(
            previous = if (breaksMessageGroupingAtDateBoundary(previous, current, config, strings, now)) {
                null
            } else {
                previous
            },
            next = null,
            groupingWindowMillis = config.groupingWindowMillis,
        )
        assertEquals(MessageGroupStatus.SINGLE, group)
    }

    @Test
    fun breaksMessageGroupingAtDateBoundary_falseOnSameDay() {
        val previous = messageAt(2024, 6, 15, 9, 0)
        val current = messageAt(2024, 6, 15, 9, 30)
        assertFalse(
            breaksMessageGroupingAtDateBoundary(
                previous,
                current,
                config,
                strings,
                instantUtc(2024, 6, 15, 12, 0),
            ),
        )
    }

    private fun messageAt(year: Int, month: Int, day: Int, hour: Int, minute: Int): ChatMessage =
        ChatMessage(
            id = "$year$month$day$hour$minute",
            author = ChatUser(id = "u1", name = "User"),
            parts = listOf(MessagePart.Text("hi")),
            status = MessageStatus.SENT,
            createdAt = instantUtc(year, month, day, hour, minute),
        )

    private fun instantUtc(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime(year, month, day, hour, minute)
            .toInstant(TimeZone.UTC)
            .toEpochMilliseconds()
}
