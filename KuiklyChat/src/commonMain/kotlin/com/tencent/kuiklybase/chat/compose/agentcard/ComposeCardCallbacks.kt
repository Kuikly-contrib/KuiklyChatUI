package com.tencent.kuiklybase.chat.compose.agentcard

import androidx.compose.runtime.Composable
import com.tencent.kuiklybase.chat.agentcard.AgentCard
import com.tencent.kuiklybase.chat.agentcard.SearchItem

/**
 * Compose 版卡片交互回调集合。
 *
 * 1:1 对齐 DSL [com.tencent.kuiklybase.chat.agentcard.AgentCardCallbacks]。
 */
data class ComposeCardCallbacks(
    /** 复制代码 */
    val onCopyCode: ((String) -> Unit)? = null,
    /** 点击搜索结果条目 */
    val onSearchItemClick: ((SearchItem) -> Unit)? = null,
    /** 点击链接预览 */
    val onLinkClick: ((String) -> Unit)? = null,
    /** 点击错误卡片的重试按钮 */
    val onRetry: (() -> Unit)? = null,
    /**
     * 业务侧自定义 [AgentCard.Custom] 渲染器（可选）。
     *
     * 返回 `true` 表示业务已完整渲染该卡片，列表内置的 [CustomCard] 兜底将被跳过；
     * 返回 `false` 表示业务无法处理，回退默认渲染。
     *
     * 用途：让宿主项目（如 TdsStudio）针对特定 `card.type`（如 `version_card`）
     * 注入自有的 Composable，无需在 KuiklyChat 模块里硬编码业务卡片实现。
     */
    val customCardRenderer: (@Composable (AgentCard.Custom) -> Boolean)? = null,
)

/**
 * Compose 版卡片列表的全局尺寸/行为配置。
 *
 * 1:1 对齐 DSL [com.tencent.kuiklybase.chat.agentcard.AgentCardListOptions]。
 */
data class ComposeCardListOptions(
    val codeBlockMaxHeight: Float = 360f
)
