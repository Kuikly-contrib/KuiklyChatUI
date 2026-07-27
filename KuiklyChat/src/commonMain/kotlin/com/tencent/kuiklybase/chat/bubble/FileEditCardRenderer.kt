package com.tencent.kuiklybase.chat.bubble

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.views.*
import com.tencent.kuiklybase.chat.agui.FileEditInfo
import com.tencent.kuiklybase.chat.agui.FileOperation
import com.tencent.kuiklybase.chat.agui.RichMessageKind
import com.tencent.kuiklybase.chat.model.ChatMessage
import com.tencent.kuiklybase.chat.model.MessageContext
import com.tencent.kuiklybase.chat.session.ChatSessionConfig

/**
 * 文件编辑卡片渲染器。
 *
 * 渲染样式：
 * ```
 * ┌──────────────────────────────────────┐
 * │ ✏️ update  src/Main.kt  +12 -3       │
 * │ ┌──────────────────────────────────┐ │
 * │ │ + added line                     │ │
 * │ │ - removed line                   │ │
 * │ └──────────────────────────────────┘ │
 * └──────────────────────────────────────┘
 * ```
 */
class FileEditCardRenderer : MessageRendererFactory {
    override fun canRender(message: ChatMessage): Boolean =
        RichMessageKind.matches(message, RichMessageKind.FILE_EDIT) ||
            RichMessageKind.matches(message, RichMessageKind.FILE_READ)

    override fun render(container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig) {
        val info = FileEditInfo.from(context.message)
        val isRead = info.operation == FileOperation.READ
        val (emoji, label, accent) = when (info.operation) {
            FileOperation.CREATE -> Triple("➕", "新建", Color(0xFF10B981))
            FileOperation.DELETE -> Triple("🗑️", "删除", Color(0xFFEF4444))
            FileOperation.READ -> Triple("📖", "读取", Color(0xFF3B82F6))
            else -> Triple("✏️", "修改", Color(0xFF8B5CF6))
        }

        container.agUiCardShell(
            accentColor = accent,
            borderColor = Color(0xFFE5E7EB),
            backgroundColor = Color(0xFFFAFAFA),
            header = {
                Text {
                    attr {
                        text(emoji)
                        fontSize(14f)
                        marginRight(4f)
                    }
                }
                Text {
                    attr {
                        text(label)
                        fontSize(12f)
                        color(accent)
                        fontWeightSemiBold()
                        marginRight(8f)
                    }
                }
                Text {
                    attr {
                        text(info.filePath)
                        fontSize(12f)
                        color(Color(0xFF1F2937))
                        flex(1f)
                        lines(1)
                    }
                }
                if (!isRead && (info.additions > 0 || info.deletions > 0)) {
                    if (info.additions > 0) {
                        Text {
                            attr {
                                text("+${info.additions}")
                                fontSize(11f)
                                color(Color(0xFF10B981))
                                fontWeightSemiBold()
                                marginLeft(6f)
                            }
                        }
                    }
                    if (info.deletions > 0) {
                        Text {
                            attr {
                                text("-${info.deletions}")
                                fontSize(11f)
                                color(Color(0xFFEF4444))
                                fontWeightSemiBold()
                                marginLeft(4f)
                            }
                        }
                    }
                }
            },
            body = {
                if (info.diffText.isNotEmpty()) {
                    View {
                        attr {
                            borderRadius(6f)
                            backgroundColor(Color(0xFFF3F4F6))
                            paddingLeft(10f)
                            paddingRight(10f)
                            paddingTop(8f)
                            paddingBottom(8f)
                        }
                        Text {
                            attr {
                                text(info.diffText)
                                fontSize(11f)
                                color(Color(0xFF374151))
                                lineHeight(16f)
                            }
                        }
                    }
                }
            }
        )
    }
}
