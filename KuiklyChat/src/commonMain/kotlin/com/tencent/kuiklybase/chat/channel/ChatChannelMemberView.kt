package com.tencent.kuiklybase.chat.channel

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.views.*
import com.tencent.kuiklybase.chat.model.*

// ============================
// 频道成员项组件（ComposeView 模式）
// ============================

/**
 * 频道成员项属性
 */
class ChatChannelMemberItemAttr : ComposeAttr() {
    /** 成员数据 */
    var member: ChatChannelMember = ChatChannelMember(id = "", name = "")

    // ---- 主题颜色 ----
    var backgroundColor: Long = 0xFFFFFFFF
    var nameColor: Long = 0xFF333333
    var roleColor: Long = 0xFF999999
    var onlineIndicatorColor: Long = 0xFF4CAF50
    var offlineIndicatorColor: Long = 0xFFBBBBBB
    var avatarPlaceholderColor: Long = 0xFFE8E8E8
    var dividerColor: Long = 0xFFF0F0F0

    // ---- 尺寸 ----
    var avatarSize: Float = 40f
    var avatarRadius: Float = 20f
    var itemHeight: Float = 56f
    var itemPaddingH: Float = 16f

    // ---- 显示控制 ----
    var showOnlineStatus: Boolean = true
    var showRole: Boolean = true
    var showDivider: Boolean = true
}

/**
 * 频道成员项事件
 */
class ChatChannelMemberItemEvent : ComposeEvent() {
    var onClick: (() -> Unit)? = null
    var onLongPress: (() -> Unit)? = null
}

/**
 * 频道成员项 ComposeView
 *
 * 单个频道成员的渲染组件。
 *
 * 布局结构：
 * ```
 * ┌──────────────────────────────────────┐
 * │  [头像]  成员名称          角色标签   │
 * │  [在线]  在线状态                     │
 * ├──────────────────────────────────────┤
 * │  分隔线                              │
 * └──────────────────────────────────────┘
 * ```
 */
class ChatChannelMemberItemView : ComposeView<ChatChannelMemberItemAttr, ChatChannelMemberItemEvent>() {
    override fun createAttr(): ChatChannelMemberItemAttr = ChatChannelMemberItemAttr()
    override fun createEvent(): ChatChannelMemberItemEvent = ChatChannelMemberItemEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            val member = ctx.attr.member
            val a = ctx.attr

            View {
                attr {
                    flexDirection(FlexDirection.COLUMN)
                }

                // 成员项主体
                View {
                    attr {
                        flexDirection(FlexDirection.ROW)
                        alignItems(FlexAlign.CENTER)
                        height(a.itemHeight)
                        paddingLeft(a.itemPaddingH)
                        paddingRight(a.itemPaddingH)
                        backgroundColor(Color(a.backgroundColor))
                    }
                    event {
                        click { ctx.event.onClick?.invoke() }
                        longPress { ctx.event.onLongPress?.invoke() }
                    }

                    // ---- 头像区域 ----
                    View {
                        attr {
                            size(a.avatarSize, a.avatarSize)
                        }
                        if (member.avatarUrl.isNotEmpty()) {
                            Image {
                                attr {
                                    size(a.avatarSize, a.avatarSize)
                                    borderRadius(a.avatarRadius)
                                    src(member.avatarUrl)
                                }
                            }
                        } else {
                            // 头像占位
                            View {
                                attr {
                                    size(a.avatarSize, a.avatarSize)
                                    borderRadius(a.avatarRadius)
                                    backgroundColor(Color(a.avatarPlaceholderColor))
                                    allCenter()
                                }
                                Text {
                                    attr {
                                        text(member.name.take(1))
                                        fontSize(a.avatarSize * 0.4f)
                                        fontWeightSemiBold()
                                        color(Color(0xFF666666))
                                    }
                                }
                            }
                        }

                        // 在线状态指示器
                        if (a.showOnlineStatus) {
                            View {
                                attr {
                                    size(12f, 12f)
                                    borderRadius(6f)
                                    backgroundColor(Color(0xFFFFFFFF))
                                    positionAbsolute()
                                    bottom(0f)
                                    right(0f)
                                    allCenter()
                                }
                                View {
                                    attr {
                                        size(8f, 8f)
                                        borderRadius(4f)
                                        backgroundColor(
                                            when (member.onlineStatus) {
                                                OnlineStatus.ONLINE -> Color(a.onlineIndicatorColor)
                                                OnlineStatus.BUSY -> Color(0xFFFF9800)
                                                OnlineStatus.AWAY -> Color(0xFFFFC107)
                                                OnlineStatus.OFFLINE -> Color(a.offlineIndicatorColor)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ---- 名称和状态 ----
                    View {
                        attr {
                            flex(1f)
                            flexDirection(FlexDirection.COLUMN)
                            justifyContent(FlexJustifyContent.CENTER)
                            marginLeft(12f)
                        }
                        // 名称
                        Text {
                            attr {
                                text(member.name)
                                fontSize(15f)
                                fontWeightMedium()
                                color(Color(a.nameColor))
                            }
                        }
                        // 在线状态文字
                        if (a.showOnlineStatus) {
                            Text {
                                attr {
                                    text(
                                        when (member.onlineStatus) {
                                            OnlineStatus.ONLINE -> "在线"
                                            OnlineStatus.BUSY -> "忙碌"
                                            OnlineStatus.AWAY -> "离开"
                                            OnlineStatus.OFFLINE -> "离线"
                                        }
                                    )
                                    fontSize(12f)
                                    color(
                                        if (member.onlineStatus == OnlineStatus.ONLINE)
                                            Color(a.onlineIndicatorColor)
                                        else Color(a.offlineIndicatorColor)
                                    )
                                    marginTop(2f)
                                }
                            }
                        }
                    }

                    // ---- 角色标签 ----
                    if (a.showRole && member.role != MemberRole.MEMBER) {
                        View {
                            attr {
                                paddingLeft(8f)
                                paddingRight(8f)
                                height(22f)
                                borderRadius(11f)
                                backgroundColor(
                                    when (member.role) {
                                        MemberRole.OWNER -> Color(0xFFFFF3E0)
                                        MemberRole.ADMIN -> Color(0xFFE3F2FD)
                                        else -> Color(0xFFF5F5F5)
                                    }
                                )
                                allCenter()
                            }
                            Text {
                                attr {
                                    text(
                                        when (member.role) {
                                            MemberRole.OWNER -> "群主"
                                            MemberRole.ADMIN -> "管理员"
                                            MemberRole.GUEST -> "访客"
                                            else -> ""
                                        }
                                    )
                                    fontSize(11f)
                                    color(
                                        when (member.role) {
                                            MemberRole.OWNER -> Color(0xFFFF9800)
                                            MemberRole.ADMIN -> Color(0xFF2196F3)
                                            else -> Color(0xFF999999)
                                        }
                                    )
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
                            marginLeft(a.itemPaddingH + a.avatarSize + 12f)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 频道成员项扩展函数
 */
fun ViewContainer<*, *>.ChatChannelMemberItem(init: ChatChannelMemberItemView.() -> Unit) {
    addChild(ChatChannelMemberItemView(), init)
}

// ============================
// 频道成员列表组件（扩展函数模式）
// ============================

/**
 * ChatChannelMemberList - 频道成员列表组件
 *
 * 展示频道的所有成员，支持在线状态、角色标签等。
 *
 * @param members 成员列表
 * @param onMemberClick 成员点击回调
 * @param showOnlineStatus 是否显示在线状态
 * @param showRole 是否显示角色标签
 * @param backgroundColor 背景色
 * @param sectionTitle 分区标题（如"成员列表"、"在线成员"等）
 */
fun ViewContainer<*, *>.ChatChannelMemberList(
    members: List<ChatChannelMember>,
    onMemberClick: ((ChatChannelMember) -> Unit)? = null,
    onMemberLongPress: ((ChatChannelMember) -> Unit)? = null,
    showOnlineStatus: Boolean = true,
    showRole: Boolean = true,
    backgroundColor: Long = 0xFFFFFFFF,
    sectionTitle: String = ""
) {
    View {
        attr {
            flexDirection(FlexDirection.COLUMN)
            backgroundColor(Color(backgroundColor))
        }

        // 分区标题
        if (sectionTitle.isNotEmpty()) {
            View {
                attr {
                    paddingLeft(16f)
                    paddingRight(16f)
                    paddingTop(12f)
                    paddingBottom(8f)
                }
                Text {
                    attr {
                        text(sectionTitle)
                        fontSize(13f)
                        fontWeightMedium()
                        color(Color(0xFF999999))
                    }
                }
            }
        }

        // 成员列表
        for (member in members) {
            ChatChannelMemberItem {
                attr {
                    this.member = member
                    this.showOnlineStatus = showOnlineStatus
                    this.showRole = showRole
                    this.backgroundColor = backgroundColor
                }
                event {
                    onClick = { onMemberClick?.invoke(member) }
                    onLongPress = { onMemberLongPress?.invoke(member) }
                }
            }
        }
    }
}
