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
import com.tencent.kuiklybase.chat.bubble.TextMessageRenderer
import com.tencent.kuiklybase.chat.bubble.agUiRichRenderers
import com.tencent.kuiklybase.chat.model.ChatMessage
import com.tencent.kuiklybase.chat.model.ChatMessageHelper
import com.tencent.kuiklybase.chat.model.MessageStatus
import com.tencent.kuiklybase.chat.model.MessageType
import com.tencent.kuiklybase.chat.session.ChatSession
import com.tencent.kuiklybase.chat.session.ChatSessionConfig

/**
 * AI-Agent 富卡片 Demo 页面
 *
 * 演示新增的以下能力：
 * 1. **RichCardPayload**：类型安全的卡片构造器
 * 2. **Loading 状态体系**：会话级 / 消息级 / 步骤级
 * 3. **EventToCardMapper**：非标准 AG-UI 事件 → 自定义卡片的映射扩展
 * 4. **内置富卡片**：代码块 / 命令执行 / 文件编辑 / 工具调用 / 错误 / 建议
 */
@Page("agent_rich_card_demo", supportInLocal = true)
internal class AgentRichCardDemoPage : BasePager() {

    private val session = AgUiSession(
        threadId = "agent_rich_thread",
        selfSenderId = "user_001",
        aiSenderName = "Agent",
        aiAvatarUrl = "https://vfiles.gtimg.cn/wuji_dashboard/wupload/xy/starter/62394e19.png"
    )

    private var chatConfig: ChatSessionConfig? = null
    private var loadingLabel by observable("空闲中...")
    private var isLoading by observable(false)
    private var runCounter = 0

    private val demoActions: List<DemoAction> by lazy {
        listOf(
            DemoAction("📝 Markdown 回复", 0xFF10B981L, 0xFF059669L, ::demoMarkdown),
            DemoAction("💻 代码块卡片", 0xFF6366F1L, 0xFF4338CAL, ::demoCodeBlock),
            DemoAction("🖥️ 命令执行", 0xFF0F172AL, 0xFF334155L, ::demoCommandExec),
            DemoAction("✏️ 文件编辑", 0xFF8B5CF6L, 0xFF7C3AEDL, ::demoFileEdit),
            DemoAction("🔧 工具调用", 0xFF9F7AEAL, 0xFF805AD5L, ::demoToolCall),
            DemoAction("⏳ Loading 占位", 0xFF4F8FFFL, 0xFF3730A3L, ::demoLoading),
            DemoAction("❌ 错误卡片", 0xFFEF4444L, 0xFFDC2626L, ::demoError),
            DemoAction("💡 建议选项", 0xFF10B981L, 0xFF047857L, ::demoSuggestions),
            DemoAction("🔀 自定义事件", 0xFFF59E0BL, 0xFFD97706L, ::demoCustomMapper),
            DemoAction("📊 Markdown 拆分", 0xFF3B82F6L, 0xFF1D4ED8L, ::demoMarkdownSplit),
            DemoAction("🚀 综合演示", 0xFFE11D48L, 0xFFBE123CL, ::demoFullAgentFlow),
            DemoAction("🔄 重置", 0xFF64748BL, 0xFF475569L, ::resetAll)
        )
    }

    override fun created() {
        super.created()

        // 订阅 Loading 状态变化 → 更新顶部指示条
        session.onLoadingChanged { snapshot ->
            isLoading = snapshot.isActive
            loadingLabel = if (snapshot.isActive) snapshot.bestLabel else "空闲中..."
        }

        // 注册自定义事件 → 卡片映射器（演示非标准 AG-UI 事件接入）
        session.registerMapper(EventToCardMapper { event, _ ->
            if (event is AgUiEvent.Custom && event.name == "code_generation") {
                val map = event.value as? Map<*, *> ?: return@EventToCardMapper null
                RichCardPayload.codeBlock(
                    id = map["id"]?.toString() ?: "code_${event.timestamp}",
                    code = map["code"]?.toString().orEmpty(),
                    language = map["language"]?.toString().orEmpty(),
                    filename = map["filename"]?.toString().orEmpty(),
                    senderName = session.aiSenderName,
                    senderAvatar = session.aiAvatarUrl
                )
            } else null
        })

        session.registerMapper(EventToCardMapper { event, _ ->
            if (event is AgUiEvent.Custom && event.name == "file_operation") {
                val map = event.value as? Map<*, *> ?: return@EventToCardMapper null
                RichCardPayload.fileEdit(
                    id = map["id"]?.toString() ?: "file_${event.timestamp}",
                    filePath = map["path"]?.toString().orEmpty(),
                    operation = map["op"]?.toString() ?: FileOperation.UPDATE,
                    diffText = map["diff"]?.toString().orEmpty(),
                    additions = (map["additions"] as? Number)?.toInt() ?: 0,
                    deletions = (map["deletions"] as? Number)?.toInt() ?: 0,
                    senderName = session.aiSenderName,
                    senderAvatar = session.aiAvatarUrl
                )
            } else null
        })

        // 欢迎信息
        session.messages.add(
            ChatMessageHelper.createSystemMessage(
                "欢迎使用 AI-Agent 富卡片 Demo。\n点击上方按钮查看各种卡片与 Loading 效果。"
            )
        )
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    flex(1f)
                    flexDirection(FlexDirection.COLUMN)
                    backgroundColor(Color(0xFFF0F2F5))
                }

                // 顶部状态条
                View {
                    attr {
                        paddingTop(ctx.pagerData.statusBarHeight)
                        backgroundLinearGradient(
                            Direction.TO_RIGHT,
                            ColorStop(Color(0xFF6366F1), 0f),
                            ColorStop(Color(0xFF8B5CF6), 1f)
                        )
                    }
                    View {
                        attr {
                            height(44f)
                            flexDirectionRow()
                            allCenter()
                            paddingLeft(12f)
                            paddingRight(12f)
                        }
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
                                text("AI-Agent 富卡片 Demo")
                                fontSize(17f)
                                fontWeightSemisolid()
                                color(Color.WHITE)
                            }
                        }
                    }

                    // Loading 指示条
                    View {
                        attr {
                            height(32f)
                            flexDirectionRow()
                            alignItemsCenter()
                            paddingLeft(12f)
                            paddingRight(12f)
                            backgroundColor(Color(0x33000000))
                        }
                        View {
                            attr {
                                size(8f, 8f)
                                borderRadius(4f)
                                marginRight(6f)
                                backgroundColor(
                                    if (ctx.isLoading) Color(0xFFFBBF24)
                                    else Color(0xFFA0AEC0)
                                )
                            }
                        }
                        Text {
                            attr {
                                text(ctx.loadingLabel)
                                fontSize(12f)
                                color(Color.WHITE)
                                flex(1f)
                            }
                        }
                    }
                }

                // 控制按钮区
                Scroller {
                    attr {
                        flexDirectionRow()
                        height(56f)
                        paddingLeft(8f)
                        paddingRight(8f)
                        backgroundColor(Color.WHITE)
                    }
                    ctx.demoActions.forEach { action ->
                        View {
                            attr {
                                alignSelfCenter()
                                padding(8f, 14f, 8f, 14f)
                                marginLeft(4f)
                                marginRight(4f)
                                borderRadius(18f)
                                backgroundLinearGradient(
                                    Direction.TO_BOTTOM_RIGHT,
                                    ColorStop(Color(action.colorStart), 0f),
                                    ColorStop(Color(action.colorEnd), 1f)
                                )
                                boxShadow(BoxShadow(0f, 2f, 4f, Color(0x33000000)))
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
                                click { action.trigger() }
                            }
                        }
                    }
                }

                // 消息列表
                View {
                    attr { flex(1f) }
                    ChatSession({ ctx.session.observableMessages }) {
                        title = ""
                        showNavigationBar = false
                        selfAvatarUrl = ""

                        theme {
                            primaryColor = 0xFF6366F1
                            primaryGradientEndColor = 0xFF8B5CF6
                            backgroundColor = 0xFFF0F2F5
                        }

                        // 👇 开启所有 AG-UI 富卡片渲染 + 文本兜底
                        messageRenderers.addAll(
                            agUiRichRenderers(
                                onCopyCode = { /* 真实场景接入剪贴板 */ },
                                onSuggestionClick = { value ->
                                    // 点击建议 → 伪造一条 user 消息
                                    ctx.session.messages.add(
                                        ChatMessage(
                                            id = "user_sugg_${ctx.runCounter++}",
                                            content = value,
                                            isSelf = true,
                                            senderName = "我",
                                            senderId = ctx.session.selfSenderId,
                                            status = MessageStatus.SENT,
                                            type = MessageType.TEXT
                                        )
                                    )
                                }
                            )
                        )
                        messageRenderers.add(TextMessageRenderer())

                        messageListOptions {
                            autoScrollToBottom = true
                            enableMessageGrouping = true
                        }
                        ctx.chatConfig = this
                    }
                }
            }
        }
    }

    // ============================
    // Demo 1：Markdown 回复
    // ============================

    private fun demoMarkdown() {
        val id = "markdown_${runCounter++}"
        session.upsertMessage(
            RichCardPayload.markdown(
                id = id,
                markdown = """
                    # AI-Agent 响应
                    这是一条 **Markdown** 消息，支持：
                    - 列表项
                    - *斜体* 与 **粗体**
                    - `行内代码`
                """.trimIndent(),
                senderName = session.aiSenderName,
                senderAvatar = session.aiAvatarUrl
            )
        )
    }

    // ============================
    // Demo 2：代码块卡片
    // ============================

    private fun demoCodeBlock() {
        val id = "code_${runCounter++}"
        session.upsertMessage(
            RichCardPayload.codeBlock(
                id = id,
                filename = "Hello.kt",
                language = "kotlin",
                code = """
                    fun main() {
                        val greeting = "Hello, AG-UI!"
                        println(greeting)
                    }
                """.trimIndent(),
                senderName = session.aiSenderName,
                senderAvatar = session.aiAvatarUrl
            )
        )
    }

    // ============================
    // Demo 3：命令执行（状态流转演示）
    // ============================

    private fun demoCommandExec() {
        val id = "cmd_${runCounter++}"
        val command = "ls -la /Users/chen"
        val cwd = "/Users/chen"

        // 1. 初始：running 状态
        session.upsertMessage(
            RichCardPayload.commandExec(
                id = id,
                command = command,
                cwd = cwd,
                state = CommandState.RUNNING,
                senderName = session.aiSenderName,
                senderAvatar = session.aiAvatarUrl
            )
        )

        // 2. 模拟 1.5s 后输出完成
        setTimeout(1500) {
            session.upsertMessage(
                RichCardPayload.commandExec(
                    id = id,
                    command = command,
                    cwd = cwd,
                    state = CommandState.SUCCESS,
                    output = """
                        total 16
                        drwx------+ 68 chen staff 2176 Apr 23 20:30 .
                        drwxr-xr-x   6 root staff  192 Jan  1 08:00 ..
                        drwx------   5 chen staff  160 Apr 20 14:12 Desktop
                        drwx------+ 12 chen staff  384 Apr 22 09:00 Documents
                    """.trimIndent(),
                    exitCode = 0,
                    durationMs = 1487,
                    senderName = session.aiSenderName,
                    senderAvatar = session.aiAvatarUrl
                )
            )
        }
    }

    // ============================
    // Demo 4：文件编辑
    // ============================

    private fun demoFileEdit() {
        session.upsertMessage(
            RichCardPayload.fileEdit(
                id = "file_${runCounter++}",
                filePath = "src/commonMain/kotlin/MyAgent.kt",
                operation = FileOperation.UPDATE,
                additions = 12,
                deletions = 3,
                diffText = """
                    @@ -10,3 +10,12 @@
                    -fun handle() {}
                    +fun handle(event: Event) {
                    +    when (event) {
                    +        is Event.Started -> start()
                    +        is Event.Finished -> finish()
                    +    }
                    +}
                """.trimIndent(),
                senderName = session.aiSenderName,
                senderAvatar = session.aiAvatarUrl
            )
        )
    }

    // ============================
    // Demo 5：工具调用（使用标准 AG-UI 事件）
    // ============================

    private fun demoToolCall() {
        val runId = "run_${runCounter++}"
        val toolCallId = "call_$runId"
        session.dispatch(AgUiEvent.RunStarted(threadId = session.threadId, runId = runId))

        // pending 卡片
        session.upsertMessage(
            RichCardPayload.toolCallPending(
                id = "tool_pending_$toolCallId",
                toolName = "query_weather",
                toolCallId = toolCallId,
                args = """{"city":"Beijing"}""",
                senderName = session.aiSenderName,
                senderAvatar = session.aiAvatarUrl
            )
        )
        session.dispatch(
            AgUiEvent.ToolCallStart(
                toolCallId = toolCallId,
                toolCallName = "query_weather"
            )
        )

        setTimeout(1200) {
            session.dismissLoading("tool_pending_$toolCallId")
            session.upsertMessage(
                RichCardPayload.toolResult(
                    id = "tool_result_$toolCallId",
                    toolName = "query_weather",
                    toolCallId = toolCallId,
                    result = "Beijing · 晴 · 22°C · 微风",
                    senderName = session.aiSenderName,
                    senderAvatar = session.aiAvatarUrl
                )
            )
            session.dispatch(
                AgUiEvent.ToolCallResult(
                    messageId = "tool_result_$toolCallId",
                    toolCallId = toolCallId,
                    content = "Beijing · 晴 · 22°C · 微风"
                )
            )
            session.dispatch(AgUiEvent.RunFinished(threadId = session.threadId, runId = runId))
        }
    }

    // ============================
    // Demo 6：Loading 占位（三种风格 + 自动消失）
    // ============================

    private fun demoLoading() {
        session.showLoading(
            id = "loading_dots",
            title = "正在思考",
            subtitle = "正在分析你的需求",
            style = LoadingStyle.DOTS
        )
        setTimeout(800) {
            session.showLoading(
                id = "loading_spinner",
                title = "正在搜索文件",
                subtitle = "匹配 *.kt",
                style = LoadingStyle.SPINNER,
                target = "src/commonMain"
            )
        }
        setTimeout(1600) {
            session.showLoading(
                id = "loading_progress",
                title = "正在下载依赖",
                subtitle = "kuikly-open:core 2.7.0",
                style = LoadingStyle.PROGRESS
            )
        }
        setTimeout(2500) {
            session.dismissLoading("loading_dots")
            session.dismissLoading("loading_spinner")
            session.dismissLoading(
                "loading_progress",
                replaceWith = RichCardPayload.markdown(
                    id = "loading_progress",
                    markdown = "✅ 所有任务执行完毕！",
                    senderName = session.aiSenderName,
                    senderAvatar = session.aiAvatarUrl
                )
            )
        }
    }

    // ============================
    // Demo 7：错误卡片
    // ============================

    private fun demoError() {
        session.upsertMessage(
            RichCardPayload.error(
                id = "err_${runCounter++}",
                message = "调用 LLM 超时，请检查网络或稍后重试",
                code = "LLM_TIMEOUT_504",
                senderName = session.aiSenderName,
                senderAvatar = session.aiAvatarUrl
            )
        )
    }

    // ============================
    // Demo 8：建议选项
    // ============================

    private fun demoSuggestions() {
        session.upsertMessage(
            RichCardPayload.suggestions(
                id = "sugg_${runCounter++}",
                prompt = "你想接下来做什么？",
                options = listOf(
                    "继续优化代码" to "继续优化代码",
                    "运行测试" to "帮我运行测试",
                    "查看 Git 状态" to "查看 git status",
                    "生成 README" to "生成项目 README"
                ),
                senderName = session.aiSenderName,
                senderAvatar = session.aiAvatarUrl
            )
        )
    }

    // ============================
    // Demo 9：自定义事件映射 —— 非标准 AG-UI 事件
    // ============================

    private fun demoCustomMapper() {
        // 模拟某个业务平台发出非标准 CUSTOM 事件："code_generation"
        // 通过注册的 mapper 自动转成 CodeBlock 卡片
        session.dispatch(
            AgUiEvent.Custom(
                name = "code_generation",
                value = mapOf(
                    "id" to "custom_code_${runCounter++}",
                    "filename" to "PlatformSpecific.py",
                    "language" to "python",
                    "code" to "def hello_from_platform():\n    print('mapped by EventToCardMapper!')"
                )
            )
        )

        // 另一个非标准事件："file_operation"
        setTimeout(500) {
            session.dispatch(
                AgUiEvent.Custom(
                    name = "file_operation",
                    value = mapOf(
                        "id" to "custom_file_${runCounter++}",
                        "path" to "agent/tools/weather.py",
                        "op" to "create",
                        "additions" to 42,
                        "deletions" to 0,
                        "diff" to "+ import requests\n+ def query_weather(city):\n+     ..."
                    )
                )
            )
        }
    }

    // ============================
    // Demo 10：Markdown 混合拆分 —— 表格 / 引用 / diff / 折叠 / 清单
    // ============================

    private fun demoMarkdownSplit() {
        val runId = "split_${runCounter++}"
        val mixedMarkdown = """
            我已经完成了代码审查，以下是结果：

            | 文件 | 问题数 | 严重度 |
            |------|--------|--------|
            | Main.kt | 3 | 高 |
            | Utils.kt | 1 | 低 |

            > 建议优先修复 Main.kt 中的空指针问题。

            ```diff
            --- a/Main.kt
            +++ b/Main.kt
            @@ -10,5 +10,5 @@
            -    val name: String = null
            +    val name: String? = null
            ```

            <details>
            <summary>点击查看详细变更说明</summary>
            将非空类型改为可空类型，避免运行时 NPE。
            </details>

            - [x] 修复空指针
            - [x] 补充单元测试
            - [ ] 更新文档
        """.trimIndent()

        // 使用 MarkdownBlockExtractor 拆分为多条消息
        val blocks = MarkdownBlockExtractor.extract(
            markdown = mixedMarkdown,
            baseId = runId,
            senderName = session.aiSenderName,
            senderAvatar = session.aiAvatarUrl
        )
        blocks.forEach { session.upsertMessage(it) }
    }

    // ============================
    // Demo 11：综合演示 —— 完整 Agent 对话
    // ============================

    private fun demoFullAgentFlow() {
        val runId = "run_${runCounter++}"
        session.dispatch(AgUiEvent.RunStarted(threadId = session.threadId, runId = runId))

        // 用户消息
        session.messages.add(
            ChatMessage(
                id = "user_full_$runId",
                content = "帮我写一个查询天气的脚本，并运行测试",
                isSelf = true,
                senderName = "我",
                senderId = session.selfSenderId,
                status = MessageStatus.SENT
            )
        )

        // 1. 思考 Loading
        session.showLoading(
            id = "thinking_$runId",
            title = "正在思考",
            subtitle = "拆解任务步骤",
            style = LoadingStyle.DOTS
        )

        // 2. 0.8s：思考完毕，输出 Markdown 回复
        setTimeout(800) {
            session.dismissLoading("thinking_$runId")
            session.upsertMessage(
                RichCardPayload.markdown(
                    id = "plan_$runId",
                    markdown = "好的，我将：\n1. 创建 `weather.py`\n2. 写入查询逻辑\n3. 运行测试",
                    senderName = session.aiSenderName,
                    senderAvatar = session.aiAvatarUrl
                )
            )
        }

        // 3. 1.4s：生成代码
        setTimeout(1400) {
            session.upsertMessage(
                RichCardPayload.codeBlock(
                    id = "code_$runId",
                    filename = "weather.py",
                    language = "python",
                    code = """
                        import requests

                        def query_weather(city: str) -> dict:
                            url = f"https://api.example.com/weather?city={city}"
                            return requests.get(url).json()
                    """.trimIndent(),
                    senderName = session.aiSenderName,
                    senderAvatar = session.aiAvatarUrl
                )
            )
        }

        // 4. 2.0s：文件编辑卡片
        setTimeout(2000) {
            session.upsertMessage(
                RichCardPayload.fileEdit(
                    id = "file_$runId",
                    filePath = "weather.py",
                    operation = FileOperation.CREATE,
                    additions = 6,
                    senderName = session.aiSenderName,
                    senderAvatar = session.aiAvatarUrl
                )
            )
        }

        // 5. 2.6s：命令执行 running
        setTimeout(2600) {
            session.upsertMessage(
                RichCardPayload.commandExec(
                    id = "exec_$runId",
                    command = "python -m pytest tests/",
                    cwd = "~/project",
                    state = CommandState.RUNNING,
                    senderName = session.aiSenderName,
                    senderAvatar = session.aiAvatarUrl
                )
            )
        }

        // 6. 4.0s：命令执行完成
        setTimeout(4000) {
            session.upsertMessage(
                RichCardPayload.commandExec(
                    id = "exec_$runId",
                    command = "python -m pytest tests/",
                    cwd = "~/project",
                    state = CommandState.SUCCESS,
                    output = "===== 3 passed in 0.42s =====",
                    exitCode = 0,
                    durationMs = 1380,
                    senderName = session.aiSenderName,
                    senderAvatar = session.aiAvatarUrl
                )
            )
            // 7. 建议下一步
            session.upsertMessage(
                RichCardPayload.suggestions(
                    id = "sugg_$runId",
                    prompt = "任务完成！下一步想做什么？",
                    options = listOf(
                        "添加更多测试用例" to "添加更多测试用例",
                        "提交 Git" to "帮我 git commit",
                        "部署到服务器" to "部署到服务器"
                    ),
                    senderName = session.aiSenderName,
                    senderAvatar = session.aiAvatarUrl
                )
            )
            session.dispatch(AgUiEvent.RunFinished(threadId = session.threadId, runId = runId))
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
        runCounter = 0
    }

    private data class DemoAction(
        val label: String,
        val colorStart: Long,
        val colorEnd: Long,
        val trigger: () -> Unit
    )
}
