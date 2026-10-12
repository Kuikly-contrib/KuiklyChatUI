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
import com.tencent.kuiklybase.chat.compose.core.resolveDisplayUser
import kotlinx.coroutines.flow.collectLatest

@Composable
public fun rememberUserCache(
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
    val namespace: String,
    val userId: String,
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
    val produced = produceState(
        ResolvedUserValue(
            owner = cache,
            namespace = namespace,
            userId = userId,
            user = cache.getSync(userId, namespace),
        ),
        cache,
        namespace,
        userId,
        resolverAvailable,
    ) {
        cache.observe(userId, namespace).collectLatest {
            val sync = cache.getSync(userId, namespace)
            value = ResolvedUserValue(cache, namespace, userId, sync)
            if (sync == null && resolverAvailable) {
                latestResolver.value?.let { resolver ->
                    value = ResolvedUserValue(
                        owner = cache,
                        namespace = namespace,
                        userId = userId,
                        user = cache.getOrResolve(userId, resolver, namespace),
                    )
                }
            }
        }
    }
    val snapshot = produced.value
    val cached = snapshot.user.takeIf {
        snapshot.owner === cache && snapshot.namespace == namespace && snapshot.userId == userId
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
