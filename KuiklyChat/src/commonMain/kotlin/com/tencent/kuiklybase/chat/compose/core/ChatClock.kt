package com.tencent.kuiklybase.chat.compose.core

/** Injectable clock for message timestamps in commonMain. */
fun interface ChatClock {
    fun nowMillis(): Long

    companion object {
        val System: ChatClock = ChatClock { chatCurrentTimeMillis() }
        val Unavailable: ChatClock = ChatClock { 0L }
    }
}

internal expect fun chatCurrentTimeMillis(): Long
