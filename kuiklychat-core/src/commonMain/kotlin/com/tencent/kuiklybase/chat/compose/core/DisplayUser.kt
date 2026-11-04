package com.tencent.kuiklybase.chat.compose.core

/** Merges cached resolver data over embedded message author fields for display. */
public fun resolveDisplayUser(
    userId: String,
    embedded: ChatUser,
    cached: ChatUser?,
): ChatUser = ChatUser(
    id = userId,
    name = cached?.name?.takeIf { it.isNotEmpty() } ?: embedded.name,
    avatar = cached?.avatar?.takeIf { it.isNotEmpty() } ?: embedded.avatar,
    metadata = embedded.metadata + cached?.metadata.orEmpty(),
)
