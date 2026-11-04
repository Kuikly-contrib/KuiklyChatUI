package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.shape.CircleShape
import com.tencent.kuikly.compose.extension.keyboardHeightChange
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.material3.TextField
import com.tencent.kuikly.compose.material3.TextFieldDefaults
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.TextStyle
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuiklybase.chat.compose.agentcard.LucideSvgIcon
import com.tencent.kuiklybase.chat.compose.core.ChatStrings
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.chat.compose.core.MessagePart

/**
 * DSH Web-style input card: pending attachment strip, text field, bottom toolbar (+ / send).
 */
@Composable
internal fun ChatComposerDshInputCard(
    input: String,
    onInputChange: (String) -> Unit,
    pendingAttachments: List<MessagePart.Attachment>,
    onRemoveAttachment: (String) -> Unit,
    theme: ChatTheme,
    strings: ChatStrings,
    config: ChatScreenConfig,
    isSending: Boolean,
    canSend: Boolean,
    isPickingAttachment: Boolean,
    onOpenAttachmentSheet: () -> Unit,
    onSendOrStop: () -> Unit,
    onKeyboardHeightChange: ((Float) -> Unit)?,
    pendingContent: (
        @Composable (
            List<MessagePart.Attachment>,
            (String) -> Unit,
        ) -> Unit
    )? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(theme.composerColor))
            .border(1.dp, Color(theme.borderColor), RoundedCornerShape(22.dp))
            .padding(top = 10.dp),
    ) {
        if (pendingContent != null) {
            pendingContent(pendingAttachments, onRemoveAttachment)
        } else {
            ChatComposerAttachmentPreviewStrip(
                attachments = pendingAttachments,
                theme = theme,
                strings = strings,
                onRemove = onRemoveAttachment,
            )
        }
        TextField(
            value = input,
            onValueChange = onInputChange,
            placeholder = {
                Text(
                    text = strings.inputPlaceholder,
                    color = Color(theme.placeholderColor),
                    fontSize = theme.fontSize.sp,
                )
            },
            textStyle = TextStyle(
                color = Color(theme.incomingTextColor),
                fontSize = theme.fontSize.sp,
            ),
            maxLines = config.maxLines,
            shape = RoundedCornerShape(0.dp),
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color(theme.incomingTextColor),
                unfocusedTextColor = Color(theme.incomingTextColor),
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Color(theme.accentColor),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .then(
                    if (onKeyboardHeightChange != null) {
                        Modifier.keyboardHeightChange { onKeyboardHeightChange(it.height) }
                    } else {
                        Modifier
                    },
                ),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, bottom = 6.dp, top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(theme.incomingBubbleColor)),
                contentAlignment = Alignment.Center,
            ) {
                ChatPressableIconBox(
                    enabled = !isSending && !isPickingAttachment,
                    onClick = onOpenAttachmentSheet,
                    modifier = Modifier.size(32.dp),
                    shapeRadius = 16.dp,
                ) { pressed ->
                    LucideSvgIcon(
                        name = CHAT_COMPOSER_PLUS_ICON,
                        size = 16.dp,
                        tint = composerIconColor(
                            theme = theme,
                            enabled = !isSending && !isPickingAttachment,
                            pressed = pressed,
                        ),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            val sendEnabled = isSending || canSend
            val sendBaseColor = when {
                isSending -> theme.errorColor
                canSend -> theme.accentColor
                else -> theme.borderColor
            }
            ChatPressableFilledButton(
                enabled = sendEnabled,
                onClick = onSendOrStop,
                theme = theme,
                baseColor = sendBaseColor,
                modifier = Modifier.height(36.dp),
                shapeRadius = 18.dp,
            ) {
                Text(
                    text = if (isSending) strings.stop else strings.send,
                    color = Color(
                        if (canSend || isSending) theme.outgoingTextColor else theme.placeholderColor,
                    ),
                    fontSize = theme.smallFontSize.sp,
                    modifier = Modifier.padding(horizontal = 14.dp),
                )
            }
        }
    }
}
