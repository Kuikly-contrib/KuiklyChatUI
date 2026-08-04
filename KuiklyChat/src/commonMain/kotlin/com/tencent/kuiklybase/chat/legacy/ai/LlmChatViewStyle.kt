package com.tencent.kuiklybase.chat.legacy.ai

import com.tencent.kuiklybase.chat.compose.agentcard.ComposeCardTheme

enum class LlmChatDesignSystem {
    MATERIAL,
    CUPERTINO
}

/**
 * [KuiklyAiChatView] 的视觉配置。
 *
 * 对齐 Flutter AI Toolkit 的 `LlmChatViewStyle`。颜色沿用本仓库既有约定，
 * 用 `Long` 表示 ARGB（与 `ComposeCardTheme` / `ChatThemeOptions` 一致），
 * 渲染时再包一层 `Color(...)`。
 *
 * [cardTheme] 透传给 `compose/agentcard/` 的富内容渲染器，
 * 这样气泡配色与代码块/表格配色能一起切换，不会出现"气泡是深色、
 * 里面的代码块还是浅色"这种割裂。
 */
data class LlmChatViewStyle(
    val designSystem: LlmChatDesignSystem = LlmChatDesignSystem.MATERIAL,
    // ---- 背景 ----
    val backgroundColor: Long = 0xFFF8FAFC,

    // ---- 用户气泡 ----
    val userBubbleGradientStart: Long = 0xFF6366F1,
    val userBubbleGradientEnd: Long = 0xFF8B5CF6,
    val userTextColor: Long = 0xFFFFFFFF,

    // ---- 模型回复 ----
    val llmTextColor: Long = 0xFF334155,
    val llmBubbleColor: Long = 0xFFFFFFFF,

    // ---- 文字 ----
    val titleColor: Long = 0xFF0F172A,
    val mutedColor: Long = 0xFF64748B,
    val fontSize: Float = 14f,
    val lineHeight: Float = 20f,

    // ---- 输入区 ----
    val composerBackgroundColor: Long = 0xFFFFFFFF,
    val composerBorderColor: Long = 0xFFE2E8F0,
    val composerTextColor: Long = 0xFF0F172A,
    val composerPlaceholderColor: Long = 0xFF94A3B8,
    val sendButtonColor: Long = 0xFF6366F1,
    val sendButtonDisabledColor: Long = 0xFFCBD5E1,
    val stopButtonColor: Long = 0xFFEF4444,

    // ---- 状态色 ----
    val errorColor: Long = 0xFFEF4444,
    val errorBackgroundColor: Long = 0xFFFEF2F2,

    // ---- 建议提示 ----
    val suggestionBackgroundColor: Long = 0xFFEEF2FF,
    val suggestionBorderColor: Long = 0xFFC7D2FE,
    val suggestionTextColor: Long = 0xFF4338CA,

    // ---- 形状与间距 ----
    val bubbleRadius: Float = 12f,
    val composerRadius: Float = 20f,
    val contentPaddingH: Float = 12f,
    val messageSpacing: Float = 10f,

    /** 富内容（Markdown / 代码块 / 表格）主题 */
    val cardTheme: ComposeCardTheme = ComposeCardTheme.Default
) {
    companion object {
        val Default = LlmChatViewStyle()

        val Material = Default

        val Cupertino = Default.copy(
            designSystem = LlmChatDesignSystem.CUPERTINO,
            backgroundColor = 0xFFF2F2F7,
            userBubbleGradientStart = 0xFF007AFF,
            userBubbleGradientEnd = 0xFF007AFF,
            llmTextColor = 0xFF1C1C1E,
            llmBubbleColor = 0xFFFFFFFF,
            titleColor = 0xFF1C1C1E,
            mutedColor = 0xFF8E8E93,
            composerBackgroundColor = 0xFFF2F2F7,
            composerBorderColor = 0xFFC7C7CC,
            composerTextColor = 0xFF1C1C1E,
            composerPlaceholderColor = 0xFF8E8E93,
            sendButtonColor = 0xFF007AFF,
            sendButtonDisabledColor = 0xFFAEAEB2,
            stopButtonColor = 0xFFFF3B30,
            suggestionBackgroundColor = 0xFFFFFFFF,
            suggestionBorderColor = 0xFFD1D1D6,
            suggestionTextColor = 0xFF007AFF,
            bubbleRadius = 18f,
            composerRadius = 18f,
            cardTheme = ComposeCardTheme.Default
        )

        fun forDesignSystem(designSystem: LlmChatDesignSystem): LlmChatViewStyle = when (
            designSystem
        ) {
            LlmChatDesignSystem.MATERIAL -> Material
            LlmChatDesignSystem.CUPERTINO -> Cupertino
        }

        /** 深色主题。 */
        val Dark = LlmChatViewStyle(
            backgroundColor = 0xFF0F172A,
            llmTextColor = 0xFFE2E8F0,
            llmBubbleColor = 0xFF1E293B,
            titleColor = 0xFFF1F5F9,
            mutedColor = 0xFF94A3B8,
            composerBackgroundColor = 0xFF1E293B,
            composerBorderColor = 0xFF334155,
            composerTextColor = 0xFFF1F5F9,
            composerPlaceholderColor = 0xFF64748B,
            errorBackgroundColor = 0xFF450A0A,
            suggestionBackgroundColor = 0xFF1E1B4B,
            suggestionBorderColor = 0xFF3730A3,
            suggestionTextColor = 0xFFC7D2FE,
            cardTheme = ComposeCardTheme(
                cardBackground = 0xFF1E293B,
                titleColor = 0xFFF1F5F9,
                bodyColor = 0xFFE2E8F0,
                mutedColor = 0xFF94A3B8,
                codeBlock = com.tencent.kuiklybase.chat.compose.agentcard
                    .ComposeCodeBlockTheme.Dark
            )
        )
    }
}

/**
 * 界面文案。抽出来是为了让宿主做多语言，而不是在库里硬编码中文。
 */
data class LlmChatViewStrings(
    val inputPlaceholder: String = "问点什么…",
    val sendLabel: String = "发送",
    val stopLabel: String = "停止",
    val retryLabel: String = "重试",
    val editLabel: String = "编辑",
    val copyLabel: String = "复制",
    val editSubmitLabel: String = "发送",
    val editingLabel: String = "正在编辑最新消息",
    val editCancelLabel: String = "取消",
    val attachLabel: String = "附件",
    val voiceLabel: String = "语音",
    val photoLibraryLabel: String = "相册",
    val cameraLabel: String = "拍照",
    val fileLabel: String = "文件",
    val welcomeTitle: String = "有什么可以帮你的？",
    val welcomeSubtitle: String = "",
    val thinkingLabel: String = "正在思考…",
    val listeningLabel: String = "正在聆听…",
    val canceledLabel: String = "已停止生成",
    val emptyAttachmentName: String = "附件",
    val speechUnavailableHint: String = "当前设备不支持语音输入，请接入第三方语音识别 SDK"
) {
    companion object {
        val Default = LlmChatViewStrings()
    }
}
