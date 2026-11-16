package com.tencent.kuiklybase.chat.compose

import com.tencent.kuiklybase.chat.compose.core.ChatConnectionState
import com.tencent.kuiklybase.chat.compose.core.ChatError
import com.tencent.kuiklybase.chat.compose.core.ChatController
import com.tencent.kuiklybase.chat.compose.core.ChatIntent
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

class ChatControllerQueueTest {
    @Test
    fun logsStructuredSendEventsWithoutMessageText() = runTest(StandardTestDispatcher()) {
        val events = mutableListOf<Pair<String, Map<String, String>>>()
        val sender = ChatMessageSender {
            flow {
                emit(ChatStreamEvent.TextDelta(text = "secret response"))
                emit(ChatStreamEvent.Completed)
            }
        }
        val controller = ChatController(
            currentUser = ChatUser("user"),
            scope = this,
            sender = sender,
            logger = { _, event, attributes, _ ->
                events += event to attributes
            },
        )

        controller.sendText("secret prompt")
        testScheduler.advanceUntilIdle()

        assertEquals(
            listOf("send_started", "stream_first_token", "send_completed"),
            events.map { it.first },
        )
        assertTrue(events.all { (_, attributes) -> attributes.none { (_, value) -> value.contains("secret") } })
        assertEquals("user-0", events.first().second["message_id"])
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
                    emit(ChatStreamEvent.Failed(ChatError("network")))
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
                    emit(ChatStreamEvent.Failed(ChatError("network")))
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
                kotlinx.coroutines.awaitCancellation()
            }
        }
        val controller = ChatController(user, this, sender)

        controller.sendText("stop me")
        testScheduler.runCurrent()
        controller.dispatch(ChatIntent.StopGeneration)
        testScheduler.advanceUntilIdle()

        assertFalse(controller.state.isSending)
        assertEquals(MessageStatus.STREAMING_PARTIAL, controller.state.messages.last().status)
        assertEquals(MessageStatus.CANCELLED, controller.state.messages.first().status)
        assertEquals(listOf(controller.state.messages.last().id), controller.state.partialMessageIds)
    }

    @Test
    fun messagesQueueWhileSendingAndResumeInOrder() = runTest(StandardTestDispatcher()) {
        val gate = CompletableDeferred<Unit>()
        val user = ChatUser("user")
        val sent = mutableListOf<String>()
        val sender = ChatMessageSender { message ->
            flow {
                sent += message.text
                gate.await()
                emit(ChatStreamEvent.Completed)
            }
        }
        val controller = ChatController(user, this, sender)

        assertTrue(controller.sendText("first"))
        assertTrue(controller.sendText("second"))
        assertTrue(controller.sendText("third"))
        testScheduler.runCurrent()

        assertEquals(listOf("first"), sent)
        assertEquals(listOf("second", "third"), controller.state.queuedMessages.map { it.text })

        gate.complete(Unit)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("first", "second", "third"), sent)
        assertTrue(controller.state.queuedMessages.isEmpty())
        assertTrue(controller.state.messages.all { it.status == MessageStatus.SENT })
    }

    @Test
    fun disconnectedQueuesMessagesAndConnectedResumesThem() = runTest(StandardTestDispatcher()) {
        val user = ChatUser("user")
        val sent = mutableListOf<String>()
        val sender = ChatMessageSender { message ->
            flow {
                sent += message.text
                emit(ChatStreamEvent.Completed)
            }
        }
        val controller = ChatController(user, this, sender)
        controller.setConnectionState(ChatConnectionState.DISCONNECTED)

        assertTrue(controller.sendText("offline"))
        assertEquals(MessageStatus.SENDING, controller.state.messages.single().status)
        assertEquals(1, controller.state.queuedMessages.size)
        assertEquals(0, sent.size)

        controller.setConnectionState(ChatConnectionState.CONNECTED)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("offline"), sent)
        assertTrue(controller.state.queuedMessages.isEmpty())
    }

    @Test
    fun failedStreamQueuesUserMessageForRetry() = runTest(StandardTestDispatcher()) {
        val user = ChatUser("user")
        var attempts = 0
        val sender = ChatMessageSender {
            flow {
                attempts += 1
                if (attempts == 1) {
                    emit(ChatStreamEvent.Failed(ChatError("network", retryable = true)))
                } else {
                    emit(ChatStreamEvent.TextDelta(text = "ok"))
                    emit(ChatStreamEvent.Completed)
                }
            }
        }
        val controller = ChatController(user, this, sender)
        controller.sendText("will retry")
        testScheduler.advanceUntilIdle()

        assertEquals(ChatConnectionState.DISCONNECTED, controller.state.connectionState)
        assertEquals(listOf("will retry"), controller.state.queuedMessages.map { it.text })
        assertEquals(1, attempts)

        controller.setConnectionState(ChatConnectionState.CONNECTED)
        testScheduler.advanceUntilIdle()
        assertEquals("ok", controller.state.messages.last().text)
        assertEquals(2, attempts)
    }

    @Test
    fun partialAssistantMessageIsRetriable() = runTest(StandardTestDispatcher()) {
        val user = ChatUser("user")
        val sender = ChatMessageSender {
            flow {
                emit(ChatStreamEvent.TextDelta(text = "partial"))
                emit(ChatStreamEvent.Failed(ChatError("stopped", retryable = false)))
            }
        }
        val controller = ChatController(user, this, sender)
        controller.sendText("question")
        testScheduler.advanceUntilIdle()

        val assistant = controller.state.messages.last()
        assertEquals(MessageStatus.STREAMING_PARTIAL, assistant.status)
        assertEquals("partial", assistant.text)
        assertEquals(listOf(assistant.id), controller.state.partialMessageIds)

        controller.dispatch(ChatIntent.RetryMessage(assistant.id))
        testScheduler.advanceUntilIdle()
        assertEquals(MessageStatus.STREAMING_PARTIAL, controller.state.messages.last().status)
    }

    @Test
    fun draftStateLivesOnController() = runTest {
        val controller = ChatController(ChatUser("user"), backgroundScope)
        controller.setDraft("hello")
        controller.setDraftAttachments(listOf(MessagePart.Attachment("a", "https://example.com/a.png")))
        assertEquals("hello", controller.state.draftText)
        assertEquals("a", controller.state.draftAttachments.single().id)

        controller.clearDraft()
        assertEquals("", controller.state.draftText)
        assertTrue(controller.state.draftAttachments.isEmpty())
    }
}
