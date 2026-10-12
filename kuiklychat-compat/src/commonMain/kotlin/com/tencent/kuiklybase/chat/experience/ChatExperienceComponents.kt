package com.tencent.kuiklybase.chat.experience

import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.BoxShadow
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.Blur
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

/**
 * A reusable prompt displayed in an empty chat.
 *
 * [text] is the short label rendered in the chip while [prompt] is the full
 * message delivered to the click callback.
 */
public data class ChatSuggestion(
    val text: String,
    val prompt: String = text,
    val accentColor: Long? = null
)

/**
 * High-level chat experience options shared by [com.tencent.kuiklybase.chat.session.ChatSession].
 */
public class ChatExperienceOptions {
    /** Reactive loading-state provider used before the first message page arrives. */
    public var isLoading: () -> Boolean = { false }

    /** Render the built-in loading state when [isLoading] returns true. */
    public var showDefaultLoadingState: Boolean = true

    /** Text rendered by the built-in loading state. */
    public var loadingText: String = "正在加载消息..."

    /** Render the built-in welcome state when the message list is empty. */
    public var showDefaultEmptyState: Boolean = true

    /** Main empty-state copy. */
    public var emptyStateTitle: String = "开始一段新对话"

    /** Supporting empty-state copy. */
    public var emptyStateSubtitle: String = "输入消息，或从下面的建议开始"

    /** Short glyph rendered in the welcome hero. */
    public var emptyStateHeroText: String = ""

    /** Optional quick prompts rendered below the welcome copy. */
    public var suggestions: List<ChatSuggestion> = emptyList()

    /** Called when a quick prompt is selected. Defaults to onSendMessage when null. */
    public var onSuggestionClick: ((ChatSuggestion) -> Unit)? = null

    /** Show a floating return-to-latest button while the user is away from the bottom. */
    public var showScrollToBottomButton: Boolean = true

    /** Distance from the bottom that is still considered "at bottom". */
    public var scrollToBottomThreshold: Float = 48f
}

/** Minimal loading state that shares the visual language of the welcome state. */
public fun ViewContainer<*, *>.ChatLoadingState(
    text: String = "正在加载消息...",
    textColor: Long = 0xFF888888,
    accentColor: Long = 0xFF4F8FFF
) {
    View {
        attr {
            flex(1f)
            allCenter()
        }
        View {
            attr {
                flexDirection(FlexDirection.ROW)
                alignItems(FlexAlign.CENTER)
            }
            repeat(3) { index ->
                View {
                    attr {
                        size(7f, 7f)
                        borderRadius(3.5f)
                        backgroundColor(Color(accentColor))
                        opacity(1f - index * 0.2f)
                        marginRight(6f)
                    }
                }
            }
            Text {
                attr {
                    text(text)
                    fontSize(14f)
                    color(Color(textColor))
                    marginLeft(4f)
                }
            }
        }
    }
}

/** Styling and content for [ChatWelcomeState]. */
public class ChatWelcomeStateConfig {
    public var title: String = "开始一段新对话"
    public var subtitle: String = "输入消息，或从下面的建议开始"
    public var heroText: String = ""
    public var suggestions: List<ChatSuggestion> = emptyList()
    public var primaryColor: Long = 0xFF4F8FFF
    public var surfaceColor: Long = 0xFFFFFFFF
    public var titleColor: Long = 0xFF222222
    public var subtitleColor: Long = 0xFF888888
    public var borderColor: Long = 0xFFE8E8E8
    public var onSuggestionClick: ((ChatSuggestion) -> Unit)? = null
}

/**
 * Polished, reusable empty state for assistant and messaging experiences.
 */
public fun ViewContainer<*, *>.ChatWelcomeState(config: ChatWelcomeStateConfig.() -> Unit = {}) {
    val cfg = ChatWelcomeStateConfig().apply(config)
    View {
        attr {
            flex(1f)
        }

        View {
            attr {
                flex(1f)
                alignItems(FlexAlign.CENTER)
                justifyContentCenter()
                paddingLeft(24f)
                paddingRight(24f)
            }
                if (cfg.heroText.isNotEmpty()) {
                    View {
                        attr {
                            size(72f, 72f)
                            borderRadius(16f)
                            backgroundColor(Color(cfg.primaryColor))
                            allCenter()
                            marginBottom(16f)
                        }
                        Text {
                            attr {
                                text(cfg.heroText)
                                fontSize(28f)
                                fontWeightBold()
                                color(Color.WHITE)
                            }
                        }
                    }
                }
                Text {
                    attr {
                        text(cfg.title)
                        fontSize(24f)
                        fontWeightBold()
                        color(Color(cfg.titleColor))
                        textAlignCenter()
                    }
                }
            if (cfg.subtitle.isNotEmpty()) {
                Text {
                    attr {
                        text(cfg.subtitle)
                        fontSize(14f)
                        lineHeight(20f)
                        color(Color(cfg.subtitleColor))
                        textAlignCenter()
                        marginTop(8f)
                    }
                }
            }
        }

        if (cfg.suggestions.isNotEmpty()) {
            ChatSuggestionBar(
                suggestions = cfg.suggestions,
                surfaceColor = cfg.surfaceColor,
                textColor = cfg.titleColor,
                borderColor = cfg.borderColor,
                defaultAccentColor = cfg.primaryColor,
                onSuggestionClick = { cfg.onSuggestionClick?.invoke(it) }
            )
        }
    }
}

/**
 * One horizontal row of quick-prompt chips. Extra chips scroll sideways.
 *
 * [defaultAccentColor] stays in the signature so existing call sites compile; the bar
 * does not paint a mark from it.
 */
@Suppress("UNUSED_PARAMETER")
public fun ViewContainer<*, *>.ChatSuggestionBar(
    suggestions: List<ChatSuggestion>,
    surfaceColor: Long = 0xFFFFFFFF,
    textColor: Long = 0xFF222222,
    borderColor: Long = 0xFFE8E8E8,
    defaultAccentColor: Long = 0xFF4F8FFF,
    onSuggestionClick: (ChatSuggestion) -> Unit
) {
    addChild(ChatSuggestionBarView()) {
        attr {
            alignSelfStretch()
            height(56f)
            items.clear()
            items.addAll(suggestions)
            this.surfaceColor = surfaceColor
            this.textColor = textColor
            this.borderColor = borderColor
            this.onSuggestionClick = onSuggestionClick
        }
    }
}

private class ChatSuggestionBarView : ComposeView<ChatSuggestionBarAttr, ComposeEvent>() {
    override fun createAttr(): ChatSuggestionBarAttr = ChatSuggestionBarAttr()
    override fun createEvent(): ComposeEvent = ComposeEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Scroller {
                attr {
                    alignSelfStretch()
                    flex(1f)
                    height(56f)
                    flexDirection(FlexDirection.ROW)
                    alignItems(FlexAlign.CENTER)
                    showScrollerIndicator(false)
                    paddingLeft(16f)
                    paddingRight(16f)
                }
                vfor({ ctx.attr.items }) { suggestion ->
                    View {
                        attr {
                            height(44f)
                            borderRadius(12f)
                            backgroundColor(Color(ctx.attr.surfaceColor))
                            border(Border(0.5f, BorderStyle.SOLID, Color(ctx.attr.borderColor)))
                            flexDirection(FlexDirection.ROW)
                            alignItems(FlexAlign.CENTER)
                            paddingLeft(14f)
                            paddingRight(14f)
                            marginRight(8f)
                        }
                        event {
                            click { ctx.attr.onSuggestionClick?.invoke(suggestion) }
                        }
                        Text {
                            attr {
                                text(suggestion.text)
                                fontSize(14f)
                                fontWeightMedium()
                                color(Color(ctx.attr.textColor))
                            }
                        }
                    }
                }
            }
        }
    }
}

private class ChatSuggestionBarAttr : ComposeAttr() {
    var items by observableList<ChatSuggestion>()
    var surfaceColor by observable(0xFFFFFFFF)
    var textColor by observable(0xFF222222)
    var borderColor by observable(0xFFE8E8E8)
    var onSuggestionClick: ((ChatSuggestion) -> Unit)? = null
}

/** Floating button used by ChatSession to return to the latest message. */
internal class ChatScrollToBottomButtonView : ComposeView<ChatScrollToBottomButtonAttr, ChatScrollToBottomButtonEvent>() {
    override fun createAttr(): ChatScrollToBottomButtonAttr = ChatScrollToBottomButtonAttr()
    override fun createEvent(): ChatScrollToBottomButtonEvent = ChatScrollToBottomButtonEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            vif({ ctx.attr.visible }) {
                View {
                    attr {
                        positionAbsolute()
                        right(ctx.attr.right)
                        bottom(ctx.attr.bottom)
                        size(ctx.attr.size, ctx.attr.size)
                        borderRadius(ctx.attr.size / 2f)
                        border(Border(0.5f, BorderStyle.SOLID, Color(0xB8FFFFFF)))
                        boxShadow(BoxShadow(0f, 4f, 14f, Color(0x24000000)))
                        allCenter()
                        zIndex(4)
                    }
                    event {
                        click { ctx.event.onClick?.invoke() }
                    }
                    ScrollDownFrost(ctx.attr.size)
                    ChevronsDownMark(ctx.attr.iconColor)
                }
            }
        }
    }

    public fun setVisible(visible: Boolean) {
        attr.visible = visible
    }
}

internal class ChatScrollToBottomButtonAttr : ComposeAttr() {
    public var visible: Boolean by observable(false)
    public var size: Float by observable(42f)
    public var right: Float by observable(16f)
    public var bottom: Float by observable(16f)
    public var surfaceColor: Long by observable(0xFFFFFFFF)
    public var borderColor: Long by observable(0xFFE8E8E8)
    public var iconColor: Long by observable(0xFF4F8FFF)
}

internal class ChatScrollToBottomButtonEvent : ComposeEvent() {
    public var onClick: (() -> Unit)? = null
}

internal fun ViewContainer<*, *>.ChatScrollToBottomButton(init: ChatScrollToBottomButtonView.() -> Unit) {
    addChild(ChatScrollToBottomButtonView(), init)
}

/** Circular blur of the content behind the button, plus a translucent white tint. */
internal fun ViewContainer<*, *>.ScrollDownFrost(diameter: Float) {
    Blur {
        attr {
            absolutePositionAllZero()
            borderRadius(diameter / 2f)
            blurRadius(10f)
            backgroundColor(Color(red255 = 255, green255 = 255, blue255 = 255, alpha01 = 0.5f))
        }
    }
}

/** Lucide chevrons-down, same 24×24 path as the Compose scroll-to-bottom button. */
private fun ViewContainer<*, *>.ChevronsDownMark(color: Long) {
    Canvas({
        attr { size(22f, 22f) }
    }) { context, width, height ->
        val scale = minOf(width, height) / 24f
        context.strokeStyle(Color(color))
        context.lineWidth(2f * scale)
        context.lineCapRound()
        context.beginPath()
        context.moveTo(7f * scale, 6f * scale)
        context.lineTo(12f * scale, 11f * scale)
        context.lineTo(17f * scale, 6f * scale)
        context.stroke()
        context.beginPath()
        context.moveTo(7f * scale, 13f * scale)
        context.lineTo(12f * scale, 18f * scale)
        context.lineTo(17f * scale, 13f * scale)
        context.stroke()
    }
}
