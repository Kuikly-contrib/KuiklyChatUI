package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.style.TextOverflow
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.chat.compose.agentcard.LucideSvgIcon
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.chat.compose.core.MessagePart

internal const val CHAT_FILE_ATTACHMENT_ICON = "kuikly_chat_ic_file"

@Composable
internal fun FileAttachmentContent(
    attachment: MessagePart.Attachment,
    isMine: Boolean,
    theme: ChatTheme,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val bubbleColor = if (isMine) theme.outgoingBubbleColor else theme.incomingBubbleColor
    val textColor = if (isMine) theme.outgoingTextColor else theme.incomingTextColor
    val fileName = attachment.name.ifEmpty { attachment.uri }
    val sizeLabel = formatFileSize(attachment.sizeBytes)
    val clickModifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .background(Color(bubbleColor), RoundedCornerShape(theme.bubbleRadiusSmall.dp))
            .then(clickModifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(theme.borderColor), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            LucideSvgIcon(
                name = CHAT_FILE_ATTACHMENT_ICON,
                size = 20.dp,
                tint = Color(theme.accentColor),
            )
        }
        Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
            Text(
                text = fileName,
                color = Color(textColor),
                fontSize = theme.fontSize.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (sizeLabel.isNotEmpty()) {
                Text(
                    text = sizeLabel,
                    color = Color(theme.placeholderColor),
                    fontSize = theme.smallFontSize.sp,
                )
            }
        }
    }
}
