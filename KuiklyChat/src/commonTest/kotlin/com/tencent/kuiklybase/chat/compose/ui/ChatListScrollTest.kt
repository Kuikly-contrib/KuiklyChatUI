package com.tencent.kuiklybase.chat.compose.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatListScrollTest {
    @Test
    fun nearBottomWhenContentFitsViewport() {
        assertTrue(isNearBottom(scrollOffset = 0, itemSize = 400, viewportHeight = 800))
    }

    @Test
    fun nearBottomWhenScrolledToEnd() {
        assertTrue(isNearBottom(scrollOffset = 600, itemSize = 1000, viewportHeight = 400))
    }

    @Test
    fun notNearBottomWhenReadingHistory() {
        assertFalse(isNearBottom(scrollOffset = 50, itemSize = 1000, viewportHeight = 400))
    }

    @Test
    fun anchorOffsetAfterPrependAddsInsertedHeight() {
        assertEquals(250, anchorOffsetAfterPrepend(previousScrollOffset = 100, insertedHeight = 150))
    }
}
