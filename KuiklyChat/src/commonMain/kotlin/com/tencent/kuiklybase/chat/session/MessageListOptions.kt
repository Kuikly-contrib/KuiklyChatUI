package com.tencent.kuiklybase.chat.session

import com.tencent.kuiklybase.chat.model.*


/**
 * 消息列表行为配置（参考 Stream Chat MessageList 的行为参数）
 *
 * 集中管理列表行为：滚动、加载、分页等。
 * 对应 Stream Chat MessageList 的 onMessagesPageStartReached / loadingMoreContent 等概念。
 */
class MessageListOptions {
    /** 新消息时是否自动滚动到底部（自己的消息必须滚动，他人消息仅在底部时滚动） */
    var autoScrollToBottom: Boolean = true
    /** 是否显示头像（设为 false 适用于简洁的 1v1 聊天） */
    var showAvatar: Boolean = true
    /** 是否显示发送者昵称 */
    var showSenderName: Boolean = true

    // ---- 时间分组配置 ----
    /** 是否启用时间分组显示（两条消息间隔超过阈值自动插入时间标签） */
    var showTimeGroup: Boolean = true
    /** 时间分组间隔阈值（毫秒），默认 5 分钟 */
    var timeGroupInterval: Long = DEFAULT_TIME_GROUP_INTERVAL
    /** 自定义时间格式化器（默认使用内置的 "HH:mm" / "昨天 HH:mm" 格式） */
    var timeFormatter: TimeFormatter? = null

    // ---- 加载历史（参考 Stream Chat 的 onMessagesPageStartReached） ----
    /**
     * 滚动到顶部时触发加载更多历史消息的回调。
     * 参考 Stream Chat MessageList 的 onMessagesPageStartReached。
     * 设置后列表顶部会在加载时显示加载指示器。
     *
     * 业务方在回调中负责：
     * 1. 设置 isLoadingEarlier = true
     * 2. 异步加载历史消息并插入到 messageList 头部（使用 addAll(0, historyMessages)）
     * 3. 加载完成后设置 isLoadingEarlier = false
     * 4. 如果没有更多历史消息，设置 hasMoreEarlier = false
     *
     * 组件会自动处理插入后的位置补偿（防止列表跳动）。
     */
    var onLoadEarlier: (() -> Unit)? = null
    /** 是否正在加载历史消息（由业务方维护，加载中时不会重复触发 onLoadEarlier） */
    var isLoadingEarlier: Boolean = false
    /** 是否还有更多历史消息可加载（设为 false 后不再触发 onLoadEarlier） */
    var hasMoreEarlier: Boolean = true
    /** 触发加载历史消息的滚动距离阈值（距顶部多少像素时触发，默认 100f） */
    var loadEarlierThreshold: Float = 100f

    // ---- 消息分组策略 ----
    /** 是否启用连续消息分组（同一发送者的连续消息合并头像、缩小间距） */
    var enableMessageGrouping: Boolean = true
    /** 连续消息分组的最大时间间隔（毫秒），超过则断开分组。默认 2 分钟 */
    var messageGroupingInterval: Long = 2 * 60 * 1000L

    // ---- 正在输入指示器 ----
    /** 正在输入的用户名列表（非空时在消息列表底部显示"正在输入..."指示器） */
    var typingUsers: List<String> = emptyList()
}
