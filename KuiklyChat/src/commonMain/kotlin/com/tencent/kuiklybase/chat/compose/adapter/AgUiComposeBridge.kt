package com.tencent.kuiklybase.chat.compose.adapter

import com.tencent.kuiklybase.chat.agui.AgUiEvent
import com.tencent.kuiklybase.chat.agui.AgUiSession
import com.tencent.kuiklybase.chat.compose.core.ChatController
import com.tencent.kuiklybase.chat.compose.core.ChatIntent
import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.model.MessageType

/**
 * Bridges AG-UI structured messages into the Compose Chat UI without exposing
 * protocol events to Compose components.
 *
 * Plain text remains owned by the LLM provider adapter. This bridge only
 * mirrors structured/custom messages produced by [AgUiSession], preventing
 * duplicate assistant text while allowing AgentCard renderers to consume
 * tool, activity, reasoning and custom messages.
 */
class AgUiComposeBridge(
    private val session: AgUiSession,
    private val currentUserId: String,
    private val assistant: ChatUser = ChatUser("assistant", "Assistant")
) {
    private var controller: ChatController? = null
    private val mirroredMessageIds = mutableSetOf<String>()

    /** Pass this callback to [com.tencent.kuiklybase.chat.kuiklyai.provider.AgUiLlmProvider]. */
    val onEvent: (AgUiEvent) -> Unit = ::dispatch

    fun attach(chatController: ChatController) {
        controller = chatController
        sync()
    }

    fun dispatch(event: AgUiEvent) {
        session.dispatch(event)
        sync()
    }

    fun sync() {
        val chatController = controller ?: return
        val structuredMessages = session.messages.filter(::isStructuredMessage)
        val currentIds = structuredMessages.mapTo(HashSet()) { it.id }

        structuredMessages.forEach { message ->
            chatController.dispatch(
                ChatIntent.UpsertMessage(
                    message.toComposeChatMessage(currentUserId, assistant)
                )
            )
        }
        mirroredMessageIds
            .filterNot { it in currentIds }
            .forEach { chatController.dispatch(ChatIntent.DeleteMessage(it)) }

        mirroredMessageIds.clear()
        mirroredMessageIds.addAll(currentIds)
    }

    fun reset() {
        session.reset()
        controller?.let { chatController ->
            mirroredMessageIds.forEach { id ->
                chatController.dispatch(ChatIntent.DeleteMessage(id))
            }
        }
        mirroredMessageIds.clear()
    }

    fun detach() {
        controller = null
    }

    private fun isStructuredMessage(message: com.tencent.kuiklybase.chat.model.ChatMessage): Boolean {
        val kind = message.extra["agUiKind"]
        return message.type == MessageType.CUSTOM || (kind != null && kind != "streaming")
    }
}
