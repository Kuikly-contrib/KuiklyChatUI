package com.kuikly.kuiklychat

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.views.*
import com.kuikly.kuiklychat.base.BasePager
import com.tencent.kuiklybase.chat.channel.*
import com.tencent.kuiklybase.chat.model.*

/**
 * 频道列表 Demo 页面
 *
 * 展示 ChatChannelList 组件的使用方式。
 */
@Page("channel_list_demo", supportInLocal = true)
internal class ChannelListDemoPage : BasePager() {

    /** 频道列表数据 */
    var channelList by observableList<ChatChannel>()

    override fun created() {
        super.created()
        loadSampleChannels()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
            }

            View {
                attr {
                    flex(1f)
                    paddingTop(ctx.pagerData.statusBarHeight)
                }

                ChatChannelList({ ctx.channelList }) {
                    title = "消息"
                    showSearchBar = true
                    showOnlineIndicator = true
                    showUnreadCount = true

                    theme {
                        primaryColor = 0xFF4F8FFF
                        primaryGradientEndColor = 0xFF6C5CE7
                    }

                    onChannelClick = { channel ->
                        // 点击频道，跳转到聊天会话页面
                        val pageData = JSONObject()
                        pageData.put("pageName", "chat")
                        pageData.put("channelId", channel.id)
                        pageData.put("chatTitle", channel.name)
                        ctx.acquireModule<RouterModule>(RouterModule.MODULE_NAME)
                            .openPage("chat", pageData)
                    }

                    onChannelLongPress = { channel ->
                        // 长按频道（可弹出操作菜单）
                    }

                    onBackClick = {
                        ctx.acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
                    }

                    onTrailingClick = {
                        // 新建聊天
                    }

                    onSearchTextChange = { text ->
                        // 搜索过滤
                    }
                }
            }
        }
    }

    /**
     * 加载示例频道数据
     */
    private fun loadSampleChannels() {
        // 创建一些示例成员
        val alice = ChatChannelMember(
            id = "user_1", name = "Alice", avatarUrl = "",
            role = MemberRole.OWNER, onlineStatus = OnlineStatus.ONLINE
        )
        val bob = ChatChannelMember(
            id = "user_2", name = "Bob", avatarUrl = "",
            role = MemberRole.ADMIN, onlineStatus = OnlineStatus.ONLINE
        )
        val charlie = ChatChannelMember(
            id = "user_3", name = "Charlie", avatarUrl = "",
            role = MemberRole.MEMBER, onlineStatus = OnlineStatus.OFFLINE
        )
        val diana = ChatChannelMember(
            id = "user_4", name = "Diana", avatarUrl = "",
            role = MemberRole.MEMBER, onlineStatus = OnlineStatus.BUSY
        )
        val eve = ChatChannelMember(
            id = "user_5", name = "Eve", avatarUrl = "",
            role = MemberRole.MEMBER, onlineStatus = OnlineStatus.AWAY
        )

        // 创建示例消息
        val msg1 = ChatMessageHelper.createTextMessage("你好，最近怎么样？", isSelf = false, senderName = "Alice")
        val msg2 = ChatMessageHelper.createTextMessage("明天下午开会，别忘了", isSelf = false, senderName = "Bob")
        val msg3 = ChatMessageHelper.createTextMessage("项目进度更新：已完成 80%", isSelf = true, senderName = "我")
        val msg4 = ChatMessageHelper.createTextMessage("收到，我马上处理", isSelf = false, senderName = "Charlie")
        val msg5 = ChatMessageHelper.createTextMessage("周末一起吃饭吗？", isSelf = false, senderName = "Diana")

        channelList.addAll(listOf(
            // 置顶的单聊
            ChatChannel(
                id = "ch_1",
                type = ChannelType.DIRECT,
                name = "Alice",
                lastMessage = msg1,
                lastMessageAt = 1000L,
                unreadCount = 3,
                isPinned = true,
                memberCount = 2,
                members = listOf(alice)
            ),
            // 群聊（有未读）
            ChatChannel(
                id = "ch_2",
                type = ChannelType.GROUP,
                name = "项目讨论组",
                lastMessage = msg2,
                lastMessageAt = 900L,
                unreadCount = 12,
                memberCount = 5,
                members = listOf(alice, bob, charlie, diana, eve)
            ),
            // 单聊（已读）
            ChatChannel(
                id = "ch_3",
                type = ChannelType.DIRECT,
                name = "Bob",
                lastMessage = msg3,
                lastMessageAt = 800L,
                unreadCount = 0,
                memberCount = 2,
                members = listOf(bob)
            ),
            // 静音的群聊
            ChatChannel(
                id = "ch_4",
                type = ChannelType.GROUP,
                name = "全员通知群",
                lastMessage = msg4,
                lastMessageAt = 700L,
                unreadCount = 28,
                isMuted = true,
                memberCount = 100,
                members = listOf(charlie)
            ),
            // 公开频道
            ChatChannel(
                id = "ch_5",
                type = ChannelType.CHANNEL,
                name = "# 技术分享",
                lastMessage = msg5,
                lastMessageAt = 600L,
                unreadCount = 0,
                memberCount = 42
            ),
            // 单聊（离线）
            ChatChannel(
                id = "ch_6",
                type = ChannelType.DIRECT,
                name = "Charlie",
                lastMessage = ChatMessageHelper.createTextMessage("好的，明天见", isSelf = true),
                lastMessageAt = 500L,
                unreadCount = 0,
                memberCount = 2,
                members = listOf(charlie)
            ),
            // 群聊
            ChatChannel(
                id = "ch_7",
                type = ChannelType.GROUP,
                name = "周末活动群",
                lastMessage = ChatMessageHelper.createTextMessage("这周六去爬山怎么样？", isSelf = false, senderName = "Eve"),
                lastMessageAt = 400L,
                unreadCount = 5,
                memberCount = 8,
                members = listOf(alice, bob, charlie, diana, eve)
            ),
            // 空消息的频道
            ChatChannel(
                id = "ch_8",
                type = ChannelType.DIRECT,
                name = "Eve",
                lastMessageAt = 0L,
                unreadCount = 0,
                memberCount = 2,
                members = listOf(eve)
            )
        ))
    }
}
