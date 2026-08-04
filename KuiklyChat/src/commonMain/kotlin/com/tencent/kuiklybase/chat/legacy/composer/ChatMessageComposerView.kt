package com.tencent.kuiklybase.chat.legacy.composer

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.views.*
import com.tencent.kuiklybase.chat.session.ChatSessionConfig
import com.tencent.kuiklybase.chat.session.MessageComposerState

/**
 * ChatMessageComposer - 消息输入框组件（参考 Stream Chat Compose 的 MessageComposer）
 *
 * 架构设计（三层组件 + 5 个 Slot，与 Stream Chat 对齐）：
 *
 * ```
 * ┌──────────────────────────────────────────┐
 * │  composerHeader  (回复/编辑提示条)         │
 * │  └─ 默认: ChatComposerHeader             │
 * ├──────────────────────────────────────────┤
 * │        │                      │          │
 * │integra-│   composerInput      │ trailing  │
 * │ tions  │   └─ ChatMessageInput│ (发送按钮) │
 * │(附件等) │      └─ ChatInputField│         │
 * ├──────────────────────────────────────────┤
 * │  composerFooter  (工具栏/表情面板入口)      │
 * ├──────────────────────────────────────────┤
 * │  safeArea  (底部安全区域)                  │
 * └──────────────────────────────────────────┘
 *
 * 组件层级（对标 Stream Chat Compose）：
 * ChatMessageComposer   ← MessageComposer（完整编辑器）
 *   ├── ChatComposerHeader  ← ComposerHeader（回复/编辑提示条）
 *   ├── ChatMessageInput    ← MessageInput（消息输入组件）
 *   │     └── ChatInputField  ← InputField（基础输入框）
 *   └── ComposerTrailing    ← trailingContent（发送按钮等）
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
    var messageInputRef: ViewRef<ChatMessageInputView>? = null

    // 注册清空输入框的方法到 config
    cfg._clearComposerInput = {
        inputText = ""
        cfg._composerInputText = ""
        messageInputRef?.view?.clear()
    }

    // 发送逻辑
    val doSend: () -> Unit = {
        val text = inputText.trim()
        if (text.isNotEmpty()) {
            cfg.onSendMessage?.invoke(text)
            // 发送后清空输入框
            inputText = ""
            cfg._composerInputText = ""
            messageInputRef?.view?.clear()
            // 发送后清除回复状态
            cfg._replyingToMessage = null
        }
    }

    // 获取当前 ComposerState（传给各 Slot，包含回复/编辑状态）
    val currentState: () -> MessageComposerState = {
        MessageComposerState(
            inputValue = inputText,
            editingMessage = null,
            replyingToMessage = cfg._replyingToMessage
        )
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
            alignSelfStretch()
            flexDirection(FlexDirection.COLUMN)
            backgroundColor(Color(theme.composerBackgroundColor))
            border(Border(0.5f, BorderStyle.SOLID, Color(theme.composerBorderColor)))
        }

        // ---- composerHeader Slot（回复/编辑提示条） ----
        if (slots.composerHeader != null) {
            slots.composerHeader!!.invoke(this@View, currentState())
        } else {
            // 默认：使用 ChatComposerHeader 组件（仅在有回复/编辑状态时显示）
            val replyMsg = cfg._replyingToMessage
            if (replyMsg != null) {
                ChatComposerHeader {
                    attr {
                        mode = ComposerHeaderMode.REPLY
                        replyingToMessage = replyMsg
                        backgroundColor = theme.composerHeaderBackgroundColor
                        textColor = theme.composerHeaderTextColor
                        closeColor = theme.composerHeaderCloseColor
                        barColor = theme.composerHeaderBarColor
                        quoteTextColor = theme.composerHeaderQuoteTextColor
                    }
                    event {
                        onClose = {
                            cfg.cancelReply()
                        }
                    }
                }
            }
        }

        // ---- 主输入行（integrations + input + trailing） ----
        View {
            attr {
                alignSelfStretch()
                flexDirection(FlexDirection.ROW)
                alignItems(FlexAlign.CENTER)
                padding(10f, 12f, 10f, 12f)
            }

            // ---- integrations Slot（左侧按钮区域） ----
            if (slots.composerIntegrations != null) {
                slots.composerIntegrations!!.invoke(this@View, currentState())
            }

            // ---- input Slot（核心输入框） ----
            if (slots.composerInput != null) {
                View {
                    attr { flex(1f) }
                    slots.composerInput!!.invoke(this@View, currentState()) { newText ->
                        inputText = newText
                        cfg._composerInputText = newText
                        cfg.onInputValueChange?.invoke(newText)
                    }
                }
            } else {
                // 默认：使用 ChatMessageInput 组件，外层 View 确保在 ROW 中 flex 扩展
                View {
                    attr { flex(1f) }
                    ChatMessageInput {
                        ref { messageInputRef = it }
                        attr {
                            state = currentState()
                            placeholder = cfg.composerPlaceholder
                            placeholderColor = theme.composerPlaceholderColor
                            inputBackgroundColor = theme.composerInputBackgroundColor
                            inputBorderColor = theme.composerInputBorderColor
                            inputTextColor = theme.composerInputTextColor
                            fontSize = 15f
                            minHeight = 44f
                            borderRadius = 18f
                            returnKeyTypeSend = true

                            inlineQuoteBarColor = theme.composerHeaderBarColor
                            inlineQuoteTextColor = theme.composerHeaderQuoteTextColor
                        }
                        event {
                            onValueChange = { newText ->
                                inputText = newText
                                cfg._composerInputText = newText
                                cfg.onInputValueChange?.invoke(newText)
                            }
                            onReturn = { _ ->
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
                        height(44f)
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
