package com.tencent.kuiklybase.chat.kuiklyai.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
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
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.TextStyle
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmAttachmentSource
import com.tencent.kuiklybase.chat.kuiklyai.runtime.LlmChatController

/**
 * 输入区：待发送附件条 + 输入框 + 附件/语音/发送按钮。
 *
 * 按钮的可见性由能力决定而不是配置项决定——宿主没提供
 * [com.tencent.kuiklybase.chat.kuiklyai.platform.LlmAttachmentPicker]
 * 就不显示附件按钮，而不是显示一个点了没反应的按钮。
 *
 * @param safeAreaBottom 底部安全区高度，由宿主传 `pagerData.safeAreaInsets.bottom`
 */
@Composable
fun LlmChatInput(
    controller: LlmChatController,
    style: LlmChatViewStyle = LlmChatViewStyle.Default,
    strings: LlmChatViewStrings = LlmChatViewStrings.Default,
    safeAreaBottom: Float = 0f,
    modifier: Modifier = Modifier
) {
    val attachmentSources = controller.attachmentPicker
        ?.supportedSources
        ?.sortedBy { it.ordinal }
        .orEmpty()
    val hasAttachmentPicker = attachmentSources.isNotEmpty()
    val hasVoice = controller.isSpeechAvailable
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
                onRemove = { controller.removePendingAttachment(it) }
            )
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

        if (controller.isListening) {
            LlmListeningHint(style = style, strings = strings, controller = controller)
            Spacer(Modifier.height(8.dp))
        }

        Row(verticalAlignment = Alignment.Bottom) {
            if (hasAttachmentPicker) {
                LlmIconButton(
                    glyph = "\uD83D\uDCC1",
                    tint = Color(style.mutedColor),
                    onClick = {
                        if (attachmentSources.size == 1) {
                            controller.pickAttachment(attachmentSources.single())
                        } else {
                            showAttachmentSources = !showAttachmentSources
                        }
                    }
                )
                Spacer(Modifier.width(4.dp))
            }

            if (hasVoice) {
                LlmIconButton(
                    glyph = "\uD83C\uDFA4",
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
                Spacer(Modifier.width(4.dp))
            }

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
                    .border(
                        1.dp,
                        Color(style.composerBorderColor),
                        RoundedCornerShape(style.composerRadius.dp)
                    )
            )

            Spacer(Modifier.width(8.dp))
            LlmSendButton(controller = controller, style = style, strings = strings)
        }
    }
}

@Composable
private fun LlmAttachmentSourceMenu(
    sources: List<LlmAttachmentSource>,
    style: LlmChatViewStyle,
    strings: LlmChatViewStrings,
    onSelect: (LlmAttachmentSource) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        sources.forEachIndexed { index, source ->
            if (index > 0) Spacer(Modifier.width(8.dp))
            Text(
                text = when (source) {
                    LlmAttachmentSource.PHOTO_LIBRARY -> strings.photoLibraryLabel
                    LlmAttachmentSource.CAMERA -> strings.cameraLabel
                    LlmAttachmentSource.FILE -> strings.fileLabel
                },
                fontSize = 12.sp,
                color = Color(style.suggestionTextColor),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(style.suggestionBackgroundColor))
                    .border(1.dp, Color(style.suggestionBorderColor), RoundedCornerShape(8.dp))
                    .clickable { onSelect(source) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

/**
 * 发送 / 停止按钮。
 *
 * 生成中变成"停止"而不是禁用发送——用户最需要的操作是打断一个跑偏的回答，
 * 把按钮变灰会让人只能等它说完。
 */
@Composable
private fun LlmSendButton(
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
            text = if (generating) strings.stopLabel else strings.sendLabel,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

/**
 * 录音中提示条，带取消入口——ASR 识别错了得能退出去，而不是只能等它结束。
 */
@Composable
private fun LlmListeningHint(
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
        Text(text = "\uD83C\uDFA4", fontSize = 13.sp)
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

/**
 * 输入区左侧的图标按钮。
 *
 * 用 emoji 字形而不是 SVG：`agentcard_compose/LucideSvgIcon` 依赖资源打包，
 * 而输入区需要在没有资源注册的宿主里也能直接用。
 */
@Composable
private fun LlmIconButton(
    glyph: String,
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
        Text(text = glyph, fontSize = 16.sp, color = tint)
    }
}
