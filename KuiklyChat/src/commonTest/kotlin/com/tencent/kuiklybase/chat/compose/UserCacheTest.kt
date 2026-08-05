package com.tencent.kuiklybase.chat.compose

import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.core.ChatUserResolver
import com.tencent.kuiklybase.chat.compose.core.DEFAULT_USER_CACHE_NAMESPACE
import com.tencent.kuiklybase.chat.compose.core.UserCache
import com.tencent.kuiklybase.chat.compose.core.UserCacheKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserCacheTest {
    @Test
    fun getSyncDoesNotAdvanceLruRecency() = runTest {
        val cache = UserCache(backgroundScope, maxSize = 2)
        cache.put(ChatUser("a", "A"))
        cache.put(ChatUser("b", "B"))
        cache.getSync("a")
        cache.put(ChatUser("c", "C"))
        assertNull(cache.getSync("a")?.name)
        assertEquals("B", cache.getSync("b")?.name)
        assertEquals("C", cache.getSync("c")?.name)
        cache.close()
    }

    @Test
    fun getOrResolveDedupesConcurrentRequests() = runTest(StandardTestDispatcher()) {
        var calls = 0
        val resolver = ChatUserResolver {
            calls++
            delay(50)
            ChatUser(it, "Name")
        }
        val cache = UserCache(this, maxPendingResolutions = 8)
        val results = (1..5).map {
            async { cache.getOrResolve("u1", resolver) }
        }.awaitAll()
        testScheduler.advanceUntilIdle()
        assertEquals(1, calls)
        assertEquals(5, results.count { it?.name == "Name" })
        cache.close()
    }

    @Test
    fun negativeCacheStopsAfterMaxAttempts() = runTest(StandardTestDispatcher()) {
        var calls = 0
        val resolver = ChatUserResolver {
            calls++
            null
        }
        val cache = UserCache(this, maxAttempts = 2)
        cache.getOrResolve("u1", resolver)
        testScheduler.advanceUntilIdle()
        cache.getOrResolve("u1", resolver)
        testScheduler.advanceUntilIdle()
        cache.getOrResolve("u1", resolver)
        testScheduler.advanceUntilIdle()
        assertEquals(2, calls)
        cache.close()
    }

    @Test
    fun retryFailedResetsNegativeCache() = runTest(StandardTestDispatcher()) {
        var calls = 0
        val resolver = ChatUserResolver {
            calls++
            if (calls == 1) null else ChatUser("u1", "Ok")
        }
        val cache = UserCache(this, maxAttempts = 1)
        assertNull(cache.getOrResolve("u1", resolver))
        testScheduler.advanceUntilIdle()
        assertNull(cache.getOrResolve("u1", resolver))
        testScheduler.advanceUntilIdle()
        cache.retryFailed()
        assertEquals("Ok", cache.getOrResolve("u1", resolver)?.name)
        testScheduler.advanceUntilIdle()
        assertEquals(2, calls)
        cache.close()
    }

    @Test
    fun rejectResolverWrongId() = runTest(StandardTestDispatcher()) {
        val cache = UserCache(this)
        val resolver = ChatUserResolver { ChatUser("other", "Wrong") }
        assertNull(cache.getOrResolve("u1", resolver))
        testScheduler.advanceUntilIdle()
        assertNull(cache.getSync("u1"))
        cache.close()
    }

    @Test
    fun namespacesAreIsolated() = runTest(StandardTestDispatcher()) {
        val cache = UserCache(this)
        cache.put(ChatUser("u1", "A"), namespace = "ns1")
        cache.put(ChatUser("u1", "B"), namespace = "ns2")
        assertEquals("A", cache.getSync("u1", "ns1")?.name)
        assertEquals("B", cache.getSync("u1", "ns2")?.name)
        cache.close()
    }

    @Test
    fun removeClearsEntry() = runTest {
        val cache = UserCache(backgroundScope)
        cache.put(ChatUser("u1", "A"))
        cache.remove("u1")
        assertNull(cache.getSync("u1"))
        cache.close()
    }

    @Test
    fun emptyUserIdReturnsNullWithoutResolve() = runTest(StandardTestDispatcher()) {
        var called = false
        val cache = UserCache(this)
        val resolver = ChatUserResolver {
            called = true
            ChatUser("x")
        }
        assertNull(cache.getOrResolve("", resolver))
        testScheduler.advanceUntilIdle()
        assertTrue(!called)
        cache.close()
    }

    @Test
    fun callerCancelDoesNotBreakSharedRequest() = runTest(StandardTestDispatcher()) {
        var calls = 0
        val resolver = ChatUserResolver {
            calls++
            delay(100)
            ChatUser("u1", "Shared")
        }
        val cache = UserCache(this)
        val first = async { cache.getOrResolve("u1", resolver) }
        val second = async { cache.getOrResolve("u1", resolver) }
        first.cancel()
        testScheduler.advanceUntilIdle()
        assertEquals("Shared", second.await()?.name)
        assertEquals(1, calls)
        cache.close()
    }

    @Test
    fun invalidatedRequestReturnsNullForActiveCaller() = runTest(StandardTestDispatcher()) {
        val resolver = ChatUserResolver {
            delay(200)
            ChatUser("u1", "Late")
        }
        val cache = UserCache(this)
        val deferred = async { cache.getOrResolve("u1", resolver) }
        testScheduler.runCurrent()
        cache.put(ChatUser("u1", "Manual"))
        testScheduler.advanceUntilIdle()
        assertNull(deferred.await())
        assertEquals("Manual", cache.getSync("u1")?.name)
        cache.close()
    }

    @Test
    fun observeEmitsOnExplicitUpdate() = runTest(StandardTestDispatcher()) {
        val cache = UserCache(this)
        val key = UserCacheKey(DEFAULT_USER_CACHE_NAMESPACE, "u1")
        val values = mutableListOf<Long>()
        val job = launch {
            cache.observe(key).collect { values.add(it) }
        }
        testScheduler.runCurrent()
        cache.put(ChatUser("u1", "A"))
        testScheduler.runCurrent()
        job.cancel()
        assertTrue(values.isNotEmpty())
        cache.close()
    }

    @Test
    fun closeRejectsNewResolves() = runTest(StandardTestDispatcher()) {
        var called = false
        val cache = UserCache(this)
        cache.close()
        val resolver = ChatUserResolver {
            called = true
            ChatUser("u1")
        }
        assertNull(cache.getOrResolve("u1", resolver))
        testScheduler.advanceUntilIdle()
        assertTrue(!called)
    }

    @Test
    fun supervisorAllowsOtherUsersAfterOneFails() = runTest(StandardTestDispatcher()) {
        val cache = UserCache(this)
        val resolver = ChatUserResolver { userId ->
            if (userId == "bad") throw CancellationException("fail")
            ChatUser(userId, "Ok")
        }
        try {
            cache.getOrResolve("bad", resolver)
        } catch (_: CancellationException) {
        }
        testScheduler.advanceUntilIdle()
        assertEquals("Ok", cache.getOrResolve("good", resolver)?.name)
        testScheduler.advanceUntilIdle()
        cache.close()
    }
}
