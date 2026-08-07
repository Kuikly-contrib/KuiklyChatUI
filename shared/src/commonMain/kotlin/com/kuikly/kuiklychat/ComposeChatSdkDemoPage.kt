package com.kuikly.kuiklychat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
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
import com.tencent.kuiklybase.chat.compose.core.ChatMediaContext
import com.tencent.kuiklybase.chat.compose.core.ChatMediaRequest
import com.tencent.kuiklybase.chat.compose.core.ChatStrings
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.core.ChatUserResolver
import com.tencent.kuiklybase.chat.compose.ui.ChatMediaConfig
import com.tencent.kuiklybase.chat.compose.ui.ChatMediaRequestResolver
import com.tencent.kuiklybase.chat.compose.ui.ChatUserResolutionConfig
import com.tencent.kuiklybase.chat.compose.ui.ChatScreen

/** Standalone demo for validating the new Compose-first Chat UI SDK. */
@Page("compose_chat_sdk_demo", supportInLocal = true)
internal class ComposeChatSdkDemoPage : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent { DemoContent() }
    }

    @Composable
    private fun DemoContent() {
        val currentUser = remember { ChatUser(id = "user", name = "You") }
        val assistant = remember { ChatUser(id = "assistant", name = "Assistant") }
        val demoMessages = remember { composeChatSdkDemoMessages() }
        val controller = rememberRandomAuthorChatController(
            currentUser = currentUser,
            assistant = assistant,
            initialMessages = demoMessages,
        )
        val rendererRegistry = remember { com.tencent.kuiklybase.chat.compose.ui.defaultChatRendererRegistry() }
        val mediaLoader = remember { DemoChatMediaLoader() }
        val mediaConfig = remember(mediaLoader) {
            ChatMediaConfig(
                loader = mediaLoader,
                requestResolver = ChatMediaRequestResolver { context ->
                    ChatMediaRequest(
                        source = context.source,
                        cacheKey = "demo:${context.source}",
                        headers = if (context.source.startsWith("http")) {
                            mapOf("Authorization" to DemoChatMediaLoader.DEMO_AUTH)
                        } else {
                            emptyMap()
                        },
                        mimeType = (context as? ChatMediaContext.AttachmentPreview)
                            ?.attachment
                            ?.mimeType,
                    )
                },
            )
        }

        val attachmentIntegration = rememberDemoChatAttachmentIntegration()

        DisposableEffect(mediaLoader) {
            onDispose { mediaLoader.close() }
        }
        val theme = ChatTheme.Default.copy(
            backgroundColor = 0xFFF6F8F7,
            outgoingBubbleColor = 0xFF176B5B,
            accentColor = 0xFF176B5B
        )
        val messageActions = rememberDemoChatMessageActions(
            controller = controller,
            baseCallbacks = attachmentIntegration.callbacks,
            theme = theme,
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
            DemoHeader(
                mediaRequests = mediaLoader.requestCount,
                mediaCacheHits = mediaLoader.cacheHitCount,
            )
            Box(modifier = Modifier.weight(1f)) {
                ChatScreen(
                    controller = controller,
                    userResolution = userResolution,
                    theme = theme,
                    strings = ChatStrings.Default.copy(
                        inputPlaceholder = "",
                    ),
                    callbacks = messageActions.callbacks,
                    slots = attachmentIntegration.slots,
                    mediaConfig = mediaConfig,
                    rendererRegistry = rendererRegistry,
                    modifier = Modifier.fillMaxSize(),
                )
                messageActions.overlay()
            }
        }
    }
}

@Composable
private fun DemoHeader(
    mediaRequests: Int,
    mediaCacheHits: Int,
) {
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
            text = "Compose Chat UI SDK · 长按消息 · media $mediaCacheHits/$mediaRequests hits",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF17211E)
        )
    }
}
