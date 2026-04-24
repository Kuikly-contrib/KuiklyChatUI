package com.tencent.kuiklybase.chat.agentcard

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.*

/**
 * 思考过程卡片。
 *
 * 视觉：灰色虚线边框，斜体文本，表示 Agent 的内部推理。
 * 特性：
 * - 流式中默认展开（让用户看到实时推理过程）
 * - 非流式、内容较长时默认折叠（用户通常先看结论，按需展开）
 * - 点击整张卡片即可切换展开/收起（不再有独立按钮）
 */
fun ViewContainer<*, *>.ThinkingCard(
    card: AgentCard.Thinking,
    theme: AgentCardTheme = AgentCardTheme.Default
) {
    addChild(ThinkingCardView()) {
        attr {
            cardData = card
            this.theme = theme
        }
    }
}

internal class ThinkingCardView : ComposeView<ThinkingCardAttr, ThinkingCardEvent>() {
    override fun createAttr() = ThinkingCardAttr()
    override fun createEvent() = ThinkingCardEvent()

    /** 展开/折叠状态（运行时状态） */
    private var expanded by observable(true)
    /** 是否已应用过初始折叠策略（避免 cardData 更新时反复覆盖用户操作） */
    private var initialExpandApplied = false

    override fun created() {
        super.created()
        // 首次创建时按策略决定初始折叠态
        if (!initialExpandApplied) {
            val content = attr.cardData.content
            // 流式过程中默认展开，让用户看到实时推理；否则：> 80 字默认折叠
            expanded = attr.cardData.streaming || content.length <= 80
            initialExpandApplied = true
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            val card = ctx.attr.cardData
            // 整张卡片可点击切换展开态
            View {
                attr {
                    marginLeft(4f)
                    marginRight(4f)
                    marginTop(4f)
                    marginBottom(4f)
                    borderRadius(8f)
                    backgroundColor(Color(0xFFF8FAFC))
                    border(Border(1f, BorderStyle.DASHED, Color(0xFFCBD5E1)))
                    paddingLeft(12f)
                    paddingRight(12f)
                    paddingTop(10f)
                    paddingBottom(10f)
                }
                event {
                    click { ctx.expanded = !ctx.expanded }
                }

                // 头部
                View {
                    attr {
                        flexDirectionRow()
                        alignItemsCenter()
                        marginBottom(if (ctx.expanded) 6f else 0f)
                    }
                    Text {
                        attr {
                            text("深度思考")
                            fontSize(11f)
                            color(Color(0xFF64748B))
                            fontWeightSemiBold()
                            marginRight(8f)
                        }
                    }
                    // 折叠时显示内容预览
                    vif({ !ctx.expanded }) {
                        Text {
                            attr {
                                text(card.content.take(30).let { if (card.content.length > 30) "$it…" else it })
                                fontSize(11f)
                                color(Color(0xFF94A3B8))
                                fontStyleItalic()
                                flex(1f)
                                lines(1)
                            }
                        }
                    }
                    vif({ ctx.expanded }) {
                        View { attr { flex(1f) } }
                    }
                    if (card.streaming) {
                        Text {
                            attr {
                                text("◌ 思考中")
                                fontSize(10f)
                                color(Color(0xFFF59E0B))
                                marginRight(4f)
                            }
                        }
                    }
                    // 展开/收起状态指示（仅视觉提示，无独立点击区域）
                    Text {
                        attr {
                            text(if (ctx.expanded) "▼" else "▶")
                            fontSize(10f)
                            color(Color(0xFF94A3B8))
                        }
                    }
                }

                // 展开态才显示完整内容
                vif({ ctx.expanded }) {
                    Text {
                        attr {
                            text(card.content + if (card.streaming) " ▎" else "")
                            fontSize(13f)
                            color(Color(0xFF475569))
                            fontStyleItalic()
                            lineHeight(19f)
                        }
                    }
                }
            }
        }
    }
}

internal class ThinkingCardAttr : ComposeAttr() {
    var cardData: AgentCard.Thinking by observable(
        AgentCard.Thinking(id = "", content = "")
    )
    var theme: AgentCardTheme by observable(AgentCardTheme.Default)
}

internal class ThinkingCardEvent : ComposeEvent()
