package com.tencent.kuiklybase.chat.model

import com.tencent.kuiklybase.chat.compose.core.ChatClock

public object ChatMessageHelper {
    private var messageIdCounter = 0

    /**
     * 生成消息唯一ID（使用计数器 + 系统纳秒时间确保唯一性）
     *
     * 注意：在高并发场景中，建议业务方使用 UUID 或服务端生成的 ID。
     */
    public fun generateId(): String {
        messageIdCounter++
        return "msg_${messageIdCounter}_${platformCurrentTimeMillis()}"
    }

    /**
     * 获取当前时间戳（毫秒）
     *
     * 在 KMP commonMain 中没有直接获取系统时间的 API，
     * 使用 messageIdCounter 作为备选。业务方应在创建消息时传入真实时间戳。
     */
    private fun platformCurrentTimeMillis(): Long = ChatClock.System.nowMillis()

    /**
     * 创建文本消息
     */
    public fun createTextMessage(
        content: String,
        isSelf: Boolean,
        senderName: String = "",
        senderAvatar: String = "",
        senderId: String = "",
        status: MessageStatus = MessageStatus.SENT,
        timestamp: Long = 0L,
        reactions: List<ReactionItem> = emptyList()
    ): ChatMessage {
        return sessionChatMessage(
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
        ).withCanonicalParts()
    }

    /**
     * 创建图片消息
     */
    public fun createImageMessage(
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
        return sessionChatMessage(
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
        ).withCanonicalParts()
    }

    /**
     * 创建视频消息
     */
    public fun createVideoMessage(
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
        return sessionChatMessage(
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
        ).withCanonicalParts()
    }

    /**
     * 创建文件消息
     */
    public fun createFileMessage(
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
        return sessionChatMessage(
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
        ).withCanonicalParts()
    }

    /**
     * 创建系统消息
     */
    public fun createSystemMessage(content: String, timestamp: Long = 0L): ChatMessage {
        return sessionChatMessage(
            id = generateId(),
            content = content,
            isSelf = false,
            type = MessageType.SYSTEM,
            timestamp = timestamp
        ).withCanonicalParts()
    }

    /**
     * 创建自定义消息
     */
    public fun createCustomMessage(
        content: String,
        isSelf: Boolean,
        senderName: String = "",
        senderAvatar: String = "",
        senderId: String = "",
        extra: Map<String, String> = emptyMap(),
        timestamp: Long = 0L
    ): ChatMessage {
        return sessionChatMessage(
            id = generateId(),
            content = content,
            isSelf = isSelf,
            type = MessageType.CUSTOM,
            senderName = senderName,
            senderAvatar = senderAvatar,
            senderId = senderId,
            timestamp = timestamp,
            extra = extra
        ).withCanonicalParts()
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
    public fun buildMessageContext(
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

    /**
     * 从输入框文本与已发送附件创建消息。
     *
     * - 纯单图：`IMAGE`，走原有图片气泡
     * - 文本 + 图/文件，或多图：`TEXT` 并写入 ordered [com.tencent.kuiklybase.chat.compose.core.MessagePart]
     */
    public fun createComposerMessage(
        text: String,
        attachments: List<Attachment> = emptyList(),
        isSelf: Boolean,
        senderName: String = "",
        senderAvatar: String = "",
        senderId: String = "",
        status: MessageStatus = MessageStatus.SENT,
        timestamp: Long = 0L,
        quotedMessage: ChatMessage? = null,
    ): ChatMessage {
        val trimmed = text.trim()
        val imageAttachments = attachments.filter { it.type == AttachmentType.IMAGE }
        val fileAttachments = attachments.filter { it.type == AttachmentType.FILE }
        val type = when {
            trimmed.isEmpty() && imageAttachments.size == 1 && fileAttachments.isEmpty() ->
                MessageType.IMAGE
            trimmed.isEmpty() && imageAttachments.isEmpty() && fileAttachments.size == 1 ->
                MessageType.FILE
            else -> MessageType.TEXT
        }
        val messageId = generateId()
        val explicitParts = buildMessageParts(
            messageId,
            if (type == MessageType.TEXT) trimmed else "",
            attachments,
        )
        val content = when (type) {
            MessageType.IMAGE -> imageAttachments.first().url
            MessageType.FILE -> fileAttachments.first().title.ifEmpty { trimmed }
            else -> trimmed
        }
        return sessionChatMessage(
            id = messageId,
            content = content,
            isSelf = isSelf,
            type = type,
            status = status,
            senderName = senderName,
            senderAvatar = senderAvatar,
            senderId = senderId,
            timestamp = timestamp,
            attachments = attachments,
            parts = explicitParts,
            quotedMessage = quotedMessage,
        ).withCanonicalParts()
    }

    public fun createAiStreamingPlaceholder(
        senderName: String,
        senderAvatar: String = "",
        senderId: String = "",
    ): ChatMessage {
        return sessionChatMessage(
            id = generateId(),
            content = "",
            isSelf = false,
            type = MessageType.TEXT,
            status = MessageStatus.STREAMING,
            senderName = senderName,
            senderAvatar = senderAvatar,
            senderId = senderId,
        ).withCanonicalParts()
    }

    /**
     * 将 AI 占位消息更新为 Markdown 流式正文。
     */
    public fun updateAiStreamingContent(
        message: ChatMessage,
        text: String,
        status: MessageStatus = MessageStatus.STREAMING,
    ): ChatMessage = message.copy(
        parts = listOf(
            com.tencent.kuiklybase.chat.compose.core.MessagePart.Text(
                value = text,
                format = com.tencent.kuiklybase.chat.compose.core.TextFormat.MARKDOWN,
            ),
        ),
        status = status,
    )

    /** Adapter SPI: ordered canonical parts without exposing [ChatMessage.parts]. */
    public fun orderedParts(message: ChatMessage): List<com.tencent.kuiklybase.chat.compose.core.MessagePart> {
        return message.resolveParts()
    }

    public fun updateContent(
        message: ChatMessage,
        content: String,
        status: MessageStatus = message.sessionStatus,
    ): ChatMessage = message.withUpdatedContent(content, status)

    /** Fill canonical parts when a session message was constructed without them. */
    public fun persistParts(message: ChatMessage): ChatMessage = message.withCanonicalParts()

    /** Wrap a canonical Compose/SDK message for Kuikly DSL session rendering. */
    public fun fromCanonical(
        payload: com.tencent.kuiklybase.chat.compose.core.ChatMessage,
        currentUserId: String,
    ): ChatMessage = payload.toSessionMessage(currentUserId)

    /** Replace extra metadata and rebuild ordered parts from the new envelope. */
    public fun withExtra(message: ChatMessage, extra: Map<String, String>): ChatMessage =
        message.copy(metadata = extra).withRebuiltParts()
}
