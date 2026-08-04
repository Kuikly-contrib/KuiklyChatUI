package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.chat.compose.core.ChatController
import com.tencent.kuiklybase.chat.compose.core.ChatIntent
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatStrings
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.chat.compose.core.MessageGroupStatus
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.compose.core.MessageStatus

@Composable
internal fun EmptyChatContent(strings: ChatStrings, theme: ChatTheme) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = strings.empty,
            color = Color(theme.placeholderColor),
            fontSize = theme.fontSize.sp
        )
    }
}

@Composable
internal fun DefaultChatMessageItem(
    message: ChatMessage,
    groupStatus: MessageGroupStatus,
    controller: ChatController,
    theme: ChatTheme,
    strings: ChatStrings,
    config: ChatScreenConfig,
    callbacks: ChatCallbacks
) {
    val isMine = message.author.id == controller.currentUser.id
    val showName = config.showSenderName && !isMine &&
        (groupStatus == MessageGroupStatus.SINGLE || groupStatus == MessageGroupStatus.FIRST)
    val bubbleColor = if (isMine) theme.outgoingBubbleColor else theme.incomingBubbleColor
    val textColor = if (isMine) theme.outgoingTextColor else theme.incomingTextColor
    val topPadding = if (groupStatus == MessageGroupStatus.MIDDLE || groupStatus == MessageGroupStatus.LAST) 2f else 8f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding.dp, start = theme.contentPadding.dp, end = theme.contentPadding.dp),
        contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column {
            if (showName && message.author.name.isNotEmpty()) {
                Text(
                    text = message.author.name,
                    color = Color(theme.placeholderColor),
                    fontSize = theme.smallFontSize.sp,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(theme.bubbleRadius.dp))
                        .background(Color(bubbleColor))
                        .clickable { callbacks.onMessageClick?.invoke(message) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    message.parts.forEach { part ->
                        when (part) {
                            is MessagePart.Text -> Text(
                                text = part.value,
                                color = Color(textColor),
                                fontSize = theme.fontSize.sp
                            )
                            is MessagePart.Attachment -> Text(
                                text = part.name.ifEmpty { part.uri },
                                color = Color(textColor),
                                fontSize = theme.fontSize.sp
                            )
                            is MessagePart.Custom -> Text(
                                text = part.payload["text"].orEmpty().ifEmpty { part.type },
                                color = Color(textColor),
                                fontSize = theme.fontSize.sp
                            )
                            is MessagePart.AgentCardContent -> Text(
                                text = "[${part.card::class.simpleName ?: "AgentCard"}]",
                                color = Color(textColor),
                                fontSize = theme.fontSize.sp
                            )
                        }
                    }
                }
                if (config.showStatus && message.status == MessageStatus.FAILED) {
                    Text(
                        text = strings.retry,
                        color = Color(theme.errorColor),
                        fontSize = theme.smallFontSize.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clickable {
                                callbacks.onRetryClick?.invoke(message)
                                    ?: controller.dispatch(ChatIntent.RetryMessage(message.id))
                            }
                    )
                }
            }
        }
    }
}
