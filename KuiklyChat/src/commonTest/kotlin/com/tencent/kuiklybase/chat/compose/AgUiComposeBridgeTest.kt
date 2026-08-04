package com.tencent.kuiklybase.chat.compose

import com.tencent.kuiklybase.chat.agui.AgUiEvent
import com.tencent.kuiklybase.chat.agui.AgUiSession
import com.tencent.kuiklybase.chat.compose.adapter.AgUiComposeBridge
import com.tencent.kuiklybase.chat.compose.core.ChatController
import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.compose.core.MessageStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AgUiComposeBridgeTest {
    @Test
    fun mirrorsStructuredMessagesAndRemovesThemAfterReset() = runTest {
        val controller = ChatController(ChatUser("user"), this)
        val session = AgUiSession.forTesting(
            threadId = "thread-1",
            selfSenderId = "user"
        )
        val bridge = AgUiComposeBridge(session, currentUserId = "user")
        bridge.attach(controller)

        bridge.dispatch(
            AgUiEvent.ToolCallStart(
                toolCallId = "call-1",
                toolCallName = "search"
            )
        )
        bridge.dispatch(
            AgUiEvent.ToolCallResult(
                messageId = "result-1",
                toolCallId = "call-1",
                content = "done"
            )
        )

        val mirrored = controller.state.messages.single()
        assertEquals("tool_result", mirrored.parts.filterIsInstance<MessagePart.Custom>().single().type)
        assertEquals(MessageStatus.SENT, mirrored.status)

        bridge.reset()
        assertTrue(controller.state.messages.isEmpty())
    }

    @Test
    fun doesNotMirrorPlainTextMessages() = runTest {
        val controller = ChatController(ChatUser("user"), this)
        val session = AgUiSession.forTesting(selfSenderId = "user")
        val bridge = AgUiComposeBridge(session, currentUserId = "user")
        bridge.attach(controller)

        bridge.dispatch(AgUiEvent.TextMessageStart("text-1"))
        bridge.dispatch(AgUiEvent.TextMessageContent("text-1", "hello"))

        assertFalse(controller.state.messages.any { it.id == "text-1" })
    }
}
