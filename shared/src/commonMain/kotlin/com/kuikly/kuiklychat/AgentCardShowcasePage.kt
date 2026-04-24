package com.kuikly.kuiklychat

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.*
import com.kuikly.kuiklychat.base.BasePager
import com.tencent.kuiklybase.chat.agentcard.*
import com.tencent.kuiklybase.chat.agui.AgUiEvent
import com.tencent.kuiklybase.chat.agui.AgUiRole

/**
 * Agent 卡片系统 Demo 页面
 *
 * 演示：
 * - 12 种 AgentCard 类型的视觉效果
 * - AG-UI 事件驱动卡片自动生成
 * - 流式更新（文本 / 工具调用 / 命令执行）
 */
@Page("agent_card_demo", supportInLocal = true)
internal class AgentCardShowcasePage : BasePager() {

    // Pager 字段级 observableList —— 这个委托在 BasePager（Pager）里创建时就会接入响应式系统，
    // 成为唯一的"事实来源"。session 也直接操作这同一个 list 实例。
    private var cards by observableList<AgentCard>()
    private val session = AgUiCardSession(threadId = "showcase", selfId = "me")
    private var statusText by observable("空闲中...")
    private var isRunning by observable(false)
    private var runCounter = 0

    private val demoActions: List<DemoAction> by lazy {
        listOf(
            DemoAction(" 所有卡片", 0xFF6366F1L, 0xFF8B5CF6L, ::demoAllCards),
            DemoAction(" 流式文本", 0xFF10B981L, 0xFF059669L, ::demoStreamText),
            DemoAction(" 工具调用", 0xFF9F7AEAL, 0xFF805AD5L, ::demoToolCall),
            DemoAction(" 命令执行", 0xFF0F172AL, 0xFF334155L, ::demoCommandExec),
            DemoAction(" 代码块", 0xFF4338CAL, 0xFF6366F1L, ::demoCodeBlock),
            DemoAction(" 文件编辑", 0xFF8B5CF6L, 0xFF7C3AEDL, ::demoFileEdit),
            DemoAction(" 搜索结果", 0xFF3B82F6L, 0xFF1D4ED8L, ::demoSearch),
            DemoAction(" 思考过程", 0xFF64748BL, 0xFF475569L, ::demoThinking),
            DemoAction(" 任务规划", 0xFFA855F7L, 0xFF9333EAL, ::demoPlan),
            DemoAction(" Loading", 0xFFF59E0BL, 0xFFD97706L, ::demoLoading),
            DemoAction(" 错误", 0xFFEF4444L, 0xFFDC2626L, ::demoError),
            DemoAction(" 链接预览", 0xFF06B6D4L, 0xFF0891B2L, ::demoLinkPreview),
            DemoAction(" 表格 / 图表", 0xFFEC4899L, 0xFFDB2777L, ::demoTableChart),
            DemoAction(" 完整 Agent 流程", 0xFFE11D48L, 0xFFBE123CL, ::demoFullFlow),
            DemoAction(" 重置", 0xFF6B7280L, 0xFF4B5563L, ::reset)
        )
    }

    override fun created() {
        super.created()
        // 把 Pager 级的 observableList 注入到 session —— 后续 upsert 都作用于这个响应式 list
        @Suppress("UNCHECKED_CAST")
        session.bindCards(cards as MutableList<AgentCard>, migrate = false)

        session.onRunStarted = { runId ->
            statusText = " Run: $runId"
            isRunning = true
        }
        session.onRunFinished = { runId ->
            statusText = " 完成: $runId"
            isRunning = false
        }
        session.onRunError = { msg, _ ->
            statusText = " $msg"
            isRunning = false
        }
        setTimeout(100) {
            demoAllCards()
            statusText = " 已展示全部卡片，点击按钮可单独体验"
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    flex(1f)
                    flexDirection(FlexDirection.COLUMN)
                    backgroundColor(Color(0xFFF8FAFC))
                }

                // ===== 顶部导航栏 =====
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
                                text("Agent 卡片 UI")
                                fontSize(17f)
                                fontWeightSemisolid()
                                color(Color.WHITE)
                            }
                        }
                    }

                    // 状态条
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
                                    if (ctx.isRunning) Color(0xFFFBBF24)
                                    else Color(0xFFA0AEC0)
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
                        Text {
                            attr {
                                text("📇 ${ctx.cards.size}")
                                fontSize(11f)
                                color(Color(0xFFFBD38D))
                                fontWeightSemiBold()
                            }
                        }
                    }
                }

                // ===== 操作按钮 =====
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
                                    fontSize(12f)
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

                // ===== 卡片列表 =====
                View {
                    attr { flex(1f) }
                    AgentCardList(
                        cards = { ctx.cards as ObservableList<AgentCard> },
                        callbacks = AgentCardCallbacks(
                            onCopyCode = { /* 真实场景接入剪贴板 */ },
                            onLinkClick = { url ->
                                // 可触发 RouterModule 打开浏览器
                                println("Open link: $url")
                            },
                            onRetry = { println("Retry!") }
                        ),
                        options = AgentCardListOptions(
                            // 代码块超过 ~16 行就进入滚动模式
                            codeBlockMaxHeight = 300f
                        )
                    )
                }
            }
        }
    }

    // ============================
    // 单卡片 Demo
    // ============================

    private fun demoAllCards() {
        session.reset()
        session.upsert(AgentCard.UserMessage(
            id = "user_all",
            content = "帮我展示一下所有卡片类型"
        ))
        session.upsert(AgentCard.TextMessage(
            id = "text_all",
            content = "好的，下面是每种卡片的展示："
        ))
        demoThinking()
        demoPlan()
        demoCodeBlock()
        demoCommandExec()
        demoFileEdit()
        demoToolCall()
        demoSearch()
        demoLoading()
        demoError()
        demoLinkPreview()
        demoTableChart()
    }

    private fun demoStreamText() {
        val runId = "run_text_${runCounter++}"
        val messageId = "msg_text_$runId"

        session.upsert(AgentCard.UserMessage(
            id = "user_$runId",
            content = "介绍一下 AG-UI 协议"
        ))

        session.dispatch(AgUiEvent.RunStarted(threadId = session.threadId, runId = runId))
        session.dispatch(AgUiEvent.TextMessageStart(messageId = messageId, role = AgUiRole.ASSISTANT))

        val chunks = listOf(
            "AG-UI（Agent-User Interaction）是 ",
            "一个轻量级的事件驱动协议，",
            "用于连接 AI Agent 和前端应用。\n\n",
            "核心特性：\n",
            "• 16 种标准化事件类型\n",
            "• 传输层无关\n",
            "• 支持流式文本、工具调用"
        )
        scheduleChunks(chunks, 250, { i ->
            session.dispatch(AgUiEvent.TextMessageContent(
                messageId = messageId,
                delta = chunks[i]
            ))
        }) {
            session.dispatch(AgUiEvent.TextMessageEnd(messageId = messageId))
            session.dispatch(AgUiEvent.RunFinished(threadId = session.threadId, runId = runId))
        }
    }

    private fun demoToolCall() {
        val runId = "run_tool_${runCounter++}"
        val toolCallId = "call_$runId"
        session.dispatch(AgUiEvent.RunStarted(threadId = session.threadId, runId = runId))
        session.dispatch(AgUiEvent.ToolCallStart(
            toolCallId = toolCallId,
            toolCallName = "query_weather"
        ))
        session.dispatch(AgUiEvent.ToolCallArgs(
            toolCallId = toolCallId,
            delta = """{"city":"Beijing","unit":"celsius"}"""
        ))
        setTimeout(1200) {
            session.dispatch(AgUiEvent.ToolCallEnd(toolCallId = toolCallId))
            session.dispatch(AgUiEvent.ToolCallResult(
                messageId = "tool_result_$runId",
                toolCallId = toolCallId,
                content = "Beijing · 晴 · 22°C · 微风（查询成功）"
            ))
            session.dispatch(AgUiEvent.RunFinished(threadId = session.threadId, runId = runId))
        }
    }

    private fun demoCommandExec() {
        val id = "cmd_${runCounter++}"
        session.upsert(AgentCard.CommandExec(
            id = id,
            command = "npm run build",
            cwd = "~/project",
            shell = "bash",
            state = ExecState.RUNNING
        ))
        setTimeout(1500) {
            session.upsert(AgentCard.CommandExec(
                id = id,
                command = "npm run build",
                cwd = "~/project",
                shell = "bash",
                state = ExecState.SUCCESS,
                output = """
                    > movie-list@1.0.0 build
                    > vite build

                    ✓ 42 modules transformed.
                    dist/index.html           0.46 kB │ gzip: 0.30 kB
                    dist/assets/index.css     8.12 kB │ gzip: 2.45 kB
                    dist/assets/index.js    142.67 kB │ gzip: 46.12 kB
                """.trimIndent(),
                exitCode = 0,
                durationMs = 1487
            ))
        }
    }

    private fun demoCodeBlock() {
        session.upsert(AgentCard.CodeBlock(
            id = "code_${runCounter++}",
            filename = "SearchBar.tsx",
            language = "typescript",
            code = """
                import { useState } from 'react';
                import './SearchBar.css';

                export function SearchBar({ onSearch }) {
                    const [query, setQuery] = useState('');
                    return (
                        <div className="search-bar">
                            <input value={query} onChange={e => setQuery(e.target.value)} />
                            <button onClick={() => onSearch(query)}>
                                Search
                            </button>
                        </div>
                    );
                }
            """.trimIndent()
        ))
    }

    private fun demoFileEdit() {
        session.upsert(AgentCard.FileEdit(
            id = "file_${runCounter++}",
            filePath = "src/components/MovieList.tsx",
            operation = FileOp.UPDATE,
            additions = 12,
            deletions = 3,
            diffText = """
                @@ -10,3 +10,12 @@
                -function MovieList({ movies }) {
                +function MovieList({ movies, query, minRating }) {
                +    const filtered = useMemo(() => {
                +        return movies
                +            .filter(m => m.title.includes(query))
                +            .filter(m => m.rating >= minRating);
                +    }, [movies, query, minRating]);
                -    return movies.map(m => <MovieCard movie={m} />);
                +    return filtered.map(m => <MovieCard movie={m} />);
                }
            """.trimIndent()
        ))
    }

    private fun demoSearch() {
        session.upsert(AgentCard.SearchResult(
            id = "search_${runCounter++}",
            query = "React 性能优化",
            items = listOf(
                SearchItem(
                    title = "React 性能优化完全指南",
                    snippet = "从 memo、useMemo、useCallback 到 Concurrent Mode，全方位剖析 React 性能优化手段。",
                    url = "https://example.com/react-perf",
                    score = 4.8f
                ),
                SearchItem(
                    title = "React 组件性能优化实战",
                    snippet = "通过实际案例讲解常见的性能问题与解决方案。",
                    url = "https://example.com/react-components",
                    score = 4.5f
                ),
                SearchItem(
                    title = "使用 React DevTools 分析性能",
                    snippet = "学会用 Profiler 定位性能瓶颈。",
                    url = "https://example.com/devtools",
                    score = 4.2f
                )
            ),
            totalCount = 127
        ))
    }

    private fun demoThinking() {
        val id = "think_${runCounter++}"
        session.upsert(AgentCard.Thinking(
            id = id,
            content = "用户想要搜索功能，",
            streaming = true
        ))
        setTimeout(600) {
            session.upsert(AgentCard.Thinking(
                id = id,
                content = "用户想要搜索功能，我需要先分析项目结构，然后创建 SearchBar 组件，",
                streaming = true
            ))
        }
        setTimeout(1200) {
            session.upsert(AgentCard.Thinking(
                id = id,
                content = "用户想要搜索功能，我需要先分析项目结构，然后创建 SearchBar 组件，最后集成到 MovieList。",
                streaming = false
            ))
        }
    }

    /**
     * 任务规划 Demo —— 通过 AG-UI CUSTOM 事件驱动 PlanCard 流式更新
     *
     * 演示协议链路：
     *   CUSTOM(plan_created)  — 创建整体清单（全部 pending）
     *   CUSTOM(plan_updated)  — 依次把某步切换成 running → done
     */
    private fun demoPlan() {
        val planId = "plan_${runCounter++}"
        // 1) 初始规划快照（整体插入）
        session.dispatch(AgUiEvent.Custom(
            name = "plan_created",
            value = mapOf(
                "id" to planId,
                "title" to "实施计划",
                "summary" to "共 4 步",
                "steps" to listOf(
                    mapOf("id" to "s1", "title" to "创建 SearchBar 搜索栏组件", "status" to "running"),
                    mapOf("id" to "s2", "title" to "创建 RatingSlider 评分滑块组件", "status" to "pending"),
                    mapOf("id" to "s3", "title" to "集成搜索逻辑到 MovieList", "status" to "pending"),
                    mapOf("id" to "s4", "title" to "添加搜索结果空状态", "status" to "pending")
                )
            )
        ))
        // 2) 依次推进进度（每一步用 plan_updated 事件）
        setTimeout(900) {
            session.dispatch(AgUiEvent.Custom(
                name = "plan_updated",
                value = mapOf(
                    "id" to planId,
                    "steps" to listOf(
                        mapOf("id" to "s1", "title" to "创建 SearchBar 搜索栏组件", "status" to "done"),
                        mapOf("id" to "s2", "title" to "创建 RatingSlider 评分滑块组件", "status" to "running")
                    )
                )
            ))
        }
        setTimeout(1800) {
            session.dispatch(AgUiEvent.Custom(
                name = "plan_updated",
                value = mapOf(
                    "id" to planId,
                    "steps" to listOf(
                        mapOf("id" to "s2", "title" to "创建 RatingSlider 评分滑块组件", "status" to "done"),
                        mapOf("id" to "s3", "title" to "集成搜索逻辑到 MovieList", "status" to "running")
                    )
                )
            ))
        }
        setTimeout(2700) {
            session.dispatch(AgUiEvent.Custom(
                name = "plan_updated",
                value = mapOf(
                    "id" to planId,
                    "steps" to listOf(
                        mapOf("id" to "s3", "title" to "集成搜索逻辑到 MovieList", "status" to "done"),
                        mapOf("id" to "s4", "title" to "添加搜索结果空状态", "status" to "running")
                    )
                )
            ))
        }
        setTimeout(3600) {
            session.dispatch(AgUiEvent.Custom(
                name = "plan_updated",
                value = mapOf(
                    "id" to planId,
                    "summary" to "规划完成",
                    "steps" to listOf(
                        mapOf("id" to "s4", "title" to "添加搜索结果空状态", "status" to "done")
                    )
                )
            ))
        }
    }

    private fun demoLoading() {
        session.upsert(AgentCard.Loading(
            id = "load_1_${runCounter++}",
            title = "正在分析代码",
            subtitle = "扫描 42 个文件",
            style = LoadingStyle.DOTS
        ))
    }

    private fun demoError() {
        session.upsert(AgentCard.Error(
            id = "err_${runCounter++}",
            message = "调用 LLM 服务超时",
            code = "LLM_TIMEOUT_504",
            detail = "请求耗时 30s 超出限制，请检查网络或稍后重试"
        ))
    }

    private fun demoLinkPreview() {
        session.upsert(AgentCard.LinkPreview(
            id = "link_${runCounter++}",
            url = "https://kuikly.tencent.com",
            title = "Kuikly - 腾讯跨端开发框架",
            description = "基于 Kotlin Multiplatform 的跨端开发框架，一次编码全端运行。",
            siteName = "Kuikly 官网",
            iconUrl = "https://vfiles.gtimg.cn/wuji_dashboard/wupload/xy/starter/62394e19.png"
        ))
        session.upsert(AgentCard.LinkPreview(
            id = "link2_${runCounter++}",
            url = "https://docs.ag-ui.com/concepts/events",
            title = "AG-UI Events Documentation",
            description = "Complete reference of all 16 standard AG-UI event types.",
            siteName = "AG-UI Docs"
        ))
    }

    private fun demoTableChart() {
        session.upsert(AgentCard.Table(
            id = "table_${runCounter++}",
            title = "代码审查结果",
            headers = listOf("文件", "问题数", "严重度", "修复建议"),
            rows = listOf(
                listOf("Main.kt", "3", "高", "修复空指针"),
                listOf("Utils.kt", "1", "低", "优化注释"),
                listOf("Api.kt", "2", "中", "添加异常处理"),
                listOf("User.kt", "0", "-", "无需修复")
            )
        ))
        session.upsert(AgentCard.Chart(
            id = "chart_${runCounter++}",
            title = "代码质量评分",
            chartType = ChartType.BAR,
            data = listOf(
                ChartPoint("可读性", 85f),
                ChartPoint("性能", 72f),
                ChartPoint("安全性", 94f),
                ChartPoint("可维护", 78f),
                ChartPoint("测试覆盖", 61f)
            )
        ))
    }

    // ============================
    // 完整 Agent 流程 Demo（模拟 Codebuddy 交互）
    // ============================

    private fun demoFullFlow() {
        session.reset()
        val runId = "full_${runCounter++}"

        // 1. 用户消息
        session.upsert(AgentCard.UserMessage(
            id = "user_full",
            content = "给电影清单添加一个搜索功能，可以按名称搜索，也能按评分区间筛选。先看看项目的情况～"
        ))

        session.dispatch(AgUiEvent.RunStarted(threadId = session.threadId, runId = runId))

        // 2. 分析阶段
        setTimeout(300) {
            session.upsert(AgentCard.Loading(
                id = "analyzing",
                title = "分析完成，需修改 4 处",
                style = LoadingStyle.SPINNER
            ))
        }
        setTimeout(1000) {
            session.removeById("analyzing")
            // 展示项目目录树（用 Custom 卡片）
            session.upsert(AgentCard.Custom(
                id = "project_tree",
                type = "读取项目目录结构",
                title = "src/",
                payload = mapOf(
                    "src/components/" to "",
                    "src/hooks/" to "",
                    "src/services/" to "",
                    "src/styles/" to ""
                )
            ))
        }

        // 3. 规划步骤（通过 AG-UI CUSTOM 事件驱动 Plan 卡片）
        setTimeout(1500) {
            session.dispatch(AgUiEvent.Custom(
                name = "plan_created",
                value = mapOf(
                    "id" to "plan_full",
                    "title" to "实施计划",
                    "summary" to "共 4 步",
                    "steps" to listOf(
                        mapOf("id" to "s1", "title" to "创建 SearchBar 搜索栏组件", "status" to "pending"),
                        mapOf("id" to "s2", "title" to "创建 RatingSlider 评分滑块组件", "status" to "pending"),
                        mapOf("id" to "s3", "title" to "集成搜索逻辑到 MovieList", "status" to "pending"),
                        mapOf("id" to "s4", "title" to "添加搜索结果空状态", "status" to "pending")
                    )
                )
            ))
        }

        // 4. 思考
        setTimeout(2200) {
            session.upsert(AgentCard.Thinking(
                id = "thinking_full",
                content = "在前端直接搞定搜索过滤，加一个搜索栏和评分滑块，实时过滤列表就行，不用改后端。",
                streaming = false
            ))
        }

        // 5. 创建 SearchBar 组件（同时把 Plan 的第 1 步切到 running）
        setTimeout(2800) {
            session.dispatch(AgUiEvent.Custom(
                name = "plan_updated",
                value = mapOf(
                    "id" to "plan_full",
                    "steps" to listOf(
                        mapOf("id" to "s1", "title" to "创建 SearchBar 搜索栏组件", "status" to "running")
                    )
                )
            ))
        }
        setTimeout(3000) {
            session.upsert(AgentCard.CodeBlock(
                id = "code_searchbar",
                filename = "src/components/SearchBar.tsx",
                language = "typescript",
                code = """
                    import { useState } from 'react';
                    import './SearchBar.css';

                    export function SearchBar({ onSearch }) {
                        const [query, setQuery] = useState('');
                        return (
                            <div className="search-bar">
                                <input
                                    value={query}
                                    onChange={e => setQuery(e.target.value)}
                                />
                                <button onClick={() => onSearch(query)}>
                                    Search
                                </button>
                            </div>
                        );
                    }
                """.trimIndent()
            ))
        }

        // 6. 文件编辑（第 1 步 done、第 3 步 running —— 跳过 s2 演示 running/pending 切换）
        setTimeout(4000) {
            session.dispatch(AgUiEvent.Custom(
                name = "plan_updated",
                value = mapOf(
                    "id" to "plan_full",
                    "steps" to listOf(
                        mapOf("id" to "s1", "title" to "创建 SearchBar 搜索栏组件", "status" to "done"),
                        mapOf("id" to "s2", "title" to "创建 RatingSlider 评分滑块组件", "status" to "done"),
                        mapOf("id" to "s3", "title" to "集成搜索逻辑到 MovieList", "status" to "running")
                    )
                )
            ))
        }
        setTimeout(4200) {
            session.upsert(AgentCard.FileEdit(
                id = "edit_movielist",
                filePath = "src/components/MovieList.tsx",
                operation = FileOp.UPDATE,
                additions = 18,
                deletions = 5,
                diffText = """
                    @@ -10,5 +10,18 @@
                    -function MovieList({ movies }) {
                    +function MovieList({ movies, query, minRating }) {
                    +    const filtered = useMemo(() => {
                    +        return movies
                    +            .filter(m => m.title.includes(query))
                    +            .filter(m => m.rating >= minRating);
                    +    }, [movies, query, minRating]);
                    -    return movies.map(m => <MovieCard movie={m} />);
                    +    return filtered.map(m => <MovieCard movie={m} />);
                    }
                """.trimIndent()
            ))
        }

        // 7. 命令执行（第 3 步 done、第 4 步 running）
        setTimeout(5400) {
            session.dispatch(AgUiEvent.Custom(
                name = "plan_updated",
                value = mapOf(
                    "id" to "plan_full",
                    "steps" to listOf(
                        mapOf("id" to "s3", "title" to "集成搜索逻辑到 MovieList", "status" to "done"),
                        mapOf("id" to "s4", "title" to "添加搜索结果空状态", "status" to "running")
                    )
                )
            ))
        }
        setTimeout(5500) {
            session.upsert(AgentCard.CommandExec(
                id = "run_build",
                command = "npm run build",
                cwd = "~/project",
                shell = "bash",
                state = ExecState.RUNNING
            ))
        }

        // 8. 命令完成（第 4 步 done —— Plan 整体完成）
        setTimeout(7000) {
            session.upsert(AgentCard.CommandExec(
                id = "run_build",
                command = "npm run build",
                cwd = "~/project",
                shell = "bash",
                state = ExecState.SUCCESS,
                output = """
                    > movie-list@1.0.0 build
                    > vite build

                    ✓ 42 modules transformed.
                    dist/index.html           0.46 kB │ gzip: 0.30 kB
                    dist/assets/index.css     8.12 kB │ gzip: 2.45 kB
                    dist/assets/index.js    142.67 kB │ gzip: 46.12 kB
                """.trimIndent(),
                exitCode = 0,
                durationMs = 1487
            ))
            session.dispatch(AgUiEvent.Custom(
                name = "plan_updated",
                value = mapOf(
                    "id" to "plan_full",
                    "summary" to "规划完成",
                    "steps" to listOf(
                        mapOf("id" to "s4", "title" to "添加搜索结果空状态", "status" to "done")
                    )
                )
            ))
        }

        // 9. 最终总结
        setTimeout(7800) {
            session.upsert(AgentCard.TextMessage(
                id = "summary",
                content = "搞定！搜索功能已经加好了——搜关键词、滑评分筛选都能用。没搜到结果的时候也会有友好提示，你可以试试看～"
            ))
            session.dispatch(AgUiEvent.RunFinished(threadId = session.threadId, runId = runId))
        }
    }

    private fun reset() {
        session.reset()
        statusText = "空闲中..."
        isRunning = false
    }

    // ============================
    // 工具
    // ============================

    private fun scheduleChunks(
        chunks: List<String>,
        intervalMs: Int,
        each: (Int) -> Unit,
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
        setTimeout(50) { step(0) }
    }

    private data class DemoAction(
        val label: String,
        val colorStart: Long,
        val colorEnd: Long,
        val trigger: () -> Unit
    )
}
