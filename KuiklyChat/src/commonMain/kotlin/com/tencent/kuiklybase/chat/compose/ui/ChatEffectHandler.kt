package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.chat.compose.core.ChatController
import com.tencent.kuiklybase.chat.compose.core.ChatEffect
import com.tencent.kuiklybase.chat.compose.core.ChatIntent
import com.tencent.kuiklybase.chat.compose.core.ChatStrings
import com.tencent.kuiklybase.chat.compose.core.ChatTheme

@Composable
internal fun ChatEffectHandler(
    controller: ChatController,
    scrollState: ChatListScrollState,
    theme: ChatTheme,
    strings: ChatStrings,
    slots: ChatSlots,
    callbacks: ChatCallbacks
) {
    var transientError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(controller.effect) {
        when (val effect = controller.effect) {
            ChatEffect.ScrollToBottom -> scrollState.scrollToBottom(force = true)
            is ChatEffect.ShowError -> transientError = effect.message
            is ChatEffect.CopyToClipboard -> callbacks.onCopyToClipboard?.invoke(effect.text)
            null -> Unit
        }
        controller.consumeEffect()
    }

    val bannerMessage = transientError ?: controller.state.error?.message
    if (bannerMessage != null) {
        slots.errorBannerContent?.invoke(bannerMessage) ?: DefaultErrorBanner(
            message = bannerMessage,
            theme = theme,
            strings = strings,
            onDismiss = {
                transientError = null
                controller.dispatch(ChatIntent.ClearError)
            }
        )
    }
}

@Composable
private fun DefaultErrorBanner(
    message: String,
    theme: ChatTheme,
    strings: ChatStrings,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(theme.errorColor))
            .clickable { onDismiss() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "${strings.failed}: $message",
            color = Color(theme.outgoingTextColor),
            fontSize = theme.smallFontSize.sp
        )
    }
}
