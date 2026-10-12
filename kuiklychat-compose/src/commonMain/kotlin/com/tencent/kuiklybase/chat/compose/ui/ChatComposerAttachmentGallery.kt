package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter
import com.tencent.kuikly.compose.extension.bouncesEnable
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.lazy.LazyRow
import com.tencent.kuikly.compose.foundation.lazy.items
import com.tencent.kuikly.compose.foundation.shape.CircleShape
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.layout.ContentScale
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.chat.compose.core.ChatStrings
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.chat.compose.core.MessagePart

/**
 * Pending attachments inside the composer: a horizontal filmstrip.
 *
 * The composer owns the list and removal. This view only lays out thumbnails
 * so the host can swap it through [ChatSlots.pendingAttachmentsContent].
 */
@Composable
public fun ChatComposerAttachmentPreviewStrip(
    attachments: List<MessagePart.Attachment>,
    theme: ChatTheme,
    strings: ChatStrings,
    onRemove: (String) -> Unit,
) {
    if (attachments.isEmpty()) return
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .bouncesEnable(true),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(attachments, key = { it.id }) { attachment ->
            ComposerAttachmentThumb(
                attachment = attachment,
                theme = theme,
                strings = strings,
                onRemove = { onRemove(attachment.id) },
            )
        }
    }
}

@Composable
private fun ComposerAttachmentThumb(
    attachment: MessagePart.Attachment,
    theme: ChatTheme,
    strings: ChatStrings,
    onRemove: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(shape)
            .background(Color(theme.incomingBubbleColor))
            .border(1.dp, Color(theme.borderColor), shape),
    ) {
        if (isImageMimeType(attachment.mimeType)) {
            Image(
                painter = rememberAsyncImagePainter(attachmentDisplaySource(attachment)),
                contentDescription = attachment.name.ifBlank { strings.attachmentFallbackName },
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = attachment.name.ifBlank { strings.attachmentFallbackName },
                    fontSize = 10.sp,
                    color = Color(theme.incomingTextColor),
                    maxLines = 3,
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(32.dp)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xCC17201D)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "\u00D7", fontSize = 14.sp, color = Color.White)
            }
        }
    }
}
