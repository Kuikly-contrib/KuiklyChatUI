package com.tencent.kuiklybase.chat.model

// ============================
// 预览/Mock 数据（参考 Stream Chat 的 stream-chat-android-previewdata 模块）
// ============================

/**
 * 预览数据集合
 *
 * 提供用于 Preview、测试和 Demo 的 Mock 数据。
 * 参考 Stream Chat 的 PreviewUserData / PreviewMessageData / PreviewChannelData。
 *
 * 使用方式：先显式依赖 `:kuiklychat-testing`（不随伞形 `KuiklyChat` 传递），再：
 * ```kotlin
 * val messages = ChatPreviewData.messages()
 * val channels = ChatPreviewData.channels()
 * ```
 *
 * Preview / Mock 不是产品 SDK keep-set，随时可改，不要当 Stable 冻结。
 */
public object ChatPreviewData {

    // ============================
    // 用户数据
    // ============================

    /** 当前用户（自己） */
    public val currentUser: ChatChannelMember = ChatChannelMember(
        id = "user_self",
        name = "我",
        avatarUrl = "https://vfiles.gtimg.cn/wuji_dashboard/wupload/xy/starter/62394e19.png",
        role = MemberRole.MEMBER,
        onlineStatus = OnlineStatus.ONLINE
    )

    /** 预览用户列表 */
    public val users: List<ChatChannelMember> = listOf(
        ChatChannelMember(
            id = "user_1",
            name = "张三",
            avatarUrl = "https://randomuser.me/api/portraits/men/1.jpg",
            role = MemberRole.MEMBER,
            onlineStatus = OnlineStatus.ONLINE
        ),
        ChatChannelMember(
            id = "user_2",
            name = "李四",
            avatarUrl = "https://randomuser.me/api/portraits/women/2.jpg",
            role = MemberRole.ADMIN,
            onlineStatus = OnlineStatus.OFFLINE
        ),
        ChatChannelMember(
            id = "user_3",
            name = "王五",
            avatarUrl = "https://randomuser.me/api/portraits/men/3.jpg",
            role = MemberRole.MEMBER,
            onlineStatus = OnlineStatus.AWAY
        ),
        ChatChannelMember(
            id = "user_4",
            name = "赵六",
            avatarUrl = "https://randomuser.me/api/portraits/women/4.jpg",
            role = MemberRole.MEMBER,
            onlineStatus = OnlineStatus.BUSY
        ),
        ChatChannelMember(
            id = "user_5",
            name = "孙七",
            avatarUrl = "https://randomuser.me/api/portraits/men/5.jpg",
            role = MemberRole.OWNER,
            onlineStatus = OnlineStatus.ONLINE
        )
    )

    // ============================
    // 消息数据
    // ============================

    /**
     * 生成预览消息列表
     *
     * @param count 消息数量
     * @param channelType 频道类型（影响是否显示发送者名称）
     * @return 消息列表
     */
    public fun messages(count: Int = 10, channelType: ChannelType = ChannelType.GROUP): List<ChatMessage> {
        val baseTimestamp = 1700000000000L // 2023-11-14 基准时间
        val sampleMessages = listOf(
            "你好！最近怎么样？",
            "挺好的，谢谢关心 😊",
            "周末有什么计划吗？",
            "打算去爬山，你要一起吗？",
            "好啊！几点出发？",
            "早上8点在地铁站集合吧",
            "没问题，我准时到",
            "记得带水和零食 🎒",
            "好的，还需要带什么？",
            "防晒霜和帽子也带上吧，天气预报说明天很晒 ☀️",
            "了解，那我们明天见！",
            "明天见 👋",
            "对了，你看到群里发的那个链接了吗？",
            "看到了，挺有意思的",
            "我觉得我们可以试试那个方案"
        )

        return (0 until count).map { index ->
            val isSelf = index % 3 == 0 // 每3条消息中有1条是自己的
            val user = if (isSelf) currentUser else users[index % users.size]
            val timestamp = baseTimestamp + index * 60 * 1000L // 每条消息间隔1分钟

            sessionChatMessage(
                id = "preview_msg_$index",
                content = sampleMessages[index % sampleMessages.size],
                isSelf = isSelf,
                type = MessageType.TEXT,
                status = if (isSelf) MessageStatus.SENT else MessageStatus.SENT,
                senderName = user.name,
                senderAvatar = user.avatarUrl,
                senderId = user.id,
                timestamp = timestamp,
                reactions = if (index % 5 == 0) listOf(
                    ReactionItem("👍", 3, isSelf),
                    ReactionItem("❤️", 1, false)
                ) else emptyList()
            )
        }
    }

    /**
     * 生成包含多种消息类型的预览消息列表
     */
    public fun mixedMessages(): List<ChatMessage> {
        val baseTimestamp = 1700000000000L
        return listOf(
            // 系统消息
            ChatMessageHelper.createSystemMessage(
                content = "张三 加入了群聊",
                timestamp = baseTimestamp
            ),
            // 文本消息
            sessionChatMessage(
                id = "mix_1",
                content = "大家好！",
                isSelf = false,
                type = MessageType.TEXT,
                senderName = "张三",
                senderAvatar = users[0].avatarUrl,
                senderId = "user_1",
                timestamp = baseTimestamp + 60000
            ),
            // 图片消息
            sessionChatMessage(
                id = "mix_2",
                content = "https://picsum.photos/400/300",
                isSelf = false,
                type = MessageType.IMAGE,
                senderName = "李四",
                senderAvatar = users[1].avatarUrl,
                senderId = "user_2",
                timestamp = baseTimestamp + 120000,
                extra = mapOf("width" to "400", "height" to "300")
            ),
            // 自己的文本消息
            sessionChatMessage(
                id = "mix_3",
                content = "这张照片拍得真好！👍",
                isSelf = true,
                type = MessageType.TEXT,
                senderName = currentUser.name,
                senderAvatar = currentUser.avatarUrl,
                senderId = currentUser.id,
                timestamp = baseTimestamp + 180000,
                status = MessageStatus.SENT,
                readBy = listOf(currentUser.id),
            ),
            // 引用回复消息
            sessionChatMessage(
                id = "mix_4",
                content = "确实很漂亮",
                isSelf = false,
                type = MessageType.TEXT,
                senderName = "王五",
                senderAvatar = users[2].avatarUrl,
                senderId = "user_3",
                timestamp = baseTimestamp + 240000,
                quotedMessage = sessionChatMessage(
                    id = "mix_2",
                    content = "[图片]",
                    isSelf = false,
                    senderName = "李四"
                )
            ),
            // 文件消息
            sessionChatMessage(
                id = "mix_5",
                content = "项目方案.pdf",
                isSelf = true,
                type = MessageType.FILE,
                senderName = currentUser.name,
                senderAvatar = currentUser.avatarUrl,
                senderId = currentUser.id,
                timestamp = baseTimestamp + 300000,
                extra = mapOf(
                    "fileUrl" to "https://example.com/doc.pdf",
                    "fileName" to "项目方案.pdf",
                    "mimeType" to "application/pdf",
                    "fileSize" to "2048000"
                ),
                attachments = listOf(
                    Attachment(
                        type = AttachmentType.FILE,
                        url = "https://example.com/doc.pdf",
                        title = "项目方案.pdf",
                        mimeType = "application/pdf",
                        fileSize = 2048000
                    )
                )
            ),
            // 发送中的消息
            sessionChatMessage(
                id = "mix_6",
                content = "这个方案大家看看",
                isSelf = true,
                type = MessageType.TEXT,
                senderName = currentUser.name,
                senderAvatar = currentUser.avatarUrl,
                senderId = currentUser.id,
                timestamp = baseTimestamp + 360000,
                status = MessageStatus.SENDING
            )
        )
    }

    // ============================
    // 频道数据
    // ============================

    /**
     * 生成预览频道列表
     */
    public fun channels(): List<ChatChannel> {
        val baseTimestamp = 1700000000000L
        return listOf(
            ChatChannel(
                id = "ch_1",
                type = ChannelType.DIRECT,
                name = "张三",
                avatarUrl = users[0].avatarUrl,
                lastMessage = sessionChatMessage(
                    id = "last_1",
                    content = "明天见！",
                    isSelf = false,
                    senderName = "张三",
                    timestamp = baseTimestamp + 600000
                ),
                lastMessageAt = baseTimestamp + 600000,
                unreadCount = 3,
                memberCount = 2,
                members = listOf(users[0])
            ),
            ChatChannel(
                id = "ch_2",
                type = ChannelType.GROUP,
                name = "项目讨论组",
                avatarUrl = "",
                lastMessage = sessionChatMessage(
                    id = "last_2",
                    content = "方案已经更新了",
                    isSelf = true,
                    senderName = currentUser.name,
                    timestamp = baseTimestamp + 500000
                ),
                lastMessageAt = baseTimestamp + 500000,
                unreadCount = 0,
                memberCount = 5,
                members = users.take(4),
                isPinned = true
            ),
            ChatChannel(
                id = "ch_3",
                type = ChannelType.DIRECT,
                name = "李四",
                avatarUrl = users[1].avatarUrl,
                lastMessage = sessionChatMessage(
                    id = "last_3",
                    content = "[图片]",
                    isSelf = false,
                    type = MessageType.IMAGE,
                    senderName = "李四",
                    timestamp = baseTimestamp + 400000
                ),
                lastMessageAt = baseTimestamp + 400000,
                unreadCount = 1,
                memberCount = 2,
                members = listOf(users[1])
            ),
            ChatChannel(
                id = "ch_4",
                type = ChannelType.GROUP,
                name = "周末爬山群",
                avatarUrl = "",
                lastMessage = sessionChatMessage(
                    id = "last_4",
                    content = "记得带水和零食",
                    isSelf = false,
                    senderName = "王五",
                    timestamp = baseTimestamp + 300000
                ),
                lastMessageAt = baseTimestamp + 300000,
                unreadCount = 12,
                memberCount = 8,
                members = users
            ),
            ChatChannel(
                id = "ch_5",
                type = ChannelType.CHANNEL,
                name = "技术分享频道",
                avatarUrl = "",
                lastMessage = sessionChatMessage(
                    id = "last_5",
                    content = "今天分享 Kotlin Multiplatform 的最佳实践",
                    isSelf = false,
                    senderName = "孙七",
                    timestamp = baseTimestamp + 200000
                ),
                lastMessageAt = baseTimestamp + 200000,
                unreadCount = 0,
                memberCount = 50,
                isMuted = true
            ),
            ChatChannel(
                id = "ch_6",
                type = ChannelType.DIRECT,
                name = "赵六",
                avatarUrl = users[3].avatarUrl,
                lastMessage = sessionChatMessage(
                    id = "last_6",
                    content = "好的，收到",
                    isSelf = true,
                    senderName = currentUser.name,
                    timestamp = baseTimestamp + 100000
                ),
                lastMessageAt = baseTimestamp + 100000,
                unreadCount = 0,
                memberCount = 2,
                members = listOf(users[3])
            )
        )
    }
}

// ============================
// Mock Repository 实现
// ============================

/**
 * Mock 数据仓库实现
 *
 * 使用 ChatPreviewData 提供的静态数据，用于 Demo 和测试。
 * 业务方可参考此实现来接入自己的后端。
 */
public class MockChatRepository : ChatRepository {

    private val previewChannels = ChatPreviewData.channels().toMutableList()
    private val previewMessages = mutableMapOf<String, MutableList<ChatMessage>>()

    init {
        // 为每个频道生成预览消息
        previewChannels.forEach { channel ->
            previewMessages[channel.id] = ChatPreviewData.messages(15).toMutableList()
        }
    }

    override suspend fun getChannels(limit: Int, offset: Int): ChatChannelsResponse {
        val result = previewChannels.drop(offset).take(limit)
        return ChatChannelsResponse(
            channels = result,
            hasMore = offset + limit < previewChannels.size
        )
    }

    override suspend fun getChannel(channelId: String): ChatChannel? {
        return previewChannels.find { it.id == channelId }
    }

    override suspend fun getMessages(
        channelId: String,
        limit: Int,
        before: String?
    ): ChatMessagesResponse {
        val messages = previewMessages[channelId] ?: emptyList()
        val startIndex = if (before != null) {
            val idx = messages.indexOfFirst { it.id == before }
            if (idx > 0) maxOf(0, idx - limit) else 0
        } else {
            maxOf(0, messages.size - limit)
        }
        val result = messages.subList(startIndex, minOf(startIndex + limit, messages.size))
        return ChatMessagesResponse(
            messages = result,
            hasMore = startIndex > 0
        )
    }

    override suspend fun sendMessage(channelId: String, message: ChatMessage): ChatMessage {
        val sentMessage = message.copy(status = MessageStatus.SENT)
        previewMessages.getOrPut(channelId) { mutableListOf() }.add(sentMessage)
        return sentMessage
    }

    override suspend fun editMessage(messageId: String, newContent: String): ChatMessage {
        previewMessages.values.forEach { messages ->
            val index = messages.indexOfFirst { it.id == messageId }
            if (index >= 0) {
                val edited = ChatMessageHelper.updateContent(messages[index], newContent)
                    .copy(isEdited = true)
                messages[index] = edited
                return edited
            }
        }
        throw IllegalArgumentException("Message not found: $messageId")
    }

    override suspend fun deleteMessage(messageId: String): Boolean {
        previewMessages.values.forEach { messages ->
            val index = messages.indexOfFirst { it.id == messageId }
            if (index >= 0) {
                messages[index] = messages[index].copy(isDeleted = true)
                return true
            }
        }
        return false
    }

    override suspend fun toggleReaction(messageId: String, reactionType: String): ChatMessage {
        previewMessages.values.forEach { messages ->
            val index = messages.indexOfFirst { it.id == messageId }
            if (index >= 0) {
                val msg = messages[index]
                val existingReaction = msg.reactions.find { it.type == reactionType }
                val newReactions = if (existingReaction?.isOwnReaction == true) {
                    msg.reactions.filter { it.type != reactionType }
                } else {
                    msg.reactions + ReactionItem(reactionType, 1, true)
                }
                val updated = msg.copy(reactions = newReactions)
                messages[index] = updated
                return updated
            }
        }
        throw IllegalArgumentException("Message not found: $messageId")
    }

    override suspend fun pinMessage(messageId: String, pinned: Boolean): ChatMessage {
        previewMessages.values.forEach { messages ->
            val index = messages.indexOfFirst { it.id == messageId }
            if (index >= 0) {
                val updated = messages[index].copy(isPinned = pinned)
                messages[index] = updated
                return updated
            }
        }
        throw IllegalArgumentException("Message not found: $messageId")
    }

    override suspend fun sendTypingEvent(channelId: String) {
        // Mock: 不做任何操作
    }

    override suspend fun getTypingUsers(channelId: String): List<String> {
        // Mock: 返回空列表
        return emptyList()
    }

    override suspend fun markChannelRead(channelId: String) {
        val index = previewChannels.indexOfFirst { it.id == channelId }
        if (index >= 0) {
            previewChannels[index] = previewChannels[index].copy(unreadCount = 0)
        }
    }

    override suspend fun muteChannel(channelId: String, muted: Boolean) {
        val index = previewChannels.indexOfFirst { it.id == channelId }
        if (index >= 0) {
            previewChannels[index] = previewChannels[index].copy(isMuted = muted)
        }
    }

    override suspend fun pinChannel(channelId: String, pinned: Boolean) {
        val index = previewChannels.indexOfFirst { it.id == channelId }
        if (index >= 0) {
            previewChannels[index] = previewChannels[index].copy(isPinned = pinned)
        }
    }

    override suspend fun deleteChannel(channelId: String): Boolean {
        return previewChannels.removeAll { it.id == channelId }
    }
}
