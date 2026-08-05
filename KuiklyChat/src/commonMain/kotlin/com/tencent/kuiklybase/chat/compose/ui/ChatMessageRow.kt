package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.chat.compose.core.ChatController
import com.tencent.kuiklybase.chat.compose.core.ChatIntent
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatStrings
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.chat.compose.core.MessageGroupStatus
import com.tencent.kuiklybase.chat.compose.core.MessageStatus

@Composable
internal fun ChatMessageRow(
    message: ChatMessage,
    groupStatus: MessageGroupStatus,
    controller: ChatController,
    theme: ChatTheme,
    strings: ChatStrings,
    config: ChatScreenConfig,
    slots: ChatSlots,
    callbacks: ChatCallbacks,
    content: @Composable () -> Unit,
    reserveSenderNameSpace: Boolean = false,
) {
    val isMine = message.author.id == controller.currentUser.id
    val showAvatar = shouldShowAvatar(groupStatus, config.showAvatar, config.hideAvatarWhenGrouped)
    val showName = config.showSenderName && !isMine &&
        (groupStatus == MessageGroupStatus.SINGLE || groupStatus == MessageGroupStatus.FIRST)
    val avatarSize = config.avatarSizeDp.dp
    val avatarClick = callbacks.onAvatarClick?.let { handler -> { handler(message.author) } }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = messageRowTopPadding(groupStatus),
                start = theme.contentPadding.dp,
                end = theme.contentPadding.dp
            ),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMine && showAvatar) {
            ChatAvatar(
                user = message.author,
                theme = theme,
                size = avatarSize,
                onClick = avatarClick,
                modifier = Modifier.padding(end = 8.dp),
                avatarContent = slots.avatarContent
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(0.82f),
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
        ) {
            if (showName) {
                if (message.author.name.isNotEmpty()) {
                    Text(
                        text = message.author.name,
                        color = Color(theme.placeholderColor),
                        fontSize = theme.smallFontSize.sp,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                } else if (reserveSenderNameSpace) {
                    Text(
                        text = "",
                        color = Color(theme.placeholderColor),
                        fontSize = theme.smallFontSize.sp,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
            ) {
                content()
                if (config.showStatus && message.status == MessageStatus.FAILED) {
                    Text(
                        text = strings.retry,
                        color = Color(theme.errorColor),
                        fontSize = theme.smallFontSize.sp,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clickable {
                                callbacks.onRetryClick?.invoke(message)
                                    ?: controller.dispatch(ChatIntent.RetryMessage(message.id))
                            }
                    )
                }
            }

            if (config.timeAndStatusPosition == TimeAndStatusPosition.BELOW_BUBBLE) {
                if (slots.timeAndStatusContent != null) {
                    slots.timeAndStatusContent.invoke(message, isMine)
                } else {
                    MessageTimeAndStatus(
                        message = message,
                        isMine = isMine,
                        theme = theme,
                        strings = strings,
                        config = config,
                        modifier = Modifier.align(if (isMine) Alignment.End else Alignment.Start)
                    )
                }
            }
        }

        if (isMine && showAvatar) {
            ChatAvatar(
                user = message.author,
                theme = theme,
                size = avatarSize,
                onClick = avatarClick,
                modifier = Modifier.padding(start = 8.dp),
                avatarContent = slots.avatarContent
            )
        }
    }
}

@Composable
internal fun DefaultChatMessageRow(
    message: ChatMessage,
    groupStatus: MessageGroupStatus,
    controller: ChatController,
    theme: ChatTheme,
    strings: ChatStrings,
    config: ChatScreenConfig,
    slots: ChatSlots,
    callbacks: ChatCallbacks,
    reserveSenderNameSpace: Boolean = false,
) {
    val rowContent: @Composable () -> Unit = {
        TextBubbleContent(
            message = message,
            groupStatus = groupStatus,
            isMine = message.author.id == controller.currentUser.id,
            theme = theme,
            strings = strings,
            config = config,
            callbacks = callbacks
        )
    }

    if (slots.messageRowContent != null) {
        slots.messageRowContent.invoke(message, groupStatus, rowContent)
    } else {
        ChatMessageRow(
            message = message,
            groupStatus = groupStatus,
            controller = controller,
            theme = theme,
            strings = strings,
            config = config,
            slots = slots,
            callbacks = callbacks,
            content = rowContent,
            reserveSenderNameSpace = reserveSenderNameSpace,
        )
    }
}
