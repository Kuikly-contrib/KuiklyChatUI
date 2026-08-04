package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.LazyListState
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuiklybase.chat.compose.core.ChatController
import com.tencent.kuiklybase.chat.compose.core.ChatIntent
import com.tencent.kuiklybase.chat.compose.core.ChatStrings
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.chat.compose.core.MessageRendererRegistry
import com.tencent.kuiklybase.chat.compose.core.groupStatus
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
    rendererRegistry: MessageRendererRegistry,
    modifier: Modifier = Modifier
) {
    val state = controller.state
    val convFingerprint = state.messages.firstOrNull()?.id ?: "empty"
    val listState = scrollState.listState

    val tailSignature = state.messages.size to (state.messages.lastOrNull()?.text?.length ?: 0)

    LaunchedEffect(convFingerprint, tailSignature) {
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
            listState.firstVisibleItemScrollOffset <= config.loadEarlierThresholdPx
        }
            .distinctUntilChanged()
            .collect { atTop ->
                if (atTop && state.messages.isNotEmpty()) {
                    controller.dispatch(ChatIntent.LoadEarlier)
                }
            }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item(key = "__chat_messages__") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    when {
                        state.isInitialLoading -> {
                            slots.loadEarlierContent?.invoke(true) { } ?: LoadingEarlierHeader(strings, theme, true)
                        }
                        state.isLoadingEarlier -> {
                            slots.loadEarlierContent?.invoke(true) { } ?: LoadingEarlierHeader(strings, theme, true)
                        }
                        state.hasMoreEarlier && state.messages.isNotEmpty() -> {
                            slots.loadEarlierContent?.invoke(false) {
                                controller.dispatch(ChatIntent.LoadEarlier)
                            } ?: Text(
                                text = strings.loadEarlier,
                                color = Color(theme.accentColor),
                                fontSize = theme.smallFontSize.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { controller.dispatch(ChatIntent.LoadEarlier) }
                                    .padding(8.dp)
                            )
                        }
                    }

                    if (state.messages.isEmpty() && !state.isInitialLoading) {
                        slots.emptyContent?.invoke() ?: EmptyChatContent(strings, theme)
                    }

                    state.messages.forEachIndexed { index, message ->
                        key(message.id) {
                            val previous = state.messages.getOrNull(index - 1)
                            val next = state.messages.getOrNull(index + 1)
                            val group = message.groupStatus(
                                previous = previous,
                                next = next,
                                groupingWindowMillis = config.groupingWindowMillis
                            )
                            val customRenderer = rendererRegistry.rendererFor(message)
                            if (customRenderer != null) {
                                val rendererContent: @Composable () -> Unit = {
                                    customRenderer.Render(
                                        message,
                                        group,
                                        theme,
                                        Modifier.fillMaxWidth()
                                    )
                                }
                                if (slots.messageRowContent != null) {
                                    slots.messageRowContent.invoke(message, group, rendererContent)
                                } else {
                                    ChatMessageRow(
                                        message = message,
                                        groupStatus = group,
                                        controller = controller,
                                        theme = theme,
                                        strings = strings,
                                        config = config,
                                        slots = slots,
                                        callbacks = callbacks,
                                        content = rendererContent
                                    )
                                }
                            } else if (slots.messageContent != null) {
                                slots.messageContent.invoke(message, group)
                            } else {
                                DefaultChatMessageRow(
                                    message = message,
                                    groupStatus = group,
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
