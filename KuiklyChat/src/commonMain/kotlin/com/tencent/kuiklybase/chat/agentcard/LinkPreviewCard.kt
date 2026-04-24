package com.tencent.kuiklybase.chat.agentcard

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.views.*

/**
 * 链接预览卡片。
 */
fun ViewContainer<*, *>.LinkPreviewCard(
    card: AgentCard.LinkPreview,
    theme: AgentCardTheme = AgentCardTheme.Default,
    onClick: ((String) -> Unit)? = null
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
            flexDirectionRow()
            paddingLeft(12f)
            paddingRight(12f)
            paddingTop(10f)
            paddingBottom(10f)
        }
        // 图标
        View {
            attr {
                size(44f, 44f)
                borderRadius(6f)
                backgroundColor(Color(0xFFF1F5F9))
                allCenter()
                marginRight(10f)
            }
            if (card.iconUrl.isNotEmpty()) {
                Image {
                    attr {
                        size(40f, 40f)
                        borderRadius(4f)
                        src(card.iconUrl)
                        resizeCover()
                    }
                }
            } else {
                Text {
                    attr {
                        text("🔗")
                        fontSize(20f)
                    }
                }
            }
        }
        // 右侧信息
        View {
            attr { flex(1f) }
            if (card.siteName.isNotEmpty()) {
                Text {
                    attr {
                        text(card.siteName)
                        fontSize(10f)
                        color(Color(theme.mutedColor))
                        marginBottom(2f)
                    }
                }
            }
            Text {
                attr {
                    text(card.title.ifEmpty { card.url })
                    fontSize(13f)
                    color(Color(theme.linkColor))
                    fontWeightSemiBold()
                    lines(2)
                    lineHeight(17f)
                }
            }
            if (card.description.isNotEmpty()) {
                Text {
                    attr {
                        text(card.description)
                        fontSize(11f)
                        color(Color(theme.bodyColor))
                        marginTop(3f)
                        lines(2)
                        lineHeight(15f)
                    }
                }
            }
            Text {
                attr {
                    text(card.url)
                    fontSize(10f)
                    color(Color(theme.mutedColor))
                    marginTop(4f)
                    lines(1)
                }
            }
        }
        event {
            click { onClick?.invoke(card.url) }
        }
    }
}
