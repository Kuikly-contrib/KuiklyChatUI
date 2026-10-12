package com.tencent.kuiklybase.chat.session.controller

import com.tencent.kuikly.core.directives.scrollToPosition
import com.tencent.kuiklybase.chat.compose.core.ChatIntent

internal fun ChatControllerSessionView.updateNearBottom(offsetY: Float, viewHeight: Float, contentHeight: Float) {
    if (contentHeight <= 0f) return
    val remaining = contentHeight - offsetY - viewHeight
    val near = remaining <= NEAR_BOTTOM_PX
    sessionAttr().stickToBottom = near
    if (near == sessionAttr().nearBottom) return
    sessionAttr().nearBottom = near
    sessionAttr().controller.dispatch(ChatIntent.SetListNearBottom(near))
}

internal fun ChatControllerSessionView.scrollSessionToLatest(clearUnread: Boolean) {
    val last = sessionAttr().messages.size
    if (last > 0) scrollSessionToMessageIndex(last - 1)
    sessionAttr().nearBottom = true
    sessionAttr().stickToBottom = true
    sessionAttr().controller.dispatch(ChatIntent.SetListNearBottom(true))
    if (clearUnread) sessionAttr().controller.dispatch(ChatIntent.ClearUnread)
}

internal fun ChatControllerSessionView.scrollSessionToMessageIndex(messageIndex: Int) {
    val listIndex = messageIndex + LOAD_EARLIER_HEADER
    getPager().addTaskWhenPagerUpdateLayoutFinish {
        listRef?.view?.scrollToPosition(listIndex, 0f, false)
    }
}

internal const val NEAR_BOTTOM_PX = 48f
internal const val LOAD_EARLIER_HEADER = 1
