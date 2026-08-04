package com.tencent.kuiklybase.chat.compose.adapter

import androidx.compose.runtime.Composable
import com.tencent.kuiklybase.chat.agentcard.AgentCard
import com.tencent.kuiklybase.chat.compose.agentcard.ComposeCardCallbacks
import com.tencent.kuiklybase.chat.compose.agentcard.ComposeCardListOptions
import com.tencent.kuiklybase.chat.compose.agentcard.ComposeCardTheme
import com.tencent.kuiklybase.chat.compose.agentcard.ComposeRenderCard
import com.tencent.kuiklybase.chat.compose.core.AGENT_CARD_RENDERER_KEY
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.compose.core.MessageRenderer
import com.tencent.kuiklybase.chat.compose.core.MessageRendererRegistry

/** Bridges custom AG-UI messages into the existing Compose AgentCard renderer. */
fun agentCardRendererRegistry(
    theme: ComposeCardTheme = ComposeCardTheme.Default,
    callbacks: ComposeCardCallbacks = ComposeCardCallbacks(),
    options: ComposeCardListOptions = ComposeCardListOptions()
): MessageRendererRegistry {
    val renderer = MessageRenderer { message, _, _, _ ->
        RenderAgentCard(message, theme, callbacks, options)
    }
    return MessageRendererRegistry.Empty.plus(AGENT_CARD_RENDERER_KEY, renderer)
        .plus("activity", renderer)
        .plus("tool_result", renderer)
        .plus("reasoning", renderer)
        .plus("custom", renderer)
}

@Composable
private fun RenderAgentCard(
    message: ChatMessage,
    theme: ComposeCardTheme,
    callbacks: ComposeCardCallbacks,
    options: ComposeCardListOptions
) {
    val agentCard = message.parts.filterIsInstance<MessagePart.AgentCardContent>().firstOrNull()
    val custom = message.parts.filterIsInstance<MessagePart.Custom>().firstOrNull()
    val card = agentCard?.card ?: custom?.let {
        AgentCard.Custom(
            id = message.id,
            type = it.type,
            title = it.payload["title"].orEmpty(),
            payload = it.payload
        )
    } ?: return
    ComposeRenderCard(
        card = card,
        theme = theme,
        callbacks = callbacks,
        options = options
    )
}
