package com.tencent.kuiklybase.chat.compose.adapter

import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.compose.core.MessageStatus
import com.tencent.kuiklybase.chat.compose.core.SYSTEM_CHAT_USER
import com.tencent.kuiklybase.chat.model.Attachment
import com.tencent.kuiklybase.chat.model.AttachmentType
import com.tencent.kuiklybase.chat.model.ChatMessageHelper
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

    @Test
    fun imageMessageMapsToAttachmentWithoutDuplicateText() {
        val legacy = ChatMessageHelper.createImageMessage(
            imageUrl = "https://cdn.example/photo.jpg",
            isSelf = true,
            senderId = "user",
        )
        val mapped = legacy.toComposeChatMessage("user")
        assertEquals(1, mapped.parts.size)
        val attachment = mapped.parts.single() as MessagePart.Attachment
        assertEquals("https://cdn.example/photo.jpg", attachment.uri)
        assertEquals("image/unknown", attachment.mimeType)
    }

    @Test
    fun fileMessageMapsToAttachmentWithoutDuplicateFilenameText() {
        val legacy = ChatMessageHelper.createFileMessage(
            fileUrl = "https://cdn.example/spec.pdf",
            fileName = "spec.pdf",
            isSelf = false,
            senderId = "assistant",
            mimeType = "application/pdf",
            fileSize = 2048L,
        )
        val mapped = legacy.toComposeChatMessage("user", ChatUser("assistant", "Assistant"))
        assertEquals(1, mapped.parts.size)
        val attachment = mapped.parts.single() as MessagePart.Attachment
        assertEquals("https://cdn.example/spec.pdf", attachment.uri)
        assertEquals("spec.pdf", attachment.name)
        assertEquals("application/pdf", attachment.mimeType)
    }

    @Test
    fun imageCaptionPreservedWhenDistinctFromUrl() {
        val legacy = LegacyChatMessage(
            id = "img-caption",
            content = "请看附图",
            isSelf = false,
            senderId = "assistant",
            type = MessageType.IMAGE,
            attachments = listOf(
                Attachment(
                    type = AttachmentType.IMAGE,
                    url = "https://cdn.example/photo.jpg",
                    mimeType = "image/jpeg",
                )
            ),
        )
        val mapped = legacy.toComposeChatMessage("user", ChatUser("assistant", "Assistant"))
        assertEquals(2, mapped.parts.size)
        assertTrue(mapped.parts[0] is MessagePart.Text)
        assertEquals("请看附图", (mapped.parts[0] as MessagePart.Text).value)
        assertTrue(mapped.parts[1] is MessagePart.Attachment)
    }
}
