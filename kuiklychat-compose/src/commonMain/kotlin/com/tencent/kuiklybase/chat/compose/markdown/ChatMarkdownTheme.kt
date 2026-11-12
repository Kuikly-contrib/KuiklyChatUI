package com.tencent.kuiklybase.chat.compose.markdown

import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.SpanStyle
import com.tencent.kuikly.compose.ui.text.TextLinkStyles
import com.tencent.kuikly.compose.ui.text.TextStyle
import com.tencent.kuikly.compose.ui.text.font.FontFamily
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.markdown.model.DefaultMarkdownColors
import com.tencent.kuiklybase.markdown.model.DefaultMarkdownTypography
import com.tencent.kuiklybase.markdown.model.MarkdownColors
import com.tencent.kuiklybase.markdown.model.MarkdownTypography

internal data class ChatMarkdownStyle(
    val colors: MarkdownColors,
    val typography: MarkdownTypography,
)

internal fun chatMarkdownStyle(
    theme: ChatTheme,
    isMine: Boolean,
): ChatMarkdownStyle {
    val textColor = Color(if (isMine) theme.outgoingTextColor else theme.incomingTextColor)
    val linkColor = Color(if (isMine) 0xFFE0E7FF else theme.accentColor)
    val body = TextStyle(color = textColor, fontSize = theme.fontSize.sp)
    val code = body.copy(
        fontFamily = FontFamily.Monospace,
        fontSize = (theme.fontSize - 1f).sp,
    )
    fun heading(scale: Float, weight: FontWeight) = body.copy(
        fontSize = (theme.fontSize * scale).sp,
        fontWeight = weight,
    )
    return ChatMarkdownStyle(
        colors = DefaultMarkdownColors(
            text = textColor,
            codeText = textColor,
            inlineCodeText = textColor,
            linkText = linkColor,
            codeBackground = Color(if (isMine) 0x33FFFFFF else 0xFFF5F5F5),
            inlineCodeBackground = Color(if (isMine) 0x44FFFFFF else 0xFFE8E8E8),
            dividerColor = Color(if (isMine) 0x55FFFFFF else theme.borderColor),
            tableText = textColor,
            tableBackground = Color(if (isMine) 0x33FFFFFF else 0xFFFAFAFA),
            tableHeaderBackground = Color(if (isMine) 0x22FFFFFF else 0xFFF1F5F9),
            tableStroke = Color(if (isMine) 0x55FFFFFF else theme.borderColor),
        ),
        typography = DefaultMarkdownTypography(
            h1 = heading(1.5f, FontWeight.Bold),
            h2 = heading(1.35f, FontWeight.Bold),
            h3 = heading(1.2f, FontWeight.Bold),
            h4 = heading(1.1f, FontWeight.SemiBold),
            h5 = heading(1f, FontWeight.SemiBold),
            h6 = heading(1f, FontWeight.SemiBold),
            text = body,
            code = code,
            inlineCode = code,
            quote = body,
            paragraph = body,
            ordered = body,
            bullet = body,
            list = body,
            link = body.copy(color = linkColor),
            textLink = TextLinkStyles(style = SpanStyle(color = linkColor)),
            table = code,
        ),
    )
}
