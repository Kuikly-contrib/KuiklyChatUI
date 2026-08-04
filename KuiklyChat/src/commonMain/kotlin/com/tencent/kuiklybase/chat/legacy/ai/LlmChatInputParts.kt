package com.tencent.kuiklybase.chat.legacy.ai

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.extension.keyboardHeightChange
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.foundation.text.BasicTextField
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.SolidColor
import com.tencent.kuikly.compose.ui.text.TextStyle
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.chat.compose.agentcard.LucideSvgIcon
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmAttachmentSource
import com.tencent.kuiklybase.chat.kuiklyai.runtime.LlmChatController

@Composable
internal fun LlmAttachmentSourceMenu(
    sources: List<LlmAttachmentSource>,
    style: LlmChatViewStyle,
    strings: LlmChatViewStrings,
    modifier: Modifier = Modifier,
    onSelect: (LlmAttachmentSource) -> Unit
) {
    Row(modifier = modifier.fillMaxWidth()) {
        sources.forEachIndexed { index, source ->
            if (index > 0) Spacer(Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(style.suggestionBackgroundColor))
                    .border(1.dp, Color(style.suggestionBorderColor), RoundedCornerShape(8.dp))
                    .clickable { onSelect(source) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LucideSvgIcon(
                    name = when (source) {
                        LlmAttachmentSource.PHOTO_LIBRARY -> "kuikly_chat_ic_image"
                        LlmAttachmentSource.CAMERA -> "kuikly_chat_ic_camera"
                        LlmAttachmentSource.FILE -> "kuikly_chat_ic_file"
                    },
                    size = 16.dp,
                    tint = Color(style.suggestionTextColor)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = when (source) {
                        LlmAttachmentSource.PHOTO_LIBRARY -> strings.photoLibraryLabel
                        LlmAttachmentSource.CAMERA -> strings.cameraLabel
                        LlmAttachmentSource.FILE -> strings.fileLabel
                    },
                    fontSize = 12.sp,
                    color = Color(style.suggestionTextColor)
                )
            }
        }
    }
}

@Composable
internal fun LlmSendButton(
    controller: LlmChatController,
    style: LlmChatViewStyle,
    strings: LlmChatViewStrings
) {
    val generating = controller.isGenerating
    val enabled = generating || controller.canSend
    val background = when {
        generating -> Color(style.stopButtonColor)
        controller.canSend -> Color(style.sendButtonColor)
        else -> Color(style.sendButtonDisabledColor)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(style.composerRadius.dp))
            .background(background)
            .then(
                if (enabled) {
                    Modifier.clickable {
                        if (generating) controller.stop() else controller.send()
                    }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when {
                generating -> strings.stopLabel
                controller.editState is com.tencent.kuiklybase.chat.kuiklyai.runtime.LlmChatEditState.Editing -> strings.editSubmitLabel
                else -> strings.sendLabel
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

@Composable
internal fun CupertinoInputField(
    controller: LlmChatController,
    style: LlmChatViewStyle,
    strings: LlmChatViewStrings,
    onKeyboardHeightChange: ((Float) -> Unit)? = null,
    modifier: Modifier
) {
    BasicTextField(
        value = controller.inputText,
        onValueChange = controller::onInputChange,
        textStyle = TextStyle(
            fontSize = style.fontSize.sp,
            color = Color(style.composerTextColor)
        ),
        cursorBrush = SolidColor(Color(style.sendButtonColor)),
        maxLines = 5,
        modifier = modifier
            .then(
                if (onKeyboardHeightChange != null) {
                    Modifier.keyboardHeightChange { onKeyboardHeightChange(it.height) }
                } else {
                    Modifier
                }
            )
            .background(Color(style.composerBackgroundColor), RoundedCornerShape(style.composerRadius.dp))
            .border(1.dp, Color(style.composerBorderColor), RoundedCornerShape(style.composerRadius.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        decorationBox = { innerTextField ->
            Box {
                if (controller.inputText.isEmpty()) {
                    Text(
                        text = strings.inputPlaceholder,
                        fontSize = style.fontSize.sp,
                        color = Color(style.composerPlaceholderColor)
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
internal fun LlmListeningHint(
    style: LlmChatViewStyle,
    strings: LlmChatViewStrings,
    controller: LlmChatController
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(style.suggestionBackgroundColor))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LucideSvgIcon(
            name = "kuikly_chat_ic_mic",
            size = 14.dp,
            tint = Color(style.suggestionTextColor)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = strings.listeningLabel,
            fontSize = 12.sp,
            color = Color(style.suggestionTextColor),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "\u00D7",
            fontSize = 15.sp,
            color = Color(style.mutedColor),
            modifier = Modifier.clickable { controller.cancelListening() }
        )
    }
}

@Composable
internal fun LlmIconButton(
    iconName: String,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        LucideSvgIcon(name = iconName, size = 18.dp, tint = tint)
    }
}
