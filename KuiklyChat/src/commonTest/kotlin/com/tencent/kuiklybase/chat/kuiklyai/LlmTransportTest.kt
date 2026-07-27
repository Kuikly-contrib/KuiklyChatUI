package com.tencent.kuiklybase.chat.kuiklyai

import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmHttpRequest
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmStreamSink
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmSubscription
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmTransport
import com.tencent.kuiklybase.chat.kuiklyai.provider.streamAsFlow
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LlmTransportTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun cancelingCollectorCancelsTransportAndRejectsLateChunks() = runTest {
        lateinit var sink: LlmStreamSink
        var cancelCount = 0
        val transport = LlmTransport { _, streamSink ->
            sink = streamSink
            LlmSubscription { cancelCount++ }
        }
        val chunks = mutableListOf<String>()

        val collection = launch {
            transport.streamAsFlow(LlmHttpRequest(url = "memory://stream"))
                .collect { chunks.add(it) }
        }
        runCurrent()
        sink.onChunk("first")
        runCurrent()

        collection.cancelAndJoin()
        sink.onChunk("late")
        runCurrent()

        assertEquals(listOf("first"), chunks)
        assertEquals(1, cancelCount)
    }
}
