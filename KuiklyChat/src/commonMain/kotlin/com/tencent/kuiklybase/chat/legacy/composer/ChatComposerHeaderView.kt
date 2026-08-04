package com.tencent.kuiklybase.chat.legacy.composer

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.views.*
import com.tencent.kuiklybase.chat.model.ChatMessage

/**
 * ChatComposerHeader — 输入框顶部提示条组件（对标 Stream Chat Compose 的 ComposerHeader）
 *
 * 在输入框上方显示回复/编辑状态的提示条，包含：
 * - 回复模式：左侧竖线 + "回复 xxx" + 引用摘要 + 右侧关闭按钮
 * - 编辑模式：左侧竖线 + "编辑消息" + 右侧关闭按钮
 *
 * 此组件是 ComposerHeader Slot 的默认实现，也可独立使用。
 */
class ChatComposerHeaderView : ComposeView<ChatComposerHeaderAttr, ChatComposerHeaderEvent>() {

    override fun createAttr(): ChatComposerHeaderAttr = ChatComposerHeaderAttr()
    override fun createEvent(): ChatComposerHeaderEvent = ChatComposerHeaderEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    flexDirection(FlexDirection.ROW)
                    alignItems(FlexAlign.CENTER)
                    backgroundColor(Color(ctx.attr.backgroundColor))
                    padding(8f, 12f, 8f, 12f)
                }

                // 左侧竖线
                View {
                    attr {
                        width(3f)
                        height(32f)
                        borderRadius(1.5f)
                        backgroundColor(Color(ctx.attr.barColor))
                    }
                }

                // 中间内容
                View {
                    attr {
                        flex(1f)
                        flexDirection(FlexDirection.COLUMN)
                        marginLeft(8f)
                        marginRight(8f)
                    }

                    // 标题行（"回复 xxx" 或 "编辑消息"）
                    Text {
                        attr {
                            text(when (ctx.attr.mode) {
                                ComposerHeaderMode.REPLY -> "回复 ${ctx.attr.replyingToMessage?.senderName ?: ""}"
                                ComposerHeaderMode.EDIT -> "编辑消息"
                            })
                            fontSize(13f)
                            fontWeightMedium()
                            color(Color(ctx.attr.textColor))
                        }
                    }

                    // 引用内容行（回复模式下显示消息摘要）
                    if (ctx.attr.mode == ComposerHeaderMode.REPLY && ctx.attr.replyingToMessage != null) {
                        Text {
                            attr {
                                text(ctx.attr.replyingToMessage!!.content.take(50).let {
                                    if (ctx.attr.replyingToMessage!!.content.length > 50) "$it..." else it
                                })
                                fontSize(12f)
                                color(Color(ctx.attr.quoteTextColor))
                                marginTop(2f)
                            }
                        }
                    }

                    // 编辑模式下的原文摘要
                    if (ctx.attr.mode == ComposerHeaderMode.EDIT && ctx.attr.editingMessage != null) {
                        Text {
                            attr {
                                text(ctx.attr.editingMessage!!.content.take(50).let {
                                    if (ctx.attr.editingMessage!!.content.length > 50) "$it..." else it
                                })
                                fontSize(12f)
                                color(Color(ctx.attr.quoteTextColor))
                                marginTop(2f)
                            }
                        }
                    }
                }

                // 右侧关闭按钮
                View {
                    attr {
                        size(24f, 24f)
                        allCenter()
                        marginLeft(4f)
                    }
                    event {
                        click {
                            ctx.event.onClose?.invoke()
                        }
                    }
                    // ✕ 图标
                    Text {
                        attr {
                            text("✕")
                            fontSize(14f)
                            color(Color(ctx.attr.closeColor))
                        }
                    }
                }
            }
        }
    }
}

/**
 * ComposerHeader 模式枚举
 */
enum class ComposerHeaderMode {
    /** 回复消息模式 */
    REPLY,
    /** 编辑消息模式 */
    EDIT
}

class ChatComposerHeaderAttr : ComposeAttr() {
    /** 当前模式（回复/编辑） */
    var mode: ComposerHeaderMode by observable(ComposerHeaderMode.REPLY)
    /** 正在回复的消息 */
    var replyingToMessage: ChatMessage? by observable(null)
    /** 正在编辑的消息 */
    var editingMessage: ChatMessage? by observable(null)
    /** 背景色 */
    var backgroundColor: Long by observable(0xFFF0F2F5)
    /** 文字颜色（标题） */
    var textColor: Long by observable(0xFF666666)
    /** 关闭按钮颜色 */
    var closeColor: Long by observable(0xFF999999)
    /** 左侧竖线颜色 */
    var barColor: Long by observable(0xFF4F8FFF)
    /** 引用文字颜色 */
    var quoteTextColor: Long by observable(0xFF999999)
}

class ChatComposerHeaderEvent : ComposeEvent() {
    /** 关闭/取消回调 */
    var onClose: (() -> Unit)? = null
}

/**
 * 便捷扩展函数：在 ViewContainer 中使用 ChatComposerHeader
 */
fun ViewContainer<*, *>.ChatComposerHeader(init: ChatComposerHeaderView.() -> Unit) {
    addChild(ChatComposerHeaderView(), init)
}
