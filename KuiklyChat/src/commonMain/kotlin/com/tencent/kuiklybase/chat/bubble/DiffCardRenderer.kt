package com.tencent.kuiklybase.chat.bubble

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.views.*
import com.tencent.kuiklybase.chat.agui.AgUiExtraKeys
import com.tencent.kuiklybase.chat.agui.RichMessageKind
import com.tencent.kuiklybase.chat.model.ChatMessage
import com.tencent.kuiklybase.chat.model.MessageContext
import com.tencent.kuiklybase.chat.session.ChatSessionConfig

/**
 * Diff 对比卡片渲染器。
 *
 * 对 diff 文本做语法着色：
 * - `+` 行 → 绿色背景
 * - `-` 行 → 红色背景
 * - `@@` 行 → 灰色注释
 * - 上下文行 → 默认背景
 */
class DiffCardRenderer : MessageRendererFactory {
    override fun canRender(message: ChatMessage): Boolean =
        RichMessageKind.matches(message, RichMessageKind.DIFF)

    override fun render(container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig) {
        val msg = context.message
        val lines = msg.content.lines()
        val lang = msg.extra[AgUiExtraKeys.DIFF_LANGUAGE].orEmpty().ifEmpty { "diff" }

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

                // Header
                View {
                    attr {
                        flexDirectionRow()
                        alignItemsCenter()
                        paddingLeft(10f)
                        paddingRight(10f)
                        paddingTop(6f)
                        paddingBottom(6f)
                        backgroundColor(Color(0xFF0F172A))
                    }
                    View {
                        attr {
                            borderRadius(3f)
                            backgroundColor(Color(0xFF10B981))
                            paddingLeft(6f)
                            paddingRight(6f)
                            paddingTop(2f)
                            paddingBottom(2f)
                            marginRight(8f)
                        }
                        Text {
                            attr {
                                text(lang)
                                fontSize(10f)
                                color(Color.WHITE)
                                fontWeightSemiBold()
                            }
                        }
                    }
                    Text {
                        attr {
                            text("${lines.size} 行变更")
                            fontSize(11f)
                            color(Color(0xFF94A3B8))
                            flex(1f)
                        }
                    }
                }

                // Diff 内容（行级着色）
                View {
                    attr {
                        paddingLeft(10f)
                        paddingRight(10f)
                        paddingTop(8f)
                        paddingBottom(8f)
                    }
                    lines.forEach { line ->
                        val (bgColor, textColor) = when {
                            line.startsWith("+") && !line.startsWith("+++") ->
                                Color(0x1A10B981) to Color(0xFF10B981)
                            line.startsWith("-") && !line.startsWith("---") ->
                                Color(0x1AEF4444) to Color(0xFFEF4444)
                            line.startsWith("@@") ->
                                Color(0xFF1E293B) to Color(0xFF94A3B8)
                            else -> Color(0xFF1E293B) to Color(0xFFE2E8F0)
                        }
                        View {
                            attr {
                                backgroundColor(bgColor)
                                paddingLeft(6f)
                                paddingRight(6f)
                                paddingTop(1f)
                                paddingBottom(1f)
                            }
                            Text {
                                attr {
                                    text(line)
                                    fontSize(11f)
                                    color(textColor)
                                    lineHeight(16f)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
