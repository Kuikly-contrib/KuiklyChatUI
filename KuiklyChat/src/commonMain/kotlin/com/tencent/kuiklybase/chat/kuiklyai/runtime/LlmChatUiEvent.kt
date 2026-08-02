package com.tencent.kuiklybase.chat.kuiklyai.runtime

/** One-shot events rendered as a snackbar, toast, or platform equivalent. */
sealed interface LlmChatUiEvent {
    data class TextCopied(val messageId: String) : LlmChatUiEvent
    data class AttachmentRejected(val message: String) : LlmChatUiEvent
    data class PlatformOperationFailed(val message: String) : LlmChatUiEvent
}
