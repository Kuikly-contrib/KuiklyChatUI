package com.tencent.kuiklybase.chat.bubble

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.views.*
import com.tencent.kuiklybase.chat.agui.LoadingInfo
import com.tencent.kuiklybase.chat.agui.LoadingStyle
import com.tencent.kuiklybase.chat.agui.RichMessageKind
import com.tencent.kuiklybase.chat.model.ChatMessage
import com.tencent.kuiklybase.chat.model.MessageContext
import com.tencent.kuiklybase.chat.session.ChatSessionConfig

/**
 * Loading 占位消息渲染器。
 *
 * 匹配 `agUiKind == "loading"`，根据 [LoadingStyle] 渲染不同的指示器：
 * - `dots`：三个跳动的点
 * - `spinner`：旋转图标
 * - `progress`：进度条
 * - `skeleton`：骨架屏
 */
class LoadingCardRenderer : MessageRendererFactory {
    override fun canRender(message: ChatMessage): Boolean =
        RichMessageKind.matches(message, RichMessageKind.LOADING) ||
            RichMessageKind.matches(message, RichMessageKind.TOOL_CALL_PENDING)

    override fun render(container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig) {
        val info = LoadingInfo.from(context.message)
        container.agUiCardShell(
            accentColor = Color(0xFF4F8FFF),
            borderColor = Color(0xFFE0E7FF),
            backgroundColor = Color(0xFFF5F8FF),
            header = {
                // Loading 点点动画（静态三个点，实际抖动靠 CSS/动画 —— Kuikly 可通过 animator 实现，这里用占位符）
                LoadingIndicator(info.style, Color(0xFF4F8FFF))
                Text {
                    attr {
                        text(info.title)
                        fontSize(13f)
                        color(Color(0xFF1A202C))
                        fontWeightSemiBold()
                        flex(1f)
                        marginLeft(8f)
                    }
                }
            },
            body = {
                if (info.subtitle.isNotEmpty()) {
                    Text {
                        attr {
                            text(info.subtitle)
                            fontSize(12f)
                            color(Color(0xFF4A5568))
                            lineHeight(18f)
                        }
                    }
                }
                if (info.target.isNotEmpty()) {
                    View {
                        attr {
                            flexDirectionRow()
                            alignItemsCenter()
                            marginTop(6f)
                            paddingLeft(8f)
                            paddingRight(8f)
                            paddingTop(3f)
                            paddingBottom(3f)
                            borderRadius(10f)
                            backgroundColor(Color(0xFFE0E7FF))
                            alignSelfFlexStart()
                        }
                        Text {
                            attr {
                                text("→ ${info.target}")
                                fontSize(11f)
                                color(Color(0xFF3730A3))
                            }
                        }
                    }
                }
            }
        )
    }
}
