package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.dp
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
    ChatComposerAttachmentPreviewStrip(
        attachments = attachments,
        theme = theme,
        strings = strings,
        onRemove = onRemove,
    )
}
