package com.tencent.kuiklybase.chat.kuiklyai.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.LazyListState
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmChatMessage
import com.tencent.kuiklybase.chat.kuiklyai.runtime.LlmChatController

/**
 * 消息列表。
 *
 * ## 滚动实现说明
 *
 * 用 [LazyColumn] 包一个 `item {}`、内部再用普通 [Column] 全量渲染，
 * 而不是常规的 `items(messages)`。这是本仓库既有的 Kuikly 适配手法
 * （见 `agentcard_compose/ComposeAgentCardList`）：Kuikly Compose 不支持
 * `Modifier.verticalScroll`，而 lazy 测量在某些场景下不渲染 viewport 外的项，
 * 表现为"消息后面是一大段空白"。LazyColumn 提供滚动能力，
 * 内层 Column 提供"全部测量"语义，两者互补。
 *
 * 对话消息量在百级，全量渲染开销可接受。
 *
 * @param messageBuilder 自定义消息渲染，返回 true 表示已接管、跳过默认渲染。
 *                       对应 Flutter AI Toolkit 的 custom response widgets。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LlmMessageList(
    controller: LlmChatController,
    style: LlmChatViewStyle = LlmChatViewStyle.Default,
    strings: LlmChatViewStrings = LlmChatViewStrings.Default,
    suggestions: List<String> = emptyList(),
    onEditMessage: ((LlmChatMessage) -> Unit)? = null,
    messageBuilder: (@Composable (LlmChatMessage) -> Boolean)? = null,
    modifier: Modifier = Modifier
) {
    val messages = controller.messages
    val listState = remember { LazyListState() }

    // 贴底触发签名：消息条数 + 末条文本长度。流式追加时后者持续变化，
    // 所以每来一批 token 都会重新评估是否需要贴底。
    val tailSignature = messages.size to (messages.lastOrNull()?.text?.length ?: 0)

    LaunchedEffect(tailSignature) {
        if (messages.isEmpty()) return@LaunchedEffect
        val info = listState.layoutInfo
        val itemSize = info.visibleItemsInfo.firstOrNull()?.size ?: 0
        val viewport = info.viewportSize.height
        val curOffset = listState.firstVisibleItemScrollOffset
        val maxOffset = (itemSize - viewport).coerceAtLeast(0)
        // 只有当前已接近底部时才自动滚——用户上滑翻历史时不该被拽回去
        val nearBottom = maxOffset == 0 || curOffset >= maxOffset - viewport / 4

        if (nearBottom) {
            // 瞬移而非动画：流式 token 高频触发，动画会断断续续。
            // 直接挂在 LaunchedEffect 上，新的 token 会取消上一次滚动，避免任务堆积。
            listState.scrollToItem(0, scrollOffset = Int.MAX_VALUE / 2)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(style.backgroundColor))
    ) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item(key = "__llm_messages__") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (messages.isEmpty()) {
                        LlmWelcomeSection(
                            suggestions = suggestions,
                            onSuggestionClick = { controller.sendPrompt(it) },
                            style = style,
                            strings = strings
                        )
                    }

                    messages.forEach { message ->
                        val handled = messageBuilder?.invoke(message) ?: false
                        if (!handled) {
                            DefaultMessageRow(
                                message = message,
                                controller = controller,
                                style = style,
                                strings = strings,
                                onEditMessage = onEditMessage
                            )
                        }
                    }

                    // 底部留白：避免最后一条紧贴输入区
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * 按来源分派到用户气泡 / 模型气泡。
 */
@Composable
private fun DefaultMessageRow(
    message: LlmChatMessage,
    controller: LlmChatController,
    style: LlmChatViewStyle,
    strings: LlmChatViewStrings,
    onEditMessage: ((LlmChatMessage) -> Unit)?
) {
    if (message.isUser) {
        LlmUserBubble(
            message = message,
            style = style,
            strings = strings,
            onEditClick = onEditMessage
        )
    } else {
        LlmResponseBubble(
            message = message,
            style = style,
            strings = strings,
            onRetryClick = if (
                message.isRetryableFailure && controller.messages.lastOrNull()?.id == message.id
            ) {
                { controller.retryLast() }
            } else {
                null
            }
        )
    }
}
