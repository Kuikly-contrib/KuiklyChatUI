package com.tencent.kuiklybase.chat.kuiklyai.provider

/**
 * Test-only transport that emits the first chunk then waits until cancelled.
 */
class HangingSseLlmTransport(
    private val firstPayload: String
) : LlmTransport {
    override fun openStream(request: LlmHttpRequest, sink: LlmStreamSink): LlmSubscription {
        var cancelled = false
        sink.onChunk(firstPayload)
        return LlmSubscription { cancelled = true }
    }
}
