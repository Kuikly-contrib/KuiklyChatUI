package com.tencent.kuiklybase.chat.composer

import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmAttachment
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmAttachmentKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChatDockComposerMetricsTest {
    @Test
    fun lineCountCapsAndWraps() {
        assertEquals(1, estimateComposerLineCount("", maxLines = 5))
        assertEquals(5, estimateComposerLineCount("\n\n\n\n\n\n", maxLines = 5))
        assertEquals(2, estimateComposerLineCount("x".repeat(21), maxLines = 5, charsPerLine = 20))
    }

    @Test
    fun keyboardReplacesNavigationInset() {
        assertEquals(34f + DOCK_BOTTOM_GAP, composerDockBottom(0f, 34f))
        assertEquals(DOCK_MAX_NAVIGATION_INSET + DOCK_BOTTOM_GAP, composerDockBottom(0f, 80f))
        assertEquals(320f + DOCK_BOTTOM_GAP, composerDockBottom(300f, 34f))
        assertEquals(288f + DOCK_BOTTOM_GAP, composerDockBottom(300f, 80f))
    }

    @Test
    fun panelGrowsForLinesAndAttachments() {
        assertEquals(DOCK_COLLAPSED_HEIGHT, composerPanelHeight(false, 1, false))
        val aboveOneLine = composerPanelHeight(false, 1, false, textAboveIcons = true)
        assertTrue(aboveOneLine > DOCK_COLLAPSED_HEIGHT)
        assertEquals(
            aboveOneLine + DOCK_INPUT_LINE_HEIGHT,
            composerPanelHeight(false, 2, false),
        )
        assertEquals(
            composerPanelHeight(false, 2, false) + DOCK_INPUT_LINE_HEIGHT,
            composerPanelHeight(false, 3, false),
        )
        assertEquals(
            composerPanelHeight(false, 2, false) + (DOCK_MAX_INPUT_LINES - 2) * DOCK_INPUT_LINE_HEIGHT,
            composerPanelHeight(false, DOCK_MAX_INPUT_LINES, false),
        )
        assertEquals(
            composerPanelHeight(false, DOCK_MAX_INPUT_LINES, false),
            composerPanelHeight(false, DOCK_MAX_INPUT_LINES + 4, false),
        )
        val expanded = composerPanelHeight(true, 1, false)
        assertTrue(expanded > DOCK_COLLAPSED_HEIGHT)
        val withImages = composerPanelHeight(true, 1, true)
        assertTrue(withImages > DOCK_ATTACHMENT_STRIP_HEIGHT + 15f + DOCK_INPUT_LINE_HEIGHT)
        assertEquals(
            withImages + DOCK_INPUT_LINE_HEIGHT,
            composerPanelHeight(true, 2, true),
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
