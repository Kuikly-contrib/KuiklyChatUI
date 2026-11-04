package com.tencent.kuiklybase.chat.kuiklyai

import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmAttachment
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmAttachmentKind
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmProviderCapabilities
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LlmProviderCapabilitiesTest {

    @Test
    fun rejectsUnsupportedKindAndOversizedAttachment() {
        val capabilities = LlmProviderCapabilities(
            attachmentKinds = setOf(LlmAttachmentKind.IMAGE),
            acceptedMimeTypes = setOf("image/png"),
            maxAttachmentBytes = 10
        )

        assertTrue(
            capabilities.supports(
                LlmAttachment(
                    id = "image",
                    kind = LlmAttachmentKind.IMAGE,
                    mimeType = "image/png",
                    sizeBytes = 10
                )
            )
        )
        assertFalse(
            capabilities.supports(
                LlmAttachment(id = "file", kind = LlmAttachmentKind.FILE)
            )
        )
        assertFalse(
            capabilities.supports(
                LlmAttachment(
                    id = "large",
                    kind = LlmAttachmentKind.IMAGE,
                    mimeType = "image/png",
                    sizeBytes = 11
                )
            )
        )
    }
}
