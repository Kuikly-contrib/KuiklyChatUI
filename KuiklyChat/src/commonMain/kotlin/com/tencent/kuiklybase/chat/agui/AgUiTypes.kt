package com.tencent.kuiklybase.chat.agui

/**
 * AG-UI 协议 — 类型定义
 *
 * 严格对齐 AG-UI 官方协议规范（https://docs.ag-ui.com/），
 * 并保留对自定义扩展（Activity / Custom / Meta）的开放能力。
 *
 * 设计原则：
 * - **协议中立**：仅定义协议数据结构，不耦合任何 UI 组件；
 * - **传输无关**：不包含 SSE / WebSocket / HTTP 传输逻辑（由业务方适配）；
 * - **向后兼容**：未知字段统一落入 rawEvent 透传，支持渐进升级。
 *
 * 协议对应关系：
 * | 概念         | AG-UI 官方                       | KuiklyChat 映射                           |
 * |-------------|----------------------------------|-------------------------------------------|
 * | 消息角色      | "user" / "assistant" / "system"  | [AgUiRole]                                |
 * | 消息         | BaseMessage + 派生类              | [AgUiMessage]                             |
 * | 工具调用      | ToolCall                         | [AgUiToolCall]                            |
 * | 事件         | BaseEvent + 16 种派生类           | [AgUiEvent]                               |
 */

/**
 * 消息发送者角色
 *
 * 对应 AG-UI 官方 BaseMessage.role 字段可选值。
 */
object AgUiRole {
    const val USER = "user"
    const val ASSISTANT = "assistant"
    const val SYSTEM = "system"
    const val TOOL = "tool"
    const val DEVELOPER = "developer"
    const val ACTIVITY = "activity"
    const val REASONING = "reasoning"
}

/**
 * 工具调用函数定义
 *
 * 对应 AG-UI 官方 ToolCall.function 字段。
 *
 * @param name 函数名
 * @param arguments JSON 编码的参数字符串（流式拼接的结果）
 */
data class AgUiToolFunction(
    val name: String,
    val arguments: String = ""
)

/**
 * 工具调用
 *
 * 对应 AG-UI 官方 ToolCall 接口。Assistant 消息可携带一个或多个工具调用。
 *
 * @param id 工具调用唯一 ID
 * @param type 工具类型，目前仅 "function"
 * @param function 函数调用详情
 */
data class AgUiToolCall(
    val id: String,
    val type: String = "function",
    val function: AgUiToolFunction
)

/**
 * AG-UI 协议原生消息
 *
 * 对应 AG-UI 官方 BaseMessage + 各派生类的并集，通过 role 字段区分具体类型。
 *
 * 这是 **协议层** 的消息模型，区别于 UI 层的 [com.tencent.kuiklybase.chat.model.ChatMessage]。
 * 业务方可使用 [AgUiMessageConverter] 在两层之间转换。
 *
 * @param id 消息唯一 ID
 * @param role 发送者角色
 * @param content 文本内容（assistant 使用工具调用时可为空）
 * @param name 可选名称标识
 * @param toolCalls assistant 消息的工具调用列表
 * @param toolCallId tool 消息响应的原始调用 ID
 * @param activityType activity 消息的活动类型（如 "PLAN"、"SEARCH"）
 * @param activityContent activity 消息的结构化内容（JSON Map）
 * @param extras 其他扩展字段（透传原始 JSON）
 */
data class AgUiMessage(
    val id: String,
    val role: String,
    val content: String = "",
    val name: String = "",
    val toolCalls: List<AgUiToolCall> = emptyList(),
    val toolCallId: String = "",
    val activityType: String = "",
    val activityContent: Map<String, Any?> = emptyMap(),
    val extras: Map<String, Any?> = emptyMap()
)

/**
 * AG-UI 运行上下文
 *
 * 对应 AG-UI 官方 Context 条目 —— 提供给 agent 的上下文信息。
 */
data class AgUiContext(
    val description: String,
    val value: String
)

/**
 * AG-UI 工具定义
 *
 * 对应 AG-UI 官方 Tool —— 告诉 agent 它可以调用哪些函数。
 *
 * @param name 工具名称（唯一）
 * @param description 工具描述（给 LLM 阅读）
 * @param parameters JSON Schema 参数定义
 */
data class AgUiTool(
    val name: String,
    val description: String,
    val parameters: Map<String, Any?> = emptyMap()
)

/**
 * RunAgentInput — 一次 agent 运行的输入
 *
 * 对应 AG-UI 官方 RunAgentInput。业务方构建此对象并转为 JSON 发送给后端。
 *
 * @param threadId 会话线程 ID
 * @param runId 当前运行 ID
 * @param parentRunId 父级运行 ID（分支/时间旅行场景）
 * @param state 当前 agent 状态（透传）
 * @param messages 历史消息
 * @param tools 可用工具列表
 * @param context 上下文
 * @param forwardedProps 额外透传属性
 */
data class AgUiRunAgentInput(
    val threadId: String,
    val runId: String,
    val parentRunId: String = "",
    val state: Map<String, Any?> = emptyMap(),
    val messages: List<AgUiMessage> = emptyList(),
    val tools: List<AgUiTool> = emptyList(),
    val context: List<AgUiContext> = emptyList(),
    val forwardedProps: Map<String, Any?> = emptyMap()
)
