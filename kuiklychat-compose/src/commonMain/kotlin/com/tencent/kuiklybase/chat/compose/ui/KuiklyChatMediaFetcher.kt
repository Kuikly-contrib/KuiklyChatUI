package com.tencent.kuiklybase.chat.compose.ui

import com.tencent.kuikly.core.module.NetworkModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuiklybase.chat.compose.core.ChatMediaFetcher
import com.tencent.kuiklybase.chat.compose.core.ChatMediaRequest
import com.tencent.kuiklybase.chat.compose.core.decodeInlineMediaSource
import com.tencent.kuiklybase.chat.compose.core.isNetworkMediaSource
import com.tencent.kuiklybase.chat.compose.core.ChatByteCacheModule
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class KuiklyChatMediaFetcher(
    private val networkModule: () -> NetworkModule?,
    private val byteCacheModule: () -> ChatByteCacheModule? = { null },
) : ChatMediaFetcher {
    override suspend fun fetch(
        request: ChatMediaRequest,
        onProgress: suspend (receivedBytes: Long, totalBytes: Long?) -> Unit,
    ): ByteArray {
        val source = request.source.trim()
        if (isNetworkMediaSource(source)) {
            val bytes = fetchNetwork(source, request.headers)
            onProgress(bytes.size.toLong(), bytes.size.toLong())
            return bytes
        }
        decodeInlineMediaSource(source)?.let { bytes ->
            onProgress(bytes.size.toLong(), bytes.size.toLong())
            return bytes
        }
        readLocalMediaFile(source)?.let { bytes ->
            onProgress(bytes.size.toLong(), bytes.size.toLong())
            return bytes
        }
        throw IllegalArgumentException("Unsupported media source")
    }

    private fun readLocalMediaFile(source: String): ByteArray? =
        byteCacheModule()?.readLocalFile(source)

    private suspend fun fetchNetwork(
        source: String,
        headers: Map<String, String>,
    ): ByteArray = suspendCancellableCoroutine { continuation ->
        val module = networkModule()
        if (module == null) {
            continuation.resumeWithException(
                IllegalStateException("NetworkModule is unavailable"),
            )
            return@suspendCancellableCoroutine
        }
        val headerJson = JSONObject().apply {
            headers.forEach { (name, value) -> put(name, value) }
        }
        module.httpRequestBinary(
            url = source,
            isPost = false,
            bytes = ByteArray(0),
            headers = headerJson,
            responseCallback = { data, success, errorMessage, response ->
                if (!continuation.isActive) return@httpRequestBinary
                val validStatus = response.statusCode == null ||
                    response.statusCode in 200..299
                if (success && validStatus) {
                    continuation.resume(data)
                } else {
                    continuation.resumeWithException(
                        IllegalStateException(errorMessage.ifBlank { "Media request failed" }),
                    )
                }
            },
        )
    }
}
