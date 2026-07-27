package com.tencent.kuiklybase.chat.session

import com.tencent.kuiklybase.chat.model.MessageAction
import com.tencent.kuiklybase.chat.model.MessageType

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
