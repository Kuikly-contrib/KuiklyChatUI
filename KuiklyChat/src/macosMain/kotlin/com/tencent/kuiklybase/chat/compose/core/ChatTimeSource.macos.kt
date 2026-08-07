package com.tencent.kuiklybase.chat.compose.core

import platform.Foundation.NSDate
import platform.Foundation.date
import platform.Foundation.timeIntervalSince1970

internal actual fun chatCurrentTimeMillis(): Long =
    (NSDate.date().timeIntervalSince1970() * 1_000.0).toLong()
