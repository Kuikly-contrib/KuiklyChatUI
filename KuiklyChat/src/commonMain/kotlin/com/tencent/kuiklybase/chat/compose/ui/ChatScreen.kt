package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.lazy.LazyListState
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuiklybase.chat.compose.adapter.agentCardRendererRegistry
import com.tencent.kuiklybase.chat.compose.core.ChatController
import com.tencent.kuiklybase.chat.compose.core.ChatStrings
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.chat.compose.core.MessageRendererRegistry

/** Compose DSL entry point for the new Chat UI SDK. */
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
    rendererRegistry: MessageRendererRegistry = defaultChatRendererRegistry()
) {
    val scope = rememberCoroutineScope()
    val convFingerprint = controller.state.messages.firstOrNull()?.id ?: "empty"
    val listState = remember(convFingerprint) { LazyListState() }
    val scrollState = remember(listState, scope) { ChatListScrollState(listState, scope) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(theme.backgroundColor))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                rendererRegistry = rendererRegistry,
                modifier = Modifier.weight(1f)
            )
            ChatComposer(
                controller = controller,
                theme = theme,
                strings = strings,
                config = config,
                slots = slots,
                callbacks = callbacks
            )
        }
    }
}

fun defaultChatRendererRegistry(): MessageRendererRegistry =
    agentCardRendererRegistry().withFallback(unknownCustomRenderer())
