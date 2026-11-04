package com.tencent.kuiklybase.chat.compose.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AttachmentUriCodecTest {
    @Test
    fun roundTripDataUri() {
        val uri = AttachmentUriCodec.toDataUri("image/png", "abc123")
        assertEquals("data:image/png;base64,abc123", uri)
        val parsed = AttachmentUriCodec.parseDataUri(uri)
        assertNotNull(parsed)
        assertEquals("image/png", parsed.mimeType)
        assertEquals("abc123", parsed.base64Payload)
    }

    @Test
    fun parseRejectsInvalidDataUri() {
        assertNull(AttachmentUriCodec.parseDataUri("https://example.com/a.png"))
        assertNull(AttachmentUriCodec.parseDataUri("data:image/png,abc"))
        assertNull(AttachmentUriCodec.parseDataUri("data:;base64,abc"))
        assertNull(AttachmentUriCodec.parseDataUri("data:image/png;base64,"))
    }
}
