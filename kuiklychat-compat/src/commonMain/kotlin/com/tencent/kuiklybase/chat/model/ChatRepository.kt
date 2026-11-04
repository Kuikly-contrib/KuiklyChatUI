package com.tencent.kuiklybase.chat.model

// ============================
// 数据仓库接口（参考 Stream Chat 的数据层解耦设计）
// ============================

/**
 * API 响应封装
 *
 * @param messages 消息列表
 * @param hasMore 是否还有更多数据
 */
public data class ChatMessagesResponse(
    val messages: List<ChatMessage>,
    val hasMore: Boolean = true
)

/**
 * 频道列表 API 响应封装
 *
 * @param channels 频道列表
 * @param hasMore 是否还有更多数据
 */
public data class ChatChannelsResponse(
    val channels: List<ChatChannel>,
    val hasMore: Boolean = true
)

/**
 * 聊天数据仓库接口（参考 Stream Chat 的 ChatClient 解耦设计）
 *
 * 定义聊天组件所需的所有数据操作接口，使组件库不绑定任何具体后端。
 * 业务方通过实现此接口来接入自己的数据源（REST API、WebSocket、Firebase、本地数据库等）。
 *
 * **这是 Stream Chat 做得不好的地方的改进**：Stream 强绑定了自己的 ChatClient，
 * 而我们通过接口抽象，让组件库可以接入任何后端。
 *
 * 使用方式：
 * ```kotlin
 * // 实现自定义数据源
 * class MyApiChatRepository : ChatRepository {
 *     override suspend fun getChannels(limit, offset) = ...
 *     override suspend fun getMessages(channelId, limit, before) = ...
 *     override suspend fun sendMessage(channelId, message) = ...
 * }
 *
 * // 在 Demo 中使用
 * val repository: ChatRepository = MyApiChatRepository()
 * val channels = repository.getChannels()
 * ```
 */
public interface ChatRepository {

    // ============================
    // 频道操作
    // ============================

    /**
     * 获取频道列表
     *
     * @param limit 每页数量
     * @param offset 偏移量（用于分页）
     * @return 频道列表响应
     */
    public suspend fun getChannels(
        limit: Int = 20,
        offset: Int = 0
    ): ChatChannelsResponse

    /**
     * 获取单个频道详情
     *
     * @param channelId 频道 ID
     * @return 频道数据（null 表示不存在）
     */
    public suspend fun getChannel(channelId: String): ChatChannel?

    // ============================
    // 消息操作
    // ============================

    /**
     * 获取指定频道的消息列表
     *
     * @param channelId 频道 ID
     * @param limit 每页数量
     * @param before 加载此消息 ID 之前的消息（用于加载历史消息）
     * @return 消息列表响应
     */
    public suspend fun getMessages(
        channelId: String,
        limit: Int = 30,
        before: String? = null
    ): ChatMessagesResponse

    /**
     * 发送消息
     *
     * @param channelId 频道 ID
     * @param message 要发送的消息
     * @return 发送成功后的消息（可能包含服务端生成的 ID、时间戳等）
     */
    public suspend fun sendMessage(
        channelId: String,
        message: ChatMessage
    ): ChatMessage

    /**
     * 编辑消息
     *
     * @param messageId 消息 ID
     * @param newContent 新的消息内容
     * @return 编辑后的消息
     */
    public suspend fun editMessage(
        messageId: String,
        newContent: String
    ): ChatMessage

    /**
     * 删除消息（软删除）
     *
     * @param messageId 消息 ID
     * @return 是否删除成功
     */
    public suspend fun deleteMessage(messageId: String): Boolean

    // ============================
    // 反应操作
    // ============================

    /**
     * 添加/移除消息反应
     *
     * @param messageId 消息 ID
     * @param reactionType 反应类型（如 "like"、"love"、"😂"）
     * @return 更新后的消息
     */
    public suspend fun toggleReaction(
        messageId: String,
        reactionType: String
    ): ChatMessage

    // ============================
    // 置顶操作
    // ============================

    /**
     * 置顶/取消置顶消息
     *
     * @param messageId 消息 ID
     * @param pinned 是否置顶
     * @return 更新后的消息
     */
    public suspend fun pinMessage(
        messageId: String,
        pinned: Boolean
    ): ChatMessage

    // ============================
    // 输入状态
    // ============================

    /**
     * 发送正在输入事件
     *
     * @param channelId 频道 ID
     */
    public suspend fun sendTypingEvent(channelId: String)

    /**
     * 获取正在输入的用户列表
     *
     * @param channelId 频道 ID
     * @return 正在输入的用户名列表
     */
    public suspend fun getTypingUsers(channelId: String): List<String>

    // ============================
    // 频道状态操作
    // ============================

    /**
     * 标记频道已读
     *
     * @param channelId 频道 ID
     */
    public suspend fun markChannelRead(channelId: String)

    /**
     * 静音/取消静音频道
     *
     * @param channelId 频道 ID
     * @param muted 是否静音
     */
    public suspend fun muteChannel(channelId: String, muted: Boolean)

    /**
     * 置顶/取消置顶频道
     *
     * @param channelId 频道 ID
     * @param pinned 是否置顶
     */
    public suspend fun pinChannel(channelId: String, pinned: Boolean)

    /**
     * 删除频道
     *
     * @param channelId 频道 ID
     * @return 是否删除成功
     */
    public suspend fun deleteChannel(channelId: String): Boolean
}
