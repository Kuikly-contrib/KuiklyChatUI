package com.tencent.kuiklybase.chat.compose

import com.tencent.kuiklybase.chat.compose.core.ChatController
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatMessageSender
import com.tencent.kuiklybase.chat.compose.core.ChatStreamEvent
import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.compose.core.regenerateReply
import com.tencent.kuiklybase.chat.compose.core.showConversation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ChatControllerConversationTest {
    @Test
    fun showConversationDropsTheInFlightReply() = runTest(StandardTestDispatcher()) {
        val user = ChatUser("user")
        val gate = CompletableDeferred<Unit>()
        val sender = ChatMessageSender {
            flow {
                emit(ChatStreamEvent.TextDelta(text = "partial"))
                gate.await()
                emit(ChatStreamEvent.TextDelta(text = " late"))
                emit(ChatStreamEvent.Completed)
            }
        }
        val controller = ChatController(user, this, sender)
        controller.sendText("hi")
        testScheduler.runCurrent()
        val kept = ChatMessage("other", ChatUser("assistant", "Assistant"), listOf(MessagePart.Text("kept")))
        controller.showConversation(listOf(kept))
        gate.complete(Unit)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("other"), controller.state.messages.map { it.id })
        assertEquals("kept", controller.state.messages.single().text)
        assertFalse(controller.state.isSending)
    }

    @Test
    fun regenerateReplyKeepsTheUserMessage() = runTest(StandardTestDispatcher()) {
        val user = ChatUser("user")
        var attempts = 0
        val sender = ChatMessageSender {
            flow {
                attempts += 1
                emit(ChatStreamEvent.TextDelta(text = if (attempts == 1) "first" else "second"))
                emit(ChatStreamEvent.Completed)
            }
        }
        val controller = ChatController(user, this, sender)
        controller.sendText("ask")
        testScheduler.advanceUntilIdle()
        val assistantId = controller.state.messages.last().id
        controller.regenerateReply(assistantId)
        testScheduler.advanceUntilIdle()

        assertEquals(2, attempts)
        assertEquals(2, controller.state.messages.size)
        assertEquals("ask", controller.state.messages.first().text)
        assertEquals("second", controller.state.messages.last().text)
    }
}
