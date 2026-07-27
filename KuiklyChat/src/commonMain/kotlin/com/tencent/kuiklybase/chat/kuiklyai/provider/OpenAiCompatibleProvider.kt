package com.tencent.kuiklybase.chat.kuiklyai.provider

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 通用 OpenAI 兼容端点 provider。
 *
 * **不绑定任何厂商**：只要服务端实现了 `POST {baseUrl}/chat/completions`
 * 的 SSE 流式协议就能接——OpenAI、DeepSeek、Kimi、通义、混元、
 * Azure OpenAI、vLLM、Ollama、LM Studio、以及公司内网的自建网关都适用。
 * 换供应商只需要改 [baseUrl] 和 [model] 两个字符串。
 *
 * 网络能力由宿主通过 [LlmTransport] 注入，本类不含任何 HTTP 实现，
 * 因此在 commonMain 里保持平台无关。
 *
 * ```kotlin
 * val provider = OpenAiCompatibleProvider(
 *     transport = KuiklyHttpTransport(pager),   // 宿主实现
 *     baseUrl = "https://api.deepseek.com/v1",
 *     model = "deepseek-chat",
 *     apiKey = BuildConfig.LLM_KEY,
 * )
 * ```
 *
 * @param transport 传输实现，由宿主注入
 * @param baseUrl 端点前缀，不含 `/chat/completions`，尾部斜杠可有可无
 * @param model 模型名
 * @param apiKey 鉴权 key；为空时不发 `Authorization` 头（内网网关常见）
 * @param systemPrompt 每轮请求都会置顶的 system 消息，为空则不发
 * @param extraHeaders 附加请求头，例如 Azure 的 `api-key`、网关的租户标识
 * @param extraBody 附加请求体字段，例如 `top_p`、厂商私有开关
 */
class OpenAiCompatibleProvider(
    private val transport: LlmTransport,
    private val baseUrl: String,
    private val model: String,
    private val apiKey: String = "",
    private val systemPrompt: String = "",
    private val temperature: Double? = null,
    private val maxTokens: Int? = null,
    private val extraHeaders: Map<String, String> = emptyMap(),
    private val extraBody: Map<String, String> = emptyMap(),
    private val timeoutMs: Long = 60_000L,
    clock: LlmClock = LlmClock.Unavailable
) : BaseLlmProvider(clock, idPrefix = "oai") {

    override fun streamCompletion(context: List<LlmChatMessage>): Flow<String> = flow {
        val request = LlmHttpRequest(
            url = "${baseUrl.trimEnd('/')}/chat/completions",
            method = "POST",
            headers = buildHeaders(),
            body = OpenAiWireFormat.buildRequestBody(
                model = model,
                context = context,
                systemPrompt = systemPrompt,
                temperature = temperature,
                maxTokens = maxTokens,
                extraBody = extraBody
            ),
            timeoutMs = timeoutMs
        )

        val decoder = SseDecoder()
        transport.streamAsFlow(request).collect { chunk ->
            decoder.decode(chunk).forEach { payload ->
                val delta = OpenAiWireFormat.parseDelta(payload)
                if (delta.isNotEmpty()) emit(delta)
            }
        }
        decoder.flush().forEach { payload ->
            val delta = OpenAiWireFormat.parseDelta(payload)
            if (delta.isNotEmpty()) emit(delta)
        }
    }

    private fun buildHeaders(): Map<String, String> = buildMap {
        put("Content-Type", "application/json")
        put("Accept", "text/event-stream")
        if (apiKey.isNotEmpty()) put("Authorization", "Bearer $apiKey")
        putAll(extraHeaders)
    }
}
