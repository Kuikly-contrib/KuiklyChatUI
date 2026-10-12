package com.tencent.kuiklybase.chat.compose.core

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChatCrossCacheTest {
    @Test
    fun sha256MatchesKnownVector() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            sha256Hex("abc"),
        )
    }

    @Test
    fun namespaceNormalizationPreventsPathTraversal() {
        assertEquals("tenant_.._images", normalizeChatCacheNamespace("tenant/../images"))
        assertEquals(DEFAULT_CHAT_MEDIA_CACHE_NAMESPACE, normalizeChatCacheNamespace("..."))
    }

    @Test
    fun memoryStoreSupportsDirectCacheApiAndCopiesBytes() = runTest {
        val store = MemoryChatByteCache()
        val original = byteArrayOf(1, 2, 3)
        store.set("old", original)
        original[0] = 9

        assertContentEquals(byteArrayOf(1, 2, 3), store.get("old"))
        assertTrue(store.contains("old"))

        store.updateKey("old", "new")
        assertFalse(store.contains("old"))
        assertContentEquals(byteArrayOf(1, 2, 3), store.get("new"))

        store.delete("new")
        assertFalse(store.contains("new"))
        assertFailsWith<ChatCacheMissException> { store.updateKey("missing", "next") }
    }

    @Test
    fun downloadCachesBytesAndPropagatesRequestOptions() = runTest {
        val fetcher = RecordingFetcher(byteArrayOf(4, 5, 6))
        val cache = ChatCrossCache(MemoryChatByteCache(), fetcher)
        val request = ChatMediaRequest(
            source = "https://cdn.example/image.png",
            cacheKey = "tenant:image",
            headers = mapOf("Authorization" to "Bearer secret"),
            mimeType = "image/png",
        )

        assertContentEquals(byteArrayOf(4, 5, 6), cache.downloadAndSave(request))
        assertContentEquals(byteArrayOf(4, 5, 6), cache.downloadAndSave(request))
        assertEquals(1, fetcher.fetchCount)
        assertEquals(request, fetcher.lastRequest)
        assertTrue(cache.contains(request.cacheKey))
    }

    @Test
    fun concurrentMissesShareOneFetch() = runTest {
        val releaseFetch = CompletableDeferred<Unit>()
        val fetcher = RecordingFetcher(byteArrayOf(7, 8), releaseFetch)
        val cache = ChatCrossCache(MemoryChatByteCache(), fetcher)
        val request = ChatMediaRequest("https://cdn.example/shared.png")

        val first = async { cache.downloadAndSave(request) }
        runCurrent()
        val second = async { cache.downloadAndSave(request) }
        runCurrent()

        assertEquals(1, fetcher.fetchCount)
        releaseFetch.complete(Unit)
        assertContentEquals(byteArrayOf(7, 8), first.await())
        assertContentEquals(byteArrayOf(7, 8), second.await())
    }

    @Test
    fun loaderEmitsProgressThenCachedDataUri() = runTest {
        val fetcher = RecordingFetcher(byteArrayOf(1, 2, 3))
        val cache = ChatCrossCache(MemoryChatByteCache(), fetcher)

        val events = cache.load(
            ChatMediaRequest(
                source = "https://cdn.example/image",
                mimeType = "image/jpeg",
            ),
        ).toList()

        assertEquals(ChatMediaLoadEvent.Progress(3L, 3L), events.first())
        assertEquals(
            ChatMediaLoadEvent.Ready("data:image/jpeg;base64,AQID"),
            events.last(),
        )
    }

    @Test
    fun directPainterSourcesBypassFetcher() = runTest {
        val fetcher = RecordingFetcher(byteArrayOf(1))
        val cache = ChatCrossCache(MemoryChatByteCache(), fetcher)

        val events = cache.load(ChatMediaRequest("assets/common/avatar.png")).toList()

        assertEquals(listOf(ChatMediaLoadEvent.Ready("assets/common/avatar.png")), events)
        assertEquals(0, fetcher.fetchCount)
    }

    @Test
    fun inlineSourcesDecodeBase64() {
        assertContentEquals(
            byteArrayOf(1, 2, 3),
            decodeInlineMediaSource("data:image/png;base64,AQID"),
        )
        assertContentEquals(byteArrayOf(1, 2, 3), decodeInlineMediaSource("AQID"))
        assertEquals(null, decodeInlineMediaSource("not base64!"))
    }

    private class RecordingFetcher(
        private val result: ByteArray,
        private val release: CompletableDeferred<Unit>? = null,
    ) : ChatMediaFetcher {
        var fetchCount: Int = 0
            private set
        var lastRequest: ChatMediaRequest? = null
            private set

        override suspend fun fetch(
            request: ChatMediaRequest,
            onProgress: suspend (Long, Long?) -> Unit,
        ): ByteArray {
            fetchCount++
            lastRequest = request
            release?.await()
            onProgress(result.size.toLong(), result.size.toLong())
            return result.copyOf()
        }
    }
}
