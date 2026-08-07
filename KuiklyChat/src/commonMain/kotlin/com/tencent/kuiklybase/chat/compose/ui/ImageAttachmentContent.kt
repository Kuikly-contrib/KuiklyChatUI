package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.aspectRatio
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuiklybase.chat.compose.core.ChatMediaContext
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatStrings
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.chat.compose.core.MessagePart

@Composable
internal fun ImageAttachmentContent(
    message: ChatMessage,
    attachment: MessagePart.Attachment,
    isMine: Boolean,
    theme: ChatTheme,
    strings: ChatStrings,
    slots: ChatSlots,
    mediaRuntime: ChatMediaRuntime,
    modifier: Modifier = Modifier,
) {
    val displaySource = attachmentDisplaySource(attachment)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
    ) {
        ChatImage(
            mediaContext = ChatMediaContext.AttachmentPreview(
                message = message,
                attachment = attachment,
                source = displaySource,
            ),
            contentDescription = attachment.name.ifEmpty { null },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(theme.bubbleRadiusSmall.dp)),
            theme = theme,
            strings = strings,
            imageContent = slots.imageContent,
            mediaRuntime = mediaRuntime,
            errorContent = {
                ChatImageErrorPlaceholder(theme = theme, strings = strings)
            },
        )
    }
}
