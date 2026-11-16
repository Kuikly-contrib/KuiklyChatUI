package com.tencent.kuiklybase.chat.model

/**
 * 从 Loading 消息的 extra 中提取 Loading 配置。
 */
public data class LoadingInfo(
    val title: String,
    val subtitle: String,
    val style: String,
    val target: String
) {
    public companion object {
        public fun from(message: ChatMessage): LoadingInfo = LoadingInfo(
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
public data class CommandInfo(
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

    public companion object {
        public fun from(message: ChatMessage): CommandInfo = CommandInfo(
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
public data class CodeBlockInfo(
    val language: String,
    val filename: String,
    val lineCount: Int,
    val code: String
) {
    public companion object {
        public fun from(message: ChatMessage): CodeBlockInfo = CodeBlockInfo(
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
public data class FileEditInfo(
    val filePath: String,
    val operation: String,
    val additions: Int,
    val deletions: Int,
    val diffText: String
) {
    public companion object {
        public fun from(message: ChatMessage): FileEditInfo = FileEditInfo(
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
public data class ToolCallInfo(
    val toolName: String,
    val toolCallId: String,
    val args: String,
    val result: String,
    val errorMessage: String
) {
    public companion object {
        public fun from(message: ChatMessage): ToolCallInfo = ToolCallInfo(
            toolName = message.extra[AgUiExtraKeys.TOOL_CALL_NAME].orEmpty(),
            toolCallId = message.extra[AgUiExtraKeys.TOOL_CALL_ID].orEmpty(),
            args = message.extra[AgUiExtraKeys.TOOL_ARGS].orEmpty(),
            result = message.content,
            errorMessage = message.extra[AgUiExtraKeys.TOOL_ERROR_MESSAGE].orEmpty()
        )
    }
}
