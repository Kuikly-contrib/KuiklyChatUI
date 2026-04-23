package com.tencent.kuiklybase.chat.agui

import com.tencent.kuiklybase.chat.model.ChatMessage

/**
 * 富消息卡片类型（AG-UI 扩展）。
 *
 * AI-Agent 场景里，一条消息可能是：
 * - 纯文本（markdown / 普通文字）
 * - 代码块（多语言）
 * - 命令执行（shell / python）
 * - 文件编辑（diff / 新建）
 * - 工具调用卡片
 * - 思考过程
 * - 搜索结果
 * - 加载中占位
 * - 错误提示
 * - 链接预览
 * - 图表 / 表格
 * - 业务自定义
 *
 * 这里定义的**都是字符串常量**，而非封闭枚举 —— 业务方可直接扩展任意新值，保留最大灵活性。
 * 渲染时通过 [RichMessageKind.of] 从 [ChatMessage] 中读取，不存在时回落到 `MessageType`。
 */
object RichMessageKind {
    // ---- 基础对话 ----
    /** 普通流式文本（AG-UI TEXT_MESSAGE_* 产生的消息默认类型） */
    const val TEXT_STREAM = "text_stream"
    /** Markdown 渲染文本（用于 AI 返回富文本内容） */
    const val MARKDOWN = "markdown"
    /** 纯思考过程 / 推理内容（REASONING_*） */
    const val REASONING = "reasoning"

    // ---- 工具调用 ----
    /** 工具调用进行中（TOOL_CALL_START 触发，结果到来前的占位） */
    const val TOOL_CALL_PENDING = "tool_call_pending"
    /** 工具调用完成结果 */
    const val TOOL_RESULT = "tool_result"
    /** 工具调用错误 */
    const val TOOL_ERROR = "tool_error"

    // ---- 代码相关 ----
    /** 代码块卡片（含语言 / 复制 / 行号等） */
    const val CODE_BLOCK = "code_block"
    /** 命令行执行卡片（Shell / Terminal 输出） */
    const val COMMAND_EXEC = "command_exec"
    /** 文件编辑卡片（创建 / 修改 / 删除 / diff 展示） */
    const val FILE_EDIT = "file_edit"
    /** 文件读取预览卡片 */
    const val FILE_READ = "file_read"

    // ---- 信息展示 ----
    /** 搜索结果列表 */
    const val SEARCH_RESULT = "search_result"
    /** 链接预览卡片（OG meta） */
    const val LINK_PREVIEW = "link_preview"
    /** 表格卡片 */
    const val TABLE = "table"
    /** 引用块卡片 */
    const val QUOTE = "quote"
    /** 折叠内容卡片（details/summary） */
    const val COLLAPSIBLE = "collapsible"
    /** Diff 对比卡片 */
    const val DIFF = "diff"
    /** 任务列表（Checklist / TODO list） */
    const val CHECKLIST = "checklist"

    // ---- 进度 / 任务 ----
    /** AG-UI Activity 事件产生的任务卡片（含进度、步骤） */
    const val ACTIVITY = "activity"
    /** Loading 占位消息 */
    const val LOADING = "loading"
    /** 错误展示（来自 RunError 等） */
    const val ERROR = "error"

    // ---- 交互 ----
    /** 建议选项卡片（quick reply / action chips） */
    const val SUGGESTIONS = "suggestions"
    /** 需要用户确认 / 输入的表单卡片 */
    const val INPUT_REQUEST = "input_request"

    /**
     * 从 [ChatMessage.extra] 中读取 `agUiKind` 字段。
     *
     * 优先使用显式声明；没有时返回空串。
     */
    fun of(message: ChatMessage): String = message.extra[AgUiExtraKeys.KIND].orEmpty()

    /**
     * 判断消息是否属于某一类 kind。
     */
    fun matches(message: ChatMessage, kind: String): Boolean = of(message) == kind
}

/**
 * ChatMessage.extra 中使用的标准 key 常量集合。
 *
 * 统一替代所有散落的字符串 literal，避免拼写错误。
 */
object AgUiExtraKeys {
    // ---- 通用标记 ----
    /** 富消息类型（对应 [RichMessageKind]） */
    const val KIND = "agUiKind"

    /** AG-UI 角色（user / assistant / system / tool / reasoning / activity 等） */
    const val ROLE = "agUiRole"

    /** 是否流式中（"true" / "false"） */
    const val STREAMING = "agUiStreaming"

    // ---- 工具调用 ----
    const val TOOL_CALL_ID = "toolCallId"
    const val TOOL_CALL_NAME = "toolCallName"
    const val TOOL_ARGS = "toolArgs"
    const val TOOL_ERROR_MESSAGE = "toolErrorMessage"

    // ---- Activity ----
    const val ACTIVITY_TYPE = "activityType"
    const val ACTIVITY_STATE = "activityState"      // running / success / failed / cancelled
    const val ACTIVITY_PROGRESS = "activityProgress" // "0.0" ~ "1.0"
    const val ACTIVITY_TITLE = "activityTitle"
    const val ACTIVITY_STEP_TOTAL = "activityStepTotal"
    const val ACTIVITY_STEP_DONE = "activityStepDone"

    // ---- 代码块 ----
    const val CODE_LANGUAGE = "codeLanguage"
    const val CODE_FILENAME = "codeFilename"
    const val CODE_LINE_COUNT = "codeLineCount"

    // ---- 命令执行 ----
    const val COMMAND_CMD = "command"
    const val COMMAND_CWD = "commandCwd"
    const val COMMAND_EXIT_CODE = "commandExitCode"
    const val COMMAND_DURATION_MS = "commandDurationMs"
    const val COMMAND_STATE = "commandState" // running / success / failed

    // ---- 文件编辑 ----
    const val FILE_PATH = "filePath"
    const val FILE_OPERATION = "fileOperation" // create / update / delete
    const val FILE_DIFF_ADDITIONS = "fileDiffAdditions"
    const val FILE_DIFF_DELETIONS = "fileDiffDeletions"

    // ---- 通用 Loading ----
    /** Loading 标题（如"正在搜索..."） */
    const val LOADING_TITLE = "loadingTitle"
    /** Loading 副标题（当前子任务） */
    const val LOADING_SUBTITLE = "loadingSubtitle"
    /** Loading 展示风格：dots / spinner / progress */
    const val LOADING_STYLE = "loadingStyle"
    /** Loading 关联的目标实体（如正在调用的工具名） */
    const val LOADING_TARGET = "loadingTarget"

    // ---- 错误 ----
    const val ERROR_CODE = "errorCode"
    const val ERROR_MESSAGE = "errorMessage"

    // ---- 表格 ----
    const val TABLE_HEADERS = "tableHeaders" // JSON 数组字符串
    const val TABLE_ROWS = "tableRows"       // JSON 数组字符串（二维）
    const val TABLE_CAPTION = "tableCaption"

    // ---- 引用块 ----
    const val QUOTE_SOURCE = "quoteSource"
    const val QUOTE_LEVEL = "quoteLevel"     // 嵌套层级 "1", "2" ...

    // ---- 折叠内容 ----
    const val COLLAPSIBLE_SUMMARY = "collapsibleSummary"
    const val COLLAPSIBLE_OPEN = "collapsibleOpen" // "true" / "false"

    // ---- Diff ----
    const val DIFF_ORIGINAL = "diffOriginal"
    const val DIFF_MODIFIED = "diffModified"
    const val DIFF_LANGUAGE = "diffLanguage"

    // ---- Checklist ----
    const val CHECKLIST_ITEMS = "checklistItems"     // JSON 数组字符串
    const val CHECKLIST_COMPLETED = "checklistCompleted" // 已完成数量
}

/**
 * Activity 状态值（对应 [AgUiExtraKeys.ACTIVITY_STATE]）。
 */
object ActivityState {
    const val RUNNING = "running"
    const val SUCCESS = "success"
    const val FAILED = "failed"
    const val CANCELLED = "cancelled"
}

/**
 * Loading 风格（对应 [AgUiExtraKeys.LOADING_STYLE]）。
 */
object LoadingStyle {
    /** 三个跳动的点（适合等待回复） */
    const val DOTS = "dots"
    /** 旋转指示器 */
    const val SPINNER = "spinner"
    /** 进度条（需配合 [AgUiExtraKeys.ACTIVITY_PROGRESS]） */
    const val PROGRESS = "progress"
    /** 脉冲骨架屏 */
    const val SKELETON = "skeleton"
}

/**
 * 命令执行状态（对应 [AgUiExtraKeys.COMMAND_STATE]）。
 */
object CommandState {
    const val RUNNING = "running"
    const val SUCCESS = "success"
    const val FAILED = "failed"
}

/**
 * 文件操作类型（对应 [AgUiExtraKeys.FILE_OPERATION]）。
 */
object FileOperation {
    const val CREATE = "create"
    const val UPDATE = "update"
    const val DELETE = "delete"
    const val READ = "read"
}
