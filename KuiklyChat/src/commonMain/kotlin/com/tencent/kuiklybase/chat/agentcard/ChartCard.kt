package com.tencent.kuiklybase.chat.agentcard

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.views.*

/**
 * 图表卡片（条形图）。
 *
 * 简易条形图，用百分比宽度展示数据。后续可扩展 LINE / PIE。
 */
fun ViewContainer<*, *>.ChartCard(
    card: AgentCard.Chart,
    theme: AgentCardTheme = AgentCardTheme.Default
) {
    val maxValue = card.data.maxOfOrNull { it.value } ?: 1f
    val colors = listOf(
        0xFF6366F1L, 0xFF8B5CF6L, 0xFFEC4899L, 0xFFEF4444L,
        0xFFF59E0BL, 0xFF10B981L, 0xFF06B6D4L, 0xFF3B82F6L
    )

    View {
        attr {
            marginLeft(theme.cardMarginH)
            marginRight(theme.cardMarginH)
            marginTop(theme.cardMarginV)
            marginBottom(theme.cardMarginV)
            borderRadius(theme.cardRadius)
            backgroundColor(Color(theme.cardBackground))
            border(Border(1f, BorderStyle.SOLID, Color(theme.borderColor)))
            paddingLeft(12f)
            paddingRight(12f)
            paddingTop(12f)
            paddingBottom(12f)
        }
        // 标题
        View {
            attr {
                flexDirectionRow()
                alignItemsCenter()
                marginBottom(10f)
            }
            Text {
                attr {
                    text("📈")
                    fontSize(14f)
                    marginRight(6f)
                }
            }
            Text {
                attr {
                    text(card.title.ifEmpty { "Chart" })
                    fontSize(13f)
                    color(Color(theme.titleColor))
                    fontWeightSemiBold()
                    flex(1f)
                }
            }
            Text {
                attr {
                    text(when (card.chartType) {
                        ChartType.BAR -> "条形"
                        ChartType.LINE -> "折线"
                        ChartType.PIE -> "饼图"
                    })
                    fontSize(10f)
                    color(Color(theme.mutedColor))
                }
            }
        }

        // 条形图主体
        card.data.forEachIndexed { index, point ->
            val color = colors[index % colors.size]
            val ratio = if (maxValue > 0) point.value / maxValue else 0f

            View {
                attr {
                    flexDirectionRow()
                    alignItemsCenter()
                    marginBottom(6f)
                }
                // 标签
                Text {
                    attr {
                        text(point.label)
                        fontSize(11f)
                        color(Color(theme.bodyColor))
                        width(64f)
                        lines(1)
                    }
                }
                // 条形
                View {
                    attr {
                        flex(1f)
                        height(18f)
                        borderRadius(4f)
                        backgroundColor(Color(0xFFF1F5F9))
                        flexDirectionRow()
                        marginRight(6f)
                    }
                    View {
                        attr {
                            height(18f)
                            borderRadius(4f)
                            backgroundColor(Color(color))
                            flex(ratio.coerceIn(0.01f, 1f))
                        }
                    }
                    View {
                        attr {
                            flex((1f - ratio).coerceIn(0f, 0.99f))
                        }
                    }
                }
                // 数值
                Text {
                    attr {
                        text(formatValue(point.value))
                        fontSize(11f)
                        color(Color(theme.titleColor))
                        fontWeightSemiBold()
                        width(52f)
                    }
                }
            }
        }
    }
}

private fun formatValue(v: Float): String {
    if (v == v.toInt().toFloat()) return v.toInt().toString()
    return ((v * 10).toInt() / 10.0).toString()
}
