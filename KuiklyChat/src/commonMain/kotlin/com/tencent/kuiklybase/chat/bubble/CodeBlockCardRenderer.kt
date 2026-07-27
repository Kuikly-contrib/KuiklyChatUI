package com.tencent.kuiklybase.chat.bubble

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.views.*
import com.tencent.kuiklybase.chat.agui.CodeBlockInfo
import com.tencent.kuiklybase.chat.agui.RichMessageKind
import com.tencent.kuiklybase.chat.model.ChatMessage
import com.tencent.kuiklybase.chat.model.MessageContext
import com.tencent.kuiklybase.chat.session.ChatSessionConfig

/**
 * 代码块卡片渲染器。
 *
 * 渲染样式：
 * ```
 * ┌──────────────────────────────────┐
 * │ kotlin │ Hello.kt        📋 复制 │ ← 头部
 * ├──────────────────────────────────┤
 * │ fun main() {                     │
 * │     println("Hello")             │
 * │ }                                │
 * └──────────────────────────────────┘
 * ```
 */
class CodeBlockCardRenderer(
    /** 复制按钮点击回调 */
    val onCopyClick: ((code: String) -> Unit)? = null
) : MessageRendererFactory {
    override fun canRender(message: ChatMessage): Boolean =
        RichMessageKind.matches(message, RichMessageKind.CODE_BLOCK)

    override fun render(container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig) {
        val info = CodeBlockInfo.from(context.message)
        container.View {
            attr {
                flexDirectionRow()
                alignSelfStretch()
                paddingLeft(12f)
                paddingRight(12f)
                paddingTop(4f)
                paddingBottom(4f)
            }
            View { attr { width(48f) } }

            View {
                attr {
                    flex(1f)
                    borderRadius(8f)
                    backgroundColor(Color(0xFF1E293B))
                    border(Border(1f, BorderStyle.SOLID, Color(0xFF334155)))
                }

                // 头部：语言 + 文件名 + 复制按钮
                View {
                    attr {
                        flexDirectionRow()
                        alignItemsCenter()
                        paddingLeft(10f)
                        paddingRight(10f)
                        paddingTop(6f)
                        paddingBottom(6f)
                        backgroundColor(Color(0xFF0F172A))
                        borderRadius(8f)
                    }
                    if (info.language.isNotEmpty()) {
                        View {
                            attr {
                                borderRadius(3f)
                                backgroundColor(Color(0xFF6366F1))
                                paddingLeft(6f)
                                paddingRight(6f)
                                paddingTop(2f)
                                paddingBottom(2f)
                                marginRight(8f)
                            }
                            Text {
                                attr {
                                    text(info.language)
                                    fontSize(10f)
                                    color(Color.WHITE)
                                    fontWeightSemiBold()
                                }
                            }
                        }
                    }
                    Text {
                        attr {
                            text(info.filename.ifEmpty { "code" })
                            fontSize(11f)
                            color(Color(0xFF94A3B8))
                            flex(1f)
                        }
                    }
                    if (info.lineCount > 0) {
                        Text {
                            attr {
                                text("${info.lineCount} 行")
                                fontSize(10f)
                                color(Color(0xFF64748B))
                                marginRight(10f)
                            }
                        }
                    }
                    // 复制按钮
                    View {
                        attr {
                            paddingLeft(6f)
                            paddingRight(6f)
                            paddingTop(2f)
                            paddingBottom(2f)
                            borderRadius(4f)
                            backgroundColor(Color(0x22FFFFFF))
                        }
                        Text {
                            attr {
                                text("📋 复制")
                                fontSize(10f)
                                color(Color.WHITE)
                            }
                        }
                        event {
                            click { onCopyClick?.invoke(info.code) }
                        }
                    }
                }

                // 代码内容
                View {
                    attr {
                        paddingLeft(12f)
                        paddingRight(12f)
                        paddingTop(10f)
                        paddingBottom(10f)
                    }
                    Text {
                        attr {
                            text(info.code)
                            fontSize(12f)
                            color(Color(0xFFE2E8F0))
                            lineHeight(17f)
                        }
                    }
                }
            }
        }
    }
}
