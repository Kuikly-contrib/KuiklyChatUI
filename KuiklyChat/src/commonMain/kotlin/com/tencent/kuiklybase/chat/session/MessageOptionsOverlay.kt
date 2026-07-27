package com.tencent.kuiklybase.chat.session

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.views.*
import com.tencent.kuiklybase.chat.bubble.*
import com.tencent.kuiklybase.chat.model.*

/**
 * 内部 ComposeView，用于管理消息操作菜单的响应式状态。
 * ChatSessionConfig 是普通类，不支持 by observable，
 * 而 vif 需要 observable 属性才能响应变化。
 * 此组件将菜单的显示/隐藏状态放在 ComposeAttr 中，
 * 通过 observable 实现响应式更新。
 */
internal class MessageOptionsOverlayView : ComposeView<MessageOptionsOverlayAttr, ComposeEvent>() {
    override fun createAttr(): MessageOptionsOverlayAttr = MessageOptionsOverlayAttr()
    override fun createEvent(): ComposeEvent = ComposeEvent()

    /** 公开 attr 访问器，因为 ComposeView.attr 是 protected 的 */
    val overlayAttr: MessageOptionsOverlayAttr get() = attr

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            vif({ ctx.attr.isVisible }) {
                View {
                    attr {
                        positionAbsolute()
                        top(0f)
                        left(0f)
                        right(0f)
                        bottom(0f)
                    }
                    ChatMessageOptions {
                        attr {
                            message = ctx.attr.targetMessage
                            actions = ctx.attr.menuActions
                            targetX = ctx.attr.targetX
                            targetY = ctx.attr.targetY
                            targetWidth = ctx.attr.targetW
                            targetHeight = ctx.attr.targetH
                            screenWidth = ctx.pagerData.pageViewWidth
                            screenHeight = ctx.pagerData.pageViewHeight
                            // 传递主题色
                            menuBgColor = ctx.attr.menuBgColor
                            textColor = ctx.attr.textColor
                            destructiveColor = ctx.attr.destructiveColor
                            dividerColor = ctx.attr.dividerColor
                            // 气泡样式
                            bubblePrimaryColor = ctx.attr.bubblePrimaryColor
                            bubblePrimaryGradientEndColor = ctx.attr.bubblePrimaryGradientEndColor
                            bubbleOtherBubbleColor = ctx.attr.bubbleOtherBubbleColor
                            bubbleOtherTextColor = ctx.attr.bubbleOtherTextColor
                            bubbleSelfTextColor = ctx.attr.bubbleSelfTextColor
                            bubblePaddingH = ctx.attr.bubblePaddingH
                            bubblePaddingV = ctx.attr.bubblePaddingV
                            bubbleFontSize = ctx.attr.bubbleFontSize
                            bubbleLineHeight = ctx.attr.bubbleLineHeight
                        }
                        event {
                            onActionClick = { action ->
                                ctx.attr.onActionClickCallback?.invoke(action)
                            }
                            onReactionSelect = { emoji ->
                                ctx.attr.onReactionSelectCallback?.invoke(emoji)
                            }
                            onDismiss = {
                                ctx.attr.isVisible = false
                                ctx.attr.onDismissCallback?.invoke()
                            }
                        }
                    }
                }
            }
        }
    }
}

internal class MessageOptionsOverlayAttr : ComposeAttr() {
    var isVisible: Boolean by observable(false)
    var targetMessage: ChatMessage? by observable(null)
    var menuActions: List<MessageAction> by observable(emptyList())
    var targetX: Float by observable(0f)
    var targetY: Float by observable(0f)
    var targetW: Float by observable(0f)
    var targetH: Float by observable(0f)
    // 主题色
    var menuBgColor: Long by observable(0xF2FFFFFF)
    var textColor: Long by observable(0xFF1A1A1A)
    var destructiveColor: Long by observable(0xFFFF3B30)
    var dividerColor: Long by observable(0x1A000000)
    // 气泡样式
    var bubblePrimaryColor: Long by observable(0xFF4F8FFF)
    var bubblePrimaryGradientEndColor: Long by observable(0xFF6C5CE7)
    var bubbleOtherBubbleColor: Long by observable(0xFFFFFFFF)
    var bubbleOtherTextColor: Long by observable(0xFF333333)
    var bubbleSelfTextColor: Long by observable(0xFFFFFFFF)
    var bubblePaddingH: Float by observable(12f)
    var bubblePaddingV: Float by observable(10f)
    var bubbleFontSize: Float by observable(15f)
    var bubbleLineHeight: Float by observable(22f)
    // 回调
    var onActionClickCallback: ((MessageAction) -> Unit)? = null
    var onReactionSelectCallback: ((String) -> Unit)? = null
    var onDismissCallback: (() -> Unit)? = null
}

internal fun ViewContainer<*, *>.MessageOptionsOverlay(init: MessageOptionsOverlayView.() -> Unit) {
    addChild(MessageOptionsOverlayView(), init)
}
