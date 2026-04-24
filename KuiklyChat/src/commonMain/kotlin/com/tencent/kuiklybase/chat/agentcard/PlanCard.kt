package com.tencent.kuiklybase.chat.agentcard

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.*
import com.tencent.kuiklybase.chat.widget.BouncingDots

/**
 * 任务规划卡片。
 *
 * 视觉：淡紫色主题，左上角"📋 规划"图标 + 标题 + 进度徽章（已完成/总数）。
 * 清单按步骤渲染：每行一个圆形状态图标 + 步骤标题，completed 删除线、running 高亮。
 *
 * 交互：
 * - 点击头部切换展开/折叠
 * - 默认展开
 */
fun ViewContainer<*, *>.PlanCard(
    card: AgentCard.Plan,
    theme: AgentCardTheme = AgentCardTheme.Default
) {
    addChild(PlanCardView()) {
        attr {
            cardData = card
            this.theme = theme
        }
    }
}

internal class PlanCardView : ComposeView<PlanCardAttr, ComposeEvent>() {
    override fun createAttr() = PlanCardAttr()
    override fun createEvent() = ComposeEvent()

    /** 展开状态 */
    private var expanded by observable(true)
    /** 是否已应用过初始折叠策略 */
    private var initialExpandApplied = false

    override fun created() {
        super.created()
        if (!initialExpandApplied) {
            // 默认展开；未来可按 步骤数 / 全部完成 情况调整
            expanded = true
            initialExpandApplied = true
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            val card = ctx.attr.cardData
            val theme = ctx.attr.theme
            val done = card.steps.count { it.status == PlanStepStatus.DONE || it.status == PlanStepStatus.SKIPPED }
            val total = card.steps.size
            val allDone = total > 0 && done == total

            View {
                attr {
                    marginLeft(theme.cardMarginH)
                    marginRight(theme.cardMarginH)
                    marginTop(theme.cardMarginV)
                    marginBottom(theme.cardMarginV)
                    borderRadius(theme.cardRadius)
                    backgroundColor(Color(0xFFFAF5FF))
                    border(Border(1f, BorderStyle.SOLID, Color(0xFFDDD6FE)))
                }

                // ===== 头部（可点击切换展开态）=====
                View {
                    attr {
                        flexDirectionRow()
                        alignItemsCenter()
                        paddingLeft(12f)
                        paddingRight(12f)
                        paddingTop(10f)
                        paddingBottom(10f)
                    }
                    event {
                        click {
                            if (ctx.attr.cardData.steps.isNotEmpty()) {
                                ctx.expanded = !ctx.expanded
                            }
                        }
                    }
                    // 图标
                    Text {
                        attr {
                            text("📋")
                            fontSize(15f)
                            marginRight(8f)
                        }
                    }
                    // 标题
                    Text {
                        attr {
                            text(card.title)
                            fontSize(13f)
                            color(Color(0xFF6B21A8))
                            fontWeightSemiBold()
                        }
                    }
                    // 摘要（可选）
                    vif({ ctx.attr.cardData.summary.isNotEmpty() }) {
                        Text {
                            attr {
                                text(" · ${ctx.attr.cardData.summary}")
                                fontSize(11f)
                                color(Color(0xFF8B5CF6))
                                marginLeft(2f)
                            }
                        }
                    }
                    // 弹性占位
                    View { attr { flex(1f) } }
                    // 进度徽章
                    vif({ ctx.attr.cardData.steps.isNotEmpty() }) {
                        View {
                            attr {
                                paddingLeft(6f)
                                paddingRight(6f)
                                paddingTop(2f)
                                paddingBottom(2f)
                                borderRadius(10f)
                                backgroundColor(
                                    if (allDone) Color(0xFF10B981)
                                    else Color(0xFF8B5CF6)
                                )
                            }
                            Text {
                                attr {
                                    text(if (allDone) "✓ 已完成" else "$done / $total")
                                    fontSize(10f)
                                    color(Color.WHITE)
                                    fontWeightSemiBold()
                                }
                            }
                        }
                    }
                }

                // ===== 步骤清单（展开态）=====
                vif({ ctx.expanded && ctx.attr.cardData.steps.isNotEmpty() }) {
                    View {
                        attr {
                            paddingLeft(12f)
                            paddingRight(12f)
                            paddingBottom(10f)
                        }
                        // 注意：这里是静态渲染，steps 更新会随 cardData observable 触发 body 重跑
                        ctx.attr.cardData.steps.forEachIndexed { index, step ->
                            val isLast = index == ctx.attr.cardData.steps.size - 1
                            PlanStepRow(step, isLast, theme)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 单个 Plan 步骤行。
 *
 * 左侧：状态图标（✓ / ○ / ✗ / ⌔）
 * 右侧：标题（DONE 时加删除线）+ 可选详情
 */
private fun ViewContainer<*, *>.PlanStepRow(
    step: PlanStep,
    isLast: Boolean,
    theme: AgentCardTheme
) {
    View {
        attr {
            flexDirectionRow()
            alignItems(com.tencent.kuikly.core.layout.FlexAlign.FLEX_START)
            paddingTop(6f)
            paddingBottom(if (isLast) 0f else 6f)
        }

        // 状态图标容器（圆形徽章）
        View {
            attr {
                size(20f, 20f)
                borderRadius(10f)
                allCenter()
                marginRight(10f)
                marginTop(1f)
                backgroundColor(
                    when (step.status) {
                        PlanStepStatus.DONE -> Color(0xFF10B981)
                        PlanStepStatus.RUNNING -> Color(0xFFF59E0B)
                        PlanStepStatus.FAILED -> Color(0xFFEF4444)
                        PlanStepStatus.SKIPPED -> Color(0xFFCBD5E1)
                        PlanStepStatus.PENDING -> Color(0xFFE9D5FF)
                    }
                )
            }
            when (step.status) {
                PlanStepStatus.DONE -> Text {
                    attr { text("✓"); fontSize(12f); color(Color.WHITE); fontWeightBold() }
                }
                PlanStepStatus.RUNNING -> BouncingDots {
                    attr {
                        dotColor = 0xFFFFFFFF
                        dotSize = 3f
                        dotGap = 2f
                    }
                }
                PlanStepStatus.FAILED -> Text {
                    attr { text("✗"); fontSize(12f); color(Color.WHITE); fontWeightBold() }
                }
                PlanStepStatus.SKIPPED -> Text {
                    attr { text("—"); fontSize(10f); color(Color.WHITE); fontWeightBold() }
                }
                PlanStepStatus.PENDING -> Text {
                    attr { text(""); fontSize(1f) }
                }
            }
        }

        // 文本区
        View {
            attr {
                flex(1f)
            }
            Text {
                attr {
                    text(step.title)
                    fontSize(13f)
                    lineHeight(19f)
                    color(
                        when (step.status) {
                            PlanStepStatus.DONE -> Color(0xFF6E7681)
                            PlanStepStatus.SKIPPED -> Color(0xFF94A3B8)
                            PlanStepStatus.FAILED -> Color(theme.errorColor)
                            PlanStepStatus.RUNNING -> Color(0xFF0F172A)
                            PlanStepStatus.PENDING -> Color(0xFF475569)
                        }
                    )
                    if (step.status == PlanStepStatus.RUNNING) {
                        fontWeightSemiBold()
                    }
                    // 已完成 / 已跳过：加删除线
                    if (step.status == PlanStepStatus.DONE ||
                        step.status == PlanStepStatus.SKIPPED
                    ) {
                        textDecorationLineThrough()
                    }
                }
            }
            if (step.detail.isNotEmpty()) {
                Text {
                    attr {
                        text(step.detail)
                        fontSize(11f)
                        color(Color(0xFF94A3B8))
                        lineHeight(16f)
                        marginTop(2f)
                    }
                }
            }
        }
    }
}

internal class PlanCardAttr : ComposeAttr() {
    var cardData: AgentCard.Plan by observable(
        AgentCard.Plan(id = "")
    )
    var theme: AgentCardTheme by observable(AgentCardTheme.Default)
}
