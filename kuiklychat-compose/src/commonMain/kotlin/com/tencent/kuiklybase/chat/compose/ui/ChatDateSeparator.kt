package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatStrings
import com.tencent.kuiklybase.chat.compose.core.ChatTheme

/** Flyer-style date header above a message when the calendar day changes. */
internal fun resolveDateSeparatorLabel(
    previous: ChatMessage?,
    current: ChatMessage,
    config: ChatScreenConfig,
    strings: ChatStrings,
    nowMillis: Long,
): String? {
    if (!config.showDateSeparators) return null
    return config.dateTimeFormatter.dateSeparatorLabel(
        previousCreatedAt = previous?.createdAt,
        currentCreatedAt = current.createdAt,
        nowMillis = nowMillis,
        strings = strings,
    )
}

internal fun breaksMessageGroupingAtDateBoundary(
    earlier: ChatMessage?,
    later: ChatMessage,
    config: ChatScreenConfig,
    strings: ChatStrings,
    nowMillis: Long,
): Boolean = resolveDateSeparatorLabel(
    previous = earlier,
    current = later,
    config = config,
    strings = strings,
    nowMillis = nowMillis,
) != null

@Composable
internal fun ChatMessageDateHeader(
    label: String,
    theme: ChatTheme,
    slots: ChatSlots,
    timestampMillis: Long,
) {
    if (slots.dateSeparatorContent != null) {
        slots.dateSeparatorContent.invoke(timestampMillis, label)
    } else {
        ChatDateSeparator(label = label, theme = theme)
    }
}

@Composable
internal fun ChatDateSeparator(
    label: String,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color(theme.dateSeparatorTextColor),
            fontSize = theme.smallFontSize.sp,
            modifier = Modifier
                .background(
                    color = Color(theme.dateSeparatorBgColor),
                    shape = RoundedCornerShape(10.dp),
                )
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
