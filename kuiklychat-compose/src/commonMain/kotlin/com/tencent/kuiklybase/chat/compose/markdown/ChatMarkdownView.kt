package com.tencent.kuiklybase.chat.compose.markdown

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.markdown.annotator.LocalUriHandler
import com.tencent.kuiklybase.markdown.annotator.UriHandler
import com.tencent.kuiklybase.markdown.compose.Markdown

@Composable
internal fun ChatMarkdownView(
    content: String,
    isMine: Boolean,
    theme: ChatTheme,
    onLinkClick: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val style = remember(theme, isMine) { chatMarkdownStyle(theme, isMine) }
    val uriHandler = remember(onLinkClick) {
        object : UriHandler {
            override suspend fun openUri(uri: String) {
                onLinkClick?.invoke(uri)
            }

            override suspend fun openRef(ref: List<String>, x: Float, y: Float) {
                onLinkClick?.invoke(ref.lastOrNull().orEmpty())
            }
        }
    }
    CompositionLocalProvider(LocalUriHandler provides uriHandler) {
        Markdown(
            content = content,
            colors = style.colors,
            typography = style.typography,
            modifier = modifier,
        )
    }
}
