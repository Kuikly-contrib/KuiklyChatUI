package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.chat.compose.core.MessagePart

public fun unknownCustomRenderer(): MessageRenderer = MessageRenderer { message, _, theme, modifier ->
    UnknownCustomMessage(message, theme, modifier)
}

@Composable
private fun UnknownCustomMessage(
    message: ChatMessage,
    theme: ChatTheme,
    modifier: Modifier
) {
    val custom = message.parts.filterIsInstance<MessagePart.Custom>().firstOrNull()
    val label = custom?.payload["text"].orEmpty().ifEmpty { custom?.type ?: "custom" }
    Text(
        text = label,
        color = Color(theme.incomingTextColor),
        fontSize = theme.fontSize.sp,
        modifier = modifier
            .clip(RoundedCornerShape(theme.bubbleRadius.dp))
            .background(Color(theme.incomingBubbleColor))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    )
}
