package com.tencent.kuiklybase.chat.compose.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChatListScrollTest {
    @Test
    fun nearBottomWhenListIsEmpty() {
        assertTrue(isNearBottomOfLazyItems(lastVisibleIndex = 0, totalItemsCount = 0))
    }

    @Test
    fun nearBottomWhenLastItemsVisible() {
        assertTrue(
            isNearBottomOfLazyItems(
                lastVisibleIndex = 9,
                totalItemsCount = 10,
                lastVisibleOffset = 700,
                lastVisibleSize = 80,
                viewportEndOffset = 780,
            )
        )
    }

    @Test
    fun notNearBottomWhenReadingHistory() {
        assertFalse(
            isNearBottomOfLazyItems(
                lastVisibleIndex = 2,
                totalItemsCount = 20,
            )
        )
    }

    @Test
    fun remainingDistanceUsesViewportEnd() {
        assertEquals(40, remainingScrollDistance(700, 120, 780))
        assertTrue(isNearBottomByRemaining(40, 48))
        assertFalse(isNearBottomByRemaining(80, 48))
    }

    @Test
    fun messageKeyMapsToLazyIndexAfterHeader() {
        assertEquals(3, lazyIndexOfMessageKey(listOf("a", "b", "c"), "c"))
        assertNull(lazyIndexOfMessageKey(listOf("a", "b"), "missing"))
        assertEquals("b", firstVisibleMessageKey(listOf(CHAT_LIST_LOAD_EARLIER_KEY, "b"), listOf("a", "b")))
    }
}
