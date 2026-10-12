package com.kuikly.kuiklychat.dsl.chat

import com.kuikly.kuiklychat.base.BasePager
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.views.View
import com.tencent.kuiklybase.chat.compose.core.ChatController
import com.tencent.kuiklybase.chat.compose.core.ChatConversationDirectory
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatMessageSender
import com.tencent.kuiklybase.chat.compose.core.ChatStreamEvent
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.core.TextFormat
import com.tencent.kuiklybase.chat.session.controller.ChatConversationHost
import com.tencent.kuiklybase.chat.session.controller.ChatControllerSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Usage sample for the chat session: send a message and stream a Markdown reply.
 */
@Page("chat_component_demo", supportInLocal = true)
internal class ChatComponentDemoPage : BasePager() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val controller = ChatController(
        currentUser = ChatUser(id = "me", name = "我"),
        scope = scope,
        sender = ChatMessageSender { message -> scriptedReply(message) },
    )
    private lateinit var conversationHost: ChatConversationHost

    override fun created() {
        super.created()
        conversationHost = ChatConversationHost(
            controller = controller,
            directory = ChatConversationDirectory(demoChatConversationStore()),
        )
        conversationHost.restore()
    }

    override fun pageWillDestroy() {
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
                    backgroundColor(Color(CHAT_COMPONENT_DEMO_THEME.backgroundColor))
                }
                ChatControllerSession(
                    controller = ctx.controller,
                    theme = CHAT_COMPONENT_DEMO_THEME,
                    composerBottomInset = ctx.pagerData.safeAreaInsets.bottom,
                    headerTitle = "Chat",
                    conversationHost = ctx.conversationHost,
                )
            }
        }
    }
}

private val CHAT_COMPONENT_DEMO_THEME = ChatTheme.Default.copy(
    backgroundColor = 0xFFF6F8F7,
    outgoingBubbleColor = 0xFF176B5B,
    incomingTextColor = 0xFF334155,
    accentColor = 0xFF176B5B,
    systemBackgroundColor = 0xFFF8FAFC,
    fontSize = 14f,
    bubbleRadius = 12f,
)

private fun scriptedReply(message: ChatMessage): Flow<ChatStreamEvent> = flow {
    val topic = message.text.lineSequence().firstOrNull()?.trim().orEmpty().ifBlank { "这条消息" }
    val reply = """
        # $topic

        这是组件自带的脚本回复，用来看 **流式 Markdown**。
    """.trimIndent()
    var index = 0
    while (index < reply.length) {
        val end = minOf(index + 8, reply.length)
        emit(ChatStreamEvent.TextDelta(text = reply.substring(index, end), format = TextFormat.MARKDOWN))
        index = end
        delay(28)
    }
    emit(ChatStreamEvent.Completed)
}
