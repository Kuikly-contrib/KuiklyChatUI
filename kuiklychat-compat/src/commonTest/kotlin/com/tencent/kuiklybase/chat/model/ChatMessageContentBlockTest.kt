package com.tencent.kuiklybase.chat.model

import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.compose.core.TextFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.tencent.kuiklybase.chat.model.sessionChatMessage

class ChatMessageContentBlockTest {

    @Test
    fun resolveParts_prefersExplicitParts() {
        val message = sessionChatMessage(
            id = "1",
            content = "legacy",
            isSelf = false,
            parts = listOf(MessagePart.Text("# Title", TextFormat.MARKDOWN)),
        )
        assertEquals(1, message.resolveParts().size)
        val text = message.resolveParts().single() as MessagePart.Text
        assertEquals(TextFormat.MARKDOWN, text.format)
    }

    @Test
    fun resolveParts_fallsBackToTextContent() {
        val message = sessionChatMessage(
            id = "2",
            content = "hello",
            isSelf = true,
            type = MessageType.TEXT,
        )
        val parts = message.resolveParts()
        assertEquals(1, parts.size)
        assertEquals("hello", (parts[0] as MessagePart.Text).value)
    }

    @Test
    fun attachmentDraft_toAttachment_usesUploadUri() {
        val draft = AttachmentDraft(
            previewUri = "file:///preview.jpg",
            uploadUri = "data:image/jpeg;base64,abc",
            mimeType = "image/jpeg",
            fileName = "photo.jpg",
        )
        val attachment = draft.toAttachment()
        assertEquals(AttachmentType.IMAGE, attachment.type)
        assertEquals("data:image/jpeg;base64,abc", attachment.url)
        assertEquals("photo.jpg", attachment.title)
    }

    @Test
    fun primaryTextContent_readsFromParts() {
        val message = sessionChatMessage(
            id = "3",
            content = "",
            isSelf = false,
            parts = listOf(
                MessagePart.Attachment(id = "img", uri = "x", mimeType = "image/unknown"),
                MessagePart.Text("answer"),
            ),
        )
        assertEquals("answer", message.primaryTextContent())
    }

    @Test
    fun resolveParts_includesTextAttachments() {
        val message = sessionChatMessage(
            id = "4",
            content = "see photo",
            isSelf = true,
            type = MessageType.TEXT,
            attachments = listOf(
                Attachment(AttachmentType.IMAGE, url = "https://img/a.jpg", mimeType = "image/jpeg"),
                Attachment(AttachmentType.IMAGE, url = "https://img/b.jpg", mimeType = "image/jpeg"),
            ),
        )
        val parts = message.resolveParts()
        assertEquals(3, parts.size)
        assertEquals("see photo", (parts[0] as MessagePart.Text).value)
        assertEquals("https://img/a.jpg", (parts[1] as MessagePart.Attachment).uri)
        assertEquals("https://img/b.jpg", (parts[2] as MessagePart.Attachment).uri)
    }

    @Test
    fun createComposerMessage_mixedTextAndImages_usesParts() {
        val message = ChatMessageHelper.createComposerMessage(
            text = "look",
            attachments = listOf(
                Attachment(AttachmentType.IMAGE, url = "https://img/a.jpg", mimeType = "image/jpeg"),
            ),
            isSelf = true,
            senderName = "me",
        )
        assertEquals(MessageType.TEXT, message.type)
        assertEquals(2, message.parts.size)
        assertTrue(message.parts[0] is MessagePart.Text)
        assertTrue(message.parts[1] is MessagePart.Attachment)
    }

    @Test
    fun createComposerMessage_singleImage_usesImageType() {
        val message = ChatMessageHelper.createComposerMessage(
            text = "",
            attachments = listOf(
                Attachment(AttachmentType.IMAGE, url = "https://img/a.jpg", mimeType = "image/jpeg"),
            ),
            isSelf = true,
        )
        assertEquals(MessageType.IMAGE, message.type)
        val attachment = message.parts.single() as MessagePart.Attachment
        assertEquals("https://img/a.jpg", attachment.uri)
        assertEquals("https://img/a.jpg", message.content)
    }

    @Test
    fun updateAiStreamingContent_writesMarkdownTextPart() {
        val placeholder = ChatMessageHelper.createAiStreamingPlaceholder(senderName = "bot")
        val updated = ChatMessageHelper.updateAiStreamingContent(placeholder, "# hi")
        assertEquals("# hi", updated.content)
        assertEquals(MessageStatus.STREAMING, updated.sessionStatus)
        val text = updated.parts.single() as MessagePart.Text
        assertEquals(TextFormat.MARKDOWN, text.format)
    }

    @Test
    fun updateContent_streamingTextKeepsTextPart() {
        val streaming = ChatMessageHelper.persistParts(
            sessionChatMessage(
                id = "stream-1",
                content = "",
                isSelf = false,
                type = MessageType.TEXT,
                status = MessageStatus.STREAMING,
                extra = mapOf("agUiKind" to "streaming"),
            )
        )
        val updated = ChatMessageHelper.updateContent(streaming, "hello **md**")
        val text = updated.parts.single() as MessagePart.Text
        assertEquals("hello **md**", text.value)
        assertEquals(TextFormat.PLAIN, text.format)
        val mapped = updated.toCore("user")
        assertEquals("hello **md**", (mapped.parts.single() as MessagePart.Text).value)
    }

    @Test
    fun withExtra_reasoningRebuildsCustomPart() {
        val system = ChatMessageHelper.createSystemMessage("thinking")
        val reasoning = ChatMessageHelper.withExtra(system, mapOf("agUiKind" to "reasoning"))
        assertEquals(MessageType.SYSTEM, reasoning.type)
        val mapped = reasoning.toCore("user")
        val custom = mapped.parts.single() as MessagePart.Custom
        assertEquals("reasoning", custom.type)
        assertEquals("thinking", custom.payload["text"])
    }

    @Test
    fun constructorStoresCanonicalParts() {
        val message = sessionChatMessage(id = "c1", content = "hi", isSelf = true)
        val text = message.parts.single() as MessagePart.Text
        assertEquals("hi", text.value)
        assertEquals(message.parts, message.toCore("user").parts)
    }

    @Test
    fun copyContentRebuildsCanonicalTextPart() {
        val original = sessionChatMessage(id = "c2", content = "v1", isSelf = false)
        val updated = ChatMessageHelper.updateContent(original, "v2")
        assertEquals("v2", updated.content)
        assertEquals("v2", (updated.parts.single() as MessagePart.Text).value)
    }

    @Test
    fun toSessionMessageRoundTripsParts() {
        val original = ChatMessageHelper.createTextMessage("round", isSelf = true, senderId = "user")
        val core = original.toCore("user")
        val session = core.toSessionMessage("user")
        assertEquals(core.parts, session.parts)
        assertEquals(true, session.isSelf)
        assertEquals(MessageType.TEXT, session.type)
    }
}
