package com.tencent.kuiklybase.chat.compose.core

/** How [MessagePart.Text] with [TextFormat.MARKDOWN] behaves while [MessageStatus.STREAMING]. */
public enum class StreamingMarkdownMode {
    /** Show plain text plus an optional cursor until the message completes. */
    PLAIN_UNTIL_COMPLETE,
    /** Re-render Markdown on every content update (higher cost). */
    RENDER_EACH_UPDATE,
}

public data class ChatTextRenderConfig(
    val streamingMarkdownMode: StreamingMarkdownMode = StreamingMarkdownMode.PLAIN_UNTIL_COMPLETE,
    val showStreamingCursor: Boolean = true,
) {
    public companion object {
        public val Default: ChatTextRenderConfig = ChatTextRenderConfig()
    }
}

public enum class TextPartDisplayMode {
    PLAIN,
    PLAIN_WITH_CURSOR,
    MARKDOWN,
}

public fun resolveTextPartDisplayMode(
    format: TextFormat,
    status: MessageStatus,
    config: ChatTextRenderConfig,
): TextPartDisplayMode {
    if (format == TextFormat.PLAIN) {
        return if (config.showStreamingCursor && status == MessageStatus.STREAMING) {
            TextPartDisplayMode.PLAIN_WITH_CURSOR
        } else {
            TextPartDisplayMode.PLAIN
        }
    }
    if (status == MessageStatus.STREAMING &&
        config.streamingMarkdownMode == StreamingMarkdownMode.PLAIN_UNTIL_COMPLETE
    ) {
        return if (config.showStreamingCursor) {
            TextPartDisplayMode.PLAIN_WITH_CURSOR
        } else {
            TextPartDisplayMode.PLAIN
        }
    }
    return TextPartDisplayMode.MARKDOWN
}
