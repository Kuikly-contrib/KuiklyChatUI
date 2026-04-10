package com.tencent.kuiklybase.chat.channel

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.views.*
import com.tencent.kuiklybase.chat.model.*

// ============================
// 频道详情头部组件（ComposeView 模式）
// ============================

/**
 * 频道详情头部属性
 */
class ChatChannelHeaderAttr : ComposeAttr() {
    /** 频道数据 */
    var channel: ChatChannel = ChatChannel(id = "")

    // ---- 主题颜色 ----
    /** 背景色 */
    var backgroundColor: Long = 0xFFFFFFFF
    /** 主色 */
    var primaryColor: Long = 0xFF4F8FFF
    /** 渐变结束色 */
    var primaryGradientEndColor: Long = 0xFF6C5CE7
    /** 频道名称颜色 */
    var channelNameColor: Long = 0xFF333333
    /** 描述文字颜色 */
    var descriptionColor: Long = 0xFF999999
    /** 在线状态颜色 */
    var onlineIndicatorColor: Long = 0xFF4CAF50
    /** 头像占位背景色 */
    var avatarPlaceholderColor: Long = 0xFFE8E8E8
    /** 分隔线颜色 */
    var dividerColor: Long = 0xFFF0F0F0
    /** 操作按钮颜色 */
    var actionButtonColor: Long = 0xFF4F8FFF
    /** 操作按钮文字颜色 */
    var actionButtonTextColor: Long = 0xFF666666

    // ---- 尺寸 ----
    /** 头像尺寸 */
    var avatarSize: Float = 64f
    /** 头像圆角 */
    var avatarRadius: Float = 12f

    // ---- 显示控制 ----
    /** 是否显示成员数 */
    var showMemberCount: Boolean = true
    /** 是否显示在线状态 */
    var showOnlineStatus: Boolean = true
    /** 是否显示操作按钮（语音、视频、搜索等） */
    var showActionButtons: Boolean = true
}

/**
 * 频道详情头部事件
 */
class ChatChannelHeaderEvent : ComposeEvent() {
    /** 头像点击 */
    var onAvatarClick: (() -> Unit)? = null
    /** 语音通话按钮点击 */
    var onVoiceCallClick: (() -> Unit)? = null
    /** 视频通话按钮点击 */
    var onVideoCallClick: (() -> Unit)? = null
    /** 搜索按钮点击 */
    var onSearchClick: (() -> Unit)? = null
    /** 更多按钮点击 */
    var onMoreClick: (() -> Unit)? = null
}

/**
 * 频道详情头部 ComposeView
 *
 * 展示频道的详细信息，通常放在频道内部页面的顶部。
 * 包含频道头像、名称、成员数、在线状态和操作按钮。
 *
 * 布局结构：
 * ```
 * ┌──────────────────────────────────────┐
 * │          [大头像]                     │
 * │        频道名称                       │
 * │     成员数 · 在线数                   │
 * │                                      │
 * │  [语音]  [视频]  [搜索]  [更多]       │
 * ├──────────────────────────────────────┤
 * │  分隔线                              │
 * └──────────────────────────────────────┘
 * ```
 */
class ChatChannelHeaderView : ComposeView<ChatChannelHeaderAttr, ChatChannelHeaderEvent>() {
    override fun createAttr(): ChatChannelHeaderAttr = ChatChannelHeaderAttr()
    override fun createEvent(): ChatChannelHeaderEvent = ChatChannelHeaderEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            val channel = ctx.attr.channel
            val a = ctx.attr

            View {
                attr {
                    flexDirection(FlexDirection.COLUMN)
                    alignItems(FlexAlign.CENTER)
                    backgroundColor(Color(a.backgroundColor))
                    paddingTop(20f)
                    paddingBottom(16f)
                }

                // ---- 头像 ----
                View {
                    attr {
                        size(a.avatarSize, a.avatarSize)
                    }
                    if (channel.avatarUrl.isNotEmpty()) {
                        Image {
                            attr {
                                size(a.avatarSize, a.avatarSize)
                                borderRadius(a.avatarRadius)
                                src(channel.avatarUrl)
                            }
                        }
                    } else {
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
                    event {
                        click { ctx.event.onAvatarClick?.invoke() }
                    }
                }

                // ---- 频道名称 ----
                Text {
                    attr {
                        text(channel.name)
                        fontSize(20f)
                        fontWeightBold()
                        color(Color(a.channelNameColor))
                        marginTop(12f)
                        textAlignCenter()
                    }
                }

                // ---- 成员数 / 在线状态 ----
                if (a.showMemberCount || a.showOnlineStatus) {
                    View {
                        attr {
                            flexDirectionRow()
                            alignItemsCenter()
                            marginTop(4f)
                        }

                        if (a.showMemberCount && channel.memberCount > 0) {
                            Text {
                                attr {
                                    val memberText = "${channel.memberCount} 位成员"
                                    text(memberText)
                                    fontSize(14f)
                                    color(Color(a.descriptionColor))
                                }
                            }
                        }

                        if (a.showOnlineStatus && a.showMemberCount && channel.memberCount > 0) {
                            val onlineCount = channel.members.count { it.onlineStatus == OnlineStatus.ONLINE }
                            if (onlineCount > 0) {
                                Text {
                                    attr {
                                        text(" · ")
                                        fontSize(14f)
                                        color(Color(a.descriptionColor))
                                    }
                                }
                                // 在线指示点
                                View {
                                    attr {
                                        size(6f, 6f)
                                        borderRadius(3f)
                                        backgroundColor(Color(a.onlineIndicatorColor))
                                        marginRight(4f)
                                    }
                                }
                                Text {
                                    attr {
                                        text("$onlineCount 在线")
                                        fontSize(14f)
                                        color(Color(a.onlineIndicatorColor))
                                    }
                                }
                            }
                        }

                        // 单聊时显示对方在线状态
                        if (channel.type == ChannelType.DIRECT && a.showOnlineStatus) {
                            val otherMember = channel.members.firstOrNull()
                            if (otherMember != null) {
                                View {
                                    attr {
                                        size(6f, 6f)
                                        borderRadius(3f)
                                        backgroundColor(
                                            if (otherMember.onlineStatus == OnlineStatus.ONLINE)
                                                Color(a.onlineIndicatorColor)
                                            else Color(0xFFBBBBBB)
                                        )
                                        marginRight(4f)
                                    }
                                }
                                Text {
                                    attr {
                                        text(
                                            when (otherMember.onlineStatus) {
                                                OnlineStatus.ONLINE -> "在线"
                                                OnlineStatus.BUSY -> "忙碌"
                                                OnlineStatus.AWAY -> "离开"
                                                OnlineStatus.OFFLINE -> "离线"
                                            }
                                        )
                                        fontSize(14f)
                                        color(Color(a.descriptionColor))
                                    }
                                }
                            }
                        }
                    }
                }

                // ---- 操作按钮 ----
                if (a.showActionButtons) {
                    View {
                        attr {
                            flexDirectionRow()
                            justifyContent(FlexJustifyContent.CENTER)
                            marginTop(16f)
                        }

                        // 语音通话
                        renderActionButton("📞", "语音") {
                            ctx.event.onVoiceCallClick?.invoke()
                        }
                        // 视频通话
                        renderActionButton("📹", "视频") {
                            ctx.event.onVideoCallClick?.invoke()
                        }
                        // 搜索
                        renderActionButton("🔍", "搜索") {
                            ctx.event.onSearchClick?.invoke()
                        }
                        // 更多
                        renderActionButton("⋯", "更多") {
                            ctx.event.onMoreClick?.invoke()
                        }
                    }
                }

                // ---- 底部分隔线 ----
                View {
                    attr {
                        height(8f)
                        backgroundColor(Color(a.dividerColor))
                        marginTop(16f)
                        alignSelfStretch()
                    }
                }
            }
        }
    }
}

/**
 * 渲染操作按钮
 */
private fun ViewContainer<*, *>.renderActionButton(
    icon: String,
    label: String,
    onClick: () -> Unit
) {
    View {
        attr {
            alignItems(FlexAlign.CENTER)
            marginLeft(16f)
            marginRight(16f)
        }
        // 图标圆形背景
        View {
            attr {
                size(44f, 44f)
                borderRadius(22f)
                backgroundColor(Color(0xFFF5F5F5))
                allCenter()
            }
            Text {
                attr {
                    text(icon)
                    fontSize(20f)
                }
            }
        }
        // 标签
        Text {
            attr {
                text(label)
                fontSize(12f)
                color(Color(0xFF666666))
                marginTop(4f)
            }
        }
        event {
            click { onClick() }
        }
    }
}

/**
 * 频道详情头部扩展函数
 */
fun ViewContainer<*, *>.ChatChannelHeader(init: ChatChannelHeaderView.() -> Unit) {
    addChild(ChatChannelHeaderView(), init)
}
