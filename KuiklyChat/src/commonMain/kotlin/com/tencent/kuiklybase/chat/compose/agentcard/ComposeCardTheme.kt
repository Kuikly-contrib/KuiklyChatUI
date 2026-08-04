package com.tencent.kuiklybase.chat.compose.agentcard

/**
 * AgentCard Compose 版主题配置。
 *
 * 所有颜色值、圆角、间距 1:1 对齐 [com.tencent.kuiklybase.chat.agentcard.AgentCardTheme]。
 */
/**
 * 代码块 / 终端 / 行号 / Diff 等"代码相关 UI"的统一调色板。
 *
 * 所有显示代码、命令行、文件 diff 的卡片（CodeBlockCard / ToolCallCard 内嵌
 * 代码块 / CommandExecCard / FileEditCard 等）都应该读这里的字段，方便整体切换
 * Light / Dark 主题，无需逐个文件改色值。
 *
 * 默认值采用 GitHub Light 风格：白底 + 浅灰边 + 深色文字。
 */
data class ComposeCodeBlockTheme(
    // ---- 主体 ----
    /** 代码 / 终端正文区背景 */
    val background: Long = 0xFFFFFFFF,
    /** 代码 / 终端正文文字色 */
    val textColor: Long = 0xFF24292F,
    /** 行内 code 背景（如 markdown `code`） */
    val inlineBackground: Long = 0xFFF8FAFC,

    // ---- 头部（文件名/标题栏）----
    /** 头部背景（区分主体的浅灰条） */
    val headerBackground: Long = 0xFFF8FAFC,
    /** 头部标题色 */
    val headerTitleColor: Long = 0xFF0F172A,
    /** 头部副信息色（行数、耗时等） */
    val headerMutedColor: Long = 0xFF64748B,

    // ---- 边框 / 分隔线 ----
    /** 代码块外框 + 头部底边分隔线 */
    val borderColor: Long = 0xFFE2E8F0,

    // ---- 行号 ----
    val lineNumberBackground: Long = 0xFFF8FAFC,
    val lineNumberColor: Long = 0xFF94A3B8,

    // ---- Diff ----
    val diffAddBg: Long = 0xFFDCFCE7,
    val diffAddText: Long = 0xFF15803D,
    val diffRemoveBg: Long = 0xFFFEE2E2,
    val diffRemoveText: Long = 0xFFB91C1C,
) {
    /** CodeHighlighter 是否使用深色主题（影响 token 配色） */
    val isDark: Boolean get() = background == 0xFF0D1117 || background == 0xFF161B22

    companion object {
        /** 默认浅色（GitHub Light） */
        val Light = ComposeCodeBlockTheme()

        /** 深色风格（GitHub Dark）— 备用 */
        val Dark = ComposeCodeBlockTheme(
            background = 0xFF0D1117,
            textColor = 0xFFE6EDF3,
            inlineBackground = 0xFF161B22,
            headerBackground = 0xFF161B22,
            headerTitleColor = 0xFFE6EDF3,
            headerMutedColor = 0xFF8B949E,
            borderColor = 0xFF30363D,
            lineNumberBackground = 0xFF0D1117,
            lineNumberColor = 0xFF484F58,
        )
    }
}

data class ComposeCardTheme(
    // ---- 背景 ----
    val cardBackground: Long = 0xFFFFFFFF,

    // ---- 边框 ----
    // 卡片边框：透明（去框风格）
    val borderColor: Long = 0x00FFFFFF,
    val accentBorder: Long = 0xFF6366F1,

    // ---- 文字 ----
    val titleColor: Long = 0xFF0F172A,
    val bodyColor: Long = 0xFF334155,
    val mutedColor: Long = 0xFF64748B,
    val linkColor: Long = 0xFF3B82F6,

    // ---- 状态色 ----
    val successColor: Long = 0xFF10B981,
    val warningColor: Long = 0xFFF59E0B,
    val errorColor: Long = 0xFFEF4444,
    val infoColor: Long = 0xFF3B82F6,

    // ---- 代码块（统一在 [ComposeCodeBlockTheme] 里管理）----
    val codeBlock: ComposeCodeBlockTheme = ComposeCodeBlockTheme.Light,

    // ---- 圆角 ----
    val cardRadius: Float = 10f,
    val innerRadius: Float = 6f,

    // ---- 间距 ----
    val cardPaddingH: Float = 12f,
    val cardPaddingV: Float = 10f,
    /** 卡片之间的纵向间距（用于无边框去框风格，需要更大留白） */
    val cardMarginV: Float = 10f,
    val cardMarginH: Float = 12f
) {
    // ===== 兼容旧字段：转发到 codeBlock，避免一次性改所有调用点 =====
    val codeBackground: Long get() = codeBlock.background
    val terminalBackground: Long get() = codeBlock.background
    val inlineCodeBackground: Long get() = codeBlock.inlineBackground
    val codeColor: Long get() = codeBlock.textColor
    val lineNumberBackground: Long get() = codeBlock.lineNumberBackground
    val lineNumberColor: Long get() = codeBlock.lineNumberColor
    val diffAddBg: Long get() = codeBlock.diffAddBg
    val diffAddText: Long get() = codeBlock.diffAddText
    val diffRemoveBg: Long get() = codeBlock.diffRemoveBg
    val diffRemoveText: Long get() = codeBlock.diffRemoveText

    companion object {
        val Default = ComposeCardTheme()
    }
}
