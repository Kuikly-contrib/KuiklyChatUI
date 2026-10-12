package com.tencent.kuiklybase.chat.model

import com.tencent.kuiklybase.chat.model.*

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
public object RichMessageKind {
    // ---- 基础对话 ----
    /** 普通流式文本（AG-UI TEXT_MESSAGE_* 产生的消息默认类型） */
    public const val TEXT_STREAM: String = "text_stream"
    /** Markdown 渲染文本（用于 AI 返回富文本内容） */
    public const val MARKDOWN: String = "markdown"
    /** 纯思考过程 / 推理内容（REASONING_*） */
    public const val REASONING: String = "reasoning"

    // ---- 工具调用 ----
    /** 工具调用进行中（TOOL_CALL_START 触发，结果到来前的占位） */
    public const val TOOL_CALL_PENDING: String = "tool_call_pending"
    /** 工具调用完成结果 */
    public const val TOOL_RESULT: String = "tool_result"
    /** 工具调用错误 */
    public const val TOOL_ERROR: String = "tool_error"

    // ---- 代码相关 ----
    /** 代码块卡片（含语言 / 复制 / 行号等） */
    public const val CODE_BLOCK: String = "code_block"
    /** 命令行执行卡片（Shell / Terminal 输出） */
    public const val COMMAND_EXEC: String = "command_exec"
    /** 文件编辑卡片（创建 / 修改 / 删除 / diff 展示） */
    public const val FILE_EDIT: String = "file_edit"
    /** 文件读取预览卡片 */
    public const val FILE_READ: String = "file_read"

    // ---- 信息展示 ----
    /** 搜索结果列表 */
    public const val SEARCH_RESULT: String = "search_result"
    /** 链接预览卡片（OG meta） */
    public const val LINK_PREVIEW: String = "link_preview"
    /** 表格卡片 */
    public const val TABLE: String = "table"
    /** 引用块卡片 */
    public const val QUOTE: String = "quote"
    /** 折叠内容卡片（details/summary） */
    public const val COLLAPSIBLE: String = "collapsible"
    /** Diff 对比卡片 */
    public const val DIFF: String = "diff"
    /** 任务列表（Checklist / TODO list） */
    public const val CHECKLIST: String = "checklist"

    // ---- 进度 / 任务 ----
    /** AG-UI Activity 事件产生的任务卡片（含进度、步骤） */
    public const val ACTIVITY: String = "activity"
    /** Loading 占位消息 */
    public const val LOADING: String = "loading"
    /** 错误展示（来自 RunError 等） */
    public const val ERROR: String = "error"

    // ---- 交互 ----
    /** 建议选项卡片（quick reply / action chips） */
    public const val SUGGESTIONS: String = "suggestions"
    /** 需要用户确认 / 输入的表单卡片 */
    public const val INPUT_REQUEST: String = "input_request"

    /**
     * 从 [ChatMessage.extra] 中读取 `agUiKind` 字段。
     *
     * 优先使用显式声明；没有时返回空串。
     */
    public fun of(message: ChatMessage): String = message.extra[AgUiExtraKeys.KIND].orEmpty()

    /**
     * 判断消息是否属于某一类 kind。
     */
    public fun matches(message: ChatMessage, kind: String): Boolean = of(message) == kind
}

/**
 * ChatMessage.extra 中使用的标准 key 常量集合。
 *
 * 统一替代所有散落的字符串 literal，避免拼写错误。
 */
public object AgUiExtraKeys {
    // ---- 通用标记 ----
    /** 富消息类型（对应 [RichMessageKind]） */
    public const val KIND: String = "agUiKind"

    /** AG-UI 角色（user / assistant / system / tool / reasoning / activity 等） */
    public const val ROLE: String = "agUiRole"

    /** 是否流式中（"true" / "false"） */
    public const val STREAMING: String = "agUiStreaming"

    // ---- 工具调用 ----
    public const val TOOL_CALL_ID: String = "toolCallId"
    public const val TOOL_CALL_NAME: String = "toolCallName"
    public const val TOOL_ARGS: String = "toolArgs"
    public const val TOOL_ERROR_MESSAGE: String = "toolErrorMessage"

    // ---- Activity ----
    public const val ACTIVITY_TYPE: String = "activityType"
    public const val ACTIVITY_STATE: String = "activityState"      // running / success / failed / cancelled
    public const val ACTIVITY_PROGRESS: String = "activityProgress" // "0.0" ~ "1.0"
    public const val ACTIVITY_TITLE: String = "activityTitle"
    public const val ACTIVITY_STEP_TOTAL: String = "activityStepTotal"
    public const val ACTIVITY_STEP_DONE: String = "activityStepDone"

    // ---- 代码块 ----
    public const val CODE_LANGUAGE: String = "codeLanguage"
    public const val CODE_FILENAME: String = "codeFilename"
    public const val CODE_LINE_COUNT: String = "codeLineCount"

    // ---- 命令执行 ----
    public const val COMMAND_CMD: String = "command"
    public const val COMMAND_CWD: String = "commandCwd"
    public const val COMMAND_EXIT_CODE: String = "commandExitCode"
    public const val COMMAND_DURATION_MS: String = "commandDurationMs"
    public const val COMMAND_STATE: String = "commandState" // running / success / failed

    // ---- 文件编辑 ----
    public const val FILE_PATH: String = "filePath"
    public const val FILE_OPERATION: String = "fileOperation" // create / update / delete
    public const val FILE_DIFF_ADDITIONS: String = "fileDiffAdditions"
    public const val FILE_DIFF_DELETIONS: String = "fileDiffDeletions"

    // ---- 通用 Loading ----
    /** Loading 标题（如"正在搜索..."） */
    public const val LOADING_TITLE: String = "loadingTitle"
    /** Loading 副标题（当前子任务） */
    public const val LOADING_SUBTITLE: String = "loadingSubtitle"
    /** Loading 展示风格：dots / spinner / progress */
    public const val LOADING_STYLE: String = "loadingStyle"
    /** Loading 关联的目标实体（如正在调用的工具名） */
    public const val LOADING_TARGET: String = "loadingTarget"

    // ---- 错误 ----
    public const val ERROR_CODE: String = "errorCode"
    public const val ERROR_MESSAGE: String = "errorMessage"

    // ---- 表格 ----
    public const val TABLE_HEADERS: String = "tableHeaders" // JSON 数组字符串
    public const val TABLE_ROWS: String = "tableRows"       // JSON 数组字符串（二维）
    public const val TABLE_CAPTION: String = "tableCaption"

    // ---- 引用块 ----
    public const val QUOTE_SOURCE: String = "quoteSource"
    public const val QUOTE_LEVEL: String = "quoteLevel"     // 嵌套层级 "1", "2" ...

    // ---- 折叠内容 ----
    public const val COLLAPSIBLE_SUMMARY: String = "collapsibleSummary"
    public const val COLLAPSIBLE_OPEN: String = "collapsibleOpen" // "true" / "false"

    // ---- Diff ----
    public const val DIFF_ORIGINAL: String = "diffOriginal"
    public const val DIFF_MODIFIED: String = "diffModified"
    public const val DIFF_LANGUAGE: String = "diffLanguage"

    // ---- Checklist ----
    public const val CHECKLIST_ITEMS: String = "checklistItems"     // JSON 数组字符串
    public const val CHECKLIST_COMPLETED: String = "checklistCompleted" // 已完成数量
}

/**
 * Activity 状态值（对应 [AgUiExtraKeys.ACTIVITY_STATE]）。
 */
public object ActivityState {
    public const val RUNNING: String = "running"
    public const val SUCCESS: String = "success"
    public const val FAILED: String = "failed"
    public const val CANCELLED: String = "cancelled"
}

/**
 * Loading 风格（对应 [AgUiExtraKeys.LOADING_STYLE]）。
 */
public object LoadingStyle {
    /** 三个跳动的点（适合等待回复） */
    public const val DOTS: String = "dots"
    /** 旋转指示器 */
    public const val SPINNER: String = "spinner"
    /** 进度条（需配合 [AgUiExtraKeys.ACTIVITY_PROGRESS]） */
    public const val PROGRESS: String = "progress"
    /** 脉冲骨架屏 */
    public const val SKELETON: String = "skeleton"
}

/**
 * 命令执行状态（对应 [AgUiExtraKeys.COMMAND_STATE]）。
 */
public object CommandState {
    public const val RUNNING: String = "running"
    public const val SUCCESS: String = "success"
    public const val FAILED: String = "failed"
}

/**
 * 文件操作类型（对应 [AgUiExtraKeys.FILE_OPERATION]）。
 */
public object FileOperation {
    public const val CREATE: String = "create"
    public const val UPDATE: String = "update"
    public const val DELETE: String = "delete"
    public const val READ: String = "read"
}
