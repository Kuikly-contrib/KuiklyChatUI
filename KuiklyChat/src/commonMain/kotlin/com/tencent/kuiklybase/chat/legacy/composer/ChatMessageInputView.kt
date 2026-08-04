package com.tencent.kuiklybase.chat.legacy.composer

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.views.*
import com.tencent.kuiklybase.chat.model.ChatMessage
import com.tencent.kuiklybase.chat.session.MessageComposerState

/**
 * ChatMessageInput — 消息输入组件（对标 Stream Chat Compose 的 MessageInput）
 *
 * 在 ChatInputField 基础上封装的消息输入组件，专门用于聊天消息场景。
 * 对标 Stream Chat 的 MessageInput，额外支持：
 * - 回复消息引用摘要（在输入框上方显示引用的消息摘要）
 * - 编辑消息模式（在输入框上方显示"正在编辑"提示）
 * - 与 ComposerState 联动
 *
 * 组件层级：
 * ```
 * ChatMessageComposer (完整编辑器)
 *   ├── ChatComposerHeader (回复/编辑提示条)
 *   ├── ChatMessageInput (消息输入组件) ← 本组件
 *   │     └── ChatInputField (基础输入框)
 *   ├── ComposerTrailing (发送按钮等)
 *   └── ComposerFooter (工具栏)
 * ```
 *
 * 使用示例：
 * ```kotlin
 * container.ChatMessageInput {
 *     attr {
 *         state = MessageComposerState(inputValue = "hello", replyingToMessage = msg)
 *     }
 *     event {
 *         onValueChange = { text -> ... }
 *         onReturn = { text -> ... }
 *     }
 * }
 * ```
 */
class ChatMessageInputView : ComposeView<ChatMessageInputAttr, ChatMessageInputEvent>() {

    override fun createAttr(): ChatMessageInputAttr = ChatMessageInputAttr()
    override fun createEvent(): ChatMessageInputEvent = ChatMessageInputEvent()

    /** 内部 ChatInputField 引用 */
    internal var inputFieldRef: ViewRef<ChatInputFieldView>? = null

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    flex(1f)
                    minHeight(ctx.attr.minHeight)
                    flexDirection(FlexDirection.COLUMN)
                    alignSelfStretch()
                }

                // ---- 内嵌引用回复摘要（轻量级，显示在输入框内部上方） ----
                if (ctx.attr.state.replyingToMessage != null || ctx.attr.state.editingMessage != null) {
                    View {
                        attr {
                            flexDirection(FlexDirection.ROW)
                            alignItems(FlexAlign.CENTER)
                            backgroundColor(Color(ctx.attr.inlineQuoteBgColor))
                            borderRadius(ctx.attr.borderRadius, ctx.attr.borderRadius, 0f, 0f)
                            padding(6f, 10f, 6f, 10f)
                        }

                        // 左侧竖线
                        View {
                            attr {
                                width(2f)
                                height(16f)
                                borderRadius(1f)
                                backgroundColor(Color(ctx.attr.inlineQuoteBarColor))
                            }
                        }

                        // 引用文本
                        val quoteMessage = ctx.attr.state.replyingToMessage ?: ctx.attr.state.editingMessage
                        val prefix = if (ctx.attr.state.editingMessage != null) "[编辑] " else "[回复] "
                        Text {
                            attr {
                                text(prefix + (quoteMessage?.content?.take(40)?.let {
                                    if ((quoteMessage?.content?.length ?: 0) > 40) "$it..." else it
                                } ?: ""))
                                fontSize(12f)
                                color(Color(ctx.attr.inlineQuoteTextColor))
                                marginLeft(6f)
                                flex(1f)
                            }
                        }
                    }
                }

                // ---- 输入框 ----
                ChatInputField {
                    ref { ctx.inputFieldRef = it }
                    attr {
                        placeholder = ctx.attr.placeholder
                        placeholderColor = ctx.attr.placeholderColor
                        backgroundColor = ctx.attr.inputBackgroundColor
                        borderColor = ctx.attr.inputBorderColor
                        borderWidth = ctx.attr.inputBorderWidth
                        borderRadius = ctx.attr.borderRadius
                        textColor = ctx.attr.inputTextColor
                        fontSize = ctx.attr.fontSize
                        minHeight = ctx.attr.minHeight
                        paddingLeft = ctx.attr.inputPaddingLeft
                        paddingRight = ctx.attr.inputPaddingRight
                        paddingTop = ctx.attr.inputPaddingTop
                        paddingBottom = ctx.attr.inputPaddingBottom
                        returnKeyTypeSend = ctx.attr.returnKeyTypeSend
                        autofocus = ctx.attr.autofocus
                    }
                    event {
                        onValueChange = { text ->
                            ctx.event.onValueChange?.invoke(text)
                        }
                        onReturn = { text ->
                            ctx.event.onReturn?.invoke(text)
                        }
                    }
                }
            }
        }
    }

    // ========== 委托方法 ==========

    /** 清空输入框 */
    fun clear() {
        inputFieldRef?.view?.clear()
    }

    /** 设置输入框文本 */
    fun setText(text: String) {
        inputFieldRef?.view?.setText(text)
    }

    /** 获取输入框文本 */
    fun getText(): String {
        return inputFieldRef?.view?.getText() ?: ""
    }
}

class ChatMessageInputAttr : ComposeAttr() {
    /** 当前 Composer 状态 */
    var state: MessageComposerState by observable(MessageComposerState())
    /** 占位文字 */
    var placeholder: String by observable("输入消息...")
    /** 占位文字颜色 */
    var placeholderColor: Long by observable(0xFFBBBBBB)
    /** 输入框背景色 */
    var inputBackgroundColor: Long by observable(0xFFFFFFFF)
    /** 输入框边框色 */
    var inputBorderColor: Long by observable(0xFFE0E0E0)
    /** 输入框边框宽度 */
    var inputBorderWidth: Float by observable(0.5f)
    /** 输入框圆角 */
    var borderRadius: Float by observable(18f)
    /** 输入文字颜色 */
    var inputTextColor: Long by observable(0xFF333333)
    /** 字号 */
    var fontSize: Float by observable(15f)
    /** 最小高度 */
    var minHeight: Float by observable(44f)
    /** 输入框左内边距 */
    var inputPaddingLeft: Float by observable(14f)
    /** 输入框右内边距 */
    var inputPaddingRight: Float by observable(14f)
    /** 输入框上内边距 */
    var inputPaddingTop: Float by observable(0f)
    /** 输入框下内边距 */
    var inputPaddingBottom: Float by observable(0f)
    /** 键盘返回键是否为"发送" */
    var returnKeyTypeSend: Boolean by observable(true)
    /** 是否自动聚焦 */
    var autofocus: Boolean by observable(false)

    // ---- 内嵌引用样式 ----
    /** 内嵌引用条背景色 */
    var inlineQuoteBgColor: Long by observable(0xFFF5F5F5)
    /** 内嵌引用条竖线颜色 */
    var inlineQuoteBarColor: Long by observable(0xFF4F8FFF)
    /** 内嵌引用文字颜色 */
    var inlineQuoteTextColor: Long by observable(0xFF999999)
}

class ChatMessageInputEvent : ComposeEvent() {
    /** 文本变化回调 */
    var onValueChange: ((String) -> Unit)? = null
    /** 回车/发送回调 */
    var onReturn: ((String) -> Unit)? = null
}

/**
 * 便捷扩展函数：在 ViewContainer 中使用 ChatMessageInput
 */
fun ViewContainer<*, *>.ChatMessageInput(init: ChatMessageInputView.() -> Unit) {
    addChild(ChatMessageInputView(), init)
}
