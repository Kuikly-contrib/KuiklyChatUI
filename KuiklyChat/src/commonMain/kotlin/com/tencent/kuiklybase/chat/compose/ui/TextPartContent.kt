package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatTextRenderConfig
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.compose.core.TextPartDisplayMode
import com.tencent.kuiklybase.chat.compose.core.resolveTextPartDisplayMode
import com.tencent.kuiklybase.chat.compose.markdown.chatMarkdownConfig
import com.tencent.kuiklybase.markdown.compose.MarkdownView

private const val STREAMING_CURSOR = " \u258E"

@Composable
internal fun TextPartContent(
    message: ChatMessage,
    part: MessagePart.Text,
    isMine: Boolean,
    theme: ChatTheme,
    textRenderConfig: ChatTextRenderConfig,
    slots: ChatSlots,
    callbacks: ChatCallbacks,
    modifier: Modifier = Modifier,
) {
    slots.textPartContent?.invoke(message, part, isMine) ?: run {
        DefaultTextPartContent(
            message = message,
            part = part,
            isMine = isMine,
            theme = theme,
            textRenderConfig = textRenderConfig,
            callbacks = callbacks,
            modifier = modifier,
        )
    }
}

@Composable
private fun DefaultTextPartContent(
    message: ChatMessage,
    part: MessagePart.Text,
    isMine: Boolean,
    theme: ChatTheme,
    textRenderConfig: ChatTextRenderConfig,
    callbacks: ChatCallbacks,
    modifier: Modifier = Modifier,
) {
    val textColor = if (isMine) theme.outgoingTextColor else theme.incomingTextColor
    val displayMode =
        resolveTextPartDisplayMode(
            format = part.format,
            status = message.status,
            config = textRenderConfig,
        )
    when (displayMode) {
        TextPartDisplayMode.PLAIN -> {
            Text(
                text = part.value,
                color = Color(textColor),
                fontSize = theme.fontSize.sp,
                modifier = modifier.fillMaxWidth(),
            )
        }
        TextPartDisplayMode.PLAIN_WITH_CURSOR -> {
            Text(
                text = part.value + STREAMING_CURSOR,
                color = Color(textColor),
                fontSize = theme.fontSize.sp,
                modifier = modifier.fillMaxWidth(),
            )
        }
        TextPartDisplayMode.MARKDOWN -> {
            val latestLinkHandler = rememberUpdatedState(callbacks.onLinkClick)
            val latestMessage = rememberUpdatedState(message)
            val markdownConfig =
                remember(theme, isMine) {
                    chatMarkdownConfig(theme, isMine) { url ->
                        latestLinkHandler.value?.invoke(latestMessage.value, url)
                    }
                }
            MarkdownView(
                modifier = modifier.fillMaxWidth(),
                content = part.value,
                config = markdownConfig,
            )
        }
    }
}
