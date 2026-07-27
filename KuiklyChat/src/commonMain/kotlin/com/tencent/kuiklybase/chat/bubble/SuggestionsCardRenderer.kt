package com.tencent.kuiklybase.chat.bubble

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.views.*
import com.tencent.kuiklybase.chat.agui.RichMessageKind
import com.tencent.kuiklybase.chat.model.ChatMessage
import com.tencent.kuiklybase.chat.model.MessageContext
import com.tencent.kuiklybase.chat.session.ChatSessionConfig

/**
 * 建议选项卡片渲染器：把候选回复渲染成一排可点击的胶囊按钮。
 */
class SuggestionsCardRenderer(
    /** 点击某个建议时的回调：(value) -> Unit */
    val onSuggestionClick: ((String) -> Unit)? = null
) : MessageRendererFactory {
    override fun canRender(message: ChatMessage): Boolean =
        RichMessageKind.matches(message, RichMessageKind.SUGGESTIONS)

    override fun render(container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig) {
        val msg = context.message
        val count = msg.extra["suggestion.count"]?.toIntOrNull() ?: 0
        val options = (0 until count).map { i ->
            val label = msg.extra["suggestion.$i.label"].orEmpty()
            val value = msg.extra["suggestion.$i.value"].orEmpty()
            label to value
        }

        container.agUiCardShell(
            accentColor = Color(0xFF10B981),
            borderColor = Color(0xFFE5E7EB),
            backgroundColor = Color(0xFFFFFFFF),
            header = {
                Text {
                    attr {
                        text("💡")
                        fontSize(14f)
                        marginRight(6f)
                    }
                }
                Text {
                    attr {
                        text(msg.content.ifEmpty { "你可以这样回复：" })
                        fontSize(12f)
                        color(Color(0xFF1F2937))
                        flex(1f)
                    }
                }
            },
            body = {
                View {
                    attr {
                        flexDirectionRow()
                        flexWrapWrap()
                    }
                    options.forEach { (label, value) ->
                        View {
                            attr {
                                borderRadius(14f)
                                backgroundColor(Color(0xFFF0FDF4))
                                border(Border(1f, BorderStyle.SOLID, Color(0xFF10B981)))
                                paddingLeft(10f)
                                paddingRight(10f)
                                paddingTop(5f)
                                paddingBottom(5f)
                                marginRight(6f)
                                marginBottom(6f)
                            }
                            Text {
                                attr {
                                    text(label)
                                    fontSize(12f)
                                    color(Color(0xFF047857))
                                }
                            }
                            event {
                                click { onSuggestionClick?.invoke(value) }
                            }
                        }
                    }
                }
            }
        )
    }
}
