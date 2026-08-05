package com.kuikly.kuiklychat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuiklybase.chat.agentcard.AgUiCardSession
import com.tencent.kuiklybase.chat.compose.adapter.AgentCardComposeBridge
import com.tencent.kuiklybase.chat.compose.adapter.rememberLlmChatUiController
import com.tencent.kuiklybase.chat.compose.core.ChatStrings
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.core.ChatUserResolver
import com.tencent.kuiklybase.chat.compose.ui.ChatUserResolutionConfig
import com.tencent.kuiklybase.chat.compose.ui.ChatScreen
import com.tencent.kuiklybase.chat.kuiklyai.provider.AgUiLlmProvider
import com.tencent.kuiklybase.chat.kuiklyai.provider.AgUiScriptedScenarios
import com.tencent.kuiklybase.chat.kuiklyai.provider.ScriptedSseLlmTransport

/** Standalone demo for validating the new Compose-first Chat UI SDK. */
@Page("compose_chat_sdk_demo", supportInLocal = true)
internal class ComposeChatSdkDemoPage : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent { DemoContent() }
    }

    @Composable
    private fun DemoContent() {
        val cardSession = remember { AgUiCardSession(cards = mutableListOf()) }
        val cardBridge = remember(cardSession) { AgentCardComposeBridge(cardSession) }
        val transport = remember { ScriptedSseLlmTransport(AgUiScriptedScenarios.textAndToolCall) }
        val provider = remember(transport, cardBridge) {
            AgUiLlmProvider(
                transport = transport,
                endpoint = "memory://agui",
                onEvent = cardBridge.onEvent
            )
        }
        val controller = rememberLlmChatUiController(
            provider = provider,
            currentUser = ChatUser(id = "user", name = "You"),
            assistant = ChatUser(id = "assistant", name = "Assistant")
        )
        val rendererRegistry = remember { com.tencent.kuiklybase.chat.compose.ui.defaultChatRendererRegistry() }

        DisposableEffect(controller, cardBridge) {
            cardBridge.attach(controller)
            onDispose { cardBridge.detach() }
        }
        val theme = ChatTheme.Default.copy(
            backgroundColor = 0xFFF6F8F7,
            outgoingBubbleColor = 0xFF176B5B,
            accentColor = 0xFF176B5B
        )

        val userResolution = remember {
            ChatUserResolutionConfig(
                cacheNamespace = "compose_demo",
                resolver = ChatUserResolver { userId ->
                    when (userId) {
                        "user" -> ChatUser(id = userId, name = "You")
                        "assistant" -> ChatUser(id = userId, name = "Assistant")
                        else -> ChatUser(id = userId, name = userId)
                    }
                },
                reserveSenderNameSpace = true,
            )
        }

        Column(modifier = Modifier.fillMaxSize().background(Color(theme.backgroundColor))) {
            DemoHeader()
            ChatScreen(
                controller = controller,
                userResolution = userResolution,
                theme = theme,
                strings = ChatStrings.Default.copy(inputPlaceholder = "试试新的 Compose Chat UI"),
                rendererRegistry = rendererRegistry,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DemoHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(Color.White)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = com.tencent.kuikly.compose.foundation.layout.Arrangement.Center
    ) {
        Text(
            text = "Compose Chat UI SDK",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF17211E)
        )
    }
}
