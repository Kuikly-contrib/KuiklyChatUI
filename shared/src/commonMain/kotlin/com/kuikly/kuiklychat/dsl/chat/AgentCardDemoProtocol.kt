package com.kuikly.kuiklychat.dsl.chat

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuiklybase.chat.compose.core.ChatError
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatStreamEvent
import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.compose.core.MessageStatus
import com.tencent.kuiklybase.chat.kuiklyai.provider.SseDecoder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal const val A2UI_PART_TYPE: String = "a2ui"
internal const val A2UI_JSONL_KEY: String = "jsonl"
private const val KIMI_DEFAULT_MODEL = "kimi-k2.6"
/**
 * Streams a Kimi reply as one assistant message whose parts mirror the reply order:
 * Markdown text segments and A2UI card segments (one `Custom("a2ui")` part per surface).
 */
internal fun agentCardDemoEvents(
    message: ChatMessage,
    apiKey: String,
    model: String = KIMI_DEFAULT_MODEL,
    prompt: String = message.text,
    history: List<KimiTurn> = emptyList(),
): Flow<ChatStreamEvent> = flow {
    val replyId = "a2ui-${message.id}"
    if (apiKey.isBlank()) {
        emit(ChatStreamEvent.ReplaceMessage(replyMessage(replyId, offlineParts(), MessageStatus.STREAMING)))
        emit(ChatStreamEvent.Completed)
        return@flow
    }

    val parser = A2uiMixedReplyParser()
    val sse = SseDecoder()
    val rawReply = StringBuilder()
    var lastParts: List<MessagePart> = emptyList()

    suspend fun publish() {
        val parts = parser.parts()
        if (parts == lastParts) return
        lastParts = parts
        emit(ChatStreamEvent.ReplaceMessage(replyMessage(replyId, parts, MessageStatus.STREAMING)))
    }

    fun take(delta: String?) {
        if (delta == null) return
        rawReply.append(delta)
        parser.feed(delta)
    }

    try {
        streamKimiA2ui(apiKey, model.ifBlank { KIMI_DEFAULT_MODEL }, prompt, history).collect { chunk ->
            sse.decode(chunk).forEach { take(parseKimiDelta(it)) }
            publish()
        }
        sse.flush().forEach { take(parseKimiDelta(it)) }
        parser.finish()
        logAgentCardDemo("llm_response", rawReply.toString())
        if (!parser.hasContent) throw IllegalStateException("模型没有返回内容")
        publish()
        emit(ChatStreamEvent.Completed)
    } catch (error: Throwable) {
        if (rawReply.isNotEmpty()) logAgentCardDemo("llm_response", rawReply.toString())
        emit(ChatStreamEvent.Failed(ChatError(error.message ?: "Kimi 请求失败", code = "kimi")))
    }
}

/** Debug replay of a captured reply (text and/or JSONL); runs the same parser. */
internal fun replayA2uiEvents(message: ChatMessage, reply: String): Flow<ChatStreamEvent> = flow {
    val parser = A2uiMixedReplyParser()
    parser.feed(reply)
    parser.finish()
    emit(ChatStreamEvent.ReplaceMessage(replyMessage("a2ui-${message.id}", parser.parts(), MessageStatus.STREAMING)))
    emit(ChatStreamEvent.Completed)
}

private fun parseKimiDelta(payload: String): String? {
    val json = runCatching { JSONObject(payload) }.getOrNull() ?: return null
    json.optJSONObject("error")?.let { error ->
        throw IllegalStateException(error.optString("message").ifEmpty { "Kimi 返回错误" })
    }
    return json.optJSONArray("choices")
        ?.optJSONObject(0)
        ?.optJSONObject("delta")
        ?.optString("content")
        ?.takeIf { it.isNotEmpty() }
}

internal fun replyMessage(id: String, parts: List<MessagePart>, status: MessageStatus): ChatMessage = ChatMessage(
    id = id,
    author = ChatUser(id = "agent", name = "Agent"),
    parts = parts,
    status = status,
)

private fun offlineParts(): List<MessagePart> {
    val parser = A2uiMixedReplyParser()
    parser.feed("未配置 Kimi API Key，下面是一张离线示例卡片。\n" + OFFLINE_SAMPLE_JSONL)
    parser.finish()
    return parser.parts()
}

private val OFFLINE_SAMPLE_JSONL = """
{"version":"v0.9","createSurface":{"surfaceId":"offline_sample","catalogId":"https://a2ui.org/specification/v0_9/catalogs/basic/catalog.json"}}
{"version":"v0.9","updateComponents":{"surfaceId":"offline_sample","components":[{"id":"root","component":"Card","child":"body"},{"id":"body","component":"Column","children":["title","summary"]},{"id":"title","component":"Text","text":"未配置 Kimi API Key","variant":"h3"},{"id":"summary","component":"Text","text":"在 local.properties 中设置 KIMI_API_KEY 后重新编译。","variant":"body"}]}}
""".trimIndent()
