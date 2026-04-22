package com.tencent.kuiklybase.chat.agui

import com.tencent.kuiklybase.chat.model.MessageStatus
import com.tencent.kuiklybase.chat.model.MessageType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * AG-UI 协议一致性测试（端到端场景）
 *
 * 模拟符合 AG-UI 官方规范的真实事件序列，验证完整 Run 场景的正确性。
 * 数据源：https://docs.ag-ui.com/concepts/events —— 官方事件流样例。
 *
 * 每个测试模拟一次完整的 SSE 流 —— 逐行解析 + 顺序分发 —— 
 * 最终断言 session.messages 达到预期状态，等同于真实后端接入的冒烟测试。
 *
 * 官方规范的关键约束（均验证）：
 * - RUN_STARTED 和 RUN_FINISHED/ERROR 形成完整边界
 * - TEXT_MESSAGE_START → CONTENT × N → END 严格顺序
 * - TOOL_CALL_START → ARGS × N → END → RESULT 完整链路
 * - 同 messageId 的事件归属同一消息
 * - 事件乱序/缺失时状态机能容错恢复
 */
class AgUiProtocolConformanceTest {

    /**
     * 场景 1：简单对话 —— "Hello" → "Hi there!"
     *
     * 最小可运行 Run：无工具调用、单轮问答。
     */
    @Test
    fun scenario_simple_conversation() {
        val session = AgUiSession.forTesting(threadId = "t1", aiSenderName = "AI")
        val events = listOf(
            """{"type":"RUN_STARTED","threadId":"t1","runId":"r1"}""",
            """{"type":"TEXT_MESSAGE_START","messageId":"m1","role":"assistant"}""",
            """{"type":"TEXT_MESSAGE_CONTENT","messageId":"m1","delta":"Hi "}""",
            """{"type":"TEXT_MESSAGE_CONTENT","messageId":"m1","delta":"there!"}""",
            """{"type":"TEXT_MESSAGE_END","messageId":"m1"}""",
            """{"type":"RUN_FINISHED","threadId":"t1","runId":"r1"}"""
        )

        events.forEach { session.dispatchJson(it) }

        assertFalse(session.isRunning)
        assertEquals(1, session.messages.size)
        val reply = session.messages[0]
        assertEquals("m1", reply.id)
        assertEquals("Hi there!", reply.content)
        assertEquals(MessageStatus.SENT, reply.status)
        assertFalse(reply.isSelf)
    }

    /**
     * 场景 2：带工具调用的对话 —— 天气查询
     *
     * 标准 AG-UI Function Calling 流：
     * assistant 文本 → tool call → tool result → assistant 追加回答
     */
    @Test
    fun scenario_tool_call_weather_query() {
        val session = AgUiSession.forTesting(threadId = "t1", aiSenderName = "AI")
        val events = listOf(
            """{"type":"RUN_STARTED","threadId":"t1","runId":"r1"}""",
            """{"type":"TEXT_MESSAGE_START","messageId":"m1","role":"assistant"}""",
            """{"type":"TEXT_MESSAGE_CONTENT","messageId":"m1","delta":"Let me check the weather."}""",
            """{"type":"TEXT_MESSAGE_END","messageId":"m1"}""",
            """{"type":"TOOL_CALL_START","toolCallId":"c1","toolCallName":"get_weather","parentMessageId":"m1"}""",
            """{"type":"TOOL_CALL_ARGS","toolCallId":"c1","delta":"{\"city\":"}""",
            """{"type":"TOOL_CALL_ARGS","toolCallId":"c1","delta":"\"Beijing\"}"}""",
            """{"type":"TOOL_CALL_END","toolCallId":"c1"}""",
            """{"type":"TOOL_CALL_RESULT","messageId":"tm1","toolCallId":"c1","content":"22°C, sunny"}""",
            """{"type":"TEXT_MESSAGE_START","messageId":"m2","role":"assistant"}""",
            """{"type":"TEXT_MESSAGE_CONTENT","messageId":"m2","delta":"Beijing is 22°C and sunny."}""",
            """{"type":"TEXT_MESSAGE_END","messageId":"m2"}""",
            """{"type":"RUN_FINISHED","threadId":"t1","runId":"r1"}"""
        )

        events.forEach { session.dispatchJson(it) }

        // 预期：3 条消息 —— assistant 查询前文、tool 结果、assistant 最终回答
        assertEquals(3, session.messages.size)
        assertEquals("Let me check the weather.", session.messages[0].content)
        assertEquals(MessageType.SYSTEM, session.messages[1].type)
        assertTrue(session.messages[1].content.contains("22°C, sunny"))
        assertEquals("Beijing is 22°C and sunny.", session.messages[2].content)
    }

    /**
     * 场景 3：Chunk 便捷模式 —— 整个 Run 全用 TEXT_MESSAGE_CHUNK
     *
     * 对应官方 TextMessageChunkEvent 规范：
     * - 首 chunk 必带 messageId
     * - 后续 chunk 可省略 messageId，自动沿用
     * - 切换 messageId 或流结束时自动闭合
     */
    @Test
    fun scenario_chunk_mode_single_message() {
        val session = AgUiSession.forTesting(threadId = "t1")
        val events = listOf(
            """{"type":"RUN_STARTED","threadId":"t1","runId":"r1"}""",
            """{"type":"TEXT_MESSAGE_CHUNK","messageId":"m1","role":"assistant","delta":"Part 1 "}""",
            """{"type":"TEXT_MESSAGE_CHUNK","delta":"Part 2 "}""",
            """{"type":"TEXT_MESSAGE_CHUNK","delta":"Part 3"}""",
            """{"type":"RUN_FINISHED","threadId":"t1","runId":"r1"}"""
        )

        events.forEach { session.dispatchJson(it) }

        assertEquals(1, session.messages.size)
        assertEquals("Part 1 Part 2 Part 3", session.messages[0].content)
        assertEquals(MessageStatus.SENT, session.messages[0].status)
    }

    /**
     * 场景 4：Chunk 模式 —— 多消息切换
     *
     * 验证切换 messageId 时，前一条自动闭合为 SENT。
     */
    @Test
    fun scenario_chunk_mode_multiple_messages() {
        val session = AgUiSession.forTesting(threadId = "t1")
        val events = listOf(
            """{"type":"RUN_STARTED","threadId":"t1","runId":"r1"}""",
            """{"type":"TEXT_MESSAGE_CHUNK","messageId":"m1","role":"assistant","delta":"A"}""",
            """{"type":"TEXT_MESSAGE_CHUNK","messageId":"m2","role":"assistant","delta":"B"}""",
            """{"type":"RUN_FINISHED","threadId":"t1","runId":"r1"}"""
        )
        events.forEach { session.dispatchJson(it) }

        assertEquals(2, session.messages.size)
        assertEquals("A", session.messages[0].content)
        assertEquals(MessageStatus.SENT, session.messages[0].status)
        assertEquals("B", session.messages[1].content)
        assertEquals(MessageStatus.SENT, session.messages[1].status)
    }

    /**
     * 场景 5：Activity 事件 —— 任务规划
     *
     * ACTIVITY_SNAPSHOT 建立初始快照，ACTIVITY_DELTA 增量更新。
     */
    @Test
    fun scenario_activity_plan_with_progress() {
        val session = AgUiSession.forTesting(threadId = "t1")
        val events = listOf(
            """{"type":"RUN_STARTED","threadId":"t1","runId":"r1"}""",
            """{"type":"ACTIVITY_SNAPSHOT","messageId":"plan_1","activityType":"PLAN","content":{"title":"3 steps","steps":["a","b","c"]}}""",
            """{"type":"ACTIVITY_DELTA","messageId":"plan_1","activityType":"PLAN","patch":[{"op":"replace","path":"/steps/0/done","value":true}]}""",
            """{"type":"RUN_FINISHED","threadId":"t1","runId":"r1"}"""
        )
        events.forEach { session.dispatchJson(it) }

        assertEquals(1, session.messages.size)
        val msg = session.messages[0]
        assertEquals(MessageType.CUSTOM, msg.type)
        assertEquals("activity", msg.extra["agUiKind"])
        assertEquals("PLAN", msg.extra["activityType"])
        assertTrue(msg.content.contains("3 steps"))
    }

    /**
     * 场景 6：Reasoning 推理过程
     *
     * AG-UI 规范：Reasoning 消息与 assistant 消息独立，
     * 通过 REASONING_MESSAGE_* 事件流式呈现。
     */
    @Test
    fun scenario_reasoning_separate_from_answer() {
        val session = AgUiSession.forTesting(threadId = "t1")
        val events = listOf(
            """{"type":"RUN_STARTED","threadId":"t1","runId":"r1"}""",
            """{"type":"REASONING_MESSAGE_START","messageId":"r1","role":"assistant"}""",
            """{"type":"REASONING_MESSAGE_CONTENT","messageId":"r1","delta":"Let me think..."}""",
            """{"type":"REASONING_MESSAGE_CONTENT","messageId":"r1","delta":" 2+2=4."}""",
            """{"type":"REASONING_MESSAGE_END","messageId":"r1"}""",
            """{"type":"TEXT_MESSAGE_START","messageId":"a1","role":"assistant"}""",
            """{"type":"TEXT_MESSAGE_CONTENT","messageId":"a1","delta":"The answer is 4."}""",
            """{"type":"TEXT_MESSAGE_END","messageId":"a1"}""",
            """{"type":"RUN_FINISHED","threadId":"t1","runId":"r1"}"""
        )
        events.forEach { session.dispatchJson(it) }

        assertEquals(2, session.messages.size)
        // 第一条：Reasoning（SYSTEM 类型，带 agUiKind=reasoning 标记）
        assertEquals(MessageType.SYSTEM, session.messages[0].type)
        assertEquals("reasoning", session.messages[0].extra["agUiKind"])
        assertEquals("Let me think... 2+2=4.", session.messages[0].content)
        // 第二条：最终答案（TEXT）
        assertEquals(MessageType.TEXT, session.messages[1].type)
        assertEquals("The answer is 4.", session.messages[1].content)
    }

    /**
     * 场景 7：错误恢复 —— 流式中断后 RunError
     *
     * 未完成的消息被标记为 FAILED，便于 UI 展示"重试"按钮。
     */
    @Test
    fun scenario_run_error_during_stream() {
        val session = AgUiSession.forTesting(threadId = "t1")
        val events = listOf(
            """{"type":"RUN_STARTED","threadId":"t1","runId":"r1"}""",
            """{"type":"TEXT_MESSAGE_START","messageId":"m1","role":"assistant"}""",
            """{"type":"TEXT_MESSAGE_CONTENT","messageId":"m1","delta":"This is a partial"}""",
            """{"type":"RUN_ERROR","message":"connection lost","code":"NETWORK"}"""
        )
        events.forEach { session.dispatchJson(it) }

        assertFalse(session.isRunning)
        assertEquals("connection lost", session.lastError)
        assertEquals(1, session.messages.size)
        val failed = session.messages[0]
        assertEquals("This is a partial", failed.content)
        assertEquals(MessageStatus.FAILED, failed.status)
    }

    /**
     * 场景 8：断线重连 —— MESSAGES_SNAPSHOT 恢复历史
     *
     * 前端重新连接后，服务端发来完整消息快照，
     * 状态机应当丢弃之前所有内容并以快照为准。
     */
    @Test
    fun scenario_reconnect_with_messages_snapshot() {
        val session = AgUiSession.forTesting(threadId = "t1")

        // 连接 1：收到一部分消息
        session.dispatchJson("""{"type":"TEXT_MESSAGE_START","messageId":"m1","role":"assistant"}""")
        session.dispatchJson("""{"type":"TEXT_MESSAGE_CONTENT","messageId":"m1","delta":"partial"}""")
        assertEquals(1, session.messages.size)

        // 断线重连，服务端推送完整快照
        session.dispatchJson("""
            {"type":"MESSAGES_SNAPSHOT","messages":[
                {"id":"u1","role":"user","content":"Hi"},
                {"id":"a1","role":"assistant","content":"Hello, welcome back!"}
            ]}
        """.trimIndent())

        assertEquals(2, session.messages.size)
        assertEquals("Hi", session.messages[0].content)
        assertEquals("Hello, welcome back!", session.messages[1].content)
        assertTrue(session.messages[0].isSelf)
        assertFalse(session.messages[1].isSelf)
    }

    /**
     * 场景 9：SSE 数据流全链路 —— 模拟真实 HTTP SSE 响应解析
     *
     * 验证 dispatchSseData 正确过滤 [DONE] 与空行。
     */
    @Test
    fun scenario_sse_stream_with_done_marker() {
        val session = AgUiSession.forTesting(threadId = "t1")
        // 模拟 SSE data: 行
        val sseDataLines = listOf(
            """{"type":"RUN_STARTED","threadId":"t1","runId":"r1"}""",
            "",   // 空行心跳
            """{"type":"TEXT_MESSAGE_CHUNK","messageId":"m1","role":"assistant","delta":"OK"}""",
            """{"type":"RUN_FINISHED","threadId":"t1","runId":"r1"}""",
            "[DONE]"
        )
        sseDataLines.forEach { session.dispatchSseData(it) }

        assertFalse(session.isRunning)
        assertEquals(1, session.messages.size)
        assertEquals("OK", session.messages[0].content)
        assertEquals(MessageStatus.SENT, session.messages[0].status)
    }

    /**
     * 场景 10：多步骤 Run —— STEP_STARTED / STEP_FINISHED 嵌套
     *
     * 验证 Step 事件不影响消息生成，仅用于进度反馈。
     */
    @Test
    fun scenario_multi_step_run() {
        val session = AgUiSession.forTesting(threadId = "t1")
        val stepTrace = mutableListOf<String>()
        session.onStepStarted = { stepTrace += "start:$it" }
        session.onStepFinished = { stepTrace += "end:$it" }

        val events = listOf(
            """{"type":"RUN_STARTED","threadId":"t1","runId":"r1"}""",
            """{"type":"STEP_STARTED","stepName":"retrieve"}""",
            """{"type":"STEP_FINISHED","stepName":"retrieve"}""",
            """{"type":"STEP_STARTED","stepName":"generate"}""",
            """{"type":"TEXT_MESSAGE_CHUNK","messageId":"m1","role":"assistant","delta":"done"}""",
            """{"type":"STEP_FINISHED","stepName":"generate"}""",
            """{"type":"RUN_FINISHED","threadId":"t1","runId":"r1"}"""
        )
        events.forEach { session.dispatchJson(it) }

        assertEquals(
            listOf("start:retrieve", "end:retrieve", "start:generate", "end:generate"),
            stepTrace
        )
        assertEquals(1, session.messages.size)
        assertEquals("done", session.messages[0].content)
    }

    /**
     * 场景 11：事件 timestamp 正确保留
     *
     * 确保 timestamp 字段透传，便于调试与排序。
     */
    @Test
    fun scenario_timestamp_preserved_in_events() {
        val event = AgUiEventParser.parseEvent(
            """{"type":"RUN_STARTED","threadId":"t1","runId":"r1","timestamp":1714063982123}"""
        )
        assertTrue(event is AgUiEvent.RunStarted)
        assertEquals(1714063982123L, event.timestamp)
    }

    /**
     * 场景 12：未知事件优雅降级
     *
     * 协议向前兼容 —— 未来新增的事件类型不影响当前实现。
     */
    @Test
    fun scenario_future_event_types_gracefully_degraded() {
        val session = AgUiSession.forTesting(threadId = "t1")
        var unknownCount = 0
        session.handlerRegistry.unknownEventHandler =
            AgUiUnknownEventHandler { _, _ -> unknownCount++ }

        val events = listOf(
            """{"type":"RUN_STARTED","threadId":"t1","runId":"r1"}""",
            """{"type":"NEW_FUTURE_EVENT_1","data":"x"}""",
            """{"type":"TEXT_MESSAGE_CHUNK","messageId":"m1","role":"assistant","delta":"ok"}""",
            """{"type":"NEW_FUTURE_EVENT_2","data":"y"}""",
            """{"type":"RUN_FINISHED","threadId":"t1","runId":"r1"}"""
        )
        events.forEach { session.dispatchJson(it) }

        // 未知事件被兜底处理，不影响已知事件的正确执行
        assertEquals(2, unknownCount)
        assertEquals(1, session.messages.size)
        assertEquals("ok", session.messages[0].content)
    }
}
