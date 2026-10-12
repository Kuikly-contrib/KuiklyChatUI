package com.tencent.kuiklybase.chat.kuiklyai

import com.tencent.kuiklybase.chat.kuiklyai.history.LlmChatSerializer
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmAttachment
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmAttachmentKind
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmChatMessage
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmMessageOrigin
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmMessageState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 历史序列化往返测试。
 */
class LlmChatSerializerTest {

    private val sample = listOf(
        LlmChatMessage(
            id = "m1",
            origin = LlmMessageOrigin.USER,
            text = "帮我看看这段代码",
            createdAt = 1_700_000_000_000L,
            extra = mapOf("source" to "voice")
        ),
        LlmChatMessage(
            id = "m2",
            origin = LlmMessageOrigin.LLM,
            text = "好的，问题在第 3 行。",
            createdAt = 1_700_000_001_000L
        )
    )

    @Test
    fun roundTripPreservesCoreFields() {
        val restored = LlmChatSerializer.decode(LlmChatSerializer.encode(sample))
        assertEquals(sample, restored)
    }

    @Test
    fun encodesVersionField() {
        assertTrue(LlmChatSerializer.encode(sample).contains("\"version\":1"))
    }

    @Test
    fun emptyAndBlankInputDecodeToEmptyList() {
        assertEquals(emptyList(), LlmChatSerializer.decode(""))
        assertEquals(emptyList(), LlmChatSerializer.decode("   "))
    }

    @Test
    fun garbageInputDecodesToEmptyListWithoutThrowing() {
        // 恢复历史失败最多是少了上下文，不该让页面崩掉
        assertEquals(emptyList(), LlmChatSerializer.decode("this is not json"))
        assertEquals(emptyList(), LlmChatSerializer.decode("[1,2,3]"))
    }

    @Test
    fun futureVersionIsRejected() {
        val futureFormat = """{"version":999,"messages":[{"id":"x","origin":"USER"}]}"""
        assertEquals(emptyList(), LlmChatSerializer.decode(futureFormat))
    }

    @Test
    fun streamingMessageIsRestoredAsCanceled() {
        // 一条卡在 STREAMING 的消息会让 UI 永远转圈，
        // 而它对应的请求早就随进程结束了
        val streaming = listOf(
            LlmChatMessage(
                id = "m1",
                origin = LlmMessageOrigin.LLM,
                text = "半句话",
                state = LlmMessageState.STREAMING
            )
        )
        val restored = LlmChatSerializer.decode(LlmChatSerializer.encode(streaming))
        assertEquals(LlmMessageState.CANCELED, restored.single().state)
    }

    @Test
    fun failedStateAndErrorSurvive() {
        val failed = listOf(
            LlmChatMessage(
                id = "m1",
                origin = LlmMessageOrigin.LLM,
                state = LlmMessageState.FAILED,
                error = "rate limited"
            )
        )
        val restored = LlmChatSerializer.decode(LlmChatSerializer.encode(failed)).single()
        assertEquals(LlmMessageState.FAILED, restored.state)
        assertEquals("rate limited", restored.error)
    }

    @Test
    fun attachmentMetadataSurvivesButInlineBytesAreDropped() {
        val messages = listOf(
            LlmChatMessage(
                id = "m1",
                origin = LlmMessageOrigin.USER,
                attachments = listOf(
                    LlmAttachment(
                        id = "a1",
                        kind = LlmAttachmentKind.IMAGE,
                        name = "photo.png",
                        mimeType = "image/png",
                        url = "https://example.com/photo.png",
                        bytesBase64 = "QUJDREVG",
                        sizeBytes = 2048L
                    )
                )
            )
        )
        val restored = LlmChatSerializer.decode(LlmChatSerializer.encode(messages))
        val attachment = restored.single().attachments.single()

        assertEquals("a1", attachment.id)
        assertEquals(LlmAttachmentKind.IMAGE, attachment.kind)
        assertEquals("photo.png", attachment.name)
        assertEquals("https://example.com/photo.png", attachment.url)
        assertEquals(2048L, attachment.sizeBytes)
        // 刻意不落盘：一张图能把历史撑到几 MB
        assertEquals("", attachment.bytesBase64)
    }

    @Test
    fun unknownEnumValuesFallBackInsteadOfDroppingMessage() {
        val payload = """
            {"version":1,"messages":[
              {"id":"m1","origin":"MARTIAN","text":"hi","state":"WAT"}
            ]}
        """.trimIndent()
        val restored = LlmChatSerializer.decode(payload).single()
        assertEquals(LlmMessageOrigin.LLM, restored.origin)
        assertEquals(LlmMessageState.DONE, restored.state)
        assertEquals("hi", restored.text)
    }

    @Test
    fun emptyHistoryRoundTrips() {
        assertEquals(emptyList(), LlmChatSerializer.decode(LlmChatSerializer.encode(emptyList())))
    }
}
