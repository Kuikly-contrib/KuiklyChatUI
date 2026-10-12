package com.tencent.kuiklybase.chat.model

public data class Attachment(
    val type: AttachmentType,
    val url: String,
    val title: String = "",
    val mimeType: String = "",
    val fileSize: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val duration: Float = 0f,
    val thumbnailUrl: String = "",
    val extra: Map<String, String> = emptyMap()
)

/**
 * 消息操作定义（对标 Stream Chat 的 MessageAction）
 *
 * 用于配置长按消息弹出的操作菜单项。
 *
 * @param key 操作标识（如 "copy"、"reply"、"quote"、"edit"、"delete"、"pin"、"reaction"）
 * @param label 操作显示文本
 * @param icon 操作图标（base64 或 URL）
 * @param isDestructive 是否为危险操作（红色显示，如删除）
 * @param isVisible 动态可见性判断函数
 */
public data class MessageAction(
    val key: String,
    val label: String,
    val icon: String = "",
    val isDestructive: Boolean = false,
    val isVisible: (ChatMessage) -> Boolean = { true }
)

/**
 * 聊天消息数据模型
 *
 * @param id 消息唯一ID
 * @param content 消息内容（文本内容或图片URL）
 * @param isSelf 是否为自己发送的消息
 * @param type 消息类型
 * @param status 消息发送状态
 * @param senderName 发送者名称
 * @param senderAvatar 发送者头像URL
 * @param timestamp 消息时间戳（毫秒）
 * @param extra 扩展数据（用于自定义消息携带额外信息，如图片尺寸、链接等）
 * @param senderId 发送者唯一 ID（区分同名用户）
 * @param reactions 消息反应列表（点赞、表情等）
 * @param threadCount 线程回复数量
 * @param isEdited 消息是否已被编辑
 * @param isDeleted 消息是否已被删除（软删除）
 * @param isPinned 消息是否被置顶
 * @param readBy 已读用户 ID 列表
 * @param attachments 附件列表（未来替代 content+extra 方案）
 */
