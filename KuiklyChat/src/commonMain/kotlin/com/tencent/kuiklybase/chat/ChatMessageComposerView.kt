package com.tencent.kuiklybase.chat

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.views.*

/**
 * ChatMessageComposer - 消息输入框组件（参考 Stream Chat Compose 的 MessageComposer）
 *
 * 架构设计（5 个 Slot，与 Stream Chat 对齐）：
 *
 * ```
 * ┌──────────────────────────────────────────┐
 * │  composerHeader  (回复/编辑提示条)         │
 * ├──────────────────────────────────────────┤
 * │        │                      │          │
 * │integra-│   composerInput      │ trailing  │
 * │ tions  │   (核心输入框)        │ (发送按钮) │
 * │(附件等) │                      │          │
 * ├──────────────────────────────────────────┤
 * │  composerFooter  (工具栏/表情面板入口)      │
 * ├──────────────────────────────────────────┤
 * │  safeArea  (底部安全区域)                  │
 * └──────────────────────────────────────────┘
 * ```
 *
 * 自定义层级（从轻到重）：
 * 1. 修改属性：composerPlaceholder、onSendMessage 等
 * 2. 替换局部 Slot：composerInput、composerTrailing、composerIntegrations 等
 * 3. 替换整体：messageComposer Slot（完全自定义）
 */
fun ViewContainer<*, *>.ChatMessageComposer(
    cfg: ChatSessionConfig,
    safeAreaBottom: Float = 0f
) {
    val theme = cfg.theme
    val slots = cfg.slots

    // 输入框内部状态
    var inputText = ""
    var inputViewRef: ViewRef<InputView>? = null

    // 注册清空输入框的方法到 config
    cfg._clearComposerInput = {
        inputText = ""
        cfg._composerInputText = ""
        inputViewRef?.view?.setText("")
    }

    // 发送逻辑
    val doSend: () -> Unit = {
        val text = inputText.trim()
        if (text.isNotEmpty()) {
            cfg.onSendMessage?.invoke(text)
            // 发送后清空输入框
            inputText = ""
            cfg._composerInputText = ""
            inputViewRef?.view?.setText("")
        }
    }

    // 获取当前 ComposerState（传给各 Slot）
    val currentState: () -> MessageComposerState = {
        MessageComposerState(inputValue = inputText)
    }

    // ========== 检查是否使用整体替换 Slot ==========
    if (slots.messageComposer != null) {
        // 完全自定义模式：使用方自行实现整个输入框
        slots.messageComposer!!.invoke(this, currentState()) { text ->
            cfg.onSendMessage?.invoke(text)
        }
        return
    }

    // ========== 默认 MessageComposer 实现 ==========
    View {
        attr {
            flexDirection(FlexDirection.COLUMN)
            backgroundColor(Color(theme.composerBackgroundColor))
            border(Border(0.5f, BorderStyle.SOLID, Color(theme.composerBorderColor)))
        }

        // ---- composerHeader Slot（回复/编辑提示条） ----
        if (slots.composerHeader != null) {
            slots.composerHeader!!.invoke(this@View, currentState())
        }

        // ---- 主输入行（integrations + input + trailing） ----
        View {
            attr {
                flexDirection(FlexDirection.ROW)
                alignItems(FlexAlign.CENTER)
                padding(8f, 12f, 8f, 12f)
            }

            // ---- integrations Slot（左侧按钮区域） ----
            if (slots.composerIntegrations != null) {
                slots.composerIntegrations!!.invoke(this@View, currentState())
            }

            // ---- input Slot（核心输入框） ----
            if (slots.composerInput != null) {
                slots.composerInput!!.invoke(this@View, currentState()) { newText ->
                    inputText = newText
                    cfg._composerInputText = newText
                    cfg.onInputValueChange?.invoke(newText)
                }
            } else {
                // 默认输入框
                View {
                    attr {
                        flex(1f)
                        height(36f)
                        backgroundColor(Color(theme.composerInputBackgroundColor))
                        borderRadius(18f)
                        border(Border(0.5f, BorderStyle.SOLID, Color(theme.composerInputBorderColor)))
                        flexDirection(FlexDirection.ROW)
                        alignItems(FlexAlign.CENTER)
                    }
                    Input {
                        ref { inputViewRef = it }
                        attr {
                            flex(1f)
                            height(36f)
                            fontSize(15f)
                            color(Color(theme.composerInputTextColor))
                            placeholder(cfg.composerPlaceholder)
                            placeholderColor(Color(theme.composerPlaceholderColor))
                            marginLeft(14f)
                            marginRight(14f)
                            returnKeyTypeSend()
                        }
                        event {
                            textDidChange { params ->
                                inputText = params.text
                                cfg._composerInputText = params.text
                                cfg.onInputValueChange?.invoke(params.text)
                            }
                            inputReturn { params ->
                                if (params.text.isNotBlank()) {
                                    inputText = params.text
                                    cfg._composerInputText = params.text
                                }
                                doSend()
                            }
                        }
                    }
                }
            }

            // ---- trailing Slot（右侧发送按钮） ----
            if (slots.composerTrailing != null) {
                slots.composerTrailing!!.invoke(this@View, currentState()) { text ->
                    cfg.onSendMessage?.invoke(text)
                    cfg._clearComposerInput?.invoke()
                }
            } else {
                // 默认发送按钮
                View {
                    attr {
                        minWidth(60f)
                        height(36f)
                        marginLeft(4f)
                        borderRadius(18f)
                        backgroundLinearGradient(
                            Direction.TO_RIGHT,
                            ColorStop(Color(theme.primaryColor), 0f),
                            ColorStop(Color(theme.primaryGradientEndColor), 1f)
                        )
                        allCenter()
                        padding(0f, 12f, 0f, 12f)
                    }
                    Text {
                        attr {
                            text(cfg.composerSendButtonText)
                            fontSize(14f)
                            fontWeightMedium()
                            color(Color(theme.composerSendButtonTextColor))
                        }
                    }
                    event {
                        click {
                            doSend()
                        }
                    }
                }
            }
        }

        // ---- composerFooter Slot（底部工具栏） ----
        if (slots.composerFooter != null) {
            slots.composerFooter!!.invoke(this@View, currentState())
        }

        // ---- 底部安全区域 ----
        if (safeAreaBottom > 0f) {
            View {
                attr {
                    height(safeAreaBottom)
                    backgroundColor(Color(theme.composerBackgroundColor))
                }
            }
        }
    }
}
