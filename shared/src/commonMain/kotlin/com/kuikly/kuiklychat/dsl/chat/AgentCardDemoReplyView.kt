package com.kuikly.kuiklychat.dsl.chat

import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.views.View
import com.tencent.kuiklybase.KuiklyMarkdown
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.config.MarkdownColors
import com.tencent.kuiklybase.config.MarkdownConfig
import com.tencent.kuiklybase.config.MarkdownTypography
import com.tencent.kuiklybase.config.TextStyleConfig
import com.tencent.kuiklybase.config.FontWeight

/** Text segments between cards; follows the theme's body size like the session's Markdown bubble. */
internal fun mixedReplyMarkdownConfig(theme: ChatTheme): MarkdownConfig {
    val size = theme.fontSize
    val body = TextStyleConfig(fontSize = size, color = theme.incomingTextColor, lineHeight = size + 8f)
    fun heading(delta: Float) = body.copy(fontSize = size + delta, lineHeight = size + delta + 8f, fontWeight = FontWeight.Bold)
    return MarkdownConfig(
        colors = MarkdownColors(text = theme.incomingTextColor, linkColor = theme.accentColor),
        typography = MarkdownTypography(
            text = body,
            paragraph = body,
            ordered = body,
            bullet = body,
            list = body,
            h1 = heading(4f),
            h2 = heading(3f),
            h3 = heading(2f),
            h4 = heading(1f),
            h5 = heading(0f),
            h6 = heading(0f),
        ),
    )
}

/**
 * Draws a mixed reply in order: Markdown text segments and A2UI cards.
 * Returns false for messages without cards so the session keeps its default bubble
 * (and its streaming Markdown) for plain-text replies.
 */
internal fun ViewContainer<*, *>.renderMixedReply(
    message: ChatMessage,
    width: Float,
    markdownConfig: MarkdownConfig,
    actions: AgentCardDemoActions,
): Boolean {
    if (a2uiParts(message).isEmpty()) return false
    View {
        attr {
            flexDirection(FlexDirection.COLUMN)
            width(width)
        }
        message.parts.forEachIndexed { index, part ->
            when {
                part is MessagePart.Text -> View {
                    attr {
                        width(width)
                        if (index > 0) marginTop(8f)
                    }
                    KuiklyMarkdown(content = part.value, config = markdownConfig)
                }
                part is MessagePart.Custom && part.type == A2UI_PART_TYPE -> View {
                    attr { if (index > 0) marginTop(10f) }
                    renderCard(message, part, width, actions)
                }
            }
        }
    }
    return true
}

private fun ViewContainer<*, *>.renderCard(
    message: ChatMessage,
    part: MessagePart.Custom,
    width: Float,
    actions: AgentCardDemoActions,
) {
    val jsonl = part.payload[A2UI_JSONL_KEY] ?: return
    val surfaceId = a2uiSurfaceOf(part)
    renderA2uiSurface(
        jsonl = a2uiReplayJsonl(jsonl, part.payload[A2UI_STATE_KEY].orEmpty()),
        width = width,
        onAction = { actionJson -> actions.handle(message.id, surfaceId, actionJson) },
        onSurface = { writeLine -> actions.attach(message.id, surfaceId, writeLine) },
        onDataModelChanged = { model -> actions.saveState(message.id, surfaceId, model) },
    )
}
