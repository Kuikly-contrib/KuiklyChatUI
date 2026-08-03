package com.tencent.kuiklybase.chat.kuiklyai.runtime

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmClipboard
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmChatMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

internal class LlmCopyFeedbackController(
    private val scope: CoroutineScope,
    private val clipboard: LlmClipboard?,
    private val onEvent: (LlmChatUiEvent) -> Unit
) {
    var copiedMessageId by mutableStateOf<String?>(null)
        private set

    private var job: Job? = null

    fun copy(message: LlmChatMessage) {
        val target = clipboard ?: return
        if (message.text.isEmpty()) return
        job?.cancel()
        copiedMessageId = null
        job = scope.launch {
            try {
                target.copyText(
                    label = if (message.isUser) "User message" else "AI response",
                    text = message.text
                )
                copiedMessageId = message.id
                delay(COPY_FEEDBACK_DURATION_MS)
                if (copiedMessageId == message.id) copiedMessageId = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                onEvent(LlmChatUiEvent.PlatformOperationFailed(e.message ?: "Unable to copy text"))
            } finally {
                if (job === coroutineContext[Job]) job = null
            }
        }
    }

    fun dispose() {
        job?.cancel()
        job = null
        copiedMessageId = null
    }
}

private const val COPY_FEEDBACK_DURATION_MS = 3_000L
