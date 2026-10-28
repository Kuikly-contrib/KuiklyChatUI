package com.kuikly.kuiklychat.dsl.chat

import kotlinx.coroutines.flow.Flow

/** One earlier chat turn sent back to the model for context. */
internal data class KimiTurn(val role: String, val content: String)

/** Platform HTTP bridge used only by the host demo. */
internal expect fun streamKimiA2ui(
    apiKey: String,
    model: String,
    prompt: String,
    history: List<KimiTurn> = emptyList(),
): Flow<String>
