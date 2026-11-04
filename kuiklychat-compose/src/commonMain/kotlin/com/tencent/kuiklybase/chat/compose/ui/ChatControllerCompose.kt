package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.tencent.kuiklybase.chat.compose.core.ChatController
import com.tencent.kuiklybase.chat.compose.core.ChatState

@Composable
internal fun ChatController.collectChatState(): ChatState {
    val snapshot by stateFlow.collectAsState()
    return snapshot
}
