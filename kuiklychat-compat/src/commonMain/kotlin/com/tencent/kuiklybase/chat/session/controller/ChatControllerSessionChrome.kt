package com.tencent.kuiklybase.chat.session.controller

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuiklybase.chat.experience.ChatSuggestion
import com.tencent.kuiklybase.chat.experience.ChatWelcomeState

internal fun defaultControllerSuggestions(): List<ChatSuggestion> = listOf(
    ChatSuggestion(
        text = "今晚吃什么",
        prompt = "今晚两个人在家吃饭，帮我挑三道半小时能做好的家常菜，并写出做法要点。",
    ),
    ChatSuggestion(
        text = "周末怎么过",
        prompt = "这个周末想在家附近轻松走走，帮我排一个不赶路的两天安排。",
    ),
    ChatSuggestion(
        text = "列个购物单",
        prompt = "列一份两人三天的买菜清单，以家常菜为主，少买加工食品。",
    ),
    ChatSuggestion(
        text = "回一条消息",
        prompt = "朋友问我周末有没有空一起吃饭，帮我写一条自然的回复：周六晚上可以。",
    ),
)

internal fun ChatControllerSessionView.sessionHeader(container: ViewContainer<*, *>) {
    val host = this
    container.View {
        attr { flexDirection(FlexDirection.COLUMN) }
        event { click { host.dismissComposerKeyboard() } }
        View {
            attr {
                val reported = maxOf(host.pagerData.statusBarHeight, host.pagerData.safeAreaInsets.top)
                // Empty views are dropped, so the safe area has to be margin on the title row.
                marginTop(if (reported >= 20f) reported else 32f)
                height(44f)
                flexDirection(FlexDirection.ROW)
                alignItemsCenter()
                paddingLeft(8f)
                paddingRight(8f)
            }
            vif({ host.sessionAttr().conversationHost != null }) {
                View {
                    attr { width(44f); height(44f); allCenter() }
                    event {
                        click {
                            host.openConversationDrawer()
                        }
                    }
                    menuMark(host.sessionAttr().theme.incomingTextColor)
                }
            }
            Text {
                attr {
                    val title = if (host.sessionAttr().conversationHost != null) {
                        host.sessionAttr().conversationTitle
                    } else {
                        host.sessionAttr().headerTitle
                    }
                    text(title.ifBlank { host.sessionAttr().headerTitle })
                    fontSize(17f)
                    fontWeightMedium()
                    color(Color(host.sessionAttr().theme.incomingTextColor))
                    flex(1f)
                    textAlignCenter()
                    marginRight(if (host.sessionAttr().conversationHost != null) 44f else 0f)
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.menuMark(color: Long) {
    Canvas({ attr { size(20f, 20f) } }) { context, width, height ->
        val scale = minOf(width, height) / 20f
        context.strokeStyle(Color(color))
        context.lineWidth(1.8f * scale)
        context.lineCapRound()
        context.beginPath()
        context.moveTo(3f * scale, 5f * scale)
        context.lineTo(17f * scale, 5f * scale)
        context.stroke()
        context.beginPath()
        context.moveTo(3f * scale, 10f * scale)
        context.lineTo(17f * scale, 10f * scale)
        context.stroke()
        context.beginPath()
        context.moveTo(3f * scale, 15f * scale)
        context.lineTo(17f * scale, 15f * scale)
        context.stroke()
    }
}

internal fun ChatControllerSessionView.sessionWelcome(container: ViewContainer<*, *>) {
    val host = this
    container.View {
        attr {
            positionAbsolute()
            top(0f)
            left(0f)
            right(0f)
            bottom(0f)
        }
        event {
            click { host.dismissComposerKeyboard() }
        }
        ChatWelcomeState {
            val theme = host.sessionAttr().theme
            title = host.sessionAttr().welcomeTitle
            subtitle = host.sessionAttr().welcomeSubtitle
            heroText = ""
            suggestions = host.sessionAttr().suggestions
            primaryColor = theme.accentColor
            surfaceColor = theme.composerColor
            titleColor = theme.incomingTextColor
            subtitleColor = theme.timeTextColor
            borderColor = theme.borderColor
            onSuggestionClick = { host.sendPrompt(it.prompt) }
        }
    }
}
