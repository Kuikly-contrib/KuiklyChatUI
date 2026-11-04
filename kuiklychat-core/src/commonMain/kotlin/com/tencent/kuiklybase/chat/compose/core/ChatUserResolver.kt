package com.tencent.kuiklybase.chat.compose.core

/**
 * Resolves display [ChatUser] by id. Return null when the user is unknown or resolution fails.
 *
 * Implementations must be non-blocking suspend functions: this module targets js and does not
 * assume [kotlinx.coroutines.Dispatchers.IO].
 */
public fun interface ChatUserResolver {
    public suspend fun resolve(userId: String): ChatUser?
}
