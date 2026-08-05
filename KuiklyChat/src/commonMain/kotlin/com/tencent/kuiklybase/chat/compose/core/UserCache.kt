package com.tencent.kuiklybase.chat.compose.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withTimeoutOrNull

/**
 * LRU-backed user directory cache with in-flight deduplication and namespace isolation.
 *
 * Threading: not thread-safe; intended for main-thread Compose usage and single-threaded tests.
 */
class UserCache(
    parentScope: CoroutineScope,
    private val maxSize: Int = 100,
    private val maxAttempts: Int = 2,
    private val maxPendingResolutions: Int = 32,
    private val maxConcurrentResolutions: Int = 8,
    private val resolveTimeoutMillis: Long = 15_000L,
) {
    private val supervisor = SupervisorJob(parentScope.coroutineContext[kotlinx.coroutines.Job])
    private val scope = CoroutineScope(parentScope.coroutineContext + supervisor)

    private val records = mutableMapOf<UserCacheKey, CacheRecord>()
    private val accessOrder = ArrayDeque<UserCacheKey>()
    private val inFlight = mutableMapOf<UserCacheKey, InFlightRequest>()
    private val observers = mutableMapOf<UserCacheKey, ObserverNode>()
    private val admissionPermits = Semaphore(maxPendingResolutions)
    private val resolvePermits = Semaphore(maxConcurrentResolutions)

    private var closed = false
    private var nextToken = 0L
    private var nextRevision = 1L

    init {
        require(maxSize > 0)
        require(maxAttempts > 0)
        require(maxPendingResolutions > 0)
        require(maxConcurrentResolutions > 0)
        require(resolveTimeoutMillis > 0)
    }

    fun getSync(userId: String, namespace: String = DEFAULT_USER_CACHE_NAMESPACE): ChatUser? {
        if (userId.isEmpty()) return null
        return records[UserCacheKey(namespace, userId)]?.user
    }

    suspend fun getOrResolve(
        userId: String,
        resolver: ChatUserResolver,
        namespace: String = DEFAULT_USER_CACHE_NAMESPACE,
    ): ChatUser? {
        if (userId.isEmpty() || closed) return null
        val key = UserCacheKey(namespace, userId)

        val cached = records[key]
        if (cached != null) {
            if (cached.failureCount >= maxAttempts) return null
            if (cached.user != null) {
                touch(key)
                return cached.user
            }
        }

        val existing = inFlight[key]
        if (existing != null) return awaitInFlight(key, existing)

        admissionPermits.acquire()
        var leaseTransferred = false
        try {
            if (closed) return null
            val recheck = records[key]
            if (recheck != null) {
                if (recheck.user != null) {
                    touch(key)
                    return recheck.user
                }
                if (recheck.failureCount >= maxAttempts) return null
            }
            val again = inFlight[key]
            if (again != null) return awaitInFlight(key, again)

            val token = ++nextToken
            val deferred = scope.async(start = kotlinx.coroutines.CoroutineStart.LAZY) {
                resolveWithLimits(key, userId, resolver, token)
            }
            val request = InFlightRequest(token = token, deferred = deferred)
            inFlight[key] = request
            deferred.invokeOnCompletion {
                admissionPermits.release()
                val current = inFlight[key]
                if (current?.token == token) inFlight.remove(key)
            }
            leaseTransferred = true
            deferred.start()
            return awaitInFlight(key, request)
        } finally {
            if (!leaseTransferred) admissionPermits.release()
        }
    }

    fun put(user: ChatUser, namespace: String = DEFAULT_USER_CACHE_NAMESPACE) {
        if (closed || user.id.isEmpty()) return
        val key = UserCacheKey(namespace, user.id)
        invalidateInFlight(key, RequestCancellationReason.INVALIDATED)
        writeRecord(key, CacheRecord(user = user, failureCount = 0))
        bumpRevision(key)
    }

    fun updateUser(userId: String, user: ChatUser?, namespace: String = DEFAULT_USER_CACHE_NAMESPACE) {
        if (closed || userId.isEmpty()) return
        val key = UserCacheKey(namespace, userId)
        invalidateInFlight(key, RequestCancellationReason.INVALIDATED)
        if (user == null) {
            records.remove(key)
            accessOrder.remove(key)
        } else {
            writeRecord(key, CacheRecord(user = user, failureCount = 0))
        }
        bumpRevision(key)
    }

    fun remove(userId: String, namespace: String = DEFAULT_USER_CACHE_NAMESPACE) {
        if (closed || userId.isEmpty()) return
        val key = UserCacheKey(namespace, userId)
        invalidateInFlight(key, RequestCancellationReason.INVALIDATED)
        records.remove(key)
        accessOrder.remove(key)
        bumpRevision(key)
    }

    fun retryFailed(namespace: String = DEFAULT_USER_CACHE_NAMESPACE) {
        if (closed) return
        records.keys.filter { it.namespace == namespace }.forEach { key ->
            val record = records[key]
            if (record != null && record.user == null && record.failureCount > 0) {
                records[key] = record.copy(failureCount = 0)
            }
        }
        observers.filter { it.key.namespace == namespace }.keys.forEach(::bumpRevision)
    }

    fun clear(namespace: String = DEFAULT_USER_CACHE_NAMESPACE) {
        if (closed) return
        val keys = records.keys.filter { it.namespace == namespace }
        keys.forEach { key ->
            invalidateInFlight(key, RequestCancellationReason.INVALIDATED)
            records.remove(key)
            accessOrder.remove(key)
        }
        keys.forEach(::bumpRevision)
    }

    fun clearAll() {
        if (closed) return
        val keys = records.keys.toList()
        keys.forEach { key ->
            invalidateInFlight(key, RequestCancellationReason.INVALIDATED)
            records.remove(key)
        }
        accessOrder.clear()
        keys.forEach(::bumpRevision)
    }

    fun close() {
        if (closed) return
        closed = true
        inFlight.values.forEach { request ->
            request.cancellationReason = RequestCancellationReason.CACHE_CLOSED
            request.deferred.cancel()
        }
        inFlight.clear()
        observers.values.forEach { node ->
            node.revision.value = USER_CACHE_CLOSED_REVISION
        }
        observers.clear()
        supervisor.cancel()
    }

    internal fun observe(key: UserCacheKey): Flow<Long> {
        if (closed) return emptyFlow()
        return flow {
            val node = registerObserver(key)
            try {
                node.revision.takeWhile { it != USER_CACHE_CLOSED_REVISION }.collect { emit(it) }
            } finally {
                unregisterObserver(key, node)
            }
        }
    }

    private suspend fun resolveWithLimits(
        key: UserCacheKey,
        userId: String,
        resolver: ChatUserResolver,
        token: Long,
    ): ChatUser? {
        try {
            resolvePermits.acquire()
            val result = withTimeoutOrNull(resolveTimeoutMillis) {
                resolver.resolve(userId)
            }
            if (closed) throw CancellationException("UserCache closed")
            if (inFlight[key]?.token != token) return null

            if (result == null) {
                recordFailure(key)
                return null
            }
            if (result.id != userId) {
                recordFailure(key)
                return null
            }
            if (inFlight[key]?.token != token) return null
            writeRecord(key, CacheRecord(user = result, failureCount = 0))
            bumpRevision(key)
            return result
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            if (inFlight[key]?.token == token) recordFailure(key)
            return null
        } finally {
            resolvePermits.release()
        }
    }

    private suspend fun awaitInFlight(key: UserCacheKey, request: InFlightRequest): ChatUser? {
        try {
            return request.deferred.await()
        } catch (cancelled: CancellationException) {
            currentCoroutineContext().ensureActive()
            when (request.cancellationReason) {
                RequestCancellationReason.INVALIDATED -> return null
                RequestCancellationReason.CACHE_CLOSED -> throw cancelled
                null -> throw cancelled
            }
        }
    }

    private fun invalidateInFlight(key: UserCacheKey, reason: RequestCancellationReason) {
        val request = inFlight.remove(key) ?: return
        request.cancellationReason = reason
        request.deferred.cancel()
    }

    private fun recordFailure(key: UserCacheKey) {
        val current = records[key]
        val failures = (current?.failureCount ?: 0) + 1
        writeRecord(key, CacheRecord(user = null, failureCount = failures))
    }

    private fun writeRecord(key: UserCacheKey, record: CacheRecord) {
        records[key] = record
        touch(key)
        evictIfNeeded()
    }

    private fun touch(key: UserCacheKey) {
        accessOrder.remove(key)
        accessOrder.addLast(key)
    }

    private fun evictIfNeeded() {
        while (records.size > maxSize && accessOrder.isNotEmpty()) {
            val oldest = accessOrder.removeFirst()
            records.remove(oldest)
        }
    }

    private fun bumpRevision(key: UserCacheKey) {
        observers[key]?.revision?.value = ++nextRevision
    }

    private fun registerObserver(key: UserCacheKey): ObserverNode {
        val node = observers[key]
        if (node != null) {
            node.refCount++
            return node
        }
        val created = ObserverNode(refCount = 1, revision = MutableStateFlow(nextRevision))
        observers[key] = created
        return created
    }

    private fun unregisterObserver(key: UserCacheKey, node: ObserverNode) {
        node.refCount--
        if (node.refCount <= 0) observers.remove(key)
    }
}
