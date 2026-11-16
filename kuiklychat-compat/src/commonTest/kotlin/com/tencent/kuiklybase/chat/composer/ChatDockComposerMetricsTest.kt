package com.tencent.kuiklybase.chat.composer

import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmAttachment
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmAttachmentKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChatDockComposerMetricsTest {
    @Test
    fun inputHeightUsesMeasuredBoundsInsteadOfCharacterCount() {
        assertEquals(DOCK_INPUT_LINE_HEIGHT, composerInputLineHeight(DOCK_INPUT_LINE_HEIGHT - 8f))
        assertEquals(28f, composerInputLineHeight(20f))
        assertEquals(composerInputLineHeight(12f) * DOCK_MAX_INPUT_LINES, composerMaxInputHeight(12f))
        assertEquals(
            composerInputLineHeight(16f) * DOCK_MAX_INPUT_LINES,
            composerMaxInputHeight(16f),
        )
        assertTrue(composerMaxInputHeight(24f) > DOCK_ACTION_SIZE)
    }

    @Test
    fun keyboardReplacesNavigationInset() {
        assertEquals(34f + DOCK_BOTTOM_GAP, composerDockBottom(0f, 34f))
        assertEquals(DOCK_MAX_NAVIGATION_INSET + DOCK_BOTTOM_GAP, composerDockBottom(0f, 80f))
        assertEquals(
            300f + DOCK_KEYBOARD_CLEARANCE + DOCK_BOTTOM_GAP,
            composerDockBottom(300f, 34f),
        )
        assertEquals(
            268f + DOCK_KEYBOARD_CLEARANCE + DOCK_BOTTOM_GAP,
            composerDockBottom(300f, 80f),
        )
    }

    @Test
    fun imageBytesBecomePreviewUri() {
        val attachment = LlmAttachment(
            id = "img-1",
            kind = LlmAttachmentKind.IMAGE,
            name = "shot.jpg",
            mimeType = "image/jpeg",
            bytesBase64 = "abc",
            thumbnailBase64 = "thumb",
        ).toMessageAttachment()
        assertEquals("img-1", attachment.id)
        assertEquals("data:image/jpeg;base64,abc", attachment.uri)
        assertEquals("data:image/jpeg;base64,thumb", attachment.previewUri)
    }

    @Test
    fun localPreviewUriIsWhatTheGalleryShows() {
        val attachment = LlmAttachment(
            id = "img-2",
            kind = LlmAttachmentKind.IMAGE,
            mimeType = "image/jpeg",
            bytesBase64 = "abc",
            thumbnailBase64 = "thumb",
            localPreviewUri = "file:///tmp/shot.jpg",
        ).toMessageAttachment()
        assertEquals("data:image/jpeg;base64,abc", attachment.uri)
        assertEquals("file:///tmp/shot.jpg", attachment.previewUri)
    }
}
