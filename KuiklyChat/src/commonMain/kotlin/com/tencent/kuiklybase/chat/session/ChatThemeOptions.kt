package com.tencent.kuiklybase.chat.session

import com.tencent.kuiklybase.chat.model.*


// ============================
// 配置分组（参考 Stream Chat 的 ChatTheme + Options 分层设计）
// ============================

/**
 * 主题配置（参考 Stream Chat 的 ChatTheme / StreamColors / StreamShapes / StreamDimens）
 *
 * 集中管理所有视觉相关配置，与行为配置分离。
 * 对应 Stream Chat 的 colors + shapes + dimens + typography 概念。
 */
class ChatThemeOptions {
    // ---- 主题模式 ----
    /** 主题模式（浅色/深色/跟随系统） */
    var themeMode: ChatThemeMode = ChatThemeMode.LIGHT

    // ---- Colors（对应 StreamColors） ----
    /** 主色（用于自己的气泡、导航栏等） */
    var primaryColor: Long = 0xFF4F8FFF
    /** 渐变结束色 */
    var primaryGradientEndColor: Long = 0xFF6C5CE7
    /** 页面背景色（当没有设置背景图时生效） */
    var backgroundColor: Long = 0xFFF0F2F5
    /** 对方消息气泡背景色（默认白色） */
    var otherBubbleColor: Long = 0xFFFFFFFF
    /** 对方消息文字颜色 */
    var otherTextColor: Long = 0xFF333333
    /** 自己消息文字颜色 */
    var selfTextColor: Long = 0xFFFFFFFF

    // ---- Shapes（对应 StreamShapes） ----
    /** 头像圆角半径（20f = 圆形，8f = 微信风格圆角方形，0f = 方形） */
    var avatarRadius: Float = 8f

    // ---- Dimens（对应 StreamDimens） ----
    /** 气泡最大宽度占屏幕宽度的比例（默认 0.65） */
    var bubbleMaxWidthRatio: Float = 0.65f
    /** 气泡内水平 padding（单侧，默认 12f） */
    var bubblePaddingH: Float = 12f
    /** 气泡内垂直 padding（单侧，默认 10f） */
    var bubblePaddingV: Float = 10f
    /** 消息文字大小（默认 15f） */
    var messageFontSize: Float = 15f
    /** 消息行高（默认 22f） */
    var messageLineHeight: Float = 22f
    /** 头像尺寸（宽高相同，默认 40f） */
    var avatarSize: Float = 40f
    /** 消息行垂直 padding（默认 6f） */
    var rowPaddingV: Float = 6f
    /** 消息行水平 padding（默认 12f） */
    var rowPaddingH: Float = 12f
    /** 头像与气泡的间距（默认 8f） */
    var avatarBubbleGap: Float = 8f
    /** 聊天区域背景图 URL（设置后 backgroundColor 对消息列表区域不生效） */
    var backgroundImage: String = ""
    /** 图片消息的最大宽度（默认 200f） */
    var imageMaxWidth: Float = 200f
    /** 图片消息的最大高度（默认 200f） */
    var imageMaxHeight: Float = 200f
    /** 图片消息的圆角（默认 12f） */
    var imageRadius: Float = 12f

    // ---- MessageComposer 主题（参考 Stream Chat ChatTheme 中 composer 相关配置） ----
    /** 输入栏背景色 */
    var composerBackgroundColor: Long = 0xFFF8F8F8
    /** 输入栏顶部边框色 */
    var composerBorderColor: Long = 0xFFE0E0E0
    /** 输入框背景色 */
    var composerInputBackgroundColor: Long = 0xFFFFFFFF
    /** 输入框边框色 */
    var composerInputBorderColor: Long = 0xFFE0E0E0
    /** 输入框文字颜色 */
    var composerInputTextColor: Long = 0xFF333333
    /** 输入框占位文字颜色 */
    var composerPlaceholderColor: Long = 0xFFBBBBBB
    /** 发送按钮文字颜色 */
    var composerSendButtonTextColor: Long = 0xFFFFFFFF

    // ---- ComposerHeader 主题（回复/编辑提示条） ----
    /** ComposerHeader 背景色 */
    var composerHeaderBackgroundColor: Long = 0xFFF0F2F5
    /** ComposerHeader 文字颜色 */
    var composerHeaderTextColor: Long = 0xFF666666
    /** ComposerHeader 关闭按钮颜色 */
    var composerHeaderCloseColor: Long = 0xFF999999
    /** ComposerHeader 引用竖线颜色 */
    var composerHeaderBarColor: Long = 0xFF4F8FFF
    /** ComposerHeader 引用文字颜色 */
    var composerHeaderQuoteTextColor: Long = 0xFF999999

    /**
     * 获取当前主题的颜色方案。
     * 如果用户在 theme { } 中手动设置了颜色，则使用用户设置的值。
     * 否则根据 themeMode 返回对应的预定义颜色。
     */
    fun resolvedColors(): ChatThemeColors {
        val base = resolveThemeColors(themeMode)
        // 如果用户手动设置了某些颜色，用用户值覆盖预定义值
        return base.copy(
            primaryColor = primaryColor,
            primaryGradientEndColor = primaryGradientEndColor,
            backgroundColor = backgroundColor,
            otherBubbleColor = otherBubbleColor,
            otherTextColor = otherTextColor,
            selfTextColor = selfTextColor,
            composerBackgroundColor = composerBackgroundColor,
            composerBorderColor = composerBorderColor,
            composerInputBackgroundColor = composerInputBackgroundColor,
            composerInputBorderColor = composerInputBorderColor,
            composerInputTextColor = composerInputTextColor,
            composerPlaceholderColor = composerPlaceholderColor,
            composerSendButtonTextColor = composerSendButtonTextColor,
            composerHeaderBackgroundColor = composerHeaderBackgroundColor,
            composerHeaderTextColor = composerHeaderTextColor,
            composerHeaderCloseColor = composerHeaderCloseColor,
            composerHeaderBarColor = composerHeaderBarColor,
            composerHeaderQuoteTextColor = composerHeaderQuoteTextColor
        )
    }

    /**
     * 应用预定义主题色到当前配置（便捷方法）
     */
    fun applyThemeColors(colors: ChatThemeColors) {
        primaryColor = colors.primaryColor
        primaryGradientEndColor = colors.primaryGradientEndColor
        backgroundColor = colors.backgroundColor
        otherBubbleColor = colors.otherBubbleColor
        otherTextColor = colors.otherTextColor
        selfTextColor = colors.selfTextColor
    }

    /**
     * 切换到深色主题
     */
    fun useDarkTheme() {
        themeMode = ChatThemeMode.DARK
        applyThemeColors(DarkThemeColors)
    }

    /**
     * 切换到浅色主题
     */
    fun useLightTheme() {
        themeMode = ChatThemeMode.LIGHT
        applyThemeColors(LightThemeColors)
    }
}
