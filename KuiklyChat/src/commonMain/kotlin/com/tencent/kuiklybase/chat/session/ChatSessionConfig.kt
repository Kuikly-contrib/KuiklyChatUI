package com.tencent.kuiklybase.chat.session

import com.tencent.kuiklybase.chat.bubble.MessageRendererFactory
import com.tencent.kuiklybase.chat.model.*

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
 * 同时保持向后兼容：旧的扁平属性由父类 [ChatSessionConfigCompat] 代理到对应 Options。
 */
class ChatSessionConfig : ChatSessionConfigCompat() {

    // ========== 组件工厂（参考 Stream Chat 的 ChatComponentFactory） ==========

    /**
     * 全局组件工厂。
     * 用于替换原子组件（头像、状态指示器、重发按钮等）的默认渲染方式。
     * 优先级链：Slot API > ComponentFactory > 内置默认渲染。
     *
     * 使用方式：
     * ```kotlin
     * config.componentFactory = object : DefaultChatComponentFactory() {
     *     override fun renderAvatar(...) { /* 自定义头像 */ }
     * }
     * ```
     */
    var componentFactory: ChatComponentFactory = DefaultChatComponentFactory()

    // ========== Handler / Formatter（参考 Stream Chat 的可替换处理器） ==========

    /**
     * 日期分隔符处理器。
     * 决定两条消息之间是否需要插入日期分隔符，以及分隔符的文本内容。
     * 替换后可自定义日期分隔逻辑和格式化。
     */
    var dateSeparatorHandler: DateSeparatorHandler = DefaultDateSeparatorHandler()

    /**
     * 消息分组处理器。
     * 决定消息在分组中的位置（TOP/MIDDLE/BOTTOM/SINGLE），
     * 用于控制头像显示、间距缩小等。
     */
    var messagePositionHandler: MessagePositionHandler = DefaultMessagePositionHandler()

    /**
     * 消息文本格式化器。
     * 用于自定义消息内容的文本渲染（如 Markdown 解析、@提及高亮等）。
     */
    var messageTextFormatter: MessageTextFormatter = DefaultMessageTextFormatter()

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

    // ========== 导航栏配置 ==========
    var title: String = "聊天"
    var showNavigationBar: Boolean = true
    var showBackButton: Boolean = true

    // ========== 头像配置 ==========
    var selfAvatarUrl: String = ""

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

    // ========== 便捷方法：获取消息位置 ==========

    /**
     * 使用 messagePositionHandler 计算消息在分组中的位置。
     * 便捷方法，内部使用。
     */
    fun getMessagePosition(prev: ChatMessage?, current: ChatMessage, next: ChatMessage?): MessagePosition {
        return messagePositionHandler.handleMessagePosition(
            prev, current, next, messageListOptions.messageGroupingInterval
        )
    }

    /**
     * 使用 dateSeparatorHandler 判断是否需要日期分隔符。
     * 便捷方法，内部使用。
     */
    fun shouldShowDateSeparator(prevTimestamp: Long, currentTimestamp: Long): Boolean {
        return dateSeparatorHandler.shouldAddDateSeparator(
            prevTimestamp, currentTimestamp, messageListOptions.timeGroupInterval
        )
    }

    /**
     * 使用 dateSeparatorHandler 格式化日期。
     * 便捷方法，内部使用。
     */
    fun formatDateSeparator(timestamp: Long): String {
        return messageListOptions.timeFormatter?.invoke(timestamp)
            ?: dateSeparatorHandler.formatDate(timestamp)
    }
}
