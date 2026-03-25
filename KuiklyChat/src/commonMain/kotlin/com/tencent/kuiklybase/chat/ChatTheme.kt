package com.tencent.kuiklybase.chat

/**
 * 主题模式枚举（对标 Stream Chat 的 ChatTheme 双色系支持）
 */
enum class ChatThemeMode {
    /** 浅色模式 */
    LIGHT,
    /** 深色模式 */
    DARK,
    /** 跟随系统（预留，当前默认按 LIGHT 处理） */
    SYSTEM
}

/**
 * 主题颜色配置集合
 *
 * 将颜色独立为数据类，便于 light/dark 两套色系切换。
 */
data class ChatThemeColors(
    /** 主色（用于自己的气泡、导航栏等） */
    val primaryColor: Long = 0xFF4F8FFF,
    /** 渐变结束色 */
    val primaryGradientEndColor: Long = 0xFF6C5CE7,
    /** 页面背景色 */
    val backgroundColor: Long = 0xFFF0F2F5,
    /** 对方消息气泡背景色 */
    val otherBubbleColor: Long = 0xFFFFFFFF,
    /** 对方消息文字颜色 */
    val otherTextColor: Long = 0xFF333333,
    /** 自己消息文字颜色 */
    val selfTextColor: Long = 0xFFFFFFFF,
    /** 系统消息背景色 */
    val systemMessageBgColor: Long = 0xFFCECECE,
    /** 系统消息文字颜色 */
    val systemMessageTextColor: Long = 0xFFFFFFFF,
    /** 日期分隔符背景色 */
    val dateSeparatorBgColor: Long = 0xFFCECECE,
    /** 日期分隔符文字颜色 */
    val dateSeparatorTextColor: Long = 0xFFFFFFFF,
    /** 操作菜单背景色 */
    val menuBackgroundColor: Long = 0xFFFFFFFF,
    /** 操作菜单文字颜色 */
    val menuTextColor: Long = 0xFF333333,
    /** 操作菜单危险操作文字颜色 */
    val menuDestructiveColor: Long = 0xFFFF4444,
    /** 引用回复区域背景色 */
    val quoteReplyBgColor: Long = 0x1A000000,
    /** 引用回复竖线颜色 */
    val quoteReplyBarColor: Long = 0xFF4F8FFF,
    /** 引用回复文字颜色 */
    val quoteReplyTextColor: Long = 0xFF666666,
    /** 反应栏背景色 */
    val reactionBgColor: Long = 0xFFF0F0F0,
    /** 反应栏高亮背景色（自己已反应） */
    val reactionHighlightBgColor: Long = 0xFFE3F2FD,
    /** 反应栏文字颜色 */
    val reactionTextColor: Long = 0xFF666666,
    /** 输入指示器文字颜色 */
    val typingIndicatorTextColor: Long = 0xFF999999,
    /** 输入指示器圆点颜色 */
    val typingIndicatorDotColor: Long = 0xFF999999,
    /** 头像占位背景色 */
    val avatarPlaceholderColor: Long = 0xFFE8E8E8,
    /** 名字文字颜色 */
    val senderNameColor: Long = 0xFF999999,
    /** 导航栏标题颜色 */
    val navigationTitleColor: Long = 0xFFFFFFFF,
    /** 遮罩层颜色 */
    val overlayColor: Long = 0x99000000,
    /** 分隔线颜色 */
    val dividerColor: Long = 0xFFE0E0E0,
    /** 已读回执文字颜色 */
    val readReceiptColor: Long = 0xFF999999,
    /** 未读消息计数背景色 */
    val unreadBadgeBgColor: Long = 0xFFFF4444,
    /** 未读消息计数文字颜色 */
    val unreadBadgeTextColor: Long = 0xFFFFFFFF,
    /** 在线状态指示器颜色 */
    val onlineIndicatorColor: Long = 0xFF4CAF50,
    /** 置顶标记颜色 */
    val pinnedIndicatorColor: Long = 0xFF4F8FFF,
    /** 已编辑标记文字颜色 */
    val editedLabelColor: Long = 0xFF999999
)

/**
 * 预定义的浅色主题颜色方案
 */
val LightThemeColors = ChatThemeColors()

/**
 * 预定义的深色主题颜色方案
 */
val DarkThemeColors = ChatThemeColors(
    primaryColor = 0xFF5B9FFF,
    primaryGradientEndColor = 0xFF7B6EE7,
    backgroundColor = 0xFF1A1A2E,
    otherBubbleColor = 0xFF2D2D44,
    otherTextColor = 0xFFE0E0E0,
    selfTextColor = 0xFFFFFFFF,
    systemMessageBgColor = 0xFF3D3D5C,
    systemMessageTextColor = 0xFFB0B0B0,
    dateSeparatorBgColor = 0xFF3D3D5C,
    dateSeparatorTextColor = 0xFFB0B0B0,
    menuBackgroundColor = 0xFF2D2D44,
    menuTextColor = 0xFFE0E0E0,
    menuDestructiveColor = 0xFFFF6B6B,
    quoteReplyBgColor = 0x33FFFFFF,
    quoteReplyBarColor = 0xFF5B9FFF,
    quoteReplyTextColor = 0xFFB0B0B0,
    reactionBgColor = 0xFF2D2D44,
    reactionHighlightBgColor = 0xFF1A3A5C,
    reactionTextColor = 0xFFB0B0B0,
    typingIndicatorTextColor = 0xFF808080,
    typingIndicatorDotColor = 0xFF808080,
    avatarPlaceholderColor = 0xFF3D3D5C,
    senderNameColor = 0xFF808080,
    navigationTitleColor = 0xFFE0E0E0,
    overlayColor = 0xCC000000,
    dividerColor = 0xFF3D3D5C,
    readReceiptColor = 0xFF808080,
    unreadBadgeBgColor = 0xFFFF6B6B,
    unreadBadgeTextColor = 0xFFFFFFFF,
    onlineIndicatorColor = 0xFF66BB6A,
    pinnedIndicatorColor = 0xFF5B9FFF,
    editedLabelColor = 0xFF808080
)

/**
 * 根据主题模式解析当前应使用的颜色方案
 *
 * @param mode 主题模式
 * @return 对应的颜色配置
 */
fun resolveThemeColors(mode: ChatThemeMode): ChatThemeColors {
    return when (mode) {
        ChatThemeMode.LIGHT -> LightThemeColors
        ChatThemeMode.DARK -> DarkThemeColors
        ChatThemeMode.SYSTEM -> LightThemeColors // 预留，当前默认 LIGHT
    }
}
