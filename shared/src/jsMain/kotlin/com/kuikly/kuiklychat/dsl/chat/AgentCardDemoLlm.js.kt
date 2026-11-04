package com.kuikly.kuiklychat.dsl.chat

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal actual fun streamKimiA2ui(
    apiKey: String,
    model: String,
    prompt: String,
    history: List<KimiTurn>,
): Flow<String> = flow {
    throw IllegalStateException("当前 Demo 的真实 Kimi transport 尚未接入 JS 宿主")
}

