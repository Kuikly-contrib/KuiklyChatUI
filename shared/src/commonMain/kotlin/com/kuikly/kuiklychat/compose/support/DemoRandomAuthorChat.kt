package com.kuikly.kuiklychat.compose.support

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.tencent.kuiklybase.chat.compose.core.ChatController
import com.tencent.kuiklybase.chat.compose.core.ChatMessageSender
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatStreamEvent
import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.core.MessageStatus
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

/**
 * Demo-only sender: each outgoing composer message is randomly attributed to
 * [currentUser] or [assistant], without calling an LLM backend.
 */
internal fun randomAuthorDemoSender(
    currentUser: ChatUser,
    assistant: ChatUser,
): ChatMessageSender =
    ChatMessageSender { userMessage ->
        flow {
            val author = if (Random.nextBoolean()) currentUser else assistant
            emit(
                ChatStreamEvent.ReplaceMessage(
                    userMessage.copy(
                        author = author,
                        status = MessageStatus.SENT,
                    ),
                ),
            )
            emit(ChatStreamEvent.Completed)
        }
    }

@Composable
internal fun rememberRandomAuthorChatController(
    currentUser: ChatUser,
    assistant: ChatUser,
    initialMessages: List<ChatMessage> = emptyList(),
): ChatController {
    val scope = rememberCoroutineScope()
    val controller =
        remember(scope, currentUser, assistant, initialMessages) {
            ChatController(
                currentUser = currentUser,
                scope = scope,
                sender = randomAuthorDemoSender(currentUser, assistant),
                assistantUser = assistant,
                initialMessages = initialMessages,
            )
        }
    DisposableEffect(controller) {
        onDispose { controller.dispose() }
    }
    return controller
}
