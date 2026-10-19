package com.kuikly.kuiklychat.dsl.chat

import com.tencent.kuikly.core.module.CallbackRef
import com.tencent.kuikly.core.module.NetworkModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmErrorCode
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmHttpRequest
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmStreamSink
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmSubscription
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmTransport

/**
 * Demo adapter for Kuikly NetworkModule's streaming HTTP contract.
 *
 * The common contract lives in KuiklyUI's core NetworkModule; each native renderer implements
 * the corresponding httpStreamRequest and closeStreamRequest methods.
 */
internal class KuiklyNetworkLlmTransport(
    private val networkModule: NetworkModule,
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
            module = networkModule,
            request = request,
            body = body,
            sink = sink,
        ).open()
    }
}

private class KuiklyStreamCall(
    private val module: NetworkModule,
    private val request: LlmHttpRequest,
    private val body: JSONObject?,
    private val sink: LlmStreamSink,
) {
    private val requestId = nextRequestId()
    private var callbackRef: CallbackRef? = null
    private var terminalBeforeRefAssigned = false
    private var closed = false

    fun open(): LlmSubscription {
        val params = JSONObject().apply {
            put("url", request.url)
            put("method", request.method)
            body?.let { put("param", it) }
            if (request.headers.isNotEmpty()) {
                put("headers", JSONObject().apply {
                    request.headers.forEach { (name, value) -> put(name, value) }
                })
            }
            put("timeout", timeoutSeconds(request.timeoutMs))
            put("requestId", requestId)
        }

        val returnValue = module.toNative(
            keepCallbackAlive = true,
            methodName = METHOD_HTTP_STREAM_REQUEST,
            param = params.toString(),
            callback = { event ->
                if (event != null && !closed) handleEvent(event)
            },
            syncCall = false,
        )
        callbackRef = returnValue.callbackRef
        if (terminalBeforeRefAssigned) {
            callbackRef?.let(module::removeCallback)
            callbackRef = null
        }
        return LlmSubscription { close() }
    }

    private fun handleEvent(event: JSONObject) {
        when (event.optString("event", "")) {
            EVENT_DATA -> sink.onChunk(event.optString("data", ""))
            EVENT_COMPLETE -> {
                finish()
                sink.onComplete()
            }
            EVENT_ERROR -> {
                val statusCode = event.optInt("statusCode", 0).takeIf { it > 0 }
                val message = event.optString("data", "").ifBlank { "Streaming request failed" }
                finish()
                sink.onError(errorCode(statusCode, message), message)
            }
        }
    }

    private fun finish() {
        if (closed) return
        closed = true
        val ref = callbackRef
        if (ref == null) {
            terminalBeforeRefAssigned = true
        } else {
            module.removeCallback(ref)
            callbackRef = null
        }
    }

    private fun close() {
        if (closed) return
        closed = true
        module.toNative(
            keepCallbackAlive = false,
            methodName = METHOD_CLOSE_STREAM_REQUEST,
            param = JSONObject().apply { put("requestId", requestId) }.toString(),
            callback = null,
            syncCall = false,
        )
        callbackRef?.let(module::removeCallback)
        callbackRef = null
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

private var streamRequestCounter = 0L

private fun nextRequestId(): String {
    streamRequestCounter += 1
    return "kuikly_chat_stream_$streamRequestCounter"
}

private const val METHOD_HTTP_STREAM_REQUEST = "httpStreamRequest"
private const val METHOD_CLOSE_STREAM_REQUEST = "closeStreamRequest"
private const val EVENT_DATA = "data"
private const val EVENT_COMPLETE = "complete"
private const val EVENT_ERROR = "error"
