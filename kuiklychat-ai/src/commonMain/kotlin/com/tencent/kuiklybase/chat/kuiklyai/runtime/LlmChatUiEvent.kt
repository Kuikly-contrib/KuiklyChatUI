package com.tencent.kuiklybase.chat.kuiklyai.runtime

/** One-shot events rendered as a snackbar, toast, or platform equivalent. */
public sealed interface LlmChatUiEvent {
    public data class TextCopied(val messageId: String) : LlmChatUiEvent
    public data class AttachmentRejected(val message: String) : LlmChatUiEvent
    public data class PlatformOperationFailed(val message: String) : LlmChatUiEvent
}
