package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.wrapContentWidth
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatStrings
import com.tencent.kuiklybase.chat.compose.core.ChatTextRenderConfig
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.chat.compose.core.MessageGroupStatus
import com.tencent.kuiklybase.chat.compose.core.MessagePart

@Composable
internal fun TextBubbleContent(
    message: ChatMessage,
    groupStatus: MessageGroupStatus,
    isMine: Boolean,
    theme: ChatTheme,
    strings: ChatStrings,
    config: ChatScreenConfig,
    slots: ChatSlots,
    callbacks: ChatCallbacks,
    textRenderConfig: ChatTextRenderConfig = ChatTextRenderConfig.Default,
    mediaRuntime: ChatMediaRuntime,
    modifier: Modifier = Modifier
) {
    val bubbleColor = if (isMine) theme.outgoingBubbleColor else theme.incomingBubbleColor
    val textColor = if (isMine) theme.outgoingTextColor else theme.incomingTextColor
    val shape = groupedBubbleShape(
        groupStatus = groupStatus,
        isMine = isMine,
        largeRadius = theme.bubbleRadius.dp,
        smallRadius = theme.bubbleRadiusSmall.dp
    )
    val showInsideTime = config.timeAndStatusPosition == TimeAndStatusPosition.INSIDE_BUBBLE &&
        (config.showTime || config.showStatus)
    val hasAttachments = message.parts.any { it is MessagePart.Attachment }
    val bubbleModifier = modifier
        .clip(shape)
        .background(Color(bubbleColor))
        .then(
            if (!hasAttachments && callbacks.onMessageClick != null) {
                Modifier.clickable { callbacks.onMessageClick.invoke(message) }
            } else {
                Modifier
            }
        )
        .padding(horizontal = 14.dp, vertical = 10.dp)

    Column(modifier = bubbleModifier.wrapContentWidth()) {
        message.parts.forEachIndexed { index, part ->
            when (part) {
                is MessagePart.Text -> TextPartContent(
                    message = message,
                    part = part,
                    isMine = isMine,
                    theme = theme,
                    textRenderConfig = textRenderConfig,
                    slots = slots,
                    callbacks = callbacks,
                )
                is MessagePart.Attachment -> {
                    val onClick = callbacks.onAttachmentClick?.let { handler ->
                        { handler(message, part) }
                    }
                    AttachmentContent(
                        message = message,
                        attachment = part,
                        partIndex = index,
                        isMine = isMine,
                        theme = theme,
                        strings = strings,
                        slots = slots,
                        mediaRuntime = mediaRuntime,
                        onClick = onClick,
                    )
                }
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
                is MessagePart.SystemNotice -> Text(
                    text = part.text,
                    color = Color(textColor),
                    fontSize = theme.fontSize.sp
                )
            }
        }
        if (showInsideTime) {
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MessageTimeAndStatus(
                    message = message,
                    isMine = isMine,
                    theme = theme,
                    strings = strings,
                    config = config,
                    modifier = Modifier
                )
            }
        }
    }
}
