package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.chat.compose.core.ChatController
import com.tencent.kuiklybase.chat.compose.core.ChatIntent
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatStrings
import com.tencent.kuiklybase.chat.compose.core.ChatTextRenderConfig
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.chat.compose.core.ChatUserResolver
import com.tencent.kuiklybase.chat.compose.core.MessageRendererRegistry
import com.tencent.kuiklybase.chat.compose.core.UserCache
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChatMessageList(
    controller: ChatController,
    scrollState: ChatListScrollState,
    theme: ChatTheme,
    strings: ChatStrings,
    config: ChatScreenConfig,
    slots: ChatSlots,
    callbacks: ChatCallbacks,
    textRenderConfig: ChatTextRenderConfig,
    mediaRuntime: ChatMediaRuntime,
    rendererRegistry: MessageRendererRegistry,
    userResolution: ChatUserResolutionConfig? = null,
    userCache: UserCache? = null,
    latestResolver: State<ChatUserResolver?>? = null,
    resolverAvailable: Boolean = false,
    modifier: Modifier = Modifier
) {
    val state = controller.state
    val listState = scrollState.listState
    val lastMessage = state.messages.lastOrNull()
    val tailSignature = lastMessage?.let { it.id to it.text.length }

    LaunchedEffect(tailSignature) {
        scrollState.scrollToBottom(force = false)
    }

    LaunchedEffect(listState) {
        snapshotFlow { scrollState.isNearBottom() }
            .distinctUntilChanged()
            .collect { nearBottom ->
                controller.dispatch(ChatIntent.SetListNearBottom(nearBottom))
            }
    }

    var anchorScrollOffset by remember { mutableIntStateOf(0) }
    var messagesBeforeLoad by remember { mutableIntStateOf(0) }

    LaunchedEffect(state.isLoadingEarlier) {
        if (state.isLoadingEarlier) {
            anchorScrollOffset = listState.firstVisibleItemScrollOffset
            messagesBeforeLoad = state.messages.size
        }
    }

    LaunchedEffect(state.messages.size, state.isLoadingEarlier) {
        if (!state.isLoadingEarlier && anchorScrollOffset > 0 && messagesBeforeLoad > 0) {
            val insertedCount = state.messages.size - messagesBeforeLoad
            if (insertedCount > 0) {
                val insertedHeight = insertedCount * config.estimatedMessageHeightPx
                val newOffset = anchorOffsetAfterPrepend(anchorScrollOffset, insertedHeight)
                listState.scrollToItem(0, newOffset)
            }
            anchorScrollOffset = 0
            messagesBeforeLoad = 0
        }
    }

    LaunchedEffect(listState, state.hasMoreEarlier, state.messages.size) {
        if (!state.hasMoreEarlier || state.isLoadingEarlier || state.isInitialLoading) return@LaunchedEffect
        snapshotFlow {
            listState.firstVisibleItemIndex == 0 &&
                listState.firstVisibleItemScrollOffset <= config.loadEarlierThresholdPx
        }
            .distinctUntilChanged()
            .collect { atTop ->
                if (atTop && state.messages.isNotEmpty()) {
                    controller.dispatch(ChatIntent.LoadEarlier)
                }
            }
    }

    val reserveSenderNameSpace = userResolution?.reserveSenderNameSpace == true

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item(key = "__chat_messages__") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LoadEarlierSection(
                        state = controller.state,
                        slots = slots,
                        strings = strings,
                        theme = theme,
                        onLoadEarlier = { controller.dispatch(ChatIntent.LoadEarlier) }
                    )

                    if (state.messages.isEmpty() && !state.isInitialLoading) {
                        slots.emptyContent?.invoke() ?: EmptyChatContent(strings, theme)
                    }

                    state.messages.forEachIndexed { index, rawMessage ->
                        key(rawMessage.id) {
                            ChatMessageListItem(
                                rawMessage = rawMessage,
                                index = index,
                                messages = state.messages,
                                controller = controller,
                                theme = theme,
                                strings = strings,
                                config = config,
                                slots = slots,
                                callbacks = callbacks,
                                textRenderConfig = textRenderConfig,
                                mediaRuntime = mediaRuntime,
                                rendererRegistry = rendererRegistry,
                                userResolution = userResolution,
                                userCache = userCache,
                                latestResolver = latestResolver,
                                resolverAvailable = resolverAvailable,
                                reserveSenderNameSpace = reserveSenderNameSpace,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        if (!scrollState.isNearBottom() && state.messages.isNotEmpty()) {
            val onScrollDown = {
                scrollState.scrollToBottom(force = true)
                controller.dispatch(ChatIntent.ClearUnread)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            ) {
                slots.scrollToBottomContent?.invoke(state.unreadCount, onScrollDown)
                    ?: ScrollToBottomButton(
                        unreadCount = state.unreadCount,
                        theme = theme,
                        strings = strings,
                        onClick = onScrollDown
                    )
            }
        }
    }
}

@Composable
private fun LoadEarlierSection(
    state: com.tencent.kuiklybase.chat.compose.core.ChatState,
    slots: ChatSlots,
    strings: ChatStrings,
    theme: ChatTheme,
    onLoadEarlier: () -> Unit,
) {
    when {
        state.isInitialLoading -> {
            slots.loadEarlierContent?.invoke(true) { } ?: LoadingEarlierHeader(strings, theme, true)
        }
        state.isLoadingEarlier -> {
            slots.loadEarlierContent?.invoke(true) { } ?: LoadingEarlierHeader(strings, theme, true)
        }
        state.hasMoreEarlier && state.messages.isNotEmpty() -> {
            slots.loadEarlierContent?.invoke(false, onLoadEarlier)
                ?: ChatPressableText(
                    enabled = true,
                    onClick = onLoadEarlier,
                    modifier = Modifier.fillMaxWidth(),
                ) { _ ->
                    Text(
                        text = strings.loadEarlier,
                        color = Color(theme.accentColor),
                        fontSize = theme.smallFontSize.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                    )
                }
        }
    }
}

@Composable
private fun LoadingEarlierHeader(strings: ChatStrings, theme: ChatTheme, loading: Boolean) {
    if (!loading) return
    Text(
        text = strings.loading,
        color = Color(theme.placeholderColor),
        fontSize = theme.smallFontSize.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    )
}
