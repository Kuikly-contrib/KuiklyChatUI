package com.tencent.kuiklybase.chat.session

import com.tencent.kuiklybase.chat.model.*

/**
 * [ChatSessionConfig] 的旧版扁平 API 兼容层。
 *
 * 早期配置项都直接挂在 Config 上（`config.primaryColor = ...`），后来按 Stream Chat 的
 * 分层设计收敛到 [theme] / [messageListOptions] / [slots] 三组 Options。这里保留旧属性并
 * 代理到对应 Options，让老代码无需改动。
 *
 * 之所以用父类而不是扩展属性：扩展属性会改变调用方的 import 需求并破坏 Java 互操作，
 * 而继承能原样保留成员语义。新代码请直接使用 Options 分组。
 */
abstract class ChatSessionConfigCompat {

    // ========== Options 分组 ==========

    /** 主题配置（颜色、尺寸、形状） */
    val theme = ChatThemeOptions()
    /** 消息列表行为配置（滚动、加载、分组） */
    val messageListOptions = MessageListOptions()
    /** 渲染插槽配置 */
    val slots = ChatSlotOptions()

    // ========== 便捷 DSL 配置方法 ==========

    /** DSL 方式配置主题 */
    fun theme(block: ChatThemeOptions.() -> Unit) { theme.apply(block) }
    /** DSL 方式配置消息列表行为 */
    fun messageListOptions(block: MessageListOptions.() -> Unit) { messageListOptions.apply(block) }
    /** DSL 方式配置渲染插槽 */
    fun slots(block: ChatSlotOptions.() -> Unit) { slots.apply(block) }

    // ========== 向后兼容属性（代理到 Options 分组） ==========

    /** @see ChatThemeOptions.primaryColor */
    var primaryColor: Long
        get() = theme.primaryColor
        set(value) { theme.primaryColor = value }
    /** @see ChatThemeOptions.primaryGradientEndColor */
    var primaryGradientEndColor: Long
        get() = theme.primaryGradientEndColor
        set(value) { theme.primaryGradientEndColor = value }
    /** @see ChatThemeOptions.backgroundColor */
    var backgroundColor: Long
        get() = theme.backgroundColor
        set(value) { theme.backgroundColor = value }
    /** @see ChatThemeOptions.otherBubbleColor */
    var otherBubbleColor: Long
        get() = theme.otherBubbleColor
        set(value) { theme.otherBubbleColor = value }
    /** @see ChatThemeOptions.otherTextColor */
    var otherTextColor: Long
        get() = theme.otherTextColor
        set(value) { theme.otherTextColor = value }
    /** @see ChatThemeOptions.selfTextColor */
    var selfTextColor: Long
        get() = theme.selfTextColor
        set(value) { theme.selfTextColor = value }
    /** @see ChatThemeOptions.backgroundImage */
    var backgroundImage: String
        get() = theme.backgroundImage
        set(value) { theme.backgroundImage = value }
    /** @see ChatThemeOptions.bubbleMaxWidthRatio */
    var bubbleMaxWidthRatio: Float
        get() = theme.bubbleMaxWidthRatio
        set(value) { theme.bubbleMaxWidthRatio = value }
    /** @see ChatThemeOptions.bubblePaddingH */
    var bubblePaddingH: Float
        get() = theme.bubblePaddingH
        set(value) { theme.bubblePaddingH = value }
    /** @see ChatThemeOptions.bubblePaddingV */
    var bubblePaddingV: Float
        get() = theme.bubblePaddingV
        set(value) { theme.bubblePaddingV = value }
    /** @see ChatThemeOptions.messageFontSize */
    var messageFontSize: Float
        get() = theme.messageFontSize
        set(value) { theme.messageFontSize = value }
    /** @see ChatThemeOptions.messageLineHeight */
    var messageLineHeight: Float
        get() = theme.messageLineHeight
        set(value) { theme.messageLineHeight = value }
    /** @see ChatThemeOptions.avatarSize */
    var avatarSize: Float
        get() = theme.avatarSize
        set(value) { theme.avatarSize = value }
    /** @see ChatThemeOptions.rowPaddingV */
    var rowPaddingV: Float
        get() = theme.rowPaddingV
        set(value) { theme.rowPaddingV = value }
    /** @see ChatThemeOptions.rowPaddingH */
    var rowPaddingH: Float
        get() = theme.rowPaddingH
        set(value) { theme.rowPaddingH = value }
    /** @see ChatThemeOptions.avatarBubbleGap */
    var avatarBubbleGap: Float
        get() = theme.avatarBubbleGap
        set(value) { theme.avatarBubbleGap = value }
    /** @see ChatThemeOptions.avatarRadius */
    var avatarRadius: Float
        get() = theme.avatarRadius
        set(value) { theme.avatarRadius = value }
    /** @see MessageListOptions.autoScrollToBottom */
    var autoScrollToBottom: Boolean
        get() = messageListOptions.autoScrollToBottom
        set(value) { messageListOptions.autoScrollToBottom = value }
    /** @see MessageListOptions.showAvatar */
    var showAvatar: Boolean
        get() = messageListOptions.showAvatar
        set(value) { messageListOptions.showAvatar = value }
    /** @see MessageListOptions.showSenderName */
    var showSenderName: Boolean
        get() = messageListOptions.showSenderName
        set(value) { messageListOptions.showSenderName = value }
    /** @see MessageListOptions.showTimeGroup */
    var showTimeGroup: Boolean
        get() = messageListOptions.showTimeGroup
        set(value) { messageListOptions.showTimeGroup = value }
    /** @see MessageListOptions.timeGroupInterval */
    var timeGroupInterval: Long
        get() = messageListOptions.timeGroupInterval
        set(value) { messageListOptions.timeGroupInterval = value }
    /** @see MessageListOptions.timeFormatter */
    var timeFormatter: TimeFormatter?
        get() = messageListOptions.timeFormatter
        set(value) { messageListOptions.timeFormatter = value }
    /** @see MessageListOptions.onLoadEarlier */
    var onLoadEarlier: (() -> Unit)?
        get() = messageListOptions.onLoadEarlier
        set(value) { messageListOptions.onLoadEarlier = value }
    /** @see MessageListOptions.isLoadingEarlier */
    var isLoadingEarlier: Boolean
        get() = messageListOptions.isLoadingEarlier
        set(value) { messageListOptions.isLoadingEarlier = value }
    /** @see MessageListOptions.hasMoreEarlier */
    var hasMoreEarlier: Boolean
        get() = messageListOptions.hasMoreEarlier
        set(value) { messageListOptions.hasMoreEarlier = value }

    // ========== 旧 Slot 兼容属性（代理到 slots 分组） ==========

    /** @see ChatSlotOptions.messageBubble */
    var messageBubble: MessageBubbleSlot?
        get() = slots.messageBubble
        set(value) { slots.messageBubble = value }
    /** @see ChatSlotOptions.systemMessage */
    var systemMessage: SimpleBubbleSlot?
        get() = slots.systemMessage
        set(value) { slots.systemMessage = value }
    /** @see ChatSlotOptions.navigationBar */
    var navigationBar: NavigationBarSlot?
        get() = slots.navigationBar
        set(value) { slots.navigationBar = value }
    /** @see ChatSlotOptions.navigationBarTrailing */
    var navigationBarTrailing: ViewSlot?
        get() = slots.navigationBarTrailing
        set(value) { slots.navigationBarTrailing = value }
    /** @see ChatSlotOptions.emptyContent */
    var emptyView: ViewSlot?
        get() = slots.emptyContent
        set(value) { slots.emptyContent = value }
}
