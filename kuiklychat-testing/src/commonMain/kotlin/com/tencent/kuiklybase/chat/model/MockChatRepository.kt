package com.tencent.kuiklybase.chat.model

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
