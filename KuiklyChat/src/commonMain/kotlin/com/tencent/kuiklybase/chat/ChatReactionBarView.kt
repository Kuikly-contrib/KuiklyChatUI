package com.tencent.kuiklybase.chat

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexWrap
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.views.*

// ============================
// 消息反应栏组件
// ============================

/**
 * ChatReactionBarView - 消息反应栏组件
 *
 * 在气泡下方横向展示 emoji 反应列表，每个反应包含 emoji + 数量。
 * 自己已添加的反应会高亮显示。
 * 点击反应触发 onReactionClick 回调。
 *
 * 使用方式：
 * ```
 * ChatReactionBar {
 *     attr {
 *         reactions = listOf(
 *             ReactionItem("👍", 3, true),
 *             ReactionItem("❤️", 1, false)
 *         )
 *     }
 *     event {
 *         onReactionClick = { reactionType -> /* 处理点击 */ }
 *     }
 * }
 * ```
 */
class ChatReactionBarView : ComposeView<ChatReactionBarAttr, ChatReactionBarEvent>() {
    override fun createAttr(): ChatReactionBarAttr = ChatReactionBarAttr()
    override fun createEvent(): ChatReactionBarEvent = ChatReactionBarEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    flexDirectionRow()
                    flexWrap(FlexWrap.WRAP)
                    marginTop(4f)
                }

                for (reaction in ctx.attr.reactions) {
                    View {
                        attr {
                            flexDirectionRow()
                            alignItems(FlexAlign.CENTER)
                            if (reaction.isOwnReaction) {
                                backgroundColor(Color(ctx.attr.highlightBgColor))
                                border(Border(1f, BorderStyle.SOLID, Color(ctx.attr.highlightBorderColor)))
                            } else {
                                backgroundColor(Color(ctx.attr.bgColor))
                            }
                            borderRadius(12f)
                            padding(2f, 8f, 2f, 8f)
                            marginRight(4f)
                            marginBottom(2f)
                        }
                        // Emoji
                        Text {
                            attr {
                                text(reaction.type)
                                fontSize(14f)
                            }
                        }
                        // 数量
                        if (reaction.count > 1) {
                            Text {
                                attr {
                                    text(" ${reaction.count}")
                                    fontSize(11f)
                                    color(Color(ctx.attr.countTextColor))
                                    marginLeft(2f)
                                }
                            }
                        }
                        event {
                            click {
                                ctx.event.onReactionClick?.invoke(reaction.type)
                            }
                        }
                    }
                }
            }
        }
    }
}

class ChatReactionBarAttr : ComposeAttr() {
    /** 反应列表 */
    var reactions: List<ReactionItem> by observable(emptyList())
    /** 默认背景色 */
    var bgColor: Long by observable(0xFFF0F0F0)
    /** 高亮背景色（自己已反应） */
    var highlightBgColor: Long by observable(0xFFE3F2FD)
    /** 高亮边框色 */
    var highlightBorderColor: Long by observable(0xFF4F8FFF)
    /** 计数文字颜色 */
    var countTextColor: Long by observable(0xFF666666)
}

class ChatReactionBarEvent : ComposeEvent() {
    /** 反应点击回调（参数：反应类型标识） */
    var onReactionClick: ((String) -> Unit)? = null
}

fun ViewContainer<*, *>.ChatReactionBar(init: ChatReactionBarView.() -> Unit) {
    addChild(ChatReactionBarView(), init)
}
