package com.tencent.kuiklybase.chat.compose.markdown

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuiklybase.chat.compose.core.ChatTheme

@Composable
internal expect fun ChatMarkdownView(
    content: String,
    isMine: Boolean,
    theme: ChatTheme,
    onLinkClick: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
)
