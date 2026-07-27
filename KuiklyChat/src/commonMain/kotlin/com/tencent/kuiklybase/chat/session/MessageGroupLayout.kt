package com.tencent.kuiklybase.chat.session

import com.tencent.kuiklybase.chat.model.MessageContext

/**
 * 消息分组带来的布局差异：连续同一发送者的消息只在末条显示头像、只在首条显示名称，且间距收窄。
 *
 * 参考 Stream Chat 的 messagePositionHandler。
 */
internal data class MessageGroupLayout(
    val showAvatar: Boolean,
    val showSenderName: Boolean,
    val verticalPadding: Float
)

/** 按当前分组配置解析该条消息的头像/名称/间距。 */
internal fun MessageContext.resolveGroupLayout(cfg: ChatSessionConfig): MessageGroupLayout {
    val listOptions = cfg.messageListOptions
    val grouping = listOptions.enableMessageGrouping

    return MessageGroupLayout(
        showAvatar = if (grouping) listOptions.showAvatar && isLastInGroup else listOptions.showAvatar,
        showSenderName = !message.isSelf && listOptions.showSenderName && (!grouping || isFirstInGroup),
        verticalPadding = if (grouping && !isFirstInGroup) 1f else cfg.theme.rowPaddingV
    )
}
