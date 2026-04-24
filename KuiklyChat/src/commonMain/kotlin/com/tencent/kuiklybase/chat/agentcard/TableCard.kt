package com.tencent.kuiklybase.chat.agentcard

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.views.*

/**
 * 表格卡片。
 *
 * 支持横向滚动（列数多时），表头深色+表格交替行底色。
 */
fun ViewContainer<*, *>.TableCard(
    card: AgentCard.Table,
    theme: AgentCardTheme = AgentCardTheme.Default
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
        // 标题
        if (card.title.isNotEmpty()) {
            View {
                attr {
                    paddingLeft(12f)
                    paddingRight(12f)
                    paddingTop(8f)
                    paddingBottom(8f)
                    backgroundColor(Color(0xFFF8FAFC))
                    borderRadius(BorderRectRadius(
                        theme.cardRadius, theme.cardRadius, 0f, 0f
                    ))
                    flexDirectionRow()
                    alignItemsCenter()
                }
                Text {
                    attr {
                        text("📊")
                        fontSize(13f)
                        marginRight(6f)
                    }
                }
                Text {
                    attr {
                        text(card.title)
                        fontSize(13f)
                        color(Color(theme.titleColor))
                        fontWeightSemiBold()
                    }
                }
            }
        }

        // 表格主体
        Scroller {
            attr {
                flexDirectionRow()
                paddingLeft(0f)
            }
            View {
                attr { flexDirectionColumn() }
                // 表头
                View {
                    attr {
                        flexDirectionRow()
                        backgroundColor(Color(0xFFF1F5F9))
                    }
                    card.headers.forEach { header ->
                        View {
                            attr {
                                minWidth(100f)
                                paddingLeft(10f)
                                paddingRight(10f)
                                paddingTop(8f)
                                paddingBottom(8f)
                            }
                            Text {
                                attr {
                                    text(header)
                                    fontSize(12f)
                                    color(Color(theme.titleColor))
                                    fontWeightSemiBold()
                                }
                            }
                        }
                    }
                }
                // 数据行
                card.rows.forEachIndexed { rowIndex, row ->
                    View {
                        attr {
                            flexDirectionRow()
                            backgroundColor(
                                if (rowIndex % 2 == 0) Color(theme.cardBackground)
                                else Color(0xFFFAFBFC)
                            )
                            borderTop(Border(1f, BorderStyle.SOLID, Color(0xFFF1F5F9)))
                        }
                        row.forEach { cell ->
                            View {
                                attr {
                                    minWidth(100f)
                                    paddingLeft(10f)
                                    paddingRight(10f)
                                    paddingTop(8f)
                                    paddingBottom(8f)
                                }
                                Text {
                                    attr {
                                        text(cell)
                                        fontSize(12f)
                                        color(Color(theme.bodyColor))
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
}
