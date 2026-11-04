package com.tencent.kuiklybase.chat.compose.markdown

import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.config.MarkdownColors
import com.tencent.kuiklybase.config.MarkdownConfig
import com.tencent.kuiklybase.config.MarkdownTypography
import com.tencent.kuiklybase.config.TextStyleConfig

internal fun chatMarkdownConfig(
    theme: ChatTheme,
    isMine: Boolean,
    onLinkClick: ((String) -> Unit)?,
): MarkdownConfig {
    val textColor = if (isMine) theme.outgoingTextColor else theme.incomingTextColor
    val colors =
        if (isMine) {
            MarkdownColors(
                text = textColor,
                codeBackground = 0x33FFFFFF,
                inlineCodeBackground = 0x44FFFFFF,
                dividerColor = 0x55FFFFFF,
                tableBackground = 0x33FFFFFF,
                blockQuoteBar = 0xCCFFFFFF,
                blockQuoteBackground = 0x22FFFFFF,
                linkColor = 0xFFE0E7FF,
                codeText = textColor,
            )
        } else {
            MarkdownColors(
                text = textColor,
                linkColor = theme.accentColor,
                codeText = textColor,
            )
        }
    val bodySize = theme.fontSize
    val typography =
        MarkdownTypography(
            text = TextStyleConfig(fontSize = bodySize),
            paragraph = TextStyleConfig(fontSize = bodySize),
            ordered = TextStyleConfig(fontSize = bodySize),
            bullet = TextStyleConfig(fontSize = bodySize),
            list = TextStyleConfig(fontSize = bodySize),
            quote = TextStyleConfig(fontSize = bodySize),
            textLink = TextStyleConfig(fontSize = bodySize),
            h1 = TextStyleConfig(fontSize = bodySize * 1.5f),
            h2 = TextStyleConfig(fontSize = bodySize * 1.35f),
            h3 = TextStyleConfig(fontSize = bodySize * 1.2f),
            h4 = TextStyleConfig(fontSize = bodySize * 1.1f),
            h5 = TextStyleConfig(fontSize = bodySize),
            h6 = TextStyleConfig(fontSize = bodySize),
        )
    return MarkdownConfig(
        colors = colors,
        typography = typography,
        onLinkClick = onLinkClick,
    )
}
