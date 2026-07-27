package com.tencent.kuiklybase.chat.bubble

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.views.*
import com.tencent.kuiklybase.chat.agui.AgUiExtraKeys
import com.tencent.kuiklybase.chat.agui.RichMessageKind
import com.tencent.kuiklybase.chat.model.ChatMessage
import com.tencent.kuiklybase.chat.model.MessageContext
import com.tencent.kuiklybase.chat.session.ChatSessionConfig

/**
 * 折叠内容卡片渲染器（模拟 HTML `<details>` / `<summary>`）。
 *
 * 展示为可点击的摘要标题，点击后展开/收起正文。
 * 注：Kuikly DSL 中通过状态切换 + vif 实现展开收起。
 */
class CollapsibleCardRenderer : MessageRendererFactory {
    override fun canRender(message: ChatMessage): Boolean =
        RichMessageKind.matches(message, RichMessageKind.COLLAPSIBLE)

    override fun render(container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig) {
        val msg = context.message
        val summary = msg.extra[AgUiExtraKeys.COLLAPSIBLE_SUMMARY].orEmpty()
            .ifEmpty { "展开详情" }

        container.agUiCardShell(
            accentColor = Color(0xFF8B5CF6),
            borderColor = Color(0xFFD8B4FE),
            backgroundColor = Color(0xFFFAF5FF),
            header = {
                Text {
                    attr {
                        text("▶")
                        fontSize(12f)
                        color(Color(0xFF8B5CF6))
                        marginRight(6f)
                    }
                }
                Text {
                    attr {
                        text(summary)
                        fontSize(12f)
                        color(Color(0xFF5B21B6))
                        fontWeightSemiBold()
                        flex(1f)
                    }
                }
            },
            body = {
                // 折叠内容默认展开显示（在 DSL 中可用 vif 实现点击切换，
                // 这里为了简化先直接展示全文；业务方可自行封装带状态的组件）
                Text {
                    attr {
                        text(msg.content)
                        fontSize(12f)
                        color(Color(0xFF4C1D95))
                        lineHeight(18f)
                    }
                }
            }
        )
    }
}
