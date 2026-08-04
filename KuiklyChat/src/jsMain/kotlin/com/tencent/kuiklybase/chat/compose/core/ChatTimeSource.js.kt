package com.tencent.kuiklybase.chat.compose.core

internal actual fun chatCurrentTimeMillis(): Long = kotlin.js.Date.now().toLong()
