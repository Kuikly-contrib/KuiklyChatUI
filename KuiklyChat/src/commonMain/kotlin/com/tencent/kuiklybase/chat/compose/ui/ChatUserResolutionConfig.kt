package com.tencent.kuiklybase.chat.compose.ui

import com.tencent.kuiklybase.chat.compose.core.ChatUserResolver
import com.tencent.kuiklybase.chat.compose.core.UserCache

data class ChatUserResolutionConfig(
    /** Stable identity for account / tenant / resolver semantics — not resolver object identity. */
    val cacheNamespace: String,
    val resolver: ChatUserResolver? = null,
    val cache: UserCache? = null,
    /** Reserve nickname row height for inbound FIRST/SINGLE when name is still empty. */
    val reserveSenderNameSpace: Boolean = true,
)
