package com.tencent.kuiklybase.chat.session

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.directives.scrollToPosition
import com.tencent.kuikly.core.directives.vforLazy
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.views.*
import com.tencent.kuiklybase.chat.legacy.composer.*
import com.tencent.kuiklybase.chat.indicator.*
import com.tencent.kuiklybase.chat.model.*
import com.tencent.kuiklybase.chat.navigation.*

// 本文件只保留 ChatSession 的骨架（导航栏 / 列表 / 输入区 / 操作菜单）与滚动控制，
// 单条消息的渲染在 ChatSessionMessageItem.kt，默认气泡在 DefaultTextBubbleRenderer.kt
// 与 DefaultImageBubbleRenderer.kt，操作菜单浮层在 MessageOptionsOverlay.kt。

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
    // 记录上次已知的消息数量，用于检测新消息
    var lastKnownSize = messageList().size

    // ========== 加载历史消息相关状态 ==========
    // 记录插入历史消息前的消息数量，用于位置补偿
    var sizeBeforeLoadEarlier = 0
    // 是否需要进行位置补偿（头部插入历史消息后防跳动）
    var needPositionCompensation = false

    // ========== 滚动控制 ==========

    val scrollToIndex: (Int, Boolean) -> Unit = { index, animate ->
        getPager().addTaskWhenPagerUpdateLayoutFinish {
            listViewRef?.view?.scrollToPosition(index = index, offset = 0f, animate = animate)
        }
    }

    val scrollToBottom: (Boolean) -> Unit = { animate ->
        val list = messageList()
        if (list.isNotEmpty()) {
            scrollToIndex(list.size - 1, animate)
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
            val currentSize = messageList().size
            val insertedCount = currentSize - sizeBeforeLoadEarlier
            if (insertedCount > 0) {
                scrollToIndex(insertedCount, false)
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
        val showOptions: (ChatMessage, Float, Float, Float, Float) -> Unit = { message, x, y, w, h ->
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
        }
        cfg.onMessageLongPressWithPosition = { message, x, y, w, h ->
            showOptions(message, x, y, w, h)
            // 也调用用户设置的原始回调
            userLongPressWithPosition?.invoke(message, x, y, w, h)
        }
        // 同时包装无位置的长按（作为兜底）
        if (cfg.onMessageLongPress == null) {
            cfg.onMessageLongPress = { message ->
                showOptions(message, 0f, 100f, 200f, 50f)
                userLongPress?.invoke(message)
            }
        }
    }

    // scrollToMessage：滚动到指定消息 ID 的位置
    cfg.scrollToMessageAction = { messageId, animate ->
        val index = messageList().indexOfFirst { it.id == messageId }
        if (index >= 0) {
            scrollToIndex(index, animate)
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
                        cfg._currentScrollOffsetY = params.offsetY
                        // 在滚动过程中也实时更新 atBottom 状态，避免在用户手指还在滑动时
                        // 收到新消息导致 atBottom 判断不准确
                        atBottom = params.offsetY + params.viewHeight + 10 >= params.contentHeight
                        // 检测是否滚动接近顶部，触发加载历史消息
                        checkLoadEarlier(params.offsetY)
                    }
                    scrollEnd { params ->
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
                    ChatMessageListItem(message, index, messageList(), cfg)
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
            // P0: 默认 composerHeader — 使用 ChatComposerHeader 组件
            if (slots.composerHeader == null && cfg._replyingToMessage != null) {
                slots.composerHeader = { container, state ->
                    val replyMsg = cfg._replyingToMessage
                    if (replyMsg != null) {
                        container.apply {
                            ChatComposerHeader {
                                attr {
                                    mode = ComposerHeaderMode.REPLY
                                    replyingToMessage = replyMsg
                                    backgroundColor = themeColors.composerHeaderBackgroundColor
                                    textColor = themeColors.composerHeaderTextColor
                                    closeColor = themeColors.composerHeaderCloseColor
                                    barColor = themeColors.composerHeaderBarColor
                                    quoteTextColor = themeColors.composerHeaderQuoteTextColor
                                }
                                event {
                                    onClose = {
                                        cfg.cancelReply()
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
