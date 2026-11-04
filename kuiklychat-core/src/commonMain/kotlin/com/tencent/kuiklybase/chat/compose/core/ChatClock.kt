package com.tencent.kuiklybase.chat.compose.core

import kotlinx.datetime.Clock

/** Injectable clock for message timestamps in commonMain. */
public fun interface ChatClock {
    public fun nowMillis(): Long

    public companion object {
        public val System: ChatClock = ChatClock { Clock.System.now().toEpochMilliseconds() }
        public val Unavailable: ChatClock = ChatClock { 0L }
    }
}
