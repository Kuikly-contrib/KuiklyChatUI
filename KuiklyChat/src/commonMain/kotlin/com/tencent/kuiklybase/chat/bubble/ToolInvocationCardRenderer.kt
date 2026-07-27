package com.tencent.kuiklybase.chat.bubble

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.views.*
import com.tencent.kuiklybase.chat.agui.RichMessageKind
import com.tencent.kuiklybase.chat.agui.ToolCallInfo
import com.tencent.kuiklybase.chat.model.ChatMessage
import com.tencent.kuiklybase.chat.model.MessageContext
import com.tencent.kuiklybase.chat.session.ChatSessionConfig

/**
 * 工具调用结果卡片渲染器，同时覆盖成功（`tool_result`）与失败（`tool_error`）两种形态。
 */
class ToolInvocationCardRenderer : MessageRendererFactory {
    override fun canRender(message: ChatMessage): Boolean =
        RichMessageKind.matches(message, RichMessageKind.TOOL_RESULT) ||
            RichMessageKind.matches(message, RichMessageKind.TOOL_ERROR)

    override fun render(container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig) {
        val info = ToolCallInfo.from(context.message)
        val isError = RichMessageKind.matches(context.message, RichMessageKind.TOOL_ERROR)
        val accent = if (isError) Color(0xFFEF4444) else Color(0xFF9F7AEA)
        val bg = if (isError) Color(0xFFFEF2F2) else Color(0xFFFAF5FF)

        container.agUiCardShell(
            accentColor = accent,
            borderColor = accent,
            backgroundColor = bg,
            header = {
                Text {
                    attr {
                        text(if (isError) "⚠️" else "🔧")
                        fontSize(14f)
                        marginRight(6f)
                    }
                }
                Text {
                    attr {
                        text(if (isError) "工具调用失败" else "工具调用")
                        fontSize(12f)
                        color(accent)
                        fontWeightSemiBold()
                        flex(1f)
                    }
                }
                if (info.toolName.isNotEmpty()) {
                    View {
                        attr {
                            borderRadius(4f)
                            backgroundColor(accent)
                            paddingLeft(6f)
                            paddingRight(6f)
                            paddingTop(2f)
                            paddingBottom(2f)
                        }
                        Text {
                            attr {
                                text(info.toolName)
                                fontSize(10f)
                                color(Color.WHITE)
                            }
                        }
                    }
                }
            },
            body = {
                if (info.args.isNotEmpty() && !isError) {
                    View {
                        attr {
                            borderRadius(4f)
                            backgroundColor(Color(0x11000000))
                            paddingLeft(8f)
                            paddingRight(8f)
                            paddingTop(4f)
                            paddingBottom(4f)
                            marginBottom(6f)
                        }
                        Text {
                            attr {
                                text(info.args)
                                fontSize(11f)
                                color(Color(0xFF4A5568))
                                lineHeight(15f)
                            }
                        }
                    }
                }
                Text {
                    attr {
                        text(if (isError) info.errorMessage else info.result)
                        fontSize(13f)
                        color(if (isError) Color(0xFF991B1B) else Color(0xFF2D3436))
                        lineHeight(18f)
                    }
                }
            }
        )
    }
}
