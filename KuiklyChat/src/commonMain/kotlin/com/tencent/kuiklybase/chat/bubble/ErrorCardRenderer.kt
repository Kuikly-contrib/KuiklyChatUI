package com.tencent.kuiklybase.chat.bubble

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.views.*
import com.tencent.kuiklybase.chat.agui.AgUiExtraKeys
import com.tencent.kuiklybase.chat.agui.RichMessageKind
import com.tencent.kuiklybase.chat.model.ChatMessage
import com.tencent.kuiklybase.chat.model.MessageContext
import com.tencent.kuiklybase.chat.session.ChatSessionConfig

/**
 * 错误卡片渲染器：展示 `RunError` 等错误消息的文案与错误码。
 */
class ErrorCardRenderer : MessageRendererFactory {
    override fun canRender(message: ChatMessage): Boolean =
        RichMessageKind.matches(message, RichMessageKind.ERROR)

    override fun render(container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig) {
        val msg = context.message
        val errorMsg = msg.extra[AgUiExtraKeys.ERROR_MESSAGE].orEmpty().ifEmpty { msg.content }
        val errorCode = msg.extra[AgUiExtraKeys.ERROR_CODE].orEmpty()

        container.agUiCardShell(
            accentColor = Color(0xFFEF4444),
            borderColor = Color(0xFFFECACA),
            backgroundColor = Color(0xFFFEF2F2),
            header = {
                Text {
                    attr {
                        text("❌")
                        fontSize(14f)
                        marginRight(6f)
                    }
                }
                Text {
                    attr {
                        text("错误")
                        fontSize(12f)
                        color(Color(0xFFDC2626))
                        fontWeightSemiBold()
                        flex(1f)
                    }
                }
                if (errorCode.isNotEmpty()) {
                    View {
                        attr {
                            borderRadius(4f)
                            backgroundColor(Color(0xFFEF4444))
                            paddingLeft(6f)
                            paddingRight(6f)
                            paddingTop(2f)
                            paddingBottom(2f)
                        }
                        Text {
                            attr {
                                text(errorCode)
                                fontSize(10f)
                                color(Color.WHITE)
                            }
                        }
                    }
                }
            },
            body = {
                Text {
                    attr {
                        text(errorMsg)
                        fontSize(13f)
                        color(Color(0xFF991B1B))
                        lineHeight(18f)
                    }
                }
            }
        )
    }
}
