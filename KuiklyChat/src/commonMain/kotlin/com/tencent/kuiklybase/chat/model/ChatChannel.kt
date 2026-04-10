package com.tencent.kuiklybase.chat.model

/**
 * 频道/会话类型
 */
enum class ChannelType {
    /** 单聊（1v1） */
    DIRECT,
    /** 群聊 */
    GROUP,
    /** 频道（类似 Discord/Slack 的公开频道） */
    CHANNEL,
    /** 自定义类型 */
    CUSTOM
}

/**
 * 频道成员角色
 */
enum class MemberRole {
    /** 拥有者 */
    OWNER,
    /** 管理员 */
    ADMIN,
    /** 普通成员 */
    MEMBER,
    /** 访客（只读） */
    GUEST
}

/**
 * 频道成员在线状态
 */
enum class OnlineStatus {
    /** 在线 */
    ONLINE,
    /** 离线 */
    OFFLINE,
    /** 忙碌 */
    BUSY,
    /** 离开 */
    AWAY
}

/**
 * 频道成员数据模型
 *
 * @param id 成员唯一 ID
 * @param name 成员显示名称
 * @param avatarUrl 成员头像 URL
 * @param role 成员角色
 * @param onlineStatus 在线状态
 * @param lastActiveAt 最后活跃时间戳（毫秒）
 * @param extra 扩展数据
 */
data class ChatChannelMember(
    val id: String,
    val name: String,
    val avatarUrl: String = "",
    val role: MemberRole = MemberRole.MEMBER,
    val onlineStatus: OnlineStatus = OnlineStatus.OFFLINE,
    val lastActiveAt: Long = 0L,
    val extra: Map<String, String> = emptyMap()
)

/**
 * 频道/会话数据模型（对标 Stream Chat 的 Channel）
 *
 * 代表一个聊天频道或会话，用于频道列表展示。
 *
 * @param id 频道唯一 ID
 * @param type 频道类型
 * @param name 频道名称（群聊名、频道名；单聊时为对方昵称）
 * @param avatarUrl 频道头像 URL（群聊头像；单聊时为对方头像）
 * @param lastMessage 最后一条消息（用于列表预览）
 * @param lastMessageAt 最后消息时间戳（毫秒，用于排序）
 * @param unreadCount 未读消息数
 * @param isMuted 是否已静音
 * @param isPinned 是否已置顶
 * @param memberCount 成员总数
 * @param members 成员列表（可选，按需加载）
 * @param createdAt 频道创建时间戳（毫秒）
 * @param updatedAt 频道更新时间戳（毫秒）
 * @param extra 扩展数据（业务方自定义字段）
 */
data class ChatChannel(
    val id: String,
    val type: ChannelType = ChannelType.DIRECT,
    val name: String = "",
    val avatarUrl: String = "",
    val lastMessage: ChatMessage? = null,
    val lastMessageAt: Long = 0L,
    val unreadCount: Int = 0,
    val isMuted: Boolean = false,
    val isPinned: Boolean = false,
    val memberCount: Int = 0,
    val members: List<ChatChannelMember> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val extra: Map<String, String> = emptyMap()
)

// ============================
// 工具类
// ============================

/**
 * 频道工具类
 */
object ChatChannelHelper {
    private var channelIdCounter = 0

    /**
     * 生成频道唯一 ID
     */
    fun generateId(): String {
        channelIdCounter++
        return "channel_${channelIdCounter}"
    }

    /**
     * 创建单聊频道
     */
    fun createDirectChannel(
        otherUserName: String,
        otherUserAvatar: String = "",
        otherUserId: String = "",
        lastMessage: ChatMessage? = null,
        lastMessageAt: Long = 0L,
        unreadCount: Int = 0
    ): ChatChannel {
        return ChatChannel(
            id = generateId(),
            type = ChannelType.DIRECT,
            name = otherUserName,
            avatarUrl = otherUserAvatar,
            lastMessage = lastMessage,
            lastMessageAt = lastMessageAt,
            unreadCount = unreadCount,
            memberCount = 2,
            members = listOf(
                ChatChannelMember(id = otherUserId, name = otherUserName, avatarUrl = otherUserAvatar)
            )
        )
    }

    /**
     * 创建群聊频道
     */
    fun createGroupChannel(
        name: String,
        avatarUrl: String = "",
        members: List<ChatChannelMember> = emptyList(),
        lastMessage: ChatMessage? = null,
        lastMessageAt: Long = 0L,
        unreadCount: Int = 0
    ): ChatChannel {
        return ChatChannel(
            id = generateId(),
            type = ChannelType.GROUP,
            name = name,
            avatarUrl = avatarUrl,
            lastMessage = lastMessage,
            lastMessageAt = lastMessageAt,
            unreadCount = unreadCount,
            memberCount = members.size,
            members = members
        )
    }

    /**
     * 创建公开频道
     */
    fun createPublicChannel(
        name: String,
        avatarUrl: String = "",
        memberCount: Int = 0,
        lastMessage: ChatMessage? = null,
        lastMessageAt: Long = 0L,
        unreadCount: Int = 0
    ): ChatChannel {
        return ChatChannel(
            id = generateId(),
            type = ChannelType.CHANNEL,
            name = name,
            avatarUrl = avatarUrl,
            lastMessage = lastMessage,
            lastMessageAt = lastMessageAt,
            unreadCount = unreadCount,
            memberCount = memberCount
        )
    }

    /**
     * 获取频道最后消息的预览文本
     *
     * @param channel 频道
     * @param maxLength 最大字符数
     */
    fun getLastMessagePreview(channel: ChatChannel, maxLength: Int = 30): String {
        val msg = channel.lastMessage ?: return ""
        val prefix = if (channel.type != ChannelType.DIRECT && !msg.isSelf && msg.senderName.isNotEmpty()) {
            "${msg.senderName}: "
        } else {
            ""
        }
        val content = when (msg.type) {
            MessageType.TEXT -> msg.content
            MessageType.IMAGE -> "[图片]"
            MessageType.VIDEO -> "[视频]"
            MessageType.FILE -> "[文件]"
            MessageType.SYSTEM -> msg.content
            MessageType.CUSTOM -> "[自定义消息]"
        }
        val full = "$prefix$content"
        return if (full.length > maxLength) full.substring(0, maxLength) + "..." else full
    }

    /**
     * 格式化频道最后消息时间
     *
     * @param timestamp 时间戳（毫秒）
     * @return 格式化后的时间字符串（简化实现）
     */
    fun formatLastMessageTime(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        // 简化实现：在真实项目中应使用 kotlinx-datetime 进行完整的时间格式化
        // 此处返回占位文本，业务方可通过 timeFormatter 自定义
        return "刚刚"
    }
}
