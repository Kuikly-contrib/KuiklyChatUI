package com.tencent.kuiklybase.chat.kuiklyai.provider

import com.tencent.kuiklybase.chat.agui.AgUiEvent
import com.tencent.kuiklybase.chat.agui.AgUiEventParser
import com.tencent.kuiklybase.chat.agui.AgUiRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * 把现有 AG-UI 协议栈接进 [LlmProvider] 契约。
 *
 * 复用 `agui/` 包已有的事件解析与协议类型，不重复造轮子；
 * 网络仍然走宿主注入的 [LlmTransport]，所以这个 provider 同样不带任何
 * 厂商依赖——换后端只改 [endpoint]。
 *
 * ## 关于富事件
 *
 * [LlmProvider] 的契约是 `Flow<String>`，只能承载纯文本增量，
 * 工具调用、思考过程、任务规划这些结构化事件塞不进去。
 * 需要渲染富卡片的宿主可以挂 [onEvent]，拿到解析后的 [AgUiEvent] 自行驱动
 * `AgUiCardSession`，同时文本仍按标准契约流给 KuiklyAiChatView。
 * 这样"简单场景开箱即用、复杂场景不封顶"两件事可以同时成立。
 *
 * @param transport 传输实现，由宿主注入
 * @param endpoint AG-UI agent 的完整 URL
 * @param threadId 会话线程 ID
 * @param headers 附加请求头（鉴权等）
 * @param onEvent 每个解析成功的 AG-UI 事件都会回调一次，用于驱动富卡片渲染
 */
class AgUiLlmProvider(
    private val transport: LlmTransport,
    private val endpoint: String,
    private val threadId: String = "kuiklyai_thread",
    private val headers: Map<String, String> = emptyMap(),
    private val timeoutMs: Long = 120_000L,
    clock: LlmClock = LlmClock.Unavailable,
    private val onEvent: ((AgUiEvent) -> Unit)? = null
) : BaseLlmProvider(clock, idPrefix = "agui") {

    private val runIds = LlmIdGenerator("run")

    override fun streamCompletion(context: List<LlmChatMessage>): Flow<String> = flow {
        val request = LlmHttpRequest(
            url = endpoint,
            method = "POST",
            headers = buildMap {
                put("Content-Type", "application/json")
                put("Accept", "text/event-stream")
                putAll(headers)
            },
            body = buildRunAgentInput(context),
            timeoutMs = timeoutMs
        )

        val decoder = SseDecoder()
        transport.streamAsFlow(request).collect { chunk ->
            decoder.decode(chunk).forEach { payload ->
                emitDeltaFor(payload)
            }
        }
        decoder.flush().forEach { payload -> emitDeltaFor(payload) }
    }

    /**
     * 解析一条 SSE payload，把文本增量发出去，并把事件透传给 [onEvent]。
     *
     * 解析失败的 payload 直接跳过而不是中断整条流——AG-UI 后端常在流里
     * 夹带心跳或自定义字段，为一条不认识的行整轮对话失败不值得。
     */
    private suspend fun FlowCollector<String>.emitDeltaFor(payload: String) {
        val event = runCatching { AgUiEventParser.parseEvent(payload) }.getOrNull() ?: return
        onEvent?.invoke(event)

        when (event) {
            is AgUiEvent.TextMessageContent -> if (event.delta.isNotEmpty()) emit(event.delta)
            is AgUiEvent.TextMessageChunk -> if (event.delta.isNotEmpty()) emit(event.delta)
            is AgUiEvent.RunError -> throw LlmProviderException(
                classify(event.code),
                event.message.ifEmpty { "Agent 运行失败" }
            )
            else -> Unit
        }
    }

    private fun classify(code: String): LlmErrorCode = when (code.uppercase()) {
        "TIMEOUT" -> LlmErrorCode.TIMEOUT
        "RATE_LIMIT", "TOO_MANY_REQUESTS" -> LlmErrorCode.RATE_LIMIT
        "UNAUTHORIZED", "FORBIDDEN" -> LlmErrorCode.AUTH
        "INVALID_REQUEST", "BAD_REQUEST" -> LlmErrorCode.INVALID_REQUEST
        "CONTENT_FILTER", "REJECTED" -> LlmErrorCode.REJECTED
        "" -> LlmErrorCode.UNKNOWN
        else -> LlmErrorCode.SERVER
    }

    /**
     * 构造 AG-UI 的 `RunAgentInput` 请求体。
     */
    private fun buildRunAgentInput(context: List<LlmChatMessage>): String {
        val payload = buildJsonObject {
            put("threadId", threadId)
            put("runId", runIds.next())
            putJsonArray("messages") {
                context.forEach { message ->
                    add(
                        buildJsonObject {
                            put("id", message.id)
                            put(
                                "role",
                                if (message.isUser) AgUiRole.USER else AgUiRole.ASSISTANT
                            )
                            put("content", message.text)
                        }
                    )
                }
            }
        }
        return payload.toString()
    }
}
