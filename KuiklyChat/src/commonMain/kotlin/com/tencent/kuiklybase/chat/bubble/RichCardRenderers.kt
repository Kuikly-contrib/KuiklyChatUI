package com.tencent.kuiklybase.chat.bubble

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.views.*
import com.tencent.kuiklybase.chat.agui.*
import com.tencent.kuiklybase.chat.model.ChatMessage
import com.tencent.kuiklybase.chat.model.MessageContext
import com.tencent.kuiklybase.chat.session.ChatSessionConfig

// ============================
// 通用工具：AI 卡片外壳
// ============================

/**
 * 统一的 AI 卡片外壳：左侧头像占位 + 右侧卡片主体。
 *
 * 提供一致的气泡左右布局，避免每个卡片重复写 `View { width(48f) } View { flex(1f) ... }`。
 *
 * @param accentColor 主题强调色（左侧竖条 + 标题）
 * @param borderColor 卡片边框色（null 不画边框）
 * @param backgroundColor 卡片背景色
 * @param header 头部 slot（图标 + 标题）
 * @param body 主体 slot（展示核心内容）
 */
internal fun ViewContainer<*, *>.agUiCardShell(
    accentColor: Color,
    borderColor: Color? = null,
    backgroundColor: Color = Color(0xFFFFFFFF),
    header: ViewContainer<*, *>.() -> Unit,
    body: ViewContainer<*, *>.() -> Unit
) {
    View {
        attr {
            flexDirectionRow()
            alignSelfStretch()
            paddingLeft(12f)
            paddingRight(12f)
            paddingTop(4f)
            paddingBottom(4f)
        }
        // 左侧头像占位
        View { attr { width(48f) } }

        // 右侧卡片
        View {
            attr {
                flex(1f)
                borderRadius(10f)
                backgroundColor(backgroundColor)
                borderColor?.let { border(Border(1f, BorderStyle.SOLID, it)) }
                paddingLeft(12f)
                paddingRight(12f)
                paddingTop(10f)
                paddingBottom(10f)
            }
            // Header
            View {
                attr {
                    flexDirectionRow()
                    alignItemsCenter()
                    marginBottom(8f)
                }
                // 左侧强调色竖条
                View {
                    attr {
                        width(3f)
                        height(14f)
                        borderRadius(1.5f)
                        backgroundColor(accentColor)
                        marginRight(8f)
                    }
                }
                header()
            }
            body()
        }
    }
}

// ============================
// 1. Loading 卡片渲染器
// ============================

/**
 * Loading 占位消息渲染器。
 *
 * 匹配 `agUiKind == "loading"`，根据 [LoadingStyle] 渲染不同的指示器：
 * - `dots`：三个跳动的点
 * - `spinner`：旋转图标
 * - `progress`：进度条
 * - `skeleton`：骨架屏
 */
class LoadingCardRenderer : MessageRendererFactory {
    override fun canRender(message: ChatMessage): Boolean =
        RichMessageKind.matches(message, RichMessageKind.LOADING) ||
            RichMessageKind.matches(message, RichMessageKind.TOOL_CALL_PENDING)

    override fun render(container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig) {
        val info = LoadingInfo.from(context.message)
        container.agUiCardShell(
            accentColor = Color(0xFF4F8FFF),
            borderColor = Color(0xFFE0E7FF),
            backgroundColor = Color(0xFFF5F8FF),
            header = {
                // Loading 点点动画（静态三个点，实际抖动靠 CSS/动画 —— Kuikly 可通过 animator 实现，这里用占位符）
                LoadingIndicator(info.style, Color(0xFF4F8FFF))
                Text {
                    attr {
                        text(info.title)
                        fontSize(13f)
                        color(Color(0xFF1A202C))
                        fontWeightSemiBold()
                        flex(1f)
                        marginLeft(8f)
                    }
                }
            },
            body = {
                if (info.subtitle.isNotEmpty()) {
                    Text {
                        attr {
                            text(info.subtitle)
                            fontSize(12f)
                            color(Color(0xFF4A5568))
                            lineHeight(18f)
                        }
                    }
                }
                if (info.target.isNotEmpty()) {
                    View {
                        attr {
                            flexDirectionRow()
                            alignItemsCenter()
                            marginTop(6f)
                            paddingLeft(8f)
                            paddingRight(8f)
                            paddingTop(3f)
                            paddingBottom(3f)
                            borderRadius(10f)
                            backgroundColor(Color(0xFFE0E7FF))
                            alignSelfFlexStart()
                        }
                        Text {
                            attr {
                                text("→ ${info.target}")
                                fontSize(11f)
                                color(Color(0xFF3730A3))
                            }
                        }
                    }
                }
            }
        )
    }
}

/**
 * Loading 小图标（根据风格渲染不同样式）。
 *
 * 注：真正的动画效果需要业务方结合 Animator 实现，这里先提供视觉占位。
 */
internal fun ViewContainer<*, *>.LoadingIndicator(style: String, color: Color) {
    when (style) {
        LoadingStyle.SPINNER -> {
            Text {
                attr {
                    text("⏳")
                    fontSize(14f)
                }
            }
        }
        LoadingStyle.PROGRESS -> {
            Text {
                attr {
                    text("▰")
                    fontSize(14f)
                    color(color)
                }
            }
        }
        LoadingStyle.SKELETON -> {
            View {
                attr {
                    width(14f)
                    height(14f)
                    borderRadius(3f)
                    backgroundColor(Color(0xFFE2E8F0))
                }
            }
        }
        else -> {
            // DOTS：静态三个点
            View {
                attr {
                    flexDirectionRow()
                    alignItemsCenter()
                }
                repeat(3) { i ->
                    View {
                        attr {
                            size(5f, 5f)
                            borderRadius(2.5f)
                            backgroundColor(color)
                            if (i < 2) marginRight(2f)
                        }
                    }
                }
            }
        }
    }
}

// ============================
// 2. 代码块卡片渲染器
// ============================

/**
 * 代码块卡片渲染器。
 *
 * 渲染样式：
 * ```
 * ┌──────────────────────────────────┐
 * │ kotlin │ Hello.kt        📋 复制 │ ← 头部
 * ├──────────────────────────────────┤
 * │ fun main() {                     │
 * │     println("Hello")             │
 * │ }                                │
 * └──────────────────────────────────┘
 * ```
 */
class CodeBlockCardRenderer(
    /** 复制按钮点击回调 */
    val onCopyClick: ((code: String) -> Unit)? = null
) : MessageRendererFactory {
    override fun canRender(message: ChatMessage): Boolean =
        RichMessageKind.matches(message, RichMessageKind.CODE_BLOCK)

    override fun render(container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig) {
        val info = CodeBlockInfo.from(context.message)
        container.View {
            attr {
                flexDirectionRow()
                alignSelfStretch()
                paddingLeft(12f)
                paddingRight(12f)
                paddingTop(4f)
                paddingBottom(4f)
            }
            View { attr { width(48f) } }

            View {
                attr {
                    flex(1f)
                    borderRadius(8f)
                    backgroundColor(Color(0xFF1E293B))
                    border(Border(1f, BorderStyle.SOLID, Color(0xFF334155)))
                }

                // 头部：语言 + 文件名 + 复制按钮
                View {
                    attr {
                        flexDirectionRow()
                        alignItemsCenter()
                        paddingLeft(10f)
                        paddingRight(10f)
                        paddingTop(6f)
                        paddingBottom(6f)
                        backgroundColor(Color(0xFF0F172A))
                        borderRadius(8f)
                    }
                    if (info.language.isNotEmpty()) {
                        View {
                            attr {
                                borderRadius(3f)
                                backgroundColor(Color(0xFF6366F1))
                                paddingLeft(6f)
                                paddingRight(6f)
                                paddingTop(2f)
                                paddingBottom(2f)
                                marginRight(8f)
                            }
                            Text {
                                attr {
                                    text(info.language)
                                    fontSize(10f)
                                    color(Color.WHITE)
                                    fontWeightSemiBold()
                                }
                            }
                        }
                    }
                    Text {
                        attr {
                            text(info.filename.ifEmpty { "code" })
                            fontSize(11f)
                            color(Color(0xFF94A3B8))
                            flex(1f)
                        }
                    }
                    if (info.lineCount > 0) {
                        Text {
                            attr {
                                text("${info.lineCount} 行")
                                fontSize(10f)
                                color(Color(0xFF64748B))
                                marginRight(10f)
                            }
                        }
                    }
                    // 复制按钮
                    View {
                        attr {
                            paddingLeft(6f)
                            paddingRight(6f)
                            paddingTop(2f)
                            paddingBottom(2f)
                            borderRadius(4f)
                            backgroundColor(Color(0x22FFFFFF))
                        }
                        Text {
                            attr {
                                text("📋 复制")
                                fontSize(10f)
                                color(Color.WHITE)
                            }
                        }
                        event {
                            click { onCopyClick?.invoke(info.code) }
                        }
                    }
                }

                // 代码内容
                View {
                    attr {
                        paddingLeft(12f)
                        paddingRight(12f)
                        paddingTop(10f)
                        paddingBottom(10f)
                    }
                    Text {
                        attr {
                            text(info.code)
                            fontSize(12f)
                            color(Color(0xFFE2E8F0))
                            lineHeight(17f)
                        }
                    }
                }
            }
        }
    }
}

// ============================
// 3. 命令执行卡片渲染器
// ============================

/**
 * 命令执行卡片渲染器。
 *
 * 渲染样式：
 * ```
 * ┌──────────────────────────────────────┐
 * │ ● running  ~/work                    │
 * │ $ ls -la                             │
 * │ total 8                              │
 * │ drwx... ...                          │
 * └──────────────────────────────────────┘
 * ```
 */
class CommandExecCardRenderer : MessageRendererFactory {
    override fun canRender(message: ChatMessage): Boolean =
        RichMessageKind.matches(message, RichMessageKind.COMMAND_EXEC)

    override fun render(container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig) {
        val info = CommandInfo.from(context.message)
        val statusColor = when (info.state) {
            CommandState.SUCCESS -> Color(0xFF10B981)
            CommandState.FAILED -> Color(0xFFEF4444)
            else -> Color(0xFFFBBF24)
        }
        val statusText = when (info.state) {
            CommandState.SUCCESS -> "● success"
            CommandState.FAILED -> "● failed"
            else -> "◌ running"
        }

        container.View {
            attr {
                flexDirectionRow()
                alignSelfStretch()
                paddingLeft(12f)
                paddingRight(12f)
                paddingTop(4f)
                paddingBottom(4f)
            }
            View { attr { width(48f) } }

            View {
                attr {
                    flex(1f)
                    borderRadius(8f)
                    backgroundColor(Color(0xFF0F172A))
                    border(Border(1f, BorderStyle.SOLID, Color(0xFF1E293B)))
                }

                // Header: 状态 + CWD + 耗时
                View {
                    attr {
                        flexDirectionRow()
                        alignItemsCenter()
                        paddingLeft(10f)
                        paddingRight(10f)
                        paddingTop(6f)
                        paddingBottom(6f)
                    }
                    Text {
                        attr {
                            text(statusText)
                            fontSize(10f)
                            color(statusColor)
                            fontWeightSemiBold()
                            marginRight(8f)
                        }
                    }
                    if (info.cwd.isNotEmpty()) {
                        Text {
                            attr {
                                text(info.cwd)
                                fontSize(10f)
                                color(Color(0xFF64748B))
                                flex(1f)
                            }
                        }
                    } else {
                        View { attr { flex(1f) } }
                    }
                    info.durationMs?.let { ms ->
                        Text {
                            attr {
                                text("${ms}ms")
                                fontSize(10f)
                                color(Color(0xFF64748B))
                                marginRight(6f)
                            }
                        }
                    }
                    info.exitCode?.let { code ->
                        Text {
                            attr {
                                text("exit $code")
                                fontSize(10f)
                                color(if (code == 0) Color(0xFF10B981) else Color(0xFFEF4444))
                            }
                        }
                    }
                }

                // 分隔线
                View {
                    attr {
                        height(1f)
                        backgroundColor(Color(0xFF1E293B))
                    }
                }

                // Body: 命令 + 输出
                View {
                    attr {
                        paddingLeft(12f)
                        paddingRight(12f)
                        paddingTop(8f)
                        paddingBottom(8f)
                    }
                    Text {
                        attr {
                            text("$ ${info.command}")
                            fontSize(12f)
                            color(Color(0xFF93C5FD))
                            lineHeight(17f)
                            marginBottom(4f)
                        }
                    }
                    if (info.output.isNotEmpty()) {
                        Text {
                            attr {
                                text(info.output)
                                fontSize(11f)
                                color(Color(0xFFCBD5E1))
                                lineHeight(16f)
                            }
                        }
                    } else if (info.isRunning) {
                        View {
                            attr {
                                flexDirectionRow()
                                alignItemsCenter()
                                marginTop(2f)
                            }
                            LoadingIndicator(LoadingStyle.DOTS, Color(0xFFFBBF24))
                            Text {
                                attr {
                                    text("  执行中...")
                                    fontSize(11f)
                                    color(Color(0xFF64748B))
                                    marginLeft(6f)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================
// 4. 文件编辑 / 读取卡片
// ============================

/**
 * 文件编辑卡片渲染器。
 *
 * 渲染样式：
 * ```
 * ┌──────────────────────────────────────┐
 * │ ✏️ update  src/Main.kt  +12 -3       │
 * │ ┌──────────────────────────────────┐ │
 * │ │ + added line                     │ │
 * │ │ - removed line                   │ │
 * │ └──────────────────────────────────┘ │
 * └──────────────────────────────────────┘
 * ```
 */
class FileEditCardRenderer : MessageRendererFactory {
    override fun canRender(message: ChatMessage): Boolean =
        RichMessageKind.matches(message, RichMessageKind.FILE_EDIT) ||
            RichMessageKind.matches(message, RichMessageKind.FILE_READ)

    override fun render(container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig) {
        val info = FileEditInfo.from(context.message)
        val isRead = info.operation == FileOperation.READ
        val (emoji, label, accent) = when (info.operation) {
            FileOperation.CREATE -> Triple("➕", "新建", Color(0xFF10B981))
            FileOperation.DELETE -> Triple("🗑️", "删除", Color(0xFFEF4444))
            FileOperation.READ -> Triple("📖", "读取", Color(0xFF3B82F6))
            else -> Triple("✏️", "修改", Color(0xFF8B5CF6))
        }

        container.agUiCardShell(
            accentColor = accent,
            borderColor = Color(0xFFE5E7EB),
            backgroundColor = Color(0xFFFAFAFA),
            header = {
                Text {
                    attr {
                        text(emoji)
                        fontSize(14f)
                        marginRight(4f)
                    }
                }
                Text {
                    attr {
                        text(label)
                        fontSize(12f)
                        color(accent)
                        fontWeightSemiBold()
                        marginRight(8f)
                    }
                }
                Text {
                    attr {
                        text(info.filePath)
                        fontSize(12f)
                        color(Color(0xFF1F2937))
                        flex(1f)
                        lines(1)
                    }
                }
                if (!isRead && (info.additions > 0 || info.deletions > 0)) {
                    if (info.additions > 0) {
                        Text {
                            attr {
                                text("+${info.additions}")
                                fontSize(11f)
                                color(Color(0xFF10B981))
                                fontWeightSemiBold()
                                marginLeft(6f)
                            }
                        }
                    }
                    if (info.deletions > 0) {
                        Text {
                            attr {
                                text("-${info.deletions}")
                                fontSize(11f)
                                color(Color(0xFFEF4444))
                                fontWeightSemiBold()
                                marginLeft(4f)
                            }
                        }
                    }
                }
            },
            body = {
                if (info.diffText.isNotEmpty()) {
                    View {
                        attr {
                            borderRadius(6f)
                            backgroundColor(Color(0xFFF3F4F6))
                            paddingLeft(10f)
                            paddingRight(10f)
                            paddingTop(8f)
                            paddingBottom(8f)
                        }
                        Text {
                            attr {
                                text(info.diffText)
                                fontSize(11f)
                                color(Color(0xFF374151))
                                lineHeight(16f)
                            }
                        }
                    }
                }
            }
        )
    }
}

// ============================
// 5. 工具结果卡片
// ============================

class ToolInvocationCardRenderer : MessageRendererFactory {
    override fun canRender(message: ChatMessage): Boolean =
        RichMessageKind.matches(message, RichMessageKind.TOOL_RESULT) ||
            RichMessageKind.matches(message, RichMessageKind.TOOL_ERROR)

    override fun render(container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig) {
        val info = ToolCallInfo.from(context.message)
        val isError = RichMessageKind.matches(context.message, RichMessageKind.TOOL_ERROR)
        val accent = if (isError) Color(0xFFEF4444) else Color(0xFF9F7AEA)
        val bg = if (isError) Color(0xFFFEF2F2) else Color(0xFFFAF5FF)

        container.agUiCardShell(
            accentColor = accent,
            borderColor = accent,
            backgroundColor = bg,
            header = {
                Text {
                    attr {
                        text(if (isError) "⚠️" else "🔧")
                        fontSize(14f)
                        marginRight(6f)
                    }
                }
                Text {
                    attr {
                        text(if (isError) "工具调用失败" else "工具调用")
                        fontSize(12f)
                        color(accent)
                        fontWeightSemiBold()
                        flex(1f)
                    }
                }
                if (info.toolName.isNotEmpty()) {
                    View {
                        attr {
                            borderRadius(4f)
                            backgroundColor(accent)
                            paddingLeft(6f)
                            paddingRight(6f)
                            paddingTop(2f)
                            paddingBottom(2f)
                        }
                        Text {
                            attr {
                                text(info.toolName)
                                fontSize(10f)
                                color(Color.WHITE)
                            }
                        }
                    }
                }
            },
            body = {
                if (info.args.isNotEmpty() && !isError) {
                    View {
                        attr {
                            borderRadius(4f)
                            backgroundColor(Color(0x11000000))
                            paddingLeft(8f)
                            paddingRight(8f)
                            paddingTop(4f)
                            paddingBottom(4f)
                            marginBottom(6f)
                        }
                        Text {
                            attr {
                                text(info.args)
                                fontSize(11f)
                                color(Color(0xFF4A5568))
                                lineHeight(15f)
                            }
                        }
                    }
                }
                Text {
                    attr {
                        text(if (isError) info.errorMessage else info.result)
                        fontSize(13f)
                        color(if (isError) Color(0xFF991B1B) else Color(0xFF2D3436))
                        lineHeight(18f)
                    }
                }
            }
        )
    }
}

// ============================
// 6. 错误卡片
// ============================

class ErrorCardRenderer : MessageRendererFactory {
    override fun canRender(message: ChatMessage): Boolean =
        RichMessageKind.matches(message, RichMessageKind.ERROR)

    override fun render(container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig) {
        val msg = context.message
        val errorMsg = msg.extra[AgUiExtraKeys.ERROR_MESSAGE].orEmpty().ifEmpty { msg.content }
        val errorCode = msg.extra[AgUiExtraKeys.ERROR_CODE].orEmpty()

        container.agUiCardShell(
            accentColor = Color(0xFFEF4444),
            borderColor = Color(0xFFFECACA),
            backgroundColor = Color(0xFFFEF2F2),
            header = {
                Text {
                    attr {
                        text("❌")
                        fontSize(14f)
                        marginRight(6f)
                    }
                }
                Text {
                    attr {
                        text("错误")
                        fontSize(12f)
                        color(Color(0xFFDC2626))
                        fontWeightSemiBold()
                        flex(1f)
                    }
                }
                if (errorCode.isNotEmpty()) {
                    View {
                        attr {
                            borderRadius(4f)
                            backgroundColor(Color(0xFFEF4444))
                            paddingLeft(6f)
                            paddingRight(6f)
                            paddingTop(2f)
                            paddingBottom(2f)
                        }
                        Text {
                            attr {
                                text(errorCode)
                                fontSize(10f)
                                color(Color.WHITE)
                            }
                        }
                    }
                }
            },
            body = {
                Text {
                    attr {
                        text(errorMsg)
                        fontSize(13f)
                        color(Color(0xFF991B1B))
                        lineHeight(18f)
                    }
                }
            }
        )
    }
}

// ============================
// 7. 建议选项卡片
// ============================

class SuggestionsCardRenderer(
    /** 点击某个建议时的回调：(value) -> Unit */
    val onSuggestionClick: ((String) -> Unit)? = null
) : MessageRendererFactory {
    override fun canRender(message: ChatMessage): Boolean =
        RichMessageKind.matches(message, RichMessageKind.SUGGESTIONS)

    override fun render(container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig) {
        val msg = context.message
        val count = msg.extra["suggestion.count"]?.toIntOrNull() ?: 0
        val options = (0 until count).map { i ->
            val label = msg.extra["suggestion.$i.label"].orEmpty()
            val value = msg.extra["suggestion.$i.value"].orEmpty()
            label to value
        }

        container.agUiCardShell(
            accentColor = Color(0xFF10B981),
            borderColor = Color(0xFFE5E7EB),
            backgroundColor = Color(0xFFFFFFFF),
            header = {
                Text {
                    attr {
                        text("💡")
                        fontSize(14f)
                        marginRight(6f)
                    }
                }
                Text {
                    attr {
                        text(msg.content.ifEmpty { "你可以这样回复：" })
                        fontSize(12f)
                        color(Color(0xFF1F2937))
                        flex(1f)
                    }
                }
            },
            body = {
                View {
                    attr {
                        flexDirectionRow()
                        flexWrapWrap()
                    }
                    options.forEach { (label, value) ->
                        View {
                            attr {
                                borderRadius(14f)
                                backgroundColor(Color(0xFFF0FDF4))
                                border(Border(1f, BorderStyle.SOLID, Color(0xFF10B981)))
                                paddingLeft(10f)
                                paddingRight(10f)
                                paddingTop(5f)
                                paddingBottom(5f)
                                marginRight(6f)
                                marginBottom(6f)
                            }
                            Text {
                                attr {
                                    text(label)
                                    fontSize(12f)
                                    color(Color(0xFF047857))
                                }
                            }
                            event {
                                click { onSuggestionClick?.invoke(value) }
                            }
                        }
                    }
                }
            }
        )
    }
}

// ============================
// 8. 表格卡片渲染器
// ============================

/**
 * 表格卡片渲染器。
 *
 * 将 Markdown 表格渲染为带边框的网格布局：
 * ```
 * ┌─────────┬─────────┬─────────┐
 * │  Header │  Header │  Header │
 * ├─────────┼─────────┼─────────┤
 * │  Cell   │  Cell   │  Cell   │
 * └─────────┴─────────┴─────────┘
 * ```
 */
class TableCardRenderer : MessageRendererFactory {
    override fun canRender(message: ChatMessage): Boolean =
        RichMessageKind.matches(message, RichMessageKind.TABLE)

    override fun render(container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig) {
        val msg = context.message
        val headers = parseJsonStringArray(msg.extra[AgUiExtraKeys.TABLE_HEADERS].orEmpty())
        val rows = parseJson2DArray(msg.extra[AgUiExtraKeys.TABLE_ROWS].orEmpty())
        val caption = msg.extra[AgUiExtraKeys.TABLE_CAPTION].orEmpty()

        container.agUiCardShell(
            accentColor = Color(0xFF3B82F6),
            borderColor = Color(0xFFBFDBFE),
            backgroundColor = Color(0xFFFFFFFF),
            header = {
                Text {
                    attr {
                        text("📊")
                        fontSize(14f)
                        marginRight(6f)
                    }
                }
                Text {
                    attr {
                        text(caption.ifEmpty { "表格" })
                        fontSize(12f)
                        color(Color(0xFF1E3A5F))
                        fontWeightSemiBold()
                        flex(1f)
                    }
                }
            },
            body = {
                if (headers.isNotEmpty() || rows.isNotEmpty()) {
                    val allCols = if (headers.isNotEmpty()) headers.size else rows.firstOrNull()?.size ?: 0

                    // 表头行
                    if (headers.isNotEmpty()) {
                        View {
                            attr {
                                flexDirectionRow()
                                backgroundColor(Color(0xFFEBF5FF))
                                borderRadius(6f, 6f, 0f, 0f)
                            }
                            headers.forEach { h ->
                                View {
                                    attr {
                                        if (allCols > 0) flex(1f / allCols) else flex(1f)
                                        padding(8f)
                                        borderRight(Border(1f, BorderStyle.SOLID, Color(0xFFBFDBFE)))
                                    }
                                    Text {
                                        attr {
                                            text(h)
                                            fontSize(11f)
                                            color(Color(0xFF1E3A5F))
                                            fontWeightSemiBold()
                                            lines(1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // 数据行
                    rows.forEachIndexed { rowIdx, row ->
                        View {
                            attr {
                                flexDirectionRow()
                                if (rowIdx == rows.size - 1) borderRadius(0f, 0f, 6f, 6f)
                                backgroundColor(if (rowIdx % 2 == 0) Color(0xFFFFFFFF) else Color(0xFFF8FAFC))
                            }
                            row.forEach { cell ->
                                View {
                                    attr {
                                        if (allCols > 0) flex(1f / allCols) else flex(1f)
                                        padding(8f)
                                        borderRight(Border(1f, BorderStyle.SOLID, Color(0xFFE2E8F0)))
                                        borderBottom(Border(1f, BorderStyle.SOLID, Color(0xFFE2E8F0)))
                                    }
                                    Text {
                                        attr {
                                            text(cell)
                                            fontSize(11f)
                                            color(Color(0xFF334155))
                                            lineHeight(16f)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text {
                        attr {
                            text("（空表格）")
                            fontSize(12f)
                            color(Color(0xFF94A3B8))
                        }
                    }
                }
            }
        )
    }

    private fun parseJsonStringArray(json: String): List<String> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.optString(it, "") ?: "" }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseJson2DArray(json: String): List<List<String>> {
        if (json.isBlank()) return emptyList()
        return try {
            val outer = JSONArray(json)
            (0 until outer.length()).map { i ->
                val inner = outer.optJSONArray(i)
                (0 until (inner?.length() ?: 0)).map { j ->
                    inner?.optString(j, "") ?: ""
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

// ============================
// 9. 引用块卡片渲染器
// ============================

/**
 * 引用块卡片渲染器。
 *
 * 左侧蓝色竖条 + 斜体文字，模拟 Markdown `> ...` 效果：
 * ```
 * ┃ 这是被引用的内容，
 * ┃ 可以有多行。
 * ```
 */
class QuoteCardRenderer : MessageRendererFactory {
    override fun canRender(message: ChatMessage): Boolean =
        RichMessageKind.matches(message, RichMessageKind.QUOTE)

    override fun render(container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig) {
        val msg = context.message
        val source = msg.extra[AgUiExtraKeys.QUOTE_SOURCE].orEmpty()

        container.agUiCardShell(
            accentColor = Color(0xFF3B82F6),
            borderColor = null,
            backgroundColor = Color(0xFFF0F9FF),
            header = {
                Text {
                    attr {
                        text("💬")
                        fontSize(14f)
                        marginRight(6f)
                    }
                }
                Text {
                    attr {
                        text("引用")
                        fontSize(12f)
                        color(Color(0xFF3B82F6))
                        fontWeightSemiBold()
                        flex(1f)
                    }
                }
                if (source.isNotEmpty()) {
                    Text {
                        attr {
                            text("— $source")
                            fontSize(10f)
                            color(Color(0xFF94A3B8))
                        }
                    }
                }
            },
            body = {
                Text {
                    attr {
                        text(msg.content)
                        fontSize(13f)
                        color(Color(0xFF334155))
                        lineHeight(20f)
                        fontStyleItalic()
                    }
                }
            }
        )
    }
}

// ============================
// 10. 折叠内容卡片渲染器
// ============================

/**
 * 折叠内容卡片渲染器（模拟 HTML `<details>` / `<summary>`）。
 *
 * 展示为可点击的摘要标题，点击后展开/收起正文。
 * 注：Kuikly DSL 中通过状态切换 + vif 实现展开收起。
 */
class CollapsibleCardRenderer : MessageRendererFactory {
    override fun canRender(message: ChatMessage): Boolean =
        RichMessageKind.matches(message, RichMessageKind.COLLAPSIBLE)

    override fun render(container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig) {
        val msg = context.message
        val summary = msg.extra[AgUiExtraKeys.COLLAPSIBLE_SUMMARY].orEmpty()
            .ifEmpty { "展开详情" }

        container.agUiCardShell(
            accentColor = Color(0xFF8B5CF6),
            borderColor = Color(0xFFD8B4FE),
            backgroundColor = Color(0xFFFAF5FF),
            header = {
                Text {
                    attr {
                        text("▶")
                        fontSize(12f)
                        color(Color(0xFF8B5CF6))
                        marginRight(6f)
                    }
                }
                Text {
                    attr {
                        text(summary)
                        fontSize(12f)
                        color(Color(0xFF5B21B6))
                        fontWeightSemiBold()
                        flex(1f)
                    }
                }
            },
            body = {
                // 折叠内容默认展开显示（在 DSL 中可用 vif 实现点击切换，
                // 这里为了简化先直接展示全文；业务方可自行封装带状态的组件）
                Text {
                    attr {
                        text(msg.content)
                        fontSize(12f)
                        color(Color(0xFF4C1D95))
                        lineHeight(18f)
                    }
                }
            }
        )
    }
}

// ============================
// 11. Diff 对比卡片渲染器
// ============================

/**
 * Diff 对比卡片渲染器。
 *
 * 对 diff 文本做语法着色：
 * - `+` 行 → 绿色背景
 * - `-` 行 → 红色背景
 * - `@@` 行 → 灰色注释
 * - 上下文行 → 默认背景
 */
class DiffCardRenderer : MessageRendererFactory {
    override fun canRender(message: ChatMessage): Boolean =
        RichMessageKind.matches(message, RichMessageKind.DIFF)

    override fun render(container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig) {
        val msg = context.message
        val lines = msg.content.lines()
        val lang = msg.extra[AgUiExtraKeys.DIFF_LANGUAGE].orEmpty().ifEmpty { "diff" }

        container.View {
            attr {
                flexDirectionRow()
                alignSelfStretch()
                paddingLeft(12f)
                paddingRight(12f)
                paddingTop(4f)
                paddingBottom(4f)
            }
            View { attr { width(48f) } }

            View {
                attr {
                    flex(1f)
                    borderRadius(8f)
                    backgroundColor(Color(0xFF1E293B))
                    border(Border(1f, BorderStyle.SOLID, Color(0xFF334155)))
                }

                // Header
                View {
                    attr {
                        flexDirectionRow()
                        alignItemsCenter()
                        paddingLeft(10f)
                        paddingRight(10f)
                        paddingTop(6f)
                        paddingBottom(6f)
                        backgroundColor(Color(0xFF0F172A))
                    }
                    View {
                        attr {
                            borderRadius(3f)
                            backgroundColor(Color(0xFF10B981))
                            paddingLeft(6f)
                            paddingRight(6f)
                            paddingTop(2f)
                            paddingBottom(2f)
                            marginRight(8f)
                        }
                        Text {
                            attr {
                                text(lang)
                                fontSize(10f)
                                color(Color.WHITE)
                                fontWeightSemiBold()
                            }
                        }
                    }
                    Text {
                        attr {
                            text("${lines.size} 行变更")
                            fontSize(11f)
                            color(Color(0xFF94A3B8))
                            flex(1f)
                        }
                    }
                }

                // Diff 内容（行级着色）
                View {
                    attr {
                        paddingLeft(10f)
                        paddingRight(10f)
                        paddingTop(8f)
                        paddingBottom(8f)
                    }
                    lines.forEach { line ->
                        val (bgColor, textColor) = when {
                            line.startsWith("+") && !line.startsWith("+++") ->
                                Color(0x1A10B981) to Color(0xFF10B981)
                            line.startsWith("-") && !line.startsWith("---") ->
                                Color(0x1AEF4444) to Color(0xFFEF4444)
                            line.startsWith("@@") ->
                                Color(0xFF1E293B) to Color(0xFF94A3B8)
                            else -> Color(0xFF1E293B) to Color(0xFFE2E8F0)
                        }
                        View {
                            attr {
                                backgroundColor(bgColor)
                                paddingLeft(6f)
                                paddingRight(6f)
                                paddingTop(1f)
                                paddingBottom(1f)
                            }
                            Text {
                                attr {
                                    text(line)
                                    fontSize(11f)
                                    color(textColor)
                                    lineHeight(16f)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================
// 12. 任务列表卡片渲染器
// ============================

/**
 * 任务列表（Checklist / TODO list）渲染器。
 *
 * 渲染为带复选框的列表：
 * ```
 * ☑️ 已完成项
 * ☐  未完成项
 * ```
 */
class ChecklistCardRenderer : MessageRendererFactory {
    override fun canRender(message: ChatMessage): Boolean =
        RichMessageKind.matches(message, RichMessageKind.CHECKLIST)

    override fun render(container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig) {
        val msg = context.message
        val items = parseChecklistItems(msg.extra[AgUiExtraKeys.CHECKLIST_ITEMS].orEmpty())
        val completed = msg.extra[AgUiExtraKeys.CHECKLIST_COMPLETED]?.toIntOrNull() ?: 0

        container.agUiCardShell(
            accentColor = Color(0xFF10B981),
            borderColor = Color(0xFFA7F3D0),
            backgroundColor = Color(0xFFF0FDF4),
            header = {
                Text {
                    attr {
                        text("✅")
                        fontSize(14f)
                        marginRight(6f)
                    }
                }
                Text {
                    attr {
                        text("任务清单")
                        fontSize(12f)
                        color(Color(0xFF065F46))
                        fontWeightSemiBold()
                        flex(1f)
                    }
                }
                Text {
                    attr {
                        text("$completed / ${items.size}")
                        fontSize(11f)
                        color(Color(0xFF059669))
                        fontWeightSemiBold()
                    }
                }
            },
            body = {
                items.forEach { (text, checked) ->
                    View {
                        attr {
                            flexDirectionRow()
                            alignItemsCenter()
                            marginBottom(4f)
                        }
                        Text {
                            attr {
                                text(if (checked) "☑️" else "⬜")
                                fontSize(14f)
                                marginRight(8f)
                            }
                        }
                        Text {
                            attr {
                                text(text)
                                fontSize(12f)
                                color(if (checked) Color(0xFF94A3B8) else Color(0xFF334155))
                                lineHeight(18f)
                            }
                        }
                    }
                }
            }
        )
    }

    private fun parseChecklistItems(json: String): List<Pair<String, Boolean>> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.optJSONObject(i)
                val text = obj?.optString("text", "") ?: ""
                val checked = obj?.optBoolean("checked", false) ?: false
                text to checked
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

// ============================
// 统一注册入口
// ============================

/**
 * 返回一组 AG-UI 富卡片渲染器，可直接追加到 [ChatSessionConfig.messageRenderers]。
 *
 * 用法：
 * ```
 * ChatSession({ session.observableMessages }) {
 *     messageRenderers.addAll(agUiRichRenderers())
 * }
 * ```
 *
 * @param onCopyCode 代码块"复制"按钮点击回调
 * @param onSuggestionClick 建议选项卡片中某项点击回调
 */
fun agUiRichRenderers(
    onCopyCode: ((String) -> Unit)? = null,
    onSuggestionClick: ((String) -> Unit)? = null
): List<MessageRendererFactory> = listOf(
    LoadingCardRenderer(),
    CodeBlockCardRenderer(onCopyClick = onCopyCode),
    DiffCardRenderer(),
    CommandExecCardRenderer(),
    FileEditCardRenderer(),
    ToolInvocationCardRenderer(),
    ErrorCardRenderer(),
    SuggestionsCardRenderer(onSuggestionClick = onSuggestionClick),
    TableCardRenderer(),
    QuoteCardRenderer(),
    CollapsibleCardRenderer(),
    ChecklistCardRenderer()
)
