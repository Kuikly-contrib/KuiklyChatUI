package com.tencent.kuiklybase.chat

import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.reactive.collection.ObservableList

// ============================
// Slot 类型定义（参考 Stream Chat Compose 的 Slot API 设计）
// ============================

/**
 * 消息气泡渲染 Slot（带上下文）
 *
 * 用于自定义单条消息的渲染方式。提供 MessageContext 以支持上下文感知渲染。
 * - container: 父容器，在其中添加自定义组件
 * - context: 消息上下文（含前后消息、分组信息）
 * - config: 当前聊天会话配置（可获取主题色等）
 */
typealias MessageBubbleSlot = (container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig) -> Unit

/**
 * 旧版气泡 Slot（兼容，不含上下文）
 */
typealias SimpleBubbleSlot = (container: ViewContainer<*, *>, message: ChatMessage, config: ChatSessionConfig) -> Unit

/**
 * 导航栏渲染 Slot
 *
 * 用于完全替换默认导航栏。
 * - container: 父容器
 * - config: 当前聊天会话配置
 */
typealias NavigationBarSlot = (container: ViewContainer<*, *>, config: ChatSessionConfig) -> Unit

/**
 * 通用渲染 Slot（用于导航栏右侧按钮、空消息占位等轻量定制场景）
 * - container: 父容器
 */
typealias ViewSlot = (container: ViewContainer<*, *>) -> Unit

/**
 * 时间标签格式化器
 *
 * 接收消息时间戳（毫秒），返回格式化后的时间字符串。
 * 默认实现会根据时间距离返回 "HH:mm"、"昨天 HH:mm"、"MM-dd HH:mm" 等。
 */
typealias TimeFormatter = (timestamp: Long) -> String

/**
 * 日期分隔符渲染 Slot
 *
 * 用于自定义日期分隔符的渲染方式。
 * - container: 父容器
 * - timestamp: 分隔位置的时间戳
 * - formattedDate: 格式化后的日期文字
 */
typealias DateSeparatorSlot = (container: ViewContainer<*, *>, timestamp: Long, formattedDate: String) -> Unit

/**
 * 正在输入指示器渲染 Slot
 *
 * 用于自定义输入指示器的渲染方式。
 * - container: 父容器
 * - typingUsers: 正在输入的用户名列表
 */
typealias TypingIndicatorSlot = (container: ViewContainer<*, *>, typingUsers: List<String>) -> Unit

/**
 * 消息反应栏渲染 Slot
 *
 * 用于自定义消息反应栏的渲染方式。
 * - container: 父容器
 * - reactions: 反应列表
 * - onReactionClick: 反应点击回调
 */
typealias ReactionBarSlot = (container: ViewContainer<*, *>, reactions: List<ReactionItem>, onReactionClick: (String) -> Unit) -> Unit

/**
 * 消息操作菜单渲染 Slot
 *
 * 用于自定义消息操作菜单的渲染方式。
 * - container: 父容器
 * - message: 目标消息
 * - actions: 操作列表
 * - onActionClick: 操作点击回调
 * - onDismiss: 关闭菜单回调
 */
typealias MessageOptionsSlot = (container: ViewContainer<*, *>, message: ChatMessage, actions: List<MessageAction>, onActionClick: (MessageAction) -> Unit, onDismiss: () -> Unit) -> Unit

// ============================
// MessageComposer Slot 类型定义（参考 Stream Chat Compose MessageComposer 的 Slot API）
// ============================

/**
 * 输入框整体替换 Slot
 *
 * 完全替换默认的 MessageComposer，由使用方自行实现整个输入框区域。
 * 参考 Stream Chat 的 stateless MessageComposer。
 * - container: 父容器
 * - state: 当前输入框状态
 * - onSendMessage: 发送消息回调（传入文本内容）
 */
typealias MessageComposerSlot = (container: ViewContainer<*, *>, state: MessageComposerState, onSendMessage: (String) -> Unit) -> Unit

/**
 * 输入框左侧集成按钮区域 Slot（参考 Stream Chat MessageComposer 的 integrations Slot）
 *
 * 默认为空，可以放置附件选择、语音输入等按钮。
 * - container: 父容器（Row 内部，水平排列）
 * - state: 当前输入框状态
 */
typealias ComposerIntegrationsSlot = (container: ViewContainer<*, *>, state: MessageComposerState) -> Unit

/**
 * 输入框核心输入区域 Slot（参考 Stream Chat MessageComposer 的 input Slot）
 *
 * 替换默认的 Input 组件。可以自定义输入框样式、多行输入、富文本等。
 * - container: 父容器
 * - state: 当前输入框状态
 * - onValueChange: 文本变化回调
 */
typealias ComposerInputSlot = (container: ViewContainer<*, *>, state: MessageComposerState, onValueChange: (String) -> Unit) -> Unit

/**
 * 输入框右侧内容 Slot（参考 Stream Chat MessageComposer 的 trailingContent Slot）
 *
 * 默认为发送按钮。可替换为语音/发送切换等复合组件。
 * - container: 父容器（Row 内部，水平排列）
 * - state: 当前输入框状态
 * - onSendMessage: 发送回调
 */
typealias ComposerTrailingSlot = (container: ViewContainer<*, *>, state: MessageComposerState, onSendMessage: (String) -> Unit) -> Unit

/**
 * 输入框顶部内容 Slot（参考 Stream Chat MessageComposer 的 headerContent Slot）
 *
 * 在输入框上方显示额外内容，例如「正在回复 xxx」、「正在编辑」提示条。
 * - container: 父容器
 * - state: 当前输入框状态
 */
typealias ComposerHeaderSlot = (container: ViewContainer<*, *>, state: MessageComposerState) -> Unit

/**
 * 输入框底部内容 Slot（参考 Stream Chat MessageComposer 的 footerContent Slot）
 *
 * 在输入框下方（安全区域上方）显示额外内容，例如工具栏、表情面板入口等。
 * - container: 父容器
 * - state: 当前输入框状态
 */
typealias ComposerFooterSlot = (container: ViewContainer<*, *>, state: MessageComposerState) -> Unit

/**
 * 消息输入框状态（参考 Stream Chat 的 MessageComposerState）
 *
 * 封装输入框当前的所有状态，传递给各 Slot 使用。
 */
data class MessageComposerState(
    /** 当前输入框中的文本 */
    val inputValue: String = "",
    /** 是否正在编辑某条消息（非 null 时表示编辑模式） */
    val editingMessage: ChatMessage? = null,
    /** 正在回复的消息（非 null 时表示回复模式） */
    val replyingToMessage: ChatMessage? = null,
    /** 附件列表（预留，当前未使用） */
    val attachments: List<String> = emptyList()
)

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
            composerSendButtonTextColor = composerSendButtonTextColor
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

/**
 * Slot 配置（参考 Stream Chat 的 Slot API / Content 自定义层级）
 *
 * 集中管理所有可替换渲染插槽。
 * 对应 Stream Chat 的 itemContent / emptyContent / loadingContent / helperContent 等概念。
 */
class ChatSlotOptions {
    // ---- 消息类型独立 Slot（参考 Stream Chat 的 attachmentFactories） ----

    /**
     * 自定义文本消息气泡渲染（带上下文）。
     *
     * 参考 Stream Chat 的 itemContent Slot：提供 MessageContext 以支持上下文感知渲染
     * （连续消息合并头像、缩小间距等）。
     */
    var textBubble: MessageBubbleSlot? = null

    /**
     * 自定义图片消息渲染（带上下文）。
     *
     * 默认渲染：带圆角的图片 + 加载占位。
     * 设置后替换默认图片消息渲染。
     */
    var imageBubble: MessageBubbleSlot? = null

    /**
     * 自定义消息类型渲染（MessageType.CUSTOM）。
     *
     * 用于渲染语音消息、文件消息、卡片消息等自定义类型。
     * 必须设置此 Slot 才能渲染 CUSTOM 类型消息。
     */
    var customBubble: MessageBubbleSlot? = null

    /**
     * 统一消息气泡渲染（设置后覆盖 textBubble/imageBubble，所有非系统消息都使用此 Slot）。
     *
     * 参考 Stream Chat MessageList 的 itemContent：完全自定义消息项渲染。
     * 优先级高于 textBubble/imageBubble/customBubble。
     */
    var messageBubble: MessageBubbleSlot? = null

    /**
     * 自定义系统消息渲染。
     */
    var systemMessage: SimpleBubbleSlot? = null

    // ---- 列表状态 Slot（参考 Stream Chat MessageList 的对应 Slot） ----

    /**
     * 消息列表为空时的占位渲染。
     *
     * 参考 Stream Chat MessageList 的 emptyContent。
     */
    var emptyContent: ViewSlot? = null

    /**
     * 列表加载中（首次加载）时的渲染。
     *
     * 参考 Stream Chat MessageList 的 loadingContent。
     */
    var loadingContent: ViewSlot? = null

    /**
     * 加载更多历史消息时的顶部加载指示器。
     *
     * 参考 Stream Chat MessageList 的 loadingMoreContent。
     */
    var loadingMoreContent: ViewSlot? = null

    /**
     * 列表辅助内容（如"滚动到底部"悬浮按钮、"N条新消息"提示）。
     *
     * 参考 Stream Chat MessageList 的 helperContent。
     */
    var helperContent: ViewSlot? = null

    // ---- 导航栏 Slot ----

    /**
     * 自定义导航栏渲染。设置此 Slot 后将完全替换默认导航栏。
     */
    var navigationBar: NavigationBarSlot? = null

    /**
     * 导航栏右侧操作区域 Slot（如"更多"按钮、搜索按钮等）。
     */
    var navigationBarTrailing: ViewSlot? = null

    // ---- 新增 Slot（P0 功能） ----

    /**
     * 自定义日期分隔符渲染。
     */
    var dateSeparator: DateSeparatorSlot? = null

    /**
     * 自定义正在输入指示器渲染。
     */
    var typingIndicator: TypingIndicatorSlot? = null

    /**
     * 自定义消息反应栏渲染。
     */
    var reactionBar: ReactionBarSlot? = null

    /**
     * 自定义消息操作菜单渲染。
     */
    var messageOptions: MessageOptionsSlot? = null

    // ---- MessageComposer Slots（参考 Stream Chat MessageComposer 的 Slot API） ----

    /**
     * 完全替换默认 MessageComposer。
     * 设置后，所有其他 composer* Slot 失效，由使用方自行实现整个输入框。
     * 参考 Stream Chat 的 stateless MessageComposer 模式。
     */
    var messageComposer: MessageComposerSlot? = null

    /**
     * 输入框左侧集成按钮区域（附件选择、语音等）。
     * 参考 Stream Chat MessageComposer 的 integrations Slot。
     * 默认为空（不显示左侧按钮）。
     */
    var composerIntegrations: ComposerIntegrationsSlot? = null

    /**
     * 核心输入区域。替换默认的 Input 组件。
     * 参考 Stream Chat MessageComposer 的 input Slot。
     */
    var composerInput: ComposerInputSlot? = null

    /**
     * 输入框右侧内容（默认为发送按钮）。
     * 参考 Stream Chat MessageComposer 的 trailingContent Slot。
     */
    var composerTrailing: ComposerTrailingSlot? = null

    /**
     * 输入框顶部内容（如"正在回复..."、"正在编辑..."提示条）。
     * 参考 Stream Chat MessageComposer 的 headerContent Slot。
     */
    var composerHeader: ComposerHeaderSlot? = null

    /**
     * 输入框底部内容（如工具栏、表情面板入口）。
     * 参考 Stream Chat MessageComposer 的 footerContent Slot。
     */
    var composerFooter: ComposerFooterSlot? = null
}

// ============================
// 聊天会话配置（兼容旧 API + 新 Options 分组）
// ============================

/**
 * ChatSession 的配置参数
 *
 * 架构参考 Stream Chat Compose SDK：
 * - ChatTheme → [theme] 主题配置
 * - MessageList 行为参数 → [messageList] 列表行为配置
 * - Slot APIs → [slots] 渲染插槽配置
 *
 * 同时保持向后兼容，所有旧属性仍可直接在 Config 上访问（代理到对应 Options）。
 */
class ChatSessionConfig {
    // ========== Options 分组 ==========

    /** 主题配置（颜色、尺寸、形状） */
    val theme = ChatThemeOptions()
    /** 消息列表行为配置（滚动、加载、分组） */
    val messageListOptions = MessageListOptions()
    /** 渲染插槽配置 */
    val slots = ChatSlotOptions()

    // ========== 消息渲染工厂（对标 Stream Chat attachmentFactories） ==========

    /**
     * 消息渲染器工厂列表。
     * 遍历列表，第一个 canRender() 返回 true 的工厂执行渲染。
     * 在 Slot 未设置时才生效（Slot 优先级高于 Factory）。
     */
    val messageRenderers: MutableList<MessageRendererFactory> = mutableListOf()

    // ========== 消息操作菜单配置 ==========

    /**
     * 消息操作列表（长按消息弹出的菜单项）。
     * 默认提供常用操作，业务方可自定义。
     */
    var messageActions: List<MessageAction> = defaultMessageActions()

    // ========== 便捷 DSL 配置方法 ==========

    /** DSL 方式配置主题 */
    fun theme(block: ChatThemeOptions.() -> Unit) { theme.apply(block) }
    /** DSL 方式配置消息列表行为 */
    fun messageListOptions(block: MessageListOptions.() -> Unit) { messageListOptions.apply(block) }
    /** DSL 方式配置渲染插槽 */
    fun slots(block: ChatSlotOptions.() -> Unit) { slots.apply(block) }

    // ========== 导航栏配置 ==========
    var title: String = "聊天"
    var showNavigationBar: Boolean = true
    var showBackButton: Boolean = true

    // ========== 头像配置 ==========
    var selfAvatarUrl: String = ""

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

    // ========== 内部方法引用 ==========

    /**
     * 滚动到底部的方法引用。ChatSession 内部会在初始化时设置此函数。
     */
    var scrollToBottomAction: ((animate: Boolean) -> Unit)? = null

    /**
     * 滚动到指定消息。ChatSession 内部会在初始化时设置此函数。
     */
    var scrollToMessageAction: ((messageId: String, animate: Boolean) -> Unit)? = null

    // ========== 内部坐标修正（由 ChatSessionView 维护） ==========

    /**
     * 当前列表滚动偏移量（内部使用，由 ChatSessionView 的 scroll 事件更新）
     */
    internal var _currentScrollOffsetY: Float = 0f

    /**
     * 消息列表可视区域在页面根节点中的 top 位置（内部使用）
     */
    internal var _messageAreaTopInRoot: Float = 0f

    /**
     * 消息列表可视区域的高度（内部使用）
     */
    internal var _messageAreaHeight: Float = 0f

    /**
     * 修正坐标：将 convertFrame(toView=null) 得到的列表内容坐标转换为屏幕可见坐标。
     *
     * 对于 ScrollView（List）内的子 View，convertFrame 返回的 Y 值可能包含了整个
     * 列表内容的偏移（而非屏幕上的可见位置）。此方法通过列表的 scrollOffset 和
     * 列表可视区域位置进行修正。
     *
     * @param rawY convertFrame(frame, toView=null) 返回的原始 Y 值
     * @return 修正后的屏幕可见 Y 坐标
     */
    internal fun correctBubbleY(rawY: Float): Float {
        return rawY - _currentScrollOffsetY
    }

    // ========== MessageComposer 配置（参考 Stream Chat MessageComposer） ==========

    /**
     * 是否显示内置的 MessageComposer 输入框。
     * 设置为 true 时，ChatSession 底部会自动显示输入框。
     * 设置为 false 时，不显示输入框（使用方可自行在 ChatSession 外部放置输入框）。
     */
    var showMessageComposer: Boolean = false

    /**
     * 底部安全区域高度（用于 MessageComposer 底部留白）。
     * 使用方需要在 Pager/ComposeView 中传入 pagerData.safeAreaInsets.bottom 的值。
     * 例如：composerSafeAreaBottom = pagerData.safeAreaInsets.bottom
     */
    var composerSafeAreaBottom: Float = 0f

    /**
     * 输入框提示文字（参考 Stream Chat MessageComposer 的 label）。
     */
    var composerPlaceholder: String = "输入消息..."

    /**
     * 发送按钮文字。
     */
    var composerSendButtonText: String = "发送"

    /**
     * 发送消息回调。当用户点击发送按钮或按回车时触发。
     * 使用方在此回调中处理消息发送逻辑（添加到 messageList 等）。
     * 参考 Stream Chat MessageComposer 的 onSendMessage。
     */
    var onSendMessage: ((text: String) -> Unit)? = null

    /**
     * 输入文本变化回调。
     * 参考 Stream Chat MessageComposer 的 onValueChange。
     */
    var onInputValueChange: ((text: String) -> Unit)? = null

    /**
     * 附件按钮点击回调。
     * 参考 Stream Chat MessageComposer 的 onAttachmentsClick。
     */
    var onAttachmentsClick: (() -> Unit)? = null

    /**
     * 输入框内部状态引用（内部使用）。
     * ChatSession 内部维护，使用方可通过 composerState 获取当前输入状态。
     */
    internal var _composerInputText: String = ""

    /**
     * 清空输入框的方法引用（内部使用）。
     * ChatSession 内部设置。
     */
    internal var _clearComposerInput: (() -> Unit)? = null

    /**
     * 获取当前 MessageComposer 的状态
     */
    fun getComposerState(): MessageComposerState {
        return MessageComposerState(
            inputValue = _composerInputText,
            replyingToMessage = _replyingToMessage
        )
    }

    // ========== P0: 引用回复状态管理 ==========

    /**
     * 当前正在回复的消息（非 null 时 ComposerHeader 自动显示"正在回复 xxx"提示条）。
     * 设置此值后，发送的下一条消息会自动携带 quotedMessage。
     */
    var _replyingToMessage: ChatMessage? = null

    /**
     * 设置回复消息（业务方调用）
     */
    fun replyToMessage(message: ChatMessage?) {
        _replyingToMessage = message
    }

    /**
     * 取消回复
     */
    fun cancelReply() {
        _replyingToMessage = null
    }

    // ========== P0: 操作菜单内置状态管理 ==========

    /**
     * 是否启用内置操作菜单（默认 true）。
     * 启用后，长按消息自动弹出内置操作菜单，无需业务方维护菜单状态。
     * 设置为 false 可禁用内置菜单，由业务方自行管理。
     */
    var enableBuiltInMessageOptions: Boolean = true

    /**
     * 内部：操作菜单是否显示（由 ChatSession 自动维护）
     */
    internal var _showMessageOptions: Boolean = false

    /**
     * 内部：操作菜单目标消息
     */
    internal var _optionsTargetMessage: ChatMessage? = null

    /**
     * 内部：操作菜单目标位置
     */
    internal var _optionsTargetX: Float = 0f
    internal var _optionsTargetY: Float = 0f
    internal var _optionsTargetW: Float = 0f
    internal var _optionsTargetH: Float = 0f

    // ========== 事件回调 ==========

    var onBackClick: (() -> Unit)? = null
    var onMessageClick: ((ChatMessage) -> Unit)? = null
    var onMessageLongPress: ((ChatMessage) -> Unit)? = null
    /**
     * 带位置信息的长按回调。
     * 参数：消息、消息气泡在页面坐标系中的 X、Y、宽度、高度。
     * 设置后优先于 onMessageLongPress 调用。
     */
    var onMessageLongPressWithPosition: ((ChatMessage, Float, Float, Float, Float) -> Unit)? = null
    /** 失败消息重发回调 */
    var onResend: ((ChatMessage) -> Unit)? = null
    /** 点击用户头像回调（参考 Stream Chat 的 onUserAvatarClick） */
    var onAvatarClick: ((ChatMessage) -> Unit)? = null

    // ---- 新增事件回调 ----

    /** 消息反应点击回调（参数：消息、反应类型） */
    var onReactionClick: ((ChatMessage, String) -> Unit)? = null
    /** 消息编辑回调（参数：被编辑的消息） */
    var onEditMessage: ((ChatMessage) -> Unit)? = null
    /** 消息删除回调（参数：被删除的消息） */
    var onDeleteMessage: ((ChatMessage) -> Unit)? = null
    /** 线程回复点击回调（参数：主消息） */
    var onThreadClick: ((ChatMessage) -> Unit)? = null
    /** 消息操作菜单项点击回调（参数：消息、操作） */
    var onMessageAction: ((ChatMessage, MessageAction) -> Unit)? = null
    /** 消息置顶回调（参数：被置顶/取消置顶的消息） */
    var onPinMessage: ((ChatMessage) -> Unit)? = null

}

// ============================
// 默认消息操作列表
// ============================

/**
 * 生成默认的消息操作列表
 */
fun defaultMessageActions(): List<MessageAction> = listOf(
    MessageAction(
        key = "copy",
        label = "复制",
        isVisible = { it.type == MessageType.TEXT }
    ),
    MessageAction(
        key = "reply",
        label = "回复"
    ),
    MessageAction(
        key = "quote",
        label = "引用"
    ),
    MessageAction(
        key = "edit",
        label = "编辑",
        isVisible = { it.isSelf && it.type == MessageType.TEXT }
    ),
    MessageAction(
        key = "pin",
        label = "置顶",
        isVisible = { !it.isPinned }
    ),
    MessageAction(
        key = "unpin",
        label = "取消置顶",
        isVisible = { it.isPinned }
    ),
    MessageAction(
        key = "delete",
        label = "删除",
        isDestructive = true,
        isVisible = { it.isSelf }
    )
)
