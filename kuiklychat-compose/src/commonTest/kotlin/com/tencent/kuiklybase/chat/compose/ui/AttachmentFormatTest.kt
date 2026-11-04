package com.tencent.kuiklybase.chat.compose.ui

import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AttachmentFormatTest {
    @Test
    fun isImageMimeTypeOnlyForImagePrefix() {
        assertTrue(isImageMimeType("image/jpeg"))
        assertFalse(isImageMimeType("application/pdf"))
        assertFalse(isImageMimeType(""))
    }

    @Test
    fun formatFileSizeUsesBinaryUnits() {
        assertEquals("", formatFileSize(0))
        assertEquals("512 B", formatFileSize(512))
        assertEquals("1 KB", formatFileSize(1024))
        assertEquals("1.5 MB", formatFileSize(1024 * 1024 + 512 * 1024))
    }

    @Test
    fun attachmentDisplaySourcePrefersPreview() {
        val attachment = MessagePart.Attachment(
            id = "a1",
            uri = "https://origin.example/full.png",
            previewUri = "data:image/png;base64,abc",
        )
        assertEquals("data:image/png;base64,abc", attachmentDisplaySource(attachment))
        assertEquals("https://origin.example/full.png", attachment.uri)
    }

    @Test
    fun isDisplayableImageSourceAcceptsHttpAndDataImage() {
        assertTrue(isDisplayableImageSource("https://cdn.example/a.png"))
        assertTrue(isDisplayableImageSource("data:image/png;base64,abc"))
        assertFalse(isDisplayableImageSource("data:application/pdf;base64,abc"))
        assertFalse(isDisplayableImageSource(""))
    }

    @Test
    fun isImageOnlyMessageRequiresAllImageAttachments() {
        val user = ChatUser("user")
        val image = MessagePart.Attachment(id = "a1", uri = "https://example.com/a.png", mimeType = "image/png")
        val file = MessagePart.Attachment(id = "a2", uri = "https://example.com/a.pdf", mimeType = "application/pdf")
        val text = MessagePart.Text("hello")

        assertTrue(isImageOnlyMessage(ChatMessage("m1", user, listOf(image))))
        assertFalse(isImageOnlyMessage(ChatMessage("m2", user, listOf(image, text))))
        assertFalse(isImageOnlyMessage(ChatMessage("m3", user, listOf(file))))
        assertFalse(isImageOnlyMessage(ChatMessage("m4", user, emptyList())))
    }
}
