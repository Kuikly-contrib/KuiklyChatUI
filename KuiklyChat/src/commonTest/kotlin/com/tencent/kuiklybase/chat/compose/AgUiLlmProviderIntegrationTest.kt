package com.tencent.kuiklybase.chat.compose

import com.tencent.kuiklybase.chat.agentcard.AgUiCardSession
import com.tencent.kuiklybase.chat.agentcard.AgentCard
import com.tencent.kuiklybase.chat.agui.AgUiSession
import com.tencent.kuiklybase.chat.compose.adapter.AgentCardComposeBridge
import com.tencent.kuiklybase.chat.compose.adapter.AgUiComposeBridge
import com.tencent.kuiklybase.chat.compose.adapter.LlmChatAdapter
import com.tencent.kuiklybase.chat.compose.core.ChatController
import com.tencent.kuiklybase.chat.compose.core.ChatIntent
import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.compose.core.MessageStatus
import com.tencent.kuiklybase.chat.kuiklyai.provider.HangingSseLlmTransport
import com.tencent.kuiklybase.chat.kuiklyai.provider.AgUiLlmProvider
import com.tencent.kuiklybase.chat.kuiklyai.provider.AgUiScriptedScenarios
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmTransport
import com.tencent.kuiklybase.chat.kuiklyai.provider.ScriptedSseLlmTransport
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AgUiLlmProviderIntegrationTest {
    @Test
    fun scriptedTextStreamCompletesAssistantMessage() = runTest(StandardTestDispatcher()) {
        val controller = createAgentCardController(AgUiScriptedScenarios.textOnly)
        controller.sendText("hello")
        testScheduler.advanceUntilIdle()

        assertEquals(2, controller.state.messages.size)
        assertEquals("Hello Compose", controller.state.messages.last().text)
        assertEquals(MessageStatus.SENT, controller.state.messages.last().status)
        assertEquals(MessageStatus.SENT, controller.state.messages.first().status)
    }

    @Test
    fun scriptedToolCallAddsAgentCardWithoutDuplicateText() = runTest(StandardTestDispatcher()) {
        val cardSession = AgUiCardSession(cards = mutableListOf())
        val bridge = AgentCardComposeBridge(cardSession)
        val controller = createController(
            scenario = AgUiScriptedScenarios.textAndToolCall,
            onEvent = bridge.onEvent
        )
        bridge.attach(controller)

        controller.sendText("search")
        testScheduler.advanceUntilIdle()

        assertTrue(controller.state.messages.any { it.parts.any { p -> p is MessagePart.AgentCardContent } })
        val assistantText = controller.state.messages
            .filter { it.author.id != controller.currentUser.id }
            .map { it.text }
            .joinToString("")
        assertEquals("Searching done", assistantText)
        assertTrue(cardSession.cards.any { it is AgentCard.ToolCall })
    }

    @Test
    fun agUiComposeBridgeMirrorsStructuredMessages() = runTest(StandardTestDispatcher()) {
        val session = AgUiSession.forTesting(threadId = "t1", selfSenderId = "user")
        val bridge = AgUiComposeBridge(session, currentUserId = "user")
        val controller = createController(
            scenario = AgUiScriptedScenarios.textAndToolCall,
            onEvent = bridge.onEvent
        )
        bridge.attach(controller)

        controller.sendText("tool")
        testScheduler.advanceUntilIdle()

        assertTrue(session.messages.any { message ->
            message.extra["agUiKind"] != null && message.extra["agUiKind"] != "streaming"
        })
    }

    @Test
    fun runErrorMarksFailureAndStopsSending() = runTest(StandardTestDispatcher()) {
        val controller = createAgentCardController(AgUiScriptedScenarios.runError)
        controller.sendText("fail")
        testScheduler.advanceUntilIdle()

        assertFalse(controller.state.isSending)
        assertTrue(
            controller.state.messages.any { it.status == MessageStatus.FAILED } ||
                controller.state.error != null
        )
    }

    @Test
    fun stopGenerationCancelsScriptedStream() = runTest(StandardTestDispatcher()) {
        val firstPayload = AgUiScriptedScenarios.disconnect.first()
        val transport = HangingSseLlmTransport(firstPayload)
        val controller = createController(scenario = emptyList(), transport = transport)
        controller.sendText("stop")
        testScheduler.runCurrent()
        controller.dispatch(ChatIntent.StopGeneration)
        testScheduler.advanceUntilIdle()

        assertFalse(controller.state.isSending)
    }

    private fun TestScope.createAgentCardController(scenario: List<String>): ChatController {
        val cardSession = AgUiCardSession(cards = mutableListOf())
        val bridge = AgentCardComposeBridge(cardSession)
        val controller = createController(scenario, bridge.onEvent)
        bridge.attach(controller)
        return controller
    }

    private fun TestScope.createController(
        scenario: List<String>,
        onEvent: ((com.tencent.kuiklybase.chat.agui.AgUiEvent) -> Unit)? = null,
        transport: LlmTransport = ScriptedSseLlmTransport(scenario)
    ): ChatController {
        val provider = AgUiLlmProvider(
            transport = transport,
            endpoint = "memory://agui",
            onEvent = onEvent
        )
        val adapter = LlmChatAdapter(provider, ChatUser("user", "You"), ChatUser("assistant", "Assistant"))
        return ChatController(
            currentUser = ChatUser("user", "You"),
            scope = this,
            sender = adapter.sender
        )
    }
}
