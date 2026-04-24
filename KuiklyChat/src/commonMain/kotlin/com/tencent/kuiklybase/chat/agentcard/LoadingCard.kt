package com.tencent.kuiklybase.chat.agentcard

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.*
import com.tencent.kuiklybase.chat.widget.BouncingDots

/**
 * 加载中占位卡片。
 *
 * 支持四种样式：
 * - DOTS：三个跳动的点
 * - SPINNER：旋转加载
 * - PROGRESS：进度条
 * - SKELETON：骨架屏
 */
fun ViewContainer<*, *>.LoadingCard(
    card: AgentCard.Loading,
    theme: AgentCardTheme = AgentCardTheme.Default
) {
    addChild(LoadingCardView()) {
        attr {
            cardData = card
            this.theme = theme
        }
    }
}

internal class LoadingCardView : ComposeView<LoadingCardAttr, ComposeEvent>() {
    override fun createAttr() = LoadingCardAttr()
    override fun createEvent() = ComposeEvent()

    /** 动画状态：0..2，供 SPINNER 和 PROGRESS 样式使用 */
    private var dotPhase by observable(0)
    private var timerSeq = 0

    override fun created() {
        super.created()
        if (attr.cardData.style == LoadingStyle.SPINNER ||
            attr.cardData.style == LoadingStyle.PROGRESS
        ) {
            startAnimation()
        }
    }

    private fun startAnimation() {
        timerSeq++
        val mySeq = timerSeq
        fun tick() {
            if (mySeq != timerSeq) return
            dotPhase = (dotPhase + 1) % 3
            setTimeout(400) { tick() }
        }
        setTimeout(400) { tick() }
    }

    override fun viewDestroyed() {
        super.viewDestroyed()
        timerSeq++
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
                    backgroundColor(Color(theme.cardBackground))
                    border(Border(1f, BorderStyle.SOLID, Color(theme.borderColor)))
                    paddingLeft(12f)
                    paddingRight(12f)
                    paddingTop(12f)
                    paddingBottom(12f)
                    flexDirectionRow()
                    alignItemsCenter()
                }

                // 左侧动画
                when (card.style) {
                    LoadingStyle.DOTS -> {
                        View {
                            attr {
                                marginRight(10f)
                            }
                            BouncingDots {
                                attr {
                                    dotColor = 0xFF6366F1
                                    dotSize = 8f
                                    dotGap = 3f
                                }
                            }
                        }
                    }
                    LoadingStyle.SPINNER -> {
                        Text {
                            attr {
                                text(when (ctx.dotPhase) {
                                    0 -> "◐"; 1 -> "◓"; 2 -> "◑"; else -> "◒"
                                })
                                fontSize(20f)
                                color(Color(0xFF6366F1))
                                marginRight(10f)
                            }
                        }
                    }
                    LoadingStyle.PROGRESS, LoadingStyle.SKELETON -> {
                        Text {
                            attr {
                                text("◌")
                                fontSize(18f)
                                color(Color(0xFFF59E0B))
                                marginRight(10f)
                            }
                        }
                    }
                }

                // 右侧文字
                View {
                    attr { flex(1f) }
                    Text {
                        attr {
                            text(card.title)
                            fontSize(13f)
                            color(Color(theme.titleColor))
                            fontWeightMedium()
                        }
                    }
                    if (card.subtitle.isNotEmpty()) {
                        Text {
                            attr {
                                text(card.subtitle)
                                fontSize(11f)
                                color(Color(theme.mutedColor))
                                marginTop(2f)
                            }
                        }
                    }
                    if (card.target.isNotEmpty()) {
                        Text {
                            attr {
                                text("🎯 ${card.target}")
                                fontSize(10f)
                                color(Color(theme.linkColor))
                                marginTop(3f)
                            }
                        }
                    }
                    if (card.style == LoadingStyle.PROGRESS) {
                        // 无限滚动进度条（模拟）
                        View {
                            attr {
                                height(3f)
                                marginTop(6f)
                                borderRadius(1.5f)
                                backgroundColor(Color(0xFFE2E8F0))
                                flexDirectionRow()
                            }
                            View {
                                attr {
                                    height(3f)
                                    borderRadius(1.5f)
                                    backgroundColor(Color(0xFF6366F1))
                                    flex(
                                        when (ctx.dotPhase) {
                                            0 -> 0.3f; 1 -> 0.6f; 2 -> 0.9f; else -> 0.3f
                                        }
                                    )
                                }
                            }
                            View { attr { flex(1f) } }
                        }
                    }
                }
            }
        }
    }
}

internal class LoadingCardAttr : ComposeAttr() {
    var cardData: AgentCard.Loading by observable(
        AgentCard.Loading(id = "", title = "")
    )
    var theme: AgentCardTheme by observable(AgentCardTheme.Default)
}
