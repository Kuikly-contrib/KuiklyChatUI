package com.tencent.kuiklybase.chat

/**
 * 聊天消息类型
 */
enum class MessageType {
    TEXT,       // 文本消息
    IMAGE,      // 图片消息
    VIDEO,      // 视频消息
    FILE,       // 文件消息
    SYSTEM,     // 系统消息（如时间提示、系统通知等）
    CUSTOM      // 自定义消息（由业务方通过 Slot 渲染）
}

/**
 * 消息发送状态
 */
enum class MessageStatus {
    SENDING,    // 发送中
    SENT,       // 已发送
    FAILED,     // 发送失败
    READ        // 已读
}

/**
 * 附件类型枚举
 */
enum class AttachmentType {
    IMAGE,          // 图片附件
    VIDEO,          // 视频附件
    FILE,           // 文件附件
    GIPHY,          // GIF 动图
    LINK_PREVIEW    // 链接预览
}

/**
 * 附件数据模型（对标 Stream Chat 的 Attachment）
 *
 * @param type 附件类型
 * @param url 附件资源 URL
 * @param title 附件标题（文件名等）
 * @param mimeType MIME 类型（如 "image/jpeg"、"video/mp4"）
 * @param fileSize 文件大小（字节）
 * @param width 宽度（图片/视频/GIF）
 * @param height 高度（图片/视频/GIF）
 * @param duration 时长（视频/音频，秒）
 * @param thumbnailUrl 缩略图 URL（视频/文件预览图）
 * @param extra 扩展数据
 */
data class Attachment(
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
 * 消息反应项（对标 Stream Chat 的 ReactionItem）
 *
 * @param type 反应类型标识（如 "like"、"love"、"😂"）
 * @param count 该反应的总数
 * @param isOwnReaction 当前用户是否已添加此反应
 */
data class ReactionItem(
    val type: String,
    val count: Int = 1,
    val isOwnReaction: Boolean = false
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
data class MessageAction(
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
object ChatMessageHelper {
    private var messageIdCounter = 0

    /**
     * 生成消息唯一ID（使用计数器 + 系统纳秒时间确保唯一性）
     *
     * 注意：在高并发场景中，建议业务方使用 UUID 或服务端生成的 ID。
     */
    @Synchronized
    fun generateId(): String {
        messageIdCounter++
        return "msg_${messageIdCounter}_${platformCurrentTimeMillis()}"
    }

    /**
     * 获取当前时间戳（毫秒）
     *
     * 在 KMP commonMain 中没有直接获取系统时间的 API，
     * 使用 messageIdCounter 作为备选。业务方应在创建消息时传入真实时间戳。
     */
    private fun platformCurrentTimeMillis(): Long {
        // 在真实项目中应使用 kotlinx-datetime: Clock.System.now().toEpochMilliseconds()
        // 此处使用递增计数保证唯一性
        return messageIdCounter.toLong()
    }

    /**
     * 创建文本消息
     */
    fun createTextMessage(
        content: String,
        isSelf: Boolean,
        senderName: String = "",
        senderAvatar: String = "",
        senderId: String = "",
        status: MessageStatus = MessageStatus.SENT,
        timestamp: Long = 0L,
        reactions: List<ReactionItem> = emptyList()
    ): ChatMessage {
        return ChatMessage(
            id = generateId(),
            content = content,
            isSelf = isSelf,
            type = MessageType.TEXT,
            status = status,
            senderName = senderName,
            senderAvatar = senderAvatar,
            senderId = senderId,
            timestamp = timestamp,
            reactions = reactions
        )
    }

    /**
     * 创建图片消息
     */
    fun createImageMessage(
        imageUrl: String,
        isSelf: Boolean,
        senderName: String = "",
        senderAvatar: String = "",
        senderId: String = "",
        width: Int = 0,
        height: Int = 0,
        timestamp: Long = 0L,
        reactions: List<ReactionItem> = emptyList()
    ): ChatMessage {
        return ChatMessage(
            id = generateId(),
            content = imageUrl,
            isSelf = isSelf,
            type = MessageType.IMAGE,
            senderName = senderName,
            senderAvatar = senderAvatar,
            senderId = senderId,
            timestamp = timestamp,
            extra = mapOf("width" to width.toString(), "height" to height.toString()),
            reactions = reactions
        )
    }

    /**
     * 创建视频消息
     */
    fun createVideoMessage(
        videoUrl: String,
        isSelf: Boolean,
        senderName: String = "",
        senderAvatar: String = "",
        senderId: String = "",
        thumbnailUrl: String = "",
        width: Int = 0,
        height: Int = 0,
        duration: Float = 0f,
        timestamp: Long = 0L
    ): ChatMessage {
        return ChatMessage(
            id = generateId(),
            content = videoUrl,
            isSelf = isSelf,
            type = MessageType.VIDEO,
            senderName = senderName,
            senderAvatar = senderAvatar,
            senderId = senderId,
            timestamp = timestamp,
            extra = mapOf(
                "width" to width.toString(),
                "height" to height.toString(),
                "duration" to duration.toString(),
                "thumbnailUrl" to thumbnailUrl
            ),
            attachments = listOf(
                Attachment(
                    type = AttachmentType.VIDEO,
                    url = videoUrl,
                    thumbnailUrl = thumbnailUrl,
                    width = width,
                    height = height,
                    duration = duration
                )
            )
        )
    }

    /**
     * 创建文件消息
     */
    fun createFileMessage(
        fileUrl: String,
        fileName: String,
        isSelf: Boolean,
        senderName: String = "",
        senderAvatar: String = "",
        senderId: String = "",
        mimeType: String = "",
        fileSize: Long = 0L,
        timestamp: Long = 0L
    ): ChatMessage {
        return ChatMessage(
            id = generateId(),
            content = fileName,
            isSelf = isSelf,
            type = MessageType.FILE,
            senderName = senderName,
            senderAvatar = senderAvatar,
            senderId = senderId,
            timestamp = timestamp,
            extra = mapOf(
                "fileUrl" to fileUrl,
                "fileName" to fileName,
                "mimeType" to mimeType,
                "fileSize" to fileSize.toString()
            ),
            attachments = listOf(
                Attachment(
                    type = AttachmentType.FILE,
                    url = fileUrl,
                    title = fileName,
                    mimeType = mimeType,
                    fileSize = fileSize
                )
            )
        )
    }

    /**
     * 创建系统消息
     */
    fun createSystemMessage(content: String, timestamp: Long = 0L): ChatMessage {
        return ChatMessage(
            id = generateId(),
            content = content,
            isSelf = false,
            type = MessageType.SYSTEM,
            timestamp = timestamp
        )
    }

    /**
     * 创建自定义消息
     */
    fun createCustomMessage(
        content: String,
        isSelf: Boolean,
        senderName: String = "",
        senderAvatar: String = "",
        senderId: String = "",
        extra: Map<String, String> = emptyMap(),
        timestamp: Long = 0L
    ): ChatMessage {
        return ChatMessage(
            id = generateId(),
            content = content,
            isSelf = isSelf,
            type = MessageType.CUSTOM,
            senderName = senderName,
            senderAvatar = senderAvatar,
            senderId = senderId,
            timestamp = timestamp,
            extra = extra
        )
    }

    /**
     * 计算消息分组信息
     *
     * 参考 Stream Chat 的 messagePositionHandler，根据前后消息判断当前消息
     * 是否为分组的第一条/最后一条，用于决定头像显示和间距。
     *
     * @param messages 消息列表
     * @param index 当前消息的索引
     * @param groupingInterval 分组间隔阈值（毫秒）
     */
    fun buildMessageContext(
        messages: List<ChatMessage>,
        index: Int,
        groupingInterval: Long = 2 * 60 * 1000L
    ): MessageContext {
        val message = messages[index]
        val prev = if (index > 0) messages[index - 1] else null
        val next = if (index < messages.size - 1) messages[index + 1] else null

        // 判断是否与上一条消息属于同一分组
        val sameGroupAsPrev = prev != null
                && prev.isSelf == message.isSelf
                && (if (message.senderId.isNotEmpty() && prev.senderId.isNotEmpty()) prev.senderId == message.senderId else prev.senderName == message.senderName)
                && prev.type != MessageType.SYSTEM
                && message.type != MessageType.SYSTEM
                && (message.timestamp - prev.timestamp < groupingInterval || message.timestamp == 0L || prev.timestamp == 0L)

        // 判断是否与下一条消息属于同一分组
        val sameGroupAsNext = next != null
                && next.isSelf == message.isSelf
                && (if (message.senderId.isNotEmpty() && next.senderId.isNotEmpty()) next.senderId == message.senderId else next.senderName == message.senderName)
                && next.type != MessageType.SYSTEM
                && message.type != MessageType.SYSTEM
                && (next.timestamp - message.timestamp < groupingInterval || message.timestamp == 0L || next.timestamp == 0L)

        return MessageContext(
            message = message,
            previousMessage = prev,
            nextMessage = next,
            index = index,
            isFirstInGroup = !sameGroupAsPrev,
            isLastInGroup = !sameGroupAsNext
        )
    }
}
