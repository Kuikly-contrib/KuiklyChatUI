package com.tencent.kuiklybase.chat.compose

import com.tencent.kuiklybase.chat.compose.core.ChatController
import com.tencent.kuiklybase.chat.compose.core.ChatHistoryPage
import com.tencent.kuiklybase.chat.compose.core.ChatIntent
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatMessageSender
import com.tencent.kuiklybase.chat.compose.core.ChatStreamEvent
import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.compose.core.MessageStatus
import com.tencent.kuiklybase.chat.compose.core.groupStatus
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatControllerTest {
    @Test
    fun sendWithoutSenderAddsSentMessage() = runTest {
        val user = ChatUser("user", "Yuki")
        val controller = ChatController(user, backgroundScope)

        assertTrue(controller.sendText(" hello "))
        assertEquals("hello", controller.state.messages.single().text)
        assertEquals(MessageStatus.SENT, controller.state.messages.single().status)
        assertFalse(controller.state.isSending)
    }

    @Test
    fun blankSendIsRejected() = runTest {
        val controller = ChatController(ChatUser("user"), backgroundScope)
        assertFalse(controller.sendText(""))
        assertFalse(controller.sendText("   "))
        assertTrue(controller.state.messages.isEmpty())
    }

    @Test
    fun streamDeltaCreatesAndCompletesAssistantMessage() = runTest(StandardTestDispatcher()) {
        val user = ChatUser("user")
        val sender = ChatMessageSender {
            flow {
                emit(ChatStreamEvent.TextDelta(text = "hel"))
                emit(ChatStreamEvent.TextDelta(text = "lo"))
                emit(ChatStreamEvent.Completed)
            }
        }
        val controller = ChatController(user, this, sender)

        assertTrue(controller.sendText("question"))
        testScheduler.advanceUntilIdle()

        assertEquals(2, controller.state.messages.size)
        assertEquals("hello", controller.state.messages.last().text)
        assertEquals(MessageStatus.SENT, controller.state.messages.last().status)
        assertEquals(MessageStatus.SENT, controller.state.messages.first().status)
        assertFalse(controller.state.isSending)
    }

    @Test
    fun failedMessageCanBeRetried() = runTest(StandardTestDispatcher()) {
        val user = ChatUser("user")
        var attempts = 0
        val sender = ChatMessageSender {
            flow {
                attempts += 1
                if (attempts == 1) {
                    emit(ChatStreamEvent.Failed(com.tencent.kuiklybase.chat.compose.core.ChatError("network")))
                } else {
                    emit(ChatStreamEvent.TextDelta(text = "ok"))
                    emit(ChatStreamEvent.Completed)
                }
            }
        }
        val controller = ChatController(user, this, sender)

        controller.sendText("retry me")
        testScheduler.advanceUntilIdle()
        val failedId = controller.state.messages.last().id
        assertEquals(MessageStatus.FAILED, controller.state.messages.last().status)

        controller.dispatch(ChatIntent.RetryMessage(failedId))
        testScheduler.advanceUntilIdle()

        assertEquals("ok", controller.state.messages.last().text)
        assertEquals(MessageStatus.SENT, controller.state.messages.last().status)
        assertEquals(2, attempts)
    }

    @Test
    fun assistantFailureRetriesUserPrompt() = runTest(StandardTestDispatcher()) {
        val user = ChatUser("user")
        var attempts = 0
        val sender = ChatMessageSender {
            flow {
                attempts += 1
                if (attempts == 1) {
                    emit(ChatStreamEvent.TextDelta(text = "partial"))
                    emit(ChatStreamEvent.Failed(com.tencent.kuiklybase.chat.compose.core.ChatError("network")))
                } else {
                    emit(ChatStreamEvent.TextDelta(text = "fixed"))
                    emit(ChatStreamEvent.Completed)
                }
            }
        }
        val controller = ChatController(user, this, sender)

        controller.sendText("original prompt")
        testScheduler.advanceUntilIdle()
        val failedAssistantId = controller.state.messages.last().id

        controller.dispatch(ChatIntent.RetryMessage(failedAssistantId))
        testScheduler.advanceUntilIdle()

        assertEquals("fixed", controller.state.messages.last().text)
        assertEquals("original prompt", controller.state.messages.first().text)
        assertEquals(2, attempts)
    }

    @Test
    fun stopGenerationCancelsAssistant() = runTest(StandardTestDispatcher()) {
        val user = ChatUser("user")
        val sender = ChatMessageSender {
            flow {
                emit(ChatStreamEvent.TextDelta(text = "partial"))
                awaitCancellation()
            }
        }
        val controller = ChatController(user, this, sender)

        controller.sendText("stop me")
        testScheduler.runCurrent()
        controller.dispatch(ChatIntent.StopGeneration)
        testScheduler.advanceUntilIdle()

        assertFalse(controller.state.isSending)
        assertEquals(MessageStatus.CANCELLED, controller.state.messages.last().status)
        assertEquals(MessageStatus.CANCELLED, controller.state.messages.first().status)
    }

    @Test
    fun loadEarlierPrependsAndDedupes() = runTest(StandardTestDispatcher()) {
        val user = ChatUser("user")
        val loader = com.tencent.kuiklybase.chat.compose.core.ChatHistoryLoader { beforeId ->
            when (beforeId) {
                null -> ChatHistoryPage(
                    messages = listOf(
                        ChatMessage("m1", user, listOf(MessagePart.Text("old")))
                    ),
                    hasMoreEarlier = true
                )
                "m1" -> ChatHistoryPage(
                    messages = listOf(
                        ChatMessage("m0", user, listOf(MessagePart.Text("older"))),
                        ChatMessage("m1", user, listOf(MessagePart.Text("old")))
                    ),
                    hasMoreEarlier = false
                )
                else -> ChatHistoryPage(emptyList(), false)
            }
        }
        val controller = ChatController(user, this, historyLoader = loader)
        testScheduler.advanceUntilIdle()

        assertEquals(1, controller.state.messages.size)
        controller.dispatch(ChatIntent.LoadEarlier)
        testScheduler.advanceUntilIdle()
        assertEquals(2, controller.state.messages.size)
        assertEquals("older", controller.state.messages.first().text)
        assertFalse(controller.state.hasMoreEarlier)
    }

    @Test
    fun unreadIncrementsWhenNotNearBottom() = runTest {
        val controller = ChatController(ChatUser("user"), backgroundScope)
        controller.dispatch(ChatIntent.SetListNearBottom(false))
        assertTrue(controller.sendText("hello"))
        assertEquals(1, controller.state.unreadCount)
        controller.dispatch(ChatIntent.SetListNearBottom(true))
        assertEquals(0, controller.state.unreadCount)
    }

    @Test
    fun disposeCancelsActiveRequest() = runTest(StandardTestDispatcher()) {
        val user = ChatUser("user")
        var cancelled = false
        val sender = ChatMessageSender {
            flow {
                try {
                    awaitCancellation()
                } catch (_: kotlinx.coroutines.CancellationException) {
                    cancelled = true
                    throw kotlinx.coroutines.CancellationException()
                }
            }
        }
        val controller = ChatController(user, this, sender)
        controller.sendText("dispose")
        testScheduler.runCurrent()
        controller.dispose()
        testScheduler.advanceUntilIdle()
        assertTrue(cancelled)
    }

    @Test
    fun groupStatusSeparatesDifferentAuthors() {
        val first = ChatMessage("1", ChatUser("a"), listOf(MessagePart.Text("one")), createdAt = 1L)
        val second = ChatMessage("2", ChatUser("b"), listOf(MessagePart.Text("two")), createdAt = 2L)

        assertEquals(
            com.tencent.kuiklybase.chat.compose.core.MessageGroupStatus.SINGLE,
            first.groupStatus(previous = null, next = second)
        )
    }
}
