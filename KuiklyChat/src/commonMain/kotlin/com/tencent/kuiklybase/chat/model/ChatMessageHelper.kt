package com.tencent.kuiklybase.chat.model

object ChatMessageHelper {
    private var messageIdCounter = 0

    /**
     * 生成消息唯一ID（使用计数器 + 系统纳秒时间确保唯一性）
     *
     * 注意：在高并发场景中，建议业务方使用 UUID 或服务端生成的 ID。
     */
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
