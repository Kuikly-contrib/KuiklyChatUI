package com.tencent.kuiklybase.chat.session

import com.tencent.kuikly.core.base.*
import com.tencent.kuiklybase.chat.bubble.*
import com.tencent.kuiklybase.chat.model.*

/**
 * 默认文本气泡渲染（支持上下文感知的消息分组）。
 *
 * 没有任何 Slot 或 [MessageRendererFactory] 命中 TEXT / FILE 消息时的兜底实现。
 */
internal fun renderDefaultBubble(
    container: ViewContainer<*, *>,
    msgContext: MessageContext,
    cfg: ChatSessionConfig
) {
    val message = msgContext.message
    val theme = cfg.theme
    val themeColors = theme.resolvedColors()
    val layout = msgContext.resolveGroupLayout(cfg)

    container.ChatBubble {
        attr {
            content = message.content
            isSelf = message.isSelf
            avatarUrl = message.senderAvatar
            selfAvatarUrl = cfg.selfAvatarUrl
            senderName = if (layout.showSenderName) message.senderName else ""
            primaryColor = theme.primaryColor
            primaryGradientEndColor = theme.primaryGradientEndColor
            otherBubbleColor = theme.otherBubbleColor
            otherTextColor = theme.otherTextColor
            selfTextColor = theme.selfTextColor
            showAvatar = layout.showAvatar
            showAvatarPlaceholder = !layout.showAvatar && cfg.messageListOptions.showAvatar
            status = message.status
            // 布局配置透传
            bubbleMaxWidthRatio = theme.bubbleMaxWidthRatio
            bubblePaddingH = theme.bubblePaddingH
            bubblePaddingV = theme.bubblePaddingV
            messageFontSize = theme.messageFontSize
            messageLineHeight = theme.messageLineHeight
            avatarSize = theme.avatarSize
            rowPaddingV = layout.verticalPadding
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
            // 文件消息属性
            if (message.type == MessageType.FILE) {
                val attachment = message.attachments.firstOrNull()
                fileName = attachment?.title?.ifEmpty { message.content } ?: message.content
                fileSize = attachment?.fileSize ?: message.extra["fileSize"]?.toLongOrNull() ?: 0L
                fileMimeType = attachment?.mimeType ?: message.extra["mimeType"] ?: ""
            }
            componentFactory = cfg.componentFactory
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
