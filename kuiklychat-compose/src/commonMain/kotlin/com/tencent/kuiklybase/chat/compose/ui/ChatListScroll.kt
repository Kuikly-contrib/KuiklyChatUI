package com.tencent.kuiklybase.chat.compose.ui

import com.tencent.kuikly.compose.foundation.lazy.LazyListState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal const val CHAT_LIST_LOAD_EARLIER_KEY = "__load_earlier__"
internal const val CHAT_LIST_FOOTER_KEY = "__chat_footer__"

/** Load-earlier header occupies lazy index 0; messages start at 1. */
internal fun chatMessageLazyIndex(messageIndex: Int): Int = messageIndex + 1

public fun remainingScrollDistance(
    lastVisibleOffset: Int,
    lastVisibleSize: Int,
    viewportEndOffset: Int,
): Int = (lastVisibleOffset + lastVisibleSize - viewportEndOffset).coerceAtLeast(0)

public fun isNearBottomByRemaining(remainingPx: Int, thresholdPx: Int): Boolean =
    remainingPx <= thresholdPx.coerceAtLeast(0)

public fun isNearBottomOfLazyItems(
    lastVisibleIndex: Int,
    totalItemsCount: Int,
    lastVisibleOffset: Int = 0,
    lastVisibleSize: Int = 0,
    viewportEndOffset: Int = 0,
    thresholdPx: Int = 0,
): Boolean {
    if (totalItemsCount <= 0) return true
    if (lastVisibleIndex < totalItemsCount - 2) return false
    return isNearBottomByRemaining(
        remainingScrollDistance(lastVisibleOffset, lastVisibleSize, viewportEndOffset),
        thresholdPx,
    )
}

public fun firstVisibleMessageKey(visibleKeys: List<Any?>, messages: List<String>): String? {
    val messageIds = messages.toHashSet()
    return visibleKeys.filterIsInstance<String>().firstOrNull { it in messageIds }
}

public fun lazyIndexOfMessageKey(messages: List<String>, key: String?): Int? {
    if (key == null) return null
    val messageIndex = messages.indexOf(key)
    if (messageIndex < 0) return null
    return chatMessageLazyIndex(messageIndex)
}

/** Wraps [LazyListState] with chat-specific scroll behavior. */
public class ChatListScrollState(
    public val listState: LazyListState,
    private val scope: CoroutineScope
) {
    public fun isNearBottom(thresholdPx: Int = 0): Boolean {
        val info = listState.layoutInfo
        val lastVisible = info.visibleItemsInfo.lastOrNull() ?: return info.totalItemsCount <= 0
        return isNearBottomOfLazyItems(
            lastVisibleIndex = lastVisible.index,
            totalItemsCount = info.totalItemsCount,
            lastVisibleOffset = lastVisible.offset,
            lastVisibleSize = lastVisible.size,
            viewportEndOffset = info.viewportEndOffset,
            thresholdPx = thresholdPx,
        )
    }

    public fun scrollToBottom(force: Boolean = false) {
        if (!force && !isNearBottom()) return
        scope.launch {
            val lastIndex = (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
            listState.scrollToItem(lastIndex)
        }
    }
}
