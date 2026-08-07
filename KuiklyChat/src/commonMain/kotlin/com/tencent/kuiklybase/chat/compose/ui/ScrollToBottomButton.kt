package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.chat.compose.core.ChatStrings
import com.tencent.kuiklybase.chat.compose.core.ChatTheme

@Composable
internal fun ScrollToBottomButton(
    unreadCount: Int,
    theme: ChatTheme,
    strings: ChatStrings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ChatPressableFilledButton(
        enabled = true,
        onClick = onClick,
        theme = theme,
        baseColor = theme.composerColor,
        modifier = modifier,
        shapeRadius = 24.dp,
    ) {
        val label = if (unreadCount > 0) {
            "${strings.scrollToBottom} ($unreadCount)"
        } else {
            strings.scrollToBottom
        }
        Text(
            text = label,
            color = Color(theme.accentColor),
            fontSize = theme.smallFontSize.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}
