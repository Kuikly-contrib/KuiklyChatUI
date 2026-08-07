package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.lazy.LazyListState
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuiklybase.chat.compose.adapter.agentCardRendererRegistry
import com.tencent.kuiklybase.chat.compose.core.ChatController
import com.tencent.kuiklybase.chat.compose.core.ChatStrings
import com.tencent.kuiklybase.chat.compose.core.ChatTextRenderConfig
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.chat.compose.core.MessageRendererRegistry
import com.tencent.kuiklybase.chat.compose.core.UserCache

/** Compose DSL entry point for the new Chat UI SDK (no user resolution). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    controller: ChatController,
    modifier: Modifier = Modifier,
    theme: ChatTheme = ChatTheme.Default,
    strings: ChatStrings = ChatStrings.Default,
    config: ChatScreenConfig = ChatScreenConfig.Default,
    slots: ChatSlots = ChatSlots.Default,
    callbacks: ChatCallbacks = ChatCallbacks.Empty,
    mediaConfig: ChatMediaConfig = ChatMediaConfig.Default,
    textRenderConfig: ChatTextRenderConfig = ChatTextRenderConfig.Default,
    rendererRegistry: MessageRendererRegistry = defaultChatRendererRegistry()
) {
    ChatScreenContent(
        controller = controller,
        userResolution = null,
        modifier = modifier,
        theme = theme,
        strings = strings,
        config = config,
        slots = slots,
        callbacks = callbacks,
        mediaConfig = mediaConfig,
        textRenderConfig = textRenderConfig,
        rendererRegistry = rendererRegistry,
    )
}

/** Compose DSL entry with [ChatUserResolutionConfig] for async user directory resolution. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    controller: ChatController,
    userResolution: ChatUserResolutionConfig,
    modifier: Modifier = Modifier,
    theme: ChatTheme = ChatTheme.Default,
    strings: ChatStrings = ChatStrings.Default,
    config: ChatScreenConfig = ChatScreenConfig.Default,
    slots: ChatSlots = ChatSlots.Default,
    callbacks: ChatCallbacks = ChatCallbacks.Empty,
    mediaConfig: ChatMediaConfig = ChatMediaConfig.Default,
    textRenderConfig: ChatTextRenderConfig = ChatTextRenderConfig.Default,
    rendererRegistry: MessageRendererRegistry = defaultChatRendererRegistry()
) {
    require(userResolution.cacheNamespace.isNotBlank()) {
        "ChatUserResolutionConfig.cacheNamespace must not be blank"
    }
    ChatScreenContent(
        controller = controller,
        userResolution = userResolution,
        modifier = modifier,
        theme = theme,
        strings = strings,
        config = config,
        slots = slots,
        callbacks = callbacks,
        mediaConfig = mediaConfig,
        textRenderConfig = textRenderConfig,
        rendererRegistry = rendererRegistry,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatScreenContent(
    controller: ChatController,
    userResolution: ChatUserResolutionConfig?,
    modifier: Modifier,
    theme: ChatTheme,
    strings: ChatStrings,
    config: ChatScreenConfig,
    slots: ChatSlots,
    callbacks: ChatCallbacks,
    mediaConfig: ChatMediaConfig,
    textRenderConfig: ChatTextRenderConfig,
    rendererRegistry: MessageRendererRegistry,
) {
    val scope = rememberCoroutineScope()
    val listState = remember(controller) { LazyListState() }
    val scrollState = remember(listState, scope) { ChatListScrollState(listState, scope) }

    val ownedCache = if (userResolution != null && userResolution.cache == null) {
        rememberUserCache()
    } else {
        null
    }
    val cache: UserCache? = userResolution?.cache ?: ownedCache
    val latestResolver = rememberUpdatedState(userResolution?.resolver)
    val resolverAvailable = userResolution?.resolver != null
    val mediaRuntime = rememberChatMediaRuntime(mediaConfig)
    var keyboardHeight by remember { mutableStateOf(0f) }
    // Kuikly keyboardHeightChange reports layout units; use as dp directly (see KuiklyAiChatView).
    val keyboardPadding = keyboardHeight.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(theme.backgroundColor))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = keyboardPadding),
        ) {
            ChatEffectHandler(
                controller = controller,
                scrollState = scrollState,
                theme = theme,
                strings = strings,
                slots = slots,
                callbacks = callbacks
            )
            ChatMessageList(
                controller = controller,
                scrollState = scrollState,
                theme = theme,
                strings = strings,
                config = config,
                slots = slots,
                callbacks = callbacks,
                textRenderConfig = textRenderConfig,
                mediaRuntime = mediaRuntime,
                rendererRegistry = rendererRegistry,
                userResolution = userResolution,
                userCache = cache,
                latestResolver = latestResolver,
                resolverAvailable = resolverAvailable,
                modifier = Modifier.weight(1f)
            )
            ChatComposer(
                controller = controller,
                theme = theme,
                strings = strings,
                config = config,
                slots = slots,
                callbacks = callbacks,
                onKeyboardHeightChange = { keyboardHeight = it },
            )
        }
    }
}

fun defaultChatRendererRegistry(): MessageRendererRegistry =
    agentCardRendererRegistry().withFallback(unknownCustomRenderer())
