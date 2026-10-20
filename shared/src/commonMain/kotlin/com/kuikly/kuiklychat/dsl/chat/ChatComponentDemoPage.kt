package com.kuikly.kuiklychat.dsl.chat

import com.kuikly.kuiklychat.base.BasePager
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.layout.FlexDirection
import com.kuikly.kuiklychat.module.KRSseModule
import com.tencent.kuikly.core.views.View
import com.tencent.kuiklybase.chat.compose.core.ChatController
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatMessageSender
import com.tencent.kuiklybase.chat.compose.core.ChatStreamEvent
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.core.TextFormat
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmProvider
import com.tencent.kuiklybase.chat.kuiklyai.provider.OpenAiCompatibleProvider
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
    private var liveProvider: LlmProvider? = null
    private val controller = ChatController(
        currentUser = ChatUser(id = "me", name = "我"),
        scope = scope,
        sender = ChatMessageSender { message -> reply(message) },
    )

    override fun created() {
        super.created()
        liveProvider = createLiveProvider()
    }

    override fun pageWillDestroy() {
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
                    headerTitle = if (ctx.liveProvider == null) "Chat" else "Chat · SSE",
                )
            }
        }
    }

    private fun createLiveProvider(): LlmProvider? {
        val baseUrl = pageData.params.optString("llmBaseUrl").trim()
        val model = pageData.params.optString("llmModel").trim()
        if (baseUrl.isEmpty() || model.isEmpty()) return null

        return OpenAiCompatibleProvider(
            transport = KuiklyNetworkLlmTransport(
                acquireModule<KRSseModule>(KRSseModule.MODULE_NAME),
            ),
            baseUrl = baseUrl,
            model = model,
            apiKey = pageData.params.optString("llmApiKey"),
            systemPrompt = pageData.params.optString("llmSystemPrompt")
                .ifBlank { "You are a helpful assistant. Reply in the user's language." },
        )
    }

    private fun reply(message: ChatMessage): Flow<ChatStreamEvent> {
        val provider = liveProvider ?: return scriptedReply(message)
        return flow {
            provider.generateStream(message.text).collect { delta ->
                emit(ChatStreamEvent.TextDelta(text = delta, format = TextFormat.MARKDOWN))
            }
            emit(ChatStreamEvent.Completed)
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
