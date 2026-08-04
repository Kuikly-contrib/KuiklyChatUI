package com.tencent.kuiklybase.chat.legacy.composer

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.views.*

/**
 * ChatInputField — 基础输入框原子组件（对标 Stream Chat Compose 的 InputField）
 *
 * 最底层的输入框组件，基于 Kuikly 的 InputView 封装。提供：
 * - 圆角胶囊/矩形输入框外壳
 * - 自定义边框、背景、内边距
 * - 键盘返回类型配置
 * - 占位文字
 * - 文本变化和提交回调
 * - 输入框引用（用于外部设置文本/清空）
 *
 * 此组件是 ComposerInput Slot 的默认实现，也可独立使用。
 *
 * 使用示例：
 * ```kotlin
 * container.ChatInputField {
 *     attr {
 *         placeholder = "输入消息..."
 *         backgroundColor = 0xFFFFFFFF
 *         borderColor = 0xFFE0E0E0
 *         borderRadius = 18f
 *         textColor = 0xFF333333
 *         fontSize = 15f
 *     }
 *     event {
 *         onValueChange = { text -> /* 处理文本变化 */ }
 *         onReturn = { text -> /* 处理回车发送 */ }
 *     }
 * }
 * ```
 */
class ChatInputFieldView : ComposeView<ChatInputFieldAttr, ChatInputFieldEvent>() {

    override fun createAttr(): ChatInputFieldAttr = ChatInputFieldAttr()
    override fun createEvent(): ChatInputFieldEvent = ChatInputFieldEvent()

    /** 内部 InputView 引用 */
    internal var inputRef: ViewRef<InputView>? = null
    /** 内部维护的当前文本 */
    internal var currentText: String = ""

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    flex(1f)
                    minHeight(ctx.attr.minHeight)
                    backgroundColor(Color(ctx.attr.backgroundColor))
                    borderRadius(ctx.attr.borderRadius)
                    border(Border(ctx.attr.borderWidth, BorderStyle.SOLID, Color(ctx.attr.borderColor)))
                    flexDirection(FlexDirection.ROW)
                    alignItems(FlexAlign.CENTER)
                    paddingLeft(ctx.attr.paddingLeft)
                    paddingRight(ctx.attr.paddingRight)
                }

                // 前置内容（如左侧图标）
                if (ctx.attr.leadingContent != null) {
                    ctx.attr.leadingContent!!.invoke(this@View)
                }

                Input {
                    ref { ctx.inputRef = it }
                    attr {
                        flex(1f)
                        height(ctx.attr.minHeight)
                        fontSize(ctx.attr.fontSize)
                        color(Color(ctx.attr.textColor))
                        placeholder(ctx.attr.placeholder)
                        placeholderColor(Color(ctx.attr.placeholderColor))
                        if (ctx.attr.returnKeyTypeSend) {
                            returnKeyTypeSend()
                        }
                        if (ctx.attr.autofocus) {
                            autofocus(true)
                        }
                    }
                    event {
                        textDidChange { params ->
                            ctx.currentText = params.text
                            ctx.event.onValueChange?.invoke(params.text)
                        }
                        inputReturn { params ->
                            if (params.text.isNotBlank()) {
                                ctx.event.onReturn?.invoke(params.text)
                            }
                        }
                    }
                }

                // 后置内容（如清除按钮）
                if (ctx.attr.trailingContent != null) {
                    ctx.attr.trailingContent!!.invoke(this@View)
                }
            }
        }
    }

    // ========== 外部可操作方法 ==========

    /**
     * 设置输入框文本
     */
    fun setText(text: String) {
        currentText = text
        inputRef?.view?.setText(text)
    }

    /**
     * 清空输入框
     */
    fun clear() {
        currentText = ""
        inputRef?.view?.setText("")
    }

    /**
     * 获取当前输入框文本
     */
    fun getText(): String {
        return currentText
    }
}

class ChatInputFieldAttr : ComposeAttr() {
    /** 占位提示文字 */
    var placeholder: String by observable("输入消息...")
    /** 占位文字颜色 */
    var placeholderColor: Long by observable(0xFFBBBBBB)
    /** 输入框背景色 */
    var backgroundColor: Long by observable(0xFFFFFFFF)
    /** 输入框边框色 */
    var borderColor: Long by observable(0xFFE0E0E0)
    /** 边框宽度 */
    var borderWidth: Float by observable(0.5f)
    /** 圆角半径（18f = 胶囊形，8f = 圆角矩形） */
    var borderRadius: Float by observable(18f)
    /** 输入文字颜色 */
    var textColor: Long by observable(0xFF333333)
    /** 字号 */
    var fontSize: Float by observable(15f)
    /** 最小高度 */
    var minHeight: Float by observable(44f)
    /** 左内边距 */
    var paddingLeft: Float by observable(14f)
    /** 右内边距 */
    var paddingRight: Float by observable(14f)
    /** 上内边距 */
    var paddingTop: Float by observable(8f)
    /** 下内边距 */
    var paddingBottom: Float by observable(8f)
    /** 键盘返回键是否为"发送" */
    var returnKeyTypeSend: Boolean by observable(true)
    /** 是否自动聚焦 */
    var autofocus: Boolean by observable(false)
    /** 前置内容（输入框内部左侧） */
    var leadingContent: ((ViewContainer<*, *>) -> Unit)? by observable(null)
    /** 后置内容（输入框内部右侧） */
    var trailingContent: ((ViewContainer<*, *>) -> Unit)? by observable(null)
}

class ChatInputFieldEvent : ComposeEvent() {
    /** 文本变化回调 */
    var onValueChange: ((String) -> Unit)? = null
    /** 回车/发送回调 */
    var onReturn: ((String) -> Unit)? = null
}

/**
 * 便捷扩展函数：在 ViewContainer 中使用 ChatInputField
 */
fun ViewContainer<*, *>.ChatInputField(init: ChatInputFieldView.() -> Unit) {
    addChild(ChatInputFieldView(), init)
}
