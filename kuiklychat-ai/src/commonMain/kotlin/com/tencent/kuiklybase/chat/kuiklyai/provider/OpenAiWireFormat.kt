package com.tencent.kuiklybase.chat.kuiklyai.provider

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * OpenAI `/chat/completions` 协议的编解码。
 *
 * 之所以手写 [JsonObject] 而不是用 `@Serializable` 数据类：兼容端点之间
 * 字段差异不小（有的多返回 `reasoning_content`，有的 `delta` 会缺字段，
 * 有的在流末尾塞一个只带 `usage` 的 chunk）。宽松解析比严格反序列化更抗造，
 * 遇到不认识的字段直接忽略，而不是整条流失败。
 *
 * 拆成独立文件是为了让这层纯函数可以脱离网络直接单测。
 */
internal object OpenAiWireFormat {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * 构造 `/chat/completions` 的流式请求体。
     */
    fun buildRequestBody(
        model: String,
        context: List<LlmChatMessage>,
        systemPrompt: String,
        temperature: Double?,
        maxTokens: Int?,
        extraBody: Map<String, String>
    ): String {
        val payload = buildJsonObject {
            put("model", model)
            put("stream", true)
            temperature?.let { put("temperature", it) }
            maxTokens?.let { put("max_tokens", it) }
            extraBody.forEach { (key, value) -> put(key, value) }

            putJsonArray("messages") {
                if (systemPrompt.isNotEmpty()) {
                    add(
                        buildJsonObject {
                            put("role", "system")
                            put("content", systemPrompt)
                        }
                    )
                }
                context.forEach { add(encodeMessage(it)) }
            }
        }
        return payload.toString()
    }

    /**
     * 一条消息 → OpenAI message 对象。
     *
     * 纯文本走 `content: String`；带附件时走 multimodal 的 parts 数组
     * （只有真需要时才用数组形式，避免不支持多模态的端点直接报错）。
     */
    private fun encodeMessage(message: LlmChatMessage): JsonObject = buildJsonObject {
        put("role", if (message.isUser) "user" else "assistant")

        val inlineImages = message.attachments.filter {
            it.kind == LlmAttachmentKind.IMAGE && (it.hasInlineBytes || it.url.isNotEmpty())
        }
        if (inlineImages.isEmpty()) {
            put("content", message.text)
            return@buildJsonObject
        }

        putJsonArray("content") {
            if (message.text.isNotEmpty()) {
                add(
                    buildJsonObject {
                        put("type", "text")
                        put("text", message.text)
                    }
                )
            }
            inlineImages.forEach { attachment ->
                add(
                    buildJsonObject {
                        put("type", "image_url")
                        putJsonObject("image_url") {
                            put("url", imageUrlFor(attachment))
                        }
                    }
                )
            }
        }
    }

    private fun imageUrlFor(attachment: LlmAttachment): String = when {
        attachment.hasInlineBytes -> {
            val mime = attachment.mimeType.ifEmpty { "image/png" }
            "data:$mime;base64,${attachment.bytesBase64}"
        }
        else -> attachment.url
    }

    /**
     * 解析一条 SSE payload，取出增量文本。
     *
     * @return 增量文本；该 chunk 不含正文（例如只带 `usage` 或 `role`）时返回空串
     * @throws LlmProviderException payload 是错误对象，或结构完全无法识别
     */
    fun parseDelta(payload: String): String {
        val element = runCatching { json.parseToJsonElement(payload) }.getOrNull()
            ?: throw LlmProviderException(
                LlmErrorCode.MALFORMED_RESPONSE,
                "无法解析响应片段：${payload.take(200)}"
            )

        val root = element as? JsonObject ?: return ""

        root["error"]?.let { throw errorFrom(it as? JsonObject) }

        val choices = root["choices"] as? JsonArray ?: return ""
        val firstChoice = choices.firstOrNull()?.jsonObject ?: return ""

        // 流式用 delta，非流式（部分端点忽略 stream=true）用 message
        val holder = (firstChoice["delta"] ?: firstChoice["message"])?.jsonObject ?: return ""
        return holder["content"]?.jsonPrimitive?.contentOrNull.orEmpty()
    }

    /**
     * 解析非流式的错误响应体（HTTP 4xx/5xx 时 body 通常是个纯 JSON 错误对象）。
     */
    fun parseErrorBody(body: String): LlmProviderException? {
        val root = runCatching { json.parseToJsonElement(body) }.getOrNull() as? JsonObject
            ?: return null
        val error = root["error"] ?: return null
        return errorFrom(error as? JsonObject)
    }

    private fun errorFrom(error: JsonObject?): LlmProviderException {
        val message = error?.get("message")?.jsonPrimitive?.contentOrNull
            ?: "模型服务返回了未知错误"
        val type = error?.get("type")?.jsonPrimitive?.contentOrNull.orEmpty()
        val code = error?.get("code")?.jsonPrimitive?.contentOrNull.orEmpty()
        return LlmProviderException(classify(type, code, message), message)
    }

    /**
     * 把端点各不相同的错误标识归一到 [LlmErrorCode]。
     *
     * 匹配 type/code 优先，兜底再看 message 关键字——不同厂商的兼容端点
     * 在这两个字段上的一致性很差，只看其中一个会漏。
     */
    private fun classify(type: String, code: String, message: String): LlmErrorCode {
        val haystack = "$type $code $message".lowercase()
        return when {
            haystack.contains("invalid_api_key") ||
                haystack.contains("authentication") ||
                haystack.contains("unauthorized") ||
                haystack.contains("permission") -> LlmErrorCode.AUTH

            haystack.contains("rate_limit") ||
                haystack.contains("quota") ||
                haystack.contains("too many requests") -> LlmErrorCode.RATE_LIMIT

            haystack.contains("content_filter") ||
                haystack.contains("content_policy") ||
                haystack.contains("safety") -> LlmErrorCode.REJECTED

            haystack.contains("timeout") -> LlmErrorCode.TIMEOUT

            haystack.contains("invalid_request") ||
                haystack.contains("invalid parameter") -> LlmErrorCode.INVALID_REQUEST

            haystack.contains("server_error") ||
                haystack.contains("internal") ||
                haystack.contains("overloaded") -> LlmErrorCode.SERVER

            else -> LlmErrorCode.UNKNOWN
        }
    }
}
