package com.tencent.kuiklybase.chat.agentcard

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.views.*

/**
 * 搜索结果卡片。
 *
 * 展示搜索 query + 结果列表（标题/片段/URL/评分）。
 */
fun ViewContainer<*, *>.SearchResultCard(
    card: AgentCard.SearchResult,
    theme: AgentCardTheme = AgentCardTheme.Default,
    onItemClick: ((SearchItem) -> Unit)? = null
) {
    View {
        attr {
            marginLeft(theme.cardMarginH)
            marginRight(theme.cardMarginH)
            marginTop(theme.cardMarginV)
            marginBottom(theme.cardMarginV)
            borderRadius(theme.cardRadius)
            backgroundColor(Color(theme.cardBackground))
            border(Border(1f, BorderStyle.SOLID, Color(theme.borderColor)))
        }

        // 头部
        View {
            attr {
                paddingLeft(12f)
                paddingRight(12f)
                paddingTop(10f)
                paddingBottom(10f)
                backgroundColor(Color(0xFFEFF6FF))
                borderRadius(BorderRectRadius(
                    theme.cardRadius, theme.cardRadius, 0f, 0f
                ))
            }
            View {
                attr {
                    flexDirectionRow()
                    alignItemsCenter()
                    marginBottom(2f)
                }
                Text {
                    attr {
                        text("🔍")
                        fontSize(13f)
                        marginRight(6f)
                    }
                }
                Text {
                    attr {
                        text("搜索")
                        fontSize(11f)
                        color(Color(0xFF1D4ED8))
                        fontWeightSemiBold()
                        flex(1f)
                    }
                }
                Text {
                    attr {
                        text("${card.totalCount} 条结果")
                        fontSize(11f)
                        color(Color(theme.mutedColor))
                    }
                }
            }
            Text {
                attr {
                    text("「${card.query}」")
                    fontSize(13f)
                    color(Color(theme.titleColor))
                    fontWeightMedium()
                }
            }
        }

        // 结果列表
        card.items.forEachIndexed { index, item ->
            View {
                attr {
                    paddingLeft(12f)
                    paddingRight(12f)
                    paddingTop(10f)
                    paddingBottom(10f)
                    if (index > 0) {
                        borderTop(Border(1f, BorderStyle.SOLID, Color(0xFFF1F5F9)))
                    }
                }
                View {
                    attr {
                        flexDirectionRow()
                        alignItemsCenter()
                        marginBottom(4f)
                    }
                    Text {
                        attr {
                            text("${index + 1}.")
                            fontSize(11f)
                            color(Color(theme.mutedColor))
                            marginRight(6f)
                        }
                    }
                    Text {
                        attr {
                            text(item.title)
                            fontSize(13f)
                            color(Color(theme.linkColor))
                            fontWeightMedium()
                            flex(1f)
                            lines(1)
                        }
                    }
                    if (item.score > 0f) {
                        Text {
                            attr {
                                text("⭐ ${formatScore(item.score)}")
                                fontSize(10f)
                                color(Color(theme.warningColor))
                            }
                        }
                    }
                }
                if (item.snippet.isNotEmpty()) {
                    Text {
                        attr {
                            text(item.snippet)
                            fontSize(12f)
                            color(Color(theme.bodyColor))
                            lineHeight(17f)
                            lines(2)
                        }
                    }
                }
                if (item.url.isNotEmpty()) {
                    Text {
                        attr {
                            text(item.url)
                            fontSize(10f)
                            color(Color(theme.mutedColor))
                            marginTop(3f)
                            lines(1)
                        }
                    }
                }
                event {
                    click { onItemClick?.invoke(item) }
                }
            }
        }

        if (card.items.isEmpty()) {
            View {
                attr {
                    paddingLeft(12f)
                    paddingRight(12f)
                    paddingTop(14f)
                    paddingBottom(14f)
                    allCenter()
                }
                Text {
                    attr {
                        text("暂无搜索结果")
                        fontSize(12f)
                        color(Color(theme.mutedColor))
                    }
                }
            }
        }
    }
}

private fun formatScore(score: Float): String {
    val v = (score * 10).toInt() / 10.0
    return v.toString()
}
