package com.tencent.kuiklybase.chat.channel

import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuiklybase.chat.model.*

// ============================
// Slot 类型定义（参考 Stream Chat Compose 的 ChannelList Slot API）
// ============================

/**
 * 频道列表项渲染 Slot
 *
 * 用于自定义单个频道项的渲染方式。
 * - container: 父容器
 * - channel: 频道数据
 * - config: 频道列表配置
 */
typealias ChannelItemSlot = (container: ViewContainer<*, *>, channel: ChatChannel, config: ChatChannelListConfig) -> Unit

/**
 * 频道列表头部 Slot（搜索栏、标题等）
 * - container: 父容器
 * - config: 频道列表配置
 */
typealias ChannelListHeaderSlot = (container: ViewContainer<*, *>, config: ChatChannelListConfig) -> Unit

/**
 * 频道列表空状态 Slot
 * - container: 父容器
 */
typealias ChannelListEmptySlot = (container: ViewContainer<*, *>) -> Unit

/**
 * 频道列表加载中 Slot
 * - container: 父容器
 */
typealias ChannelListLoadingSlot = (container: ViewContainer<*, *>) -> Unit

/**
 * 频道分隔线 Slot
 * - container: 父容器
 * - index: 当前频道在列表中的索引
 */
typealias ChannelDividerSlot = (container: ViewContainer<*, *>, index: Int) -> Unit

/**
 * 频道列表时间格式化器
 * 接收时间戳（毫秒），返回格式化后的时间字符串
 */
typealias ChannelTimeFormatter = (timestamp: Long) -> String

// ============================
// 频道列表主题配置
// ============================

/**
 * 频道列表主题颜色配置
 */
class ChannelListTheme {
    // ---- 整体背景 ----
    /** 页面背景色 */
    var backgroundColor: Long = 0xFFFFFFFF
    /** 主色（用于导航栏、高亮等） */
    var primaryColor: Long = 0xFF4F8FFF
    /** 渐变结束色 */
    var primaryGradientEndColor: Long = 0xFF6C5CE7

    // ---- 频道项 ----
    /** 频道项背景色 */
    var itemBackgroundColor: Long = 0xFFFFFFFF
    /** 频道项按下态背景色 */
    var itemPressedBackgroundColor: Long = 0xFFF5F5F5
    /** 置顶频道项背景色 */
    var pinnedItemBackgroundColor: Long = 0xFFF8F8FF
    /** 频道名称文字颜色 */
    var channelNameColor: Long = 0xFF333333
    /** 最后消息预览文字颜色 */
    var lastMessageColor: Long = 0xFF999999
    /** 时间文字颜色 */
    var timeColor: Long = 0xFFBBBBBB
    /** 未读计数背景色 */
    var unreadBadgeColor: Long = 0xFFFF4444
    /** 未读计数文字颜色 */
    var unreadBadgeTextColor: Long = 0xFFFFFFFF
    /** 静音图标颜色 */
    var mutedIconColor: Long = 0xFFBBBBBB
    /** 分隔线颜色 */
    var dividerColor: Long = 0xFFF0F0F0
    /** 头像占位背景色 */
    var avatarPlaceholderColor: Long = 0xFFE8E8E8
    /** 在线状态指示器颜色 */
    var onlineIndicatorColor: Long = 0xFF4CAF50

    // ---- 导航栏 ----
    /** 导航栏标题颜色 */
    var headerTitleColor: Long = 0xFFFFFFFF
    /** 搜索栏背景色 */
    var searchBarBackgroundColor: Long = 0xFFF0F2F5
    /** 搜索栏文字颜色 */
    var searchBarTextColor: Long = 0xFF333333
    /** 搜索栏占位文字颜色 */
    var searchBarPlaceholderColor: Long = 0xFFBBBBBB

    // ---- 尺寸 ----
    /** 频道项高度 */
    var itemHeight: Float = 72f
    /** 头像尺寸 */
    var avatarSize: Float = 48f
    /** 头像圆角 */
    var avatarRadius: Float = 8f
    /** 频道名称字号 */
    var channelNameFontSize: Float = 16f
    /** 最后消息预览字号 */
    var lastMessageFontSize: Float = 14f
    /** 时间字号 */
    var timeFontSize: Float = 12f
    /** 未读计数字号 */
    var unreadBadgeFontSize: Float = 11f
    /** 频道项水平内边距 */
    var itemPaddingH: Float = 16f
    /** 头像与文字间距 */
    var avatarTextGap: Float = 12f
}

// ============================
// 频道列表 Slot 配置
// ============================

/**
 * 频道列表 Slot 配置
 */
class ChannelListSlots {
    /**
     * 自定义频道列表项渲染。
     * 设置后替换默认的频道项渲染。
     * 参考 Stream Chat ChannelList 的 itemContent。
     */
    var channelItem: ChannelItemSlot? = null

    /**
     * 自定义频道列表头部（导航栏 + 搜索栏）。
     * 设置后替换默认头部。
     * 参考 Stream Chat ChannelList 的 headerContent。
     */
    var header: ChannelListHeaderSlot? = null

    /**
     * 频道列表为空时的占位渲染。
     * 参考 Stream Chat ChannelList 的 emptyContent。
     */
    var emptyContent: ChannelListEmptySlot? = null

    /**
     * 频道列表加载中的渲染。
     * 参考 Stream Chat ChannelList 的 loadingContent。
     */
    var loadingContent: ChannelListLoadingSlot? = null

    /**
     * 自定义频道项之间的分隔线。
     */
    var divider: ChannelDividerSlot? = null
}

// ============================
// 频道列表配置
// ============================

/**
 * ChatChannelList 的配置参数
 *
 * 架构参考 Stream Chat Compose SDK 的 ChannelList：
 * - theme: 主题配置（颜色、尺寸）
 * - slots: 渲染插槽配置
 * - 事件回调
 */
class ChatChannelListConfig {
    // ========== Options 分组 ==========

    /** 主题配置 */
    val theme = ChannelListTheme()
    /** 渲染插槽配置 */
    val slots = ChannelListSlots()

    // ========== DSL 配置方法 ==========

    /** DSL 方式配置主题 */
    fun theme(block: ChannelListTheme.() -> Unit) { theme.apply(block) }
    /** DSL 方式配置渲染插槽 */
    fun slots(block: ChannelListSlots.() -> Unit) { slots.apply(block) }

    // ========== 导航栏配置 ==========

    /** 导航栏标题 */
    var title: String = "消息"
    /** 是否显示导航栏 */
    var showHeader: Boolean = true
    /** 是否显示搜索栏 */
    var showSearchBar: Boolean = true
    /** 搜索栏占位文字 */
    var searchPlaceholder: String = "搜索"

    // ========== 列表行为配置 ==========

    /** 是否显示在线状态指示器 */
    var showOnlineIndicator: Boolean = true
    /** 是否显示未读计数 */
    var showUnreadCount: Boolean = true
    /** 是否显示最后消息预览 */
    var showLastMessage: Boolean = true
    /** 是否显示最后消息时间 */
    var showLastMessageTime: Boolean = true
    /** 自定义时间格式化器 */
    var timeFormatter: ChannelTimeFormatter? = null

    // ========== 加载更多配置 ==========

    /** 滚动到底部时触发加载更多频道的回调 */
    var onLoadMore: (() -> Unit)? = null
    /** 是否正在加载更多 */
    var isLoadingMore: Boolean = false
    /** 是否还有更多频道可加载 */
    var hasMore: Boolean = true

    // ========== 事件回调 ==========

    /** 频道点击回调 */
    var onChannelClick: ((ChatChannel) -> Unit)? = null
    /** 频道长按回调 */
    var onChannelLongPress: ((ChatChannel) -> Unit)? = null
    /** 频道删除回调（左滑删除） */
    var onChannelDelete: ((ChatChannel) -> Unit)? = null
    /** 频道置顶/取消置顶回调 */
    var onChannelPin: ((ChatChannel) -> Unit)? = null
    /** 频道静音/取消静音回调 */
    var onChannelMute: ((ChatChannel) -> Unit)? = null
    /** 搜索文本变化回调 */
    var onSearchTextChange: ((String) -> Unit)? = null
    /** 返回按钮点击回调 */
    var onBackClick: (() -> Unit)? = null
    /** 右上角操作按钮点击回调（如新建聊天） */
    var onTrailingClick: (() -> Unit)? = null
}
