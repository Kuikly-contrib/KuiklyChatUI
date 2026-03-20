package com.tencent.kuiklybase.chat

/**
 * 聊天消息类型
 */
enum class MessageType {
    TEXT,       // 文本消息
    IMAGE,      // 图片消息
    SYSTEM      // 系统消息（如时间提示、系统通知等）
}

/**
 * 聊天消息数据模型
 *
 * @param id 消息唯一ID
 * @param content 消息内容（文本内容或图片URL）
 * @param isSelf 是否为自己发送的消息
 * @param type 消息类型
 * @param senderName 发送者名称
 * @param senderAvatar 发送者头像URL
 * @param timestamp 消息时间戳（毫秒）
 */
data class ChatMessage(
    val id: String,
    val content: String,
    val isSelf: Boolean,
    val type: MessageType = MessageType.TEXT,
    val senderName: String = "",
    val senderAvatar: String = "",
    val timestamp: Long = 0L
)

/**
 * 聊天组件配置
 *
 * @param title 聊天标题
 * @param showNavigationBar 是否显示顶部导航栏
 * @param showBackButton 是否显示返回按钮
 * @param inputPlaceholder 输入框占位文本
 * @param selfAvatarUrl 自己的头像URL
 * @param primaryColor 主题色（十六进制整数，如 0xFF4F8FFF）
 * @param primaryGradientEndColor 主题渐变结束色
 * @param backgroundColor 聊天背景色
 */
data class ChatConfig(
    val title: String = "聊天",
    val showNavigationBar: Boolean = true,
    val showBackButton: Boolean = true,
    val inputPlaceholder: String = "输入消息...",
    val selfAvatarUrl: String = "",
    val primaryColor: Long = 0xFF4F8FFF,
    val primaryGradientEndColor: Long = 0xFF6C5CE7,
    val backgroundColor: Long = 0xFFF0F2F5
)

/**
 * 聊天消息工具类
 */
object ChatMessageHelper {
    private var messageIdCounter = 0

    /**
     * 生成消息唯一ID
     */
    fun generateId(): String {
        messageIdCounter++
        return "msg_${messageIdCounter}_${currentTimestamp()}"
    }

    /**
     * 获取当前时间戳（简单实现）
     */
    private fun currentTimestamp(): Long {
        return messageIdCounter.toLong()
    }

    /**
     * 创建文本消息
     */
    fun createTextMessage(
        content: String,
        isSelf: Boolean,
        senderName: String = "",
        senderAvatar: String = ""
    ): ChatMessage {
        return ChatMessage(
            id = generateId(),
            content = content,
            isSelf = isSelf,
            type = MessageType.TEXT,
            senderName = senderName,
            senderAvatar = senderAvatar,
            timestamp = 0L
        )
    }

    /**
     * 创建系统消息
     */
    fun createSystemMessage(content: String): ChatMessage {
        return ChatMessage(
            id = generateId(),
            content = content,
            isSelf = false,
            type = MessageType.SYSTEM,
            timestamp = 0L
        )
    }
}
