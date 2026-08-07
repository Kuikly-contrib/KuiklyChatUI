package com.tencent.kuiklybase.chat.compose.ui

import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuiklybase.chat.compose.core.ChatStrings
import com.tencent.kuiklybase.chat.compose.core.MessageGroupStatus
import com.tencent.kuiklybase.chat.compose.core.MessageStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MessageRowLogicTest {
    @Test
    fun shouldShowAvatarWhenGroupedHiddenForMiddleAndLast() {
        assertTrue(shouldShowAvatar(MessageGroupStatus.SINGLE, showAvatar = true, hideAvatarWhenGrouped = true))
        assertTrue(shouldShowAvatar(MessageGroupStatus.FIRST, showAvatar = true, hideAvatarWhenGrouped = true))
        assertFalse(shouldShowAvatar(MessageGroupStatus.MIDDLE, showAvatar = true, hideAvatarWhenGrouped = true))
        assertFalse(shouldShowAvatar(MessageGroupStatus.LAST, showAvatar = true, hideAvatarWhenGrouped = true))
    }

    @Test
    fun shouldReserveAvatarSpaceWhenGroupedAvatarHidden() {
        assertFalse(
            shouldReserveAvatarSpace(
                MessageGroupStatus.SINGLE,
                showAvatar = true,
                hideAvatarWhenGrouped = true,
            ),
        )
        assertFalse(
            shouldReserveAvatarSpace(
                MessageGroupStatus.FIRST,
                showAvatar = true,
                hideAvatarWhenGrouped = true,
            ),
        )
        assertTrue(
            shouldReserveAvatarSpace(
                MessageGroupStatus.MIDDLE,
                showAvatar = true,
                hideAvatarWhenGrouped = true,
            ),
        )
        assertTrue(
            shouldReserveAvatarSpace(
                MessageGroupStatus.LAST,
                showAvatar = true,
                hideAvatarWhenGrouped = true,
            ),
        )
        assertFalse(
            shouldReserveAvatarSpace(
                MessageGroupStatus.MIDDLE,
                showAvatar = true,
                hideAvatarWhenGrouped = false,
            ),
        )
    }

    @Test
    fun shouldShowAvatarWhenGroupingHideDisabled() {
        assertTrue(shouldShowAvatar(MessageGroupStatus.MIDDLE, showAvatar = true, hideAvatarWhenGrouped = false))
    }

    @Test
    fun formatMessageTimeReturnsEmptyForZeroTimestamp() {
        assertEquals("", formatMessageTime(0L))
    }

    @Test
    fun formatMessageTimeUsesHourMinute() {
        val timestamp = (14L * 3600000) + (30L * 60000)
        assertEquals("14:30", formatMessageTime(timestamp))
    }

    @Test
    fun statusLabelMapsActiveStates() {
        val strings = ChatStrings.Default
        assertEquals(strings.sending, statusLabel(MessageStatus.SENDING, strings))
        assertEquals(strings.streaming, statusLabel(MessageStatus.STREAMING, strings))
        assertEquals(strings.cancelled, statusLabel(MessageStatus.CANCELLED, strings))
        assertEquals(null, statusLabel(MessageStatus.SENT, strings))
    }

    @Test
    fun bubbleCornerRadiiForIncomingGroup() {
        val large = 18.dp
        val small = 4.dp
        val single = bubbleCornerRadii(MessageGroupStatus.SINGLE, isMine = false, large, small)
        assertEquals(large, single.topStart)
        assertEquals(large, single.bottomStart)

        val first = bubbleCornerRadii(MessageGroupStatus.FIRST, isMine = false, large, small)
        assertEquals(large, first.topStart)
        assertEquals(small, first.bottomStart)

        val middle = bubbleCornerRadii(MessageGroupStatus.MIDDLE, isMine = false, large, small)
        assertEquals(small, middle.topStart)
        assertEquals(small, middle.bottomStart)

        val last = bubbleCornerRadii(MessageGroupStatus.LAST, isMine = false, large, small)
        assertEquals(small, last.topStart)
        assertEquals(large, last.bottomStart)
    }

    @Test
    fun bubbleCornerRadiiForOutgoingGroup() {
        val large = 18.dp
        val small = 4.dp
        val first = bubbleCornerRadii(MessageGroupStatus.FIRST, isMine = true, large, small)
        assertEquals(small, first.bottomEnd)

        val middle = bubbleCornerRadii(MessageGroupStatus.MIDDLE, isMine = true, large, small)
        assertEquals(small, middle.topEnd)
        assertEquals(small, middle.bottomEnd)

        val last = bubbleCornerRadii(MessageGroupStatus.LAST, isMine = true, large, small)
        assertEquals(small, last.topEnd)
        assertEquals(large, last.bottomEnd)
    }

    @Test
    fun messageRowTopPaddingUsesGroupSpacing() {
        assertEquals(8.dp, messageRowTopPadding(MessageGroupStatus.SINGLE))
        assertEquals(2.dp, messageRowTopPadding(MessageGroupStatus.MIDDLE))
    }
}
