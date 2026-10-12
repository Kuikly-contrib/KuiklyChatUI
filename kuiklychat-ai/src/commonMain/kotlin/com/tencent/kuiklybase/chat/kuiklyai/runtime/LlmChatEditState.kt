package com.tencent.kuiklybase.chat.kuiklyai.runtime

import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmAttachment

/** Default composer edit state. */
public sealed interface LlmChatEditState {
    public data object Idle : LlmChatEditState

    public data class Editing(
        val messageId: String,
        val originalInputText: String,
        val originalPendingAttachments: List<LlmAttachment>
    ) : LlmChatEditState
}
