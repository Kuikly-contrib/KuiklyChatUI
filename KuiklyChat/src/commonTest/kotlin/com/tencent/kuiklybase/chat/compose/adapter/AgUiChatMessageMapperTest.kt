package com.tencent.kuiklybase.chat.compose.adapter

import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.compose.core.MessageStatus
import com.tencent.kuiklybase.chat.compose.core.SYSTEM_CHAT_USER
import com.tencent.kuiklybase.chat.model.ChatMessage as LegacyChatMessage
import com.tencent.kuiklybase.chat.model.MessageStatus as LegacyMessageStatus
import com.tencent.kuiklybase.chat.model.MessageType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AgUiChatMessageMapperTest {
    @Test
    fun plainTextMapsToTextPart() {
        val legacy = LegacyChatMessage(
            id = "m1",
            content = "hello",
            senderId = "user",
            senderName = "Yuki",
            isSelf = true,
            status = LegacyMessageStatus.SENT
        )
        val mapped = legacy.toComposeChatMessage("user")
        assertEquals("hello", mapped.text)
        assertEquals(MessageStatus.SENT, mapped.status)
        assertTrue(mapped.parts.single() is MessagePart.Text)
    }

    @Test
    fun customAgUiKindMapsToCustomPart() {
        val legacy = LegacyChatMessage(
            id = "m2",
            content = "tool output",
            senderId = "assistant",
            isSelf = false,
            type = MessageType.CUSTOM,
            extra = mapOf("agUiKind" to "tool_result", "agentCardType" to "tool_call")
        )
        val mapped = legacy.toComposeChatMessage("user", ChatUser("assistant", "Assistant"))
        val custom = mapped.parts.single() as MessagePart.Custom
        assertEquals("tool_call", custom.type)
        assertEquals("tool output", custom.payload["text"])
    }

    @Test
    fun failedStatusMapsCorrectly() {
        val legacy = LegacyChatMessage(
            id = "m3",
            content = "oops",
            senderId = "user",
            isSelf = true,
            status = LegacyMessageStatus.FAILED
        )
        val mapped = legacy.toComposeChatMessage("user")
        assertEquals(MessageStatus.FAILED, mapped.status)
    }

    @Test
    fun plainSystemMapsToSystemNotice() {
        val legacy = LegacyChatMessage(
            id = "sys1",
            content = "Alice 加入了会话",
            senderId = "assistant",
            isSelf = false,
            type = MessageType.SYSTEM,
        )
        val mapped = legacy.toComposeChatMessage("user", ChatUser("assistant", "Assistant"))
        assertEquals(SYSTEM_CHAT_USER, mapped.author)
        val notice = mapped.parts.single()
        assertTrue(notice is MessagePart.SystemNotice)
        assertEquals("Alice 加入了会话", notice.text)
    }

    @Test
    fun systemWithAgUiKindStillMapsToCustom() {
        val legacy = LegacyChatMessage(
            id = "sys2",
            content = "reasoning",
            senderId = "assistant",
            isSelf = false,
            type = MessageType.SYSTEM,
            extra = mapOf("agUiKind" to "reasoning"),
        )
        val mapped = legacy.toComposeChatMessage("user", ChatUser("assistant", "Assistant"))
        val custom = mapped.parts.single() as MessagePart.Custom
        assertEquals("reasoning", custom.type)
    }
}
