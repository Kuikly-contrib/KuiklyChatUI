package com.tencent.kuiklybase.chat.agui

import com.tencent.kuiklybase.chat.model.ChatMessage

/**
 * AG-UI 自定义事件扩展点注册表
 *
 * AG-UI 协议官方提供了三类扩展事件：
 * - **Activity Events**（ACTIVITY_SNAPSHOT / ACTIVITY_DELTA）：结构化任务进度展示（计划、搜索等）
 * - **Custom Events**（CUSTOM）：应用私有事件（name + value）
 * - **Meta Events**（META）：侧通道注解（thumbs_up / tag / note 等）
 *
 * 这三类事件的语义由业务方自行定义，故本注册表提供扩展点。
 * 业务方通过注册处理器来响应自定义事件。
 *
 * 使用示例：
 * ```
 * val registry = AgUiHandlerRegistry()
 *
 * // 注册 Activity 处理器：将 PLAN 类型活动渲染为自定义消息卡片
 * registry.registerActivityHandler("PLAN") { event, session ->
 *     val message = ChatMessageHelper.createCustomMessage(
 *         content = event.content["title"]?.toString().orEmpty(),
 *         isSelf = false,
 *         extra = mapOf("activityType" to "PLAN", "state" to (event.content["state"]?.toString() ?: ""))
 *     )
 *     session.upsertMessage(message.copy(id = event.messageId))
 * }
 *
 * // 注册 Custom 事件：处理语音消息
 * registry.registerCustomHandler("voice_message") { event, session ->
 *     // 业务处理
 * }
 *
 * // 注册工具调用完成处理器：将工具结果作为消息展示
 * registry.registerToolResultHandler { event, session ->
 *     // 业务处理
 * }
 * ```
 */
class AgUiHandlerRegistry {

    /** Activity 事件处理器：按 activityType 分发 */
    private val activityHandlers = mutableMapOf<String, AgUiActivityHandler>()

    /** Custom 事件处理器：按 name 分发 */
    private val customHandlers = mutableMapOf<String, AgUiCustomHandler>()

    /** Meta 事件处理器：按 metaType 分发 */
    private val metaHandlers = mutableMapOf<String, AgUiMetaHandler>()

    /** 全局工具结果处理器（可选） */
    var toolResultHandler: AgUiToolResultHandler? = null

    /** 全局工具调用开始处理器（可选） */
    var toolCallStartHandler: AgUiToolCallStartHandler? = null

    /** 全局状态变更处理器（可选） */
    var stateChangeHandler: AgUiStateChangeHandler? = null

    /** 全局未知事件兜底处理器（便于调试/日志） */
    var unknownEventHandler: AgUiUnknownEventHandler? = null

    /**
     * 注册 Activity 事件处理器（按 activityType 分发）。
     *
     * @param activityType 活动类型，如 "PLAN"、"SEARCH"
     * @param handler 处理器
     */
    fun registerActivityHandler(activityType: String, handler: AgUiActivityHandler) {
        activityHandlers[activityType] = handler
    }

    /** 注册 Custom 事件处理器（按 name 分发）。 */
    fun registerCustomHandler(name: String, handler: AgUiCustomHandler) {
        customHandlers[name] = handler
    }

    /** 注册 Meta 事件处理器（按 metaType 分发）。 */
    fun registerMetaHandler(metaType: String, handler: AgUiMetaHandler) {
        metaHandlers[metaType] = handler
    }

    internal fun getActivityHandler(activityType: String): AgUiActivityHandler? =
        activityHandlers[activityType]

    internal fun getCustomHandler(name: String): AgUiCustomHandler? =
        customHandlers[name]

    internal fun getMetaHandler(metaType: String): AgUiMetaHandler? =
        metaHandlers[metaType]
}

/**
 * Activity 事件处理器函数签名
 *
 * 同一个 activityType 会收到 SNAPSHOT 和 DELTA 两种事件。
 * 处理器需自行区分并更新对应的 UI 消息。
 */
fun interface AgUiActivityHandler {
    /**
     * @param event 原始事件（SNAPSHOT 或 DELTA）
     * @param session 会话对象，可通过 [AgUiSession.upsertMessage] 注入/更新消息
     */
    fun handle(event: AgUiEvent, session: AgUiSession)
}

/** Custom 事件处理器 */
fun interface AgUiCustomHandler {
    fun handle(event: AgUiEvent.Custom, session: AgUiSession)
}

/** Meta 事件处理器 */
fun interface AgUiMetaHandler {
    fun handle(event: AgUiEvent.Meta, session: AgUiSession)
}

/**
 * 工具调用开始处理器
 *
 * 工具调用场景下，业务方可自定义如何将工具调用展示给用户（如折叠卡片、临时提示条等）。
 */
fun interface AgUiToolCallStartHandler {
    fun handle(
        toolCallId: String,
        toolCallName: String,
        parentMessageId: String,
        session: AgUiSession
    )
}

/**
 * 工具调用结果处理器
 *
 * 当收到 TOOL_CALL_RESULT 或 ToolMessage 时触发。
 * 默认行为：将结果添加为 SYSTEM 类型消息；业务方可自定义以渲染结构化卡片。
 */
fun interface AgUiToolResultHandler {
    /**
     * @return 若返回非 null，[AgUiSession] 会将其插入 [messageList]；返回 null 表示业务方已自行处理。
     */
    fun handle(
        toolCallId: String,
        toolCallName: String,
        content: String,
        session: AgUiSession
    ): ChatMessage?
}

/** 状态变更处理器（STATE_SNAPSHOT / STATE_DELTA） */
fun interface AgUiStateChangeHandler {
    fun handle(state: Map<String, Any?>, isDelta: Boolean, session: AgUiSession)
}

/** 未知事件处理器（调试用） */
fun interface AgUiUnknownEventHandler {
    fun handle(event: AgUiEvent.Unknown, session: AgUiSession)
}
