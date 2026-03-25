package com.tencent.kuiklybase.chat

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.directives.scrollToPosition
import com.tencent.kuikly.core.directives.vforLazy
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.views.*


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
                                            color(Color(0xFF999999))
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
                                        color(Color(0xFFBBBBBB))
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
                                        color(Color(0xFFBBBBBB))
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
                                                MessageType.FILE -> renderDefaultBubble(this@itemRoot, msgContext, cfg)
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
                        }
                    }
                }
            }
        }

        // ========== MessageComposer 输入框（参考 Stream Chat Compose 的 MessageComposer） ==========
        if (cfg.showMessageComposer) {
            ChatMessageComposer(
                cfg = cfg,
                safeAreaBottom = cfg.composerSafeAreaBottom
            )
        }
    }
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

    // 计算图片显示尺寸
    val rawWidth = message.extra["width"]?.toFloatOrNull() ?: 0f
    val rawHeight = message.extra["height"]?.toFloatOrNull() ?: 0f
    val maxW = theme.imageMaxWidth
    val maxH = theme.imageMaxHeight
    val (displayWidth, displayHeight) = if (rawWidth > 0 && rawHeight > 0) {
        val scale = minOf(maxW / rawWidth, maxH / rawHeight, 1f)
        (rawWidth * scale) to (rawHeight * scale)
    } else {
        maxW to maxH
    }

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
                    // 发送者名称（显示在第一组消息上方，与气泡左边缘对齐）
                    if (showNameForThis) {
                        Text {
                            attr {
                                text(message.senderName)
                                fontSize(12f)
                                color(Color(0xFF999999))
                                marginBottom(4f)
                                // 名字左侧缩进：头像宽度 + 头像与气泡间距，与气泡左边缘对齐
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
                                    backgroundColor(Color(0xFFE8E8E8))
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
                            }
                        } else if (listOptions.showAvatar) {
                            // 分组内非最后一条：头像占位
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
                            // 图片
                            View {
                                ref { imgBubbleRef = it }
                                attr {
                                    size(displayWidth, displayHeight)
                                    borderRadius(theme.imageRadius)
                                    backgroundColor(Color(0xFFE8E8E8))
                                }
                                Image {
                                    attr {
                                        size(displayWidth, displayHeight)
                                        borderRadius(theme.imageRadius)
                                        src(message.content)
                                        resizeCover()
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
                                        color(Color(0xFF4F8FFF))
                                        marginTop(2f)
                                    }
                                }
                            }
                            // 反应栏（图片下方）
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
                // 自己的消息：图片 + 头像在右
                // 重发按钮
                if (message.status == MessageStatus.FAILED) {
                    View {
                        attr {
                            size(24f, 24f)
                            borderRadius(12f)
                            backgroundColor(Color(0xFFFF4444))
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
                            backgroundColor(Color(0xFFE8E8E8))
                        }
                        Image {
                            attr {
                                size(displayWidth, displayHeight)
                                borderRadius(theme.imageRadius)
                                src(message.content)
                                resizeCover()
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
                                color(Color(0xFF4F8FFF))
                                marginTop(2f)
                            }
                        }
                    }
                    // 反应栏（图片下方）
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
                            backgroundColor(Color(0xFFE8E8E8))
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
                    }
                } else if (listOptions.showAvatar) {
                    // 分组内非最后一条：头像占位
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
