package com.tencent.kuiklybase.chat

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.*

// ============================
// 聊天会话组件（完整聊天界面）
// ============================

/**
 * ChatSessionView - 完整的聊天会话组件
 *
 * 包含：
 * - 顶部导航栏（可选）
 * - 消息列表（支持 vfor 循环渲染）
 * - 底部输入栏（带发送按钮）
 *
 * 使用方式：
 * ```kotlin
 * ChatSession {
 *     attr {
 *         title = "聊天"
 *         messages = yourMessageList
 *         showNavigationBar = true
 *     }
 *     event {
 *         onSendMessage = { text -> /* 处理发送消息 */ }
 *     }
 * }
 * ```
 */
class ChatSessionView : ComposeView<ChatSessionAttr, ChatSessionEvent>() {
    override fun createAttr(): ChatSessionAttr = ChatSessionAttr()
    override fun createEvent(): ChatSessionEvent = ChatSessionEvent()

    private var keyboardHeight: Float by observable(0f)
    private lateinit var listRef: ViewRef<ListView<*, *>>

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    flex(1f)
                    backgroundColor(Color(ctx.attr.backgroundColor))
                    flexDirection(FlexDirection.COLUMN)
                }

                // ========== 顶部导航栏 ==========
                if (ctx.attr.showNavigationBar) {
                    ChatNavigationBar {
                        attr {
                            title = ctx.attr.title
                            showBackButton = ctx.attr.showBackButton
                            primaryColor = ctx.attr.primaryColor
                            primaryGradientEndColor = ctx.attr.primaryGradientEndColor
                        }
                        event {
                            onBackClick = ctx.event.onBackClick
                        }
                    }
                }

                // ========== 消息列表区域 ==========
                List {
                    ref { ctx.listRef = it }
                    attr {
                        flex(1f)
                        flexDirection(FlexDirection.COLUMN)
                    }

                    // 循环渲染消息
                    vfor({ ctx.attr.messages }) { message ->
                        if (message.type == MessageType.SYSTEM) {
                            ChatSystemMessage {
                                attr {
                                    this.message = message.content
                                }
                            }
                        } else {
                            ChatBubble {
                                attr {
                                    content = message.content
                                    isSelf = message.isSelf
                                    avatarUrl = message.senderAvatar
                                    selfAvatarUrl = ctx.attr.selfAvatarUrl
                                    senderName = if (!message.isSelf) message.senderName else ""
                                    primaryColor = ctx.attr.primaryColor
                                    primaryGradientEndColor = ctx.attr.primaryGradientEndColor
                                }
                                event {
                                    onLongPress = {
                                        ctx.event.onMessageLongPress?.invoke(message)
                                    }
                                }
                            }
                        }
                    }

                    // 底部留白
                    View {
                        attr {
                            height(8f)
                        }
                    }
                }

                // ========== 底部输入栏 ==========
                View {
                    attr {
                        marginBottom(ctx.keyboardHeight)
                    }
                    ChatInputBar {
                        attr {
                            placeholder = ctx.attr.inputPlaceholder
                            primaryColor = ctx.attr.primaryColor
                            primaryGradientEndColor = ctx.attr.primaryGradientEndColor
                        }
                        event {
                            onSendMessage = { text ->
                                ctx.event.onSendMessage?.invoke(text)
                            }
                            onKeyboardHeightChange = { height ->
                                ctx.keyboardHeight = height
                                if (height > 0f) {
                                    ctx.scrollToBottom(true)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun viewDidLoad() {
        super.viewDidLoad()
        // 延迟滚动到底部
        setTimeout(300) {
            scrollToBottom(false)
        }
    }

    /**
     * 滚动到底部
     */
    fun scrollToBottom(animated: Boolean) {
        val count = attr.messages.size
        if (count > 0) {
            listRef.view?.setContentOffset(0f, Float.MAX_VALUE, animated)
        }
    }
}

class ChatSessionAttr : ComposeAttr() {
    // 导航栏配置
    var title: String by observable("聊天")
    var showNavigationBar: Boolean by observable(true)
    var showBackButton: Boolean by observable(true)

    // 消息数据（必须使用 observableList 才能触发 vfor 更新）
    var messages: ObservableList<ChatMessage> by observableList<ChatMessage>()

    // 输入栏配置
    var inputPlaceholder: String by observable("输入消息...")

    // 头像配置
    var selfAvatarUrl: String by observable("")

    // 主题色配置
    var primaryColor: Long by observable(0xFF4F8FFF)
    var primaryGradientEndColor: Long by observable(0xFF6C5CE7)
    var backgroundColor: Long by observable(0xFFF0F2F5)
}

class ChatSessionEvent : ComposeEvent() {
    /** 发送消息回调 */
    var onSendMessage: ((String) -> Unit)? = null
    /** 返回按钮点击回调 */
    var onBackClick: (() -> Unit)? = null
    /** 消息长按回调 */
    var onMessageLongPress: ((ChatMessage) -> Unit)? = null
}

/**
 * DSL 扩展函数 - 在任意 ViewContainer 中使用 ChatSession
 */
fun ViewContainer<*, *>.ChatSession(init: ChatSessionView.() -> Unit) {
    addChild(ChatSessionView(), init)
}
