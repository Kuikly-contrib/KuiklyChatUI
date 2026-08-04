package com.tencent.kuiklybase.chat.compose.ui

import com.tencent.kuikly.compose.foundation.lazy.LazyListState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Pure scroll heuristics for sticky-bottom and history anchor correction. */
fun isNearBottom(scrollOffset: Int, itemSize: Int, viewportHeight: Int): Boolean {
    val maxOffset = (itemSize - viewportHeight).coerceAtLeast(0)
    return maxOffset == 0 || scrollOffset >= maxOffset - viewportHeight / 4
}

fun anchorOffsetAfterPrepend(previousScrollOffset: Int, insertedHeight: Int): Int =
    previousScrollOffset + insertedHeight

/** Wraps [LazyListState] with chat-specific scroll behavior. */
class ChatListScrollState(
    val listState: LazyListState,
    private val scope: CoroutineScope
) {
    fun isNearBottom(): Boolean {
        val info = listState.layoutInfo
        val itemSize = info.visibleItemsInfo.firstOrNull()?.size ?: 0
        val viewport = info.viewportSize.height
        return isNearBottom(listState.firstVisibleItemScrollOffset, itemSize, viewport)
    }

    fun scrollToBottom(force: Boolean = false) {
        if (!force && !isNearBottom()) return
        scope.launch {
            listState.scrollToItem(0, Int.MAX_VALUE / 2)
        }
    }
}
