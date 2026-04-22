package com.tencent.kuiklybase.chat.agui

/**
 * AG-UI 协议 — 事件类型枚举
 *
 * 与 AG-UI 官方 EventType 严格对齐，覆盖 16 种标准事件以及 Activity / Reasoning 等扩展事件。
 * 详见：https://docs.ag-ui.com/concepts/events
 */
object AgUiEventType {
    // Lifecycle（生命周期）
    const val RUN_STARTED = "RUN_STARTED"
    const val RUN_FINISHED = "RUN_FINISHED"
    const val RUN_ERROR = "RUN_ERROR"
    const val STEP_STARTED = "STEP_STARTED"
    const val STEP_FINISHED = "STEP_FINISHED"

    // Text Message（文本消息）
    const val TEXT_MESSAGE_START = "TEXT_MESSAGE_START"
    const val TEXT_MESSAGE_CONTENT = "TEXT_MESSAGE_CONTENT"
    const val TEXT_MESSAGE_END = "TEXT_MESSAGE_END"
    const val TEXT_MESSAGE_CHUNK = "TEXT_MESSAGE_CHUNK"

    // Tool Call（工具调用）
    const val TOOL_CALL_START = "TOOL_CALL_START"
    const val TOOL_CALL_ARGS = "TOOL_CALL_ARGS"
    const val TOOL_CALL_END = "TOOL_CALL_END"
    const val TOOL_CALL_RESULT = "TOOL_CALL_RESULT"
    const val TOOL_CALL_CHUNK = "TOOL_CALL_CHUNK"

    // State Management（状态管理）
    const val STATE_SNAPSHOT = "STATE_SNAPSHOT"
    const val STATE_DELTA = "STATE_DELTA"
    const val MESSAGES_SNAPSHOT = "MESSAGES_SNAPSHOT"

    // Activity（活动）
    const val ACTIVITY_SNAPSHOT = "ACTIVITY_SNAPSHOT"
    const val ACTIVITY_DELTA = "ACTIVITY_DELTA"

    // Reasoning（推理）
    const val REASONING_START = "REASONING_START"
    const val REASONING_MESSAGE_START = "REASONING_MESSAGE_START"
    const val REASONING_MESSAGE_CONTENT = "REASONING_MESSAGE_CONTENT"
    const val REASONING_MESSAGE_END = "REASONING_MESSAGE_END"
    const val REASONING_MESSAGE_CHUNK = "REASONING_MESSAGE_CHUNK"
    const val REASONING_END = "REASONING_END"
    const val REASONING_ENCRYPTED_VALUE = "REASONING_ENCRYPTED_VALUE"

    // Special（特殊事件）
    const val RAW = "RAW"
    const val CUSTOM = "CUSTOM"
    const val META = "META"
}

/**
 * AG-UI 协议事件基类（密封类）
 *
 * 对应官方 BaseEvent + 各派生类。所有事件共享 [type]、[timestamp]、[rawEvent] 三个基础字段。
 *
 * 使用密封类让消费方通过 `when` 表达式强制处理所有类型，保证协议完整性。
 */
sealed class AgUiEvent {
    abstract val type: String
    abstract val timestamp: Long
    /** 原始 JSON 字符串，便于业务方透传或调试 */
    abstract val rawEvent: String

    // ---- Lifecycle Events ----

    data class RunStarted(
        val threadId: String,
        val runId: String,
        val parentRunId: String = "",
        override val timestamp: Long = 0L,
        override val rawEvent: String = ""
    ) : AgUiEvent() {
        override val type: String get() = AgUiEventType.RUN_STARTED
    }

    data class RunFinished(
        val threadId: String,
        val runId: String,
        val result: Map<String, Any?> = emptyMap(),
        override val timestamp: Long = 0L,
        override val rawEvent: String = ""
    ) : AgUiEvent() {
        override val type: String get() = AgUiEventType.RUN_FINISHED
    }

    data class RunError(
        val message: String,
        val code: String = "",
        override val timestamp: Long = 0L,
        override val rawEvent: String = ""
    ) : AgUiEvent() {
        override val type: String get() = AgUiEventType.RUN_ERROR
    }

    data class StepStarted(
        val stepName: String,
        override val timestamp: Long = 0L,
        override val rawEvent: String = ""
    ) : AgUiEvent() {
        override val type: String get() = AgUiEventType.STEP_STARTED
    }

    data class StepFinished(
        val stepName: String,
        override val timestamp: Long = 0L,
        override val rawEvent: String = ""
    ) : AgUiEvent() {
        override val type: String get() = AgUiEventType.STEP_FINISHED
    }

    // ---- Text Message Events ----

    data class TextMessageStart(
        val messageId: String,
        val role: String = AgUiRole.ASSISTANT,
        override val timestamp: Long = 0L,
        override val rawEvent: String = ""
    ) : AgUiEvent() {
        override val type: String get() = AgUiEventType.TEXT_MESSAGE_START
    }

    data class TextMessageContent(
        val messageId: String,
        val delta: String,
        override val timestamp: Long = 0L,
        override val rawEvent: String = ""
    ) : AgUiEvent() {
        override val type: String get() = AgUiEventType.TEXT_MESSAGE_CONTENT
    }

    data class TextMessageEnd(
        val messageId: String,
        override val timestamp: Long = 0L,
        override val rawEvent: String = ""
    ) : AgUiEvent() {
        override val type: String get() = AgUiEventType.TEXT_MESSAGE_END
    }

    /** 便捷事件：首 chunk 必须带 messageId；切换 messageId 或流结束自动闭合。 */
    data class TextMessageChunk(
        val messageId: String = "",
        val role: String = "",
        val delta: String = "",
        override val timestamp: Long = 0L,
        override val rawEvent: String = ""
    ) : AgUiEvent() {
        override val type: String get() = AgUiEventType.TEXT_MESSAGE_CHUNK
    }

    // ---- Tool Call Events ----

    data class ToolCallStart(
        val toolCallId: String,
        val toolCallName: String,
        val parentMessageId: String = "",
        override val timestamp: Long = 0L,
        override val rawEvent: String = ""
    ) : AgUiEvent() {
        override val type: String get() = AgUiEventType.TOOL_CALL_START
    }

    data class ToolCallArgs(
        val toolCallId: String,
        val delta: String,
        override val timestamp: Long = 0L,
        override val rawEvent: String = ""
    ) : AgUiEvent() {
        override val type: String get() = AgUiEventType.TOOL_CALL_ARGS
    }

    data class ToolCallEnd(
        val toolCallId: String,
        override val timestamp: Long = 0L,
        override val rawEvent: String = ""
    ) : AgUiEvent() {
        override val type: String get() = AgUiEventType.TOOL_CALL_END
    }

    data class ToolCallResult(
        val messageId: String,
        val toolCallId: String,
        val content: String,
        val role: String = AgUiRole.TOOL,
        override val timestamp: Long = 0L,
        override val rawEvent: String = ""
    ) : AgUiEvent() {
        override val type: String get() = AgUiEventType.TOOL_CALL_RESULT
    }

    /** 便捷事件：首 chunk 必须带 toolCallId 与 toolCallName。 */
    data class ToolCallChunk(
        val toolCallId: String = "",
        val toolCallName: String = "",
        val parentMessageId: String = "",
        val delta: String = "",
        override val timestamp: Long = 0L,
        override val rawEvent: String = ""
    ) : AgUiEvent() {
        override val type: String get() = AgUiEventType.TOOL_CALL_CHUNK
    }

    // ---- State Management Events ----

    data class StateSnapshot(
        val snapshot: Map<String, Any?>,
        override val timestamp: Long = 0L,
        override val rawEvent: String = ""
    ) : AgUiEvent() {
        override val type: String get() = AgUiEventType.STATE_SNAPSHOT
    }

    /** RFC 6902 JSON Patch 操作数组（按原始顺序透传） */
    data class StateDelta(
        val delta: List<Map<String, Any?>>,
        override val timestamp: Long = 0L,
        override val rawEvent: String = ""
    ) : AgUiEvent() {
        override val type: String get() = AgUiEventType.STATE_DELTA
    }

    data class MessagesSnapshot(
        val messages: List<AgUiMessage>,
        override val timestamp: Long = 0L,
        override val rawEvent: String = ""
    ) : AgUiEvent() {
        override val type: String get() = AgUiEventType.MESSAGES_SNAPSHOT
    }

    // ---- Activity Events ----

    data class ActivitySnapshot(
        val messageId: String,
        val activityType: String,
        val content: Map<String, Any?>,
        val replace: Boolean = true,
        override val timestamp: Long = 0L,
        override val rawEvent: String = ""
    ) : AgUiEvent() {
        override val type: String get() = AgUiEventType.ACTIVITY_SNAPSHOT
    }

    data class ActivityDelta(
        val messageId: String,
        val activityType: String,
        val patch: List<Map<String, Any?>>,
        override val timestamp: Long = 0L,
        override val rawEvent: String = ""
    ) : AgUiEvent() {
        override val type: String get() = AgUiEventType.ACTIVITY_DELTA
    }

    // ---- Reasoning Events ----

    data class ReasoningStart(
        val messageId: String,
        override val timestamp: Long = 0L,
        override val rawEvent: String = ""
    ) : AgUiEvent() {
        override val type: String get() = AgUiEventType.REASONING_START
    }

    data class ReasoningMessageStart(
        val messageId: String,
        val role: String = AgUiRole.ASSISTANT,
        override val timestamp: Long = 0L,
        override val rawEvent: String = ""
    ) : AgUiEvent() {
        override val type: String get() = AgUiEventType.REASONING_MESSAGE_START
    }

    data class ReasoningMessageContent(
        val messageId: String,
        val delta: String,
        override val timestamp: Long = 0L,
        override val rawEvent: String = ""
    ) : AgUiEvent() {
        override val type: String get() = AgUiEventType.REASONING_MESSAGE_CONTENT
    }

    data class ReasoningMessageEnd(
        val messageId: String,
        override val timestamp: Long = 0L,
        override val rawEvent: String = ""
    ) : AgUiEvent() {
        override val type: String get() = AgUiEventType.REASONING_MESSAGE_END
    }

    data class ReasoningEnd(
        val messageId: String,
        override val timestamp: Long = 0L,
        override val rawEvent: String = ""
    ) : AgUiEvent() {
        override val type: String get() = AgUiEventType.REASONING_END
    }

    // ---- Special Events ----

    /** 透传原始事件（多见于兼容异构 Agent 框架的场景） */
    data class Raw(
        val event: Map<String, Any?>,
        val source: String = "",
        override val timestamp: Long = 0L,
        override val rawEvent: String = ""
    ) : AgUiEvent() {
        override val type: String get() = AgUiEventType.RAW
    }

    /**
     * 业务自定义事件（AG-UI 官方保留扩展点）
     *
     * 通过 [AgUiHandlerRegistry.registerCustomHandler] 注册处理器后，
     * 可在 AgUiSession 中被业务逻辑消费。
     *
     * @param name 自定义事件名
     * @param value 负载（通常为 JSON）
     */
    data class Custom(
        val name: String,
        val value: Any? = null,
        override val timestamp: Long = 0L,
        override val rawEvent: String = ""
    ) : AgUiEvent() {
        override val type: String get() = AgUiEventType.CUSTOM
    }

    /**
     * Meta 事件（AG-UI 草案协议 —— 运行无关的侧通道注解事件）
     *
     * 如 thumbs_up / thumbs_down / tag 等用户反馈。
     */
    data class Meta(
        val metaType: String,
        val payload: Map<String, Any?>,
        override val timestamp: Long = 0L,
        override val rawEvent: String = ""
    ) : AgUiEvent() {
        override val type: String get() = AgUiEventType.META
    }

    /**
     * 未知类型事件（协议升级容错）
     *
     * 解析器遇到当前版本不识别的事件类型时落入此分支，业务方可选择忽略或透传打日志。
     */
    data class Unknown(
        override val type: String,
        val body: Map<String, Any?>,
        override val timestamp: Long = 0L,
        override val rawEvent: String = ""
    ) : AgUiEvent()
}
