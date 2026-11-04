package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatStrings
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.chat.compose.core.MessagePart

@Composable
internal fun AttachmentContent(
    message: ChatMessage,
    attachment: MessagePart.Attachment,
    partIndex: Int,
    isMine: Boolean,
    theme: ChatTheme,
    strings: ChatStrings,
    slots: ChatSlots,
    callbacks: ChatCallbacks,
    mediaRuntime: ChatMediaRuntime,
    onClick: (() -> Unit)?,
    imageOnly: Boolean = false,
    isFirstAttachment: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val gestureModifier = modifier.chatMessageGestures(
        message = message,
        callbacks = callbacks,
        onTap = onClick,
        enableLongPress = true,
    )
    if (slots.attachmentContent != null) {
        val clickHandler = onClick ?: {}
        slots.attachmentContent.invoke(message, attachment, isMine, gestureModifier, clickHandler)
        return
    }
    if (isImageMimeType(attachment.mimeType)) {
        ImageAttachmentContent(
            message = message,
            attachment = attachment,
            isMine = isMine,
            theme = theme,
            strings = strings,
            slots = slots,
            mediaRuntime = mediaRuntime,
            imageOnly = imageOnly,
            isFirstAttachment = isFirstAttachment,
            modifier = gestureModifier,
        )
    } else {
        FileAttachmentContent(
            attachment = attachment,
            isMine = isMine,
            theme = theme,
            modifier = gestureModifier,
        )
    }
}
