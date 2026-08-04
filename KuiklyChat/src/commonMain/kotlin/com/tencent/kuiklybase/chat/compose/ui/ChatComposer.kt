package com.tencent.kuiklybase.chat.compose.ui

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
import com.tencent.kuiklybase.chat.compose.core.ChatController
import com.tencent.kuiklybase.chat.compose.core.ChatIntent
import com.tencent.kuiklybase.chat.compose.core.ChatStrings
import com.tencent.kuiklybase.chat.compose.core.ChatTheme

@Composable
internal fun ChatComposer(
    controller: ChatController,
    theme: ChatTheme,
    strings: ChatStrings,
    config: ChatScreenConfig,
    slots: ChatSlots,
    callbacks: ChatCallbacks,
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf("") }
    val isSending = controller.state.isSending
    val canSend = input.isNotBlank() && !isSending
    val sessionError = controller.state.error

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(theme.composerColor))
            .padding(theme.contentPadding.dp)
    ) {
        if (sessionError != null) {
            Text(
                text = "${strings.failed}: ${sessionError.message}",
                color = Color(theme.errorColor),
                fontSize = theme.smallFontSize.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { controller.dispatch(ChatIntent.ClearError) }
                    .padding(bottom = 8.dp)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
        slots.composerLeadingContent?.invoke()
        if (slots.composerLeadingContent != null) Spacer(Modifier.width(8.dp))

        TextField(
            value = input,
            onValueChange = {
                input = it
                callbacks.onTextChanged?.invoke(it)
            },
            placeholder = {
                Text(
                    text = strings.inputPlaceholder,
                    color = Color(theme.placeholderColor),
                    fontSize = theme.fontSize.sp
                )
            },
            textStyle = TextStyle(
                color = Color(theme.incomingTextColor),
                fontSize = theme.fontSize.sp
            ),
            maxLines = config.maxLines,
            shape = RoundedCornerShape(theme.bubbleRadius.dp),
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color(theme.incomingTextColor),
                unfocusedTextColor = Color(theme.incomingTextColor),
                focusedContainerColor = Color(theme.composerColor),
                unfocusedContainerColor = Color(theme.composerColor),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Color(theme.accentColor)
            ),
            modifier = Modifier
                .weight(1f)
                .border(1.dp, Color(theme.borderColor), RoundedCornerShape(theme.bubbleRadius.dp))
        )
        Spacer(Modifier.width(8.dp))

        if (slots.composerTrailingContent != null) {
            slots.composerTrailingContent.invoke(isSending) {
                submit(input, controller) { input = "" }
            }
        } else {
            Box(
                modifier = Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(theme.bubbleRadius.dp))
                    .background(Color(if (isSending) theme.errorColor else theme.accentColor))
                    .clickable {
                        if (isSending) {
                            controller.dispatch(ChatIntent.StopGeneration)
                        } else if (canSend) {
                            submit(input, controller) { input = "" }
                        }
                    }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isSending) strings.stop else strings.send,
                    color = Color(theme.outgoingTextColor),
                    fontSize = theme.smallFontSize.sp
                )
            }
        }
        }
    }
}

private fun submit(input: String, controller: ChatController, clear: () -> Unit) {
    if (controller.sendText(input)) clear()
}
