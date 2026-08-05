package com.tencent.kuiklybase.chat.compose.core

const val DEFAULT_USER_CACHE_NAMESPACE = "default"

internal data class UserCacheKey(
    val namespace: String,
    val userId: String,
)
