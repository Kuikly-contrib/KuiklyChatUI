package com.kuikly.kuiklychat.compose.support

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.tencent.kuiklybase.chat.compose.core.ChatMediaLoadEvent
import com.tencent.kuiklybase.chat.compose.core.ChatMediaLoader
import com.tencent.kuiklybase.chat.compose.core.ChatMediaRequest
import kotlinx.coroutines.flow.flow

/**
 * Demo-only loader showing header propagation and cache-key reuse.
 *
 * Production authenticated loaders must fetch with the supplied headers (or return a signed/local
 * URI). This sample keeps public demo URLs unchanged so every platform can render them.
 */
internal class DemoChatMediaLoader : ChatMediaLoader {
    private val resolvedSources = mutableMapOf<String, String>()
    private var closed = false

    var requestCount by mutableIntStateOf(0)
        private set

    var cacheHitCount by mutableIntStateOf(0)
        private set

    override fun load(request: ChatMediaRequest) = flow {
        requestCount++
        if (closed) {
            emit(ChatMediaLoadEvent.Failed("Demo media loader is closed"))
            return@flow
        }
        if (request.source.startsWith("http") && request.headers["Authorization"] != DEMO_AUTH) {
            emit(ChatMediaLoadEvent.Failed("Missing demo authorization"))
            return@flow
        }
        val cached = resolvedSources[request.cacheKey]
        if (cached != null) {
            cacheHitCount++
            emit(ChatMediaLoadEvent.Ready(cached))
            return@flow
        }
        emit(ChatMediaLoadEvent.Progress(receivedBytes = 0L, totalBytes = 1L))
        resolvedSources[request.cacheKey] = request.source
        emit(ChatMediaLoadEvent.Progress(receivedBytes = 1L, totalBytes = 1L))
        emit(ChatMediaLoadEvent.Ready(request.source))
    }

    override fun close() {
        if (closed) return
        closed = true
        resolvedSources.clear()
    }

    companion object {
        const val DEMO_AUTH = "Bearer demo-token"
    }
}
