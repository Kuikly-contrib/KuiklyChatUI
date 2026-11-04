package com.tencent.kuiklybase.chat.kuiklyai.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmAttachmentPicker
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmClipboard
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmSpeechRecognizer
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmProvider

/**
 * 创建并记住一个 [LlmChatController]，随 Composable 一起销毁。
 *
 * [provider] 变化时会重建 controller（等价于切换会话）；
 * 想在同一个 provider 上保持会话，就把 provider 自己 `remember` 住。
 */
@Composable
public fun rememberLlmChatController(
    provider: LlmProvider,
    attachmentPicker: LlmAttachmentPicker? = null,
    speechRecognizer: LlmSpeechRecognizer? = null,
    clipboard: LlmClipboard? = null
): LlmChatController {
    val scope = rememberCoroutineScope()
    val controller = remember(provider, attachmentPicker, speechRecognizer, clipboard) {
        LlmChatController(
            provider = provider,
            scope = scope,
            attachmentPicker = attachmentPicker,
            speechRecognizer = speechRecognizer,
            clipboard = clipboard
        )
    }
    DisposableEffect(controller) {
        onDispose { controller.dispose() }
    }
    return controller
}
