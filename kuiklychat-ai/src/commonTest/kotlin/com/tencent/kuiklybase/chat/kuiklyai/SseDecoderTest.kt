package com.tencent.kuiklybase.chat.kuiklyai

import com.tencent.kuiklybase.chat.kuiklyai.provider.SseDecoder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [SseDecoder] 分帧测试。
 *
 * 重点覆盖"chunk 边界任意"这个真实场景——网络层不保证按行给数据，
 * 这类 bug 只在弱网/长响应时复现，靠人工测很难碰到。
 */
class SseDecoderTest {

    @Test
    fun singleCompleteLine() {
        val decoder = SseDecoder()
        assertEquals(listOf("""{"a":1}"""), decoder.decode("data: {\"a\":1}\n\n"))
    }

    @Test
    fun multipleLinesInOneChunk() {
        val decoder = SseDecoder()
        val payloads = decoder.decode("data: one\n\ndata: two\n\ndata: three\n\n")
        assertEquals(listOf("one", "two", "three"), payloads)
    }

    @Test
    fun lineSplitAcrossTwoChunks() {
        val decoder = SseDecoder()
        // 第一个 chunk 只有半行，此时不该吐出任何东西
        assertEquals(emptyList(), decoder.decode("data: {\"cont"))
        assertEquals(listOf("""{"content":"hi"}"""), decoder.decode("ent\":\"hi\"}\n\n"))
    }

    @Test
    fun byteByByteDelivery() {
        val decoder = SseDecoder()
        val source = "data: hello\n\ndata: world\n\n"
        val collected = mutableListOf<String>()
        // 逐字符投喂：最极端的分帧情况
        source.forEach { char -> collected.addAll(decoder.decode(char.toString())) }
        assertEquals(listOf("hello", "world"), collected)
    }

    @Test
    fun skipsCommentsAndBlankLines() {
        val decoder = SseDecoder()
        val payloads = decoder.decode(": heartbeat\n\ndata: real\n\n")
        assertEquals(listOf("real"), payloads)
    }

    @Test
    fun skipsNonDataFields() {
        val decoder = SseDecoder()
        val payloads = decoder.decode("event: message\nid: 42\ndata: payload\n\n")
        assertEquals(listOf("payload"), payloads)
    }

    @Test
    fun handlesCarriageReturnLineEndings() {
        val decoder = SseDecoder()
        assertEquals(listOf("crlf"), decoder.decode("data: crlf\r\n\r\n"))
    }

    @Test
    fun stopsAtDoneMarker() {
        val decoder = SseDecoder()
        val payloads = decoder.decode("data: last\n\ndata: [DONE]\n\ndata: ignored\n\n")
        assertEquals(listOf("last"), payloads)
        assertTrue(decoder.isFinished)
        // 结束后再喂数据也不该有产出
        assertEquals(emptyList(), decoder.decode("data: more\n"))
    }

    @Test
    fun flushEmitsTrailingLineWithoutNewline() {
        val decoder = SseDecoder()
        assertEquals(emptyList(), decoder.decode("data: tail"))
        assertFalse(decoder.isFinished)
        assertEquals(listOf("tail"), decoder.flush())
        // flush 之后缓冲区已清空，重复调用不该重复吐出
        assertEquals(emptyList(), decoder.flush())
    }

    @Test
    fun flushIgnoresDoneMarker() {
        val decoder = SseDecoder()
        decoder.decode("data: [DONE]")
        assertEquals(emptyList(), decoder.flush())
        assertTrue(decoder.isFinished)
    }

    @Test
    fun emptyDataLineProducesNothing() {
        val decoder = SseDecoder()
        assertEquals(emptyList(), decoder.decode("data:\n\ndata: \n\n"))
    }

    @Test
    fun joinsMultipleDataFieldsAndSupportsCrOnly() {
        val decoder = SseDecoder()
        assertEquals(listOf("first\nsecond"), decoder.decode("data: first\rdata: second\r\r"))
    }

    @Test
    fun preservesMeaningfulDataSpacesAndStripsBom() {
        val decoder = SseDecoder()
        assertEquals(listOf("  hello"), decoder.decode("\uFEFFdata:   hello\n\n"))
    }
}
