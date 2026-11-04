package com.tencent.kuiklybase.chat.compose.core

import kotlin.test.Test
import kotlin.test.assertEquals

class TextPartRenderModeTest {
    @Test
    fun plainFormatAlwaysRendersPlain() {
        assertEquals(
            TextPartDisplayMode.PLAIN,
            resolveTextPartDisplayMode(
                format = TextFormat.PLAIN,
                status = MessageStatus.SENT,
                config = ChatTextRenderConfig.Default,
            ),
        )
    }

    @Test
    fun streamingPlainShowsCursorWhenEnabled() {
        assertEquals(
            TextPartDisplayMode.PLAIN_WITH_CURSOR,
            resolveTextPartDisplayMode(
                format = TextFormat.PLAIN,
                status = MessageStatus.STREAMING,
                config = ChatTextRenderConfig(showStreamingCursor = true),
            ),
        )
    }

    @Test
    fun streamingMarkdownDefaultsToPlainUntilComplete() {
        assertEquals(
            TextPartDisplayMode.PLAIN_WITH_CURSOR,
            resolveTextPartDisplayMode(
                format = TextFormat.MARKDOWN,
                status = MessageStatus.STREAMING,
                config = ChatTextRenderConfig.Default,
            ),
        )
    }

    @Test
    fun streamingMarkdownCanRenderEachUpdate() {
        assertEquals(
            TextPartDisplayMode.MARKDOWN,
            resolveTextPartDisplayMode(
                format = TextFormat.MARKDOWN,
                status = MessageStatus.STREAMING,
                config = ChatTextRenderConfig(
                    streamingMarkdownMode = StreamingMarkdownMode.RENDER_EACH_UPDATE,
                ),
            ),
        )
    }

    @Test
    fun completedMarkdownAlwaysRendersMarkdown() {
        assertEquals(
            TextPartDisplayMode.MARKDOWN,
            resolveTextPartDisplayMode(
                format = TextFormat.MARKDOWN,
                status = MessageStatus.SENT,
                config = ChatTextRenderConfig.Default,
            ),
        )
    }
}
