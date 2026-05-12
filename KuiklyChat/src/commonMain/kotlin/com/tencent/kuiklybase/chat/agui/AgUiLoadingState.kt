package com.tencent.kuiklybase.chat.agui

/**
 * AG-UI Loading 状态体系
 *
 * 给「AI 正在做事」提供三级状态抽象：
 *
 * ```
 * 会话级（整个 Run 正在运行）
 *   └─ 步骤级（当前 Step 正在跑）
 *       └─ 消息级（具体某条消息的 Loading 占位）
 * ```
 *
 * 业务层的典型用法：
 * ```kotlin
 * // 顶部全局加载条
 * val loading = session.loadingSnapshot()
 * if (loading.isActive) {
 *     // 显示 loading.bestLabel
 * }
 *
 * // 消息级 loading 占位
 * session.showLoading(id = "planning", title = "正在规划任务...", subtitle = "分析用户需求")
 * session.updateLoading(id = "planning", subtitle = "拆解步骤")
 * session.dismissLoading("planning")
 * ```
 */

/**
 * Loading 全局快照。
 *
 * 可以响应式订阅（通过 [AgUiSession.onLoadingChanged] 回调）。
 *
 * @param runActive 是否有 Run 在跑
 * @param runId 当前 Run ID（runActive=true 时有效）
 * @param currentStep 当前步骤名（STEP_STARTED 到 STEP_FINISHED 之间）
 * @param currentToolCall 当前正在调用的工具名（未完成时）
 * @param activeLoadingMessages 当前所有 Loading 占位消息的 ID 列表
 * @param primaryLoadingTitle 最主要的 Loading 标题（用于顶部指示条）
 */
data class AgUiLoadingSnapshot(
    val runActive: Boolean,
    val runId: String,
    val currentStep: String,
    val currentToolCall: String,
    val activeLoadingMessages: List<String>,
    val primaryLoadingTitle: String
) {
    /** 是否有任何 Loading 活跃（Run / Step / Tool / Loading 消息任意一个） */
    val isActive: Boolean
        get() = runActive ||
            currentStep.isNotEmpty() ||
            currentToolCall.isNotEmpty() ||
            activeLoadingMessages.isNotEmpty()

    /**
     * 返回最适合展示的 Loading 标签。
     *
     * 优先级：显式 Loading 消息 > 工具调用 > 步骤名 > RunId > 默认文案
     */
    val bestLabel: String
        get() = when {
            primaryLoadingTitle.isNotEmpty() -> primaryLoadingTitle
            currentToolCall.isNotEmpty() -> "正在调用 $currentToolCall"
            currentStep.isNotEmpty() -> "正在执行 $currentStep"
            runActive -> "正在思考中..."
            else -> ""
        }

    companion object {
        val IDLE = AgUiLoadingSnapshot(
            runActive = false,
            runId = "",
            currentStep = "",
            currentToolCall = "",
            activeLoadingMessages = emptyList(),
            primaryLoadingTitle = ""
        )
    }
}

/**
 * Loading 状态变化监听器。
 */
fun interface AgUiLoadingListener {
    fun onLoadingChanged(snapshot: AgUiLoadingSnapshot)
}
