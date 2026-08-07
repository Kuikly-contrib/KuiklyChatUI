package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.chat.compose.agentcard.LucideSvgIcon
import com.tencent.kuiklybase.chat.compose.core.ChatStrings
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.chat.compose.core.MessagePart

internal const val CHAT_COMPOSER_PAPERCLIP_ICON = "kuikly_chat_ic_paperclip"

internal fun validateComposerAttachments(attachments: List<MessagePart.Attachment>): Boolean {
    if (attachments.isEmpty()) return false
    val ids = mutableSetOf<String>()
    return attachments.all { attachment ->
        attachment.id.isNotBlank() &&
            attachment.uri.isNotBlank() &&
            ids.add(attachment.id)
    }
}

internal fun mergePickedAttachments(
    existing: List<MessagePart.Attachment>,
    picked: List<MessagePart.Attachment>,
): List<MessagePart.Attachment> {
    if (picked.isEmpty()) return existing
    require(validateComposerAttachments(picked)) {
        "Attachment picker returned invalid attachments"
    }
    val knownIds = existing.mapTo(mutableSetOf()) { it.id }
    val merged = existing.toMutableList()
    picked.forEach { attachment ->
        if (knownIds.add(attachment.id)) {
            merged += attachment
        }
    }
    return merged
}

@Composable
internal fun ChatComposerAttachmentButton(
    enabled: Boolean,
    isPicking: Boolean,
    theme: ChatTheme,
    onClick: () -> Unit,
    content: (@Composable (Boolean, () -> Unit) -> Unit)? = null,
) {
    val click = if (enabled && !isPicking) onClick else ({})
    if (content != null) {
        content(isPicking, click)
        return
    }
    ChatPressableIconBox(
        enabled = enabled && !isPicking,
        onClick = click,
        modifier = Modifier.size(48.dp),
    ) { pressed ->
        LucideSvgIcon(
            name = CHAT_COMPOSER_PAPERCLIP_ICON,
            size = 18.dp,
            tint = composerIconColor(
                theme = theme,
                enabled = enabled && !isPicking,
                pressed = pressed,
            ),
        )
    }
}

@Composable
internal fun ChatComposerPendingAttachments(
    attachments: List<MessagePart.Attachment>,
    theme: ChatTheme,
    strings: ChatStrings,
    onRemove: (String) -> Unit,
    content: (
        @Composable (
            List<MessagePart.Attachment>,
            (String) -> Unit,
        ) -> Unit
    )? = null,
) {
    if (attachments.isEmpty()) return
    if (content != null) {
        content(attachments, onRemove)
        return
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        attachments.forEachIndexed { index, attachment ->
            if (index > 0) Spacer(Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(theme.incomingBubbleColor))
                    .border(1.dp, Color(theme.borderColor), RoundedCornerShape(8.dp))
                    .padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = attachment.name.ifBlank { strings.attachmentFallbackName },
                    color = Color(theme.incomingTextColor),
                    fontSize = theme.smallFontSize.sp,
                    maxLines = 1,
                )
                Spacer(Modifier.width(6.dp))
                ChatPressableText(
                    enabled = true,
                    onClick = { onRemove(attachment.id) },
                ) { _ ->
                    Text(
                        text = "\u00D7",
                        color = Color(theme.placeholderColor),
                        fontSize = 15.sp,
                    )
                }
            }
        }
    }
}
