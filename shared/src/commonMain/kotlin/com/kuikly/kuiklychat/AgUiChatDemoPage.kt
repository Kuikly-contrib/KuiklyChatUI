package com.kuikly.kuiklychat

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.*
import com.kuikly.kuiklychat.base.BasePager
import com.tencent.kuiklybase.chat.agui.*
import com.tencent.kuiklybase.chat.model.ChatMessage
import com.tencent.kuiklybase.chat.model.ChatMessageHelper
import com.tencent.kuiklybase.chat.model.MessageStatus
import com.tencent.kuiklybase.chat.model.MessageType
import com.tencent.kuiklybase.chat.bubble.defaultMessageRenderers
import com.tencent.kuiklybase.chat.session.ChatSession
import com.tencent.kuiklybase.chat.session.ChatSessionConfig

/**
 * AG-UI 协议 Demo 页面
 *
 * 演示内容：
 * 1. 基础文本流：TEXT_MESSAGE_START → TEXT_MESSAGE_CONTENT × N → TEXT_MESSAGE_END
 * 2. Chunk 便捷模式：TEXT_MESSAGE_CHUNK 持续追加
 * 3. 工具调用：TOOL_CALL_START → TOOL_CALL_ARGS × N → TOOL_CALL_END → TOOL_CALL_RESULT
 * 4. Activity 事件：ACTIVITY_SNAPSHOT + ACTIVITY_DELTA（通过自定义 Handler 渲染为卡片）
 * 5. Meta 事件：thumbs_up / thumbs_down（用户反馈）
 * 6. Custom 事件：自定义业务事件扩展
 * 7. Run 生命周期：RUN_STARTED / STEP_STARTED / STEP_FINISHED / RUN_FINISHED
 *
 * 使用方式：点击顶部按钮触发不同的事件流模拟。
 */
@Page("agui_demo", supportInLocal = true)
internal class AgUiChatDemoPage : BasePager() {

    /** AG-UI 会话对象 —— 直接驱动 ChatSession */
    private val session = AgUiSession(
        threadId = "demo_thread_001",
        selfSenderId = "user_001",
        aiSenderName = "AG-UI Agent",
        aiAvatarUrl = "https://vfiles.gtimg.cn/wuji_dashboard/wupload/xy/starter/62394e19.png"
    )

    /** ChatSession 的配置引用，便于后续调用滚动等 API */
    private var chatConfig: ChatSessionConfig? = null

    /** 顶部运行状态显示 */
    private var statusText by observable("空闲中...")

    /** 累计收到的事件数（用于调试） */
    private var eventCount by observable(0)

    /** 当前是否有流程正在运行，用于禁用按钮防止并发 */
    private var isRunning by observable(false)

    // ---- 事件时间轴 ----
    /** 最近事件列表（右下角浮窗展示） */
    private var eventLog by observable<List<AgUiEventLogItem>>(emptyList())

    /** 时间轴是否展开 */
    private var timelineExpanded by observable(true)

    // ---- Meta Toast 弹出提示 ----
    /** Toast 可见性 */
    private var toastVisible by observable(false)

    /** Toast 文字 */
    private var toastText by observable("")

    /** Toast 图标 */
    private var toastIcon by observable("👍")

    /** Toast 自动关闭的定时器 */
    private var toastTimerSeq = 0

    /** Demo 按钮与对应的触发逻辑 */
    private val demoActions: List<DemoAction> by lazy {
        listOf(
            DemoAction("💬 基础文本流", 0xFF48BB78L, 0xFF38A169L, ::demoBasicTextStream),
            DemoAction("⚡ Chunk 流", 0xFF4299E1L, 0xFF3182CEL, ::demoChunkStream),
            DemoAction("🔧 工具调用", 0xFF9F7AEAL, 0xFF805AD5L, ::demoToolCall),
            DemoAction("📋 Activity", 0xFF63B3EDL, 0xFF3182CEL, ::demoActivity),
            DemoAction("🧠 Reasoning", 0xFFA0AEC0L, 0xFF718096L, ::demoReasoning),
            DemoAction("👍 Meta 反馈", 0xFFF687B3L, 0xFFD53F8CL, ::demoMeta),
            DemoAction("❌ 错误模拟", 0xFFFC8181L, 0xFFE53E3EL, ::demoError),
            DemoAction("🔄 重置", 0xFF718096L, 0xFF4A5568L, ::resetAll)
        )
    }

    override fun created() {
        super.created()
        setupSession()
        // 初始欢迎消息
        session.messages.add(
            ChatMessageHelper.createSystemMessage(
                content = "欢迎使用 AG-UI 协议 Demo！点击上方按钮模拟不同类型的 Agent 事件流。"
            )
        )
    }

    /**
     * 注册自定义事件处理器
     *
     * 演示 AG-UI 协议的扩展点：Activity / Custom / Meta / Tool Result
     */
    private fun setupSession() {
        // 生命周期回调 — 更新顶部状态条
        session.onRunStarted = { runId ->
            statusText = "🚀 Run 启动：$runId"
            isRunning = true
        }
        session.onRunFinished = { runId ->
            statusText = "✅ Run 完成：$runId"
            isRunning = false
        }
        session.onRunError = { message, code ->
            statusText = "❌ Run 失败：$message"
            isRunning = false
        }
        session.onStepStarted = { stepName ->
            statusText = "🔄 Step 运行：$stepName"
        }
        session.onStepFinished = { stepName ->
            statusText = "✔️ Step 完成：$stepName"
        }

        // 注册 "PLAN" Activity 的自定义渲染 —— 将 PLAN 展示为阶段性任务卡片
        session.handlerRegistry.registerActivityHandler("PLAN") { event, session ->
            when (event) {
                is AgUiEvent.ActivitySnapshot -> {
                    val title = event.content["title"]?.toString() ?: "任务规划"
                    val steps = event.content["steps"] as? List<*>
                    val stepCount = steps?.size ?: 0
                    session.upsertMessage(
                        ChatMessageHelper.createCustomMessage(
                            content = "[PLAN] $title",
                            isSelf = false,
                            senderName = "AG-UI Agent",
                            senderAvatar = "",
                            senderId = "ai",
                            extra = mapOf(
                                "agUiKind" to "activity",
                                "activityType" to "PLAN",
                                "stepCount" to stepCount.toString()
                            )
                        ).copy(id = event.messageId)
                    )
                }
                is AgUiEvent.ActivityDelta -> {
                    // 累积 patch 计数到 extra，让 ActivityRenderer 能渲染进度条
                    val idx = session.messages.indexOfFirst { it.id == event.messageId }
                    if (idx >= 0) {
                        val current = session.messages[idx]
                        val newExtra = current.extra.toMutableMap()
                        // 使用单调递增的 patch 序号（当前已有数量 + 此次数量）
                        val existingPatches = newExtra.keys.count { it.startsWith("activity.patch.") }
                        event.patch.forEachIndexed { i, op ->
                            newExtra["activity.patch.${existingPatches + i}"] = op.toString()
                        }
                        session.messages[idx] = current.copy(extra = newExtra)
                    }
                }
                else -> Unit
            }
        }

        // 注册 Custom 事件处理器 —— 处理业务自定义的 "voice_tip" 事件
        session.handlerRegistry.registerCustomHandler("voice_tip") { event, session ->
            val text = event.value?.toString().orEmpty()
            session.upsertMessage(
                ChatMessageHelper.createSystemMessage(content = "🎙️ 语音提示：$text")
            )
        }

        // 注册 Meta 事件处理器 —— 处理用户反馈，弹出 Toast 动画
        session.handlerRegistry.registerMetaHandler("thumbs_up") { event, _ ->
            val messageId = event.payload["messageId"]?.toString().orEmpty()
            showToast("👍", "用户点赞了：$messageId")
        }
        session.handlerRegistry.registerMetaHandler("thumbs_down") { event, _ ->
            val messageId = event.payload["messageId"]?.toString().orEmpty()
            showToast("👎", "用户点踩了：$messageId")
        }

        // 注册工具结果自定义展示 —— 以更紧凑的卡片样式显示
        session.handlerRegistry.toolResultHandler =
            AgUiToolResultHandler { toolCallId, toolCallName, content, _ ->
                ChatMessageHelper.createCustomMessage(
                    content = "🔧 调用 $toolCallName → $content",
                    isSelf = false,
                    senderName = "AG-UI Agent",
                    senderId = "ai",
                    extra = mapOf(
                        "agUiKind" to "tool_result",
                        "toolCallId" to toolCallId,
                        "toolCallName" to toolCallName
                    )
                ).copy(id = "tool_result_$toolCallId")
            }

        // 未知事件打日志兜底
        session.handlerRegistry.unknownEventHandler =
            AgUiUnknownEventHandler { event, _ ->
                statusText = "⚠️ 未知事件类型：${event.type}"
            }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    flex(1f)
                    flexDirection(FlexDirection.COLUMN)
                }

                // ===== 顶部状态条 =====
                View {
                    attr {
                        paddingTop(ctx.pagerData.statusBarHeight)
                        backgroundColor(Color(0xFF6C5CE7))
                    }
                    View {
                        attr {
                            height(44f)
                            flexDirectionRow()
                            allCenter()
                            paddingLeft(12f)
                            paddingRight(12f)
                        }
                        // 返回按钮
                        View {
                            attr {
                                positionAbsolute()
                                left(12f)
                                size(24f, 24f)
                                allCenter()
                            }
                            Text {
                                attr {
                                    text("←")
                                    fontSize(20f)
                                    color(Color.WHITE)
                                }
                            }
                            event {
                                click {
                                    ctx.acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
                                }
                            }
                        }
                        Text {
                            attr {
                                text("AG-UI 协议 Demo")
                                fontSize(17f)
                                fontWeightSemisolid()
                                color(Color.WHITE)
                            }
                        }
                    }

                    // 状态信息条
                    View {
                        attr {
                            height(36f)
                            flexDirectionRow()
                            paddingLeft(12f)
                            paddingRight(12f)
                            alignItemsCenter()
                            backgroundColor(Color(0x33000000))
                        }
                        // 运行中指示灯（红点/绿点）
                        View {
                            attr {
                                size(8f, 8f)
                                borderRadius(4f)
                                marginRight(6f)
                                backgroundColor(
                                    if (ctx.isRunning) Color(0xFF48BB78)  // 运行中：绿色
                                    else Color(0xFFA0AEC0)                 // 空闲：灰色
                                )
                            }
                        }
                        Text {
                            attr {
                                text(ctx.statusText)
                                fontSize(12f)
                                color(Color.WHITE)
                                flex(1f)
                            }
                        }
                        // 事件数徽章
                        View {
                            attr {
                                borderRadius(10f)
                                backgroundColor(Color(0xFFFBD38D))
                                paddingLeft(8f)
                                paddingRight(8f)
                                paddingTop(2f)
                                paddingBottom(2f)
                                allCenter()
                            }
                            Text {
                                attr {
                                    text("📡 ${ctx.eventCount}")
                                    fontSize(11f)
                                    color(Color(0xFF744210))
                                    fontWeightSemiBold()
                                }
                            }
                        }
                    }
                }

                // ===== 控制按钮区 =====
                Scroller {
                    attr {
                        flexDirectionRow()
                        height(56f)
                        paddingLeft(8f)
                        paddingRight(8f)
                        backgroundColor(Color(0xFFF7FAFC))
                    }
                    ctx.demoActions.forEach { action ->
                        View {
                            attr {
                                alignSelfCenter()
                                padding(8f, 14f, 8f, 14f)
                                marginLeft(4f)
                                marginRight(4f)
                                borderRadius(18f)
                                if (ctx.isRunning && !action.label.contains("重置")) {
                                    // 运行中禁用态：灰色
                                    backgroundColor(Color(0xFFCBD5E0))
                                } else {
                                    // 可用态：渐变色
                                    backgroundLinearGradient(
                                        Direction.TO_BOTTOM_RIGHT,
                                        ColorStop(Color(action.colorStart), 0f),
                                        ColorStop(Color(action.colorEnd), 1f)
                                    )
                                    boxShadow(BoxShadow(0f, 2f, 4f, Color(0x33000000)))
                                }
                            }
                            Text {
                                attr {
                                    text(action.label)
                                    fontSize(13f)
                                    color(Color.WHITE)
                                    fontWeightSemiBold()
                                }
                            }
                            event {
                                click {
                                    if (!ctx.isRunning || action.label.contains("重置")) {
                                        action.trigger()
                                    }
                                }
                            }
                        }
                    }
                }

                // ===== 消息列表 =====
                View {
                    attr {
                        flex(1f)
                    }
                    // DEBUG: 显示当前消息数量（调试用，确认消息是否被正确添加）
                    Text {
                        attr {
                            text("DEBUG: messages.size = ${ctx.session.messages.size}")
                            fontSize(10f)
                            color(Color(0xFFFF0000))
                            positionAbsolute()
                            top(4f)
                            left(4f)
                        }
                    }
                    ChatSession({ ctx.session.observableMessages }) {
                        title = ""
                        showNavigationBar = false
                        selfAvatarUrl = ""

                        theme {
                            primaryColor = 0xFF6C5CE7
                            primaryGradientEndColor = 0xFFA29BFE
                            otherBubbleColor = 0xFFFFFFFF
                            backgroundColor = 0xFFF0F2F5
                        }

                        // 注册默认内置渲染器（文本 / 图片 / 系统 / 视频 / 文件）
                        messageRenderers.addAll(defaultMessageRenderers())
                        // 注册 AG-UI 专属渲染器 —— 让不同事件类型有差异化视觉
                        messageRenderers.add(ToolResultRenderer())
                        messageRenderers.add(ActivityRenderer())
                        messageRenderers.add(ReasoningRenderer())

                        messageListOptions {
                            autoScrollToBottom = true
                            enableMessageGrouping = true
                        }

                        ctx.chatConfig = this
                    }
                }

                // ===== 事件时间轴浮窗（右下角）=====
                AgUiEventTimeline(
                    items = { ctx.eventLog },
                    expanded = { ctx.timelineExpanded },
                    onToggle = { ctx.timelineExpanded = !ctx.timelineExpanded }
                )

                // ===== Meta Toast 悬浮提示 =====
                vif({ ctx.toastVisible }) {
                    View {
                        attr {
                            positionAbsolute()
                            top(ctx.pagerData.statusBarHeight + 92f)
                            left(0f)
                            right(0f)
                            alignItemsCenter()
                        }
                        View {
                            attr {
                                flexDirectionRow()
                                alignItemsCenter()
                                paddingLeft(16f)
                                paddingRight(16f)
                                paddingTop(10f)
                                paddingBottom(10f)
                                borderRadius(20f)
                                backgroundColor(Color(0xE6000000))
                                boxShadow(BoxShadow(0f, 4f, 12f, Color(0x66000000)))
                            }
                            Text {
                                attr {
                                    text(ctx.toastIcon)
                                    fontSize(16f)
                                    marginRight(8f)
                                }
                            }
                            Text {
                                attr {
                                    text(ctx.toastText)
                                    fontSize(13f)
                                    color(Color.WHITE)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ============================
    // 事件分发（统一计数 + 时间轴记录）
    // ============================

    private fun dispatch(event: AgUiEvent) {
        session.dispatch(event)
        eventCount++
        // 记录到时间轴
        val summary = summarizeEvent(event)
        eventLog = (eventLog + AgUiEventLogItem(
            seq = eventCount,
            type = event.type,
            summary = summary,
            timestamp = event.timestamp
        )).takeLast(20)
    }

    /** 为事件构造简短摘要（时间轴展示用） */
    private fun summarizeEvent(event: AgUiEvent): String = when (event) {
        is AgUiEvent.RunStarted -> event.runId
        is AgUiEvent.RunFinished -> event.runId
        is AgUiEvent.RunError -> event.message.take(20)
        is AgUiEvent.StepStarted -> event.stepName
        is AgUiEvent.StepFinished -> event.stepName
        is AgUiEvent.TextMessageStart -> event.messageId
        is AgUiEvent.TextMessageContent -> "+${event.delta.length}ch"
        is AgUiEvent.TextMessageEnd -> event.messageId
        is AgUiEvent.TextMessageChunk -> "+${event.delta.length}ch"
        is AgUiEvent.ToolCallStart -> event.toolCallName
        is AgUiEvent.ToolCallArgs -> "args+${event.delta.length}ch"
        is AgUiEvent.ToolCallEnd -> event.toolCallId
        is AgUiEvent.ToolCallResult -> event.content.take(18)
        is AgUiEvent.ToolCallChunk -> event.toolCallName
        is AgUiEvent.StateSnapshot -> "${event.snapshot.size} keys"
        is AgUiEvent.StateDelta -> "${event.delta.size} patches"
        is AgUiEvent.MessagesSnapshot -> "${event.messages.size} msgs"
        is AgUiEvent.ActivitySnapshot -> event.activityType
        is AgUiEvent.ActivityDelta -> "${event.activityType} +${event.patch.size}"
        is AgUiEvent.ReasoningStart -> event.messageId
        is AgUiEvent.ReasoningMessageStart -> event.messageId
        is AgUiEvent.ReasoningMessageContent -> "+${event.delta.length}ch"
        is AgUiEvent.ReasoningMessageEnd -> event.messageId
        is AgUiEvent.ReasoningEnd -> event.messageId
        is AgUiEvent.Raw -> event.source
        is AgUiEvent.Custom -> event.name
        is AgUiEvent.Meta -> event.metaType
        is AgUiEvent.Unknown -> event.type
    }

    /** 显示 Toast 动画（1800ms 后自动隐藏） */
    private fun showToast(icon: String, text: String) {
        toastIcon = icon
        toastText = text
        toastVisible = true
        toastTimerSeq++
        val mySeq = toastTimerSeq
        setTimeout(1800) {
            // 防止中间有新 toast 覆盖
            if (mySeq == toastTimerSeq) {
                toastVisible = false
            }
        }
    }

    private fun newRunId(): String = "run_${eventCount}_${demoActions.size}"

    // ============================
    // Demo 1：基础文本流
    // ============================

    private fun demoBasicTextStream() {
        if (isRunning) return
        val runId = newRunId()
        val messageId = "msg_text_$eventCount"

        // 模拟用户发消息：手动追加一条 user 消息（实际项目中会由输入框触发）
        session.messages.add(
            ChatMessage(
                id = "user_$eventCount",
                content = "请介绍一下 AG-UI 协议？",
                isSelf = true,
                senderName = "我",
                senderId = session.selfSenderId,
                status = MessageStatus.SENT,
                type = MessageType.TEXT
            )
        )

        dispatch(AgUiEvent.RunStarted(threadId = session.threadId, runId = runId))
        dispatch(AgUiEvent.StepStarted(stepName = "generate_text"))
        dispatch(AgUiEvent.TextMessageStart(messageId = messageId, role = AgUiRole.ASSISTANT))

        val chunks = listOf(
            "AG-UI（**Agent–User Interaction**）是 ",
            "一个**轻量级、事件驱动**的协议，",
            "专门为连接 AI Agent ",
            "与前端应用而设计。\n\n",
            "核心特性：\n",
            "- 16 种标准化事件类型\n",
            "- 传输层无关（SSE / WebSocket / HTTP 均可）\n",
            "- 支持流式文本、工具调用、Activity 等多场景",
        )
        scheduleChunks(
            chunks,
            intervalMs = 250,
            each = { index ->
                dispatch(
                    AgUiEvent.TextMessageContent(
                        messageId = messageId,
                        delta = chunks[index]
                    )
                )
            },
            onFinish = {
                dispatch(AgUiEvent.TextMessageEnd(messageId = messageId))
                dispatch(AgUiEvent.StepFinished(stepName = "generate_text"))
                dispatch(AgUiEvent.RunFinished(threadId = session.threadId, runId = runId))
            }
        )
    }

    // ============================
    // Demo 2：Chunk 便捷模式
    // ============================

    private fun demoChunkStream() {
        if (isRunning) return
        val runId = newRunId()
        val messageId = "msg_chunk_$eventCount"

        dispatch(AgUiEvent.RunStarted(threadId = session.threadId, runId = runId))

        val chunks = listOf(
            "这是 Chunk 模式：",
            "首 chunk 必须带 `messageId`，",
            "之后可以省略，",
            "流结束时自动闭合。",
        )
        // 首 chunk 带 messageId + role
        dispatch(
            AgUiEvent.TextMessageChunk(
                messageId = messageId,
                role = AgUiRole.ASSISTANT,
                delta = chunks[0]
            )
        )
        scheduleChunks(
            chunks,
            intervalMs = 300,
            startIndex = 1,
            each = { index ->
                dispatch(AgUiEvent.TextMessageChunk(delta = chunks[index]))
            },
            onFinish = {
                dispatch(AgUiEvent.RunFinished(threadId = session.threadId, runId = runId))
            }
        )
    }

    // ============================
    // Demo 3：工具调用
    // ============================

    private fun demoToolCall() {
        if (isRunning) return
        val runId = newRunId()
        val messageId = "msg_tool_$eventCount"
        val toolCallId = "call_$eventCount"

        dispatch(AgUiEvent.RunStarted(threadId = session.threadId, runId = runId))
        dispatch(
            AgUiEvent.TextMessageStart(messageId = messageId, role = AgUiRole.ASSISTANT)
        )
        dispatch(
            AgUiEvent.TextMessageContent(
                messageId = messageId,
                delta = "让我查询一下北京的天气。"
            )
        )
        dispatch(AgUiEvent.TextMessageEnd(messageId = messageId))

        // 工具调用序列
        dispatch(
            AgUiEvent.ToolCallStart(
                toolCallId = toolCallId,
                toolCallName = "query_weather",
                parentMessageId = messageId
            )
        )
        // 模拟参数分片流式到达
        dispatch(AgUiEvent.ToolCallArgs(toolCallId = toolCallId, delta = "{\"city\":"))
        setTimeout(200) {
            dispatch(AgUiEvent.ToolCallArgs(toolCallId = toolCallId, delta = "\"北京\""))
            setTimeout(200) {
                dispatch(AgUiEvent.ToolCallArgs(toolCallId = toolCallId, delta = ",\"unit\":\"celsius\"}"))
                setTimeout(300) {
                    dispatch(AgUiEvent.ToolCallEnd(toolCallId = toolCallId))
                    setTimeout(500) {
                        // 工具执行结果
                        dispatch(
                            AgUiEvent.ToolCallResult(
                                messageId = "tool_msg_$eventCount",
                                toolCallId = toolCallId,
                                content = "北京今天晴，22°C，微风"
                            )
                        )
                        setTimeout(400) {
                            // Assistant 根据工具结果继续回答
                            val followUpId = "msg_followup_$eventCount"
                            dispatch(
                                AgUiEvent.TextMessageStart(
                                    messageId = followUpId,
                                    role = AgUiRole.ASSISTANT
                                )
                            )
                            dispatch(
                                AgUiEvent.TextMessageContent(
                                    messageId = followUpId,
                                    delta = "北京今天天气不错，22°C，适合外出活动！🌞"
                                )
                            )
                            dispatch(AgUiEvent.TextMessageEnd(messageId = followUpId))
                            dispatch(
                                AgUiEvent.RunFinished(threadId = session.threadId, runId = runId)
                            )
                        }
                    }
                }
            }
        }
    }

    // ============================
    // Demo 4：Activity 事件
    // ============================

    private fun demoActivity() {
        if (isRunning) return
        val runId = newRunId()
        val activityId = "activity_$eventCount"

        dispatch(AgUiEvent.RunStarted(threadId = session.threadId, runId = runId))

        // 发送 PLAN 类型 Activity 快照
        dispatch(
            AgUiEvent.ActivitySnapshot(
                messageId = activityId,
                activityType = "PLAN",
                content = mapOf(
                    "title" to "完成用户需求的三步计划",
                    "steps" to listOf("分析需求", "编写代码", "测试验收")
                )
            )
        )

        // 模拟 3 次 Delta 更新（每次 800ms）
        setTimeout(800) {
            dispatch(
                AgUiEvent.ActivityDelta(
                    messageId = activityId,
                    activityType = "PLAN",
                    patch = listOf(mapOf("op" to "replace", "path" to "/steps/0/done", "value" to true))
                )
            )
            setTimeout(800) {
                dispatch(
                    AgUiEvent.ActivityDelta(
                        messageId = activityId,
                        activityType = "PLAN",
                        patch = listOf(mapOf("op" to "replace", "path" to "/steps/1/done", "value" to true))
                    )
                )
                setTimeout(800) {
                    dispatch(
                        AgUiEvent.ActivityDelta(
                            messageId = activityId,
                            activityType = "PLAN",
                            patch = listOf(mapOf("op" to "replace", "path" to "/steps/2/done", "value" to true))
                        )
                    )
                    dispatch(AgUiEvent.RunFinished(threadId = session.threadId, runId = runId))
                }
            }
        }
    }

    // ============================
    // Demo 5：Reasoning 推理消息
    // ============================

    private fun demoReasoning() {
        if (isRunning) return
        val runId = newRunId()
        val reasoningId = "reasoning_$eventCount"
        val answerId = "msg_answer_$eventCount"

        dispatch(AgUiEvent.RunStarted(threadId = session.threadId, runId = runId))

        dispatch(AgUiEvent.ReasoningMessageStart(messageId = reasoningId))
        val reasoningChunks = listOf(
            "用户询问数学问题，",
            "需要计算 17 × 23。",
            "拆分：17 × 20 = 340，17 × 3 = 51。",
            "求和：340 + 51 = 391。"
        )
        scheduleChunks(
            reasoningChunks,
            intervalMs = 300,
            each = { index ->
                dispatch(
                    AgUiEvent.ReasoningMessageContent(
                        messageId = reasoningId,
                        delta = reasoningChunks[index]
                    )
                )
            },
            onFinish = {
                dispatch(AgUiEvent.ReasoningMessageEnd(messageId = reasoningId))

                // 推理完成后给出正式答案
                setTimeout(300) {
                    dispatch(
                        AgUiEvent.TextMessageStart(messageId = answerId, role = AgUiRole.ASSISTANT)
                    )
                    dispatch(
                        AgUiEvent.TextMessageContent(
                            messageId = answerId,
                            delta = "17 × 23 = **391**"
                        )
                    )
                    dispatch(AgUiEvent.TextMessageEnd(messageId = answerId))
                    dispatch(AgUiEvent.RunFinished(threadId = session.threadId, runId = runId))
                }
            }
        )
    }

    // ============================
    // Demo 6：Meta 事件（用户反馈）
    // ============================

    private fun demoMeta() {
        if (isRunning) return
        // 先生成一条简单消息
        val runId = newRunId()
        val messageId = "msg_meta_$eventCount"
        dispatch(AgUiEvent.RunStarted(threadId = session.threadId, runId = runId))
        dispatch(AgUiEvent.TextMessageStart(messageId = messageId, role = AgUiRole.ASSISTANT))
        dispatch(
            AgUiEvent.TextMessageContent(
                messageId = messageId,
                delta = "这是一条等待用户反馈的消息，稍后会收到 thumbs_up 事件。"
            )
        )
        dispatch(AgUiEvent.TextMessageEnd(messageId = messageId))
        dispatch(AgUiEvent.RunFinished(threadId = session.threadId, runId = runId))

        // 模拟 Meta 事件到达（用户点赞）
        setTimeout(1000) {
            dispatch(
                AgUiEvent.Meta(
                    metaType = "thumbs_up",
                    payload = mapOf(
                        "messageId" to messageId,
                        "userId" to session.selfSenderId
                    )
                )
            )
        }

        // 再来一个自定义业务事件
        setTimeout(1500) {
            dispatch(
                AgUiEvent.Custom(
                    name = "voice_tip",
                    value = "检测到键盘输入，您也可以使用语音快速回复哦 🎙️"
                )
            )
        }
    }

    // ============================
    // Demo 7：错误模拟
    // ============================

    private fun demoError() {
        if (isRunning) return
        val runId = newRunId()
        val messageId = "msg_err_$eventCount"

        dispatch(AgUiEvent.RunStarted(threadId = session.threadId, runId = runId))
        dispatch(AgUiEvent.TextMessageStart(messageId = messageId, role = AgUiRole.ASSISTANT))
        dispatch(
            AgUiEvent.TextMessageContent(
                messageId = messageId,
                delta = "正在思考中，但是"
            )
        )

        setTimeout(600) {
            // 模拟 Agent 内部异常
            dispatch(
                AgUiEvent.RunError(
                    message = "LLM 服务超时，请稍后重试",
                    code = "LLM_TIMEOUT_504"
                )
            )
        }
    }

    // ============================
    // 重置
    // ============================

    private fun resetAll() {
        session.reset()
        session.messages.add(
            ChatMessageHelper.createSystemMessage("已重置会话。")
        )
        eventCount = 0
        statusText = "空闲中..."
        isRunning = false
        eventLog = emptyList()
        toastVisible = false
    }

    // ============================
    // 工具：异步调度分片序列
    // ============================

    /**
     * 按固定间隔逐个回调 [each]，全部回调完成后调用 [onFinish]。
     *
     * @param chunks 分片列表
     * @param intervalMs 每片回调的间隔（毫秒）
     * @param startIndex 起始索引（首片如已由调用方处理，可传 1）
     */
    private fun scheduleChunks(
        chunks: List<String>,
        intervalMs: Int,
        startIndex: Int = 0,
        each: (index: Int) -> Unit,
        onFinish: () -> Unit
    ) {
        fun step(i: Int) {
            if (i >= chunks.size) {
                onFinish()
                return
            }
            each(i)
            setTimeout(intervalMs) { step(i + 1) }
        }
        setTimeout(50) { step(startIndex) }
    }

    /** Demo 按钮动作 */
    private data class DemoAction(
        val label: String,
        val colorStart: Long,
        val colorEnd: Long,
        val trigger: () -> Unit
    )
}
