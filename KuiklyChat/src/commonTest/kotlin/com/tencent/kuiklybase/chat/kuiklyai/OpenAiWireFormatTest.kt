package com.tencent.kuiklybase.chat.kuiklyai

import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmAttachment
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmAttachmentKind
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmChatMessage
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmErrorCode
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmMessageOrigin
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmProviderException
import com.tencent.kuiklybase.chat.kuiklyai.provider.OpenAiWireFormat
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * OpenAI 兼容协议编解码测试。
 *
 * 覆盖两类风险：请求体拼装（多模态 parts 的条件分支）、
 * 响应宽松解析（兼容端点之间的字段差异）。
 */
class OpenAiWireFormatTest {

    private fun user(text: String, attachments: List<LlmAttachment> = emptyList()) =
        LlmChatMessage(
            id = "u1",
            origin = LlmMessageOrigin.USER,
            text = text,
            attachments = attachments
        )

    // ── 请求体 ────────────────────────────────────────────────────────

    @Test
    fun requestBodyCarriesModelAndStreamFlag() {
        val body = OpenAiWireFormat.buildRequestBody(
            model = "gpt-4o-mini",
            context = listOf(user("hi")),
            systemPrompt = "",
            temperature = null,
            maxTokens = null,
            extraBody = emptyMap()
        )
        assertContains(body, "\"model\":\"gpt-4o-mini\"")
        assertContains(body, "\"stream\":true")
        assertContains(body, "\"content\":\"hi\"")
    }

    @Test
    fun systemPromptGoesFirstAndIsOmittedWhenBlank() {
        val withPrompt = OpenAiWireFormat.buildRequestBody(
            model = "m", context = listOf(user("hi")), systemPrompt = "be terse",
            temperature = null, maxTokens = null, extraBody = emptyMap()
        )
        assertContains(withPrompt, "\"role\":\"system\"")
        assertTrue(
            withPrompt.indexOf("system") < withPrompt.indexOf("user"),
            "system 消息必须排在 user 之前"
        )

        val withoutPrompt = OpenAiWireFormat.buildRequestBody(
            model = "m", context = listOf(user("hi")), systemPrompt = "",
            temperature = null, maxTokens = null, extraBody = emptyMap()
        )
        assertTrue(!withoutPrompt.contains("\"role\":\"system\""))
    }

    @Test
    fun optionalParamsOnlyAppearWhenProvided() {
        val bare = OpenAiWireFormat.buildRequestBody(
            model = "m", context = listOf(user("hi")), systemPrompt = "",
            temperature = null, maxTokens = null, extraBody = emptyMap()
        )
        assertTrue(!bare.contains("temperature"))
        assertTrue(!bare.contains("max_tokens"))

        val full = OpenAiWireFormat.buildRequestBody(
            model = "m", context = listOf(user("hi")), systemPrompt = "",
            temperature = 0.7, maxTokens = 512, extraBody = mapOf("top_p" to "0.9")
        )
        assertContains(full, "temperature")
        assertContains(full, "max_tokens")
        assertContains(full, "top_p")
    }

    @Test
    fun plainTextMessageUsesStringContentNotPartsArray() {
        val body = OpenAiWireFormat.buildRequestBody(
            model = "m", context = listOf(user("just text")), systemPrompt = "",
            temperature = null, maxTokens = null, extraBody = emptyMap()
        )
        // 不带附件时必须是字符串形式——数组形式会让不支持多模态的端点报 400
        assertContains(body, "\"content\":\"just text\"")
        assertTrue(!body.contains("image_url"))
    }

    @Test
    fun inlineImageBecomesDataUrlPart() {
        val attachment = LlmAttachment(
            id = "a1",
            kind = LlmAttachmentKind.IMAGE,
            mimeType = "image/jpeg",
            bytesBase64 = "QUJD"
        )
        val body = OpenAiWireFormat.buildRequestBody(
            model = "m", context = listOf(user("look", listOf(attachment))),
            systemPrompt = "", temperature = null, maxTokens = null, extraBody = emptyMap()
        )
        assertContains(body, "image_url")
        assertContains(body, "data:image/jpeg;base64,QUJD")
        assertContains(body, "\"type\":\"text\"")
    }

    @Test
    fun remoteImageUsesUrlDirectly() {
        val attachment = LlmAttachment(
            id = "a1",
            kind = LlmAttachmentKind.IMAGE,
            url = "https://example.com/a.png"
        )
        val body = OpenAiWireFormat.buildRequestBody(
            model = "m", context = listOf(user("", listOf(attachment))),
            systemPrompt = "", temperature = null, maxTokens = null, extraBody = emptyMap()
        )
        assertContains(body, "https://example.com/a.png")
    }

    @Test
    fun nonImageAttachmentDoesNotTriggerPartsArray() {
        val attachment = LlmAttachment(
            id = "a1",
            kind = LlmAttachmentKind.FILE,
            name = "spec.pdf",
            url = "https://example.com/spec.pdf"
        )
        val body = OpenAiWireFormat.buildRequestBody(
            model = "m", context = listOf(user("read this", listOf(attachment))),
            systemPrompt = "", temperature = null, maxTokens = null, extraBody = emptyMap()
        )
        assertContains(body, "\"content\":\"read this\"")
        assertTrue(!body.contains("image_url"))
    }

    // ── 响应解析 ──────────────────────────────────────────────────────

    @Test
    fun parsesStreamingDelta() {
        val payload = """{"choices":[{"delta":{"content":"Hello"}}]}"""
        assertEquals("Hello", OpenAiWireFormat.parseDelta(payload))
    }

    @Test
    fun parsesNonStreamingMessageShape() {
        // 部分端点会忽略 stream=true，直接返回 message 而非 delta
        val payload = """{"choices":[{"message":{"content":"full text"}}]}"""
        assertEquals("full text", OpenAiWireFormat.parseDelta(payload))
    }

    @Test
    fun toleratesChunksWithoutContent() {
        // 首 chunk 常只带 role；末 chunk 常只带 usage / finish_reason
        assertEquals("", OpenAiWireFormat.parseDelta("""{"choices":[{"delta":{"role":"assistant"}}]}"""))
        assertEquals("", OpenAiWireFormat.parseDelta("""{"choices":[{"finish_reason":"stop"}]}"""))
        assertEquals("", OpenAiWireFormat.parseDelta("""{"usage":{"total_tokens":9}}"""))
        assertEquals("", OpenAiWireFormat.parseDelta("""{"choices":[]}"""))
    }

    @Test
    fun malformedJsonThrows() {
        // 截断的 JSON——宽松模式也无法解析，必须报 MALFORMED_RESPONSE
        val e = assertFailsWith<LlmProviderException> {
            OpenAiWireFormat.parseDelta("""{"choices":[{"delta":""")
        }
        assertEquals(LlmErrorCode.MALFORMED_RESPONSE, e.code)
    }

    @Test
    fun errorPayloadClassifiedAsAuth() {
        val payload =
            """{"error":{"message":"Incorrect API key provided","type":"invalid_api_key"}}"""
        val e = assertFailsWith<LlmProviderException> { OpenAiWireFormat.parseDelta(payload) }
        assertEquals(LlmErrorCode.AUTH, e.code)
        assertTrue(!e.code.retryable, "鉴权失败不该让用户重试")
    }

    @Test
    fun errorPayloadClassifiedAsRateLimit() {
        val payload = """{"error":{"message":"Rate limit reached","type":"rate_limit_error"}}"""
        val e = assertFailsWith<LlmProviderException> { OpenAiWireFormat.parseDelta(payload) }
        assertEquals(LlmErrorCode.RATE_LIMIT, e.code)
        assertTrue(e.code.retryable, "限流应当可重试")
    }

    @Test
    fun errorClassificationFallsBackToMessageKeywords() {
        // type 字段缺失时靠 message 关键字兜底——不同厂商在这两个字段上很不一致
        val payload = """{"error":{"message":"insufficient_quota for this account"}}"""
        val e = assertFailsWith<LlmProviderException> { OpenAiWireFormat.parseDelta(payload) }
        assertEquals(LlmErrorCode.RATE_LIMIT, e.code)
    }

    @Test
    fun contentFilterClassifiedAsRejected() {
        val payload = """{"error":{"message":"blocked","type":"content_filter"}}"""
        val e = assertFailsWith<LlmProviderException> { OpenAiWireFormat.parseDelta(payload) }
        assertEquals(LlmErrorCode.REJECTED, e.code)
    }

    @Test
    fun parseErrorBodyReturnsNullForNonErrorPayload() {
        assertEquals(null, OpenAiWireFormat.parseErrorBody("""{"choices":[]}"""))
        assertEquals(null, OpenAiWireFormat.parseErrorBody("garbage"))
    }

    @Test
    fun parseErrorBodyExtractsServerError() {
        val e = OpenAiWireFormat.parseErrorBody(
            """{"error":{"message":"upstream overloaded","type":"server_error"}}"""
        )
        assertEquals(LlmErrorCode.SERVER, e?.code)
    }
}
