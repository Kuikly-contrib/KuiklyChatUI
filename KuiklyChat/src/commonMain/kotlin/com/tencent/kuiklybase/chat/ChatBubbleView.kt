package com.tencent.kuiklybase.chat

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.views.*
import com.tencent.kuikly.core.views.layout.Column

// ============================
// 系统消息组件（时间提示、系统通知等）
// ============================

class ChatSystemMessageView : ComposeView<ChatSystemMessageAttr, ComposeEvent>() {
    override fun createAttr(): ChatSystemMessageAttr = ChatSystemMessageAttr()
    override fun createEvent(): ComposeEvent = ComposeEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    allCenter()
                    padding(8f, 0f, 8f, 0f)
                }
                View {
                    attr {
                        backgroundColor(Color(0xFFCECECE))
                        borderRadius(4f)
                        padding(4f, 8f, 4f, 8f)
                    }
                    Text {
                        attr {
                            text(ctx.attr.message)
                            fontSize(11f)
                            color(Color.WHITE)
                        }
                    }
                }
            }
        }
    }
}

class ChatSystemMessageAttr : ComposeAttr() {
    var message: String by observable("")
}

fun ViewContainer<*, *>.ChatSystemMessage(init: ChatSystemMessageView.() -> Unit) {
    addChild(ChatSystemMessageView(), init)
}

// ============================
// 聊天气泡组件（发送方 / 接收方消息）
// ============================

class ChatBubbleView : ComposeView<ChatBubbleAttr, ChatBubbleEvent>() {
    override fun createAttr(): ChatBubbleAttr = ChatBubbleAttr()
    override fun createEvent(): ChatBubbleEvent = ChatBubbleEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    flexDirectionRow()
                    padding(6f, 12f, 6f, 12f)
                    if (ctx.attr.isSelf) {
                        justifyContent(FlexJustifyContent.FLEX_END)
                    } else {
                        justifyContent(FlexJustifyContent.FLEX_START)
                    }
                }

                if (!ctx.attr.isSelf) {
                    // ===== 对方消息：头像在左 =====
                    // 头像
                    Image {
                        attr {
                            size(40f, 40f)
                            borderRadius(20f)
                            src(ctx.attr.avatarUrl.ifEmpty { DEFAULT_AVATAR })
                            resizeCover()
                            marginTop(2f)
                        }
                    }
                    // 消息内容区
                    Column {
                        attr {
                            marginLeft(8f)
                            maxWidth(ctx.pagerData.pageViewWidth * 0.65f)
                        }
                        // 发送者名称
                        if (ctx.attr.senderName.isNotEmpty()) {
                            Text {
                                attr {
                                    text(ctx.attr.senderName)
                                    fontSize(12f)
                                    color(Color(0xFF999999))
                                    marginBottom(4f)
                                }
                            }
                        }
                        // 消息气泡
                        View {
                            attr {
                                backgroundColor(Color.WHITE)
                                borderRadius(BorderRectRadius(2f, 12f, 12f, 12f))
                                padding(10f, 12f, 10f, 12f)
                                boxShadow(BoxShadow(0f, 1f, 6f, Color(0x1A000000)))
                            }
                            Text {
                                attr {
                                    text(ctx.attr.content)
                                    fontSize(15f)
                                    color(Color(0xFF333333))
                                    lineHeight(22f)
                                }
                            }
                            event {
                                longPress {
                                    ctx.event.onLongPress?.invoke()
                                }
                            }
                        }
                    }
                } else {
                    // ===== 自己的消息：气泡在左，头像在右 =====
                    Column {
                        attr {
                            maxWidth(ctx.pagerData.pageViewWidth * 0.65f)
                            alignItems(FlexAlign.FLEX_END)
                        }
                        // 消息气泡（渐变色）
                        View {
                            attr {
                                backgroundLinearGradient(
                                    Direction.TO_RIGHT,
                                    ColorStop(Color(ctx.attr.primaryColor), 0f),
                                    ColorStop(Color(ctx.attr.primaryGradientEndColor), 1f)
                                )
                                borderRadius(BorderRectRadius(12f, 2f, 12f, 12f))
                                padding(10f, 12f, 10f, 12f)
                                boxShadow(BoxShadow(0f, 1f, 6f, Color(0x334F8FFF)))
                            }
                            Text {
                                attr {
                                    text(ctx.attr.content)
                                    fontSize(15f)
                                    color(Color.WHITE)
                                    lineHeight(22f)
                                }
                            }
                            event {
                                longPress {
                                    ctx.event.onLongPress?.invoke()
                                }
                            }
                        }
                    }
                    // 头像
                    Image {
                        attr {
                            size(40f, 40f)
                            borderRadius(20f)
                            src(ctx.attr.selfAvatarUrl.ifEmpty { SELF_AVATAR })
                            resizeCover()
                            marginLeft(8f)
                            marginTop(2f)
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val DEFAULT_AVATAR =
            "https://vfiles.gtimg.cn/wuji_dashboard/wupload/xy/starter/62394e19.png"
        const val SELF_AVATAR =
            "https://vfiles.gtimg.cn/wuji_dashboard/wupload/xy/starter/62394e19.png"
    }
}

class ChatBubbleAttr : ComposeAttr() {
    var content: String by observable("")
    var isSelf: Boolean by observable(false)
    var avatarUrl: String by observable("")
    var selfAvatarUrl: String by observable("")
    var senderName: String by observable("")
    var primaryColor: Long by observable(0xFF4F8FFF)
    var primaryGradientEndColor: Long by observable(0xFF6C5CE7)
}

class ChatBubbleEvent : ComposeEvent() {
    var onLongPress: (() -> Unit)? = null
}

fun ViewContainer<*, *>.ChatBubble(init: ChatBubbleView.() -> Unit) {
    addChild(ChatBubbleView(), init)
}
