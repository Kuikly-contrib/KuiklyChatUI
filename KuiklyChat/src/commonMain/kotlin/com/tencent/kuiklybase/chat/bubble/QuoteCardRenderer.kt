package com.tencent.kuiklybase.chat.bubble

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.views.*
import com.tencent.kuiklybase.chat.agui.AgUiExtraKeys
import com.tencent.kuiklybase.chat.agui.RichMessageKind
import com.tencent.kuiklybase.chat.model.ChatMessage
import com.tencent.kuiklybase.chat.model.MessageContext
import com.tencent.kuiklybase.chat.session.ChatSessionConfig

/**
 * 引用块卡片渲染器。
 *
 * 左侧蓝色竖条 + 斜体文字，模拟 Markdown `> ...` 效果：
 * ```
 * ┃ 这是被引用的内容，
 * ┃ 可以有多行。
 * ```
 */
class QuoteCardRenderer : MessageRendererFactory {
    override fun canRender(message: ChatMessage): Boolean =
        RichMessageKind.matches(message, RichMessageKind.QUOTE)

    override fun render(container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig) {
        val msg = context.message
        val source = msg.extra[AgUiExtraKeys.QUOTE_SOURCE].orEmpty()

        container.agUiCardShell(
            accentColor = Color(0xFF3B82F6),
            borderColor = null,
            backgroundColor = Color(0xFFF0F9FF),
            header = {
                Text {
                    attr {
                        text("💬")
                        fontSize(14f)
                        marginRight(6f)
                    }
                }
                Text {
                    attr {
                        text("引用")
                        fontSize(12f)
                        color(Color(0xFF3B82F6))
                        fontWeightSemiBold()
                        flex(1f)
                    }
                }
                if (source.isNotEmpty()) {
                    Text {
                        attr {
                            text("— $source")
                            fontSize(10f)
                            color(Color(0xFF94A3B8))
                        }
                    }
                }
            },
            body = {
                Text {
                    attr {
                        text(msg.content)
                        fontSize(13f)
                        color(Color(0xFF334155))
                        lineHeight(20f)
                        fontStyleItalic()
                    }
                }
            }
        )
    }
}
