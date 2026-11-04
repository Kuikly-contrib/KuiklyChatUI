package com.kuikly.kuiklychat.dsl.chat

import com.kuikly.kuiklychat.module.KRSseModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmErrorCode
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmHttpRequest
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmStreamSink
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmSubscription
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmTransport

/**
 * Demo adapter for [KRSseModule].
 *
 * Published Kuikly 2.15 `NetworkModule` does not implement streaming HTTP, so the demo
 * calls the host `KRSseModule` copied from KuiklyUI main.
 */
internal class KuiklyNetworkLlmTransport(
    private val sseModule: KRSseModule,
) : LlmTransport {

    override fun openStream(request: LlmHttpRequest, sink: LlmStreamSink): LlmSubscription {
        if (request.method != "GET" && request.method != "POST") {
            sink.onError(LlmErrorCode.INVALID_REQUEST, "Kuikly stream transport only supports GET and POST")
            return LlmSubscription { }
        }

        val body = try {
            request.body.takeIf { it.isNotBlank() }?.let(::JSONObject)
        } catch (error: Throwable) {
            sink.onError(LlmErrorCode.INVALID_REQUEST, "Request body must be a JSON object: ${error.message.orEmpty()}")
            return LlmSubscription { }
        }

        return KuiklyStreamCall(
            module = sseModule,
            request = request,
            body = body,
            sink = sink,
        ).open()
    }
}

private class KuiklyStreamCall(
    private val module: KRSseModule,
    private val request: LlmHttpRequest,
    private val body: JSONObject?,
    private val sink: LlmStreamSink,
) {
    private var handle: KRSseModule.StreamRequestHandle? = null
    private var closed = false

    fun open(): LlmSubscription {
        val headers = request.headers.takeIf { it.isNotEmpty() }?.let { values ->
            JSONObject().apply {
                values.forEach { (name, value) -> put(name, value) }
            }
        }
        handle = module.httpStreamRequest(
            url = request.url,
            method = request.method,
            param = body,
            headers = headers,
            timeoutSeconds = timeoutSeconds(request.timeoutMs),
        ) { event, data, statusCode ->
            if (!closed) handleEvent(event, data, statusCode)
        }
        return LlmSubscription { close() }
    }

    private fun handleEvent(event: String, data: String, statusCode: Int) {
        when (event) {
            EVENT_DATA -> sink.onChunk(data)
            EVENT_COMPLETE -> {
                closed = true
                sink.onComplete()
            }
            EVENT_ERROR -> {
                closed = true
                val httpStatus = statusCode.takeIf { it > 0 }
                val message = data.ifBlank { "Streaming request failed" }
                sink.onError(errorCode(httpStatus, message), message)
            }
        }
    }

    private fun close() {
        if (closed) return
        closed = true
        handle?.close()
        handle = null
    }
}

private fun timeoutSeconds(timeoutMs: Long): Int =
    ((timeoutMs.coerceAtLeast(1L) + 999L) / 1_000L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

private fun errorCode(statusCode: Int?, message: String): LlmErrorCode = when {
    statusCode == 401 || statusCode == 403 -> LlmErrorCode.AUTH
    statusCode == 408 || message.contains("timeout", ignoreCase = true) -> LlmErrorCode.TIMEOUT
    statusCode == 429 -> LlmErrorCode.RATE_LIMIT
    statusCode != null && statusCode in 400..499 -> LlmErrorCode.INVALID_REQUEST
    statusCode != null && statusCode >= 500 -> LlmErrorCode.SERVER
    else -> LlmErrorCode.NETWORK
}

private const val EVENT_DATA = "data"
private const val EVENT_COMPLETE = "complete"
private const val EVENT_ERROR = "error"
