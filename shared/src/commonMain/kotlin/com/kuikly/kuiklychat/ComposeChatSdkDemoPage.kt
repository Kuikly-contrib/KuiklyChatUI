package com.kuikly.kuiklychat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.tencent.kuiklybase.chat.compose.adapter.AttachmentUriCodec
import com.tencent.kuiklybase.chat.compose.adapter.AgentCardComposeBridge
import com.tencent.kuiklybase.chat.compose.adapter.rememberLlmChatUiController
import com.tencent.kuiklybase.chat.compose.core.ChatIntent
import com.tencent.kuiklybase.chat.compose.core.ChatMediaContext
import com.tencent.kuiklybase.chat.compose.core.ChatMediaRequest
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatStrings
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.core.ChatUserResolver
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.compose.core.TextFormat
import com.tencent.kuiklybase.chat.compose.core.SYSTEM_CHAT_USER
import com.tencent.kuiklybase.chat.compose.ui.ChatMediaConfig
import com.tencent.kuiklybase.chat.compose.ui.ChatMediaRequestResolver
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

        DisposableEffect(controller, cardBridge) {
            cardBridge.attach(controller)
            onDispose { cardBridge.detach() }
        }
        DisposableEffect(mediaLoader) {
            onDispose { mediaLoader.close() }
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

        LaunchedEffect(controller) {
            if (!controller.state.messages.any { it.id.startsWith("demo-system") }) {
                val user = ChatUser(id = "user", name = "You")
                val assistant = ChatUser(id = "assistant", name = "Assistant")
                listOf(
                    ChatMessage(
                        id = "demo-system-before",
                        author = user,
                        parts = listOf(MessagePart.Text("分组示例：第一条")),
                        createdAt = 1L,
                    ),
                    ChatMessage(
                        id = "demo-system-notice",
                        author = SYSTEM_CHAT_USER,
                        parts = listOf(MessagePart.SystemNotice("Alice 加入了会话")),
                        createdAt = 2L,
                    ),
                    ChatMessage(
                        id = "demo-system-after",
                        author = user,
                        parts = listOf(MessagePart.Text("分组示例：第二条（应与上条断开）")),
                        createdAt = 3L,
                    ),
                    ChatMessage(
                        id = "demo-system-assistant",
                        author = assistant,
                        parts = listOf(MessagePart.Text("助手普通消息")),
                        createdAt = 4L,
                    ),
                ).forEach { message ->
                    controller.dispatch(ChatIntent.UpsertMessage(message))
                }
            }
        }

        LaunchedEffect(controller) {
            if (!controller.state.messages.any { it.id.startsWith("demo-markdown") }) {
                val assistant = ChatUser(id = "assistant", name = "Assistant")
                listOf(
                    ChatMessage(
                        id = "demo-markdown-plain",
                        author = assistant,
                        parts = listOf(MessagePart.Text("纯文本消息（PLAIN）")),
                    ),
                    ChatMessage(
                        id = "demo-markdown-complete",
                        author = assistant,
                        parts = listOf(
                            MessagePart.Text(
                                value = """
                                    ## Markdown 示例

                                    支持 **粗体**、*斜体*、`行内代码` 和 [链接](https://example.com)。

                                    - 无序列表
                                    - 第二项

                                    ```kotlin
                                    fun hello() = "world"
                                    ```
                                """.trimIndent(),
                                format = TextFormat.MARKDOWN,
                            ),
                        ),
                    ),
                    ChatMessage(
                        id = "demo-markdown-mixed",
                        author = assistant,
                        parts = listOf(
                            MessagePart.Text("上文说明：", format = TextFormat.PLAIN),
                            MessagePart.Text("**Markdown 段落**", format = TextFormat.MARKDOWN),
                        ),
                    ),
                ).forEach { message ->
                    controller.dispatch(ChatIntent.UpsertMessage(message))
                }
            }
        }

        LaunchedEffect(controller) {
            if (!controller.state.messages.any { it.id.startsWith("demo-attachment") }) {
                val user = ChatUser(id = "user", name = "You")
                val assistant = ChatUser(id = "assistant", name = "Assistant")
                val tinyPng = AttachmentUriCodec.toDataUri(
                    mimeType = "image/png",
                    base64Payload = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==",
                )
                listOf(
                    ChatMessage(
                        id = "demo-attachment-remote",
                        author = assistant,
                        parts = listOf(
                            MessagePart.Text("远程图片（需网络）："),
                            MessagePart.Attachment(
                                id = "demo-img-remote",
                                uri = "https://picsum.photos/320/200",
                                mimeType = "image/jpeg",
                                name = "sample.jpg",
                            ),
                        ),
                    ),
                    ChatMessage(
                        id = "demo-attachment-data",
                        author = user,
                        parts = listOf(
                            MessagePart.Attachment(
                                id = "demo-img-data",
                                uri = tinyPng,
                                mimeType = "image/png",
                                name = "pixel.png",
                            ),
                        ),
                    ),
                    ChatMessage(
                        id = "demo-attachment-file",
                        author = assistant,
                        parts = listOf(
                            MessagePart.Attachment(
                                id = "demo-file",
                                uri = "https://cdn.example/spec.pdf",
                                mimeType = "application/pdf",
                                name = "KuiklyChat-Spec.pdf",
                                sizeBytes = 1536L,
                            ),
                        ),
                    ),
                    ChatMessage(
                        id = "demo-attachment-fail",
                        author = assistant,
                        parts = listOf(
                            MessagePart.Attachment(
                                id = "demo-img-bad",
                                uri = "https://invalid.local/not-found.png",
                                mimeType = "image/png",
                                name = "broken.png",
                            ),
                        ),
                    ),
                    ChatMessage(
                        id = "demo-attachment-mixed",
                        author = assistant,
                        parts = listOf(
                            MessagePart.Text("混合：说明 + 图 + 文件"),
                            MessagePart.Attachment(
                                id = "demo-mixed-img",
                                uri = tinyPng,
                                mimeType = "image/png",
                                name = "inline.png",
                            ),
                            MessagePart.Attachment(
                                id = "demo-mixed-cached-img",
                                uri = "https://picsum.photos/320/200",
                                mimeType = "image/jpeg",
                                name = "cached-sample.jpg",
                            ),
                            MessagePart.Attachment(
                                id = "demo-mixed-file",
                                uri = "https://cdn.example/notes.txt",
                                mimeType = "text/plain",
                                name = "notes.txt",
                                sizeBytes = 420L,
                            ),
                        ),
                    ),
                ).forEach { message ->
                    controller.dispatch(ChatIntent.UpsertMessage(message))
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().background(Color(theme.backgroundColor))) {
            DemoHeader(
                mediaRequests = mediaLoader.requestCount,
                mediaCacheHits = mediaLoader.cacheHitCount,
            )
            ChatScreen(
                controller = controller,
                userResolution = userResolution,
                theme = theme,
                strings = ChatStrings.Default.copy(inputPlaceholder = "试试新的 Compose Chat UI"),
                callbacks = attachmentIntegration.callbacks,
                slots = attachmentIntegration.slots,
                mediaConfig = mediaConfig,
                rendererRegistry = rendererRegistry,
                modifier = Modifier.weight(1f)
            )
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
            text = "Compose Chat UI SDK · media $mediaCacheHits/$mediaRequests hits",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF17211E)
        )
    }
}
