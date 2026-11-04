package com.tencent.kuiklybase.chat.session.controller

import com.tencent.kuikly.core.base.Attr
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.event.Event

/**
 * 全屏预览图。双指缩放、拖动和点击关闭都在各端同名原生 View 里完成，
 * 行为对齐 Android：围绕双指中心缩放，范围 1 到 4 倍，放大后才能拖动。
 */
internal class ChatZoomImageView : DeclarativeBaseView<ChatZoomImageAttr, ChatZoomImageEvent>() {
    override fun createAttr(): ChatZoomImageAttr = ChatZoomImageAttr()

    override fun createEvent(): ChatZoomImageEvent = ChatZoomImageEvent()

    override fun viewName(): String = CHAT_ZOOM_IMAGE_VIEW
}

internal class ChatZoomImageAttr : Attr() {
    fun src(value: String): ChatZoomImageAttr {
        SRC with value
        return this
    }
}

internal class ChatZoomImageEvent : Event() {
    fun close(handler: () -> Unit) {
        register(CLOSE) { handler() }
    }
}

internal fun ViewContainer<*, *>.ChatZoomImage(init: ChatZoomImageView.() -> Unit) {
    addChild(ChatZoomImageView(), init)
}

internal const val CHAT_ZOOM_IMAGE_VIEW = "ChatZoomImage"
private const val SRC = "src"
private const val CLOSE = "close"
