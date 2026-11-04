package com.tencent.kuiklybase.chat.compose.markdown

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.chat.compose.agentcard.parseInlineMarkdown
import com.tencent.kuiklybase.chat.compose.core.ChatTheme

@Composable
internal actual fun ChatMarkdownView(
    content: String,
    isMine: Boolean,
    theme: ChatTheme,
    onLinkClick: ((String) -> Unit)?,
    modifier: Modifier,
) {
    val annotated = remember(content) { parseInlineMarkdown(content) }
    Text(
        text = annotated,
        color = Color(if (isMine) theme.outgoingTextColor else theme.incomingTextColor),
        fontSize = theme.fontSize.sp,
        modifier = modifier,
    )
}
