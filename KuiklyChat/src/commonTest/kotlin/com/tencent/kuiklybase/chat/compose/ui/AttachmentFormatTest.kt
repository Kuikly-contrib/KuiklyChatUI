package com.tencent.kuiklybase.chat.compose.ui

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
}
