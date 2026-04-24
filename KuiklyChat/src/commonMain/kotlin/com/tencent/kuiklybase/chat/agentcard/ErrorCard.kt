package com.tencent.kuiklybase.chat.agentcard

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.views.*

/**
 * 错误提示卡片。
 */
fun ViewContainer<*, *>.ErrorCard(
    card: AgentCard.Error,
    theme: AgentCardTheme = AgentCardTheme.Default,
    onRetry: (() -> Unit)? = null
) {
    View {
        attr {
            marginLeft(theme.cardMarginH)
            marginRight(theme.cardMarginH)
            marginTop(theme.cardMarginV)
            marginBottom(theme.cardMarginV)
            borderRadius(theme.cardRadius)
            backgroundColor(Color(0xFFFEF2F2))
            border(Border(1f, BorderStyle.SOLID, Color(0xFFFECACA)))
            flexDirectionRow()
            paddingLeft(12f)
            paddingRight(12f)
            paddingTop(10f)
            paddingBottom(10f)
        }
        View {
            attr { flex(1f) }
            Text {
                attr {
                    text(card.message)
                    fontSize(13f)
                    color(Color(0xFFB91C1C))
                    fontWeightSemiBold()
                    lineHeight(18f)
                }
            }
            if (card.code.isNotEmpty()) {
                View {
                    attr {
                        marginTop(4f)
                        alignSelfFlexStart()
                        paddingLeft(6f)
                        paddingRight(6f)
                        paddingTop(2f)
                        paddingBottom(2f)
                        borderRadius(3f)
                        backgroundColor(Color(0xFFFEE2E2))
                    }
                    Text {
                        attr {
                            text(card.code)
                            fontSize(10f)
                            color(Color(0xFFB91C1C))
                        }
                    }
                }
            }
            if (card.detail.isNotEmpty()) {
                Text {
                    attr {
                        text(card.detail)
                        fontSize(11f)
                        color(Color(0xFF991B1B))
                        marginTop(6f)
                        lineHeight(16f)
                    }
                }
            }
        }
    }
}
