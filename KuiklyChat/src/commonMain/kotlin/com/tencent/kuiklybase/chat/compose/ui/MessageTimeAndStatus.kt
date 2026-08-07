package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatStrings
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.chat.compose.core.MessageGroupStatus
import com.tencent.kuiklybase.chat.compose.core.MessageStatus
import com.tencent.kuiklybase.chat.compose.core.UtcChatDateTimeFormatter

enum class TimeAndStatusPosition {
    BELOW_BUBBLE,
    INSIDE_BUBBLE
}

/** Legacy UTC helper; prefer [ChatScreenConfig.dateTimeFormatter]. */
fun formatMessageTime(timestamp: Long, pattern: String = "HH:mm"): String =
    UtcChatDateTimeFormatter.formatMessageTime(timestamp, pattern)

fun statusLabel(status: MessageStatus, strings: ChatStrings): String? = when (status) {
    MessageStatus.SENDING -> strings.sending
    MessageStatus.STREAMING -> strings.streaming
    MessageStatus.CANCELLED -> strings.cancelled
    MessageStatus.SENT, MessageStatus.FAILED -> null
}

fun shouldShowTimeAndStatus(
    groupStatus: MessageGroupStatus,
    config: ChatScreenConfig,
): Boolean = shouldShowGroupedRowChrome(
    groupStatus = groupStatus,
    enabled = config.showTime || config.showStatus,
    hideWhenGrouped = config.hideTimeWhenGrouped,
)

@Composable
internal fun MessageTimeAndStatus(
    message: ChatMessage,
    isMine: Boolean,
    theme: ChatTheme,
    strings: ChatStrings,
    config: ChatScreenConfig,
    modifier: Modifier = Modifier
) {
    if (!config.showTime && !config.showStatus) return
    val timeText = if (config.showTime) {
        config.dateTimeFormatter.formatMessageTime(message.createdAt, config.timeFormat)
    } else {
        ""
    }
    val statusText = if (config.showStatus) statusLabel(message.status, strings) else null
    if (timeText.isEmpty() && statusText == null) return

    val label = when {
        timeText.isNotEmpty() && statusText != null -> "$timeText · $statusText"
        timeText.isNotEmpty() -> timeText
        else -> statusText.orEmpty()
    }

    Row(
        modifier = modifier.padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(theme.timeTextColor),
            fontSize = theme.smallFontSize.sp
        )
    }
}
