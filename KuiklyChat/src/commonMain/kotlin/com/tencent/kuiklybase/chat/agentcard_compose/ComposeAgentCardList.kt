package com.tencent.kuiklybase.chat.agentcard_compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxHeight
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.offset
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.LazyListState
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.platform.LocalDensity
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuiklybase.chat.agentcard.AgentCard
import kotlinx.coroutines.launch

/**
 * Compose 版 Agent 卡片列表。
 *
 * 对齐 DSL [com.tencent.kuiklybase.chat.agentcard.AgentCardList] 的功能，
 * 但滚动由 Compose [LazyColumn] 驱动（而非 DSL 的原生 List + vforLazy）。
 *
 * Sticky-bottom 自动滚动 (openspec: improve-streaming-text-rendering, M7):
 *   - 监听 cards.size + 末位卡片的内容长度签名（流式 token 追加）
 *   - 启发式：当前可视区已包含倒数第二项以后才自动滚（用户上滑读历史时不打断）
 *   - 用 scrollToItem (瞬移) 而非 animateScrollToItem (动画) ——
 *     10Hz 高频触发用动画会断断续续，瞬移贴合"打字机"逐字感
 *
 * @param cards 卡片数据列表
 * @param theme 主题
 * @param callbacks 卡片交互回调
 * @param options 尺寸/行为配置
 * @param modifier Compose Modifier
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ComposeAgentCardList(
    cards: List<AgentCard>,
    theme: ComposeCardTheme = ComposeCardTheme.Default,
    callbacks: ComposeCardCallbacks = ComposeCardCallbacks(),
    options: ComposeCardListOptions = ComposeCardListOptions(),
    modifier: Modifier = Modifier
) {
    // ===== Per-conversation LazyListState =====
    // 用首张卡片的 id 作"当前会话指纹"。每次会话切换时 remember(key) 重新计算
    // 得到一个全新的 LazyListState（firstVisibleItemIndex=0），等价于"按一次
    // 出厂重置"，省去 scrollToItem(0) 异步调用与 LazyColumn 测量竞态。
    //
    // 同会话内 streaming 增量：convFingerprint 不变 → 复用同一 LazyListState
    // → 用户已有的 scroll 位置保留，sticky-bottom 启发式继续生效。
    val convFingerprint = cards.firstOrNull()?.id ?: ""
    val listState = remember(convFingerprint) { LazyListState() }
    val scope = rememberCoroutineScope()

    // 末位卡片签名 — 触发 sticky-bottom 自动滚。
    val lastContentSignature = remember(cards) {
        val last = cards.lastOrNull()
        when (last) {
            is AgentCard.TextMessage -> "txt-${last.content.length}"
            is AgentCard.ToolCall    -> "tool-${last.state}-${last.result.length}"
            is AgentCard.Thinking    -> "think-${last.content.length}"
            is AgentCard.UserMessage -> "user-${last.id}"
            is AgentCard.Error       -> "err-${last.id}"
            null                     -> "empty"
            else                     -> "other-${cards.size}-${last.id}"
        }
    }

    // Sticky-bottom 自动滚：用 firstVisibleItemScrollOffset 判断当前是否
    // 已滚到接近底部（由于现在只有 1 个 item，container 的总高度等于该
    // item 的高度，scroll offset 衡量的是 item 内部的 y 偏移）。
    LaunchedEffect(convFingerprint, lastContentSignature) {
        if (cards.isEmpty()) return@LaunchedEffect
        val info = listState.layoutInfo
        val itemSize = info.visibleItemsInfo.firstOrNull()?.size ?: 0
        val viewport = info.viewportSize.height
        val curOffset = listState.firstVisibleItemScrollOffset
        val maxOffset = (itemSize - viewport).coerceAtLeast(0)
        val nearBottom = maxOffset == 0 || curOffset >= maxOffset - viewport / 4

        com.tencent.kuikly.core.log.KLog.i(
            "ComposeAgentCardList",
            "cards=${cards.size}, itemH=$itemSize, vp=$viewport, " +
                "curOffset=$curOffset, maxOffset=$maxOffset, nearBottom=$nearBottom"
        )

        if (nearBottom) {
            scope.launch {
                listState.scrollToItem(0, scrollOffset = Int.MAX_VALUE / 2)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Kuikly Compose 不支持 Modifier.verticalScroll，但 lazy-measurement
        // 在某些场景下不会渲染 viewport 外的 items（实测 cards=19, viewport=1320,
        // items[0..11] 累积 1532 已超出，items[12..18] 即使在 scroll 下也不
        // 进入 layoutInfo → 视觉上"消息后面是大段空白"）。
        //
        // Workaround（参考 .cursor/rules/kuiklyComposeDSL.mdc）：用 LazyColumn
        // 包裹单个 item {}，内部用普通 Column eager 渲染所有 cards。LazyColumn
        // 提供滚动能力，普通 Column 提供"全部测量"的语义，两者互补。
        // 19~100 张 chat cards eager 渲染开销可忽略。
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "__cards_column__") {
                // ── 诊断：确认 cards 列表在 LazyColumn item 内是否被更新 ──
                val toolCards = cards.filterIsInstance<AgentCard.ToolCall>()
                if (toolCards.isNotEmpty()) {
                    com.tencent.kuikly.core.log.KLog.i(
                        "AgUiRecompose",
                        "CardList item RECOMPOSE | cards=${cards.size} | toolCards=${toolCards.size} | firstToolResult=${toolCards.firstOrNull()?.result?.length ?: -1}"
                    )
                }
                Column(modifier = Modifier.fillMaxWidth()) {
                    cards.forEachIndexed { index, card ->
                        val cardInfo = when (card) {
                            is AgentCard.TextMessage -> "Text(${card.role},len=${card.content.length},streaming=${card.streaming})"
                            is AgentCard.UserMessage -> "User(len=${card.content.length})"
                            is AgentCard.ToolCall -> "Tool(${card.toolName},state=${card.state},resultLen=${card.result.length})"
                            is AgentCard.CodeBlock -> "Code(${card.language},file=${card.filename},len=${card.code.length})"
                            is AgentCard.FileEdit -> "FileEdit(${card.filePath.substringAfterLast('/')})"
                            is AgentCard.Thinking -> "Thinking(len=${card.content.length})"
                            is AgentCard.Loading -> "Loading"
                            is AgentCard.Error -> "Error"
                            else -> card::class.simpleName ?: "?"
                        }
                        com.tencent.kuikly.core.log.KLog.i(
                            "AgUiCards",
                            "[$index] $cardInfo id=${card.id.take(24)}"
                        )
                        ComposeRenderCard(card, theme, callbacks, options)
                    }
                    // 底部留白：防止最后一条消息被步骤条/输入栏遮挡
                    Spacer(Modifier.height(56.dp))
                }
            }
        }

        // ── 右侧滚动进度条 ──────────────────────────────────────────────
        ScrollProgressIndicator(listState, modifier = Modifier.align(Alignment.CenterEnd))
    }
}

/**
 * 单卡片分发渲染。
 *
 * 1:1 对齐 DSL [com.tencent.kuiklybase.chat.agentcard.renderCard]。
 */
@Composable
fun ComposeRenderCard(
    card: AgentCard,
    theme: ComposeCardTheme = ComposeCardTheme.Default,
    callbacks: ComposeCardCallbacks = ComposeCardCallbacks(),
    options: ComposeCardListOptions = ComposeCardListOptions()
) {
    when (card) {
        is AgentCard.TextMessage -> TextMessageCard(card, theme)
        is AgentCard.UserMessage -> UserMessageCard(card, theme)
        is AgentCard.CodeBlock -> CodeBlockCard(
            card, theme, maxHeight = options.codeBlockMaxHeight,
            onCopy = callbacks.onCopyCode
        )
        is AgentCard.CommandExec -> CommandExecCard(card, theme)
        is AgentCard.FileEdit -> FileEditCard(card, theme)
        is AgentCard.ToolCall -> ComposeToolCallCard(card, theme, onCopy = callbacks.onCopyCode)
        is AgentCard.Thinking -> ComposeThinkingCard(card, theme)
        is AgentCard.SearchResult -> ComposeSearchResultCard(
            card, theme, onItemClick = callbacks.onSearchItemClick
        )
        is AgentCard.Loading -> ComposeLoadingCard(card, theme)
        is AgentCard.Error -> ComposeErrorCard(
            card, theme, onRetry = callbacks.onRetry
        )
        is AgentCard.LinkPreview -> ComposeLinkPreviewCard(
            card, theme, onClick = callbacks.onLinkClick
        )
        is AgentCard.Table -> ComposeTableCard(card, theme)
        is AgentCard.Chart -> ComposeChartCard(card, theme)
        is AgentCard.Plan -> ComposePlanCard(card, theme)
        is AgentCard.Custom -> {
            // 优先让业务侧的 customCardRenderer 处理（如 TdsStudio 的 version_card）；
            // 返回 false 表示业务不处理，回退默认 CustomCard。
            val handled = callbacks.customCardRenderer?.invoke(card) ?: false
            if (!handled) CustomCard(card, theme)
        }
        // openspec: add-workflow-phase-progress
        is AgentCard.PhaseHeader -> ComposePhaseEntryHeader(card, theme)
        is AgentCard.TodoProgress -> ComposeTodoProgressCard(card, theme)
    }
}

// ═══════════════════════════════════════════════════════════════════════
// 滚动进度条
// ═══════════════════════════════════════════════════════════════════════

/**
 * 右侧滚动进度条：根据 LazyListState 的 scrollOffset 和 item 总高度
 * 计算 thumb 位置和大小。只在内容可滚动时显示。
 *
 * 轨道：4dp 宽，半透明灰色圆角
 * Thumb：4dp 宽，深灰色圆角，最小高度 32dp
 */
@Composable
private fun ScrollProgressIndicator(
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val info = listState.layoutInfo
    val firstItem = info.visibleItemsInfo.firstOrNull() ?: return
    val itemTotalHeight = firstItem.size  // 单 item 模式，item 高度 = 全部内容高度
    val viewportHeight = info.viewportSize.height
    if (itemTotalHeight <= viewportHeight) return // 无需滚动

    val scrollOffset = listState.firstVisibleItemScrollOffset.toFloat()
    val maxScroll = (itemTotalHeight - viewportHeight).toFloat().coerceAtLeast(1f)
    val scrollFraction = (scrollOffset / maxScroll).coerceIn(0f, 1f)

    // thumb 大小：viewport / content 比例，最小 32dp
    val thumbRatio = (viewportHeight.toFloat() / itemTotalHeight.toFloat()).coerceIn(0.05f, 1f)
    val trackHeightDp = with(LocalDensity.current) { viewportHeight.toDp() }
    val thumbHeightDp = (trackHeightDp * thumbRatio).coerceAtLeast(32.dp)
    val thumbOffsetDp = (trackHeightDp - thumbHeightDp) * scrollFraction

    // 轨道
    Box(
        modifier = modifier
            .padding(end = 2.dp, top = 4.dp, bottom = 4.dp)
            .width(4.dp)
            .fillMaxHeight()
    ) {
        // Thumb
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(thumbHeightDp)
                .offset(y = thumbOffsetDp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0x66888888))
        )
    }
}
