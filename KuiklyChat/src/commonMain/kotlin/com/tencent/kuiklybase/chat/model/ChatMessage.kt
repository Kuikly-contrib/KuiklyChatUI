package com.tencent.kuiklybase.chat.model

data class ChatMessage(
    val id: String,
    val content: String,
    val isSelf: Boolean,
    val type: MessageType = MessageType.TEXT,
    val status: MessageStatus = MessageStatus.SENT,
    val senderName: String = "",
    val senderAvatar: String = "",
    val timestamp: Long = 0L,
    val extra: Map<String, String> = emptyMap(),
    // ---- 新增字段（均有默认值，向后兼容） ----
    val senderId: String = "",
    val reactions: List<ReactionItem> = emptyList(),
    val threadCount: Int = 0,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    val isPinned: Boolean = false,
    val readBy: List<String> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
    /** 引用回复的原消息（非 null 时表示这是一条引用回复消息） */
    val quotedMessage: ChatMessage? = null
)

/**
 * 消息上下文（参考 Stream Chat 的 MessageItemState）
 *
 * 向 Slot 提供当前消息的上下文信息，包括前后消息，
 * 支持实现"连续同一发送者合并头像"、"连续消息缩小间距"等效果。
 *
 * @param message 当前消息
 * @param previousMessage 上一条消息（首条消息时为 null）
 * @param nextMessage 下一条消息（末条消息时为 null）
 * @param index 当前消息在列表中的索引
 * @param isFirstInGroup 是否为同一发送者连续消息的第一条
 * @param isLastInGroup 是否为同一发送者连续消息的最后一条（显示头像的位置）
 */
data class MessageContext(
    val message: ChatMessage,
    val previousMessage: ChatMessage? = null,
    val nextMessage: ChatMessage? = null,
    val index: Int = 0,
    val isFirstInGroup: Boolean = true,
    val isLastInGroup: Boolean = true
)

/**
 * 时间分组间隔阈值（毫秒）
 *
 * 两条消息之间超过此间隔，会自动插入时间分隔线。
 * 默认 5 分钟。
 */
const val DEFAULT_TIME_GROUP_INTERVAL = 5 * 60 * 1000L

// ============================
// 工具类
// ============================

/**
 * 聊天消息工具类
 */
