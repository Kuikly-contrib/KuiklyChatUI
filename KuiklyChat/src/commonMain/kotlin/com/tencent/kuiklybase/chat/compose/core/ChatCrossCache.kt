package com.tencent.kuiklybase.chat.compose.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** Fetches bytes for sources that are not already present in [ChatByteCache]. */
interface ChatMediaFetcher {
    suspend fun fetch(
        request: ChatMediaRequest,
        onProgress: suspend (receivedBytes: Long, totalBytes: Long?) -> Unit,
    ): ByteArray

    fun close() = Unit
}

/**
 * Cross-platform binary cache aligned with Flyer's CrossCache API.
 *
 * Storage and network concerns are injected, while [load] adapts cached bytes to the string-only
 * Kuikly image pipeline. Concurrent misses for the same cache key share one fetch.
 */
class ChatCrossCache(
    private val store: ChatByteCache,
    private val fetcher: ChatMediaFetcher,
) : ChatMediaLoader {
    private val inFlightMutex = Mutex()
    private val inFlight = mutableMapOf<String, CompletableDeferred<ByteArray>>()
    private var closed = false

    suspend fun downloadAndSave(
        request: ChatMediaRequest,
        onProgress: suspend (receivedBytes: Long, totalBytes: Long?) -> Unit = { _, _ -> },
    ): ByteArray {
        checkOpen()
        require(request.source.isNotBlank()) { "Media source must not be blank" }
        require(request.cacheKey.isNotBlank()) { "Media cache key must not be blank" }

        store.get(request.cacheKey)?.let { return it }

        var ownsFetch = false
        val shared = inFlightMutex.withLock {
            inFlight[request.cacheKey] ?: CompletableDeferred<ByteArray>().also {
                inFlight[request.cacheKey] = it
                ownsFetch = true
            }
        }
        if (!ownsFetch) return shared.await().copyOf()

        try {
            val bytes = store.get(request.cacheKey)
                ?: fetcher.fetch(request, onProgress).also { fetched ->
                    store.set(request.cacheKey, fetched)
                }
            shared.complete(bytes.copyOf())
            return bytes.copyOf()
        } catch (cancelled: CancellationException) {
            shared.cancel(cancelled)
            throw cancelled
        } catch (error: Throwable) {
            shared.completeExceptionally(error)
            throw error
        } finally {
            inFlightMutex.withLock {
                if (inFlight[request.cacheKey] === shared) {
                    inFlight.remove(request.cacheKey)
                }
            }
        }
    }

    suspend fun set(key: String, value: ByteArray) = store.set(key, value)

    suspend fun get(key: String): ByteArray =
        store.get(key) ?: throw ChatCacheMissException(key)

    suspend fun contains(key: String): Boolean = store.contains(key)

    suspend fun delete(key: String) = store.delete(key)

    suspend fun updateKey(key: String, newKey: String) = store.updateKey(key, newKey)

    suspend fun clear() = store.clear()

    override fun load(request: ChatMediaRequest): Flow<ChatMediaLoadEvent> = flow {
        if (request.source.isDirectPainterSource()) {
            emit(ChatMediaLoadEvent.Ready(request.source))
            return@flow
        }
        try {
            val bytes = downloadAndSave(request) { received, total ->
                emit(ChatMediaLoadEvent.Progress(received, total))
            }
            emit(
                ChatMediaLoadEvent.Ready(
                    bytes.toImageDataUri(resolveMediaMimeType(request)),
                ),
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            emit(ChatMediaLoadEvent.Failed(error.message ?: "Media load failed"))
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        val cancellation = CancellationException("ChatCrossCache closed")
        inFlight.values.forEach { it.cancel(cancellation) }
        inFlight.clear()
        fetcher.close()
        store.close()
    }

    private fun checkOpen() {
        check(!closed) { "ChatCrossCache is closed" }
    }
}

private fun String.isDirectPainterSource(): Boolean {
    val trimmed = trim()
    return trimmed.startsWith("assets/") || trimmed.startsWith("content://")
}

private fun resolveMediaMimeType(request: ChatMediaRequest): String {
    request.mimeType?.trim()?.takeIf { it.startsWith("image/") }?.let { return it }
    val source = request.source.trim()
    if (source.startsWith("data:")) {
        source.substringAfter("data:").substringBefore(';').substringBefore(',')
            .takeIf { it.startsWith("image/") }
            ?.let { return it }
    }
    val path = source.substringBefore('?').lowercase()
    return when {
        path.endsWith(".jpg") || path.endsWith(".jpeg") -> "image/jpeg"
        path.endsWith(".gif") -> "image/gif"
        path.endsWith(".webp") -> "image/webp"
        path.endsWith(".bmp") -> "image/bmp"
        path.endsWith(".svg") -> "image/svg+xml"
        else -> "image/png"
    }
}

@OptIn(ExperimentalEncodingApi::class)
private fun ByteArray.toImageDataUri(mimeType: String): String =
    "data:$mimeType;base64,${Base64.Default.encode(this)}"
