package com.tencent.kuiklybase.chat.compose

import com.tencent.kuiklybase.chat.compose.core.ChatController
import com.tencent.kuiklybase.chat.compose.core.ChatHistoryLoader
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatMessageSender
import com.tencent.kuiklybase.chat.compose.core.ChatStreamEvent
import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.compose.core.MessageStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatControllerRegressionTest {
    @Test
    fun externalStreamingTerminalStateReleasesSendingLock() = runTest {
        val controller = ChatController(ChatUser("user"), backgroundScope)
        controller.upsertExternalMessage(
            ChatMessage("assistant-1", ChatUser("assistant"), emptyList(), MessageStatus.STREAMING),
        )
        assertTrue(controller.state.isSending)
        controller.upsertExternalMessage(
            ChatMessage("assistant-1", ChatUser("assistant"), emptyList(), MessageStatus.SENT),
        )
        assertFalse(controller.state.isSending)
        assertTrue(controller.sendText("after stream"))
    }

    @Test
    fun textDeltaPreservesNonTextParts() = runTest(StandardTestDispatcher()) {
        val assistantId = "assistant-1"
        val attachment = MessagePart.Attachment("a1", "https://example.com/a.png")
        val sender = ChatMessageSender {
            flow {
                emit(
                    ChatStreamEvent.ReplaceMessage(
                        ChatMessage(
                            assistantId,
                            ChatUser("assistant"),
                            listOf(attachment),
                            MessageStatus.STREAMING,
                        ),
                    ),
                )
                emit(ChatStreamEvent.TextDelta(assistantId, "answer"))
                emit(ChatStreamEvent.Completed)
            }
        }
        val controller = ChatController(ChatUser("user"), this, sender)
        controller.sendText("question")
        testScheduler.advanceUntilIdle()
        val assistant = controller.state.messages.last()
        assertEquals(listOf(attachment), assistant.parts.filterIsInstance<MessagePart.Attachment>())
        assertEquals("answer", assistant.text)
    }

    @Test
    fun localMessageIdSkipsRestoredHistoryIds() = runTest {
        val restored = ChatMessage("user-0", ChatUser("user"), listOf(MessagePart.Text("old")))
        val controller = ChatController(
            ChatUser("user"),
            backgroundScope,
            initialMessages = listOf(restored),
        )
        assertTrue(controller.sendText("new"))
        assertTrue(controller.state.messages.last().id != "user-0")
    }

    @Test
    fun historyFailureDoesNotStopActiveGeneration() = runTest(StandardTestDispatcher()) {
        val gate = CompletableDeferred<Unit>()
        val sender = ChatMessageSender {
            flow {
                emit(ChatStreamEvent.TextDelta(text = "partial"))
                gate.await()
                emit(ChatStreamEvent.Completed)
            }
        }
        val loader = ChatHistoryLoader { throw IllegalStateException("history unavailable") }
        val controller = ChatController(
            ChatUser("user"),
            this,
            sender = sender,
            historyLoader = loader,
            initialMessages = listOf(
                ChatMessage("m1", ChatUser("assistant"), listOf(MessagePart.Text("old"))),
            ),
        )
        controller.sendText("question")
        testScheduler.runCurrent()
        controller.loadEarlier()
        testScheduler.advanceUntilIdle()
        assertTrue(controller.state.isSending)
        assertEquals(MessageStatus.STREAMING, controller.state.messages.last().status)
        gate.complete(Unit)
        testScheduler.advanceUntilIdle()
    }
}
