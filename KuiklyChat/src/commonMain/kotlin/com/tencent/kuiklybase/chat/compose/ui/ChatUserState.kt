package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.core.ChatUserResolver
import com.tencent.kuiklybase.chat.compose.core.UserCache
import com.tencent.kuiklybase.chat.compose.core.UserCacheKey
import com.tencent.kuiklybase.chat.compose.core.resolveDisplayUser
import kotlinx.coroutines.flow.collectLatest

@Composable
fun rememberUserCache(
    maxSize: Int = 100,
    maxAttempts: Int = 2,
    maxPendingResolutions: Int = 32,
    maxConcurrentResolutions: Int = 8,
    resolveTimeoutMillis: Long = 15_000L,
): UserCache {
    val parent = rememberCoroutineScope()
    val cache = remember(
        parent,
        maxSize,
        maxAttempts,
        maxPendingResolutions,
        maxConcurrentResolutions,
        resolveTimeoutMillis,
    ) {
        UserCache(
            parentScope = parent,
            maxSize = maxSize,
            maxAttempts = maxAttempts,
            maxPendingResolutions = maxPendingResolutions,
            maxConcurrentResolutions = maxConcurrentResolutions,
            resolveTimeoutMillis = resolveTimeoutMillis,
        )
    }
    DisposableEffect(cache) {
        onDispose { cache.close() }
    }
    return cache
}

private data class ResolvedUserValue(
    val owner: UserCache,
    val key: UserCacheKey,
    val user: ChatUser?,
)

@Composable
internal fun rememberChatUser(
    userId: String,
    embedded: ChatUser,
    cache: UserCache,
    namespace: String,
    latestResolver: State<ChatUserResolver?>,
    resolverAvailable: Boolean,
): ChatUser {
    val key = remember(namespace, userId) { UserCacheKey(namespace, userId) }
    val produced = produceState(
        ResolvedUserValue(
            owner = cache,
            key = key,
            user = cache.getSync(userId, namespace),
        ),
        cache,
        key,
        resolverAvailable,
    ) {
        cache.observe(key).collectLatest {
            val sync = cache.getSync(userId, namespace)
            value = ResolvedUserValue(cache, key, sync)
            if (sync == null && resolverAvailable) {
                latestResolver.value?.let { resolver ->
                    value = ResolvedUserValue(
                        owner = cache,
                        key = key,
                        user = cache.getOrResolve(userId, resolver, namespace),
                    )
                }
            }
        }
    }
    val snapshot = produced.value
    val cached = snapshot.user.takeIf {
        snapshot.owner === cache && snapshot.key == key
    }
    return remember(userId, embedded, cached) {
        resolveDisplayUser(userId, embedded, cached)
    }
}

@Composable
internal fun rememberResolvedMessage(
    message: ChatMessage,
    cache: UserCache,
    namespace: String,
    latestResolver: State<ChatUserResolver?>,
    resolverAvailable: Boolean,
): ChatMessage {
    val displayUser = rememberChatUser(
        userId = message.author.id,
        embedded = message.author,
        cache = cache,
        namespace = namespace,
        latestResolver = latestResolver,
        resolverAvailable = resolverAvailable,
    )
    return remember(message, displayUser) {
        if (displayUser == message.author) message else message.copy(author = displayUser)
    }
}
