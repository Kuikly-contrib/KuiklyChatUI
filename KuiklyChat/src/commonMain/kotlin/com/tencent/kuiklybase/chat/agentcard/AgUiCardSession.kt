package com.tencent.kuiklybase.chat.agentcard

import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuiklybase.chat.agui.AgUiEvent

/**
 * AG-UI 事件 → AgentCard 的会话驱动器。
 *
 * 与 [com.tencent.kuiklybase.chat.agui.AgUiSession] 不同，此 Session 直接维护
 * [ObservableList]<[AgentCard]>，驱动 [AgentCardList] 组件渲染。不依赖 ChatSession。
 *
 * 映射规则：
 * - **RUN_STARTED / RUN_FINISHED**：更新生命周期状态（不产生卡片）
 * - **RUN_ERROR**：产生 [AgentCard.Error]
 * - **STEP_STARTED / STEP_FINISHED**：产生 [AgentCard.Loading]（可选）
 * - **TEXT_MESSAGE_START/CONTENT/END/CHUNK**：upsert [AgentCard.TextMessage]（流式）
 * - **TOOL_CALL_START/ARGS/END**：upsert [AgentCard.ToolCall]（RUNNING 状态）
 * - **TOOL_CALL_RESULT**：更新 [AgentCard.ToolCall] 为 SUCCESS + result
 * - **REASONING_MESSAGE_*** ：upsert [AgentCard.Thinking]（流式）
 * - **ACTIVITY_* / CUSTOM / META**：通过 [cardMappers] 用户自定义映射
 *
 * 业务自定义事件可通过 [registerMapper] 注入任意卡片。
 */
class AgUiCardSession(
    /** 线程 ID */
    val threadId: String = "",
    /** 自己的 ID */
    val selfId: String = "self",
    /**
     * 卡片列表的底层存储。默认在类内部 new 一个 [ObservableList]，
     * 但更推荐的做法是在 Pager 里用 `var cards by observableList<AgentCard>()`
     * 创建响应式列表后，通过 [bindCards] 在 `created()` 注入进来——
     * 因为 Kuikly 的响应式追踪依赖 Pager 上下文，直接在字段初始化器里
     * new 出来的 ObservableList 不会接入响应式系统。
     */
    cards: MutableList<AgentCard> = ObservableList()
) {

    /** 对外暴露的卡片存储，可通过 [bindCards] 重新绑定。 */
    var cards: MutableList<AgentCard> = cards
        private set

    /**
     * 重新绑定底层卡片存储（通常用于注入 Pager 里 `by observableList` 创建的响应式 list）。
     * 调用方可选择是否把旧列表内容迁移到新列表。
     */
    fun bindCards(newCards: MutableList<AgentCard>, migrate: Boolean = true) {
        if (migrate && newCards !== cards && cards.isNotEmpty()) {
            newCards.addAll(cards)
        }
        cards = newCards
    }

    /** 当 [cards] 是 [ObservableList] 时的便捷访问（用于直接传给 AgentCardList） */
    @Suppress("UNCHECKED_CAST")
    val observableCards: ObservableList<AgentCard>
        get() = cards as ObservableList<AgentCard>

    /** 当前活跃的文本消息流缓冲（messageId → 累积内容） */
    private val textBuffers = mutableMapOf<String, StringBuilder>()

    /** 当前 chunk 模式下最后一个 messageId（用于 chunk 没带 id 时续接） */
    private var lastChunkMessageId: String = ""

    /** 当前活跃的工具调用缓冲（toolCallId → 工具名 + args） */
    private val toolBuffers = mutableMapOf<String, ToolBuffer>()

    /** 当前活跃的 reasoning 缓冲 */
    private val reasoningBuffers = mutableMapOf<String, StringBuilder>()

    /** 用户自定义的事件 → 卡片 mapper */
    private val cardMappers = mutableListOf<AgUiCardMapper>().apply {
        // 默认注册官方 mapper：CUSTOM plan_* → AgentCard.Plan
        add(PlanMapper)
    }

    /** 生命周期状态 */
    var currentRunId: String = ""
        private set
    var isRunning: Boolean = false
        private set
    var lastError: String = ""
        private set

    /** 生命周期回调 */
    var onRunStarted: ((runId: String) -> Unit)? = null
    var onRunFinished: ((runId: String) -> Unit)? = null
    var onRunError: ((message: String, code: String) -> Unit)? = null

    /**
     * 注册一个事件 → 卡片的自定义 mapper。
     *
     * 多个 mapper 按注册顺序依次尝试，第一个返回非空的卡片被采用。
     * 如果 mapper 返回 `null`，事件会走默认映射。
     */
    fun registerMapper(mapper: AgUiCardMapper) {
        cardMappers.add(mapper)
    }

    /**
     * 分发一个 AG-UI 事件。
     *
     * 会根据事件类型更新 [cards] 列表与内部状态。
     */
    fun dispatch(event: AgUiEvent) {
        // 1) 先让用户自定义 mapper 尝试
        for (mapper in cardMappers) {
            val card = mapper.map(event, this)
            if (card != null) {
                upsert(card)
                updateLifecycle(event)
                return
            }
        }

        // 2) 默认映射
        when (event) {
            is AgUiEvent.RunStarted -> {
                currentRunId = event.runId
                isRunning = true
                lastError = ""
                onRunStarted?.invoke(event.runId)
            }
            is AgUiEvent.RunFinished -> {
                isRunning = false
                // 闭合所有未完成的流式文本
                textBuffers.keys.toList().forEach { finalizeText(it) }
                onRunFinished?.invoke(event.runId)
            }
            is AgUiEvent.RunError -> {
                isRunning = false
                lastError = event.message
                upsert(AgentCard.Error(
                    id = "err_${event.timestamp}",
                    message = event.message,
                    code = event.code,
                    timestamp = event.timestamp
                ))
                onRunError?.invoke(event.message, event.code)
            }
            is AgUiEvent.StepStarted -> {
                // 可选：展示步骤开始的 loading 卡片
                upsert(AgentCard.Loading(
                    id = "step_${event.stepName}",
                    title = event.stepName,
                    subtitle = "步骤进行中",
                    style = LoadingStyle.SPINNER,
                    timestamp = event.timestamp
                ))
            }
            is AgUiEvent.StepFinished -> {
                // 步骤结束时移除对应 loading 卡片
                removeById("step_${event.stepName}")
            }

            is AgUiEvent.TextMessageStart -> {
                textBuffers[event.messageId] = StringBuilder()
                upsert(AgentCard.TextMessage(
                    id = event.messageId,
                    content = "",
                    role = event.role,
                    streaming = true,
                    timestamp = event.timestamp
                ))
            }
            is AgUiEvent.TextMessageContent -> {
                val buffer = textBuffers.getOrPut(event.messageId) { StringBuilder() }
                buffer.append(event.delta)
                upsert(AgentCard.TextMessage(
                    id = event.messageId,
                    content = buffer.toString(),
                    streaming = true,
                    timestamp = event.timestamp
                ))
            }
            is AgUiEvent.TextMessageEnd -> finalizeText(event.messageId)
            is AgUiEvent.TextMessageChunk -> {
                val targetId = event.messageId.ifEmpty { lastChunkMessageId }
                if (targetId.isEmpty()) return
                if (event.messageId.isNotEmpty()) {
                    if (lastChunkMessageId.isNotEmpty() && lastChunkMessageId != event.messageId) {
                        finalizeText(lastChunkMessageId)
                    }
                    lastChunkMessageId = event.messageId
                    if (!textBuffers.containsKey(event.messageId)) {
                        textBuffers[event.messageId] = StringBuilder()
                    }
                }
                val buffer = textBuffers.getOrPut(targetId) { StringBuilder() }
                buffer.append(event.delta)
                upsert(AgentCard.TextMessage(
                    id = targetId,
                    content = buffer.toString(),
                    streaming = true,
                    timestamp = event.timestamp
                ))
            }

            is AgUiEvent.ToolCallStart -> {
                toolBuffers[event.toolCallId] = ToolBuffer(
                    toolName = event.toolCallName,
                    args = StringBuilder()
                )
                upsert(AgentCard.ToolCall(
                    id = "tool_${event.toolCallId}",
                    toolName = event.toolCallName,
                    toolCallId = event.toolCallId,
                    state = ToolState.RUNNING,
                    timestamp = event.timestamp
                ))
            }
            is AgUiEvent.ToolCallArgs -> {
                toolBuffers[event.toolCallId]?.args?.append(event.delta)
                val buf = toolBuffers[event.toolCallId] ?: return
                upsert(AgentCard.ToolCall(
                    id = "tool_${event.toolCallId}",
                    toolName = buf.toolName,
                    toolCallId = event.toolCallId,
                    state = ToolState.RUNNING,
                    args = buf.args.toString(),
                    timestamp = event.timestamp
                ))
            }
            is AgUiEvent.ToolCallEnd -> {
                // 等待 RESULT 事件，不做状态变更
            }
            is AgUiEvent.ToolCallResult -> {
                val buf = toolBuffers.remove(event.toolCallId)
                upsert(AgentCard.ToolCall(
                    id = "tool_${event.toolCallId}",
                    toolName = buf?.toolName.orEmpty(),
                    toolCallId = event.toolCallId,
                    state = ToolState.SUCCESS,
                    args = buf?.args?.toString().orEmpty(),
                    result = event.content,
                    timestamp = event.timestamp
                ))
            }
            is AgUiEvent.ToolCallChunk -> {
                if (event.toolCallId.isNotEmpty() && !toolBuffers.containsKey(event.toolCallId)) {
                    toolBuffers[event.toolCallId] = ToolBuffer(
                        toolName = event.toolCallName,
                        args = StringBuilder()
                    )
                    upsert(AgentCard.ToolCall(
                        id = "tool_${event.toolCallId}",
                        toolName = event.toolCallName,
                        toolCallId = event.toolCallId,
                        state = ToolState.RUNNING,
                        timestamp = event.timestamp
                    ))
                }
                val targetId = event.toolCallId.ifEmpty { toolBuffers.keys.lastOrNull().orEmpty() }
                if (targetId.isEmpty()) return
                val buf = toolBuffers[targetId] ?: return
                if (event.delta.isNotEmpty()) buf.args.append(event.delta)
            }

            is AgUiEvent.ReasoningStart,
            is AgUiEvent.ReasoningMessageStart -> {
                val id = (event as? AgUiEvent.ReasoningStart)?.messageId
                    ?: (event as? AgUiEvent.ReasoningMessageStart)?.messageId
                    ?: return
                reasoningBuffers[id] = StringBuilder()
                upsert(AgentCard.Thinking(
                    id = id,
                    content = "",
                    streaming = true,
                    timestamp = event.timestamp
                ))
            }
            is AgUiEvent.ReasoningMessageContent -> {
                val buffer = reasoningBuffers.getOrPut(event.messageId) { StringBuilder() }
                buffer.append(event.delta)
                upsert(AgentCard.Thinking(
                    id = event.messageId,
                    content = buffer.toString(),
                    streaming = true,
                    timestamp = event.timestamp
                ))
            }
            is AgUiEvent.ReasoningMessageEnd -> {
                val buffer = reasoningBuffers.remove(event.messageId) ?: return
                upsert(AgentCard.Thinking(
                    id = event.messageId,
                    content = buffer.toString(),
                    streaming = false,
                    timestamp = event.timestamp
                ))
            }
            is AgUiEvent.ReasoningEnd -> { /* no-op */ }

            // 其它事件交给 mapper 处理；无 mapper 则忽略
            else -> Unit
        }

        updateLifecycle(event)
    }

    private fun updateLifecycle(event: AgUiEvent) {
        when (event) {
            is AgUiEvent.RunStarted -> {
                currentRunId = event.runId
                isRunning = true
            }
            is AgUiEvent.RunFinished, is AgUiEvent.RunError -> {
                isRunning = false
            }
            else -> Unit
        }
    }

    /** 插入或更新卡片。 */
    fun upsert(card: AgentCard) {
        val idx = cards.indexOfFirst { it.id == card.id }
        if (idx >= 0) {
            cards[idx] = card
        } else {
            cards.add(card)
        }
    }

    /** 按 ID 移除卡片。 */
    fun removeById(id: String) {
        val idx = cards.indexOfFirst { it.id == id }
        if (idx >= 0) cards.removeAt(idx)
    }

    /** 清空所有卡片和内部状态。 */
    fun reset() {
        cards.clear()
        textBuffers.clear()
        toolBuffers.clear()
        reasoningBuffers.clear()
        lastChunkMessageId = ""
        currentRunId = ""
        isRunning = false
        lastError = ""
    }

    private fun finalizeText(messageId: String) {
        val buffer = textBuffers.remove(messageId) ?: return
        upsert(AgentCard.TextMessage(
            id = messageId,
            content = buffer.toString(),
            streaming = false
        ))
        if (lastChunkMessageId == messageId) lastChunkMessageId = ""
    }

    private data class ToolBuffer(
        val toolName: String,
        val args: StringBuilder
    )
}

/**
 * 事件 → 卡片映射器接口。
 *
 * 业务方可实现此接口注入自定义的事件-卡片映射。返回 `null` 表示跳过此映射（走默认处理）。
 */
fun interface AgUiCardMapper {
    fun map(event: AgUiEvent, session: AgUiCardSession): AgentCard?
}
