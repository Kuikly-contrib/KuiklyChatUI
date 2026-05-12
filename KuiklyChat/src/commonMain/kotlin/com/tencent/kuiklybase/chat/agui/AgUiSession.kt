package com.tencent.kuiklybase.chat.agui

import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuiklybase.chat.model.ChatMessage
import com.tencent.kuiklybase.chat.model.ChatMessageHelper
import com.tencent.kuiklybase.chat.model.MessageStatus
import com.tencent.kuiklybase.chat.model.MessageType

/**
 * AG-UI 会话状态机
 *
 * 消费 [AgUiEvent] 事件流，维护一次 Agent 运行的完整状态：
 * - **流式文本消息**：TEXT_MESSAGE_START/CONTENT/END 与 TEXT_MESSAGE_CHUNK
 * - **工具调用**：TOOL_CALL_START/ARGS/END 与 TOOL_CALL_CHUNK
 * - **Activity 活动**：ACTIVITY_SNAPSHOT / ACTIVITY_DELTA
 * - **Reasoning 推理**：REASONING_MESSAGE_*（可单独展示或随 assistant 消息合并）
 * - **生命周期**：RUN_STARTED/FINISHED/ERROR、STEP_STARTED/FINISHED
 *
 * 状态机将协议事件转换为 [ChatMessage] 列表，可直接接入 `ChatSession({ session.messages })`。
 *
 * 关键设计：
 * - **协议与 UI 解耦**：ChatMessage 为 UI 层模型，内部通过 extra 字段携带 AG-UI 元数据；
 * - **存储可插拔**：`messages` 使用 `MutableList<ChatMessage>` 作为接口类型，便于注入：
 *     - **生产**：使用 [observableList] 创建的 `ObservableList<ChatMessage>`，
 *       可直接作为 `ChatSession({ session.messages as ObservableList<ChatMessage> })` 的数据源；
 *     - **测试**：使用 [forTesting] 工厂构造，内部用 `mutableListOf()`，无需 Kuikly Pager 环境。
 * - **扩展优先**：所有自定义事件通过 [AgUiHandlerRegistry] 分发，不硬编码业务逻辑；
 * - **幂等**：同一 messageId 多次 upsert 会合并（支持断线重连后的 MESSAGES_SNAPSHOT 覆盖）；
 * - **线程感知**：状态修改需在 Kuikly 线程执行（observable 要求）。
 *
 * 使用示例：
 * ```
 * // 生产：在 Pager 内直接 new（内部默认用 observableList）
 * class MyPage : BasePager() {
 *     private val session = AgUiSession(threadId = "t1")
 *
 *     override fun body(): ViewBuilder = {
 *         ChatSession({ session.messages as ObservableList<ChatMessage> }) { /* ... */ }
 *     }
 * }
 *
 * // 测试：不依赖 Pager 环境
 * val session = AgUiSession.forTesting()
 * session.dispatch(...)
 * ```
 */
class AgUiSession private constructor(
    /**
     * 消息列表 —— 核心状态。
     *
     * 接口类型为 `MutableList<ChatMessage>` 以便测试注入普通 list；
     * 实际运行时默认是 Kuikly 的 `ObservableList<ChatMessage>`，确保 UI 响应式更新。
     */
    val messages: MutableList<ChatMessage>,
    val threadId: String = "",
    /** 自己的发送者 ID，用于区分 user 与 assistant 消息 */
    val selfSenderId: String = "self",
    /** AI 助手名称 */
    var aiSenderName: String = "AI 助手",
    /** AI 头像 URL */
    var aiAvatarUrl: String = "",
    /** 自定义事件扩展点注册表 */
    val handlerRegistry: AgUiHandlerRegistry = AgUiHandlerRegistry()
) : AgUiMappingContext {

    /**
     * 常规构造：使用 Kuikly [ObservableList] 作为内部存储（生产环境默认用法）。
     *
     * 注意：当 AgUiSession 在 Pager 之外构造时（如单元测试），ObservableList 仍可用作普通 MutableList
     *       但响应式更新需要 Pager 上下文。测试场景请使用 [forTesting]。
     */
    constructor(
        threadId: String = "",
        selfSenderId: String = "self",
        aiSenderName: String = "AI 助手",
        aiAvatarUrl: String = "",
        handlerRegistry: AgUiHandlerRegistry = AgUiHandlerRegistry()
    ) : this(
        messages = ObservableList<ChatMessage>(),
        threadId = threadId,
        selfSenderId = selfSenderId,
        aiSenderName = aiSenderName,
        aiAvatarUrl = aiAvatarUrl,
        handlerRegistry = handlerRegistry
    )

    /** 当前运行 ID（RUN_STARTED 时写入） */
    override var currentRunId: String = ""
        private set

    /**
     * 便捷访问：将 [messages] 转为 Kuikly [ObservableList]（用于直接传给 `ChatSession`）。
     *
     * 当 session 用常规构造函数创建时，`messages` 本身就是 ObservableList，此处无额外开销。
     * 当 session 用 [forTesting] 创建时，调用该属性会抛出 `ClassCastException`（测试不应访问此属性）。
     *
     * 典型用法：
     * ```
     * ChatSession({ session.observableMessages }) { /* ... */ }
     * ```
     */
    @Suppress("UNCHECKED_CAST")
    val observableMessages: ObservableList<ChatMessage>
        get() = messages as ObservableList<ChatMessage>

    /** 当前是否正在运行（RUN_STARTED 到 RUN_FINISHED/RUN_ERROR 之间） */
    override var isRunning: Boolean = false
        private set

    /** 当前步骤名（STEP_STARTED 时写入） */
    override var currentStep: String = ""
        private set

    /** 最近一次错误信息 */
    var lastError: String = ""
        private set

    /** Agent 状态字典（STATE_SNAPSHOT / STATE_DELTA 累积） */
    private val stateMap = mutableMapOf<String, Any?>()

    /** [AgUiMappingContext.stateView] 的只读代理 */
    override val stateView: Map<String, Any?> get() = stateMap

    /** [AgUiMappingContext.messagesView] 的只读代理（返回 `messages` 自身视图） */
    override val messagesView: List<ChatMessage> get() = messages

    // ============================
    // 事件 → 卡片 Mapper 责任链
    // ============================

    /** 事件 → ChatMessage 自定义映射器链（优先级高于默认处理） */
    val mapperChain = EventToCardMapperChain()

    /** 注册一个事件映射器（等价于 mapperChain.register） */
    fun registerMapper(mapper: EventToCardMapper) {
        mapperChain.register(mapper)
    }

    // ============================
    // Loading 状态
    // ============================

    /** 工具调用 ID → 工具名（方便快速知道"当前在调用什么"） */
    private val activeToolCalls = linkedMapOf<String, String>()

    /** 当前所有活跃的 Loading 占位消息 ID（按插入顺序） */
    private val loadingMessageIds = linkedSetOf<String>()

    /** Loading 状态监听器列表 */
    private val loadingListeners = mutableListOf<AgUiLoadingListener>()

    /** 订阅 Loading 状态变化 */
    fun onLoadingChanged(listener: AgUiLoadingListener) {
        loadingListeners.add(listener)
    }

    /** 取消订阅 Loading 状态变化 */
    fun removeLoadingListener(listener: AgUiLoadingListener) {
        loadingListeners.remove(listener)
    }

    /** 获取当前 Loading 状态的只读快照 */
    fun loadingSnapshot(): AgUiLoadingSnapshot {
        val primaryTitle = loadingMessageIds.firstOrNull()
            ?.let { id -> messages.firstOrNull { it.id == id } }
            ?.let { LoadingInfo.from(it).title }
            .orEmpty()
        return AgUiLoadingSnapshot(
            runActive = isRunning,
            runId = currentRunId,
            currentStep = currentStep,
            currentToolCall = activeToolCalls.values.lastOrNull().orEmpty(),
            activeLoadingMessages = loadingMessageIds.toList(),
            primaryLoadingTitle = primaryTitle
        )
    }

    private fun notifyLoadingChanged() {
        if (loadingListeners.isEmpty()) return
        val snapshot = loadingSnapshot()
        loadingListeners.forEach { it.onLoadingChanged(snapshot) }
    }

    /**
     * 显示一条 Loading 占位消息。
     *
     * 用法：
     * ```
     * session.showLoading(id = "planning", title = "正在分析需求...")
     * // ... 耗时操作 ...
     * session.updateLoading(id = "planning", subtitle = "拆解步骤")
     * session.dismissLoading("planning")
     * ```
     *
     * @param id 消息 ID（重复调用会更新同一条消息）
     * @param title 主标题
     * @param subtitle 副标题
     * @param style Loading 风格（[LoadingStyle] 其一）
     * @param target 关联目标（工具名、文件名等）
     */
    fun showLoading(
        id: String,
        title: String = "正在思考中",
        subtitle: String = "",
        style: String = LoadingStyle.DOTS,
        target: String = ""
    ) {
        loadingMessageIds.add(id)
        upsertMessage(
            RichCardPayload.loading(
                id = id,
                title = title,
                subtitle = subtitle,
                style = style,
                target = target,
                senderName = aiSenderName,
                senderAvatar = aiAvatarUrl
            )
        )
        notifyLoadingChanged()
    }

    /**
     * 更新已存在的 Loading 占位消息。
     *
     * 任意参数传 null 表示不修改该字段；传空串视为清空。
     */
    fun updateLoading(
        id: String,
        title: String? = null,
        subtitle: String? = null,
        style: String? = null,
        target: String? = null
    ) {
        val idx = messages.indexOfFirst { it.id == id }
        if (idx < 0) return
        val current = messages[idx]
        val newExtra = current.extra.toMutableMap()
        title?.let { newExtra[AgUiExtraKeys.LOADING_TITLE] = it }
        subtitle?.let { newExtra[AgUiExtraKeys.LOADING_SUBTITLE] = it }
        style?.let { newExtra[AgUiExtraKeys.LOADING_STYLE] = it }
        target?.let { newExtra[AgUiExtraKeys.LOADING_TARGET] = it }
        messages[idx] = current.copy(
            content = title ?: current.content,
            extra = newExtra
        )
        notifyLoadingChanged()
    }

    /**
     * 移除 Loading 占位消息。
     *
     * @param id 要移除的 Loading 消息 ID
     * @param replaceWith 若非 null，用此消息替换原占位（避免闪烁），否则直接删除
     */
    fun dismissLoading(id: String, replaceWith: ChatMessage? = null) {
        loadingMessageIds.remove(id)
        val idx = messages.indexOfFirst { it.id == id }
        if (idx >= 0) {
            if (replaceWith != null) {
                messages[idx] = replaceWith
            } else {
                messages.removeAt(idx)
            }
        }
        notifyLoadingChanged()
    }

    /** 清除所有 Loading 占位消息。 */
    fun dismissAllLoading() {
        val ids = loadingMessageIds.toList()
        loadingMessageIds.clear()
        ids.forEach { id ->
            val idx = messages.indexOfFirst { it.id == id }
            if (idx >= 0) messages.removeAt(idx)
        }
        notifyLoadingChanged()
    }

    /** 生命周期回调 */
    var onRunStarted: ((runId: String) -> Unit)? = null
    var onRunFinished: ((runId: String) -> Unit)? = null
    var onRunError: ((message: String, code: String) -> Unit)? = null
    var onStepStarted: ((stepName: String) -> Unit)? = null
    var onStepFinished: ((stepName: String) -> Unit)? = null

    /** 当前正在流式生成的 assistant 消息 ID 集合（messageId → 累积内容） */
    private val streamingBuffers = mutableMapOf<String, StringBuilder>()

    /** 当前活跃的工具调用缓冲（toolCallId → 参数拼接缓冲） */
    private val toolCallBuffers = mutableMapOf<String, ToolCallBuffer>()

    /** Reasoning 消息缓冲（单独展示，与 assistant 消息并行） */
    private val reasoningBuffers = mutableMapOf<String, StringBuilder>()

    /** 当前活动消息（messageId → activityType） */
    private val activityMessages = mutableMapOf<String, String>()

    /**
     * 分发事件 —— 会话状态机的主入口。
     *
     * 调用此方法后，[messages] 列表会相应更新，UI 自动响应。
     *
     * 处理顺序：
     *  1. 先让 [mapperChain] 尝试拦截事件（业务可完全自定义事件 → 卡片映射）
     *  2. Mapper 命中 → upsert 映射产生的消息，跳过默认处理
     *  3. Mapper 未命中 → 走默认状态机分支
     *
     * @param event AG-UI 事件对象
     */
    fun dispatch(event: AgUiEvent) {
        // 1) 先让自定义 mapper 拦截（业务优先）
        val mapped = mapperChain.apply(event, this)
        if (mapped != null) {
            upsertMessage(mapped)
            // 即使被 mapper 消费，仍执行一次生命周期同步（让 isRunning / currentStep 维持正确）
            updateLifecycleState(event)
            notifyLoadingChanged()
            return
        }

        // 2) 默认状态机
        when (event) {
            // ---- Lifecycle ----
            is AgUiEvent.RunStarted -> handleRunStarted(event)
            is AgUiEvent.RunFinished -> handleRunFinished(event)
            is AgUiEvent.RunError -> handleRunError(event)
            is AgUiEvent.StepStarted -> handleStepStarted(event)
            is AgUiEvent.StepFinished -> handleStepFinished(event)

            // ---- Text Message ----
            is AgUiEvent.TextMessageStart -> handleTextMessageStart(event)
            is AgUiEvent.TextMessageContent -> handleTextMessageContent(event)
            is AgUiEvent.TextMessageEnd -> handleTextMessageEnd(event)
            is AgUiEvent.TextMessageChunk -> handleTextMessageChunk(event)

            // ---- Tool Call ----
            is AgUiEvent.ToolCallStart -> handleToolCallStart(event)
            is AgUiEvent.ToolCallArgs -> handleToolCallArgs(event)
            is AgUiEvent.ToolCallEnd -> handleToolCallEnd(event)
            is AgUiEvent.ToolCallResult -> handleToolCallResult(event)
            is AgUiEvent.ToolCallChunk -> handleToolCallChunk(event)

            // ---- State ----
            is AgUiEvent.StateSnapshot -> handleStateSnapshot(event)
            is AgUiEvent.StateDelta -> handleStateDelta(event)
            is AgUiEvent.MessagesSnapshot -> handleMessagesSnapshot(event)

            // ---- Activity ----
            is AgUiEvent.ActivitySnapshot -> handleActivitySnapshot(event)
            is AgUiEvent.ActivityDelta -> handleActivityDelta(event)

            // ---- Reasoning ----
            is AgUiEvent.ReasoningMessageStart -> reasoningBuffers[event.messageId] = StringBuilder()
            is AgUiEvent.ReasoningMessageContent -> handleReasoningContent(event)
            is AgUiEvent.ReasoningMessageEnd -> handleReasoningEnd(event)
            is AgUiEvent.ReasoningStart -> { /* 可选：开启 Reasoning 侧通道，本实现随 MessageStart 一起处理 */ }
            is AgUiEvent.ReasoningEnd -> reasoningBuffers.remove(event.messageId)

            // ---- Special ----
            is AgUiEvent.Raw -> { /* 默认忽略；业务方可通过 RAW 走 unknown 路径 */ }
            is AgUiEvent.Custom -> handlerRegistry.getCustomHandler(event.name)?.handle(event, this)
            is AgUiEvent.Meta -> handlerRegistry.getMetaHandler(event.metaType)?.handle(event, this)
            is AgUiEvent.Unknown -> handlerRegistry.unknownEventHandler?.handle(event, this)
        }

        // 每次事件分发后同步 Loading 快照
        notifyLoadingChanged()
    }

    /**
     * 把生命周期字段更新从 [handleRunStarted] 等方法剥离出来，
     * 便于 Mapper 消费事件后仍然维持生命周期状态正确性。
     */
    private fun updateLifecycleState(event: AgUiEvent) {
        when (event) {
            is AgUiEvent.RunStarted -> {
                currentRunId = event.runId
                isRunning = true
            }
            is AgUiEvent.RunFinished, is AgUiEvent.RunError -> {
                isRunning = false
            }
            is AgUiEvent.StepStarted -> currentStep = event.stepName
            is AgUiEvent.StepFinished -> if (currentStep == event.stepName) currentStep = ""
            is AgUiEvent.ToolCallStart -> activeToolCalls[event.toolCallId] = event.toolCallName
            is AgUiEvent.ToolCallResult -> activeToolCalls.remove(event.toolCallId)
            else -> Unit
        }
    }

    /**
     * 批量分发事件。
     *
     * 适用于从 RunAgent 接口一次性获取历史事件序列的场景。
     */
    fun dispatchAll(events: Iterable<AgUiEvent>) {
        events.forEach(::dispatch)
    }

    /**
     * 插入或更新一条消息到 [messages] 列表。
     *
     * 若列表中已存在相同 ID 的消息，则原地替换（触发 ObservableList 通知）；
     * 否则追加到末尾。
     *
     * 公开方法：业务方与自定义事件处理器都可使用。
     */
    fun upsertMessage(message: ChatMessage) {
        val idx = messages.indexOfFirst { it.id == message.id }
        if (idx >= 0) {
            messages[idx] = message
        } else {
            messages.add(message)
        }
    }

    /** 删除指定 ID 的消息。 */
    fun removeMessage(messageId: String) {
        val idx = messages.indexOfFirst { it.id == messageId }
        if (idx >= 0) messages.removeAt(idx)
    }

    /** 获取当前 agent 状态的只读快照。 */
    fun getState(): Map<String, Any?> = stateMap.toMap()

    /** 重置所有状态（新建会话时调用）。 */
    fun reset() {
        messages.clear()
        streamingBuffers.clear()
        toolCallBuffers.clear()
        reasoningBuffers.clear()
        activityMessages.clear()
        stateMap.clear()
        activeToolCalls.clear()
        loadingMessageIds.clear()
        currentRunId = ""
        isRunning = false
        currentStep = ""
        lastError = ""
        notifyLoadingChanged()
    }

    // ============================
    // 生命周期处理
    // ============================

    private fun handleRunStarted(event: AgUiEvent.RunStarted) {
        currentRunId = event.runId
        isRunning = true
        lastError = ""
        onRunStarted?.invoke(event.runId)
    }

    private fun handleRunFinished(event: AgUiEvent.RunFinished) {
        isRunning = false
        activeToolCalls.clear()
        // 关闭所有未闭合的流式缓冲，确保消息状态为 SENT
        streamingBuffers.keys.toList().forEach { messageId ->
            finalizeTextMessage(messageId)
        }
        onRunFinished?.invoke(event.runId)
    }

    private fun handleRunError(event: AgUiEvent.RunError) {
        isRunning = false
        lastError = event.message
        activeToolCalls.clear()
        // 将所有未完成的流式消息标记为失败
        streamingBuffers.keys.toList().forEach { messageId ->
            val idx = messages.indexOfFirst { it.id == messageId }
            if (idx >= 0) {
                messages[idx] = messages[idx].copy(status = MessageStatus.FAILED)
            }
            streamingBuffers.remove(messageId)
        }
        onRunError?.invoke(event.message, event.code)
    }

    private fun handleStepStarted(event: AgUiEvent.StepStarted) {
        currentStep = event.stepName
        onStepStarted?.invoke(event.stepName)
    }

    private fun handleStepFinished(event: AgUiEvent.StepFinished) {
        onStepFinished?.invoke(event.stepName)
        if (currentStep == event.stepName) currentStep = ""
    }

    // ============================
    // 文本消息处理（流式）
    // ============================

    private fun handleTextMessageStart(event: AgUiEvent.TextMessageStart) {
        streamingBuffers[event.messageId] = StringBuilder()
        val initialMessage = buildStreamingMessage(
            messageId = event.messageId,
            role = event.role,
            content = "",
            status = MessageStatus.SENDING
        )
        upsertMessage(initialMessage)
    }

    private fun handleTextMessageContent(event: AgUiEvent.TextMessageContent) {
        val buffer = streamingBuffers.getOrPut(event.messageId) { StringBuilder() }
        buffer.append(event.delta)
        val idx = messages.indexOfFirst { it.id == event.messageId }
        if (idx >= 0) {
            messages[idx] = messages[idx].copy(content = buffer.toString())
        } else {
            // 兼容：后端漏发 START 时，此处自动补建
            upsertMessage(
                buildStreamingMessage(
                    messageId = event.messageId,
                    role = AgUiRole.ASSISTANT,
                    content = buffer.toString(),
                    status = MessageStatus.SENDING
                )
            )
        }
    }

    private fun handleTextMessageEnd(event: AgUiEvent.TextMessageEnd) {
        finalizeTextMessage(event.messageId)
    }

    private fun handleTextMessageChunk(event: AgUiEvent.TextMessageChunk) {
        // 首 chunk 带 messageId，后续 chunk 可省略（沿用上一个 messageId）
        val currentId = event.messageId.ifEmpty { lastChunkMessageId }
        if (currentId.isEmpty()) return
        if (event.messageId.isNotEmpty()) {
            // 切换或首次出现 messageId：可能需要闭合前一条
            if (lastChunkMessageId.isNotEmpty() && lastChunkMessageId != event.messageId) {
                finalizeTextMessage(lastChunkMessageId)
            }
            lastChunkMessageId = event.messageId
            if (!streamingBuffers.containsKey(event.messageId)) {
                streamingBuffers[event.messageId] = StringBuilder()
                upsertMessage(
                    buildStreamingMessage(
                        messageId = event.messageId,
                        role = event.role.ifEmpty { AgUiRole.ASSISTANT },
                        content = "",
                        status = MessageStatus.SENDING
                    )
                )
            }
        }
        if (event.delta.isNotEmpty()) {
            handleTextMessageContent(
                AgUiEvent.TextMessageContent(
                    messageId = currentId,
                    delta = event.delta,
                    timestamp = event.timestamp,
                    rawEvent = event.rawEvent
                )
            )
        }
    }

    /** Chunk 模式下记录的上一个 messageId，便于流式续传 */
    private var lastChunkMessageId: String = ""

    private fun finalizeTextMessage(messageId: String) {
        val buffer = streamingBuffers.remove(messageId) ?: return
        val idx = messages.indexOfFirst { it.id == messageId }
        if (idx >= 0) {
            messages[idx] = messages[idx].copy(
                content = buffer.toString(),
                status = MessageStatus.SENT
            )
        }
        if (lastChunkMessageId == messageId) lastChunkMessageId = ""
    }

    // ============================
    // 工具调用处理
    // ============================

    private fun handleToolCallStart(event: AgUiEvent.ToolCallStart) {
        toolCallBuffers[event.toolCallId] = ToolCallBuffer(
            toolCallId = event.toolCallId,
            toolCallName = event.toolCallName,
            parentMessageId = event.parentMessageId,
            argsBuffer = StringBuilder()
        )
        activeToolCalls[event.toolCallId] = event.toolCallName
        handlerRegistry.toolCallStartHandler?.handle(
            event.toolCallId,
            event.toolCallName,
            event.parentMessageId,
            this
        )
    }

    private fun handleToolCallArgs(event: AgUiEvent.ToolCallArgs) {
        toolCallBuffers[event.toolCallId]?.argsBuffer?.append(event.delta)
    }

    private fun handleToolCallEnd(event: AgUiEvent.ToolCallEnd) {
        // 工具调用结束仅意味着参数组装完毕；结果由 TOOL_CALL_RESULT / ToolMessage 提供
        // 保留缓冲，等待 RESULT 事件配对
    }

    private fun handleToolCallResult(event: AgUiEvent.ToolCallResult) {
        val buffer = toolCallBuffers.remove(event.toolCallId)
        activeToolCalls.remove(event.toolCallId)
        val toolName = buffer?.toolCallName ?: ""
        val custom = handlerRegistry.toolResultHandler?.handle(
            toolCallId = event.toolCallId,
            toolCallName = toolName,
            content = event.content,
            session = this
        )
        if (custom != null) {
            upsertMessage(custom)
            return
        }
        // 默认：以 SYSTEM 类型消息展示工具结果
        val defaultMessage = ChatMessageHelper.createSystemMessage(
            content = if (toolName.isNotEmpty()) "🔧 $toolName: ${event.content}" else event.content
        ).copy(
            id = event.messageId.ifEmpty { event.toolCallId },
            extra = mapOf(
                "agUiKind" to "tool_result",
                "toolCallId" to event.toolCallId,
                "toolCallName" to toolName
            )
        )
        upsertMessage(defaultMessage)
    }

    private fun handleToolCallChunk(event: AgUiEvent.ToolCallChunk) {
        // 首 chunk 必须带 id+name，补发 START
        if (event.toolCallId.isNotEmpty() && !toolCallBuffers.containsKey(event.toolCallId)) {
            handleToolCallStart(
                AgUiEvent.ToolCallStart(
                    toolCallId = event.toolCallId,
                    toolCallName = event.toolCallName,
                    parentMessageId = event.parentMessageId,
                    timestamp = event.timestamp,
                    rawEvent = event.rawEvent
                )
            )
        }
        val targetId = event.toolCallId.ifEmpty { toolCallBuffers.keys.lastOrNull().orEmpty() }
        if (targetId.isNotEmpty() && event.delta.isNotEmpty()) {
            toolCallBuffers[targetId]?.argsBuffer?.append(event.delta)
        }
    }

    // ============================
    // 状态管理
    // ============================

    private fun handleStateSnapshot(event: AgUiEvent.StateSnapshot) {
        stateMap.clear()
        stateMap.putAll(event.snapshot)
        handlerRegistry.stateChangeHandler?.handle(getState(), isDelta = false, session = this)
    }

    private fun handleStateDelta(event: AgUiEvent.StateDelta) {
        // JSON Patch (RFC 6902) 应用 —— 实现简化版 add/replace/remove
        // 业务方如需完整 RFC 6902 支持可覆盖 stateChangeHandler
        event.delta.forEach { op ->
            val path = op["path"]?.toString().orEmpty()
            val key = path.removePrefix("/").substringBefore('/')
            if (key.isEmpty()) return@forEach
            when (op["op"]?.toString()) {
                "add", "replace" -> stateMap[key] = op["value"]
                "remove" -> stateMap.remove(key)
            }
        }
        handlerRegistry.stateChangeHandler?.handle(getState(), isDelta = true, session = this)
    }

    private fun handleMessagesSnapshot(event: AgUiEvent.MessagesSnapshot) {
        // 覆盖式快照：清空列表后重新构建
        messages.clear()
        event.messages.forEach { msg ->
            val chatMessage = convertAgUiMessage(msg)
            if (chatMessage != null) messages.add(chatMessage)
        }
    }

    // ============================
    // Activity 事件
    // ============================

    private fun handleActivitySnapshot(event: AgUiEvent.ActivitySnapshot) {
        if (!event.replace && activityMessages.containsKey(event.messageId)) return
        activityMessages[event.messageId] = event.activityType

        // 优先调用注册的 Handler
        val handler = handlerRegistry.getActivityHandler(event.activityType)
        if (handler != null) {
            handler.handle(event, this)
            return
        }

        // 默认行为：渲染为 CUSTOM 类型消息，通过 extra 携带 activity 元数据
        val extra = mutableMapOf(
            "agUiKind" to "activity",
            "activityType" to event.activityType
        )
        event.content.forEach { (k, v) -> extra["activity.$k"] = v?.toString().orEmpty() }

        val summary = extractActivitySummary(event.content, event.activityType)
        val message = ChatMessageHelper.createCustomMessage(
            content = summary,
            isSelf = false,
            senderName = aiSenderName,
            senderAvatar = aiAvatarUrl,
            senderId = "ai",
            extra = extra
        ).copy(id = event.messageId)
        upsertMessage(message)
    }

    private fun handleActivityDelta(event: AgUiEvent.ActivityDelta) {
        val handler = handlerRegistry.getActivityHandler(event.activityType)
        if (handler != null) {
            handler.handle(event, this)
            return
        }
        // 默认行为：将 patch 信息追加到 extra，便于调试（完整 JSON Patch 实现交给业务方）
        val idx = messages.indexOfFirst { it.id == event.messageId }
        if (idx >= 0) {
            val current = messages[idx]
            val newExtra = current.extra.toMutableMap()
            event.patch.forEachIndexed { index, op ->
                newExtra["activity.patch.$index"] = op.toString()
            }
            messages[idx] = current.copy(extra = newExtra)
        }
    }

    private fun extractActivitySummary(content: Map<String, Any?>, activityType: String): String {
        // 尝试从常见字段提取简介；业务方可通过 handler 自定义
        val title = content["title"]?.toString()
            ?: content["name"]?.toString()
            ?: content["summary"]?.toString()
            ?: ""
        return if (title.isNotEmpty()) "[$activityType] $title" else "[$activityType]"
    }

    // ============================
    // Reasoning 推理消息
    // ============================

    private fun handleReasoningContent(event: AgUiEvent.ReasoningMessageContent) {
        val buffer = reasoningBuffers.getOrPut(event.messageId) { StringBuilder() }
        buffer.append(event.delta)
        val idx = messages.indexOfFirst { it.id == event.messageId }
        if (idx >= 0) {
            messages[idx] = messages[idx].copy(content = buffer.toString())
        } else {
            // 首次出现：以 SYSTEM 消息形式渲染（灰色气泡），业务方可通过 extra="agUiKind"=="reasoning" 自定义
            upsertMessage(
                ChatMessageHelper.createSystemMessage(content = buffer.toString()).copy(
                    id = event.messageId,
                    extra = mapOf("agUiKind" to "reasoning")
                )
            )
        }
    }

    private fun handleReasoningEnd(event: AgUiEvent.ReasoningMessageEnd) {
        val buffer = reasoningBuffers.remove(event.messageId) ?: return
        val idx = messages.indexOfFirst { it.id == event.messageId }
        if (idx >= 0) {
            messages[idx] = messages[idx].copy(content = buffer.toString())
        }
    }

    // ============================
    // AG-UI 消息 → ChatMessage 转换
    // ============================

    private fun convertAgUiMessage(msg: AgUiMessage): ChatMessage? {
        val isSelf = msg.role == AgUiRole.USER
        val senderName = when (msg.role) {
            AgUiRole.USER -> msg.name.ifEmpty { "我" }
            AgUiRole.ASSISTANT -> msg.name.ifEmpty { aiSenderName }
            AgUiRole.SYSTEM -> ""
            AgUiRole.TOOL -> "工具"
            else -> msg.name
        }
        val senderAvatar = if (msg.role == AgUiRole.ASSISTANT) aiAvatarUrl else ""
        val senderId = if (isSelf) selfSenderId else "ai"

        return when (msg.role) {
            AgUiRole.USER, AgUiRole.ASSISTANT, AgUiRole.DEVELOPER -> ChatMessage(
                id = msg.id,
                content = msg.content,
                isSelf = isSelf,
                type = MessageType.TEXT,
                status = MessageStatus.SENT,
                senderName = senderName,
                senderAvatar = senderAvatar,
                senderId = senderId,
                extra = buildExtrasFromAgUi(msg)
            )
            AgUiRole.SYSTEM -> ChatMessageHelper.createSystemMessage(msg.content).copy(id = msg.id)
            AgUiRole.TOOL -> ChatMessageHelper.createSystemMessage(
                content = "🔧 tool → ${msg.content}"
            ).copy(
                id = msg.id,
                extra = mapOf(
                    "agUiKind" to "tool_result",
                    "toolCallId" to msg.toolCallId
                )
            )
            AgUiRole.ACTIVITY -> ChatMessageHelper.createCustomMessage(
                content = extractActivitySummary(msg.activityContent, msg.activityType),
                isSelf = false,
                senderName = aiSenderName,
                senderAvatar = aiAvatarUrl,
                senderId = "ai",
                extra = mapOf(
                    "agUiKind" to "activity",
                    "activityType" to msg.activityType
                )
            ).copy(id = msg.id)
            AgUiRole.REASONING -> ChatMessageHelper.createSystemMessage(content = msg.content).copy(
                id = msg.id,
                extra = mapOf("agUiKind" to "reasoning")
            )
            else -> null
        }
    }

    private fun buildExtrasFromAgUi(msg: AgUiMessage): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (msg.toolCalls.isNotEmpty()) {
            map["agUiKind"] = "tool_invocation"
            msg.toolCalls.forEachIndexed { i, tc ->
                map["tool.$i.id"] = tc.id
                map["tool.$i.name"] = tc.function.name
                map["tool.$i.args"] = tc.function.arguments
            }
        }
        return map
    }

    /**
     * 构建一条流式（TEXT_MESSAGE_START/CHUNK）时的占位消息。
     *
     * 根据 role 自动区分 user / assistant / system，设置合适的发送者字段。
     */
    private fun buildStreamingMessage(
        messageId: String,
        role: String,
        content: String,
        status: MessageStatus
    ): ChatMessage {
        val isSelf = role == AgUiRole.USER
        val senderName = when (role) {
            AgUiRole.USER -> "我"
            AgUiRole.ASSISTANT -> aiSenderName
            AgUiRole.SYSTEM -> ""
            else -> aiSenderName
        }
        val senderAvatar = if (role == AgUiRole.ASSISTANT) aiAvatarUrl else ""
        val senderId = if (isSelf) selfSenderId else "ai"
        val type = if (role == AgUiRole.SYSTEM) MessageType.SYSTEM else MessageType.TEXT
        return ChatMessage(
            id = messageId,
            content = content,
            isSelf = isSelf,
            type = type,
            status = status,
            senderName = senderName,
            senderAvatar = senderAvatar,
            senderId = senderId,
            extra = mapOf(
                "agUiKind" to "streaming",
                "agUiRole" to role
            )
        )
    }

    /** 工具调用缓冲 */
    private data class ToolCallBuffer(
        val toolCallId: String,
        val toolCallName: String,
        val parentMessageId: String,
        val argsBuffer: StringBuilder
    )

    companion object {
        /**
         * 为测试场景创建 AgUiSession。
         *
         * 使用普通 `mutableListOf<ChatMessage>()` 作为消息存储，**不依赖** Kuikly 的 Pager / Observer 上下文，
         * 可在任意 JVM / Native 环境下直接运行。
         *
         * ⚠️ 该方法返回的 session 的 [messages] 不具备 UI 响应式能力，**仅用于单元测试**。
         */
        fun forTesting(
            threadId: String = "",
            selfSenderId: String = "self",
            aiSenderName: String = "AI 助手",
            aiAvatarUrl: String = "",
            handlerRegistry: AgUiHandlerRegistry = AgUiHandlerRegistry()
        ): AgUiSession = AgUiSession(
            messages = mutableListOf(),
            threadId = threadId,
            selfSenderId = selfSenderId,
            aiSenderName = aiSenderName,
            aiAvatarUrl = aiAvatarUrl,
            handlerRegistry = handlerRegistry
        )
    }
}

/**
 * 便捷扩展：直接从 JSON 字符串分发事件。
 *
 * 使用场景：SSE 收到 `data: {...}` 一行后直接调用。
 */
fun AgUiSession.dispatchJson(jsonString: String): AgUiEvent? {
    val event = AgUiEventParser.parseEvent(jsonString) ?: return null
    dispatch(event)
    return event
}

/**
 * 便捷扩展：直接分发 SSE `data:` 行内容。
 *
 * 自动跳过空行与 `[DONE]` 标记。
 */
fun AgUiSession.dispatchSseData(dataLine: String): AgUiEvent? {
    val event = AgUiEventParser.parseSseData(dataLine) ?: return null
    dispatch(event)
    return event
}
