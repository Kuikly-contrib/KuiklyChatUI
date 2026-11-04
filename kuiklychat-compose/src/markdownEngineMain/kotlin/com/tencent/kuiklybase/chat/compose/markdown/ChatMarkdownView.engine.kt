package com.tencent.kuiklybase.chat.compose.markdown

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.markdown.compose.MarkdownView

@Composable
internal actual fun ChatMarkdownView(
    content: String,
    isMine: Boolean,
    theme: ChatTheme,
    onLinkClick: ((String) -> Unit)?,
    modifier: Modifier,
) {
    val markdownConfig = remember(theme, isMine, onLinkClick) {
        chatMarkdownConfig(theme, isMine, onLinkClick)
    }
    MarkdownView(
        modifier = modifier,
        content = content,
        config = markdownConfig,
    )
}
