package com.tencent.kuiklybase.chat.compose.core

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.ui.Modifier

/** Optional custom renderer for a message or a custom message part. */
fun interface MessageRenderer {
    @Composable
    fun Render(
        message: ChatMessage,
        groupStatus: MessageGroupStatus,
        theme: ChatTheme,
        modifier: Modifier
    )
}

const val AGENT_CARD_RENDERER_KEY = "__agent_card__"

class MessageRendererRegistry(
    private val renderers: Map<String, MessageRenderer> = emptyMap(),
    private val fallbackRenderer: MessageRenderer? = null
) {
    fun rendererFor(message: ChatMessage): MessageRenderer? {
        val agentCard = message.parts.filterIsInstance<MessagePart.AgentCardContent>().firstOrNull()
        if (agentCard != null) return renderers[AGENT_CARD_RENDERER_KEY]
        val type = message.parts.filterIsInstance<MessagePart.Custom>().firstOrNull()?.type
        if (type != null) {
            return renderers[type] ?: fallbackRenderer
        }
        return null
    }

    fun plus(type: String, renderer: MessageRenderer): MessageRendererRegistry {
        return MessageRendererRegistry(renderers + (type to renderer), fallbackRenderer)
    }

    fun withFallback(renderer: MessageRenderer): MessageRendererRegistry {
        return MessageRendererRegistry(renderers, renderer)
    }

    companion object {
        val Empty = MessageRendererRegistry()
    }
}
