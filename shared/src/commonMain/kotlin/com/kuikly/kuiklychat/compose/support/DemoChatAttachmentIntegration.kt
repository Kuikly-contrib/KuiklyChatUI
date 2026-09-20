package com.kuikly.kuiklychat.compose.support

import androidx.compose.runtime.Composable
import com.tencent.kuiklybase.chat.compose.core.ChatStrings
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.chat.compose.ui.ChatAttachmentPickerIntegration
import com.tencent.kuiklybase.chat.compose.ui.rememberChatAttachmentPickerIntegration
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmAttachmentPicker

/** Demo wrapper around [rememberChatAttachmentPickerIntegration] (DSH-style sheet + previews). */
data class DemoChatAttachmentIntegration(
    val integration: ChatAttachmentPickerIntegration,
) {
    val callbacks get() = integration.callbacks
    val slots get() = integration.slots
}

@Composable
fun rememberDemoChatAttachmentIntegration(
    attachmentPicker: LlmAttachmentPicker? = null,
    theme: ChatTheme = ChatTheme.Default,
    strings: ChatStrings = ChatStrings.Default,
    onError: ((Throwable) -> Unit)? = null,
): DemoChatAttachmentIntegration {
    val integration = rememberChatAttachmentPickerIntegration(
        attachmentPicker = attachmentPicker,
        theme = theme,
        strings = strings,
        onError = onError,
    )
    return DemoChatAttachmentIntegration(integration)
}
