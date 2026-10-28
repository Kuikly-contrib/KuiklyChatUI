package com.kuikly.kuiklychat.dsl.chat

import com.kuikly.kuiklychat.base.BasePager
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.views.View
import com.tencent.kuiklybase.chat.compose.core.ChatController
import com.tencent.kuiklybase.chat.compose.core.ChatConversationDirectory
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatMessageSender
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.session.controller.ChatConversationHost
import com.tencent.kuiklybase.chat.session.controller.ChatControllerSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/** Host-side demo: protocol updates become AgentCard parts and render incrementally. */
@Page("agent_card_demo", supportInLocal = true)
internal class AgentCardDemoPage : BasePager() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var kimiApiKey: String = ""
    private var kimiModel: String = "kimi-k2.6"
    /** Debug: when set, every reply replays this text/JSONL instead of calling Kimi. */
    private var replayJsonl: String = ""
    /** Action JSON waiting to be sent as the next Kimi prompt (set by a card tap). */
    private var pendingActionPrompt: String? = null
    private val controller = ChatController(
        currentUser = ChatUser(id = "me", name = "我"),
        scope = scope,
        sender = ChatMessageSender { message ->
            val prompt = pendingActionPrompt ?: message.text
            pendingActionPrompt = null
            if (replayJsonl.isNotEmpty()) {
                replayA2uiEvents(message, replayJsonl)
            } else {
                agentCardDemoEvents(message, kimiApiKey, kimiModel, prompt, kimiHistory(currentMessages(), message.id))
            }
        },
    )
    private lateinit var conversationHost: ChatConversationHost
    private fun currentMessages(): List<ChatMessage> = controller.state.messages
    private val cardActions = AgentCardDemoActions(controller) { label, prompt ->
        pendingActionPrompt = prompt
        controller.sendText(label).also { sent -> if (!sent) pendingActionPrompt = null }
    }

    override fun created() {
        super.created()
        kimiApiKey = pageData.params.optString("kimiApiKey")
        kimiModel = pageData.params.optString("kimiModel").ifEmpty { "kimi-k2.6" }
        replayJsonl = pageData.params.optString("replayJsonl")
        conversationHost = ChatConversationHost(
            controller = controller,
            directory = ChatConversationDirectory(demoChatConversationStore(key = "a2ui_demo_conversations")),
        )
        conversationHost.restore()
        // Persist after each finished reply so a killed process keeps the conversation.
        // The store calls a Kuikly module, so hop back to the pager thread first.
        val pageId = pagerId
        controller.stateFlow
            .map { it.isSending }
            .distinctUntilChanged()
            .drop(1)
            .filter { sending -> !sending }
            .onEach {
                com.tencent.kuikly.core.timer.setTimeout(pageId, 0) { cardActions.flushStates(); conversationHost.rememberCurrent() }
            }
            .launchIn(scope)
    }

    override fun pageDidDisappear() {
        super.pageDidDisappear()
        cardActions.flushStates()
        conversationHost.rememberCurrent()
    }

    override fun pageWillDestroy() {
        cardActions.flushStates()
        conversationHost.rememberCurrent()
        controller.dispose()
        scope.cancel()
        super.pageWillDestroy()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    flex(1f)
                    flexDirection(FlexDirection.COLUMN)
                    backgroundColor(Color(AGENT_CARD_DEMO_THEME.backgroundColor))
                }
                ChatControllerSession(
                    controller = ctx.controller,
                    theme = AGENT_CARD_DEMO_THEME,
                    headerTitle = "A2UI Demo",
                    conversationHost = ctx.conversationHost,
                    renderMessage = { message ->
                        renderMixedReply(
                            message = message,
                            width = ctx.pagerData.pageViewWidth - 32f,
                            markdownConfig = AGENT_CARD_DEMO_MARKDOWN,
                            actions = ctx.cardActions,
                        )
                    },
                )
            }
        }
    }
}

private val AGENT_CARD_DEMO_THEME: ChatTheme = ChatTheme.Default.copy(
    backgroundColor = 0xFFF4F7F6,
    outgoingBubbleColor = 0xFF176B5B,
    incomingTextColor = 0xFF24313A,
    accentColor = 0xFF176B5B,
    systemBackgroundColor = 0xFFE8F1EE,
    fontSize = 16f,
    bubbleRadius = 12f,
)

private val AGENT_CARD_DEMO_MARKDOWN = mixedReplyMarkdownConfig(AGENT_CARD_DEMO_THEME)
