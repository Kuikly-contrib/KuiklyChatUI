package com.tencent.kuiklybase.chat.compose.core

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.MutableStateFlow

internal data class CacheRecord(
    val user: ChatUser?,
    val failureCount: Int,
)

internal data class InFlightRequest(
    val token: Long,
    val deferred: Deferred<ChatUser?>,
    var cancellationReason: RequestCancellationReason? = null,
)

internal enum class RequestCancellationReason {
    INVALIDATED,
    CACHE_CLOSED,
}

internal data class ObserverNode(
    var refCount: Int,
    val revision: MutableStateFlow<Long>,
)

internal const val USER_CACHE_CLOSED_REVISION = Long.MAX_VALUE
