package com.kuikly.kuiklychat.dsl.chat

import android.util.Log

private const val TAG = "KuiklyA2uiDemo"

/** Logcat drops anything past ~4 KB per entry; 1000 chars stays under it even for CJK text. */
private const val CHUNK_CHARS = 1000

internal actual fun logAgentCardDemo(event: String, payload: String) {
    if (payload.length <= CHUNK_CHARS) {
        Log.d(TAG, "event=$event payload=$payload")
        return
    }
    val chunks = payload.chunked(CHUNK_CHARS)
    chunks.forEachIndexed { index, chunk ->
        Log.d(TAG, "event=$event part=${index + 1}/${chunks.size} payload=$chunk")
    }
}
