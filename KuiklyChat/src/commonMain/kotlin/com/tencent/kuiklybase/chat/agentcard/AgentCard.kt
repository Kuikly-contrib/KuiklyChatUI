package com.tencent.kuiklybase.chat.agentcard

/**
 * Agent 卡片系统 - 数据模型
 *
 * 一套独立于 ChatSession 的 Agent 事件 UI 组件。每种卡片都有对应的 data class，
 * 通过 sealed class [AgentCard] 统一管理，支持 AG-UI 事件自动映射。
 *
 * 卡片类型：
 * - [AgentCard.CodeBlock]    代码块（多语言，含行号/复制/折叠）
 * - [AgentCard.CommandExec]  命令执行（shell/python 终端风格）
 * - [AgentCard.FileEdit]     文件编辑（diff / 新建）
 * - [AgentCard.ToolCall]     工具调用（显示 result）
 * - [AgentCard.Thinking]     思考过程（灰色虚线气泡）
 * - [AgentCard.SearchResult] 搜索结果（列表 + 高亮）
 * - [AgentCard.Loading]      加载中占位（多种样式）
 * - [AgentCard.Error]        错误提示
 * - [AgentCard.LinkPreview]  链接预览
 * - [AgentCard.Table]        表格
 * - [AgentCard.Chart]        图表（条形图）
 * - [AgentCard.Plan]         任务规划清单（多步骤 / 状态 / 进度）
 * - [AgentCard.Custom]       业务自定义
 */
sealed class AgentCard {
    /** 卡片唯一 ID，用于 upsert 去重 */
    abstract val id: String
    /** 创建时间戳 */
    abstract val timestamp: Long

    // ============================
    // 1. 代码块卡片
    // ============================
    data class CodeBlock(
        override val id: String,
        val code: String,
        val language: String = "",
        val filename: String = "",
        /** 是否默认折叠（超过 10 行自动折叠） */
        val collapsedByDefault: Boolean = false,
        override val timestamp: Long = 0L
    ) : AgentCard()

    // ============================
    // 2. 命令执行卡片
    // ============================
    data class CommandExec(
        override val id: String,
        val command: String,
        val cwd: String = "",
        val shell: String = "bash",
        val state: ExecState = ExecState.RUNNING,
        val output: String = "",
        val exitCode: Int? = null,
        val durationMs: Long? = null,
        override val timestamp: Long = 0L
    ) : AgentCard()

    // ============================
    // 3. 文件编辑卡片
    // ============================
    data class FileEdit(
        override val id: String,
        val filePath: String,
        val operation: FileOp = FileOp.UPDATE,
        val diffText: String = "",
        val additions: Int = 0,
        val deletions: Int = 0,
        override val timestamp: Long = 0L
    ) : AgentCard()

    // ============================
    // 4. 工具调用卡片
    // ============================
    data class ToolCall(
        override val id: String,
        val toolName: String,
        val toolCallId: String = "",
        val state: ToolState = ToolState.RUNNING,
        /** 工具参数（JSON 字符串） */
        val args: String = "",
        /** 工具调用结果 */
        val result: String = "",
        val errorMessage: String = "",
        override val timestamp: Long = 0L
    ) : AgentCard()

    // ============================
    // 5. 思考过程卡片
    // ============================
    data class Thinking(
        override val id: String,
        val content: String,
        /** 是否正在流式生成（显示光标动画） */
        val streaming: Boolean = false,
        override val timestamp: Long = 0L
    ) : AgentCard()

    // ============================
    // 6. 搜索结果卡片
    // ============================
    data class SearchResult(
        override val id: String,
        val query: String,
        val items: List<SearchItem> = emptyList(),
        val totalCount: Int = items.size,
        override val timestamp: Long = 0L
    ) : AgentCard()

    // ============================
    // 7. 加载占位卡片
    // ============================
    data class Loading(
        override val id: String,
        val title: String = "正在处理",
        val subtitle: String = "",
        val style: LoadingStyle = LoadingStyle.DOTS,
        val target: String = "",
        override val timestamp: Long = 0L
    ) : AgentCard()

    // ============================
    // 8. 错误卡片
    // ============================
    data class Error(
        override val id: String,
        val message: String,
        val code: String = "",
        val detail: String = "",
        override val timestamp: Long = 0L
    ) : AgentCard()

    // ============================
    // 9. 链接预览卡片
    // ============================
    data class LinkPreview(
        override val id: String,
        val url: String,
        val title: String = "",
        val description: String = "",
        val iconUrl: String = "",
        val siteName: String = "",
        override val timestamp: Long = 0L
    ) : AgentCard()

    // ============================
    // 10. 表格卡片
    // ============================
    data class Table(
        override val id: String,
        val title: String = "",
        val headers: List<String>,
        val rows: List<List<String>>,
        override val timestamp: Long = 0L
    ) : AgentCard()

    // ============================
    // 11. 图表卡片（条形图）
    // ============================
    data class Chart(
        override val id: String,
        val title: String = "",
        val chartType: ChartType = ChartType.BAR,
        val data: List<ChartPoint>,
        override val timestamp: Long = 0L
    ) : AgentCard()

    // ============================
    // 12. 任务规划卡片
    // ============================
    data class Plan(
        override val id: String,
        /** 规划标题（如"实施计划"） */
        val title: String = "任务规划",
        /** 可选摘要（如"共 4 步"/"规划完成"） */
        val summary: String = "",
        /** 步骤列表 */
        val steps: List<PlanStep> = emptyList(),
        override val timestamp: Long = 0L
    ) : AgentCard()

    // ============================
    // 13. 业务自定义卡片
    // ============================
    data class Custom(
        override val id: String,
        val type: String,
        val title: String = "",
        val payload: Map<String, String> = emptyMap(),
        override val timestamp: Long = 0L
    ) : AgentCard()

    // ============================
    // 14. 文本消息卡片（assistant 流式文本）
    // ============================
    data class TextMessage(
        override val id: String,
        val content: String,
        val role: String = "assistant",
        val streaming: Boolean = false,
        override val timestamp: Long = 0L
    ) : AgentCard()

    // ============================
    // 15. 用户消息卡片
    // ============================
    data class UserMessage(
        override val id: String,
        val content: String,
        override val timestamp: Long = 0L
    ) : AgentCard()

    // ============================
    // 16. 阶段进入头（workflow phase entry header）
    //     openspec: add-workflow-phase-progress
    //     仅 5 阶段（WORKSPACE / PLAN / DESIGN / LINT / COMPILE）使用；
    //     DEVELOPMENT 由 TodoProgress 一统承担。
    // ============================
    data class PhaseHeader(
        override val id: String,
        /** "WORKSPACE_CREATE" | "PLAN_CONFIRM" | "DESIGN_SYSTEM" | "LINT" | "COMPILE" */
        val phaseId: String,
        /** 中文显示名（如 "设计系统确定"） */
        val phaseName: String,
        /** "running" | "done" | "failed" */
        val status: String,
        /** 触发因，便于调试 */
        val reason: String,
        override val timestamp: Long = 0L
    ) : AgentCard()

    // ============================
    // 17. TodoWrite 进度卡（DEV 阶段进入头 + 后续更新合体）
    //     openspec: add-workflow-phase-progress
    // ============================
    data class TodoProgress(
        override val id: String,
        val items: List<TodoProgressItem>,
        val completed: Int,
        val total: Int,
        override val timestamp: Long = 0L
    ) : AgentCard()
}

// ============================
// 枚举与辅助类型
// ============================

/** 命令执行状态 */
enum class ExecState {
    RUNNING, SUCCESS, FAILED, CANCELED
}

/** 文件操作类型 */
enum class FileOp {
    CREATE, UPDATE, DELETE, RENAME
}

/** 工具调用状态 */
enum class ToolState {
    PENDING, RUNNING, SUCCESS, FAILED
}

/**
 * TodoWrite 单条任务（KuiklyChat 模块内）。
 *
 * openspec: add-workflow-phase-progress
 */
data class TodoProgressItem(
    val content: String,
    /** "pending" | "in_progress" | "completed" */
    val status: String,
    val activeForm: String? = null,
)

/** 加载样式 */
enum class LoadingStyle {
    DOTS, SPINNER, PROGRESS, SKELETON
}

/** 图表类型 */
enum class ChartType {
    BAR, LINE, PIE
}

/** 搜索结果条目 */
data class SearchItem(
    val title: String,
    val snippet: String = "",
    val url: String = "",
    val score: Float = 0f
)

/** 图表数据点 */
data class ChartPoint(
    val label: String,
    val value: Float
)

/** 任务规划里的单个步骤状态 */
enum class PlanStepStatus {
    /** 待执行 */
    PENDING,
    /** 执行中 */
    RUNNING,
    /** 已完成 */
    DONE,
    /** 失败 */
    FAILED,
    /** 跳过 */
    SKIPPED
}

/** 任务规划里的单个步骤 */
data class PlanStep(
    /** 步骤唯一 ID（用于后续 update 时定位） */
    val id: String,
    /** 步骤标题（用户可见） */
    val title: String,
    /** 步骤状态 */
    val status: PlanStepStatus = PlanStepStatus.PENDING,
    /** 可选详细描述 */
    val detail: String = ""
)
