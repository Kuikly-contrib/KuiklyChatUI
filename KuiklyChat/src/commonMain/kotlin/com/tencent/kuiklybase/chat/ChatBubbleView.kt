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
                        backgroundColor(Color(ctx.attr.bgColor))
                        borderRadius(4f)
                        padding(4f, 8f, 4f, 8f)
                    }
                    Text {
                        attr {
                            text(ctx.attr.message)
                            fontSize(11f)
                            color(Color(ctx.attr.textColor))
                        }
                    }
                }
            }
        }
    }
}

class ChatSystemMessageAttr : ComposeAttr() {
    var message: String by observable("")
    /** 系统消息背景色（默认使用主题色） */
    var bgColor: Long by observable(0xFFCECECE)
    /** 系统消息文字颜色（默认使用主题色） */
    var textColor: Long by observable(0xFFFFFFFF)
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
                                    color(Color(ctx.attr.senderNameColor))
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
                                        backgroundColor(Color(ctx.attr.avatarPlaceholderColor))
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
                                    // P0: 头像点击事件
                                    event {
                                        click {
                                            ctx.event.onAvatarClick?.invoke()
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
                                    width(bubbleMaxWidth)
                                    alignItems(FlexAlign.FLEX_START)
                                }
                                // 消息气泡
                                View {
                                    ref { bubbleViewRef = it }
                                    attr {
                                        backgroundColor(Color(ctx.attr.otherBubbleColor))
                                        borderRadius(BorderRectRadius(2f, 12f, 12f, 12f))
                                        padding(ctx.attr.bubblePaddingV, ctx.attr.bubblePaddingH, ctx.attr.bubblePaddingV, ctx.attr.bubblePaddingH)
                                        boxShadow(BoxShadow(0f, 1f, 6f, Color(0x1A000000)))
                                        maxWidth(bubbleMaxWidth)
                                    }
                                    // P0: 引用回复块（在正文之前显示被引用的消息）
                                    if (ctx.attr.quotedMessageContent.isNotEmpty()) {
                                        // 引用块需要确定宽度让内部 Text 换行
                                        // 宽度 = 气泡最大宽度 - 气泡左右 padding
                                        val quoteBlockWidth = bubbleMaxWidth - ctx.attr.bubblePaddingH * 2
                                        View {
                                            attr {
                                                flexDirectionRow()
                                                backgroundColor(Color(ctx.attr.quoteReplyBgColor))
                                                borderRadius(6f)
                                                padding(6f, 8f, 6f, 8f)
                                                marginBottom(6f)
                                                width(quoteBlockWidth)
                                            }
                                            // 引用竖线
                                            View {
                                                attr {
                                                    width(3f)
                                                    borderRadius(1.5f)
                                                    backgroundColor(Color(ctx.attr.quoteReplyBarColor))
                                                    marginRight(8f)
                                                }
                                            }
                                            // 引用内容
                                            Column {
                                                attr {
                                                    flex(1f)
                                                    minWidth(0f)
                                                }
                                                if (ctx.attr.quotedMessageSender.isNotEmpty()) {
                                                    Text {
                                                        attr {
                                                            text(ctx.attr.quotedMessageSender)
                                                            fontSize(11f)
                                                            fontWeightMedium()
                                                            color(Color(ctx.attr.quoteReplyBarColor))
                                                            marginBottom(2f)
                                                            lines(1)
                                                        }
                                                    }
                                                }
                                                Text {
                                                    attr {
                                                        text(ctx.attr.quotedMessageContent)
                                                        fontSize(12f)
                                                        color(Color(ctx.attr.quoteReplyTextColor))
                                                        lines(2)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    // 消息内容
                                    if (ctx.attr.isDeleted) {
                                        Text {
                                            attr {
                                                text("此消息已被删除")
                                                fontSize(ctx.attr.messageFontSize)
                                                color(Color(ctx.attr.readReceiptColor))
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
                                                color(Color(ctx.attr.editedLabelColor))
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
                                            color(Color(ctx.attr.pinnedIndicatorColor))
                                            marginTop(2f)
                                        }
                                    }
                                }
                                // P1: 线程回复入口（气泡下方显示 "N 条回复"）
                                if (ctx.attr.threadCount > 0) {
                                    View {
                                        attr {
                                            flexDirectionRow()
                                            alignItems(FlexAlign.CENTER)
                                            marginTop(4f)
                                        }
                                        Text {
                                            attr {
                                                text("💬 ${ctx.attr.threadCount} 条回复")
                                                fontSize(12f)
                                                color(Color(ctx.attr.primaryColor))
                                                fontWeightMedium()
                                            }
                                        }
                                        event {
                                            click {
                                                ctx.event.onThreadClick?.invoke()
                                            }
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

                    // P0: 重发按钮（发送失败时显示在气泡左侧）——始终根据 status 自动显示
                    if (ctx.attr.status == MessageStatus.FAILED) {
                        View {
                            attr {
                                size(24f, 24f)
                                borderRadius(12f)
                                backgroundColor(Color(ctx.attr.errorColor))
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
                            width(bubbleMaxWidth)
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
                                maxWidth(bubbleMaxWidth)
                            }
                            // P0: 引用回复块（自己发送的引用）
                            if (ctx.attr.quotedMessageContent.isNotEmpty()) {
                                // 引用块需要确定宽度让内部 Text 换行
                                val quoteBlockWidth = bubbleMaxWidth - ctx.attr.bubblePaddingH * 2
                                View {
                                    attr {
                                        flexDirectionRow()
                                        backgroundColor(Color(0x33FFFFFF))
                                        borderRadius(6f)
                                        padding(6f, 8f, 6f, 8f)
                                        marginBottom(6f)
                                        width(quoteBlockWidth)
                                    }
                                    // 引用竖线
                                    View {
                                        attr {
                                            width(3f)
                                            borderRadius(1.5f)
                                            backgroundColor(Color(0xCCFFFFFF))
                                            marginRight(8f)
                                        }
                                    }
                                    // 引用内容
                                    Column {
                                        attr {
                                            flex(1f)
                                            minWidth(0f)
                                        }
                                        if (ctx.attr.quotedMessageSender.isNotEmpty()) {
                                            Text {
                                                attr {
                                                    text(ctx.attr.quotedMessageSender)
                                                    fontSize(11f)
                                                    fontWeightMedium()
                                                    color(Color(0xCCFFFFFF))
                                                    marginBottom(2f)
                                                    lines(1)
                                                }
                                            }
                                        }
                                        Text {
                                            attr {
                                                text(ctx.attr.quotedMessageContent)
                                                fontSize(12f)
                                                color(Color(0xAAFFFFFF))
                                                lines(2)
                                            }
                                        }
                                    }
                                }
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
                                    color(Color(ctx.attr.pinnedIndicatorColor))
                                    marginTop(2f)
                                }
                            }
                        }
                        // P1: 线程回复入口
                        if (ctx.attr.threadCount > 0) {
                            View {
                                attr {
                                    flexDirectionRow()
                                    alignItems(FlexAlign.CENTER)
                                    marginTop(4f)
                                }
                                Text {
                                    attr {
                                        text("💬 ${ctx.attr.threadCount} 条回复")
                                        fontSize(12f)
                                        color(Color(ctx.attr.primaryColor))
                                        fontWeightMedium()
                                    }
                                }
                                event {
                                    click {
                                        ctx.event.onThreadClick?.invoke()
                                    }
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
                                            color(Color(ctx.attr.readReceiptColor))
                                        }
                                        MessageStatus.FAILED -> {
                                            text("发送失败，点击重试")
                                            color(Color(ctx.attr.errorColor))
                                        }
                                        MessageStatus.READ -> {
                                            text("已读")
                                            color(Color(ctx.attr.readReceiptColor))
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
                                backgroundColor(Color(ctx.attr.avatarPlaceholderColor))
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
                            // P0: 头像点击事件
                            event {
                                click {
                                    ctx.event.onAvatarClick?.invoke()
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
    /** 是否显示重发按钮（发送失败时） — 保留向后兼容，现在默认自动根据 status 显示 */
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

    // ---------- P0: 主题色属性（从 ChatThemeColors 传入，消除硬编码） ----------
    /** 发送者名字颜色 */
    var senderNameColor: Long by observable(0xFF999999)
    /** 头像占位背景色 */
    var avatarPlaceholderColor: Long by observable(0xFFE8E8E8)
    /** 已读回执/状态指示文字颜色 */
    var readReceiptColor: Long by observable(0xFF999999)
    /** 已编辑标记文字颜色 */
    var editedLabelColor: Long by observable(0xFF999999)
    /** 置顶指示器颜色 */
    var pinnedIndicatorColor: Long by observable(0xFF4F8FFF)
    /** 错误/危险操作颜色（重发按钮、发送失败提示） */
    var errorColor: Long by observable(0xFFFF4444)

    // ---------- P0: 引用回复属性 ----------
    /** 被引用消息的内容（为空则不显示引用块） */
    var quotedMessageContent: String by observable("")
    /** 被引用消息的发送者名 */
    var quotedMessageSender: String by observable("")
    /** 引用回复区域背景色 */
    var quoteReplyBgColor: Long by observable(0x1A000000)
    /** 引用回复竖线颜色 */
    var quoteReplyBarColor: Long by observable(0xFF4F8FFF)
    /** 引用回复文字颜色 */
    var quoteReplyTextColor: Long by observable(0xFF666666)

    // ---------- P1: 线程回复属性 ----------
    /** 线程回复数量（> 0 时显示"N 条回复"入口） */
    var threadCount: Int by observable(0)
}

class ChatBubbleEvent : ComposeEvent() {
    var onClick: (() -> Unit)? = null
    var onLongPress: (() -> Unit)? = null
    /** 带位置信息的长按回调（x, y, width, height 相对于页面根节点） */
    var onLongPressWithPosition: ((Float, Float, Float, Float) -> Unit)? = null
    var onResendClick: (() -> Unit)? = null
    /** 反应点击回调 */
    var onReactionClick: ((String) -> Unit)? = null
    /** P0: 头像点击回调 */
    var onAvatarClick: (() -> Unit)? = null
    /** P1: 线程回复点击回调 */
    var onThreadClick: (() -> Unit)? = null
}

fun ViewContainer<*, *>.ChatBubble(init: ChatBubbleView.() -> Unit) {
    addChild(ChatBubbleView(), init)
}
