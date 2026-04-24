package com.tencent.kuiklybase.chat.agentcard

import com.tencent.kuikly.core.base.Color

/**
 * AgentCard 统一主题配置。
 *
 * 所有卡片都使用此处的颜色与尺寸，保证视觉一致性。业务方可通过构造新实例覆盖。
 */
data class AgentCardTheme(
    // ---- 背景 ----
    val cardBackground: Long = 0xFFFFFFFF,
    val codeBackground: Long = 0xFF0D1117,          // GitHub Dark 深邃黑
    val terminalBackground: Long = 0xFF0D1117,      // 终端黑
    val inlineCodeBackground: Long = 0xFFF1F5F9,

    // ---- 边框 ----
    val borderColor: Long = 0xFFE2E8F0,
    val accentBorder: Long = 0xFF6366F1,

    // ---- 文字 ----
    val titleColor: Long = 0xFF0F172A,
    val bodyColor: Long = 0xFF334155,
    val mutedColor: Long = 0xFF64748B,
    val codeColor: Long = 0xFFE6EDF3,
    val linkColor: Long = 0xFF3B82F6,

    // ---- 状态色 ----
    val successColor: Long = 0xFF10B981,
    val warningColor: Long = 0xFFF59E0B,
    val errorColor: Long = 0xFFEF4444,
    val infoColor: Long = 0xFF3B82F6,

    // ---- 行号背景 ----
    val lineNumberBackground: Long = 0xFF0D1117,
    val lineNumberColor: Long = 0xFF484F58,

    // ---- Diff 色 ----
    val diffAddBg: Long = 0xFFDCFCE7,
    val diffAddText: Long = 0xFF15803D,
    val diffRemoveBg: Long = 0xFFFEE2E2,
    val diffRemoveText: Long = 0xFFB91C1C,

    // ---- 圆角 ----
    val cardRadius: Float = 10f,
    val innerRadius: Float = 6f,

    // ---- 间距 ----
    val cardPaddingH: Float = 12f,
    val cardPaddingV: Float = 10f,
    val cardMarginV: Float = 6f,
    val cardMarginH: Float = 12f
) {
    companion object {
        val Default = AgentCardTheme()
    }
}
