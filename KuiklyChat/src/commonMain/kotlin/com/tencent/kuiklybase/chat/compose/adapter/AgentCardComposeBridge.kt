package com.tencent.kuiklybase.chat.compose.adapter

import com.tencent.kuiklybase.chat.agui.AgUiEvent
import com.tencent.kuiklybase.chat.agentcard.AgentCard
import com.tencent.kuiklybase.chat.agentcard.AgUiCardSession
import com.tencent.kuiklybase.chat.compose.core.ChatController
import com.tencent.kuiklybase.chat.compose.core.ChatIntent
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.compose.core.MessageStatus

/**
 * Bridges the existing complete AG-UI card state machine to the new Chat UI core.
 * TextMessage/UserMessage remain owned by the LLM adapter to avoid duplicate text.
 */
class AgentCardComposeBridge(
    private val session: AgUiCardSession,
    private val assistant: ChatUser = ChatUser("assistant", "Assistant")
) {
    private var controller: ChatController? = null
    private val mirroredIds = mutableSetOf<String>()

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
        val cards = session.cards.filterNot { it is AgentCard.TextMessage || it is AgentCard.UserMessage }
        val currentIds = cards.mapTo(HashSet()) { it.id }
        cards.forEach { card ->
            chatController.dispatch(ChatIntent.UpsertMessage(card.toComposeMessage(assistant)))
        }
        mirroredIds.filterNot { it in currentIds }.forEach { id ->
            chatController.dispatch(ChatIntent.DeleteMessage(id))
        }
        mirroredIds.clear()
        mirroredIds.addAll(currentIds)
    }

    fun reset() {
        session.reset()
        mirroredIds.forEach { id ->
            controller?.dispatch(ChatIntent.DeleteMessage(id))
        }
        mirroredIds.clear()
    }

    fun detach() {
        controller = null
    }
}

private fun AgentCard.toComposeMessage(assistant: ChatUser): ChatMessage {
    return ChatMessage(
        id = id,
        author = assistant,
        parts = listOf(MessagePart.AgentCardContent(this)),
        status = when (this) {
            is AgentCard.Loading -> MessageStatus.STREAMING
            is AgentCard.Thinking -> if (streaming) MessageStatus.STREAMING else MessageStatus.SENT
            is AgentCard.ToolCall -> when (state) {
                com.tencent.kuiklybase.chat.agentcard.ToolState.PENDING,
                com.tencent.kuiklybase.chat.agentcard.ToolState.RUNNING -> MessageStatus.STREAMING
                com.tencent.kuiklybase.chat.agentcard.ToolState.SUCCESS -> MessageStatus.SENT
                com.tencent.kuiklybase.chat.agentcard.ToolState.FAILED -> MessageStatus.FAILED
            }
            is AgentCard.Error -> MessageStatus.FAILED
            else -> MessageStatus.SENT
        },
        createdAt = timestamp,
        metadata = mapOf("agentCardType" to cardType())
    )
}

private fun AgentCard.cardType(): String = when (this) {
    is AgentCard.CodeBlock -> "code_block"
    is AgentCard.CommandExec -> "command_exec"
    is AgentCard.FileEdit -> "file_edit"
    is AgentCard.ToolCall -> "tool_call"
    is AgentCard.Thinking -> "thinking"
    is AgentCard.SearchResult -> "search_result"
    is AgentCard.Loading -> "loading"
    is AgentCard.Error -> "error"
    is AgentCard.LinkPreview -> "link_preview"
    is AgentCard.Table -> "table"
    is AgentCard.Chart -> "chart"
    is AgentCard.Plan -> "plan"
    is AgentCard.Custom -> type
    is AgentCard.TextMessage -> "text"
    is AgentCard.UserMessage -> "user"
    is AgentCard.PhaseHeader -> "phase_header"
    is AgentCard.TodoProgress -> "todo_progress"
}
