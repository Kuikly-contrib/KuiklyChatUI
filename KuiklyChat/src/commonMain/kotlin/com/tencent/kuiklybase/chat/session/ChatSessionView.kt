package com.tencent.kuiklybase.chat.session

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.directives.scrollToPosition
import com.tencent.kuikly.core.directives.vforLazy
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.views.*
import com.tencent.kuiklybase.chat.model.*
import com.tencent.kuiklybase.chat.bubble.*
import com.tencent.kuiklybase.chat.composer.*
import com.tencent.kuiklybase.chat.indicator.*
import com.tencent.kuiklybase.chat.navigation.*


/**
 * ChatSession - 聊天对话列表组件
 */
fun ViewContainer<*, *>.ChatSession(
    messageList: () -> ObservableList<ChatMessage>,
    config: ChatSessionConfig.() -> Unit
) {
    val cfg = ChatSessionConfig().apply(config)
    val listOptions = cfg.messageListOptions
    val slots = cfg.slots
    val theme = cfg.theme

    // 持有主题颜色引用
    val themeColors = cfg.theme.resolvedColors()

    // 持有 List 引用
    var listViewRef: ViewRef<ListView<*, *>>? = null
    // 消息列表区域的 ref，用于获取列表可视区域在页面中的位置
    var messageAreaRef: ViewRef<DivView>? = null
    // 普通变量（非响应式），通过 scroll/scrollEnd 事件维护，不会触发视图重建
    var atBottom = true
    // 当前列表滚动偏移量（通过 scroll 事件实时更新）
    var currentScrollOffsetY = 0f
    // 记录上次已知的消息数量，用于检测新消息
    var lastKnownSize = messageList().size

    // ========== 加载历史消息相关状态 ==========
    // 记录插入历史消息前的消息数量，用于位置补偿
    var sizeBeforeLoadEarlier = 0
    // 是否需要进行位置补偿（头部插入历史消息后防跳动）
    var needPositionCompensation = false

    // ========== 滚动控制 ==========

    val scrollToBottom: (Boolean) -> Unit = { animate ->
        getPager().addTaskWhenPagerUpdateLayoutFinish {
            val list = messageList()
            if (list.isNotEmpty()) {
                listViewRef?.view?.scrollToPosition(
                    index = list.size - 1,
                    offset = 0f,
                    animate = animate
                )
            }
        }
    }

    /**
     * 滚动策略
     */
    val checkAutoScroll: () -> Unit = {
        val list = messageList()
        val currentSize = list.size
        if (currentSize > lastKnownSize && !needPositionCompensation) {
            val lastMessage = list.lastOrNull()
            if (lastMessage != null && lastMessage.isSelf) {
                scrollToBottom(false)
            } else if (atBottom) {
                scrollToBottom(false)
            }
        }
        lastKnownSize = currentSize
    }

    /**
     * 检测是否需要触发加载历史消息（滚动接近顶部时触发）。
     * 参考 Stream Chat 的 onMessagesPageStartReached。
     */
    val checkLoadEarlier: (offsetY: Float) -> Unit = { offsetY ->
        if (listOptions.onLoadEarlier != null
            && !listOptions.isLoadingEarlier
            && listOptions.hasMoreEarlier
            && offsetY <= listOptions.loadEarlierThreshold
        ) {
            // 记录当前消息数量，用于后续位置补偿
            sizeBeforeLoadEarlier = messageList().size
            needPositionCompensation = true
            listOptions.onLoadEarlier?.invoke()
        }
    }

    /**
     * 位置补偿：历史消息插入到列表头部后，将滚动位置补偿到之前可见的消息。
     * 正序方案的核心难点：在顶部插入 N 条消息后，scrollToPosition(N) 让之前的第 0 条
     * 消息保持在原来的屏幕位置，避免列表跳动。
     */
    val checkPositionCompensation: () -> Unit = {
        if (needPositionCompensation) {
            val list = messageList()
            val currentSize = list.size
            val insertedCount = currentSize - sizeBeforeLoadEarlier
            if (insertedCount > 0) {
                // 布局完成后补偿位置：跳到插入数量的位置，让之前的第一条可见消息不动
                getPager().addTaskWhenPagerUpdateLayoutFinish {
                    listViewRef?.view?.scrollToPosition(
                        index = insertedCount,
                        offset = 0f,
                        animate = false
                    )
                }
            }
            needPositionCompensation = false
            lastKnownSize = currentSize
        }
    }

    cfg.scrollToBottomAction = scrollToBottom

    // P0: 内置操作菜单——通过 overlayRef 管理响应式状态
    var overlayRef: ViewRef<MessageOptionsOverlayView>? = null

    if (cfg.enableBuiltInMessageOptions) {
        val userLongPressWithPosition = cfg.onMessageLongPressWithPosition
        val userLongPress = cfg.onMessageLongPress
        cfg.onMessageLongPressWithPosition = { message, x, y, w, h ->
            if (message.type != MessageType.SYSTEM) {
                overlayRef?.view?.let { overlay ->
                    overlay.overlayAttr.targetMessage = message
                    overlay.overlayAttr.targetX = x
                    overlay.overlayAttr.targetY = y
                    overlay.overlayAttr.targetW = w
                    overlay.overlayAttr.targetH = h
                    overlay.overlayAttr.isVisible = true
                }
            }
            // 也调用用户设置的原始回调
            userLongPressWithPosition?.invoke(message, x, y, w, h)
        }
        // 同时包装无位置的长按（作为兜底）
        if (cfg.onMessageLongPress == null) {
            cfg.onMessageLongPress = { message ->
                if (message.type != MessageType.SYSTEM) {
                    overlayRef?.view?.let { overlay ->
                        overlay.overlayAttr.targetMessage = message
                        overlay.overlayAttr.targetX = 0f
                        overlay.overlayAttr.targetY = 100f
                        overlay.overlayAttr.targetW = 200f
                        overlay.overlayAttr.targetH = 50f
                        overlay.overlayAttr.isVisible = true
                    }
                }
                userLongPress?.invoke(message)
            }
        }
    }

    // scrollToMessage：滚动到指定消息 ID 的位置
    cfg.scrollToMessageAction = { messageId, animate ->
        val list = messageList()
        val index = list.indexOfFirst { it.id == messageId }
        if (index >= 0) {
            getPager().addTaskWhenPagerUpdateLayoutFinish {
                listViewRef?.view?.scrollToPosition(
                    index = index,
                    offset = 0f,
                    animate = animate
                )
            }
        }
    }

    // 首次进入页面时滚动到底部
    if (listOptions.autoScrollToBottom && messageList().isNotEmpty()) {
        scrollToBottom(false)
    }

    // ========== 根容器 ==========
    View {
        attr {
            flex(1f)
            backgroundColor(Color(theme.backgroundColor))
            flexDirection(FlexDirection.COLUMN)
        }

        // ========== 顶部导航栏 ==========
        if (cfg.showNavigationBar) {
            if (slots.navigationBar != null) {
                slots.navigationBar!!.invoke(this@View, cfg)
            } else {
                ChatNavigationBar {
                    attr {
                        title = cfg.title
                        showBackButton = cfg.showBackButton
                        primaryColor = theme.primaryColor
                        primaryGradientEndColor = theme.primaryGradientEndColor
                    }
                    event {
                        onBackClick = cfg.onBackClick
                        onTrailingClick = null
                    }
                }
            }
        }

        // ========== 消息列表区域（含背景图） ==========
        View messageArea@{
            ref { messageAreaRef = it }
            attr {
                flex(1f)
            }

            // 背景图层
            if (theme.backgroundImage.isNotEmpty()) {
                Image {
                    attr {
                        positionAbsolute()
                        top(0f)
                        left(0f)
                        right(0f)
                        bottom(0f)
                        src(theme.backgroundImage)
                        resizeCover()
                    }
                }
            }

            List {
                ref { listViewRef = it }

                attr {
                    flex(1f)
                }

                event {
                    scroll { params ->
                        currentScrollOffsetY = params.offsetY
                        cfg._currentScrollOffsetY = params.offsetY
                        // 在滚动过程中也实时更新 atBottom 状态，避免在用户手指还在滑动时
                        // 收到新消息导致 atBottom 判断不准确
                        atBottom = params.offsetY + params.viewHeight + 10 >= params.contentHeight
                        // 检测是否滚动接近顶部，触发加载历史消息
                        checkLoadEarlier(params.offsetY)
                    }
                    scrollEnd { params ->
                        currentScrollOffsetY = params.offsetY
                        cfg._currentScrollOffsetY = params.offsetY
                        atBottom = params.offsetY + params.viewHeight + 10 >= params.contentHeight
                        // scrollEnd 时也检查一下（用户轻扫到顶部惯性停下时）
                        checkLoadEarlier(params.offsetY)
                    }
                }

                vforLazy(messageList) { message, index, count ->
                    // 在首项渲染时检查是否需要位置补偿（历史消息插入后防跳动）
                    if (index == 0) {
                        checkPositionCompensation()
                    }
                    // 在最后一项渲染时检测是否需要自动滚动到底部
                    if (listOptions.autoScrollToBottom && index == count - 1) {
                        checkAutoScroll()
                    }

                    // 构建消息上下文（参考 Stream Chat 的 MessageItemState）
                    val msgContext = ChatMessageHelper.buildMessageContext(
                        messages = messageList(),
                        index = index,
                        groupingInterval = listOptions.messageGroupingInterval
                    )

                    // vforLazy 闭包内只能有一个根子节点，用外层 View 包裹日期分隔符和消息
                    View {
                    // ========== 加载历史消息指示器（仅在列表第一项上方显示） ==========
                    if (index == 0 && listOptions.onLoadEarlier != null) {
                        if (listOptions.isLoadingEarlier) {
                            // 加载中：显示加载指示器
                            if (slots.loadingMoreContent != null) {
                                slots.loadingMoreContent!!.invoke(this@View)
                            } else {
                                View {
                                    attr {
                                        allCenter()
                                        padding(12f, 0f, 8f, 0f)
                                    }
                                    Text {
                                        attr {
                                            text("加载中...")
                                            fontSize(12f)
                                            color(Color(themeColors.loadingTextColor))
                                        }
                                    }
                                }
                            }
                        } else if (listOptions.hasMoreEarlier) {
                            // 有更多历史消息：显示提示
                            View {
                                attr {
                                    allCenter()
                                    padding(12f, 0f, 8f, 0f)
                                }
                                Text {
                                    attr {
                                        text("上拉加载更多")
                                        fontSize(12f)
                                        color(Color(themeColors.hintTextColor))
                                    }
                                }
                            }
                        } else {
                            // 已加载全部历史：显示提示
                            View {
                                attr {
                                    allCenter()
                                    padding(12f, 0f, 8f, 0f)
                                }
                                Text {
                                    attr {
                                        text("—— 已经到顶了 ——")
                                        fontSize(12f)
                                        color(Color(themeColors.hintTextColor))
                                    }
                                }
                            }
                        }
                    }

                    // ========== 日期分隔符 ==========
                    if (listOptions.showTimeGroup && message.timestamp > 0L) {
                        val prevMsg = if (index > 0) messageList()[index - 1] else null
                        val prevTimestamp = prevMsg?.timestamp ?: 0L
                        val needSeparator = index == 0 || shouldShowDateSeparator(
                            prevTimestamp, message.timestamp, listOptions.timeGroupInterval
                        )
                        if (needSeparator) {
                            val formattedDate = listOptions.timeFormatter?.invoke(message.timestamp)
                                ?: defaultTimeFormat(message.timestamp)
                            if (formattedDate.isNotEmpty()) {
                                if (slots.dateSeparator != null) {
                                    slots.dateSeparator!!.invoke(this@View, message.timestamp, formattedDate)
                                } else {
                                    ChatDateSeparator {
                                        attr {
                                            dateText = formattedDate
                                            bgColor = themeColors.dateSeparatorBgColor
                                            textColor = themeColors.dateSeparatorTextColor
                                        }
                                    }
                                }
                            }
                        }
                    }

                    View itemRoot@{
                        // 根据分组信息调整间距（连续同一发送者消息缩小间距）
                        if (listOptions.enableMessageGrouping && !msgContext.isFirstInGroup && message.type != MessageType.SYSTEM) {
                            attr {
                                // 分组内消息间距缩小（2f vs 默认 rowPaddingV）
                            }
                        }

                        when (message.type) {
                            MessageType.SYSTEM -> {
                                // ---- 系统消息 ----
                                if (slots.systemMessage != null) {
                                    slots.systemMessage!!.invoke(this@itemRoot, message, cfg)
                                } else {
                                    // 尝试工厂渲染
                                    val factory = cfg.messageRenderers.firstOrNull { it.canRender(message) }
                                    if (factory != null) {
                                        factory.render(this@itemRoot, msgContext, cfg)
                                    } else {
                                    ChatSystemMessage {
                                        attr {
                                            this.message = message.content
                                            bgColor = themeColors.systemMessageBgColor
                                            textColor = themeColors.systemMessageTextColor
                                        }
                                    }
                                    }
                                }
                            }
                            else -> {
                                // ---- 非系统消息：Slot > 类型独立 Slot > Factory > 默认渲染 ----
                                when {
                                    // 1. 统一 Slot 优先级最高
                                    slots.messageBubble != null -> {
                                        slots.messageBubble!!.invoke(this@itemRoot, msgContext, cfg)
                                    }
                                    // 2. 类型独立 Slot
                                    message.type == MessageType.TEXT && slots.textBubble != null -> {
                                        slots.textBubble!!.invoke(this@itemRoot, msgContext, cfg)
                                    }
                                    message.type == MessageType.IMAGE && slots.imageBubble != null -> {
                                        slots.imageBubble!!.invoke(this@itemRoot, msgContext, cfg)
                                    }
                                    message.type == MessageType.CUSTOM && slots.customBubble != null -> {
                                        slots.customBubble!!.invoke(this@itemRoot, msgContext, cfg)
                                    }
                                    else -> {
                                        // 3. 工厂链式匹配
                                        val factory = cfg.messageRenderers.firstOrNull { it.canRender(message) }
                                        if (factory != null) {
                                            factory.render(this@itemRoot, msgContext, cfg)
                                        } else {
                                            // 4. 默认渲染（兜底）
                                            when (message.type) {
                                                MessageType.TEXT -> renderDefaultBubble(this@itemRoot, msgContext, cfg)
                                                MessageType.IMAGE -> renderDefaultImageBubble(this@itemRoot, msgContext, cfg)
                                                MessageType.VIDEO -> renderDefaultImageBubble(this@itemRoot, msgContext, cfg)
                                                MessageType.FILE -> renderDefaultFileBubble(this@itemRoot, msgContext, cfg)
                                                MessageType.CUSTOM -> {
                                                    ChatSystemMessage {
                                                        attr {
                                                            this.message = "[不支持的消息类型]"
                                                        }
                                                    }
                                                }
                                                else -> {
                                                    ChatSystemMessage {
                                                        attr {
                                                            this.message = "[不支持的消息类型]"
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }
                }
            }

            // ========== helperContent（参考 Stream Chat MessageList 的 helperContent） ==========
            // 悬浮在列表上层的辅助内容（如"滚动到底部"按钮）
            if (slots.helperContent != null) {
                slots.helperContent!!.invoke(this@messageArea)
            }

            // ========== 正在输入指示器 ==========
            if (listOptions.typingUsers.isNotEmpty()) {
                val typingText = formatTypingText(listOptions.typingUsers)
                if (slots.typingIndicator != null) {
                    slots.typingIndicator!!.invoke(this@messageArea, listOptions.typingUsers)
                } else {
                    ChatTypingIndicator {
                        attr {
                            this.typingText = typingText
                            dotColor = themeColors.typingIndicatorDotColor
                            textColor = themeColors.typingIndicatorTextColor
                        }
                    }
                }
            }
        }

        // ========== MessageComposer 输入框（参考 Stream Chat Compose 的 MessageComposer） ==========
        if (cfg.showMessageComposer) {
            // P0: 默认 composerHeader — 引用回复提示条
            if (slots.composerHeader == null && cfg._replyingToMessage != null) {
                slots.composerHeader = { container, state ->
                    val replyMsg = cfg._replyingToMessage
                    if (replyMsg != null) {
                        container.apply {
                            View {
                                attr {
                                    flexDirectionRow()
                                    alignItems(FlexAlign.CENTER)
                                    padding(8f, 12f, 8f, 12f)
                                    backgroundColor(Color(themeColors.quoteReplyBgColor))
                                    borderBottom(Border(0.5f, BorderStyle.SOLID, Color(themeColors.dividerColor)))
                                }
                                // 引用竖线
                                View {
                                    attr {
                                        width(3f)
                                        height(32f)
                                        borderRadius(1.5f)
                                        backgroundColor(Color(themeColors.quoteReplyBarColor))
                                        marginRight(8f)
                                    }
                                }
                                // 回复信息
                                View {
                                    attr {
                                        flex(1f)
                                        flexDirection(FlexDirection.COLUMN)
                                    }
                                    Text {
                                        attr {
                                            text("回复 ${replyMsg.senderName}")
                                            fontSize(12f)
                                            fontWeightMedium()
                                            color(Color(themeColors.quoteReplyBarColor))
                                        }
                                    }
                                    Text {
                                        attr {
                                            val preview = if (replyMsg.content.length > 40) {
                                                replyMsg.content.take(40) + "..."
                                            } else {
                                                replyMsg.content
                                            }
                                            text(preview)
                                            fontSize(12f)
                                            color(Color(themeColors.quoteReplyTextColor))
                                            marginTop(2f)
                                            lines(1)
                                        }
                                    }
                                }
                                // 关闭按钮
                                View {
                                    attr {
                                        size(28f, 28f)
                                        allCenter()
                                        marginLeft(8f)
                                    }
                                    Text {
                                        attr {
                                            text("✕")
                                            fontSize(16f)
                                            color(Color(themeColors.readReceiptColor))
                                        }
                                    }
                                    event {
                                        click {
                                            cfg.cancelReply()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            ChatMessageComposer(
                cfg = cfg,
                safeAreaBottom = cfg.composerSafeAreaBottom
            )
        }

        // ========== P0: 内置操作菜单（通过 ComposeView 的 observable 实现响应式状态） ==========
        if (cfg.enableBuiltInMessageOptions) {
            MessageOptionsOverlay {
                ref { overlayRef = it }
                attr {
                    menuActions = cfg.messageActions
                    // 传递主题色
                    menuBgColor = themeColors.menuBackgroundColor
                    textColor = themeColors.menuTextColor
                    destructiveColor = themeColors.menuDestructiveColor
                    dividerColor = themeColors.dividerColor
                    // 气泡样式信息
                    bubblePrimaryColor = theme.primaryColor
                    bubblePrimaryGradientEndColor = theme.primaryGradientEndColor
                    bubbleOtherBubbleColor = theme.otherBubbleColor
                    bubbleOtherTextColor = theme.otherTextColor
                    bubbleSelfTextColor = theme.selfTextColor
                    bubblePaddingH = theme.bubblePaddingH
                    bubblePaddingV = theme.bubblePaddingV
                    bubbleFontSize = theme.messageFontSize
                    bubbleLineHeight = theme.messageLineHeight
                    // 回调
                    onActionClickCallback = { action ->
                        overlayRef?.view?.overlayAttr?.targetMessage?.let { msg ->
                            // 处理内置操作
                            when (action.key) {
                                "reply", "quote" -> {
                                    cfg.replyToMessage(msg)
                                }
                            }
                            cfg.onMessageAction?.invoke(msg, action)
                        }
                    }
                    onReactionSelectCallback = { emoji ->
                        overlayRef?.view?.overlayAttr?.targetMessage?.let { msg ->
                            cfg.onReactionClick?.invoke(msg, emoji)
                        }
                    }
                    onDismissCallback = {
                        cfg._showMessageOptions = false
                        cfg._optionsTargetMessage = null
                    }
                }
            }
        }
    }
}

// ============================
// 内部：操作菜单状态管理组件
// ============================

/**
 * 内部 ComposeView，用于管理消息操作菜单的响应式状态。
 * ChatSessionConfig 是普通类，不支持 by observable，
 * 而 vif 需要 observable 属性才能响应变化。
 * 此组件将菜单的显示/隐藏状态放在 ComposeAttr 中，
 * 通过 observable 实现响应式更新。
 */
internal class MessageOptionsOverlayView : ComposeView<MessageOptionsOverlayAttr, ComposeEvent>() {
    override fun createAttr(): MessageOptionsOverlayAttr = MessageOptionsOverlayAttr()
    override fun createEvent(): ComposeEvent = ComposeEvent()

    /** 公开 attr 访问器，因为 ComposeView.attr 是 protected 的 */
    val overlayAttr: MessageOptionsOverlayAttr get() = attr

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            vif({ ctx.attr.isVisible }) {
                View {
                    attr {
                        positionAbsolute()
                        top(0f)
                        left(0f)
                        right(0f)
                        bottom(0f)
                    }
                    ChatMessageOptions {
                        attr {
                            message = ctx.attr.targetMessage
                            actions = ctx.attr.menuActions
                            targetX = ctx.attr.targetX
                            targetY = ctx.attr.targetY
                            targetWidth = ctx.attr.targetW
                            targetHeight = ctx.attr.targetH
                            screenWidth = ctx.pagerData.pageViewWidth
                            screenHeight = ctx.pagerData.pageViewHeight
                            // 传递主题色
                            menuBgColor = ctx.attr.menuBgColor
                            textColor = ctx.attr.textColor
                            destructiveColor = ctx.attr.destructiveColor
                            dividerColor = ctx.attr.dividerColor
                            // 气泡样式
                            bubblePrimaryColor = ctx.attr.bubblePrimaryColor
                            bubblePrimaryGradientEndColor = ctx.attr.bubblePrimaryGradientEndColor
                            bubbleOtherBubbleColor = ctx.attr.bubbleOtherBubbleColor
                            bubbleOtherTextColor = ctx.attr.bubbleOtherTextColor
                            bubbleSelfTextColor = ctx.attr.bubbleSelfTextColor
                            bubblePaddingH = ctx.attr.bubblePaddingH
                            bubblePaddingV = ctx.attr.bubblePaddingV
                            bubbleFontSize = ctx.attr.bubbleFontSize
                            bubbleLineHeight = ctx.attr.bubbleLineHeight
                        }
                        event {
                            onActionClick = { action ->
                                ctx.attr.onActionClickCallback?.invoke(action)
                            }
                            onReactionSelect = { emoji ->
                                ctx.attr.onReactionSelectCallback?.invoke(emoji)
                            }
                            onDismiss = {
                                ctx.attr.isVisible = false
                                ctx.attr.onDismissCallback?.invoke()
                            }
                        }
                    }
                }
            }
        }
    }
}

internal class MessageOptionsOverlayAttr : ComposeAttr() {
    var isVisible: Boolean by observable(false)
    var targetMessage: ChatMessage? by observable(null)
    var menuActions: List<MessageAction> by observable(emptyList())
    var targetX: Float by observable(0f)
    var targetY: Float by observable(0f)
    var targetW: Float by observable(0f)
    var targetH: Float by observable(0f)
    // 主题色
    var menuBgColor: Long by observable(0xF2FFFFFF)
    var textColor: Long by observable(0xFF1A1A1A)
    var destructiveColor: Long by observable(0xFFFF3B30)
    var dividerColor: Long by observable(0x1A000000)
    // 气泡样式
    var bubblePrimaryColor: Long by observable(0xFF4F8FFF)
    var bubblePrimaryGradientEndColor: Long by observable(0xFF6C5CE7)
    var bubbleOtherBubbleColor: Long by observable(0xFFFFFFFF)
    var bubbleOtherTextColor: Long by observable(0xFF333333)
    var bubbleSelfTextColor: Long by observable(0xFFFFFFFF)
    var bubblePaddingH: Float by observable(12f)
    var bubblePaddingV: Float by observable(10f)
    var bubbleFontSize: Float by observable(15f)
    var bubbleLineHeight: Float by observable(22f)
    // 回调
    var onActionClickCallback: ((MessageAction) -> Unit)? = null
    var onReactionSelectCallback: ((String) -> Unit)? = null
    var onDismissCallback: (() -> Unit)? = null
}

internal fun ViewContainer<*, *>.MessageOptionsOverlay(init: MessageOptionsOverlayView.() -> Unit) {
    addChild(MessageOptionsOverlayView(), init)
}

// ============================
// 默认渲染函数（内部使用）
// ============================

/**
 * 默认文本气泡渲染（支持上下文感知的消息分组）
 */
internal fun renderDefaultBubble(
    container: ViewContainer<*, *>,
    msgContext: MessageContext,
    cfg: ChatSessionConfig
) {
    val message = msgContext.message
    val theme = cfg.theme
    val themeColors = theme.resolvedColors()
    val listOptions = cfg.messageListOptions

    // 分组渲染：只有分组中最后一条消息显示头像（参考 Stream Chat 的 messagePositionHandler）
    val showAvatarForThis = if (listOptions.enableMessageGrouping) {
        listOptions.showAvatar && msgContext.isLastInGroup
    } else {
        listOptions.showAvatar
    }
    // 分组渲染：只有分组中第一条消息显示发送者名称
    val showNameForThis = if (listOptions.enableMessageGrouping) {
        !message.isSelf && listOptions.showSenderName && msgContext.isFirstInGroup
    } else {
        !message.isSelf && listOptions.showSenderName
    }
    // 分组内消息间距缩小
    val verticalPadding = if (listOptions.enableMessageGrouping && !msgContext.isFirstInGroup) {
        1f
    } else {
        theme.rowPaddingV
    }

    container.ChatBubble {
        attr {
            content = message.content
            isSelf = message.isSelf
            avatarUrl = message.senderAvatar
            selfAvatarUrl = cfg.selfAvatarUrl
            senderName = if (showNameForThis) message.senderName else ""
            primaryColor = theme.primaryColor
            primaryGradientEndColor = theme.primaryGradientEndColor
            otherBubbleColor = theme.otherBubbleColor
            otherTextColor = theme.otherTextColor
            selfTextColor = theme.selfTextColor
            showAvatar = showAvatarForThis
            showAvatarPlaceholder = !showAvatarForThis && listOptions.showAvatar
            status = message.status
            // 布局配置透传
            bubbleMaxWidthRatio = theme.bubbleMaxWidthRatio
            bubblePaddingH = theme.bubblePaddingH
            bubblePaddingV = theme.bubblePaddingV
            messageFontSize = theme.messageFontSize
            messageLineHeight = theme.messageLineHeight
            avatarSize = theme.avatarSize
            rowPaddingV = verticalPadding
            rowPaddingH = theme.rowPaddingH
            avatarBubbleGap = theme.avatarBubbleGap
            // 新增属性
            reactions = message.reactions
            isEdited = message.isEdited
            isDeleted = message.isDeleted
            isPinned = message.isPinned
            // P0: 主题色属性（从 resolvedColors 传入，消除硬编码）
            senderNameColor = themeColors.senderNameColor
            avatarPlaceholderColor = themeColors.avatarPlaceholderColor
            readReceiptColor = themeColors.readReceiptColor
            editedLabelColor = themeColors.editedLabelColor
            pinnedIndicatorColor = themeColors.pinnedIndicatorColor
            errorColor = themeColors.errorColor
            // P0: 引用回复属性
            val quoted = message.quotedMessage
            if (quoted != null) {
                quotedMessageContent = quoted.content
                quotedMessageSender = quoted.senderName
                quoteReplyBgColor = themeColors.quoteReplyBgColor
                quoteReplyBarColor = themeColors.quoteReplyBarColor
                quoteReplyTextColor = themeColors.quoteReplyTextColor
            }
            // P1: 线程回复
            threadCount = message.threadCount
        }
        event {
            onClick = {
                cfg.onMessageClick?.invoke(message)
            }
            onLongPress = {
                cfg.onMessageLongPress?.invoke(message)
            }
            onLongPressWithPosition = { x, y, w, h ->
                val correctedY = cfg.correctBubbleY(y)
                cfg.onMessageLongPressWithPosition?.invoke(message, x, correctedY, w, h)
                    ?: cfg.onMessageLongPress?.invoke(message)
            }
            onReactionClick = { type ->
                cfg.onReactionClick?.invoke(message, type)
            }
            // P0: 头像点击
            onAvatarClick = {
                cfg.onAvatarClick?.invoke(message)
            }
            // P0: 重发按钮点击
            onResendClick = {
                cfg.onResend?.invoke(message)
            }
            // P1: 线程回复点击
            onThreadClick = {
                cfg.onThreadClick?.invoke(message)
            }
        }
    }
}

/**
 * 默认图片消息渲染（新增，参考 Stream Chat 的 ImageAttachmentContent）
 */
internal fun renderDefaultImageBubble(
    container: ViewContainer<*, *>,
    msgContext: MessageContext,
    cfg: ChatSessionConfig
) {
    val message = msgContext.message
    val theme = cfg.theme
    val themeColors = theme.resolvedColors()
    val listOptions = cfg.messageListOptions

    // 分组渲染逻辑（与文本气泡一致）
    val showAvatarForThis = if (listOptions.enableMessageGrouping) {
        listOptions.showAvatar && msgContext.isLastInGroup
    } else {
        listOptions.showAvatar
    }
    val showNameForThis = if (listOptions.enableMessageGrouping) {
        !message.isSelf && listOptions.showSenderName && msgContext.isFirstInGroup
    } else {
        !message.isSelf && listOptions.showSenderName
    }
    val verticalPadding = if (listOptions.enableMessageGrouping && !msgContext.isFirstInGroup) {
        1f
    } else {
        theme.rowPaddingV
    }

    // 计算图片显示尺寸（优先从 Attachment 读取，兼容 extra Map）
    val attachment = message.attachments.firstOrNull()
    val rawWidth = attachment?.width?.toFloat()
        ?: message.extra["width"]?.toFloatOrNull() ?: 0f
    val rawHeight = attachment?.height?.toFloat()
        ?: message.extra["height"]?.toFloatOrNull() ?: 0f
    val maxW = theme.imageMaxWidth
    val maxH = theme.imageMaxHeight
    val (displayWidth, displayHeight) = if (rawWidth > 0 && rawHeight > 0) {
        val scale = minOf(maxW / rawWidth, maxH / rawHeight, 1f)
        (rawWidth * scale) to (rawHeight * scale)
    } else {
        maxW to maxH
    }

    // 是否为视频消息（需要播放按钮覆盖层）
    val isVideo = message.type == MessageType.VIDEO

    container.apply {
        var imgBubbleRef: ViewRef<DivView>? = null
        View {
            attr {
                flexDirectionRow()
                padding(verticalPadding, theme.rowPaddingH, verticalPadding, theme.rowPaddingH)
                if (message.isSelf) {
                    justifyContent(FlexJustifyContent.FLEX_END)
                } else {
                    justifyContent(FlexJustifyContent.FLEX_START)
                }
            }

            if (!message.isSelf) {
                // 对方消息：名字在第一组消息的上方，与气泡左边缘对齐
                View {
                    attr {
                        flexDirection(FlexDirection.COLUMN)
                        flex(1f)
                    }
                    // 发送者名称
                    if (showNameForThis) {
                        Text {
                            attr {
                                text(message.senderName)
                                fontSize(12f)
                                color(Color(themeColors.senderNameColor))
                                marginBottom(4f)
                                if (listOptions.showAvatar) {
                                    marginLeft(theme.avatarSize + theme.avatarBubbleGap)
                                }
                            }
                        }
                    }
                    // 头像 + 图片行
                    View {
                        attr {
                            flexDirectionRow()
                        }
                        if (showAvatarForThis) {
                            View {
                                attr {
                                    size(theme.avatarSize, theme.avatarSize)
                                    borderRadius(theme.avatarRadius)
                                    backgroundColor(Color(themeColors.avatarPlaceholderColor))
                                    marginTop(2f)
                                }
                                Image {
                                    attr {
                                        size(theme.avatarSize, theme.avatarSize)
                                        borderRadius(theme.avatarRadius)
                                        src(message.senderAvatar.ifEmpty { ChatBubbleView.DEFAULT_AVATAR })
                                        resizeCover()
                                    }
                                }
                                // P0: 头像点击
                                event {
                                    click { cfg.onAvatarClick?.invoke(message) }
                                }
                            }
                        } else if (listOptions.showAvatar) {
                            View {
                                attr {
                                    size(theme.avatarSize, theme.avatarSize)
                                }
                            }
                        }
                        // 图片内容区
                        View {
                            attr {
                                marginLeft(if (listOptions.showAvatar) theme.avatarBubbleGap else 0f)
                                flexDirection(FlexDirection.COLUMN)
                            }
                            // 图片 + 视频播放按钮
                            View {
                                ref { imgBubbleRef = it }
                                attr {
                                    size(displayWidth, displayHeight)
                                    borderRadius(theme.imageRadius)
                                    backgroundColor(Color(themeColors.avatarPlaceholderColor))
                                }
                                Image {
                                    attr {
                                        size(displayWidth, displayHeight)
                                        borderRadius(theme.imageRadius)
                                        // 视频消息优先使用缩略图
                                        val imgSrc = if (isVideo) {
                                            attachment?.thumbnailUrl?.ifEmpty { message.content } ?: message.content
                                        } else {
                                            message.content
                                        }
                                        src(imgSrc)
                                        resizeCover()
                                    }
                                }
                                // P1: 视频播放按钮覆盖层
                                if (isVideo) {
                                    View {
                                        attr {
                                            positionAbsolute()
                                            top(0f)
                                            left(0f)
                                            size(displayWidth, displayHeight)
                                            allCenter()
                                        }
                                        // 半透明黑色圆形背景
                                        View {
                                            attr {
                                                size(48f, 48f)
                                                borderRadius(24f)
                                                backgroundColor(Color(0x80000000))
                                                allCenter()
                                            }
                                            // 播放三角形（使用 unicode 字符）
                                            Text {
                                                attr {
                                                    text("▶")
                                                    fontSize(20f)
                                                    color(Color.WHITE)
                                                    marginLeft(3f) // 视觉居中微调
                                                }
                                            }
                                        }
                                    }
                                }
                                event {
                                    click {
                                        cfg.onMessageClick?.invoke(message)
                                    }
                                    longPress {
                                        if (cfg.onMessageLongPressWithPosition != null) {
                                            imgBubbleRef?.view?.let { view ->
                                                val frame = view.frame
                                                val frameInRoot = view.convertFrame(frame, toView = null)
                                                val correctedY = cfg.correctBubbleY(frameInRoot.y)
                                                cfg.onMessageLongPressWithPosition?.invoke(
                                                    message, frameInRoot.x, correctedY,
                                                    frameInRoot.width, frameInRoot.height
                                                )
                                            } ?: cfg.onMessageLongPress?.invoke(message)
                                        } else {
                                            cfg.onMessageLongPress?.invoke(message)
                                        }
                                    }
                                }
                            }
                            // 置顶标记
                            if (message.isPinned) {
                                Text {
                                    attr {
                                        text("📌 已置顶")
                                        fontSize(10f)
                                        color(Color(themeColors.pinnedIndicatorColor))
                                        marginTop(2f)
                                    }
                                }
                            }
                            // P1: 线程回复入口
                            if (message.threadCount > 0) {
                                View {
                                    attr {
                                        flexDirectionRow()
                                        alignItems(FlexAlign.CENTER)
                                        marginTop(4f)
                                    }
                                    Text {
                                        attr {
                                            text("💬 ${message.threadCount} 条回复")
                                            fontSize(12f)
                                            color(Color(theme.primaryColor))
                                            fontWeightMedium()
                                        }
                                    }
                                    event {
                                        click { cfg.onThreadClick?.invoke(message) }
                                    }
                                }
                            }
                            // 反应栏
                            if (message.reactions.isNotEmpty()) {
                                ChatReactionBar {
                                    attr {
                                        reactions = message.reactions
                                    }
                                    event {
                                        onReactionClick = { type ->
                                            cfg.onReactionClick?.invoke(message, type)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // 自己的消息：重发 + 图片 + 头像在右
                // P0: 重发按钮（自动根据 status 显示）
                if (message.status == MessageStatus.FAILED) {
                    View {
                        attr {
                            size(24f, 24f)
                            borderRadius(12f)
                            backgroundColor(Color(themeColors.errorColor))
                            allCenter()
                            marginRight(6f)
                            alignSelf(FlexAlign.CENTER)
                        }
                        Text {
                            attr {
                                text("!")
                                fontSize(14f)
                                fontWeightBold()
                                color(Color.WHITE)
                            }
                        }
                        event {
                            click { cfg.onResend?.invoke(message) }
                        }
                    }
                }
                // 图片 + 反应栏
                View {
                    attr {
                        flexDirection(FlexDirection.COLUMN)
                        alignItems(FlexAlign.FLEX_END)
                    }
                    View {
                        ref { imgBubbleRef = it }
                        attr {
                            size(displayWidth, displayHeight)
                            borderRadius(theme.imageRadius)
                            backgroundColor(Color(themeColors.avatarPlaceholderColor))
                        }
                        Image {
                            attr {
                                size(displayWidth, displayHeight)
                                borderRadius(theme.imageRadius)
                                val imgSrc = if (isVideo) {
                                    attachment?.thumbnailUrl?.ifEmpty { message.content } ?: message.content
                                } else {
                                    message.content
                                }
                                src(imgSrc)
                                resizeCover()
                            }
                        }
                        // P1: 视频播放按钮覆盖层
                        if (isVideo) {
                            View {
                                attr {
                                    positionAbsolute()
                                    top(0f)
                                    left(0f)
                                    size(displayWidth, displayHeight)
                                    allCenter()
                                }
                                View {
                                    attr {
                                        size(48f, 48f)
                                        borderRadius(24f)
                                        backgroundColor(Color(0x80000000))
                                        allCenter()
                                    }
                                    Text {
                                        attr {
                                            text("▶")
                                            fontSize(20f)
                                            color(Color.WHITE)
                                            marginLeft(3f)
                                        }
                                    }
                                }
                            }
                        }
                        event {
                            click {
                                cfg.onMessageClick?.invoke(message)
                            }
                            longPress {
                                if (cfg.onMessageLongPressWithPosition != null) {
                                    imgBubbleRef?.view?.let { view ->
                                        val frame = view.frame
                                        val frameInRoot = view.convertFrame(frame, toView = null)
                                        val correctedY = cfg.correctBubbleY(frameInRoot.y)
                                        cfg.onMessageLongPressWithPosition?.invoke(
                                            message, frameInRoot.x, correctedY,
                                            frameInRoot.width, frameInRoot.height
                                        )
                                    } ?: cfg.onMessageLongPress?.invoke(message)
                                } else {
                                    cfg.onMessageLongPress?.invoke(message)
                                }
                            }
                        }
                    }
                    // 置顶标记
                    if (message.isPinned) {
                        Text {
                            attr {
                                text("📌 已置顶")
                                fontSize(10f)
                                color(Color(themeColors.pinnedIndicatorColor))
                                marginTop(2f)
                            }
                        }
                    }
                    // P1: 线程回复入口
                    if (message.threadCount > 0) {
                        View {
                            attr {
                                flexDirectionRow()
                                alignItems(FlexAlign.CENTER)
                                marginTop(4f)
                            }
                            Text {
                                attr {
                                    text("💬 ${message.threadCount} 条回复")
                                    fontSize(12f)
                                    color(Color(theme.primaryColor))
                                    fontWeightMedium()
                                }
                            }
                            event {
                                click { cfg.onThreadClick?.invoke(message) }
                            }
                        }
                    }
                    // 反应栏
                    if (message.reactions.isNotEmpty()) {
                        ChatReactionBar {
                            attr {
                                reactions = message.reactions
                            }
                            event {
                                onReactionClick = { type ->
                                    cfg.onReactionClick?.invoke(message, type)
                                }
                            }
                        }
                    }
                }
                // 头像
                if (showAvatarForThis) {
                    View {
                        attr {
                            size(theme.avatarSize, theme.avatarSize)
                            borderRadius(theme.avatarRadius)
                            backgroundColor(Color(themeColors.avatarPlaceholderColor))
                            marginLeft(theme.avatarBubbleGap)
                            marginTop(2f)
                        }
                        Image {
                            attr {
                                size(theme.avatarSize, theme.avatarSize)
                                borderRadius(theme.avatarRadius)
                                src(cfg.selfAvatarUrl.ifEmpty { ChatBubbleView.SELF_AVATAR })
                                resizeCover()
                            }
                        }
                        // P0: 头像点击
                        event {
                            click { cfg.onAvatarClick?.invoke(message) }
                        }
                    }
                } else if (listOptions.showAvatar) {
                    View {
                        attr {
                            marginLeft(theme.avatarBubbleGap)
                            size(theme.avatarSize, theme.avatarSize)
                        }
                    }
                }
            }
        }
    }
}

/**
 * P1: 默认文件消息渲染（卡片样式：文件图标 + 文件名 + 文件大小）
 */
internal fun renderDefaultFileBubble(
    container: ViewContainer<*, *>,
    msgContext: MessageContext,
    cfg: ChatSessionConfig
) {
    val message = msgContext.message
    val theme = cfg.theme
    val themeColors = theme.resolvedColors()
    val listOptions = cfg.messageListOptions
    val attachment = message.attachments.firstOrNull()

    val showAvatarForThis = if (listOptions.enableMessageGrouping) {
        listOptions.showAvatar && msgContext.isLastInGroup
    } else {
        listOptions.showAvatar
    }
    val showNameForThis = if (listOptions.enableMessageGrouping) {
        !message.isSelf && listOptions.showSenderName && msgContext.isFirstInGroup
    } else {
        !message.isSelf && listOptions.showSenderName
    }
    val verticalPadding = if (listOptions.enableMessageGrouping && !msgContext.isFirstInGroup) {
        1f
    } else {
        theme.rowPaddingV
    }

    // 文件名和大小
    val fileName = attachment?.title?.ifEmpty { message.content } ?: message.content
    val fileSize = attachment?.fileSize ?: message.extra["fileSize"]?.toLongOrNull() ?: 0L
    val fileSizeText = formatFileSize(fileSize)

    container.apply {
        View {
            attr {
                flexDirectionRow()
                padding(verticalPadding, theme.rowPaddingH, verticalPadding, theme.rowPaddingH)
                if (message.isSelf) {
                    justifyContent(FlexJustifyContent.FLEX_END)
                } else {
                    justifyContent(FlexJustifyContent.FLEX_START)
                }
            }

            if (!message.isSelf) {
                View {
                    attr {
                        flexDirection(FlexDirection.COLUMN)
                        flex(1f)
                    }
                    if (showNameForThis) {
                        Text {
                            attr {
                                text(message.senderName)
                                fontSize(12f)
                                color(Color(themeColors.senderNameColor))
                                marginBottom(4f)
                                if (listOptions.showAvatar) {
                                    marginLeft(theme.avatarSize + theme.avatarBubbleGap)
                                }
                            }
                        }
                    }
                    View {
                        attr { flexDirectionRow() }
                        if (showAvatarForThis) {
                            View {
                                attr {
                                    size(theme.avatarSize, theme.avatarSize)
                                    borderRadius(theme.avatarRadius)
                                    backgroundColor(Color(themeColors.avatarPlaceholderColor))
                                    marginTop(2f)
                                }
                                Image {
                                    attr {
                                        size(theme.avatarSize, theme.avatarSize)
                                        borderRadius(theme.avatarRadius)
                                        src(message.senderAvatar.ifEmpty { ChatBubbleView.DEFAULT_AVATAR })
                                        resizeCover()
                                    }
                                }
                                event { click { cfg.onAvatarClick?.invoke(message) } }
                            }
                        } else if (listOptions.showAvatar) {
                            View { attr { size(theme.avatarSize, theme.avatarSize) } }
                        }
                        // 文件卡片
                        View {
                            attr {
                                marginLeft(if (listOptions.showAvatar) theme.avatarBubbleGap else 0f)
                                backgroundColor(Color(theme.otherBubbleColor))
                                borderRadius(12f)
                                padding(12f, 14f, 12f, 14f)
                                flexDirectionRow()
                                alignItems(FlexAlign.CENTER)
                                width(theme.bubbleMaxWidthRatio * 375f) // 固定宽度卡片
                                boxShadow(BoxShadow(0f, 1f, 6f, Color(0x1A000000)))
                            }
                            // 文件图标
                            View {
                                attr {
                                    size(40f, 40f)
                                    borderRadius(8f)
                                    backgroundColor(Color(themeColors.primaryColor))
                                    allCenter()
                                    marginRight(12f)
                                }
                                Text {
                                    attr {
                                        text("📄")
                                        fontSize(20f)
                                    }
                                }
                            }
                            // 文件名 + 大小
                            View {
                                attr {
                                    flex(1f)
                                    flexDirection(FlexDirection.COLUMN)
                                }
                                Text {
                                    attr {
                                        text(fileName)
                                        fontSize(14f)
                                        color(Color(theme.otherTextColor))
                                        lines(2)
                                    }
                                }
                                if (fileSizeText.isNotEmpty()) {
                                    Text {
                                        attr {
                                            text(fileSizeText)
                                            fontSize(11f)
                                            color(Color(themeColors.readReceiptColor))
                                            marginTop(2f)
                                        }
                                    }
                                }
                            }
                            event {
                                click { cfg.onMessageClick?.invoke(message) }
                                longPress { cfg.onMessageLongPress?.invoke(message) }
                            }
                        }
                    }
                }
            } else {
                // 自己发送的文件
                if (message.status == MessageStatus.FAILED) {
                    View {
                        attr {
                            size(24f, 24f)
                            borderRadius(12f)
                            backgroundColor(Color(themeColors.errorColor))
                            allCenter()
                            marginRight(6f)
                            alignSelf(FlexAlign.CENTER)
                        }
                        Text {
                            attr {
                                text("!")
                                fontSize(14f)
                                fontWeightBold()
                                color(Color.WHITE)
                            }
                        }
                        event { click { cfg.onResend?.invoke(message) } }
                    }
                }
                View {
                    attr {
                        backgroundColor(Color(theme.otherBubbleColor))
                        borderRadius(12f)
                        padding(12f, 14f, 12f, 14f)
                        flexDirectionRow()
                        alignItems(FlexAlign.CENTER)
                        width(theme.bubbleMaxWidthRatio * 375f)
                        boxShadow(BoxShadow(0f, 1f, 6f, Color(0x1A000000)))
                    }
                    View {
                        attr {
                            size(40f, 40f)
                            borderRadius(8f)
                            backgroundColor(Color(themeColors.primaryColor))
                            allCenter()
                            marginRight(12f)
                        }
                        Text {
                            attr {
                                text("📄")
                                fontSize(20f)
                            }
                        }
                    }
                    View {
                        attr {
                            flex(1f)
                            flexDirection(FlexDirection.COLUMN)
                        }
                        Text {
                            attr {
                                text(fileName)
                                fontSize(14f)
                                color(Color(theme.otherTextColor))
                                lines(2)
                            }
                        }
                        if (fileSizeText.isNotEmpty()) {
                            Text {
                                attr {
                                    text(fileSizeText)
                                    fontSize(11f)
                                    color(Color(themeColors.readReceiptColor))
                                    marginTop(2f)
                                }
                            }
                        }
                    }
                    event {
                        click { cfg.onMessageClick?.invoke(message) }
                        longPress { cfg.onMessageLongPress?.invoke(message) }
                    }
                }
                if (showAvatarForThis) {
                    View {
                        attr {
                            size(theme.avatarSize, theme.avatarSize)
                            borderRadius(theme.avatarRadius)
                            backgroundColor(Color(themeColors.avatarPlaceholderColor))
                            marginLeft(theme.avatarBubbleGap)
                            marginTop(2f)
                        }
                        Image {
                            attr {
                                size(theme.avatarSize, theme.avatarSize)
                                borderRadius(theme.avatarRadius)
                                src(cfg.selfAvatarUrl.ifEmpty { ChatBubbleView.SELF_AVATAR })
                                resizeCover()
                            }
                        }
                        event { click { cfg.onAvatarClick?.invoke(message) } }
                    }
                } else if (listOptions.showAvatar) {
                    View {
                        attr {
                            marginLeft(theme.avatarBubbleGap)
                            size(theme.avatarSize, theme.avatarSize)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 格式化文件大小为可读字符串
 */
private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return ""
    return when {
        bytes < 1024 -> "${bytes} B"
        bytes < 1024 * 1024 -> "${roundToDecimals(bytes / 1024.0, 1)} KB"
        bytes < 1024 * 1024 * 1024 -> "${roundToDecimals(bytes / (1024.0 * 1024.0), 1)} MB"
        else -> "${roundToDecimals(bytes / (1024.0 * 1024.0 * 1024.0), 2)} GB"
    }
}


private fun roundToDecimals(value: Double, decimals: Int): String {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    val rounded = kotlin.math.round(value * multiplier) / multiplier
    // 确保小数位数正确（例如 1.0 → "1.0" 而非 "1"）
    val parts = rounded.toString().split(".")
    val intPart = parts[0]
    val fracPart = if (parts.size > 1) parts[1] else ""
    val paddedFrac = fracPart.padEnd(decimals, '0').take(decimals)
    return "$intPart.$paddedFrac"
}
