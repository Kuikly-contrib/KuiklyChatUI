package com.tencent.kuiklybase.chat.compose.core

public const val DEFAULT_USER_CACHE_NAMESPACE: String = "default"

internal data class UserCacheKey(
    val namespace: String,
    val userId: String,
)
