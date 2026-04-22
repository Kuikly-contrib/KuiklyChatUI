package com.tencent.kuiklybase.chat.agui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * AG-UI 事件解析器单元测试
 *
 * 覆盖：
 * - 所有 16 种标准事件类型的 JSON 反序列化
 * - 关键字段提取正确性
 * - 容错场景：空串、未知类型、字段缺失、畸形 JSON
 * - SSE 协议行解析（含 [DONE] / 空行）
 *
 * 测试策略：针对每个事件类型，构造一个典型的 JSON 样本，
 * 解析后验证：
 * 1. 返回的事件类型（sealed class 分支）正确
 * 2. 所有字段值与 JSON 一致
 * 3. rawEvent 字段被正确透传
 */
class AgUiEventParserTest {

    // ============================
    // 1. 生命周期事件解析
    // ============================

    @Test
    fun parse_run_started_event() {
        val json = """{"type":"RUN_STARTED","threadId":"thread_1","runId":"run_42","parentRunId":"run_41","timestamp":1714063982000}"""
        val event = AgUiEventParser.parseEvent(json)
        assertIs<AgUiEvent.RunStarted>(event)
        assertEquals("thread_1", event.threadId)
        assertEquals("run_42", event.runId)
        assertEquals("run_41", event.parentRunId)
        assertEquals(1714063982000L, event.timestamp)
        assertEquals(json, event.rawEvent)
    }

    @Test
    fun parse_run_finished_event_with_result() {
        val json = """{"type":"RUN_FINISHED","threadId":"t1","runId":"r1","result":{"tokens":42,"model":"gpt-4"}}"""
        val event = AgUiEventParser.parseEvent(json)
        assertIs<AgUiEvent.RunFinished>(event)
        assertEquals("t1", event.threadId)
        assertEquals("r1", event.runId)
        assertEquals(42, event.result["tokens"])
        assertEquals("gpt-4", event.result["model"])
    }

    @Test
    fun parse_run_error_event() {
        val json = """{"type":"RUN_ERROR","message":"LLM timeout","code":"504"}"""
        val event = AgUiEventParser.parseEvent(json)
        assertIs<AgUiEvent.RunError>(event)
        assertEquals("LLM timeout", event.message)
        assertEquals("504", event.code)
    }

    @Test
    fun parse_step_started_and_finished_events() {
        val startEvent = AgUiEventParser.parseEvent(
            """{"type":"STEP_STARTED","stepName":"retrieve"}"""
        )
        assertIs<AgUiEvent.StepStarted>(startEvent)
        assertEquals("retrieve", startEvent.stepName)

        val endEvent = AgUiEventParser.parseEvent(
            """{"type":"STEP_FINISHED","stepName":"retrieve"}"""
        )
        assertIs<AgUiEvent.StepFinished>(endEvent)
        assertEquals("retrieve", endEvent.stepName)
    }

    // ============================
    // 2. 文本消息事件解析
    // ============================

    @Test
    fun parse_text_message_start_with_default_role() {
        val json = """{"type":"TEXT_MESSAGE_START","messageId":"m1"}"""
        val event = AgUiEventParser.parseEvent(json)
        assertIs<AgUiEvent.TextMessageStart>(event)
        assertEquals("m1", event.messageId)
        // 未指定 role 时默认为 assistant
        assertEquals(AgUiRole.ASSISTANT, event.role)
    }

    @Test
    fun parse_text_message_start_with_explicit_role() {
        val json = """{"type":"TEXT_MESSAGE_START","messageId":"m1","role":"system"}"""
        val event = AgUiEventParser.parseEvent(json)
        assertIs<AgUiEvent.TextMessageStart>(event)
        assertEquals(AgUiRole.SYSTEM, event.role)
    }

    @Test
    fun parse_text_message_content_preserves_delta_exactly() {
        // delta 可能包含特殊字符、换行、空格
        val deltaInJson = "Hello, 世界！\\n- **bold** `code`"
        val expected = "Hello, 世界！\n- **bold** `code`"
        val json = """{"type":"TEXT_MESSAGE_CONTENT","messageId":"m1","delta":"$deltaInJson"}"""
        val event = AgUiEventParser.parseEvent(json)
        assertIs<AgUiEvent.TextMessageContent>(event)
        assertEquals("m1", event.messageId)
        assertEquals(expected, event.delta)
    }

    @Test
    fun parse_text_message_end() {
        val json = """{"type":"TEXT_MESSAGE_END","messageId":"m1"}"""
        val event = AgUiEventParser.parseEvent(json)
        assertIs<AgUiEvent.TextMessageEnd>(event)
        assertEquals("m1", event.messageId)
    }

    @Test
    fun parse_text_message_chunk_all_fields_optional() {
        // 首 chunk：带 messageId + role + delta
        val first = AgUiEventParser.parseEvent(
            """{"type":"TEXT_MESSAGE_CHUNK","messageId":"m1","role":"assistant","delta":"Hi "}"""
        )
        assertIs<AgUiEvent.TextMessageChunk>(first)
        assertEquals("m1", first.messageId)
        assertEquals("assistant", first.role)
        assertEquals("Hi ", first.delta)

        // 后续 chunk：只带 delta
        val next = AgUiEventParser.parseEvent(
            """{"type":"TEXT_MESSAGE_CHUNK","delta":"World"}"""
        )
        assertIs<AgUiEvent.TextMessageChunk>(next)
        assertEquals("", next.messageId)
        assertEquals("World", next.delta)
    }

    // ============================
    // 3. 工具调用事件解析
    // ============================

    @Test
    fun parse_tool_call_start() {
        val json = """{"type":"TOOL_CALL_START","toolCallId":"call_1","toolCallName":"query_weather","parentMessageId":"m1"}"""
        val event = AgUiEventParser.parseEvent(json)
        assertIs<AgUiEvent.ToolCallStart>(event)
        assertEquals("call_1", event.toolCallId)
        assertEquals("query_weather", event.toolCallName)
        assertEquals("m1", event.parentMessageId)
    }

    @Test
    fun parse_tool_call_args_delta() {
        val json = """{"type":"TOOL_CALL_ARGS","toolCallId":"call_1","delta":"{\"city\":"}"""
        val event = AgUiEventParser.parseEvent(json)
        assertIs<AgUiEvent.ToolCallArgs>(event)
        assertEquals("call_1", event.toolCallId)
        assertEquals("{\"city\":", event.delta)
    }

    @Test
    fun parse_tool_call_end() {
        val json = """{"type":"TOOL_CALL_END","toolCallId":"call_1"}"""
        val event = AgUiEventParser.parseEvent(json)
        assertIs<AgUiEvent.ToolCallEnd>(event)
        assertEquals("call_1", event.toolCallId)
    }

    @Test
    fun parse_tool_call_result_defaults_role_to_tool() {
        val json = """{"type":"TOOL_CALL_RESULT","messageId":"tool_msg_1","toolCallId":"call_1","content":"Beijing 22°C"}"""
        val event = AgUiEventParser.parseEvent(json)
        assertIs<AgUiEvent.ToolCallResult>(event)
        assertEquals("tool_msg_1", event.messageId)
        assertEquals("call_1", event.toolCallId)
        assertEquals("Beijing 22°C", event.content)
        // role 未显式指定时默认为 "tool"
        assertEquals(AgUiRole.TOOL, event.role)
    }

    @Test
    fun parse_tool_call_chunk() {
        val json = """{"type":"TOOL_CALL_CHUNK","toolCallId":"call_1","toolCallName":"search","parentMessageId":"m1","delta":"{\"q\":\"kotlin\"}"}"""
        val event = AgUiEventParser.parseEvent(json)
        assertIs<AgUiEvent.ToolCallChunk>(event)
        assertEquals("call_1", event.toolCallId)
        assertEquals("search", event.toolCallName)
        assertEquals("m1", event.parentMessageId)
        assertEquals("{\"q\":\"kotlin\"}", event.delta)
    }

    // ============================
    // 4. 状态管理事件解析
    // ============================

    @Test
    fun parse_state_snapshot() {
        val json = """{"type":"STATE_SNAPSHOT","snapshot":{"counter":3,"user":"Alice"}}"""
        val event = AgUiEventParser.parseEvent(json)
        assertIs<AgUiEvent.StateSnapshot>(event)
        assertEquals(3, event.snapshot["counter"])
        assertEquals("Alice", event.snapshot["user"])
    }

    @Test
    fun parse_state_delta_with_json_patch_array() {
        val json = """
            {
              "type":"STATE_DELTA",
              "delta":[
                {"op":"replace","path":"/counter","value":5},
                {"op":"add","path":"/flag","value":true}
              ]
            }
        """.trimIndent()
        val event = AgUiEventParser.parseEvent(json)
        assertIs<AgUiEvent.StateDelta>(event)
        assertEquals(2, event.delta.size)
        assertEquals("replace", event.delta[0]["op"])
        assertEquals("/counter", event.delta[0]["path"])
        assertEquals(5, event.delta[0]["value"])
        assertEquals("add", event.delta[1]["op"])
    }

    @Test
    fun parse_messages_snapshot_with_multiple_roles() {
        val json = """
            {
              "type":"MESSAGES_SNAPSHOT",
              "messages":[
                {"id":"m1","role":"user","content":"Hi"},
                {"id":"m2","role":"assistant","content":"Hello!"},
                {"id":"m3","role":"tool","content":"result","toolCallId":"call_1"}
              ]
            }
        """.trimIndent()
        val event = AgUiEventParser.parseEvent(json)
        assertIs<AgUiEvent.MessagesSnapshot>(event)
        assertEquals(3, event.messages.size)
        assertEquals(AgUiRole.USER, event.messages[0].role)
        assertEquals("Hi", event.messages[0].content)
        assertEquals(AgUiRole.ASSISTANT, event.messages[1].role)
        assertEquals("Hello!", event.messages[1].content)
        assertEquals(AgUiRole.TOOL, event.messages[2].role)
        assertEquals("call_1", event.messages[2].toolCallId)
    }

    @Test
    fun parse_messages_snapshot_with_tool_calls() {
        val json = """
            {
              "type":"MESSAGES_SNAPSHOT",
              "messages":[
                {
                  "id":"m1","role":"assistant","content":"Let me check",
                  "toolCalls":[
                    {
                      "id":"call_1","type":"function",
                      "function":{"name":"get_weather","arguments":"{\"city\":\"Beijing\"}"}
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
        val event = AgUiEventParser.parseEvent(json)
        assertIs<AgUiEvent.MessagesSnapshot>(event)
        assertEquals(1, event.messages.size)
        val msg = event.messages[0]
        assertEquals(1, msg.toolCalls.size)
        assertEquals("call_1", msg.toolCalls[0].id)
        assertEquals("function", msg.toolCalls[0].type)
        assertEquals("get_weather", msg.toolCalls[0].function.name)
        assertEquals("""{"city":"Beijing"}""", msg.toolCalls[0].function.arguments)
    }

    // ============================
    // 5. Activity 事件解析
    // ============================

    @Test
    fun parse_activity_snapshot_with_replace_default_true() {
        val json = """
            {
              "type":"ACTIVITY_SNAPSHOT",
              "messageId":"act_1",
              "activityType":"PLAN",
              "content":{"title":"My plan","steps":["step1","step2"]}
            }
        """.trimIndent()
        val event = AgUiEventParser.parseEvent(json)
        assertIs<AgUiEvent.ActivitySnapshot>(event)
        assertEquals("act_1", event.messageId)
        assertEquals("PLAN", event.activityType)
        assertEquals("My plan", event.content["title"])
        // replace 未指定时默认为 true
        assertTrue(event.replace)
    }

    @Test
    fun parse_activity_snapshot_with_replace_false() {
        val json = """{"type":"ACTIVITY_SNAPSHOT","messageId":"act_1","activityType":"PLAN","content":{},"replace":false}"""
        val event = AgUiEventParser.parseEvent(json)
        assertIs<AgUiEvent.ActivitySnapshot>(event)
        assertEquals(false, event.replace)
    }

    @Test
    fun parse_activity_delta_with_patch() {
        val json = """
            {
              "type":"ACTIVITY_DELTA",
              "messageId":"act_1",
              "activityType":"PLAN",
              "patch":[{"op":"replace","path":"/steps/0/done","value":true}]
            }
        """.trimIndent()
        val event = AgUiEventParser.parseEvent(json)
        assertIs<AgUiEvent.ActivityDelta>(event)
        assertEquals("act_1", event.messageId)
        assertEquals("PLAN", event.activityType)
        assertEquals(1, event.patch.size)
        assertEquals("replace", event.patch[0]["op"])
    }

    // ============================
    // 6. Reasoning 事件解析
    // ============================

    @Test
    fun parse_reasoning_message_lifecycle() {
        val start = AgUiEventParser.parseEvent(
            """{"type":"REASONING_MESSAGE_START","messageId":"r1","role":"assistant"}"""
        )
        assertIs<AgUiEvent.ReasoningMessageStart>(start)
        assertEquals("r1", start.messageId)

        val content = AgUiEventParser.parseEvent(
            """{"type":"REASONING_MESSAGE_CONTENT","messageId":"r1","delta":"Think..."}"""
        )
        assertIs<AgUiEvent.ReasoningMessageContent>(content)
        assertEquals("Think...", content.delta)

        val end = AgUiEventParser.parseEvent(
            """{"type":"REASONING_MESSAGE_END","messageId":"r1"}"""
        )
        assertIs<AgUiEvent.ReasoningMessageEnd>(end)
    }

    @Test
    fun parse_reasoning_start_end_boundaries() {
        val start = AgUiEventParser.parseEvent("""{"type":"REASONING_START","messageId":"r1"}""")
        assertIs<AgUiEvent.ReasoningStart>(start)

        val end = AgUiEventParser.parseEvent("""{"type":"REASONING_END","messageId":"r1"}""")
        assertIs<AgUiEvent.ReasoningEnd>(end)
    }

    // ============================
    // 7. 特殊事件解析
    // ============================

    @Test
    fun parse_raw_event() {
        val json = """{"type":"RAW","event":{"foo":"bar"},"source":"legacy"}"""
        val event = AgUiEventParser.parseEvent(json)
        assertIs<AgUiEvent.Raw>(event)
        assertEquals("bar", event.event["foo"])
        assertEquals("legacy", event.source)
    }

    @Test
    fun parse_custom_event_with_arbitrary_value() {
        val json = """{"type":"CUSTOM","name":"voice_input","value":"hello world"}"""
        val event = AgUiEventParser.parseEvent(json)
        assertIs<AgUiEvent.Custom>(event)
        assertEquals("voice_input", event.name)
        assertEquals("hello world", event.value)
    }

    @Test
    fun parse_meta_event() {
        val json = """{"type":"META","metaType":"thumbs_up","payload":{"messageId":"m1","userId":"u1"}}"""
        val event = AgUiEventParser.parseEvent(json)
        assertIs<AgUiEvent.Meta>(event)
        assertEquals("thumbs_up", event.metaType)
        assertEquals("m1", event.payload["messageId"])
        assertEquals("u1", event.payload["userId"])
    }

    @Test
    fun parse_unknown_event_type_falls_back_to_Unknown() {
        val json = """{"type":"SOME_FUTURE_EVENT","someField":"someValue","timestamp":123}"""
        val event = AgUiEventParser.parseEvent(json)
        assertIs<AgUiEvent.Unknown>(event)
        assertEquals("SOME_FUTURE_EVENT", event.type)
        assertEquals("someValue", event.body["someField"])
        assertEquals(123L, event.timestamp)
    }

    // ============================
    // 8. 容错场景
    // ============================

    @Test
    fun parse_blank_string_returns_null() {
        assertNull(AgUiEventParser.parseEvent(""))
        assertNull(AgUiEventParser.parseEvent("   "))
    }

    @Test
    fun parse_malformed_json_returns_null() {
        assertNull(AgUiEventParser.parseEvent("not a json"))
        assertNull(AgUiEventParser.parseEvent("{broken"))
    }

    @Test
    fun parse_missing_optional_fields_uses_defaults() {
        // RunStarted 没有 parentRunId
        val event = AgUiEventParser.parseEvent("""{"type":"RUN_STARTED","threadId":"t1","runId":"r1"}""")
        assertIs<AgUiEvent.RunStarted>(event)
        assertEquals("", event.parentRunId)
        assertEquals(0L, event.timestamp)
    }

    // ============================
    // 9. 批量解析
    // ============================

    @Test
    fun parse_events_from_json_array() {
        val jsonArray = """
            [
              {"type":"RUN_STARTED","threadId":"t1","runId":"r1"},
              {"type":"TEXT_MESSAGE_CHUNK","messageId":"m1","role":"assistant","delta":"Hello"},
              {"type":"RUN_FINISHED","threadId":"t1","runId":"r1"}
            ]
        """.trimIndent()
        val events = AgUiEventParser.parseEvents(jsonArray)
        assertEquals(3, events.size)
        assertIs<AgUiEvent.RunStarted>(events[0])
        assertIs<AgUiEvent.TextMessageChunk>(events[1])
        assertIs<AgUiEvent.RunFinished>(events[2])
    }

    @Test
    fun parse_events_empty_array_returns_empty_list() {
        assertEquals(0, AgUiEventParser.parseEvents("[]").size)
    }

    @Test
    fun parse_events_malformed_returns_empty_list() {
        assertEquals(0, AgUiEventParser.parseEvents("not json").size)
    }

    // ============================
    // 10. SSE 解析
    // ============================

    @Test
    fun parse_sse_data_valid_json_succeeds() {
        val event = AgUiEventParser.parseSseData("""{"type":"RUN_FINISHED","threadId":"t1","runId":"r1"}""")
        assertIs<AgUiEvent.RunFinished>(event)
    }

    @Test
    fun parse_sse_data_skips_done_marker() {
        assertNull(AgUiEventParser.parseSseData("[DONE]"))
        assertNull(AgUiEventParser.parseSseData("  [DONE]  "))
    }

    @Test
    fun parse_sse_data_skips_blank_lines() {
        assertNull(AgUiEventParser.parseSseData(""))
        assertNull(AgUiEventParser.parseSseData("   \t "))
    }

    @Test
    fun parse_sse_data_trims_whitespace() {
        val event = AgUiEventParser.parseSseData(
            """  {"type":"TEXT_MESSAGE_END","messageId":"m1"}  """
        )
        assertIs<AgUiEvent.TextMessageEnd>(event)
    }

    // ============================
    // 11. 事件 type 常量与枚举值对齐
    // ============================

    @Test
    fun event_type_field_matches_constant() {
        // 所有具体事件的 type 字段应等于对应的事件类型常量
        assertEquals(AgUiEventType.RUN_STARTED, AgUiEvent.RunStarted(threadId = "", runId = "").type)
        assertEquals(AgUiEventType.TEXT_MESSAGE_CONTENT, AgUiEvent.TextMessageContent(messageId = "", delta = "").type)
        assertEquals(AgUiEventType.TOOL_CALL_RESULT, AgUiEvent.ToolCallResult(messageId = "", toolCallId = "", content = "").type)
        assertEquals(AgUiEventType.ACTIVITY_SNAPSHOT, AgUiEvent.ActivitySnapshot(messageId = "", activityType = "", content = emptyMap()).type)
        assertEquals(AgUiEventType.CUSTOM, AgUiEvent.Custom(name = "x").type)
        assertEquals(AgUiEventType.META, AgUiEvent.Meta(metaType = "", payload = emptyMap()).type)
    }
}
