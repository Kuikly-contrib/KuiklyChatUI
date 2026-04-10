package com.tencent.kuiklybase.chat.channel

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.directives.vforLazy
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.views.*
import com.tencent.kuiklybase.chat.model.*

// ============================
// 频道列表主视图
// ============================

/**
 * ChatChannelList - 频道/会话列表组件
 *
 * 参考 Stream Chat Compose 的 ChannelList 组件设计。
 * 展示用户的所有聊天频道/会话，支持搜索、未读计数、在线状态等。
 *
 * 布局结构：
 * ```
 * ┌──────────────────────────────────┐
 * │  导航栏（标题 + 操作按钮）         │
 * ├──────────────────────────────────┤
 * │  搜索栏                          │
 * ├──────────────────────────────────┤
 * │  频道列表                         │
 * │  ┌────────────────────────────┐  │
 * │  │ [头像] 频道名      时间     │  │
 * │  │        最后消息    [未读]   │  │
 * │  ├────────────────────────────┤  │
 * │  │ [头像] 频道名      时间     │  │
 * │  │        最后消息    [未读]   │  │
 * │  └────────────────────────────┘  │
 * └──────────────────────────────────┘
 * ```
 *
 * @param channelList 频道列表数据（响应式 ObservableList）
 * @param config 配置 DSL
 */
fun ViewContainer<*, *>.ChatChannelList(
    channelList: () -> ObservableList<ChatChannel>,
    config: ChatChannelListConfig.() -> Unit
) {
    val cfg = ChatChannelListConfig().apply(config)
    val theme = cfg.theme
    val slots = cfg.slots

    View {
        attr {
            flex(1f)
            flexDirection(FlexDirection.COLUMN)
            backgroundColor(Color(theme.backgroundColor))
        }

        // ========== 头部区域（导航栏 + 搜索栏） ==========
        if (cfg.showHeader) {
            if (slots.header != null) {
                // 自定义头部
                slots.header!!.invoke(this@View, cfg)
            } else {
                // 默认头部
                renderDefaultHeader(cfg)
            }
        }

        // ========== 频道列表区域 ==========
        val channels = channelList()
        if (channels.isEmpty()) {
            // 空状态
            if (slots.emptyContent != null) {
                slots.emptyContent!!.invoke(this@View)
            } else {
                renderDefaultEmptyContent(theme)
            }
        } else {
            // 频道列表
            List {
                attr {
                    flex(1f)
                }

                vforLazy({ channelList() }) { channel, index, count ->
                    if (slots.channelItem != null) {
                        // 自定义频道项
                        slots.channelItem!!.invoke(this, channel, cfg)
                    } else {
                        // 默认频道项
                        ChatChannelItem {
                            attr {
                                this.channel = channel
                                // 应用主题
                                itemBackgroundColor = theme.itemBackgroundColor
                                pinnedItemBackgroundColor = theme.pinnedItemBackgroundColor
                                channelNameColor = theme.channelNameColor
                                lastMessageColor = theme.lastMessageColor
                                timeColor = theme.timeColor
                                unreadBadgeColor = theme.unreadBadgeColor
                                unreadBadgeTextColor = theme.unreadBadgeTextColor
                                mutedIconColor = theme.mutedIconColor
                                dividerColor = theme.dividerColor
                                avatarPlaceholderColor = theme.avatarPlaceholderColor
                                onlineIndicatorColor = theme.onlineIndicatorColor
                                // 应用尺寸
                                itemHeight = theme.itemHeight
                                avatarSize = theme.avatarSize
                                avatarRadius = theme.avatarRadius
                                channelNameFontSize = theme.channelNameFontSize
                                lastMessageFontSize = theme.lastMessageFontSize
                                timeFontSize = theme.timeFontSize
                                unreadBadgeFontSize = theme.unreadBadgeFontSize
                                itemPaddingH = theme.itemPaddingH
                                avatarTextGap = theme.avatarTextGap
                                // 应用显示控制
                                showOnlineIndicator = cfg.showOnlineIndicator
                                showUnreadCount = cfg.showUnreadCount
                                showLastMessage = cfg.showLastMessage
                                showLastMessageTime = cfg.showLastMessageTime
                                timeFormatter = cfg.timeFormatter
                            }
                            event {
                                onClick = { cfg.onChannelClick?.invoke(channel) }
                                onLongPress = { cfg.onChannelLongPress?.invoke(channel) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================
// 默认渲染函数
// ============================

/**
 * 渲染默认头部（导航栏 + 搜索栏）
 */
private fun ViewContainer<*, *>.renderDefaultHeader(cfg: ChatChannelListConfig) {
    val theme = cfg.theme

    View {
        attr {
            flexDirection(FlexDirection.COLUMN)
        }

        // 导航栏
        View {
            attr {
                backgroundLinearGradient(
                    Direction.TO_RIGHT,
                    ColorStop(Color(theme.primaryColor), 0f),
                    ColorStop(Color(theme.primaryGradientEndColor), 1f)
                )
            }
            View {
                attr {
                    height(48f)
                    flexDirectionRow()
                    alignItemsCenter()
                }
                // 左侧区域
                View {
                    attr {
                        size(48f, 48f)
                        allCenter()
                    }
                    if (cfg.onBackClick != null) {
                        Image {
                            attr {
                                size(10f, 17f)
                                tintColor(Color.WHITE)
                                src(CHANNEL_LIST_BACK_ICON)
                            }
                        }
                        event {
                            click { cfg.onBackClick?.invoke() }
                        }
                    }
                }
                // 标题
                Text {
                    attr {
                        flex(1f)
                        text(cfg.title)
                        fontSize(17f)
                        fontWeightSemiBold()
                        color(Color(theme.headerTitleColor))
                        textAlignCenter()
                    }
                }
                // 右侧操作按钮
                View {
                    attr {
                        size(48f, 48f)
                        allCenter()
                    }
                    if (cfg.onTrailingClick != null) {
                        // 新建聊天图标（+号）
                        View {
                            attr {
                                size(24f, 24f)
                                allCenter()
                            }
                            Text {
                                attr {
                                    text("+")
                                    fontSize(24f)
                                    fontWeightLight()
                                    color(Color.WHITE)
                                }
                            }
                        }
                        event {
                            click { cfg.onTrailingClick?.invoke() }
                        }
                    }
                }
            }
        }

        // 搜索栏
        if (cfg.showSearchBar) {
            View {
                attr {
                    padding(8f, 12f, 8f, 12f)
                    backgroundColor(Color(theme.backgroundColor))
                }
                View {
                    attr {
                        flexDirectionRow()
                        alignItemsCenter()
                        height(36f)
                        backgroundColor(Color(theme.searchBarBackgroundColor))
                        borderRadius(18f)
                        paddingLeft(12f)
                        paddingRight(12f)
                    }
                    // 搜索图标
                    Text {
                        attr {
                            text("🔍")
                            fontSize(14f)
                            marginRight(6f)
                        }
                    }
                    // 搜索输入框
                    Input {
                        attr {
                            flex(1f)
                            height(36f)
                            fontSize(14f)
                            color(Color(theme.searchBarTextColor))
                            placeholder(cfg.searchPlaceholder)
                            placeholderColor(Color(theme.searchBarPlaceholderColor))
                        }
                        event {
                            textDidChange { params ->
                                cfg.onSearchTextChange?.invoke(params.text)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 渲染默认空状态
 */
private fun ViewContainer<*, *>.renderDefaultEmptyContent(theme: ChannelListTheme) {
    View {
        attr {
            flex(1f)
            allCenter()
        }
        // 空状态图标
        Text {
            attr {
                text("💬")
                fontSize(48f)
            }
        }
        Text {
            attr {
                text("暂无会话")
                fontSize(16f)
                color(Color(0xFF999999))
                marginTop(12f)
            }
        }
        Text {
            attr {
                text("开始一段新的对话吧")
                fontSize(14f)
                color(Color(0xFFBBBBBB))
                marginTop(4f)
            }
        }
    }
}

// ============================
// 常量
// ============================

/** 返回箭头图标（与 ChatNavigationBarView 一致） */
private const val CHANNEL_LIST_BACK_ICON =
    "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAsAAAASBAMAAAB/WzlGAAAAElBMVEUAAAAAAAAAAAAAAAAAAAAAAADgKxmiAAAABXRSTlMAIN/PELVZAGcAAAAkSURBVAjXYwABQTDJqCQAooSCHUAcVROCHBiFECTMhVoEtRYA6UMHzQlOjQIAAAAASUVORK5CYII="
