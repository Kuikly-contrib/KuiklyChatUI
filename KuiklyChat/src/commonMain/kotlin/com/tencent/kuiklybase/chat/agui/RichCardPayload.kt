package com.tencent.kuiklybase.chat.agui

import com.tencent.kuiklybase.chat.model.ChatMessage
import com.tencent.kuiklybase.chat.model.ChatMessageHelper
import com.tencent.kuiklybase.chat.model.MessageStatus
import com.tencent.kuiklybase.chat.model.MessageType

/**
 * 富消息卡片构建器
 *
 * 为 AI-Agent 场景提供**类型安全**的消息卡片创建 API，
 * 内部统一使用 [ChatMessage.extra] 中的 [AgUiExtraKeys] 标准 key 承载结构化数据，
 * 避免业务方重复发明 `extra` 字段名。
 *
 * 所有方法返回的 [ChatMessage] 可直接 upsert 到 [AgUiSession]，由 [com.tencent.kuiklybase.chat.bubble.MessageRendererFactory]
 * 根据 `agUiKind` 分发到对应渲染器。
 *
 * ### 使用示例
 *
 * ```kotlin
 * // 代码块
 * session.upsertMessage(
 *     RichCardPayload.codeBlock(
 *         id = "msg_code_1",
 *         code = "fun hello() = println(\"Hi\")",
 *         language = "kotlin",
 *         filename = "Hello.kt"
 *     )
 * )
 *
 * // 命令执行中
 * session.upsertMessage(
 *     RichCardPayload.commandExec(
 *         id = "cmd_1",
 *         command = "ls -la",
 *         state = CommandState.RUNNING
 *     )
 * )
 *
 * // 命令完成
 * session.upsertMessage(
 *     RichCardPayload.commandExec(
 *         id = "cmd_1",
 *         command = "ls -la",
 *         output = "total 8\ndrwx...",
 *         exitCode = 0,
 *         state = CommandState.SUCCESS
 *     )
 * )
 * ```
 */
object RichCardPayload {

    /**
     * 构造基础 AI 消息骨架。
     *
     * @param id 消息 ID
     * @param kind [RichMessageKind] 中的某个值，或业务自定义字符串
     * @param content 消息主体文本（用于默认渲染 / 复制 / 搜索）
     * @param extra 附加元数据（会与 agUiKind 合并）
     * @param type 底层 MessageType（默认 CUSTOM，让 MessageRendererFactory 去分发）
     * @param status 消息状态
     * @param senderName AI 名称
     * @param senderAvatar AI 头像
     */
    fun aiMessage(
        id: String,
        kind: String,
        content: String = "",
        extra: Map<String, String> = emptyMap(),
        type: MessageType = MessageType.CUSTOM,
        status: MessageStatus = MessageStatus.SENT,
        senderName: String = "AI 助手",
        senderAvatar: String = "",
        senderId: String = "ai"
    ): ChatMessage {
        val merged = buildMap {
            putAll(extra)
            put(AgUiExtraKeys.KIND, kind)
        }
        return ChatMessage(
            id = id,
            content = content,
            isSelf = false,
            type = type,
            status = status,
            senderName = senderName,
            senderAvatar = senderAvatar,
            senderId = senderId,
            extra = merged
        )
    }

    // ============================
    // 代码块卡片
    // ============================

    /**
     * 构造一个代码块卡片消息。
     *
     * @param id 消息 ID
     * @param code 代码文本
     * @param language 编程语言（"kotlin", "python", "js", "shell" ...）
     * @param filename 可选文件名（不为空时会显示在标题栏）
     */
    fun codeBlock(
        id: String,
        code: String,
        language: String = "",
        filename: String = "",
        senderName: String = "AI 助手",
        senderAvatar: String = ""
    ): ChatMessage = aiMessage(
        id = id,
        kind = RichMessageKind.CODE_BLOCK,
        content = code,
        extra = buildMap {
            if (language.isNotEmpty()) put(AgUiExtraKeys.CODE_LANGUAGE, language)
            if (filename.isNotEmpty()) put(AgUiExtraKeys.CODE_FILENAME, filename)
            put(AgUiExtraKeys.CODE_LINE_COUNT, code.count { it == '\n' }.plus(1).toString())
        },
        senderName = senderName,
        senderAvatar = senderAvatar
    )

    // ============================
    // 命令执行卡片
    // ============================

    /**
     * 构造命令执行卡片。
     *
     * 同一个 `id` 可多次 upsert：初始 `state=RUNNING`，完成时传 `SUCCESS`/`FAILED` 并带上 output。
     */
    fun commandExec(
        id: String,
        command: String,
        output: String = "",
        cwd: String = "",
        state: String = CommandState.RUNNING,
        exitCode: Int? = null,
        durationMs: Long? = null,
        senderName: String = "AI 助手",
        senderAvatar: String = ""
    ): ChatMessage = aiMessage(
        id = id,
        kind = RichMessageKind.COMMAND_EXEC,
        content = output, // 主内容为输出文本
        status = if (state == CommandState.RUNNING) MessageStatus.SENDING else MessageStatus.SENT,
        extra = buildMap {
            put(AgUiExtraKeys.COMMAND_CMD, command)
            if (cwd.isNotEmpty()) put(AgUiExtraKeys.COMMAND_CWD, cwd)
            put(AgUiExtraKeys.COMMAND_STATE, state)
            exitCode?.let { put(AgUiExtraKeys.COMMAND_EXIT_CODE, it.toString()) }
            durationMs?.let { put(AgUiExtraKeys.COMMAND_DURATION_MS, it.toString()) }
        },
        senderName = senderName,
        senderAvatar = senderAvatar
    )

    // ============================
    // 文件编辑卡片
    // ============================

    /**
     * 构造文件编辑卡片。
     *
     * 适合展示 AI 修改了什么文件、改了多少行。
     *
     * @param operation [FileOperation] 其一
     * @param diffText 可选 unified diff 文本，卡片展开时展示
     * @param additions 增加的行数
     * @param deletions 删除的行数
     */
    fun fileEdit(
        id: String,
        filePath: String,
        operation: String,
        diffText: String = "",
        additions: Int = 0,
        deletions: Int = 0,
        senderName: String = "AI 助手",
        senderAvatar: String = ""
    ): ChatMessage = aiMessage(
        id = id,
        kind = RichMessageKind.FILE_EDIT,
        content = diffText,
        extra = buildMap {
            put(AgUiExtraKeys.FILE_PATH, filePath)
            put(AgUiExtraKeys.FILE_OPERATION, operation)
            if (additions > 0) put(AgUiExtraKeys.FILE_DIFF_ADDITIONS, additions.toString())
            if (deletions > 0) put(AgUiExtraKeys.FILE_DIFF_DELETIONS, deletions.toString())
        },
        senderName = senderName,
        senderAvatar = senderAvatar
    )

    /**
     * 构造文件读取预览卡片。
     */
    fun fileRead(
        id: String,
        filePath: String,
        preview: String = "",
        language: String = "",
        senderName: String = "AI 助手",
        senderAvatar: String = ""
    ): ChatMessage = aiMessage(
        id = id,
        kind = RichMessageKind.FILE_READ,
        content = preview,
        extra = buildMap {
            put(AgUiExtraKeys.FILE_PATH, filePath)
            put(AgUiExtraKeys.FILE_OPERATION, FileOperation.READ)
            if (language.isNotEmpty()) put(AgUiExtraKeys.CODE_LANGUAGE, language)
        },
        senderName = senderName,
        senderAvatar = senderAvatar
    )

    // ============================
    // Loading 占位消息
    // ============================

    /**
     * 构造一条 Loading 占位消息。
     *
     * 配合 [AgUiSession.showLoading] / [AgUiSession.dismissLoading] 使用更方便。
     *
     * @param title 主标题（"正在思考"、"正在搜索文件..."）
     * @param subtitle 副标题（动态子任务）
     * @param style [LoadingStyle] 其一
     * @param target 相关的目标（工具名、文件名等）
     */
    fun loading(
        id: String,
        title: String = "正在思考",
        subtitle: String = "",
        style: String = LoadingStyle.DOTS,
        target: String = "",
        senderName: String = "AI 助手",
        senderAvatar: String = ""
    ): ChatMessage = aiMessage(
        id = id,
        kind = RichMessageKind.LOADING,
        content = title,
        status = MessageStatus.SENDING,
        extra = buildMap {
            put(AgUiExtraKeys.LOADING_TITLE, title)
            if (subtitle.isNotEmpty()) put(AgUiExtraKeys.LOADING_SUBTITLE, subtitle)
            put(AgUiExtraKeys.LOADING_STYLE, style)
            if (target.isNotEmpty()) put(AgUiExtraKeys.LOADING_TARGET, target)
        },
        senderName = senderName,
        senderAvatar = senderAvatar
    )

    // ============================
    // 工具调用卡片
    // ============================

    /**
     * 构造工具调用中的占位卡片（等待结果）。
     */
    fun toolCallPending(
        id: String,
        toolName: String,
        toolCallId: String = "",
        args: String = "",
        senderName: String = "AI 助手",
        senderAvatar: String = ""
    ): ChatMessage = aiMessage(
        id = id,
        kind = RichMessageKind.TOOL_CALL_PENDING,
        content = "正在调用工具：$toolName",
        status = MessageStatus.SENDING,
        extra = buildMap {
            put(AgUiExtraKeys.TOOL_CALL_NAME, toolName)
            if (toolCallId.isNotEmpty()) put(AgUiExtraKeys.TOOL_CALL_ID, toolCallId)
            if (args.isNotEmpty()) put(AgUiExtraKeys.TOOL_ARGS, args)
            put(AgUiExtraKeys.LOADING_STYLE, LoadingStyle.SPINNER)
            put(AgUiExtraKeys.LOADING_TARGET, toolName)
        },
        senderName = senderName,
        senderAvatar = senderAvatar
    )

    /** 构造工具调用结果卡片。 */
    fun toolResult(
        id: String,
        toolName: String,
        toolCallId: String = "",
        result: String,
        senderName: String = "AI 助手",
        senderAvatar: String = ""
    ): ChatMessage = aiMessage(
        id = id,
        kind = RichMessageKind.TOOL_RESULT,
        content = result,
        extra = buildMap {
            put(AgUiExtraKeys.TOOL_CALL_NAME, toolName)
            if (toolCallId.isNotEmpty()) put(AgUiExtraKeys.TOOL_CALL_ID, toolCallId)
        },
        senderName = senderName,
        senderAvatar = senderAvatar
    )

    /** 构造工具调用失败卡片。 */
    fun toolError(
        id: String,
        toolName: String,
        errorMessage: String,
        toolCallId: String = "",
        senderName: String = "AI 助手",
        senderAvatar: String = ""
    ): ChatMessage = aiMessage(
        id = id,
        kind = RichMessageKind.TOOL_ERROR,
        content = errorMessage,
        status = MessageStatus.FAILED,
        extra = buildMap {
            put(AgUiExtraKeys.TOOL_CALL_NAME, toolName)
            if (toolCallId.isNotEmpty()) put(AgUiExtraKeys.TOOL_CALL_ID, toolCallId)
            put(AgUiExtraKeys.TOOL_ERROR_MESSAGE, errorMessage)
        },
        senderName = senderName,
        senderAvatar = senderAvatar
    )

    // ============================
    // 错误展示卡片
    // ============================

    /** 构造错误消息卡片。 */
    fun error(
        id: String,
        message: String,
        code: String = "",
        senderName: String = "AI 助手",
        senderAvatar: String = ""
    ): ChatMessage = aiMessage(
        id = id,
        kind = RichMessageKind.ERROR,
        content = message,
        status = MessageStatus.FAILED,
        extra = buildMap {
            put(AgUiExtraKeys.ERROR_MESSAGE, message)
            if (code.isNotEmpty()) put(AgUiExtraKeys.ERROR_CODE, code)
        },
        senderName = senderName,
        senderAvatar = senderAvatar
    )

    // ============================
    // 搜索结果 / 链接预览 / 建议
    // ============================

    /** 构造一条 Markdown 消息。 */
    fun markdown(
        id: String,
        markdown: String,
        senderName: String = "AI 助手",
        senderAvatar: String = ""
    ): ChatMessage = aiMessage(
        id = id,
        kind = RichMessageKind.MARKDOWN,
        content = markdown,
        senderName = senderName,
        senderAvatar = senderAvatar
    )

    /**
     * 构造建议选项卡片（quick reply）。
     *
     * 各选项通过 extra 的 `suggestion.N.label` / `suggestion.N.value` 传递。
     */
    fun suggestions(
        id: String,
        prompt: String,
        options: List<Pair<String, String>>,
        senderName: String = "AI 助手",
        senderAvatar: String = ""
    ): ChatMessage = aiMessage(
        id = id,
        kind = RichMessageKind.SUGGESTIONS,
        content = prompt,
        extra = buildMap {
            options.forEachIndexed { i, (label, value) ->
                put("suggestion.$i.label", label)
                put("suggestion.$i.value", value)
            }
            put("suggestion.count", options.size.toString())
        },
        senderName = senderName,
        senderAvatar = senderAvatar
    )
}

/**
 * 从 Loading 消息的 extra 中提取 Loading 配置。
 */
data class LoadingInfo(
    val title: String,
    val subtitle: String,
    val style: String,
    val target: String
) {
    companion object {
        fun from(message: ChatMessage): LoadingInfo = LoadingInfo(
            title = message.extra[AgUiExtraKeys.LOADING_TITLE]
                ?: message.content.ifEmpty { "加载中" },
            subtitle = message.extra[AgUiExtraKeys.LOADING_SUBTITLE].orEmpty(),
            style = message.extra[AgUiExtraKeys.LOADING_STYLE] ?: LoadingStyle.DOTS,
            target = message.extra[AgUiExtraKeys.LOADING_TARGET].orEmpty()
        )
    }
}

/**
 * 从命令执行消息的 extra 中提取命令信息。
 */
data class CommandInfo(
    val command: String,
    val cwd: String,
    val state: String,
    val exitCode: Int?,
    val durationMs: Long?,
    val output: String
) {
    val isRunning: Boolean get() = state == CommandState.RUNNING
    val isSuccess: Boolean get() = state == CommandState.SUCCESS
    val isFailed: Boolean get() = state == CommandState.FAILED

    companion object {
        fun from(message: ChatMessage): CommandInfo = CommandInfo(
            command = message.extra[AgUiExtraKeys.COMMAND_CMD].orEmpty(),
            cwd = message.extra[AgUiExtraKeys.COMMAND_CWD].orEmpty(),
            state = message.extra[AgUiExtraKeys.COMMAND_STATE] ?: CommandState.RUNNING,
            exitCode = message.extra[AgUiExtraKeys.COMMAND_EXIT_CODE]?.toIntOrNull(),
            durationMs = message.extra[AgUiExtraKeys.COMMAND_DURATION_MS]?.toLongOrNull(),
            output = message.content
        )
    }
}

/**
 * 从代码块消息的 extra 中提取代码信息。
 */
data class CodeBlockInfo(
    val language: String,
    val filename: String,
    val lineCount: Int,
    val code: String
) {
    companion object {
        fun from(message: ChatMessage): CodeBlockInfo = CodeBlockInfo(
            language = message.extra[AgUiExtraKeys.CODE_LANGUAGE].orEmpty(),
            filename = message.extra[AgUiExtraKeys.CODE_FILENAME].orEmpty(),
            lineCount = message.extra[AgUiExtraKeys.CODE_LINE_COUNT]?.toIntOrNull() ?: 0,
            code = message.content
        )
    }
}

/**
 * 从文件编辑消息的 extra 中提取文件信息。
 */
data class FileEditInfo(
    val filePath: String,
    val operation: String,
    val additions: Int,
    val deletions: Int,
    val diffText: String
) {
    companion object {
        fun from(message: ChatMessage): FileEditInfo = FileEditInfo(
            filePath = message.extra[AgUiExtraKeys.FILE_PATH].orEmpty(),
            operation = message.extra[AgUiExtraKeys.FILE_OPERATION] ?: FileOperation.UPDATE,
            additions = message.extra[AgUiExtraKeys.FILE_DIFF_ADDITIONS]?.toIntOrNull() ?: 0,
            deletions = message.extra[AgUiExtraKeys.FILE_DIFF_DELETIONS]?.toIntOrNull() ?: 0,
            diffText = message.content
        )
    }
}

/**
 * 从工具调用消息的 extra 中提取工具信息。
 */
data class ToolCallInfo(
    val toolName: String,
    val toolCallId: String,
    val args: String,
    val result: String,
    val errorMessage: String
) {
    companion object {
        fun from(message: ChatMessage): ToolCallInfo = ToolCallInfo(
            toolName = message.extra[AgUiExtraKeys.TOOL_CALL_NAME].orEmpty(),
            toolCallId = message.extra[AgUiExtraKeys.TOOL_CALL_ID].orEmpty(),
            args = message.extra[AgUiExtraKeys.TOOL_ARGS].orEmpty(),
            result = message.content,
            errorMessage = message.extra[AgUiExtraKeys.TOOL_ERROR_MESSAGE].orEmpty()
        )
    }
}
