package com.tencent.kuiklybase.chat.compose.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** Fetches bytes for sources that are not already present in [ChatByteCache]. */
public interface ChatMediaFetcher {
    public suspend fun fetch(
        request: ChatMediaRequest,
        onProgress: suspend (receivedBytes: Long, totalBytes: Long?) -> Unit,
    ): ByteArray

    public fun close(): Unit {}
}

/**
 * Cross-platform binary cache aligned with Flyer's CrossCache API.
 *
 * Storage and network concerns are injected, while [load] adapts cached bytes to the string-only
 * Kuikly image pipeline. Concurrent misses for the same cache key share one fetch.
 */
public class ChatCrossCache(
    private val store: ChatByteCache,
    private val fetcher: ChatMediaFetcher,
) : ChatMediaLoader {
    private val fetchScope = CoroutineScope(SupervisorJob())
    private val inFlightMutex = Mutex()
    private val inFlight = mutableMapOf<String, CompletableDeferred<ByteArray>>()
    private var closed = false

    public suspend fun downloadAndSave(
        request: ChatMediaRequest,
        onProgress: suspend (receivedBytes: Long, totalBytes: Long?) -> Unit = { _, _ -> },
    ): ByteArray {
        checkOpen()
        require(request.source.isNotBlank()) { "Media source must not be blank" }
        require(request.cacheKey.isNotBlank()) { "Media cache key must not be blank" }

        store.get(request.cacheKey)?.let { return it }

        val shared = inFlightMutex.withLock {
            inFlight[request.cacheKey] ?: CompletableDeferred<ByteArray>().also {
                inFlight[request.cacheKey] = it
                fetchScope.launch {
                    try {
                        val bytes = store.get(request.cacheKey)
                            ?: fetcher.fetch(request, onProgress).also { fetched ->
                                store.set(request.cacheKey, fetched)
                            }
                        it.complete(bytes.copyOf())
                    } catch (cancelled: CancellationException) {
                        it.cancel(cancelled)
                    } catch (error: Throwable) {
                        it.completeExceptionally(error)
                    } finally {
                        inFlightMutex.withLock {
                            if (inFlight[request.cacheKey] === it) {
                                inFlight.remove(request.cacheKey)
                            }
                        }
                    }
                }
            }
        }
        // The producer belongs to the cache, not to the first UI collector. A
        // caller leaving composition can stop awaiting without cancelling a
        // shared fetch still needed by another caller.
        return shared.await().copyOf()
    }

    public suspend fun set(key: String, value: ByteArray): Unit = store.set(key, value)

    public suspend fun get(key: String): ByteArray =
        store.get(key) ?: throw ChatCacheMissException(key)

    public suspend fun contains(key: String): Boolean = store.contains(key)

    public suspend fun delete(key: String): Unit = store.delete(key)

    public suspend fun updateKey(key: String, newKey: String): Unit = store.updateKey(key, newKey)

    public suspend fun clear(): Unit = store.clear()

    override fun load(request: ChatMediaRequest): Flow<ChatMediaLoadEvent> = channelFlow {
        if (request.source.isDirectPainterSource()) {
            send(ChatMediaLoadEvent.Ready(request.source))
            return@channelFlow
        }
        try {
            val bytes = downloadAndSave(request) { received, total ->
                send(ChatMediaLoadEvent.Progress(received, total))
            }
            send(
                ChatMediaLoadEvent.Ready(
                    bytes.toImageDataUri(resolveMediaMimeType(request)),
                ),
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            send(ChatMediaLoadEvent.Failed(error.message ?: "Media load failed"))
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        val cancellation = CancellationException("ChatCrossCache closed")
        inFlight.values.forEach { it.cancel(cancellation) }
        inFlight.clear()
        fetchScope.cancel()
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
