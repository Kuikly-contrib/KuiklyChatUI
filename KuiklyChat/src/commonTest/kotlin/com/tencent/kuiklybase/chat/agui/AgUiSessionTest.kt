package com.tencent.kuiklybase.chat.agui

import com.tencent.kuiklybase.chat.model.MessageStatus
import com.tencent.kuiklybase.chat.model.MessageType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * AG-UI 会话状态机单元测试
 *
 * 覆盖：
 * - 运行生命周期（Run/Step）回调触发与状态切换
 * - 流式文本消息合并（Start/Content/End 与 Chunk 两种模式）
 * - 工具调用参数拼接 + 结果回传
 * - Activity / Custom / Meta 事件路由到自定义处理器
 * - MessagesSnapshot 覆盖式快照恢复
 * - 错误处理（RunError 将未完成消息标记为 FAILED）
 * - reset() 清空所有状态
 *
 * 测试策略：直接构造 AgUiEvent 对象驱动 session，
 * 验证 session.messages 列表的最终状态。
 */
class AgUiSessionTest {

    private fun newSession() = AgUiSession.forTesting(
        threadId = "t1",
        selfSenderId = "user_self",
        aiSenderName = "AI",
        aiAvatarUrl = ""
    )

    // ============================
    // 1. 生命周期事件
    // ============================

    @Test
    fun run_lifecycle_triggers_callbacks_and_updates_state() {
        val session = newSession()
        var startedRunId = ""
        var finishedRunId = ""
        session.onRunStarted = { startedRunId = it }
        session.onRunFinished = { finishedRunId = it }

        assertFalse(session.isRunning)
        session.dispatch(AgUiEvent.RunStarted(threadId = "t1", runId = "r1"))
        assertTrue(session.isRunning)
        assertEquals("r1", session.currentRunId)
        assertEquals("r1", startedRunId)

        session.dispatch(AgUiEvent.RunFinished(threadId = "t1", runId = "r1"))
        assertFalse(session.isRunning)
        assertEquals("r1", finishedRunId)
    }

    @Test
    fun step_lifecycle_tracks_current_step() {
        val session = newSession()
        val trace = mutableListOf<String>()
        session.onStepStarted = { trace += "start:$it" }
        session.onStepFinished = { trace += "end:$it" }

        session.dispatch(AgUiEvent.StepStarted("retrieve"))
        assertEquals("retrieve", session.currentStep)
        session.dispatch(AgUiEvent.StepFinished("retrieve"))
        assertEquals("", session.currentStep)

        assertEquals(listOf("start:retrieve", "end:retrieve"), trace)
    }

    // ============================
    // 2. 流式文本消息 —— 标准三段式
    // ============================

    @Test
    fun text_message_start_creates_sending_placeholder() {
        val session = newSession()
        session.dispatch(AgUiEvent.TextMessageStart(messageId = "m1", role = AgUiRole.ASSISTANT))
        assertEquals(1, session.messages.size)
        val msg = session.messages[0]
        assertEquals("m1", msg.id)
        assertEquals("", msg.content)
        assertEquals(MessageStatus.SENDING, msg.status)
        assertFalse(msg.isSelf)
    }

    @Test
    fun text_message_content_appends_delta_to_existing_message() {
        val session = newSession()
        session.dispatch(AgUiEvent.TextMessageStart(messageId = "m1", role = AgUiRole.ASSISTANT))
        session.dispatch(AgUiEvent.TextMessageContent(messageId = "m1", delta = "Hello "))
        session.dispatch(AgUiEvent.TextMessageContent(messageId = "m1", delta = "World"))

        assertEquals(1, session.messages.size)
        assertEquals("Hello World", session.messages[0].content)
        assertEquals(MessageStatus.SENDING, session.messages[0].status)
    }

    @Test
    fun text_message_end_marks_message_as_sent() {
        val session = newSession()
        session.dispatch(AgUiEvent.TextMessageStart(messageId = "m1", role = AgUiRole.ASSISTANT))
        session.dispatch(AgUiEvent.TextMessageContent(messageId = "m1", delta = "Done"))
        session.dispatch(AgUiEvent.TextMessageEnd(messageId = "m1"))

        val msg = session.messages[0]
        assertEquals("Done", msg.content)
        assertEquals(MessageStatus.SENT, msg.status)
    }

    @Test
    fun text_message_content_without_start_auto_creates_message() {
        // 容错场景：后端漏发 START，直接收到 CONTENT 也应能正常工作
        val session = newSession()
        session.dispatch(AgUiEvent.TextMessageContent(messageId = "m1", delta = "Auto-created"))
        assertEquals(1, session.messages.size)
        assertEquals("Auto-created", session.messages[0].content)
    }

    @Test
    fun text_message_user_role_creates_self_message() {
        val session = newSession()
        session.dispatch(AgUiEvent.TextMessageStart(messageId = "u1", role = AgUiRole.USER))
        session.dispatch(AgUiEvent.TextMessageContent(messageId = "u1", delta = "Hi"))
        session.dispatch(AgUiEvent.TextMessageEnd(messageId = "u1"))

        val msg = session.messages[0]
        assertTrue(msg.isSelf)
        assertEquals("Hi", msg.content)
        assertEquals("user_self", msg.senderId)
    }

    // ============================
    // 3. 流式文本消息 —— Chunk 便捷模式
    // ============================

    @Test
    fun text_message_chunk_first_with_id_creates_message() {
        val session = newSession()
        session.dispatch(
            AgUiEvent.TextMessageChunk(messageId = "m1", role = AgUiRole.ASSISTANT, delta = "Hello ")
        )
        session.dispatch(AgUiEvent.TextMessageChunk(delta = "World"))

        assertEquals(1, session.messages.size)
        assertEquals("Hello World", session.messages[0].content)
    }

    @Test
    fun text_message_chunk_switching_id_closes_previous_message() {
        val session = newSession()
        // 第一条消息
        session.dispatch(
            AgUiEvent.TextMessageChunk(messageId = "m1", role = AgUiRole.ASSISTANT, delta = "First")
        )
        // 切换到第二条 messageId —— 应自动 finalize 第一条
        session.dispatch(
            AgUiEvent.TextMessageChunk(messageId = "m2", role = AgUiRole.ASSISTANT, delta = "Second")
        )

        assertEquals(2, session.messages.size)
        assertEquals("First", session.messages[0].content)
        assertEquals(MessageStatus.SENT, session.messages[0].status)
        assertEquals("Second", session.messages[1].content)
        assertEquals(MessageStatus.SENDING, session.messages[1].status)
    }

    // ============================
    // 4. 工具调用
    // ============================

    @Test
    fun tool_call_flow_produces_result_message_via_default_handler() {
        val session = newSession()
        session.dispatch(
            AgUiEvent.ToolCallStart(
                toolCallId = "call_1",
                toolCallName = "search",
                parentMessageId = "m1"
            )
        )
        session.dispatch(AgUiEvent.ToolCallArgs(toolCallId = "call_1", delta = "{\"q\":"))
        session.dispatch(AgUiEvent.ToolCallArgs(toolCallId = "call_1", delta = "\"kotlin\"}"))
        session.dispatch(AgUiEvent.ToolCallEnd(toolCallId = "call_1"))
        // 直到收到 RESULT 才产生消息
        assertEquals(0, session.messages.size)

        session.dispatch(
            AgUiEvent.ToolCallResult(
                messageId = "tool_msg_1",
                toolCallId = "call_1",
                content = "found 42 results"
            )
        )
        assertEquals(1, session.messages.size)
        val msg = session.messages[0]
        assertEquals(MessageType.SYSTEM, msg.type)
        assertTrue(msg.content.contains("search"))
        assertTrue(msg.content.contains("found 42 results"))
    }

    @Test
    fun tool_call_custom_result_handler_overrides_default() {
        val session = newSession()
        session.handlerRegistry.toolResultHandler =
            AgUiToolResultHandler { _, name, content, _ ->
                // 返回自定义 CUSTOM 类型消息
                com.tencent.kuiklybase.chat.model.ChatMessage(
                    id = "custom_result",
                    content = "[$name] $content",
                    isSelf = false,
                    type = MessageType.CUSTOM,
                    extra = mapOf("kind" to "tool")
                )
            }

        session.dispatch(
            AgUiEvent.ToolCallStart(toolCallId = "c1", toolCallName = "db_query")
        )
        session.dispatch(
            AgUiEvent.ToolCallResult(messageId = "", toolCallId = "c1", content = "OK")
        )

        assertEquals(1, session.messages.size)
        val msg = session.messages[0]
        assertEquals("custom_result", msg.id)
        assertEquals(MessageType.CUSTOM, msg.type)
        assertEquals("[db_query] OK", msg.content)
        assertEquals("tool", msg.extra["kind"])
    }

    @Test
    fun tool_call_start_handler_invoked() {
        val session = newSession()
        val trace = mutableListOf<String>()
        session.handlerRegistry.toolCallStartHandler =
            AgUiToolCallStartHandler { id, name, parentId, _ ->
                trace += "$name|$id|$parentId"
            }

        session.dispatch(
            AgUiEvent.ToolCallStart(
                toolCallId = "c1",
                toolCallName = "search",
                parentMessageId = "p1"
            )
        )
        assertEquals(listOf("search|c1|p1"), trace)
    }

    // ============================
    // 5. Activity 事件
    // ============================

    @Test
    fun activity_snapshot_default_creates_custom_message() {
        val session = newSession()
        session.dispatch(
            AgUiEvent.ActivitySnapshot(
                messageId = "act_1",
                activityType = "PLAN",
                content = mapOf("title" to "My Plan", "steps" to listOf("a", "b"))
            )
        )
        assertEquals(1, session.messages.size)
        val msg = session.messages[0]
        assertEquals("act_1", msg.id)
        assertEquals(MessageType.CUSTOM, msg.type)
        assertEquals("activity", msg.extra["agUiKind"])
        assertEquals("PLAN", msg.extra["activityType"])
        assertTrue(msg.content.contains("PLAN"))
        assertTrue(msg.content.contains("My Plan"))
    }

    @Test
    fun activity_snapshot_with_custom_handler_routes_to_handler() {
        val session = newSession()
        var handledCount = 0
        session.handlerRegistry.registerActivityHandler("PLAN") { event, _ ->
            handledCount++
            assertTrue(event is AgUiEvent.ActivitySnapshot || event is AgUiEvent.ActivityDelta)
        }

        session.dispatch(
            AgUiEvent.ActivitySnapshot(messageId = "a1", activityType = "PLAN", content = emptyMap())
        )
        session.dispatch(
            AgUiEvent.ActivityDelta(
                messageId = "a1",
                activityType = "PLAN",
                patch = listOf(mapOf("op" to "add", "path" to "/x", "value" to 1))
            )
        )
        assertEquals(2, handledCount)
        // Handler 没调用 upsertMessage，消息列表保持空
        assertEquals(0, session.messages.size)
    }

    @Test
    fun activity_snapshot_with_replace_false_skips_when_exists() {
        val session = newSession()
        // 第一次写入
        session.dispatch(
            AgUiEvent.ActivitySnapshot(
                messageId = "a1",
                activityType = "PLAN",
                content = mapOf("title" to "First")
            )
        )
        val firstContent = session.messages[0].content
        // replace=false 且 messageId 已存在 —— 应跳过
        session.dispatch(
            AgUiEvent.ActivitySnapshot(
                messageId = "a1",
                activityType = "PLAN",
                content = mapOf("title" to "Second"),
                replace = false
            )
        )
        assertEquals(firstContent, session.messages[0].content)
    }

    // ============================
    // 6. Custom / Meta 事件路由
    // ============================

    @Test
    fun custom_event_routes_to_registered_handler() {
        val session = newSession()
        var received: AgUiEvent.Custom? = null
        session.handlerRegistry.registerCustomHandler("voice_tip") { event, _ ->
            received = event
        }
        session.dispatch(AgUiEvent.Custom(name = "voice_tip", value = "ping"))

        assertNotNull(received)
        assertEquals("voice_tip", received!!.name)
        assertEquals("ping", received!!.value)
    }

    @Test
    fun custom_event_with_no_handler_is_silently_ignored() {
        val session = newSession()
        session.dispatch(AgUiEvent.Custom(name = "unhandled", value = "x"))
        // 不应产生任何消息或异常
        assertEquals(0, session.messages.size)
    }

    @Test
    fun meta_event_routes_by_meta_type() {
        val session = newSession()
        val trace = mutableListOf<String>()
        session.handlerRegistry.registerMetaHandler("thumbs_up") { event, _ ->
            trace += "up:${event.payload["messageId"]}"
        }
        session.handlerRegistry.registerMetaHandler("thumbs_down") { event, _ ->
            trace += "down:${event.payload["messageId"]}"
        }

        session.dispatch(
            AgUiEvent.Meta(metaType = "thumbs_up", payload = mapOf("messageId" to "m1"))
        )
        session.dispatch(
            AgUiEvent.Meta(metaType = "thumbs_down", payload = mapOf("messageId" to "m2"))
        )
        assertEquals(listOf("up:m1", "down:m2"), trace)
    }

    @Test
    fun unknown_event_routes_to_unknown_handler() {
        val session = newSession()
        var received: AgUiEvent.Unknown? = null
        session.handlerRegistry.unknownEventHandler =
            AgUiUnknownEventHandler { e, _ -> received = e }

        session.dispatch(
            AgUiEvent.Unknown(type = "FUTURE", body = mapOf("k" to "v"))
        )
        assertNotNull(received)
        assertEquals("FUTURE", received!!.type)
    }

    // ============================
    // 7. 状态管理
    // ============================

    @Test
    fun state_snapshot_replaces_entire_state() {
        val session = newSession()
        session.dispatch(AgUiEvent.StateSnapshot(snapshot = mapOf("a" to 1, "b" to 2)))
        assertEquals(1, session.getState()["a"])
        assertEquals(2, session.getState()["b"])

        session.dispatch(AgUiEvent.StateSnapshot(snapshot = mapOf("c" to 3)))
        // 旧键被清掉
        assertNull(session.getState()["a"])
        assertEquals(3, session.getState()["c"])
    }

    @Test
    fun state_delta_applies_add_replace_remove() {
        val session = newSession()
        session.dispatch(AgUiEvent.StateSnapshot(snapshot = mapOf("counter" to 1)))
        session.dispatch(
            AgUiEvent.StateDelta(
                delta = listOf(
                    mapOf("op" to "replace", "path" to "/counter", "value" to 5),
                    mapOf("op" to "add", "path" to "/flag", "value" to true),
                    mapOf("op" to "remove", "path" to "/stale")  // 不存在的键 remove 应无副作用
                )
            )
        )
        assertEquals(5, session.getState()["counter"])
        assertEquals(true, session.getState()["flag"])
    }

    @Test
    fun state_change_handler_invoked_on_both_snapshot_and_delta() {
        val session = newSession()
        val trace = mutableListOf<Boolean>()
        session.handlerRegistry.stateChangeHandler =
            AgUiStateChangeHandler { _, isDelta, _ -> trace += isDelta }

        session.dispatch(AgUiEvent.StateSnapshot(snapshot = mapOf("a" to 1)))
        session.dispatch(AgUiEvent.StateDelta(delta = emptyList()))
        assertEquals(listOf(false, true), trace)
    }

    // ============================
    // 8. MessagesSnapshot 覆盖式快照
    // ============================

    @Test
    fun messages_snapshot_replaces_entire_message_list() {
        val session = newSession()
        // 先有一些消息
        session.dispatch(AgUiEvent.TextMessageStart(messageId = "m1", role = AgUiRole.ASSISTANT))
        session.dispatch(AgUiEvent.TextMessageContent(messageId = "m1", delta = "old"))
        session.dispatch(AgUiEvent.TextMessageEnd(messageId = "m1"))
        assertEquals(1, session.messages.size)

        // Snapshot 覆盖
        session.dispatch(
            AgUiEvent.MessagesSnapshot(
                messages = listOf(
                    AgUiMessage(id = "u1", role = AgUiRole.USER, content = "Hi"),
                    AgUiMessage(id = "a1", role = AgUiRole.ASSISTANT, content = "Hello"),
                    AgUiMessage(id = "s1", role = AgUiRole.SYSTEM, content = "System notice")
                )
            )
        )
        assertEquals(3, session.messages.size)
        assertEquals("Hi", session.messages[0].content)
        assertTrue(session.messages[0].isSelf)
        assertEquals("Hello", session.messages[1].content)
        assertFalse(session.messages[1].isSelf)
        assertEquals(MessageType.SYSTEM, session.messages[2].type)
    }

    // ============================
    // 9. 错误处理
    // ============================

    @Test
    fun run_error_marks_pending_messages_as_failed() {
        val session = newSession()
        var errorMessage = ""
        var errorCode = ""
        session.onRunError = { m, c -> errorMessage = m; errorCode = c }

        session.dispatch(AgUiEvent.RunStarted(threadId = "t1", runId = "r1"))
        session.dispatch(AgUiEvent.TextMessageStart(messageId = "m1", role = AgUiRole.ASSISTANT))
        session.dispatch(AgUiEvent.TextMessageContent(messageId = "m1", delta = "partial"))
        // 未收到 END 就报错
        session.dispatch(AgUiEvent.RunError(message = "timeout", code = "504"))

        assertFalse(session.isRunning)
        assertEquals("timeout", session.lastError)
        assertEquals("timeout", errorMessage)
        assertEquals("504", errorCode)

        val msg = session.messages[0]
        assertEquals(MessageStatus.FAILED, msg.status)
    }

    @Test
    fun run_finished_closes_all_open_streams() {
        val session = newSession()
        session.dispatch(AgUiEvent.RunStarted(threadId = "t1", runId = "r1"))
        session.dispatch(AgUiEvent.TextMessageStart(messageId = "m1", role = AgUiRole.ASSISTANT))
        session.dispatch(AgUiEvent.TextMessageContent(messageId = "m1", delta = "Hello"))
        // 没发 END 就 RunFinished —— 状态机应自动闭合
        session.dispatch(AgUiEvent.RunFinished(threadId = "t1", runId = "r1"))

        assertEquals(MessageStatus.SENT, session.messages[0].status)
    }

    // ============================
    // 10. reset()
    // ============================

    @Test
    fun reset_clears_all_state() {
        val session = newSession()
        session.dispatch(AgUiEvent.RunStarted(threadId = "t1", runId = "r1"))
        session.dispatch(AgUiEvent.StateSnapshot(snapshot = mapOf("x" to 1)))
        session.dispatch(AgUiEvent.TextMessageStart(messageId = "m1", role = AgUiRole.ASSISTANT))
        session.dispatch(AgUiEvent.TextMessageContent(messageId = "m1", delta = "hi"))

        session.reset()

        assertEquals(0, session.messages.size)
        assertEquals(0, session.getState().size)
        assertEquals("", session.currentRunId)
        assertFalse(session.isRunning)
    }

    // ============================
    // 11. upsertMessage / removeMessage 公开 API
    // ============================

    @Test
    fun upsert_message_adds_new_and_updates_existing() {
        val session = newSession()
        val msg1 = com.tencent.kuiklybase.chat.model.ChatMessage(
            id = "m1", content = "v1", isSelf = false
        )
        session.upsertMessage(msg1)
        assertEquals("v1", session.messages[0].content)

        val msg1updated = msg1.copy(content = "v2")
        session.upsertMessage(msg1updated)
        assertEquals(1, session.messages.size)
        assertEquals("v2", session.messages[0].content)
    }

    @Test
    fun remove_message_removes_by_id() {
        val session = newSession()
        session.upsertMessage(
            com.tencent.kuiklybase.chat.model.ChatMessage(id = "m1", content = "a", isSelf = false)
        )
        session.upsertMessage(
            com.tencent.kuiklybase.chat.model.ChatMessage(id = "m2", content = "b", isSelf = false)
        )
        session.removeMessage("m1")
        assertEquals(1, session.messages.size)
        assertEquals("m2", session.messages[0].id)
    }

    // ============================
    // 12. dispatchJson / dispatchSseData 便捷扩展
    // ============================

    @Test
    fun dispatchJson_parses_and_dispatches() {
        val session = newSession()
        val event = session.dispatchJson(
            """{"type":"TEXT_MESSAGE_START","messageId":"m1","role":"assistant"}"""
        )
        assertNotNull(event)
        assertEquals(1, session.messages.size)
    }

    @Test
    fun dispatchJson_returns_null_for_invalid_input() {
        val session = newSession()
        assertNull(session.dispatchJson("invalid"))
        assertNull(session.dispatchJson(""))
        assertEquals(0, session.messages.size)
    }

    @Test
    fun dispatchSseData_skips_done_marker() {
        val session = newSession()
        val event = session.dispatchSseData("[DONE]")
        assertNull(event)
        assertEquals(0, session.messages.size)
    }
}
