package com.tencent.kuiklybase.chat.kuiklyai.runtime

import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmAttachment

/** Default composer edit state. */
sealed interface LlmChatEditState {
    data object Idle : LlmChatEditState

    data class Editing(
        val messageId: String,
        val originalInputText: String,
        val originalPendingAttachments: List<LlmAttachment>
    ) : LlmChatEditState
}
