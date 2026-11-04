package com.tencent.kuiklybase.chat.android

import android.util.Log
import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.Semaphore
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.json.JSONObject

/**
 * Android SSE transport copied from KuiklyUI main `KRNetworkModule` stream methods.
 *
 * Register with [MODULE_NAME] from `registerExternalModule`. Kotlin callers use
 * `com.kuikly.kuiklychat.module.KRSseModule`.
 */
class KRSseModule : KuiklyRenderBaseModule() {
    private class StreamRequest(val requestId: String) {
        @Volatile
        var finished = false
        var connection: HttpURLConnection? = null
    }

    private val streamRequests = ConcurrentHashMap<String, StreamRequest>()
    private val streamRequestLock = Any()

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            METHOD_HTTP_STREAM_REQUEST -> {
                openStream(params, callback)
                null
            }
            METHOD_CLOSE_STREAM_REQUEST -> {
                closeStreamRequest(params)
                null
            }
            else -> super.call(method, params, callback)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val requests = synchronized(streamRequestLock) {
            streamRequests.values.toList().also { streamRequests.clear() }
        }
        requests.forEach(::cancelStreamRequest)
    }

    private fun openStream(params: String?, callback: KuiklyRenderCallback?) {
        val requestId = parseJsonObject(params).optString("requestId")
        val request = StreamRequest(requestId)
        val rejectReason = synchronized(streamRequestLock) {
            when {
                requestId.isEmpty() -> "requestId is empty"
                streamRequests.containsKey(requestId) -> "duplicate requestId"
                !streamSlots.tryAcquire() -> TOO_MANY_STREAMS_MESSAGE
                else -> {
                    streamRequests[requestId] = request
                    null
                }
            }
        }
        if (rejectReason != null) {
            callback?.invoke(streamErrorEvent(rejectReason))
            return
        }
        try {
            streamExecutor.execute { httpStreamRequest(request, params, callback) }
        } catch (_: RejectedExecutionException) {
            markStreamFinished(request)
            callback?.invoke(streamErrorEvent(TOO_MANY_STREAMS_MESSAGE))
        }
    }

    private fun httpStreamRequest(
        request: StreamRequest,
        params: String?,
        callback: KuiklyRenderCallback?,
    ) {
        val paramsJSON = parseJsonObject(params)
        val url = paramsJSON.optString("url")
        val method = paramsJSON.optString("method")
        val param = paramsJSON.optJSONObject("param")
        val header = paramsJSON.optJSONObject("headers")
        val cookie = paramsJSON.optString("cookie")
        val timeoutS = paramsJSON.optInt("timeout", DEFAULT_TIMEOUT_S)
        if (request.finished) return

        var reader: InputStreamReader? = null
        var errorStream: InputStream? = null
        var connection: HttpURLConnection? = null
        try {
            val requestUrl = if (method == "GET") createStreamRequestUrl(url, param) else url
            val conn = URL(requestUrl).openConnection() as HttpURLConnection
            connection = conn
            val timeoutMs = if (timeoutS > 0) timeoutS * 1000 else DEFAULT_TIMEOUT_S * 1000
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.useCaches = false
            conn.doInput = true
            conn.requestMethod = method.ifBlank { "GET" }
            addStreamHeaders(conn, header, cookie)
            val body = prepareStreamBody(conn, method, header, param)
            if (!attachStreamConnection(request, conn)) return
            conn.connect()
            if (request.finished) return
            if (body != null) {
                DataOutputStream(conn.outputStream).use { out -> out.write(body) }
            }
            if (request.finished) return
            val responseCode = conn.responseCode
            if (request.finished) return
            val headers = try {
                headerFieldsToJson(conn.headerFields)
            } catch (error: Throwable) {
                Log.e(MODULE_NAME, "stream headerFields to json exception: $error")
                ""
            }
            if (responseCode in 200..299) {
                reader = InputStreamReader(conn.inputStream, Charsets.UTF_8)
                var isFirstCallback = true
                val buffer = CharArray(8192)
                while (true) {
                    val charsRead = reader.read(buffer)
                    if (charsRead == -1 || request.finished) break
                    val chunk = String(buffer, 0, charsRead)
                    val eventData = mutableMapOf<String, Any>(
                        KEY_STREAM_EVENT to STREAM_EVENT_DATA,
                        KEY_STREAM_DATA to chunk,
                    )
                    if (isFirstCallback) {
                        isFirstCallback = false
                        eventData[KEY_HEADERS] = headers
                        eventData[KEY_STATUS_CODE] = responseCode
                    }
                    callback?.invoke(eventData)
                }
                if (claimStreamTerminal(request)) {
                    callback?.invoke(
                        mapOf(
                            KEY_STREAM_EVENT to STREAM_EVENT_COMPLETE,
                            KEY_STREAM_DATA to "",
                        ),
                    )
                }
            } else {
                errorStream = conn.errorStream
                val errorMsg = readStreamErrorBody(errorStream).ifEmpty { "HTTP error $responseCode" }
                if (claimStreamTerminal(request)) {
                    callback?.invoke(
                        mapOf(
                            KEY_STREAM_EVENT to STREAM_EVENT_ERROR,
                            KEY_STREAM_DATA to errorMsg,
                            KEY_HEADERS to headers,
                            KEY_STATUS_CODE to responseCode,
                        ),
                    )
                }
            }
        } catch (error: Exception) {
            Log.e(MODULE_NAME, "Stream request error: $error")
            if (claimStreamTerminal(request)) {
                val isTimeout = error is SocketTimeoutException
                callback?.invoke(
                    streamErrorEvent(
                        if (isTimeout) "request timeout" else (error.message ?: "io exception"),
                        if (isTimeout) STATE_CODE_TIMEOUT else STATE_CODE_UNKNOWN,
                    ),
                )
            }
        } finally {
            markStreamFinished(request)
            try {
                reader?.close()
                errorStream?.close()
                connection?.disconnect()
            } catch (error: IOException) {
                Log.e(MODULE_NAME, "Stream request close error: $error")
            }
        }
    }

    private fun readStreamErrorBody(inputStream: InputStream?): String {
        inputStream ?: return ""
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        try {
            while (output.size() < MAX_STREAM_ERROR_BODY_BYTES) {
                val toRead = minOf(buffer.size, MAX_STREAM_ERROR_BODY_BYTES - output.size())
                val len = inputStream.read(buffer, 0, toRead)
                if (len == -1) break
                output.write(buffer, 0, len)
            }
        } catch (error: IOException) {
            Log.e(MODULE_NAME, "Stream error body read error: $error")
        }
        return String(output.toByteArray(), Charsets.UTF_8)
    }

    private fun streamErrorEvent(message: String, statusCode: Int = STATE_CODE_UNKNOWN): Map<String, Any> {
        return mapOf(
            KEY_STREAM_EVENT to STREAM_EVENT_ERROR,
            KEY_STREAM_DATA to message,
            KEY_STATUS_CODE to statusCode,
        )
    }

    private fun attachStreamConnection(request: StreamRequest, connection: HttpURLConnection): Boolean {
        synchronized(streamRequestLock) {
            if (request.finished) return false
            request.connection = connection
            return true
        }
    }

    private fun claimStreamTerminal(request: StreamRequest): Boolean = markStreamFinished(request)

    private fun markStreamFinished(request: StreamRequest): Boolean {
        synchronized(streamRequestLock) {
            if (request.finished) return false
            request.finished = true
            streamRequests.remove(request.requestId, request)
            streamSlots.release()
            return true
        }
    }

    private fun cancelStreamRequest(request: StreamRequest) {
        val connection = synchronized(streamRequestLock) {
            markStreamFinished(request)
            request.connection
        }
        try {
            connection?.disconnect()
        } catch (error: Exception) {
            Log.e(MODULE_NAME, "Close stream request error: $error")
        }
    }

    private fun closeStreamRequest(params: String?) {
        val requestId = parseJsonObject(params).optString("requestId")
        val request = synchronized(streamRequestLock) { streamRequests.remove(requestId) } ?: return
        cancelStreamRequest(request)
    }

    companion object {
        const val MODULE_NAME = "KRSseModule"
        private const val METHOD_HTTP_STREAM_REQUEST = "httpStreamRequest"
        private const val METHOD_CLOSE_STREAM_REQUEST = "closeStreamRequest"
        private const val KEY_HEADERS = "headers"
        private const val KEY_STATUS_CODE = "statusCode"
        private const val KEY_STREAM_EVENT = "event"
        private const val KEY_STREAM_DATA = "data"
        private const val STREAM_EVENT_DATA = "data"
        private const val STREAM_EVENT_COMPLETE = "complete"
        private const val STREAM_EVENT_ERROR = "error"
        private const val STATE_CODE_UNKNOWN = -1000
        private const val STATE_CODE_TIMEOUT = -1001
        private const val DEFAULT_TIMEOUT_S = 30
        private const val MAX_CONCURRENT_STREAMS = 8
        private const val MAX_STREAM_ERROR_BODY_BYTES = 64 * 1024
        private const val MAX_STREAM_THREADS = MAX_CONCURRENT_STREAMS * 4
        private const val TOO_MANY_STREAMS_MESSAGE =
            "too many concurrent stream requests (max $MAX_CONCURRENT_STREAMS)"
        private val streamThreadIndex = AtomicInteger()
        private val streamSlots = Semaphore(MAX_CONCURRENT_STREAMS)
        private val streamExecutor: ThreadPoolExecutor by lazy {
            ThreadPoolExecutor(
                0,
                MAX_STREAM_THREADS,
                30L,
                TimeUnit.SECONDS,
                SynchronousQueue(),
            ) { runnable ->
                Thread(runnable, "KRSseStream-${streamThreadIndex.incrementAndGet()}").apply {
                    isDaemon = true
                }
            }
        }
    }
}
