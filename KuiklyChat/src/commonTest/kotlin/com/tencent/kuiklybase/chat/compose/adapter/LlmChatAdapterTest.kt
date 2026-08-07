package com.tencent.kuiklybase.chat.compose.adapter

import com.tencent.kuiklybase.chat.compose.core.ChatStreamEvent
import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.compose.core.TextFormat
import com.tencent.kuiklybase.chat.kuiklyai.provider.BaseLlmProvider
import com.tencent.kuiklybase.chat.kuiklyai.provider.EchoLlmProvider
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmChatMessage
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmClock
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmErrorCode
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmProviderException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LlmChatAdapterTest {
    @Test
    fun echoProviderMapsToStreamEvents() = runTest {
        val adapter = LlmChatAdapter(
            EchoLlmProvider(
                chunkSize = 2,
                delayMillis = 0L,
                responder = { _ -> "hi" }
            ),
            ChatUser("user", "You"),
            ChatUser("assistant", "Assistant")
        )
        val userMessage = com.tencent.kuiklybase.chat.compose.core.ChatMessage(
            id = "user-1",
            author = ChatUser("user"),
            parts = listOf(MessagePart.Text("hi"))
        )
        val events = adapter.sender.send(userMessage).toList()
        assertTrue(events.any { it is ChatStreamEvent.TextDelta })
        assertTrue(events.last() is ChatStreamEvent.Completed)
        val deltas = events.filterIsInstance<ChatStreamEvent.TextDelta>()
        assertEquals("hi", deltas.fold("") { acc, delta -> acc + delta.text })
        assertTrue(deltas.all { it.format == TextFormat.MARKDOWN })
    }

    @Test
    fun providerExceptionMapsToFailedEvent() = runTest {
        val adapter = LlmChatAdapter(FailingLlmProvider())
        val userMessage = com.tencent.kuiklybase.chat.compose.core.ChatMessage(
            id = "user-1",
            author = ChatUser("user"),
            parts = listOf(MessagePart.Text("hi"))
        )
        val events = adapter.sender.send(userMessage).toList()
        val failed = events.filterIsInstance<ChatStreamEvent.Failed>().single()
        assertEquals("offline", failed.error.message)
    }

    private class FailingLlmProvider : BaseLlmProvider(LlmClock.Unavailable, idPrefix = "fail") {
        override fun streamCompletion(context: List<LlmChatMessage>): Flow<String> = flow {
            throw LlmProviderException(LlmErrorCode.NETWORK, "offline")
        }
    }
}
