package com.tencent.kuiklybase.chat.kuiklyai.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.extension.keyboardHeightChange
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
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
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
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmAttachmentSource
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmAttachment
import com.tencent.kuiklybase.chat.kuiklyai.runtime.LlmChatController

/**
 * 输入区：待发送附件条 + 输入框 + 附件/语音/发送按钮。
 *
 * 按钮的可见性由能力决定而不是配置项决定——宿主没提供
 * [com.tencent.kuiklybase.chat.kuiklyai.platform.LlmAttachmentPicker]
 * 就不显示附件按钮，而不是显示一个点了没反应的按钮。
 *
 * @param safeAreaBottom 底部安全区高度，由宿主传 `pagerData.safeAreaInsets.bottom`
 * @param onKeyboardHeightChange 键盘高度变化回调（像素），父层用此值做 bottom padding 避免遮挡
 */
@Composable
internal fun LlmChatInput(
    controller: LlmChatController,
    style: LlmChatViewStyle = LlmChatViewStyle.Default,
    strings: LlmChatViewStrings = LlmChatViewStrings.Default,
    safeAreaBottom: Float = 0f,
    modifier: Modifier = Modifier,
    onPreviewImage: ((LlmAttachment) -> Unit)? = null,
    onKeyboardHeightChange: ((Float) -> Unit)? = null
) {
    val attachmentSources = controller.availableAttachmentSources
    val hasAttachmentPicker = attachmentSources.isNotEmpty()
    val hasVoice = controller.speechRecognizer != null
    var showAttachmentSources by remember(controller.attachmentPicker) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(style.composerBackgroundColor))
            .padding(
                start = style.contentPaddingH.dp,
                end = style.contentPaddingH.dp,
                top = 8.dp,
                bottom = (8f + safeAreaBottom).dp
            )
    ) {
        if (controller.pendingAttachments.isNotEmpty()) {
            LlmAttachmentRow(
                attachments = controller.pendingAttachments,
                style = style,
                strings = strings,
                onRemove = { controller.removePendingAttachment(it) },
                onPreviewImage = onPreviewImage
            )
            Spacer(Modifier.height(8.dp))
        }

        if (controller.isListening) {
            LlmListeningHint(style = style, strings = strings, controller = controller)
            Spacer(Modifier.height(8.dp))
        }

        if (showAttachmentSources) {
            LlmAttachmentSourceMenu(
                sources = attachmentSources,
                style = style,
                strings = strings,
                onSelect = {
                    showAttachmentSources = false
                    controller.pickAttachment(it)
                }
            )
            Spacer(Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasAttachmentPicker) {
                val hasPhotoLibrary = LlmAttachmentSource.PHOTO_LIBRARY in attachmentSources
                LlmIconButton(
                    iconName = if (hasPhotoLibrary) {
                        "kuikly_chat_ic_image"
                    } else {
                        "kuikly_chat_ic_upload"
                    },
                    tint = Color(style.mutedColor),
                    onClick = {
                        when {
                            hasPhotoLibrary -> controller.pickAttachment(LlmAttachmentSource.PHOTO_LIBRARY)
                            attachmentSources.size == 1 -> controller.pickAttachment(attachmentSources.single())
                            else -> showAttachmentSources = !showAttachmentSources
                        }
                    }
                )
                Spacer(Modifier.width(8.dp))
            }

            if (hasVoice) {
                LlmIconButton(
                    iconName = "kuikly_chat_ic_mic",
                    tint = if (controller.isListening) {
                        Color(style.sendButtonColor)
                    } else {
                        Color(style.mutedColor)
                    },
                    onClick = {
                        if (controller.isListening) {
                            controller.stopListening()
                        } else {
                            controller.startListening()
                        }
                    }
                )
                Spacer(Modifier.width(8.dp))
            }

            if (style.designSystem == LlmChatDesignSystem.CUPERTINO) {
                CupertinoInputField(
                    controller = controller,
                    style = style,
                    strings = strings,
                    onKeyboardHeightChange = onKeyboardHeightChange,
                    modifier = Modifier.weight(1f)
                )
            } else {
                TextField(
                    value = controller.inputText,
                    onValueChange = { controller.onInputChange(it) },
                    textStyle = TextStyle(
                        fontSize = style.fontSize.sp,
                        color = Color(style.composerTextColor)
                    ),
                    placeholder = {
                        Text(
                            text = strings.inputPlaceholder,
                            fontSize = style.fontSize.sp,
                            color = Color(style.composerPlaceholderColor)
                        )
                    },
                    maxLines = 5,
                    shape = RoundedCornerShape(style.composerRadius.dp),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color(style.composerTextColor),
                        unfocusedTextColor = Color(style.composerTextColor),
                        focusedContainerColor = Color(style.composerBackgroundColor),
                        unfocusedContainerColor = Color(style.composerBackgroundColor),
                        cursorColor = Color(style.sendButtonColor),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (onKeyboardHeightChange != null) {
                                Modifier.keyboardHeightChange { onKeyboardHeightChange(it.height) }
                            } else {
                                Modifier
                            }
                        )
                        .border(
                            1.dp,
                            Color(style.composerBorderColor),
                            RoundedCornerShape(style.composerRadius.dp)
                        )
                )
            }

            Spacer(Modifier.width(8.dp))
            LlmSendButton(controller = controller, style = style, strings = strings)
        }
    }
}
