package com.tencent.kuiklybase.chat.compose

import com.tencent.kuiklybase.chat.agui.AgUiEvent
import com.tencent.kuiklybase.chat.agentcard.AgentCard
import com.tencent.kuiklybase.chat.agentcard.AgUiCardSession
import com.tencent.kuiklybase.chat.compose.adapter.AgentCardComposeBridge
import com.tencent.kuiklybase.chat.compose.core.ChatController
import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.compose.core.MessageStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AgentCardComposeBridgeTest {
    @Test
    fun mirrorsTypedCardsAndKeepsStreamingState() = runTest {
        val session = AgUiCardSession(cards = mutableListOf())
        val controller = ChatController(ChatUser("user"), this)
        val bridge = AgentCardComposeBridge(session)
        bridge.attach(controller)

        bridge.dispatch(AgUiEvent.ToolCallStart("call-1", "search"))
        val message = controller.state.messages.single()
        val card = message.parts.filterIsInstance<MessagePart.AgentCardContent>().single().card

        assertTrue(card is AgentCard.ToolCall)
        assertEquals(MessageStatus.STREAMING, message.status)

        bridge.dispatch(AgUiEvent.ToolCallResult("result-1", "call-1", "done"))
        assertEquals(MessageStatus.SENT, controller.state.messages.single().status)
        assertEquals("done", (controller.state.messages.single().parts.single() as MessagePart.AgentCardContent)
            .card.let { it as AgentCard.ToolCall }.result)
    }

    @Test
    fun resetRemovesOnlyMirroredCards() = runTest {
        val session = AgUiCardSession(cards = mutableListOf())
        val controller = ChatController(
            ChatUser("user"),
            this,
            initialMessages = listOf(
                com.tencent.kuiklybase.chat.compose.core.ChatMessage(
                    id = "local",
                    author = ChatUser("user"),
                    parts = listOf(MessagePart.Text("local"))
                )
            )
        )
        val bridge = AgentCardComposeBridge(session)
        bridge.attach(controller)

        bridge.dispatch(AgUiEvent.RunError("failure"))
        bridge.reset()

        assertEquals(listOf("local"), controller.state.messages.map { it.id })
    }
}
