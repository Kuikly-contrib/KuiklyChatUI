package com.kuikly.kuiklychat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.tencent.kuikly.compose.ui.text.style.TextOverflow
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.compose.ui.window.Dialog
import com.tencent.kuikly.compose.ui.window.DialogProperties
import com.tencent.kuiklybase.chat.compose.core.ChatController
import com.tencent.kuiklybase.chat.compose.core.ChatIntent
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.chat.compose.core.containsSystemNotice
import com.tencent.kuiklybase.chat.compose.ui.ChatCallbacks

/**
 * Demo-only long-press menu (copy / delete) wired through [ChatCallbacks.onMessageLongPressWithPosition].
 */
@Composable
fun rememberDemoChatMessageActions(
    controller: ChatController,
    baseCallbacks: ChatCallbacks,
    theme: ChatTheme = ChatTheme.Default,
): DemoChatMessageActions {
    var menuMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }

    val callbacks = remember(baseCallbacks) {
        baseCallbacks.copy(
            onMessageLongPress = { message ->
                if (!message.containsSystemNotice) {
                    menuMessage = message
                }
            },
            onCopyToClipboard = { text ->
                baseCallbacks.onCopyToClipboard?.invoke(text)
                toast = "已复制到剪贴板"
            },
        )
    }

    val overlay: @Composable () -> Unit = {
        DemoMessageActionSheet(
            message = menuMessage,
            theme = theme,
            onDismiss = { menuMessage = null },
            onCopy = { message ->
                val text = message.text
                if (text.isNotBlank()) {
                    callbacks.onCopyToClipboard?.invoke(text)
                } else {
                    toast = "该消息没有可复制文本"
                }
                menuMessage = null
            },
            onDelete = { message ->
                controller.dispatch(ChatIntent.DeleteMessage(message.id))
                toast = "已删除消息"
                menuMessage = null
            },
        )
        DemoActionToast(
            message = toast,
            theme = theme,
            onDismiss = { toast = null },
        )
    }

    return DemoChatMessageActions(callbacks = callbacks, overlay = overlay)
}

data class DemoChatMessageActions(
    val callbacks: ChatCallbacks,
    val overlay: @Composable () -> Unit,
)

@Composable
private fun DemoMessageActionSheet(
    message: ChatMessage?,
    theme: ChatTheme,
    onDismiss: () -> Unit,
    onCopy: (ChatMessage) -> Unit,
    onDelete: (ChatMessage) -> Unit,
) {
    if (message == null) return
    val preview = messagePreviewLabel(message)
    val canCopy = message.text.isNotBlank()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            scrimColor = Color.Black.copy(alpha = 0.35f),
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onDismiss),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White),
            ) {
                Text(
                    text = preview,
                    color = Color(theme.placeholderColor),
                    fontSize = theme.smallFontSize.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                )
                DemoActionRow(
                    label = "复制",
                    enabled = canCopy,
                    theme = theme,
                    destructive = false,
                    onClick = { onCopy(message) },
                )
                DemoActionRow(
                    label = "删除",
                    enabled = true,
                    theme = theme,
                    destructive = true,
                    onClick = { onDelete(message) },
                )
                DemoActionRow(
                    label = "取消",
                    enabled = true,
                    theme = theme,
                    destructive = false,
                    onClick = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun DemoActionRow(
    label: String,
    enabled: Boolean,
    theme: ChatTheme,
    destructive: Boolean,
    onClick: () -> Unit,
) {
    val textColor = when {
        !enabled -> Color(theme.placeholderColor)
        destructive -> Color(theme.errorColor)
        else -> Color(theme.incomingTextColor)
    }
    Text(
        text = label,
        color = textColor,
        fontSize = theme.fontSize.sp,
        fontWeight = if (destructive) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (enabled) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
    )
}

@Composable
private fun DemoActionToast(
    message: String?,
    theme: ChatTheme,
    onDismiss: () -> Unit,
) {
    if (message == null) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(theme.incomingTextColor).copy(alpha = 0.88f))
                .clickable(onClick = onDismiss)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = message,
                color = Color.White,
                fontSize = theme.smallFontSize.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun messagePreviewLabel(message: ChatMessage): String {
    val text = message.text.trim()
    if (text.isNotEmpty()) return text
    val attachmentCount = message.parts.count { it is com.tencent.kuiklybase.chat.compose.core.MessagePart.Attachment }
    return if (attachmentCount > 0) {
        "附件消息（$attachmentCount 项）"
    } else {
        "消息 ${message.id}"
    }
}
