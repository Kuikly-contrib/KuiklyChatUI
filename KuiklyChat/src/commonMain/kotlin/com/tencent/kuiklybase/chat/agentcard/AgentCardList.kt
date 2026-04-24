package com.tencent.kuiklybase.chat.agentcard

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.directives.vforLazy
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.views.*

/**
 * Agent 卡片列表容器。
 *
 * 接受 [ObservableList]<[AgentCard]>，自动根据卡片类型分发到对应的渲染函数。
 *
 * 使用示例：
 * ```
 * class MyPage : BasePager() {
 *     private val session = AgUiCardSession()  // 内部维护 ObservableList<AgentCard>
 *
 *     override fun body(): ViewBuilder = {
 *         AgentCardList({ session.cards })
 *     }
 * }
 * ```
 *
 * @param cards 卡片列表提供者（响应式）
 * @param theme 主题
 * @param callbacks 卡片交互回调
 * @param options 尺寸/行为相关的全局配置（如 CodeBlock 最大高度）
 */
fun ViewContainer<*, *>.AgentCardList(
    cards: () -> ObservableList<AgentCard>,
    theme: AgentCardTheme = AgentCardTheme.Default,
    callbacks: AgentCardCallbacks = AgentCardCallbacks(),
    options: AgentCardListOptions = AgentCardListOptions()
) {
    List {
        attr {
            flex(1f)
            backgroundColor(Color(0xFFF8FAFC))
        }

        vforLazy(cards) { card, _, _ ->
            // vforLazy 闭包要求单一根节点
            View {
                attr { alignSelfStretch() }
                renderCard(card, theme, callbacks, options)
            }
        }
    }
}

/**
 * 单卡片分发渲染。外部也可直接调用（用于非列表场景，如直接嵌入到其他布局）。
 */
fun ViewContainer<*, *>.renderCard(
    card: AgentCard,
    theme: AgentCardTheme = AgentCardTheme.Default,
    callbacks: AgentCardCallbacks = AgentCardCallbacks(),
    options: AgentCardListOptions = AgentCardListOptions()
) {
    when (card) {
        is AgentCard.CodeBlock -> CodeBlockCard(
            card = card,
            theme = theme,
            maxHeight = options.codeBlockMaxHeight,
            onCopy = callbacks.onCopyCode
        )
        is AgentCard.CommandExec -> CommandExecCard(card, theme)
        is AgentCard.FileEdit -> FileEditCard(card, theme)
        is AgentCard.ToolCall -> ToolCallCard(card, theme)
        is AgentCard.Thinking -> ThinkingCard(card, theme)
        is AgentCard.SearchResult -> SearchResultCard(card, theme, callbacks.onSearchItemClick)
        is AgentCard.Loading -> LoadingCard(card, theme)
        is AgentCard.Error -> ErrorCard(card, theme, callbacks.onRetry)
        is AgentCard.LinkPreview -> LinkPreviewCard(card, theme, callbacks.onLinkClick)
        is AgentCard.Table -> TableCard(card, theme)
        is AgentCard.Chart -> ChartCard(card, theme)
        is AgentCard.Plan -> PlanCard(card, theme)
        is AgentCard.Custom -> CustomCard(card, theme)
        is AgentCard.TextMessage -> TextMessageCard(card, theme)
        is AgentCard.UserMessage -> UserMessageCard(card, theme)
    }
}

/**
 * 卡片交互回调集合。
 */
data class AgentCardCallbacks(
    /** 复制代码 */
    val onCopyCode: ((String) -> Unit)? = null,
    /** 点击搜索结果条目 */
    val onSearchItemClick: ((SearchItem) -> Unit)? = null,
    /** 点击链接预览 */
    val onLinkClick: ((String) -> Unit)? = null,
    /** 点击错误卡片的重试按钮 */
    val onRetry: (() -> Unit)? = null
)

/**
 * 卡片列表的全局尺寸/行为配置。
 *
 * 目前只包含 CodeBlock 的最大高度，后续可扩展其他卡片的尺寸约束（FileEdit、CommandExec 等）。
 */
data class AgentCardListOptions(
    /**
     * CodeBlock 代码区的最大高度（dp）。
     * `<= 0` 表示不限高（代码全部展开），超过此高度时代码区将出现上下滚动条。
     * 默认 `360f`，约等于 20 行代码。
     */
    val codeBlockMaxHeight: Float = 360f
)
