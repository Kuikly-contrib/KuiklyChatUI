package com.tencent.kuiklybase.chat.channel

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.views.*
import com.tencent.kuiklybase.chat.model.*

// ============================
// 频道列表项组件（ComposeView 模式）
// ============================

/**
 * 频道列表项属性
 */
internal class ChatChannelItemAttr : ComposeAttr() {
    /** 频道数据 */
    public var channel: ChatChannel = ChatChannel(id = "")

    // ---- 主题颜色 ----
    public var itemBackgroundColor: Long = 0xFFFFFFFF
    public var pinnedItemBackgroundColor: Long = 0xFFF8F8FF
    public var channelNameColor: Long = 0xFF333333
    public var lastMessageColor: Long = 0xFF999999
    public var timeColor: Long = 0xFFBBBBBB
    public var unreadBadgeColor: Long = 0xFFFF4444
    public var unreadBadgeTextColor: Long = 0xFFFFFFFF
    public var mutedIconColor: Long = 0xFFBBBBBB
    public var dividerColor: Long = 0xFFF0F0F0
    public var avatarPlaceholderColor: Long = 0xFFE8E8E8
    public var onlineIndicatorColor: Long = 0xFF4CAF50

    // ---- 尺寸 ----
    public var itemHeight: Float = 72f
    public var avatarSize: Float = 48f
    public var avatarRadius: Float = 8f
    public var channelNameFontSize: Float = 16f
    public var lastMessageFontSize: Float = 14f
    public var timeFontSize: Float = 12f
    public var unreadBadgeFontSize: Float = 11f
    public var itemPaddingH: Float = 16f
    public var avatarTextGap: Float = 12f

    // ---- 显示控制 ----
    public var showOnlineIndicator: Boolean = true
    public var showUnreadCount: Boolean = true
    public var showLastMessage: Boolean = true
    public var showLastMessageTime: Boolean = true
    public var showDivider: Boolean = true

    /** 自定义时间格式化器 */
    public var timeFormatter: ChannelTimeFormatter? = null
}

/**
 * 频道列表项事件
 */
internal class ChatChannelItemEvent : ComposeEvent() {
    public var onClick: (() -> Unit)? = null
    public var onLongPress: (() -> Unit)? = null
}

/**
 * 频道列表项 ComposeView
 *
 * 单个频道/会话项的渲染组件，类似微信/Stream Chat 的会话列表项。
 *
 * 布局结构：
 * ```
 * ┌─────────────────────────────────────────────────┐
 * │  [头像]  频道名称              时间              │
 * │  [在线]  最后消息预览          [未读] / [静音]    │
 * ├─────────────────────────────────────────────────┤
 * │  分隔线                                         │
 * └─────────────────────────────────────────────────┘
 * ```
 */
internal class ChatChannelItemView : ComposeView<ChatChannelItemAttr, ChatChannelItemEvent>() {
    override fun createAttr(): ChatChannelItemAttr = ChatChannelItemAttr()
    override fun createEvent(): ChatChannelItemEvent = ChatChannelItemEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            val channel = ctx.attr.channel
            val a = ctx.attr

            View {
                attr {
                    flexDirection(FlexDirection.COLUMN)
                }

                // 频道项主体
                View {
                    attr {
                        flexDirection(FlexDirection.ROW)
                        alignItems(FlexAlign.CENTER)
                        height(a.itemHeight)
                        paddingLeft(a.itemPaddingH)
                        paddingRight(a.itemPaddingH)
                        backgroundColor(
                            if (channel.isPinned) Color(a.pinnedItemBackgroundColor)
                            else Color(a.itemBackgroundColor)
                        )
                    }
                    event {
                        click { ctx.event.onClick?.invoke() }
                        longPress { ctx.event.onLongPress?.invoke() }
                    }

                    // ---- 头像区域（含在线状态指示器） ----
                    View {
                        attr {
                            size(a.avatarSize, a.avatarSize)
                        }
                        // 头像
                        if (channel.avatarUrl.isNotEmpty()) {
                            Image {
                                attr {
                                    size(a.avatarSize, a.avatarSize)
                                    borderRadius(a.avatarRadius)
                                    src(channel.avatarUrl)
                                }
                            }
                        } else {
                            // 头像占位（显示首字母）
                            View {
                                attr {
                                    size(a.avatarSize, a.avatarSize)
                                    borderRadius(a.avatarRadius)
                                    backgroundColor(Color(a.avatarPlaceholderColor))
                                    allCenter()
                                }
                                Text {
                                    attr {
                                        text(channel.name.take(1))
                                        fontSize(a.avatarSize * 0.4f)
                                        fontWeightSemiBold()
                                        color(Color(0xFF666666))
                                    }
                                }
                            }
                        }

                        // 在线状态指示器（仅单聊且开启时显示）
                        if (a.showOnlineIndicator && channel.type == ChannelType.DIRECT) {
                            val isOnline = channel.members.any { it.onlineStatus == OnlineStatus.ONLINE }
                            if (isOnline) {
                                View {
                                    attr {
                                        size(14f, 14f)
                                        borderRadius(7f)
                                        backgroundColor(Color(0xFFFFFFFF))
                                        positionAbsolute()
                                        bottom(0f)
                                        right(0f)
                                        allCenter()
                                    }
                                    View {
                                        attr {
                                            size(10f, 10f)
                                            borderRadius(5f)
                                            backgroundColor(Color(a.onlineIndicatorColor))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ---- 中间内容区域 ----
                    View {
                        attr {
                            flex(1f)
                            flexDirection(FlexDirection.COLUMN)
                            justifyContent(FlexJustifyContent.CENTER)
                            marginLeft(a.avatarTextGap)
                            marginRight(8f)
                        }

                        // 第一行：频道名称
                        View {
                            attr {
                                flexDirection(FlexDirection.ROW)
                                alignItems(FlexAlign.CENTER)
                            }
                            Text {
                                attr {
                                    flex(1f)
                                    text(channel.name)
                                    fontSize(a.channelNameFontSize)
                                    fontWeightMedium()
                                    color(Color(a.channelNameColor))
                                }
                            }
                            // 静音图标
                            if (channel.isMuted) {
                                Text {
                                    attr {
                                        text("🔇")
                                        fontSize(12f)
                                        marginLeft(4f)
                                    }
                                }
                            }
                        }

                        // 第二行：最后消息预览
                        if (a.showLastMessage && channel.lastMessage != null) {
                            Text {
                                attr {
                                    text(ChatChannelHelper.getLastMessagePreview(channel))
                                    fontSize(a.lastMessageFontSize)
                                    color(Color(a.lastMessageColor))
                                    marginTop(4f)
                                }
                            }
                        }
                    }

                    // ---- 右侧区域（时间 + 未读计数） ----
                    View {
                        attr {
                            flexDirection(FlexDirection.COLUMN)
                            alignItems(FlexAlign.FLEX_END)
                            justifyContent(FlexJustifyContent.CENTER)
                        }

                        // 时间
                        if (a.showLastMessageTime && channel.lastMessageAt > 0L) {
                            val timeText = a.timeFormatter?.invoke(channel.lastMessageAt)
                                ?: ChatChannelHelper.formatLastMessageTime(channel.lastMessageAt)
                            Text {
                                attr {
                                    text(timeText)
                                    fontSize(a.timeFontSize)
                                    color(Color(a.timeColor))
                                }
                            }
                        }

                        // 未读计数
                        if (a.showUnreadCount && channel.unreadCount > 0 && !channel.isMuted) {
                            View {
                                attr {
                                    marginTop(6f)
                                    minWidth(18f)
                                    height(18f)
                                    borderRadius(9f)
                                    backgroundColor(Color(a.unreadBadgeColor))
                                    allCenter()
                                    paddingLeft(5f)
                                    paddingRight(5f)
                                }
                                Text {
                                    attr {
                                        text(
                                            if (channel.unreadCount > 99) "99+"
                                            else channel.unreadCount.toString()
                                        )
                                        fontSize(a.unreadBadgeFontSize)
                                        color(Color(a.unreadBadgeTextColor))
                                        fontWeightMedium()
                                    }
                                }
                            }
                        } else if (a.showUnreadCount && channel.unreadCount > 0 && channel.isMuted) {
                            // 静音频道的未读：显示小红点
                            View {
                                attr {
                                    marginTop(6f)
                                    size(8f, 8f)
                                    borderRadius(4f)
                                    backgroundColor(Color(a.unreadBadgeColor))
                                }
                            }
                        }
                    }
                }

                // ---- 分隔线 ----
                if (a.showDivider) {
                    View {
                        attr {
                            height(0.5f)
                            backgroundColor(Color(a.dividerColor))
                            marginLeft(a.itemPaddingH + a.avatarSize + a.avatarTextGap)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 频道列表项扩展函数
 */
internal fun ViewContainer<*, *>.ChatChannelItem(init: ChatChannelItemView.() -> Unit) {
    addChild(ChatChannelItemView(), init)
}
