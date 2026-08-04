package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.core.MessageGroupStatus

/** Replaceable content slots for the Compose Chat UI. */
data class ChatSlots(
    val emptyContent: (@Composable () -> Unit)? = null,
    val messageContent: (@Composable (ChatMessage, MessageGroupStatus) -> Unit)? = null,
    val messageRowContent: (
        @Composable (
            ChatMessage,
            MessageGroupStatus,
            @Composable () -> Unit
        ) -> Unit
    )? = null,
    val avatarContent: (@Composable (ChatUser, Dp) -> Unit)? = null,
    val timeAndStatusContent: (@Composable (ChatMessage, Boolean) -> Unit)? = null,
    val loadEarlierContent: (@Composable (Boolean, () -> Unit) -> Unit)? = null,
    val scrollToBottomContent: (@Composable (Int, () -> Unit) -> Unit)? = null,
    val errorBannerContent: (@Composable (String) -> Unit)? = null,
    val composerLeadingContent: (@Composable () -> Unit)? = null,
    val composerTrailingContent: (@Composable (Boolean, () -> Unit) -> Unit)? = null,
    val modifier: Modifier = Modifier
) {
    companion object {
        val Default = ChatSlots()
    }
}

/** Host callbacks that are intentionally separate from state mutation. */
data class ChatCallbacks(
    val onMessageClick: ((ChatMessage) -> Unit)? = null,
    val onRetryClick: ((ChatMessage) -> Unit)? = null,
    val onTextChanged: ((String) -> Unit)? = null,
    val onCopyToClipboard: ((String) -> Unit)? = null,
    val onAvatarClick: ((ChatUser) -> Unit)? = null,
    val onLoadAvatar: ((userId: String, avatarUri: String) -> Any?)? = null
) {
    companion object {
        val Empty = ChatCallbacks()
    }
}

data class ChatScreenConfig(
    val showSenderName: Boolean = true,
    val showAvatar: Boolean = true,
    val avatarSizeDp: Float = 32f,
    val hideAvatarWhenGrouped: Boolean = true,
    val showTime: Boolean = true,
    val showStatus: Boolean = true,
    val timeAndStatusPosition: TimeAndStatusPosition = TimeAndStatusPosition.BELOW_BUBBLE,
    val timeFormat: String = "HH:mm",
    val groupingWindowMillis: Long = 5 * 60 * 1000L,
    val maxLines: Int = 5,
    val loadEarlierThresholdPx: Int = 120,
    val estimatedMessageHeightPx: Int = 72
) {
    companion object {
        val Default = ChatScreenConfig()
    }
}
