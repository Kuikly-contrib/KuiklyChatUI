package com.tencent.kuiklybase.chat.session

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.views.*
import com.tencent.kuiklybase.chat.bubble.*
import com.tencent.kuiklybase.chat.indicator.*
import com.tencent.kuiklybase.chat.model.*

/**
 * 渲染消息列表中的一项：加载历史提示（仅首项）+ 日期分隔符 + 消息本体。
 *
 * 由 [ChatSession] 的 `vforLazy` 调用。`vforLazy` 要求闭包只有一个根子节点，
 * 因此这里用一个 `alignSelfStretch` 的 View 把三部分包起来。
 */
internal fun ViewContainer<*, *>.ChatMessageListItem(
    message: ChatMessage,
    index: Int,
    messages: ObservableList<ChatMessage>,
    cfg: ChatSessionConfig
) {
    val listOptions = cfg.messageListOptions
    val themeColors = cfg.theme.resolvedColors()

    // 构建消息上下文（参考 Stream Chat 的 MessageItemState）
    val msgContext = ChatMessageHelper.buildMessageContext(
        messages = messages,
        index = index,
        groupingInterval = listOptions.messageGroupingInterval
    )

    // 使用 messagePositionHandler 计算消息在分组中的位置（可替换策略）
    val messagePosition = cfg.getMessagePosition(
        msgContext.previousMessage, msgContext.message, msgContext.nextMessage
    )
    // 将 Handler 的结果映射到 MessageContext 的分组字段
    val effectiveContext = if (listOptions.enableMessageGrouping) {
        msgContext.copy(
            isFirstInGroup = messagePosition == MessagePosition.TOP || messagePosition == MessagePosition.SINGLE,
            isLastInGroup = messagePosition == MessagePosition.BOTTOM || messagePosition == MessagePosition.SINGLE
        )
    } else {
        msgContext
    }

    View {
        attr {
            // 保证 List 项在交叉轴上拉伸填满宽度，否则内部 flex(1f) 会失效
            alignSelfStretch()
        }

        if (index == 0 && listOptions.onLoadEarlier != null) {
            renderLoadEarlierHeader(cfg, themeColors)
        }

        if (listOptions.showTimeGroup && message.timestamp > 0L) {
            renderDateSeparator(message, index, messages, cfg, themeColors)
        }

        renderMessageBody(effectiveContext, cfg, themeColors)
    }
}

/** 列表顶部的历史消息加载状态提示：加载中 / 可加载更多 / 已到顶。 */
private fun ViewContainer<*, *>.renderLoadEarlierHeader(
    cfg: ChatSessionConfig,
    themeColors: ChatThemeColors
) {
    val listOptions = cfg.messageListOptions
    val slots = cfg.slots

    if (listOptions.isLoadingEarlier) {
        if (slots.loadingMoreContent != null) {
            slots.loadingMoreContent!!.invoke(this)
        } else {
            hintRow("加载中...", themeColors.loadingTextColor)
        }
    } else if (listOptions.hasMoreEarlier) {
        hintRow("上拉加载更多", themeColors.hintTextColor)
    } else {
        hintRow("—— 已经到顶了 ——", themeColors.hintTextColor)
    }
}

private fun ViewContainer<*, *>.hintRow(hint: String, textColor: Long) {
    View {
        attr {
            allCenter()
            padding(12f, 0f, 8f, 0f)
        }
        Text {
            attr {
                text(hint)
                fontSize(12f)
                color(Color(textColor))
            }
        }
    }
}

/** 与上一条消息跨天时插入日期分隔符。 */
private fun ViewContainer<*, *>.renderDateSeparator(
    message: ChatMessage,
    index: Int,
    messages: ObservableList<ChatMessage>,
    cfg: ChatSessionConfig,
    themeColors: ChatThemeColors
) {
    val prevTimestamp = if (index > 0) messages[index - 1].timestamp else 0L
    val needSeparator = index == 0 || cfg.shouldShowDateSeparator(prevTimestamp, message.timestamp)
    if (!needSeparator) return

    val formattedDate = cfg.formatDateSeparator(message.timestamp)
    if (formattedDate.isEmpty()) return

    val slot = cfg.slots.dateSeparator
    if (slot != null) {
        slot.invoke(this, message.timestamp, formattedDate)
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

/**
 * 消息本体渲染，优先级：统一 Slot > 类型独立 Slot > 渲染器工厂 > 内置默认渲染。
 */
private fun ViewContainer<*, *>.renderMessageBody(
    context: MessageContext,
    cfg: ChatSessionConfig,
    themeColors: ChatThemeColors
) {
    val message = context.message
    val slots = cfg.slots

    View itemRoot@{
        if (message.type == MessageType.SYSTEM) {
            val slot = slots.systemMessage
            if (slot != null) {
                slot.invoke(this@itemRoot, message, cfg)
            } else {
                val factory = cfg.messageRenderers.firstOrNull { it.canRender(message) }
                if (factory != null) {
                    factory.render(this@itemRoot, context, cfg)
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
            return@itemRoot
        }

        when {
            slots.messageBubble != null ->
                slots.messageBubble!!.invoke(this@itemRoot, context, cfg)
            message.type == MessageType.TEXT && slots.textBubble != null ->
                slots.textBubble!!.invoke(this@itemRoot, context, cfg)
            message.type == MessageType.IMAGE && slots.imageBubble != null ->
                slots.imageBubble!!.invoke(this@itemRoot, context, cfg)
            message.type == MessageType.CUSTOM && slots.customBubble != null ->
                slots.customBubble!!.invoke(this@itemRoot, context, cfg)
            else -> {
                val factory = cfg.messageRenderers.firstOrNull { it.canRender(message) }
                if (factory != null) {
                    factory.render(this@itemRoot, context, cfg)
                } else {
                    renderBuiltInBubble(this@itemRoot, context, cfg)
                }
            }
        }
    }
}

/** 没有任何 Slot / 工厂命中时的兜底渲染。 */
private fun renderBuiltInBubble(
    container: ViewContainer<*, *>,
    context: MessageContext,
    cfg: ChatSessionConfig
) {
    when (context.message.type) {
        MessageType.TEXT, MessageType.FILE -> renderDefaultBubble(container, context, cfg)
        MessageType.IMAGE, MessageType.VIDEO -> renderDefaultImageBubble(container, context, cfg)
        else -> container.ChatSystemMessage {
            attr {
                this.message = "[不支持的消息类型]"
            }
        }
    }
}
