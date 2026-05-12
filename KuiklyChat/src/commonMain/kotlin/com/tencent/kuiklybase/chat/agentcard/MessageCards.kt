package com.tencent.kuiklybase.chat.agentcard

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.views.*

/**
 * Agent 文本消息卡片（assistant 流式文本）。
 *
 * 比 ChatBubble 更简洁，左侧头像 + 右侧白色气泡。
 */
fun ViewContainer<*, *>.TextMessageCard(
    card: AgentCard.TextMessage,
    theme: AgentCardTheme = AgentCardTheme.Default
) {
    View {
        attr {
            marginLeft(theme.cardMarginH)
            marginRight(theme.cardMarginH)
            marginTop(theme.cardMarginV)
            marginBottom(theme.cardMarginV)
            flexDirectionRow()
            alignItems(FlexAlign.FLEX_START)
        }
        // 气泡
        View {
            attr {
                flex(1f)
                paddingLeft(12f)
                paddingRight(12f)
                paddingTop(10f)
                paddingBottom(10f)
                borderRadius(BorderRectRadius(2f, 10f, 10f, 10f))
                backgroundColor(Color(theme.cardBackground))
                border(Border(1f, BorderStyle.SOLID, Color(theme.borderColor)))
            }
            Text {
                attr {
                    text(card.content + if (card.streaming) " ▎" else "")
                    fontSize(14f)
                    color(Color(theme.bodyColor))
                    lineHeight(20f)
                }
            }
        }
    }
}

/**
 * 用户消息卡片（右对齐，渐变蓝）。
 */
fun ViewContainer<*, *>.UserMessageCard(
    card: AgentCard.UserMessage,
    theme: AgentCardTheme = AgentCardTheme.Default
) {
    View {
        attr {
            marginLeft(theme.cardMarginH)
            marginRight(theme.cardMarginH)
            marginTop(theme.cardMarginV)
            marginBottom(theme.cardMarginV)
            flexDirectionRow()
            alignItems(FlexAlign.FLEX_END)
            justifyContentFlexEnd()
        }
        View {
            attr {
                paddingLeft(12f)
                paddingRight(12f)
                paddingTop(10f)
                paddingBottom(10f)
                borderRadius(BorderRectRadius(10f, 2f, 10f, 10f))
                backgroundLinearGradient(
                    Direction.TO_RIGHT,
                    ColorStop(Color(0xFF6366F1), 0f),
                    ColorStop(Color(0xFF8B5CF6), 1f)
                )
            }
            Text {
                attr {
                    text(card.content)
                    fontSize(14f)
                    color(Color.WHITE)
                    lineHeight(20f)
                }
            }
        }
    }
}

/**
 * 业务自定义卡片（兜底）。
 */
fun ViewContainer<*, *>.CustomCard(
    card: AgentCard.Custom,
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
            paddingLeft(12f)
            paddingRight(12f)
            paddingTop(10f)
            paddingBottom(10f)
        }
        View {
            attr {
                flexDirectionRow()
                alignItemsCenter()
                marginBottom(if (card.payload.isEmpty()) 0f else 6f)
            }
            Text {
                attr {
                    text("✨")
                    fontSize(13f)
                    marginRight(6f)
                }
            }
            View {
                attr {
                    paddingLeft(6f)
                    paddingRight(6f)
                    paddingTop(2f)
                    paddingBottom(2f)
                    borderRadius(4f)
                    backgroundColor(Color(0xFFFEF3C7))
                    marginRight(8f)
                }
                Text {
                    attr {
                        text(card.type)
                        fontSize(10f)
                        color(Color(0xFF92400E))
                        fontWeightSemiBold()
                    }
                }
            }
            Text {
                attr {
                    text(card.title.ifEmpty { "Custom" })
                    fontSize(13f)
                    color(Color(theme.titleColor))
                    fontWeightMedium()
                    flex(1f)
                }
            }
        }
        card.payload.forEach { (key, value) ->
            View {
                attr {
                    flexDirectionRow()
                    paddingTop(3f)
                    paddingBottom(3f)
                }
                Text {
                    attr {
                        text("$key:")
                        fontSize(11f)
                        color(Color(theme.mutedColor))
                        width(80f)
                    }
                }
                Text {
                    attr {
                        text(value)
                        fontSize(11f)
                        color(Color(theme.bodyColor))
                        flex(1f)
                    }
                }
            }
        }
    }
}
