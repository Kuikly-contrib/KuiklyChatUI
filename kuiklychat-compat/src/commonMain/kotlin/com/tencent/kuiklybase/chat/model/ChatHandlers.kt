package com.tencent.kuiklybase.chat.model

// ============================
// Handler / Formatter 接口（参考 Stream Chat 的可替换处理器设计）
// ============================

/**
 * 消息位置枚举
 *
 * 描述消息在分组中的位置，用于决定头像显示、间距等。
 */
public enum class MessagePosition {
    /** 分组中的唯一消息（既是第一条也是最后一条） */
    SINGLE,
    /** 分组中的第一条消息 */
    TOP,
    /** 分组中的中间消息 */
    MIDDLE,
    /** 分组中的最后一条消息 */
    BOTTOM
}

/**
 * 日期分隔符处理器接口（参考 Stream Chat 的 DateSeparatorHandler）
 *
 * 决定两条消息之间是否需要插入日期分隔符，以及分隔符的文本内容。
 * 用户可替换此接口来自定义日期分隔逻辑。
 *
 * 使用方式：
 * ```kotlin
 * config.dateSeparatorHandler = object : DateSeparatorHandler {
 *     override fun shouldAddDateSeparator(prevTimestamp, currentTimestamp, interval) = ...
 *     override fun formatDate(timestamp) = ...
 * }
 * ```
 */
public interface DateSeparatorHandler {
    /**
     * 判断是否需要在两条消息之间插入日期分隔符
     *
     * @param previousTimestamp 上一条消息的时间戳（毫秒），首条消息时为 0
     * @param currentTimestamp 当前消息的时间戳（毫秒）
     * @param interval 时间间隔阈值（毫秒）
     * @return 是否需要插入分隔符
     */
    public fun shouldAddDateSeparator(
        previousTimestamp: Long,
        currentTimestamp: Long,
        interval: Long
    ): Boolean

    /**
     * 格式化日期分隔符的显示文本
     *
     * @param timestamp 时间戳（毫秒）
     * @return 格式化后的日期字符串
     */
    public fun formatDate(timestamp: Long): String
}

/**
 * 消息分组处理器接口（参考 Stream Chat 的 MessagePositionHandler）
 *
 * 决定消息在分组中的位置，用于控制头像显示、间距缩小等。
 * 用户可替换此接口来自定义分组策略。
 *
 * 使用方式：
 * ```kotlin
 * config.messagePositionHandler = object : MessagePositionHandler {
 *     override fun handleMessagePosition(prev, current, next, interval) = ...
 * }
 * ```
 */
public interface MessagePositionHandler {
    /**
     * 判断消息在分组中的位置
     *
     * @param previousMessage 上一条消息（首条消息时为 null）
     * @param currentMessage 当前消息
     * @param nextMessage 下一条消息（末条消息时为 null）
     * @param groupingInterval 分组间隔阈值（毫秒）
     * @return 消息在分组中的位置
     */
    public fun handleMessagePosition(
        previousMessage: ChatMessage?,
        currentMessage: ChatMessage,
        nextMessage: ChatMessage?,
        groupingInterval: Long
    ): MessagePosition
}

/**
 * 频道名称格式化器接口（参考 Stream Chat 的 ChannelNameFormatter）
 *
 * 用于自定义频道在列表中显示的名称。
 *
 * 使用方式：
 * ```kotlin
 * config.channelNameFormatter = ChannelNameFormatter { channel ->
 *     if (channel.type == ChannelType.GROUP) "群聊: ${channel.name}" else channel.name
 * }
 * ```
 */
public fun interface ChannelNameFormatter {
    /**
     * 格式化频道名称
     *
     * @param channel 频道数据
     * @return 格式化后的频道名称
     */
    public fun formatChannelName(channel: ChatChannel): String
}

/**
 * 消息预览文本格式化器接口（参考 Stream Chat 的 MessagePreviewFormatter）
 *
 * 用于自定义频道列表中最后一条消息的预览文本。
 *
 * 使用方式：
 * ```kotlin
 * config.messagePreviewFormatter = MessagePreviewFormatter { channel, maxLength ->
 *     // 自定义预览文本逻辑
 * }
 * ```
 */
public fun interface MessagePreviewFormatter {
    /**
     * 格式化消息预览文本
     *
     * @param channel 频道数据（包含 lastMessage）
     * @param maxLength 最大字符数
     * @return 格式化后的预览文本
     */
    public fun formatPreview(channel: ChatChannel, maxLength: Int): String
}

/**
 * 消息文本格式化器接口（参考 Stream Chat 的 MessageTextFormatter）
 *
 * 用于自定义消息内容的文本渲染（如 Markdown 解析、@提及高亮等）。
 *
 * 使用方式：
 * ```kotlin
 * config.messageTextFormatter = MessageTextFormatter { message ->
 *     // 自定义文本格式化逻辑
 * }
 * ```
 */
public fun interface MessageTextFormatter {
    /**
     * 格式化消息文本
     *
     * @param message 消息数据
     * @return 格式化后的文本
     */
    public fun formatText(message: ChatMessage): String
}

/**
 * 时间戳格式化器接口（参考 Stream Chat 的 TimestampFormatter）
 *
 * 用于自定义频道列表中时间的显示格式。
 *
 * 使用方式：
 * ```kotlin
 * config.timestampFormatter = TimestampFormatter { timestamp ->
 *     // 自定义时间格式化
 * }
 * ```
 */
public fun interface TimestampFormatter {
    /**
     * 格式化时间戳
     *
     * @param timestamp 时间戳（毫秒）
     * @return 格式化后的时间字符串
     */
    public fun formatTimestamp(timestamp: Long): String
}

// ============================
// 默认实现
// ============================

/**
 * 默认日期分隔符处理器
 *
 * 当两条消息的时间间隔超过阈值时插入分隔符。
 * 日期格式化使用简化实现（在真实项目中应使用 kotlinx-datetime）。
 */
public class DefaultDateSeparatorHandler : DateSeparatorHandler {
    override fun shouldAddDateSeparator(
        previousTimestamp: Long,
        currentTimestamp: Long,
        interval: Long
    ): Boolean {
        if (currentTimestamp <= 0L) return false
        if (previousTimestamp <= 0L) return true
        return (currentTimestamp - previousTimestamp) >= interval
    }

    override fun formatDate(timestamp: Long): String {
        // 简化实现：返回时间戳的可读表示
        // 在真实项目中应使用 kotlinx-datetime 进行完整的时间格式化
        if (timestamp <= 0L) return ""
        // 使用简单的相对时间表示
        return formatTimestampSimple(timestamp)
    }
}

/**
 * 默认消息分组处理器
 *
 * 同一发送者在指定时间间隔内的连续消息归为一组。
 * 系统消息不参与分组。
 */
public class DefaultMessagePositionHandler : MessagePositionHandler {
    override fun handleMessagePosition(
        previousMessage: ChatMessage?,
        currentMessage: ChatMessage,
        nextMessage: ChatMessage?,
        groupingInterval: Long
    ): MessagePosition {
        // 系统消息不参与分组
        if (currentMessage.type == MessageType.SYSTEM) return MessagePosition.SINGLE

        val sameGroupAsPrev = isSameGroup(previousMessage, currentMessage, groupingInterval)
        val sameGroupAsNext = isSameGroup(currentMessage, nextMessage, groupingInterval)

        return when {
            !sameGroupAsPrev && !sameGroupAsNext -> MessagePosition.SINGLE
            !sameGroupAsPrev && sameGroupAsNext -> MessagePosition.TOP
            sameGroupAsPrev && sameGroupAsNext -> MessagePosition.MIDDLE
            sameGroupAsPrev && !sameGroupAsNext -> MessagePosition.BOTTOM
            else -> MessagePosition.SINGLE
        }
    }

    private fun isSameGroup(first: ChatMessage?, second: ChatMessage?, interval: Long): Boolean {
        if (first == null || second == null) return false
        if (first.type == MessageType.SYSTEM || second.type == MessageType.SYSTEM) return false
        if (first.isSelf != second.isSelf) return false

        // 判断是否同一发送者
        val sameSender = if (first.senderId.isNotEmpty() && second.senderId.isNotEmpty()) {
            first.senderId == second.senderId
        } else {
            first.senderName == second.senderName
        }
        if (!sameSender) return false

        // 判断时间间隔
        if (first.timestamp == 0L || second.timestamp == 0L) return true
        return kotlin.math.abs(second.timestamp - first.timestamp) < interval
    }
}

/**
 * 默认频道名称格式化器
 */
public class DefaultChannelNameFormatter : ChannelNameFormatter {
    override fun formatChannelName(channel: ChatChannel): String {
        return channel.name
    }
}

/**
 * 默认消息预览文本格式化器
 */
public class DefaultMessagePreviewFormatter : MessagePreviewFormatter {
    override fun formatPreview(channel: ChatChannel, maxLength: Int): String {
        return ChatChannelHelper.getLastMessagePreview(channel, maxLength)
    }
}

/**
 * 默认消息文本格式化器（直接返回原始文本）
 */
public class DefaultMessageTextFormatter : MessageTextFormatter {
    override fun formatText(message: ChatMessage): String {
        return message.content
    }
}

/**
 * 默认时间戳格式化器
 */
public class DefaultTimestampFormatter : TimestampFormatter {
    override fun formatTimestamp(timestamp: Long): String {
        return ChatChannelHelper.formatLastMessageTime(timestamp)
    }
}

// ============================
// 辅助函数
// ============================

/**
 * 简化的时间戳格式化（内部使用）
 *
 * 在真实项目中应使用 kotlinx-datetime 进行完整的时间格式化。
 */
internal fun formatTimestampSimple(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    // 简化实现：返回占位文本
    // 业务方应通过 DateSeparatorHandler 或 TimeFormatter 自定义
    return "刚刚"
}
