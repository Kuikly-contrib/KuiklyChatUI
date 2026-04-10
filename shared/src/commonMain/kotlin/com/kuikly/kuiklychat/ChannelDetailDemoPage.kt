package com.kuikly.kuiklychat

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.views.*
import com.kuikly.kuiklychat.base.BasePager
import com.tencent.kuiklybase.chat.channel.*
import com.tencent.kuiklybase.chat.model.*

/**
 * 频道详情 Demo 页面
 *
 * 展示 ChatChannelHeader 和 ChatChannelMemberList 组件的使用方式。
 * 模拟频道内部的信息页面（类似微信群聊信息页）。
 */
@Page("channel_detail_demo", supportInLocal = true)
internal class ChannelDetailDemoPage : BasePager() {

    /** 频道数据 */
    private var channel by observable(createSampleChannel())

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color(0xFFF0F2F5))
            }

            // 导航栏
            View {
                attr {
                    paddingTop(ctx.pagerData.statusBarHeight)
                    backgroundLinearGradient(
                        Direction.TO_RIGHT,
                        ColorStop(Color(0xFF4F8FFF), 0f),
                        ColorStop(Color(0xFF6C5CE7), 1f)
                    )
                }
                View {
                    attr {
                        height(48f)
                        flexDirectionRow()
                        alignItemsCenter()
                    }
                    // 返回按钮
                    View {
                        attr {
                            size(48f, 48f)
                            allCenter()
                        }
                        Text {
                            attr {
                                text("‹")
                                fontSize(28f)
                                color(Color.WHITE)
                            }
                        }
                        event {
                            click {
                                ctx.acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
                            }
                        }
                    }
                    Text {
                        attr {
                            flex(1f)
                            text("频道详情")
                            fontSize(17f)
                            fontWeightSemiBold()
                            color(Color.WHITE)
                            textAlignCenter()
                        }
                    }
                    View {
                        attr {
                            size(48f, 48f)
                        }
                    }
                }
            }

            // 可滚动内容区域
            Scroller {
                attr {
                    flex(1f)
                }
                View {
                    attr {
                        flexDirection(FlexDirection.COLUMN)
                    }

                    // ---- 频道头部信息 ----
                    ChatChannelHeader {
                        attr {
                            channel = ctx.channel
                            showMemberCount = true
                            showOnlineStatus = true
                            showActionButtons = true
                        }
                        event {
                            onAvatarClick = {
                                // 点击头像
                            }
                            onVoiceCallClick = {
                                // 语音通话
                            }
                            onVideoCallClick = {
                                // 视频通话
                            }
                            onSearchClick = {
                                // 搜索
                            }
                            onMoreClick = {
                                // 更多
                            }
                        }
                    }

                    // ---- 频道设置区域 ----
                    View {
                        attr {
                            backgroundColor(Color.WHITE)
                            marginTop(8f)
                        }
                        // 设置项
                        renderSettingItem("消息免打扰", ctx.channel.isMuted)
                        renderDivider()
                        renderSettingItem("置顶聊天", ctx.channel.isPinned)
                        renderDivider()
                        renderSettingItem("查找聊天记录", null)
                    }

                    // ---- 成员列表区域 ----
                    View {
                        attr {
                            backgroundColor(Color.WHITE)
                            marginTop(8f)
                        }
                        // 在线成员
                        val onlineMembers = ctx.channel.members.filter {
                            it.onlineStatus == OnlineStatus.ONLINE
                        }
                        if (onlineMembers.isNotEmpty()) {
                            ChatChannelMemberList(
                                members = onlineMembers,
                                sectionTitle = "在线 (${onlineMembers.size})",
                                onMemberClick = { member ->
                                    // 点击成员
                                }
                            )
                        }

                        // 离线成员
                        val offlineMembers = ctx.channel.members.filter {
                            it.onlineStatus != OnlineStatus.ONLINE
                        }
                        if (offlineMembers.isNotEmpty()) {
                            ChatChannelMemberList(
                                members = offlineMembers,
                                sectionTitle = "离线 (${offlineMembers.size})",
                                onMemberClick = { member ->
                                    // 点击成员
                                }
                            )
                        }
                    }

                    // ---- 底部操作区域 ----
                    View {
                        attr {
                            backgroundColor(Color.WHITE)
                            marginTop(8f)
                            marginBottom(34f)
                        }
                        // 退出群聊按钮
                        View {
                            attr {
                                height(48f)
                                allCenter()
                            }
                            Text {
                                attr {
                                    text("退出群聊")
                                    fontSize(16f)
                                    color(Color(0xFFFF4444))
                                }
                            }
                            event {
                                click {
                                    ctx.acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        /**
         * 创建示例频道数据
         */
        fun createSampleChannel(): ChatChannel {
            val members = listOf(
                ChatChannelMember(
                    id = "user_1", name = "Alice",
                    role = MemberRole.OWNER, onlineStatus = OnlineStatus.ONLINE
                ),
                ChatChannelMember(
                    id = "user_2", name = "Bob",
                    role = MemberRole.ADMIN, onlineStatus = OnlineStatus.ONLINE
                ),
                ChatChannelMember(
                    id = "user_3", name = "Charlie",
                    role = MemberRole.MEMBER, onlineStatus = OnlineStatus.OFFLINE
                ),
                ChatChannelMember(
                    id = "user_4", name = "Diana",
                    role = MemberRole.MEMBER, onlineStatus = OnlineStatus.BUSY
                ),
                ChatChannelMember(
                    id = "user_5", name = "Eve",
                    role = MemberRole.MEMBER, onlineStatus = OnlineStatus.AWAY
                ),
                ChatChannelMember(
                    id = "user_6", name = "Frank",
                    role = MemberRole.MEMBER, onlineStatus = OnlineStatus.ONLINE
                ),
                ChatChannelMember(
                    id = "user_7", name = "Grace",
                    role = MemberRole.GUEST, onlineStatus = OnlineStatus.OFFLINE
                )
            )

            return ChatChannel(
                id = "demo_channel",
                type = ChannelType.GROUP,
                name = "项目讨论组",
                memberCount = members.size,
                members = members,
                isPinned = false,
                isMuted = false
            )
        }
    }
}

// ============================
// 辅助渲染函数
// ============================

/**
 * 渲染设置项
 */
private fun ViewContainer<*, *>.renderSettingItem(title: String, isOn: Boolean?) {
    View {
        attr {
            flexDirectionRow()
            alignItemsCenter()
            height(48f)
            paddingLeft(16f)
            paddingRight(16f)
        }
        Text {
            attr {
                flex(1f)
                text(title)
                fontSize(15f)
                color(Color(0xFF333333))
            }
        }
        if (isOn != null) {
            // 开关指示
            View {
                attr {
                    size(44f, 26f)
                    borderRadius(13f)
                    backgroundColor(
                        if (isOn) Color(0xFF4F8FFF) else Color(0xFFE0E0E0)
                    )
                    justifyContentCenter()
                    paddingLeft(if (isOn) 20f else 2f)
                }
                View {
                    attr {
                        size(22f, 22f)
                        borderRadius(11f)
                        backgroundColor(Color.WHITE)
                        boxShadow(BoxShadow(0f, 1f, 3f, Color(0x33000000)))
                    }
                }
            }
        } else {
            // 箭头
            Text {
                attr {
                    text("›")
                    fontSize(20f)
                    color(Color(0xFFBBBBBB))
                }
            }
        }
    }
}

/**
 * 渲染分隔线
 */
private fun ViewContainer<*, *>.renderDivider() {
    View {
        attr {
            height(0.5f)
            backgroundColor(Color(0xFFF0F0F0))
            marginLeft(16f)
        }
    }
}
