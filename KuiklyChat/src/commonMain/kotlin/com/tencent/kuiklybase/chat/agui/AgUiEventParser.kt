package com.tencent.kuiklybase.chat.agui

import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * AG-UI 事件 JSON 解析器
 *
 * 将 AG-UI 协议的 JSON 事件（或 SSE `data:` 字段内容）解析为 [AgUiEvent] 对象。
 *
 * 使用 Kuikly 内置的 JSON 序列化 API（[JSONObject] / [JSONArray]），
 * 不依赖任何第三方 JSON 库，可直接在 commonMain 中使用。
 *
 * 容错策略：
 * - 未知事件类型 → [AgUiEvent.Unknown]（透传 body，便于调试）
 * - 解析失败 → 返回 null
 * - 关键字段缺失 → 使用默认值（如空字符串）
 *
 * 使用示例：
 * ```
 * val event = AgUiEventParser.parseEvent("""{"type":"RUN_STARTED","threadId":"t1","runId":"r1"}""")
 * when (event) {
 *     is AgUiEvent.RunStarted -> println("Run started")
 *     else -> { /* ... */ }
 * }
 * ```
 */
object AgUiEventParser {

    /**
     * 从 JSON 字符串解析单个事件。
     *
     * @param jsonString AG-UI 事件 JSON
     * @return 事件对象，解析失败返回 null
     */
    fun parseEvent(jsonString: String): AgUiEvent? {
        if (jsonString.isBlank()) return null
        return runCatching {
            val json = JSONObject(jsonString)
            parseEvent(json, jsonString)
        }.getOrNull()
    }

    /**
     * 从 [JSONObject] 解析单个事件。
     *
     * @param json 事件 JSONObject
     * @param rawJson 原始 JSON 字符串（用于 rawEvent 透传）
     */
    fun parseEvent(json: JSONObject, rawJson: String = ""): AgUiEvent {
        val type = json.optString("type")
        val timestamp = json.optLong("timestamp", 0L)
        val raw = rawJson.ifEmpty { json.toString() }

        return when (type) {
            // ---- Lifecycle ----
            AgUiEventType.RUN_STARTED -> AgUiEvent.RunStarted(
                threadId = json.optString("threadId"),
                runId = json.optString("runId"),
                parentRunId = json.optString("parentRunId"),
                timestamp = timestamp,
                rawEvent = raw
            )
            AgUiEventType.RUN_FINISHED -> AgUiEvent.RunFinished(
                threadId = json.optString("threadId"),
                runId = json.optString("runId"),
                result = json.optJSONObject("result")?.toMap() ?: emptyMap(),
                timestamp = timestamp,
                rawEvent = raw
            )
            AgUiEventType.RUN_ERROR -> AgUiEvent.RunError(
                message = json.optString("message"),
                code = json.optString("code"),
                timestamp = timestamp,
                rawEvent = raw
            )
            AgUiEventType.STEP_STARTED -> AgUiEvent.StepStarted(
                stepName = json.optString("stepName"),
                timestamp = timestamp,
                rawEvent = raw
            )
            AgUiEventType.STEP_FINISHED -> AgUiEvent.StepFinished(
                stepName = json.optString("stepName"),
                timestamp = timestamp,
                rawEvent = raw
            )

            // ---- Text Message ----
            AgUiEventType.TEXT_MESSAGE_START -> AgUiEvent.TextMessageStart(
                messageId = json.optString("messageId"),
                role = json.optString("role", AgUiRole.ASSISTANT).ifEmpty { AgUiRole.ASSISTANT },
                timestamp = timestamp,
                rawEvent = raw
            )
            AgUiEventType.TEXT_MESSAGE_CONTENT -> AgUiEvent.TextMessageContent(
                messageId = json.optString("messageId"),
                delta = json.optString("delta"),
                timestamp = timestamp,
                rawEvent = raw
            )
            AgUiEventType.TEXT_MESSAGE_END -> AgUiEvent.TextMessageEnd(
                messageId = json.optString("messageId"),
                timestamp = timestamp,
                rawEvent = raw
            )
            AgUiEventType.TEXT_MESSAGE_CHUNK -> AgUiEvent.TextMessageChunk(
                messageId = json.optString("messageId"),
                role = json.optString("role"),
                delta = json.optString("delta"),
                timestamp = timestamp,
                rawEvent = raw
            )

            // ---- Tool Call ----
            AgUiEventType.TOOL_CALL_START -> AgUiEvent.ToolCallStart(
                toolCallId = json.optString("toolCallId"),
                toolCallName = json.optString("toolCallName"),
                parentMessageId = json.optString("parentMessageId"),
                timestamp = timestamp,
                rawEvent = raw
            )
            AgUiEventType.TOOL_CALL_ARGS -> AgUiEvent.ToolCallArgs(
                toolCallId = json.optString("toolCallId"),
                delta = json.optString("delta"),
                timestamp = timestamp,
                rawEvent = raw
            )
            AgUiEventType.TOOL_CALL_END -> AgUiEvent.ToolCallEnd(
                toolCallId = json.optString("toolCallId"),
                timestamp = timestamp,
                rawEvent = raw
            )
            AgUiEventType.TOOL_CALL_RESULT -> AgUiEvent.ToolCallResult(
                messageId = json.optString("messageId"),
                toolCallId = json.optString("toolCallId"),
                content = json.optString("content"),
                role = json.optString("role", AgUiRole.TOOL).ifEmpty { AgUiRole.TOOL },
                timestamp = timestamp,
                rawEvent = raw
            )
            AgUiEventType.TOOL_CALL_CHUNK -> AgUiEvent.ToolCallChunk(
                toolCallId = json.optString("toolCallId"),
                toolCallName = json.optString("toolCallName"),
                parentMessageId = json.optString("parentMessageId"),
                delta = json.optString("delta"),
                timestamp = timestamp,
                rawEvent = raw
            )

            // ---- State Management ----
            AgUiEventType.STATE_SNAPSHOT -> AgUiEvent.StateSnapshot(
                snapshot = json.optJSONObject("snapshot")?.toMap() ?: emptyMap(),
                timestamp = timestamp,
                rawEvent = raw
            )
            AgUiEventType.STATE_DELTA -> AgUiEvent.StateDelta(
                delta = json.optJSONArray("delta")?.toMapList() ?: emptyList(),
                timestamp = timestamp,
                rawEvent = raw
            )
            AgUiEventType.MESSAGES_SNAPSHOT -> AgUiEvent.MessagesSnapshot(
                messages = json.optJSONArray("messages")?.let(AgUiMessageParser::parseMessages).orEmpty(),
                timestamp = timestamp,
                rawEvent = raw
            )

            // ---- Activity ----
            AgUiEventType.ACTIVITY_SNAPSHOT -> AgUiEvent.ActivitySnapshot(
                messageId = json.optString("messageId"),
                activityType = json.optString("activityType"),
                content = json.optJSONObject("content")?.toMap() ?: emptyMap(),
                replace = json.optBoolean("replace", true),
                timestamp = timestamp,
                rawEvent = raw
            )
            AgUiEventType.ACTIVITY_DELTA -> AgUiEvent.ActivityDelta(
                messageId = json.optString("messageId"),
                activityType = json.optString("activityType"),
                patch = json.optJSONArray("patch")?.toMapList() ?: emptyList(),
                timestamp = timestamp,
                rawEvent = raw
            )

            // ---- Reasoning ----
            AgUiEventType.REASONING_START -> AgUiEvent.ReasoningStart(
                messageId = json.optString("messageId"),
                timestamp = timestamp,
                rawEvent = raw
            )
            AgUiEventType.REASONING_MESSAGE_START -> AgUiEvent.ReasoningMessageStart(
                messageId = json.optString("messageId"),
                role = json.optString("role", AgUiRole.ASSISTANT).ifEmpty { AgUiRole.ASSISTANT },
                timestamp = timestamp,
                rawEvent = raw
            )
            AgUiEventType.REASONING_MESSAGE_CONTENT -> AgUiEvent.ReasoningMessageContent(
                messageId = json.optString("messageId"),
                delta = json.optString("delta"),
                timestamp = timestamp,
                rawEvent = raw
            )
            AgUiEventType.REASONING_MESSAGE_END -> AgUiEvent.ReasoningMessageEnd(
                messageId = json.optString("messageId"),
                timestamp = timestamp,
                rawEvent = raw
            )
            AgUiEventType.REASONING_END -> AgUiEvent.ReasoningEnd(
                messageId = json.optString("messageId"),
                timestamp = timestamp,
                rawEvent = raw
            )

            // ---- Special ----
            AgUiEventType.RAW -> AgUiEvent.Raw(
                event = json.optJSONObject("event")?.toMap() ?: emptyMap(),
                source = json.optString("source"),
                timestamp = timestamp,
                rawEvent = raw
            )
            AgUiEventType.CUSTOM -> AgUiEvent.Custom(
                name = json.optString("name"),
                value = json.opt("value"),
                timestamp = timestamp,
                rawEvent = raw
            )
            AgUiEventType.META -> AgUiEvent.Meta(
                metaType = json.optString("metaType"),
                payload = json.optJSONObject("payload")?.toMap() ?: emptyMap(),
                timestamp = timestamp,
                rawEvent = raw
            )

            else -> AgUiEvent.Unknown(
                type = type,
                body = json.toMap(),
                timestamp = timestamp,
                rawEvent = raw
            )
        }
    }

    /**
     * 从 JSON 数组字符串解析批量事件。
     *
     * 常用于 MESSAGES_SNAPSHOT 类批量投递场景。
     */
    fun parseEvents(jsonArrayString: String): List<AgUiEvent> {
        if (jsonArrayString.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(jsonArrayString)
            (0 until arr.length()).mapNotNull { idx ->
                arr.optJSONObject(idx)?.let { parseEvent(it) }
            }
        }.getOrDefault(emptyList())
    }

    /**
     * 解析 SSE 一行 `data:` 字段内容。
     *
     * 传输层用法：将 SSE 的每行 `data: {...}` 的内容传给本方法。
     * 空行 / `[DONE]` 标记会返回 null。
     */
    fun parseSseData(dataLine: String): AgUiEvent? {
        val trimmed = dataLine.trim()
        if (trimmed.isEmpty() || trimmed == "[DONE]") return null
        return parseEvent(trimmed)
    }
}

/**
 * AG-UI 消息解析工具
 *
 * 单独拆出以便 MESSAGES_SNAPSHOT 与 RunAgentInput 复用。
 */
internal object AgUiMessageParser {

    fun parseMessages(array: JSONArray): List<AgUiMessage> {
        return (0 until array.length()).mapNotNull { i ->
            array.optJSONObject(i)?.let(::parseMessage)
        }
    }

    fun parseMessage(json: JSONObject): AgUiMessage {
        return AgUiMessage(
            id = json.optString("id"),
            role = json.optString("role"),
            content = json.optString("content"),
            name = json.optString("name"),
            toolCalls = json.optJSONArray("toolCalls")?.let(::parseToolCalls).orEmpty(),
            toolCallId = json.optString("toolCallId"),
            activityType = json.optString("activityType"),
            activityContent = json.optJSONObject("content")?.takeIf {
                json.optString("role") == AgUiRole.ACTIVITY
            }?.toMap() ?: emptyMap(),
            extras = emptyMap()
        )
    }

    private fun parseToolCalls(array: JSONArray): List<AgUiToolCall> {
        return (0 until array.length()).mapNotNull { i ->
            val obj = array.optJSONObject(i) ?: return@mapNotNull null
            val fn = obj.optJSONObject("function")
            AgUiToolCall(
                id = obj.optString("id"),
                type = obj.optString("type", "function").ifEmpty { "function" },
                function = AgUiToolFunction(
                    name = fn?.optString("name").orEmpty(),
                    arguments = fn?.optString("arguments").orEmpty()
                )
            )
        }
    }
}

// ============================
// JSON 工具扩展（内部使用）
// ============================
// 注：Kuikly JSONObject 自带 toMap()，这里不再重复定义。

/** 将 [JSONArray] 转换为 `List<Map<String, Any?>>`，过滤非对象元素。 */
internal fun JSONArray.toMapList(): List<Map<String, Any?>> {
    val list = mutableListOf<Map<String, Any?>>()
    for (i in 0 until length()) {
        val obj = optJSONObject(i)
        if (obj != null) list.add(obj.toMap())
    }
    return list
}
