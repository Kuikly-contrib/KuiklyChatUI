package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.MessageGroupStatus

/** Replaceable content slots for the Compose Chat UI. */
data class ChatSlots(
    val emptyContent: (@Composable () -> Unit)? = null,
    val messageContent: (@Composable (ChatMessage, MessageGroupStatus) -> Unit)? = null,
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
    val onCopyToClipboard: ((String) -> Unit)? = null
) {
    companion object {
        val Empty = ChatCallbacks()
    }
}

data class ChatScreenConfig(
    val showSenderName: Boolean = true,
    val showStatus: Boolean = true,
    val groupingWindowMillis: Long = 5 * 60 * 1000L,
    val maxLines: Int = 5,
    val loadEarlierThresholdPx: Int = 120,
    val estimatedMessageHeightPx: Int = 72
) {
    companion object {
        val Default = ChatScreenConfig()
    }
}
