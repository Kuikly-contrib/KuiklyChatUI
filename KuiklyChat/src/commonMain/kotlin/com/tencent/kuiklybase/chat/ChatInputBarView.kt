package com.tencent.kuiklybase.chat

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.views.*
import com.tencent.kuikly.core.views.layout.Row

// ============================
// 聊天输入栏组件
// ============================

class ChatInputBarView : ComposeView<ChatInputBarAttr, ChatInputBarEvent>() {
    override fun createAttr(): ChatInputBarAttr = ChatInputBarAttr()
    override fun createEvent(): ChatInputBarEvent = ChatInputBarEvent()

    private var currentText: String = ""
    private lateinit var inputViewRef: ViewRef<InputView>

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    backgroundColor(Color(0xFFF8F8F8))
                    border(Border(0.5f, BorderStyle.SOLID, Color(0xFFE0E0E0)))
                }
                // 输入区域
                Row {
                    attr {
                        padding(8f, 12f, 8f, 12f)
                        alignItems(FlexAlign.CENTER)
                    }
                    // 输入框容器
                    View {
                        attr {
                            flex(1f)
                            height(36f)
                            backgroundColor(Color.WHITE)
                            borderRadius(18f)
                            border(Border(0.5f, BorderStyle.SOLID, Color(0xFFE0E0E0)))
                            flexDirectionRow()
                            alignItemsCenter()
                        }
                        Input {
                            ref { ctx.inputViewRef = it }
                            attr {
                                flex(1f)
                                height(36f)
                                fontSize(15f)
                                color(Color(0xFF333333))
                                placeholder(ctx.attr.placeholder)
                                placeholderColor(Color(0xFFBBBBBB))
                                marginLeft(14f)
                                marginRight(14f)
                                returnKeyTypeSend()
                            }
                            event {
                                textDidChange { params ->
                                    ctx.currentText = params.text
                                }
                                inputReturn { params ->
                                    // 优先使用回车事件中携带的文本（更可靠）
                                    if (params.text.isNotBlank()) {
                                        ctx.currentText = params.text
                                    }
                                    ctx.sendMessage()
                                }
                                keyboardHeightChange { params ->
                                    ctx.event.onKeyboardHeightChange?.invoke(params.height)
                                }
                            }
                        }
                    }

                    // 发送按钮
                    View {
                        attr {
                            size(60f, 36f)
                            marginLeft(8f)
                            borderRadius(18f)
                            backgroundLinearGradient(
                                Direction.TO_RIGHT,
                                ColorStop(Color(ctx.attr.primaryColor), 0f),
                                ColorStop(Color(ctx.attr.primaryGradientEndColor), 1f)
                            )
                            allCenter()
                        }
                        Text {
                            attr {
                                text("发送")
                                fontSize(14f)
                                fontWeightMedium()
                                color(Color.WHITE)
                            }
                        }
                        event {
                            click {
                                ctx.sendMessage()
                            }
                        }
                    }
                }

                // 底部安全区域占位
                View {
                    attr {
                        height(ctx.pagerData.safeAreaInsets.bottom)
                        backgroundColor(Color(0xFFF8F8F8))
                    }
                }
            }
        }
    }

    private fun sendMessage() {
        val text = currentText.trim()
        if (text.isNotEmpty()) {
            event.onSendMessage?.invoke(text)
            currentText = ""
            inputViewRef.view?.setText("")
        }
    }
}

class ChatInputBarAttr : ComposeAttr() {
    var placeholder: String by observable("输入消息...")
    var primaryColor: Long by observable(0xFF4F8FFF)
    var primaryGradientEndColor: Long by observable(0xFF6C5CE7)
}

class ChatInputBarEvent : ComposeEvent() {
    var onSendMessage: ((String) -> Unit)? = null
    var onKeyboardHeightChange: ((Float) -> Unit)? = null
}

fun ViewContainer<*, *>.ChatInputBar(init: ChatInputBarView.() -> Unit) {
    addChild(ChatInputBarView(), init)
}
