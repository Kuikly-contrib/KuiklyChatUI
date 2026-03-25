package com.tencent.kuiklybase.chat

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.views.*
import com.tencent.kuikly.core.views.layout.Column

// ============================
// 系统消息组件（时间提示、系统通知等）
// ============================

class ChatSystemMessageView : ComposeView<ChatSystemMessageAttr, ComposeEvent>() {
    override fun createAttr(): ChatSystemMessageAttr = ChatSystemMessageAttr()
    override fun createEvent(): ComposeEvent = ComposeEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    allCenter()
                    padding(8f, 0f, 8f, 0f)
                }
                View {
                    attr {
                        backgroundColor(Color(0xFFCECECE))
                        borderRadius(4f)
                        padding(4f, 8f, 4f, 8f)
                    }
                    Text {
                        attr {
                            text(ctx.attr.message)
                            fontSize(11f)
                            color(Color.WHITE)
                        }
                    }
                }
            }
        }
    }
}

class ChatSystemMessageAttr : ComposeAttr() {
    var message: String by observable("")
}

fun ViewContainer<*, *>.ChatSystemMessage(init: ChatSystemMessageView.() -> Unit) {
    addChild(ChatSystemMessageView(), init)
}

// ============================
// 聊天气泡组件（发送方 / 接收方消息）
// ============================

class ChatBubbleView : ComposeView<ChatBubbleAttr, ChatBubbleEvent>() {
    override fun createAttr(): ChatBubbleAttr = ChatBubbleAttr()
    override fun createEvent(): ChatBubbleEvent = ChatBubbleEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            // 计算气泡最大宽度
            val bubbleMaxWidth = ctx.pagerData.pageViewWidth * ctx.attr.bubbleMaxWidthRatio
            // 气泡内左右 padding 总和
            val bubblePaddingTotal = ctx.attr.bubblePaddingH * 2
            // 可用于显示文字的最大宽度
            val textMaxWidth = bubbleMaxWidth - bubblePaddingTotal
            // 估算文字单行宽度（中文字约等于 fontSize，英文/数字约 0.6 倍 fontSize）
            val fontSize = ctx.attr.messageFontSize
            val estimatedTextWidth = ctx.attr.content.fold(0f) { acc, c ->
                acc + if (c.code > 0x7F) fontSize else fontSize * 0.6f
            }
            // 是否需要换行：文字预估宽度超过可显示宽度
            val needsWrap = estimatedTextWidth > textMaxWidth

            // 气泡 View 的引用，用于获取气泡在页面坐标系中的精确位置
            var bubbleViewRef: ViewRef<DivView>? = null

            View {
                attr {
                    flexDirectionRow()
                    padding(ctx.attr.rowPaddingV, ctx.attr.rowPaddingH, ctx.attr.rowPaddingV, ctx.attr.rowPaddingH)
                    if (ctx.attr.isSelf) {
                        justifyContent(FlexJustifyContent.FLEX_END)
                    } else {
                        justifyContent(FlexJustifyContent.FLEX_START)
                    }
                }

                if (!ctx.attr.isSelf) {
                    // ===== 对方消息：名字在第一组消息的上方 =====
                    Column {
                        attr {
                            flex(1f)
                        }
                        // 发送者名称（显示在第一组消息上方，与气泡左边缘对齐）
                        if (ctx.attr.senderName.isNotEmpty()) {
                            Text {
                                attr {
                                    text(ctx.attr.senderName)
                                    fontSize(12f)
                                    color(Color(0xFF999999))
                                    marginBottom(4f)
                                    // 名字左侧缩进：头像宽度 + 头像与气泡间距，与气泡左边缘对齐
                                    if (ctx.attr.showAvatar || ctx.attr.showAvatarPlaceholder) {
                                        marginLeft(ctx.attr.avatarSize + ctx.attr.avatarBubbleGap)
                                    }
                                }
                            }
                        }
                        // 头像 + 气泡行
                        View {
                            attr {
                                flexDirectionRow()
                            }
                            if (ctx.attr.showAvatar) {
                                // 头像（带背景色，防止透明头像与背景融为一体）
                                View {
                                    attr {
                                        size(ctx.attr.avatarSize, ctx.attr.avatarSize)
                                        borderRadius(ctx.attr.avatarRadius)
                                        backgroundColor(Color(0xFFE8E8E8))
                                        marginTop(2f)
                                    }
                                    Image {
                                        attr {
                                            size(ctx.attr.avatarSize, ctx.attr.avatarSize)
                                            borderRadius(ctx.attr.avatarRadius)
                                            src(ctx.attr.avatarUrl.ifEmpty { DEFAULT_AVATAR })
                                            resizeCover()
                                        }
                                    }
                                }
                            } else if (ctx.attr.showAvatarPlaceholder) {
                                // 分组内非最后一条：头像占位
                                View {
                                    attr {
                                        size(ctx.attr.avatarSize, ctx.attr.avatarSize)
                                    }
                                }
                            }
                            // 消息内容区
                            Column {
                                attr {
                                    marginLeft(if (ctx.attr.showAvatar || ctx.attr.showAvatarPlaceholder) ctx.attr.avatarBubbleGap else 0f)
                                    if (needsWrap) {
                                        // 长文本：用固定宽度，让 Text 在此宽度内自动换行
                                        width(bubbleMaxWidth)
                                    }
                                    // 短文本：不设宽度，Column 自适应内容，气泡包裹文字
                                }
                                // 消息气泡
                                View {
                                    ref { bubbleViewRef = it }
                                    attr {
                                        backgroundColor(Color(ctx.attr.otherBubbleColor))
                                        borderRadius(BorderRectRadius(2f, 12f, 12f, 12f))
                                        padding(ctx.attr.bubblePaddingV, ctx.attr.bubblePaddingH, ctx.attr.bubblePaddingV, ctx.attr.bubblePaddingH)
                                        boxShadow(BoxShadow(0f, 1f, 6f, Color(0x1A000000)))
                                    }
                                    // 消息内容
                                    if (ctx.attr.isDeleted) {
                                        Text {
                                            attr {
                                                text("此消息已被删除")
                                                fontSize(ctx.attr.messageFontSize)
                                                color(Color(0xFF999999))
                                                lineHeight(ctx.attr.messageLineHeight)
                                            }
                                        }
                                    } else {
                                        Text {
                                            attr {
                                                text(ctx.attr.content)
                                                fontSize(ctx.attr.messageFontSize)
                                                color(Color(ctx.attr.otherTextColor))
                                                lineHeight(ctx.attr.messageLineHeight)
                                            }
                                        }
                                    }
                                    // 已编辑标记
                                    if (ctx.attr.isEdited && !ctx.attr.isDeleted) {
                                        Text {
                                            attr {
                                                text("(已编辑)")
                                                fontSize(10f)
                                                color(Color(0xFF999999))
                                                marginTop(2f)
                                            }
                                        }
                                    }
                                    event {
                                        click {
                                            ctx.event.onClick?.invoke()
                                        }
                                        longPress {
                                            if (ctx.event.onLongPressWithPosition != null) {
                                                bubbleViewRef?.view?.let { view ->
                                                    val frame = view.frame
                                                    val frameInRoot = view.convertFrame(frame, toView = null)
                                                    ctx.event.onLongPressWithPosition?.invoke(
                                                        frameInRoot.x, frameInRoot.y,
                                                        frameInRoot.width, frameInRoot.height
                                                    )
                                                } ?: ctx.event.onLongPress?.invoke()
                                            } else {
                                                ctx.event.onLongPress?.invoke()
                                            }
                                        }
                                    }
                                }
                                // 置顶标记
                                if (ctx.attr.isPinned) {
                                    Text {
                                        attr {
                                            text("📌 已置顶")
                                            fontSize(10f)
                                            color(Color(0xFF4F8FFF))
                                            marginTop(2f)
                                        }
                                    }
                                }
                                // 反应栏（气泡下方）
                                if (ctx.attr.reactions.isNotEmpty()) {
                                    ChatReactionBar {
                                        attr {
                                            reactions = ctx.attr.reactions
                                        }
                                        event {
                                            onReactionClick = { type ->
                                                ctx.event.onReactionClick?.invoke(type)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // ===== 自己的消息：重发按钮 + 气泡 + 头像 =====

                    // 重发按钮（发送失败时显示在气泡左侧）
                    if (ctx.attr.showResend && ctx.attr.status == MessageStatus.FAILED) {
                        View {
                            attr {
                                size(24f, 24f)
                                borderRadius(12f)
                                backgroundColor(Color(0xFFFF4444))
                                allCenter()
                                marginRight(6f)
                                alignSelf(FlexAlign.CENTER)
                            }
                            Text {
                                attr {
                                    text("!")
                                    fontSize(14f)
                                    fontWeightBold()
                                    color(Color.WHITE)
                                }
                            }
                            event {
                                click {
                                    ctx.event.onResendClick?.invoke()
                                }
                            }
                        }
                    }

                    Column {
                        attr {
                            if (needsWrap) {
                                // 长文本：用固定宽度，让 Text 在此宽度内自动换行
                                width(bubbleMaxWidth)
                            }
                            alignItems(FlexAlign.FLEX_END)
                        }
                        // 消息气泡（渐变色）
                        View {
                            ref { bubbleViewRef = it }
                            attr {
                                backgroundLinearGradient(
                                    Direction.TO_RIGHT,
                                    ColorStop(Color(ctx.attr.primaryColor), 0f),
                                    ColorStop(Color(ctx.attr.primaryGradientEndColor), 1f)
                                )
                                borderRadius(BorderRectRadius(12f, 2f, 12f, 12f))
                                padding(ctx.attr.bubblePaddingV, ctx.attr.bubblePaddingH, ctx.attr.bubblePaddingV, ctx.attr.bubblePaddingH)
                                boxShadow(BoxShadow(0f, 1f, 6f, Color(0x334F8FFF)))
                            }
                            // 消息内容
                            if (ctx.attr.isDeleted) {
                                Text {
                                    attr {
                                        text("此消息已被删除")
                                        fontSize(ctx.attr.messageFontSize)
                                        color(Color(0xCCFFFFFF))
                                        lineHeight(ctx.attr.messageLineHeight)
                                    }
                                }
                            } else {
                                Text {
                                    attr {
                                        text(ctx.attr.content)
                                        fontSize(ctx.attr.messageFontSize)
                                        color(Color(ctx.attr.selfTextColor))
                                        lineHeight(ctx.attr.messageLineHeight)
                                    }
                                }
                            }
                            // 已编辑标记
                            if (ctx.attr.isEdited && !ctx.attr.isDeleted) {
                                Text {
                                    attr {
                                        text("(已编辑)")
                                        fontSize(10f)
                                        color(Color(0xCCFFFFFF))
                                        marginTop(2f)
                                    }
                                }
                            }
                            event {
                                click {
                                    ctx.event.onClick?.invoke()
                                }
                                longPress {
                                    if (ctx.event.onLongPressWithPosition != null) {
                                        bubbleViewRef?.view?.let { view ->
                                            val frame = view.frame
                                            val frameInRoot = view.convertFrame(frame, toView = null)
                                            ctx.event.onLongPressWithPosition?.invoke(
                                                frameInRoot.x, frameInRoot.y,
                                                frameInRoot.width, frameInRoot.height
                                            )
                                        } ?: ctx.event.onLongPress?.invoke()
                                    } else {
                                        ctx.event.onLongPress?.invoke()
                                    }
                                }
                            }
                        }
                        // 置顶标记
                        if (ctx.attr.isPinned) {
                            Text {
                                attr {
                                    text("📌 已置顶")
                                    fontSize(10f)
                                    color(Color(0xFF4F8FFF))
                                    marginTop(2f)
                                }
                            }
                        }
                        // 反应栏（气泡下方）
                        if (ctx.attr.reactions.isNotEmpty()) {
                            ChatReactionBar {
                                attr {
                                    reactions = ctx.attr.reactions
                                }
                                event {
                                    onReactionClick = { type ->
                                        ctx.event.onReactionClick?.invoke(type)
                                    }
                                }
                            }
                        }
                        // 消息状态指示器（仅自己发送的消息显示）
                        if (ctx.attr.status != MessageStatus.SENT) {
                            Text {
                                attr {
                                    marginTop(2f)
                                    fontSize(10f)
                                    when (ctx.attr.status) {
                                        MessageStatus.SENDING -> {
                                            text("发送中...")
                                            color(Color(0xFF999999))
                                        }
                                        MessageStatus.FAILED -> {
                                            text("发送失败，点击重试")
                                            color(Color(0xFFFF4444))
                                        }
                                        MessageStatus.READ -> {
                                            text("已读")
                                            color(Color(0xFF999999))
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        }
                    }
                    // 头像（带背景色，防止透明头像与背景融为一体）
                    if (ctx.attr.showAvatar) {
                        View {
                            attr {
                                size(ctx.attr.avatarSize, ctx.attr.avatarSize)
                                borderRadius(ctx.attr.avatarRadius)
                                backgroundColor(Color(0xFFE8E8E8))
                                marginLeft(ctx.attr.avatarBubbleGap)
                                marginTop(2f)
                            }
                            Image {
                                attr {
                                    size(ctx.attr.avatarSize, ctx.attr.avatarSize)
                                    borderRadius(ctx.attr.avatarRadius)
                                    src(ctx.attr.selfAvatarUrl.ifEmpty { SELF_AVATAR })
                                    resizeCover()
                                }
                            }
                        }
                    } else if (ctx.attr.showAvatarPlaceholder) {
                        // 分组内非最后一条：头像占位
                        View {
                            attr {
                                marginLeft(ctx.attr.avatarBubbleGap)
                                size(ctx.attr.avatarSize, ctx.attr.avatarSize)
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val DEFAULT_AVATAR =
            "https://vfiles.gtimg.cn/wuji_dashboard/wupload/xy/starter/62394e19.png"
        const val SELF_AVATAR =
            "https://vfiles.gtimg.cn/wuji_dashboard/wupload/xy/starter/62394e19.png"
    }
}

class ChatBubbleAttr : ComposeAttr() {
    var content: String by observable("")
    var isSelf: Boolean by observable(false)
    var avatarUrl: String by observable("")
    var selfAvatarUrl: String by observable("")
    var senderName: String by observable("")
    var primaryColor: Long by observable(0xFF4F8FFF)
    var primaryGradientEndColor: Long by observable(0xFF6C5CE7)
    var status: MessageStatus by observable(MessageStatus.SENT)
    /** 对方消息气泡背景色 */
    var otherBubbleColor: Long by observable(0xFFFFFFFF)
    /** 对方消息文字颜色 */
    var otherTextColor: Long by observable(0xFF333333)
    /** 自己消息文字颜色 */
    var selfTextColor: Long by observable(0xFFFFFFFF)
    /** 是否显示头像 */
    var showAvatar: Boolean by observable(true)
    /** 不显示头像时是否保留占位空间（分组内非最后一条消息时为 true） */
    var showAvatarPlaceholder: Boolean by observable(false)
    /** 头像圆角半径（20f = 圆形，8f = 微信风格圆角方形，0f = 方形） */
    var avatarRadius: Float by observable(8f)
    /** 是否显示重发按钮（发送失败时） */
    var showResend: Boolean by observable(false)

    // ---------- 布局可配属性（均有默认值，使用方可按需覆盖） ----------
    /** 气泡最大宽度占屏幕宽度的比例 */
    var bubbleMaxWidthRatio: Float by observable(0.65f)
    /** 气泡内水平 padding（单侧） */
    var bubblePaddingH: Float by observable(12f)
    /** 气泡内垂直 padding（单侧） */
    var bubblePaddingV: Float by observable(10f)
    /** 消息文字大小 */
    var messageFontSize: Float by observable(15f)
    /** 消息行高 */
    var messageLineHeight: Float by observable(22f)
    /** 头像尺寸（宽高相同） */
    var avatarSize: Float by observable(40f)
    /** 消息行垂直 padding */
    var rowPaddingV: Float by observable(6f)
    /** 消息行水平 padding */
    var rowPaddingH: Float by observable(12f)
    /** 头像与气泡的间距 */
    var avatarBubbleGap: Float by observable(8f)

    // ---------- 新增属性（反应、编辑/删除状态） ----------
    /** 消息反应列表 */
    var reactions: List<ReactionItem> by observable(emptyList())
    /** 是否已编辑 */
    var isEdited: Boolean by observable(false)
    /** 是否已删除（软删除） */
    var isDeleted: Boolean by observable(false)
    /** 是否已置顶 */
    var isPinned: Boolean by observable(false)
}

class ChatBubbleEvent : ComposeEvent() {
    var onClick: (() -> Unit)? = null
    var onLongPress: (() -> Unit)? = null
    /** 带位置信息的长按回调（x, y, width, height 相对于页面根节点） */
    var onLongPressWithPosition: ((Float, Float, Float, Float) -> Unit)? = null
    var onResendClick: (() -> Unit)? = null
    /** 反应点击回调 */
    var onReactionClick: ((String) -> Unit)? = null
}

fun ViewContainer<*, *>.ChatBubble(init: ChatBubbleView.() -> Unit) {
    addChild(ChatBubbleView(), init)
}
