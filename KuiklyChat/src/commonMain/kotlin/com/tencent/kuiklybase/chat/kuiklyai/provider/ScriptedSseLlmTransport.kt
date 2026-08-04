package com.tencent.kuiklybase.chat.kuiklyai.provider

/**
 * In-memory [LlmTransport] that replays scripted SSE payloads for tests and demos.
 *
 * Delivery is synchronous so KMP unit tests can [advanceUntilIdle] without platform threads.
 */
class ScriptedSseLlmTransport(
    private val payloads: List<String>
) : LlmTransport {
    override fun openStream(request: LlmHttpRequest, sink: LlmStreamSink): LlmSubscription {
        var cancelled = false
        val subscription = LlmSubscription { cancelled = true }
        for (payload in payloads) {
            if (cancelled) break
            sink.onChunk(payload)
        }
        if (!cancelled) sink.onComplete()
        return subscription
    }
}
