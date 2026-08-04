package com.tencent.kuiklybase.chat.compose.adapter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.tencent.kuiklybase.chat.compose.core.ChatController
import com.tencent.kuiklybase.chat.agentcard.AgUiCardSession
import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmProvider

/** Creates the new ChatController and releases its active request with the page. */
@Composable
fun rememberLlmChatUiController(
    provider: LlmProvider,
    currentUser: ChatUser = ChatUser("user", "You"),
    assistant: ChatUser = ChatUser("assistant", "Assistant"),
    historyLoader: com.tencent.kuiklybase.chat.compose.core.ChatHistoryLoader? = null
): ChatController {
    val scope = rememberCoroutineScope()
    val adapter = remember(provider, currentUser, assistant) {
        LlmChatAdapter(provider, currentUser, assistant)
    }
    val controller = remember(provider, currentUser, assistant, scope, historyLoader) {
        ChatController(
            currentUser = currentUser,
            scope = scope,
            sender = adapter.sender,
            historyLoader = historyLoader,
            initialMessages = if (historyLoader == null) adapter.initialMessages else emptyList(),
            assistantUser = assistant
        )
    }
    DisposableEffect(controller) {
        onDispose { controller.dispose() }
    }
    return controller
}

/** Creates a controller and binds the AG-UI structured-message bridge to it. */
@Composable
fun rememberAgUiChatUiController(
    provider: LlmProvider,
    bridge: AgUiComposeBridge,
    currentUser: ChatUser = ChatUser("user", "You"),
    assistant: ChatUser = ChatUser("assistant", "Assistant")
): ChatController {
    val controller = rememberLlmChatUiController(provider, currentUser, assistant)
    DisposableEffect(controller, bridge) {
        bridge.attach(controller)
        onDispose { bridge.detach() }
    }
    return controller
}

/** Creates a controller and binds the complete AgentCard session bridge. */
@Composable
fun rememberAgentCardChatUiController(
    provider: LlmProvider,
    cardSession: AgUiCardSession,
    currentUser: ChatUser = ChatUser("user", "You"),
    assistant: ChatUser = ChatUser("assistant", "Assistant")
): ChatController {
    val controller = rememberLlmChatUiController(provider, currentUser, assistant)
    val bridge = remember(cardSession, assistant) {
        AgentCardComposeBridge(cardSession, assistant)
    }
    DisposableEffect(controller, bridge) {
        bridge.attach(controller)
        onDispose { bridge.detach() }
    }
    return controller
}
