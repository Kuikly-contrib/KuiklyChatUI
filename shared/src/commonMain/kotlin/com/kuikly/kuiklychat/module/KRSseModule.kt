package com.kuikly.kuiklychat.module

import com.tencent.kuikly.core.module.CallbackRef
import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * Kotlin side of the SSE stream module.
 *
 * Published Kuikly 2.15 `NetworkModule` has no `httpStreamRequest`. This module keeps the
 * same method names and event payload as KuiklyUI main, and the host registers a native
 * `KRSseModule` on Android, iOS, and HarmonyOS.
 */
internal class KRSseModule : Module() {
    override fun moduleName(): String = MODULE_NAME

    fun httpStreamRequest(
        url: String,
        method: String,
        param: JSONObject?,
        headers: JSONObject?,
        timeoutSeconds: Int,
        eventCallback: (event: String, data: String, statusCode: Int) -> Unit,
    ): StreamRequestHandle {
        val requestId = "sse_${streamRequestIdCounter++}"
        val params = JSONObject().apply {
            put("url", url)
            put("method", method)
            param?.let { put("param", it) }
            headers?.let { put("headers", it) }
            put("timeout", timeoutSeconds)
            put("requestId", requestId)
        }
        var callbackRef: CallbackRef? = null
        var terminalEventBeforeRefAssigned = false
        val returnValue = toNative(
            keepCallbackAlive = true,
            methodName = METHOD_HTTP_STREAM_REQUEST,
            param = params.toString(),
            callback = { result ->
                val event = result?.optString("event", "").orEmpty()
                val data = result?.optString("data", "").orEmpty()
                val statusCode = result?.optInt("statusCode", 0) ?: 0
                val isTerminal = event == EVENT_COMPLETE || event == EVENT_ERROR
                try {
                    if (event.isNotEmpty()) eventCallback(event, data, statusCode)
                } finally {
                    if (isTerminal) {
                        val ref = callbackRef
                        if (ref != null) {
                            removeCallback(ref)
                        } else {
                            terminalEventBeforeRefAssigned = true
                        }
                    }
                }
            },
            syncCall = false,
        )
        callbackRef = returnValue.callbackRef
        if (terminalEventBeforeRefAssigned) {
            callbackRef?.let(::removeCallback)
        }
        return StreamRequestHandle(this, callbackRef, requestId)
    }

    class StreamRequestHandle internal constructor(
        private val module: KRSseModule,
        private val callbackRef: CallbackRef?,
        private val requestId: String,
    ) {
        private var closed = false

        fun close() {
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
        }
    }

    companion object {
        const val MODULE_NAME = "KRSseModule"
        private const val METHOD_HTTP_STREAM_REQUEST = "httpStreamRequest"
        private const val METHOD_CLOSE_STREAM_REQUEST = "closeStreamRequest"
        private const val EVENT_COMPLETE = "complete"
        private const val EVENT_ERROR = "error"
        private var streamRequestIdCounter = 0L
    }
}
