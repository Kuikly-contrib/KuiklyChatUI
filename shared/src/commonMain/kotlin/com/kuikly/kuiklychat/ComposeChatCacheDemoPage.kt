package com.kuikly.kuiklychat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.tencent.kuiklybase.chat.compose.adapter.AttachmentUriCodec
import com.tencent.kuiklybase.chat.compose.core.ChatController
import com.tencent.kuiklybase.chat.compose.core.ChatMediaContext
import com.tencent.kuiklybase.chat.compose.core.ChatMediaRequest
import com.tencent.kuiklybase.chat.compose.core.ChatStrings
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.ui.ChatMediaConfig
import com.tencent.kuiklybase.chat.compose.ui.ChatMediaRequestResolver
import com.tencent.kuiklybase.chat.compose.ui.ChatScreen
import com.tencent.kuiklybase.chat.compose.ui.rememberChatCrossCache
import kotlinx.coroutines.delay

private const val CACHE_NAMESPACE = "compose_cache_demo"
private const val DEMO_REMOTE_IMAGE =
    "https://picsum.photos/seed/kuikly-chat-cache/240/240"

/**
 * Demonstrates the default [ChatCrossCache] path backed by [KRChatByteCacheModule].
 *
 * Requires host registration of `chatByteCacheExternalModules()` (this page's base class) and the
 * platform native bridge (`KuiklyChatAndroid` / `KuiklyChatIOS` / `KuiklyChatOhos`).
 */
@Page("compose_chat_cache_demo", supportInLocal = true)
internal class ComposeChatCacheDemoPage : ComposeChatDemoContainer() {
    override fun willInit() {
        super.willInit()
        setContent { DemoContent() }
    }

    @Composable
    private fun DemoContent() {
        val scope = rememberCoroutineScope()
        val currentUser = remember {
            ChatUser(
                id = "user",
                name = "You",
                avatar = DEMO_REMOTE_IMAGE,
            )
        }
        val assistant = remember {
            ChatUser(
                id = "assistant",
                name = "Assistant",
                avatar = DEMO_REMOTE_IMAGE,
            )
        }
        val demoMessages = remember(assistant) {
            composeChatCacheDemoMessages(assistant, DEMO_REMOTE_IMAGE)
        }
        val controller = remember(scope, currentUser, assistant, demoMessages) {
            ChatController(
                currentUser = currentUser,
                scope = scope,
                sender = randomAuthorDemoSender(currentUser, assistant),
                assistantUser = assistant,
                initialMessages = demoMessages,
            )
        }
        DisposableEffect(controller) {
            onDispose { controller.dispose() }
        }
        val cache = rememberChatCrossCache(CACHE_NAMESPACE)
        val mediaConfig = remember(cache) {
            ChatMediaConfig(
                loader = cache,
                cacheNamespace = CACHE_NAMESPACE,
                requestResolver = ChatMediaRequestResolver { context ->
                    ChatMediaRequest(
                        source = context.source,
                        cacheKey = cacheKeyFor(context.source),
                        mimeType = (context as? ChatMediaContext.AttachmentPreview)
                            ?.attachment
                            ?.mimeType,
                    )
                },
            )
        }
        var cachedEntryCount by remember { mutableIntStateOf(0) }
        var containsRemoteImage by remember { mutableStateOf(false) }
        val attachmentIntegration = rememberDemoChatAttachmentIntegration()

        LaunchedEffect(cache) {
            while (true) {
                containsRemoteImage = cache.contains(cacheKeyFor(DEMO_REMOTE_IMAGE))
                cachedEntryCount = listOf(
                    cacheKeyFor(DEMO_REMOTE_IMAGE),
                    cacheKeyFor(
                        AttachmentUriCodec.toDataUri(
                            mimeType = "image/png",
                            base64Payload = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==",
                        ),
                    ),
                ).count { key -> cache.contains(key) }
                delay(1_000)
            }
        }

        val theme = ChatTheme.Default.copy(
            backgroundColor = 0xFFF6F8F7,
            outgoingBubbleColor = 0xFF176B5B,
            accentColor = 0xFF176B5B,
        )
        val messageActions = rememberDemoChatMessageActions(
            controller = controller,
            baseCallbacks = attachmentIntegration.callbacks,
            theme = theme,
        )

        Column(modifier = Modifier.fillMaxSize().background(Color(theme.backgroundColor))) {
            CacheDemoHeader(
                containsRemoteImage = containsRemoteImage,
                cachedEntryCount = cachedEntryCount,
            )
            Box(modifier = Modifier.weight(1f)) {
                ChatScreen(
                    controller = controller,
                    theme = theme,
                    strings = ChatStrings.Default.copy(
                        inputPlaceholder = "长按消息可复制或删除",
                    ),
                    callbacks = messageActions.callbacks,
                    slots = attachmentIntegration.slots,
                    mediaConfig = mediaConfig,
                    modifier = Modifier.fillMaxSize(),
                )
                messageActions.overlay()
            }
        }
    }
}

private fun cacheKeyFor(source: String): String = "$CACHE_NAMESPACE:$source"

@Composable
private fun CacheDemoHeader(
    containsRemoteImage: Boolean,
    cachedEntryCount: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = com.tencent.kuikly.compose.foundation.layout.Arrangement.Center,
    ) {
        Text(
            text = "ChatCrossCache · KRChatByteCacheModule",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF17211E),
        )
        Text(
            text = "持久化条目 $cachedEntryCount · 远程样例 ${if (containsRemoteImage) "已缓存" else "未命中"}",
            fontSize = 13.sp,
            color = Color(0xFF5C6B66),
        )
    }
}
