package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.chat.compose.core.MessageGroupStatus
import com.tencent.kuiklybase.chat.compose.core.MessagePart

/** Optional custom renderer for a message or a custom message part. */
public fun interface MessageRenderer {
    @Composable
    public fun Render(
        message: ChatMessage,
        groupStatus: MessageGroupStatus,
        theme: ChatTheme,
        modifier: Modifier
    )
}

public const val AGENT_CARD_RENDERER_KEY: String = "__agent_card__"

public class MessageRendererRegistry(
    private val renderers: Map<String, MessageRenderer> = emptyMap(),
    private val fallbackRenderer: MessageRenderer? = null
) {
    public fun rendererFor(message: ChatMessage): MessageRenderer? {
        val agentCard = message.parts.filterIsInstance<MessagePart.AgentCardContent>().firstOrNull()
        if (agentCard != null) return renderers[AGENT_CARD_RENDERER_KEY]
        val type = message.parts.filterIsInstance<MessagePart.Custom>().firstOrNull()?.type
        if (type != null) {
            return renderers[type] ?: fallbackRenderer
        }
        return null
    }

    public fun plus(type: String, renderer: MessageRenderer): MessageRendererRegistry {
        return MessageRendererRegistry(renderers + (type to renderer), fallbackRenderer)
    }

    public fun withFallback(renderer: MessageRenderer): MessageRendererRegistry {
        return MessageRendererRegistry(renderers, fallbackRenderer)
    }

    public companion object {
        public val Empty: MessageRendererRegistry = MessageRendererRegistry()
    }
}
