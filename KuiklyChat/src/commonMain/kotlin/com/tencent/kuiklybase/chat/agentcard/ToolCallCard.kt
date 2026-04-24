package com.tencent.kuiklybase.chat.agentcard

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.*
import com.tencent.kuiklybase.chat.widget.BouncingDots

/**
 * 工具调用卡片。
 *
 * 视觉：紫色主题，左侧竖线 + 工具名徽章 + 右侧状态指示。
 * 交互：
 * - 点击头部切换结果/错误的展开/折叠
 * - SUCCESS + 结果非空时默认折叠（成功不需要看细节）
 * - FAILED / RUNNING / PENDING 默认展开
 */
fun ViewContainer<*, *>.ToolCallCard(
    card: AgentCard.ToolCall,
    theme: AgentCardTheme = AgentCardTheme.Default
) {
    addChild(ToolCallCardView()) {
        attr {
            cardData = card
            this.theme = theme
        }
    }
}

internal class ToolCallCardView : ComposeView<ToolCallCardAttr, ComposeEvent>() {
    override fun createAttr() = ToolCallCardAttr()
    override fun createEvent() = ComposeEvent()

    /** 结果/错误体展开状态 */
    private var expanded by observable(true)
    /** 是否已应用过初始折叠策略 */
    private var initialExpandApplied = false

    override fun created() {
        super.created()
        if (!initialExpandApplied) {
            val card = attr.cardData
            // SUCCESS 默认折叠（结果已在头部体现）；FAILED / RUNNING / PENDING 默认展开
            expanded = card.state != ToolState.SUCCESS
            initialExpandApplied = true
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            val card = ctx.attr.cardData
            val theme = ctx.attr.theme

            View {
                attr {
                    marginLeft(theme.cardMarginH)
                    marginRight(theme.cardMarginH)
                    marginTop(theme.cardMarginV)
                    marginBottom(theme.cardMarginV)
                    borderRadius(theme.cardRadius)
                    backgroundColor(Color(0xFFFAF5FF))
                    border(Border(1f, BorderStyle.SOLID, Color(0xFFC4B5FD)))
                    flexDirectionRow()
                }
                // 左侧装饰条
                View {
                    attr {
                        width(3f)
                        backgroundColor(Color(0xFF8B5CF6))
                        borderRadius(BorderRectRadius(
                            theme.cardRadius, 0f, 0f, theme.cardRadius
                        ))
                    }
                }

                View {
                    attr {
                        flex(1f)
                        paddingLeft(12f)
                        paddingRight(12f)
                        paddingTop(10f)
                        paddingBottom(10f)
                    }

                    // ===== 头部：工具名 + 运行状态（可点击切换展开态）=====
                    View {
                        attr {
                            flexDirectionRow()
                            alignItemsCenter()
                        }
                        event {
                            click {
                                // 只有存在可展开的结果/错误体时点击才有意义
                                val c = ctx.attr.cardData
                                val hasBody = c.result.isNotEmpty() || c.errorMessage.isNotEmpty()
                                if (hasBody) {
                                    ctx.expanded = !ctx.expanded
                                }
                            }
                        }
                        Text {
                            attr {
                                text("工具调用")
                                fontSize(11f)
                                color(Color(0xFF6B21A8))
                                fontWeightSemiBold()
                                marginRight(8f)
                            }
                        }
                        // 工具名徽章（紧贴内容，不占满）
                        View {
                            attr {
                                paddingLeft(6f)
                                paddingRight(6f)
                                paddingTop(2f)
                                paddingBottom(2f)
                                borderRadius(4f)
                                backgroundColor(Color(0xFF8B5CF6))
                                marginRight(8f)
                            }
                            Text {
                                attr {
                                    text(card.toolName)
                                    fontSize(11f)
                                    color(Color.WHITE)
                                    fontWeightSemiBold()
                                    lines(1)
                                }
                            }
                        }
                        // 中间弹性占位，把右侧状态指示推到最右
                        View { attr { flex(1f) } }
                        // ----- 运行状态 -----
                        // PENDING
                        vif({ ctx.attr.cardData.state == ToolState.PENDING }) {
                            Text {
                                attr {
                                    text("⏳ 等待")
                                    fontSize(11f)
                                    color(Color(0xFF94A3B8))
                                    fontWeightSemiBold()
                                }
                            }
                        }
                        // RUNNING：跳动圆点
                        vif({ ctx.attr.cardData.state == ToolState.RUNNING }) {
                            BouncingDots {
                                attr {
                                    dotColor = 0xFFF59E0B
                                    dotSize = 5f
                                    dotGap = 3f
                                }
                            }
                        }
                        // SUCCESS：✓
                        vif({ ctx.attr.cardData.state == ToolState.SUCCESS }) {
                            Text {
                                attr {
                                    text("✓")
                                    fontSize(13f)
                                    color(Color(0xFF10B981))
                                    fontWeightSemiBold()
                                }
                            }
                        }
                        // FAILED：✗
                        vif({ ctx.attr.cardData.state == ToolState.FAILED }) {
                            Text {
                                attr {
                                    text("✗")
                                    fontSize(13f)
                                    color(Color(0xFFEF4444))
                                    fontWeightSemiBold()
                                }
                            }
                        }
                    }

                    // ===== 结果（展开态 + SUCCESS + 有结果）=====
                    vif({
                        ctx.expanded &&
                            ctx.attr.cardData.state == ToolState.SUCCESS &&
                            ctx.attr.cardData.result.isNotEmpty()
                    }) {
                        View {
                            attr {
                                marginTop(8f)
                                paddingLeft(10f)
                                paddingRight(10f)
                                paddingTop(8f)
                                paddingBottom(8f)
                                borderRadius(6f)
                                backgroundColor(Color(0xFFFFFFFF))
                                border(Border(1f, BorderStyle.SOLID, Color(0xFFE9D5FF)))
                            }
                            Text {
                                attr {
                                    text("Result")
                                    fontSize(10f)
                                    color(Color(0xFF8B5CF6))
                                    fontWeightSemiBold()
                                    marginBottom(4f)
                                }
                            }
                            Text {
                                attr {
                                    text(ctx.attr.cardData.result)
                                    fontSize(13f)
                                    color(Color(theme.bodyColor))
                                    lineHeight(18f)
                                }
                            }
                        }
                    }

                    // ===== 错误（展开态 + FAILED + 有错误信息）=====
                    vif({
                        ctx.expanded &&
                            ctx.attr.cardData.state == ToolState.FAILED &&
                            ctx.attr.cardData.errorMessage.isNotEmpty()
                    }) {
                        View {
                            attr {
                                marginTop(8f)
                                paddingLeft(10f)
                                paddingRight(10f)
                                paddingTop(8f)
                                paddingBottom(8f)
                                borderRadius(6f)
                                backgroundColor(Color(0xFFFEF2F2))
                                border(Border(1f, BorderStyle.SOLID, Color(0xFFFECACA)))
                            }
                            Text {
                                attr {
                                    text("⚠️ ${ctx.attr.cardData.errorMessage}")
                                    fontSize(12f)
                                    color(Color(theme.errorColor))
                                    lineHeight(17f)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

internal class ToolCallCardAttr : ComposeAttr() {
    var cardData: AgentCard.ToolCall by observable(
        AgentCard.ToolCall(id = "", toolName = "", toolCallId = "")
    )
    var theme: AgentCardTheme by observable(AgentCardTheme.Default)
}
