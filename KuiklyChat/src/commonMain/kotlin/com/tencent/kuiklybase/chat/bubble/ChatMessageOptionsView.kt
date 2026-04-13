package com.tencent.kuiklybase.chat.bubble

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.base.event.layoutFrameDidChange
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.*
import com.tencent.kuikly.core.views.Blur
import com.tencent.kuiklybase.chat.model.*

// ============================
// 消息操作菜单组件
// ============================

/**
 * 默认的快捷反应 emoji 列表
 */
val DEFAULT_QUICK_REACTIONS = listOf("👍", "❤️", "😂", "😮", "😢", "🙏")

/**
 * ChatMessageOptionsView - 消息操作菜单组件
 *
 * 长按消息时弹出的操作菜单，设计参考微信/Telegram 风格：
 * - 菜单紧贴在被长按消息气泡的正上方（非整行，仅气泡区域）
 * - 全屏高斯模糊（毛玻璃）背景，被长按的消息气泡区域镂空保持清晰
 * - 顶部快捷反应 emoji 行
 * - 操作列表（复制、回复、引用、编辑、删除、置顶等）
 * - 点击模糊区域关闭菜单
 */
class ChatMessageOptionsView : ComposeView<ChatMessageOptionsAttr, ChatMessageOptionsEvent>() {
    override fun createAttr(): ChatMessageOptionsAttr = ChatMessageOptionsAttr()
    override fun createEvent(): ChatMessageOptionsEvent = ChatMessageOptionsEvent()

    // 入场动画标记
    private var animationFlag by observable(false)
    // 菜单卡片的实际布局高度（通过 layoutFrameDidChange 获取，0 表示尚未获得）
    private var menuActualHeight by observable(0f)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            val screenW = ctx.attr.screenWidth
            val screenH = ctx.attr.screenHeight

            // 菜单卡片的估算高度（用于定位计算）
            val visibleActions = ctx.attr.actions.filter { action ->
                ctx.attr.message?.let { action.isVisible(it) } ?: true
            }
            // 菜单卡片高度：优先使用实际布局高度，未获得时回退到估算
            val emojiRowHeight = if (ctx.attr.showQuickReactions) 52f else 0f
            val actionItemHeight = 41f  // 每项：padding(13) + fontSize(15) + padding(13) = 41
            val menuEstimatedHeight = emojiRowHeight + actionItemHeight * visibleActions.size
            val menuHeight = if (ctx.menuActualHeight > 0f) ctx.menuActualHeight else menuEstimatedHeight
            val menuWidth = minOf(screenW * 0.6f, 260f)

            // 被长按消息气泡的位置信息（精确到气泡区域，不包含头像等）
            val msgX = ctx.attr.targetX
            val msgY = ctx.attr.targetY
            val msgW = ctx.attr.targetWidth
            val msgH = ctx.attr.targetHeight

            // 计算菜单的 Y 位置：优先显示在气泡上方，空间不够则显示在下方
            val menuGap = 0f  // 菜单和气泡之间留一点间隙，避免视觉遮挡
            val topSpace = msgY  // 气泡上方的可用空间
            val bottomSpace = screenH - msgY - msgH  // 气泡下方的可用空间
            val showAbove = topSpace >= menuHeight + menuGap || topSpace >= bottomSpace
            val menuTop = if (showAbove) {
                // 显示在气泡上方：菜单底边 = msgY - menuGap
                maxOf(8f, msgY - menuHeight - menuGap)
            } else {
                // 显示在气泡下方：菜单顶边 = msgY + msgH + menuGap
                minOf(screenH - menuHeight - 8f, msgY + msgH + menuGap)
            }

            // 计算菜单的 X 位置：水平居中对齐气泡
            val isSelf = ctx.attr.message?.isSelf ?: false
            val menuLeft = if (isSelf) {
                // 自己的消息：菜单右边缘对齐气泡右边缘
                maxOf(8f, msgX + msgW - menuWidth)
            } else {
                // 对方消息：菜单左边缘对齐气泡左边缘
                minOf(screenW - menuWidth - 8f, maxOf(8f, msgX))
            }

            // 全屏遮罩容器
            Modal {
                attr {
                    // Modal 默认与页面等大
                }

                // ===== 全屏高斯模糊层 =====
                Blur {
                    attr {
                        positionAbsolute()
                        top(0f)
                        left(0f)
                        width(screenW)
                        height(screenH)
                        blurRadius(12.5f)
                        if (ctx.animationFlag) opacity(1f) else opacity(0f)
                        animate(Animation.easeInOut(0.2f), ctx.animationFlag)
                    }
                }

                // ===== 全屏半透明暗色遮罩（可点击关闭） =====
                View {
                    attr {
                        positionAbsolute()
                        top(0f)
                        left(0f)
                        width(screenW)
                        height(screenH)
                        backgroundColor(Color.BLACK)
                        if (ctx.animationFlag) opacity(0.15f) else opacity(0f)
                        animate(Animation.easeInOut(0.2f), ctx.animationFlag)
                    }
                    event { click { ctx.dismissWithAnimation() } }
                }

                // ===== 气泡镂空副本（在遮罩上方，让被长按的消息保持清晰可见） =====
                if (msgW > 0f && msgH > 0f) {
                    val isSelf = ctx.attr.message?.isSelf ?: false
                    val msgType = ctx.attr.message?.type ?: MessageType.TEXT
                    val msgContent = ctx.attr.message?.content ?: ""

                    View {
                        attr {
                            positionAbsolute()
                            top(msgY)
                            left(msgX)
                            width(msgW)
                            height(msgH)
                            if (ctx.animationFlag) opacity(1f) else opacity(0f)
                            animate(Animation.easeInOut(0.2f), ctx.animationFlag)
                        }

                        when (msgType) {
                            MessageType.IMAGE -> {
                                View {
                                    attr {
                                        size(msgW, msgH)
                                        borderRadius(12f)
                                        backgroundColor(Color(0xFFE8E8E8))
                                    }
                                    Image {
                                        attr {
                                            size(msgW, msgH)
                                            borderRadius(12f)
                                            src(msgContent)
                                            resizeCover()
                                        }
                                    }
                                }
                            }
                            MessageType.VIDEO -> {
                                val attachment = ctx.attr.message?.attachments?.firstOrNull()
                                val thumbUrl = attachment?.thumbnailUrl?.ifEmpty { msgContent } ?: msgContent
                                View {
                                    attr {
                                        size(msgW, msgH)
                                        borderRadius(12f)
                                        backgroundColor(Color(0xFFE8E8E8))
                                    }
                                    Image {
                                        attr {
                                            size(msgW, msgH)
                                            borderRadius(12f)
                                            src(thumbUrl)
                                            resizeCover()
                                        }
                                    }
                                    View {
                                        attr {
                                            positionAbsolute()
                                            top(0f)
                                            left(0f)
                                            size(msgW, msgH)
                                            allCenter()
                                        }
                                        View {
                                            attr {
                                                size(48f, 48f)
                                                borderRadius(24f)
                                                backgroundColor(Color(0x80000000))
                                                allCenter()
                                            }
                                            Text {
                                                attr {
                                                    text("▶")
                                                    fontSize(20f)
                                                    color(Color.WHITE)
                                                    marginLeft(3f)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            MessageType.FILE -> {
                                val attachment = ctx.attr.message?.attachments?.firstOrNull()
                                val fileName = attachment?.title?.ifEmpty { msgContent } ?: msgContent
                                val fileSize = attachment?.fileSize ?: 0L
                                val fileSizeText = if (fileSize > 0) {
                                    when {
                                        fileSize < 1024 -> "${fileSize} B"
                                        fileSize < 1024 * 1024 -> "${(fileSize / 1024.0 * 10).toLong() / 10.0} KB"
                                        else -> "${(fileSize / (1024.0 * 1024.0) * 10).toLong() / 10.0} MB"
                                    }
                                } else ""
                                val mimeType = attachment?.mimeType
                                    ?: ctx.attr.message?.extra?.get("mimeType") ?: ""
                                val fileStyle = getFileStyle(mimeType)
                                View {
                                    attr {
                                        size(msgW, msgH)
                                        backgroundColor(Color(ctx.attr.bubbleOtherBubbleColor))
                                        borderRadius(12f)
                                        padding(12f, 14f, 12f, 14f)
                                        flexDirectionRow()
                                        alignItems(FlexAlign.CENTER)
                                        boxShadow(BoxShadow(0f, 1f, 6f, Color(0x1A000000)))
                                        overflow(false)
                                    }
                                    View {
                                        attr {
                                            size(40f, 40f)
                                            borderRadius(10f)
                                            backgroundColor(Color(fileStyle.second))
                                            allCenter()
                                            marginRight(12f)
                                        }
                                        Text {
                                            attr {
                                                text(fileStyle.first)
                                                fontSize(if (fileStyle.first.length > 3) 10f else 12f)
                                                fontWeightBold()
                                                color(Color.WHITE)
                                            }
                                        }
                                    }
                                    View {
                                        attr {
                                            flex(1f)
                                            flexDirection(FlexDirection.COLUMN)
                                        }
                                        Text {
                                            attr {
                                                text(fileName)
                                                fontSize(14f)
                                                color(Color(ctx.attr.bubbleOtherTextColor))
                                                lines(2)
                                            }
                                        }
                                        if (fileSizeText.isNotEmpty()) {
                                            Text {
                                                attr {
                                                    text(fileSizeText)
                                                    fontSize(11f)
                                                    color(Color(0xFF999999))
                                                    marginTop(2f)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            else -> {
                                View {
                                    attr {
                                        size(msgW, msgH)
                                        if (isSelf) {
                                            backgroundLinearGradient(
                                                Direction.TO_RIGHT,
                                                ColorStop(Color(ctx.attr.bubblePrimaryColor), 0f),
                                                ColorStop(Color(ctx.attr.bubblePrimaryGradientEndColor), 1f)
                                            )
                                            borderRadius(BorderRectRadius(12f, 2f, 12f, 12f))
                                        } else {
                                            backgroundColor(Color(ctx.attr.bubbleOtherBubbleColor))
                                            borderRadius(BorderRectRadius(2f, 12f, 12f, 12f))
                                        }
                                        boxShadow(if (isSelf) BoxShadow(0f, 1f, 6f, Color(0x334F8FFF)) else BoxShadow(0f, 1f, 6f, Color(0x1A000000)))
                                        padding(ctx.attr.bubblePaddingV, ctx.attr.bubblePaddingH, ctx.attr.bubblePaddingV, ctx.attr.bubblePaddingH)
                                        overflow(false)
                                    }
                                    Text {
                                        attr {
                                            text(msgContent)
                                            fontSize(ctx.attr.bubbleFontSize)
                                            color(if (isSelf) Color(ctx.attr.bubbleSelfTextColor) else Color(ctx.attr.bubbleOtherTextColor))
                                            lineHeight(ctx.attr.bubbleLineHeight)
                                        }
                                    }
                                }
                            }
                        }

                        // 点击气泡副本也关闭菜单
                        event { click { ctx.dismissWithAnimation() } }
                    }
                }

                // ===== 菜单卡片（紧贴气泡上方/下方） =====
                View {
                    attr {
                        positionAbsolute()
                        top(menuTop)
                        left(menuLeft)
                        width(menuWidth)
                        backgroundColor(Color(ctx.attr.menuBgColor))
                        borderRadius(16f)
                        boxShadow(BoxShadow(0f, 8f, 30f, Color(0x40000000)))
                        // 入场动画
                        if (ctx.animationFlag) {
                            opacity(1f)
                            transform(Scale(1f, 1f))
                        } else {
                            opacity(0f)
                            transform(Scale(0.85f, 0.85f))
                        }
                        animate(Animation.easeInOut(0.2f), ctx.animationFlag)
                    }
                    event {
                        // 获取菜单卡片的实际布局高度，精确定位
                        layoutFrameDidChange { frame ->
                            if (frame.height > 0f && frame.height != ctx.menuActualHeight) {
                                ctx.menuActualHeight = frame.height
                            }
                        }
                    }

                    // 快捷反应 emoji 行
                    if (ctx.attr.showQuickReactions) {
                        View {
                            attr {
                                flexDirectionRow()
                                justifyContent(FlexJustifyContent.SPACE_AROUND)
                                padding(10f, 6f, 6f, 6f)
                                borderBottom(Border(0.5f, BorderStyle.SOLID, Color(ctx.attr.dividerColor)))
                            }
                            for (emoji in ctx.attr.quickReactions) {
                                View {
                                    attr {
                                        size(36f, 36f)
                                        allCenter()
                                        borderRadius(18f)
                                    }
                                    Text {
                                        attr {
                                            text(emoji)
                                            fontSize(20f)
                                        }
                                    }
                                    event {
                                        click {
                                            ctx.event.onReactionSelect?.invoke(emoji)
                                            ctx.dismissWithAnimation()
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 操作列表
                    for ((actionIndex, action) in visibleActions.withIndex()) {
                        View {
                            attr {
                                flexDirectionRow()
                                alignItems(FlexAlign.CENTER)
                                padding(13f, 16f, 13f, 16f)
                                if (actionIndex < visibleActions.size - 1) {
                                    borderBottom(Border(0.5f, BorderStyle.SOLID, Color(ctx.attr.dividerColor)))
                                }
                            }
                            Text {
                                attr {
                                    text(action.label)
                                    fontSize(15f)
                                    flex(1f)
                                    if (action.isDestructive) {
                                        color(Color(ctx.attr.destructiveColor))
                                    } else {
                                        color(Color(ctx.attr.textColor))
                                    }
                                }
                            }
                            event {
                                click {
                                    ctx.event.onActionClick?.invoke(action)
                                    ctx.dismissWithAnimation()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun viewDidLayout() {
        super.viewDidLayout()
        setTimeout(16) {
            animationFlag = true
        }
    }

    private var isDismissing = false

    private fun dismissWithAnimation() {
        if (isDismissing) return
        isDismissing = true
        animationFlag = false
        setTimeout(200) {
            event.onDismiss?.invoke()
        }
    }
}

class ChatMessageOptionsAttr : ComposeAttr() {
    /** 目标消息 */
    var message: ChatMessage? by observable(null)
    /** 操作列表 */
    var actions: List<MessageAction> by observable(emptyList())
    /** 是否显示快捷反应行 */
    var showQuickReactions: Boolean by observable(true)
    /** 快捷反应 emoji 列表 */
    var quickReactions: List<String> by observable(DEFAULT_QUICK_REACTIONS)

    // ---- 视觉配置 ----
    /** 菜单背景色（深色磨砂风格） */
    var menuBgColor: Long by observable(0xF2FFFFFF)
    /** 文字颜色 */
    var textColor: Long by observable(0xFF1A1A1A)
    /** 危险操作文字颜色 */
    var destructiveColor: Long by observable(0xFFFF3B30)
    /** 分隔线颜色 */
    var dividerColor: Long by observable(0x1A000000)

    // ---- 位置信息（被长按消息气泡在页面坐标系中的精确位置，不包含头像等） ----
    /** 气泡左上角 X */
    var targetX: Float by observable(0f)
    /** 气泡左上角 Y */
    var targetY: Float by observable(0f)
    /** 气泡宽度 */
    var targetWidth: Float by observable(0f)
    /** 气泡高度 */
    var targetHeight: Float by observable(0f)
    /** 屏幕宽度 */
    var screenWidth: Float by observable(375f)
    /** 屏幕高度 */
    var screenHeight: Float by observable(812f)

    // ---- 气泡镂空：气泡样式信息（用于在遮罩层上方复制一份气泡副本） ----
    /** 自己的消息主色（渐变起始色） */
    var bubblePrimaryColor: Long by observable(0xFF4F8FFF)
    /** 自己的消息渐变结束色 */
    var bubblePrimaryGradientEndColor: Long by observable(0xFF6C5CE7)
    /** 对方消息气泡背景色 */
    var bubbleOtherBubbleColor: Long by observable(0xFFFFFFFF)
    /** 对方消息文字颜色 */
    var bubbleOtherTextColor: Long by observable(0xFF333333)
    /** 自己消息文字颜色 */
    var bubbleSelfTextColor: Long by observable(0xFFFFFFFF)
    /** 气泡内水平 padding */
    var bubblePaddingH: Float by observable(12f)
    /** 气泡内垂直 padding */
    var bubblePaddingV: Float by observable(10f)
    /** 消息文字大小 */
    var bubbleFontSize: Float by observable(15f)
    /** 消息行高 */
    var bubbleLineHeight: Float by observable(22f)
}

class ChatMessageOptionsEvent : ComposeEvent() {
    /** 操作点击回调 */
    var onActionClick: ((MessageAction) -> Unit)? = null
    /** 反应选择回调 */
    var onReactionSelect: ((String) -> Unit)? = null
    /** 关闭菜单回调 */
    var onDismiss: (() -> Unit)? = null
}

fun ViewContainer<*, *>.ChatMessageOptions(init: ChatMessageOptionsView.() -> Unit) {
    addChild(ChatMessageOptionsView(), init)
}

private fun getFileStyle(mimeType: String): Pair<String, Long> {
    return when {
        mimeType.contains("pdf") -> "PDF" to 0xFFE5484D
        mimeType.contains("word") || mimeType.contains("document") -> "DOC" to 0xFF4A90D9
        mimeType.contains("excel") || mimeType.contains("spreadsheet") -> "XLS" to 0xFF30A46C
        mimeType.contains("powerpoint") || mimeType.contains("presentation") -> "PPT" to 0xFFE5734A
        mimeType.startsWith("image/") -> "IMG" to 0xFF9B59B6
        mimeType.startsWith("video/") -> "VID" to 0xFF6C5CE7
        mimeType.startsWith("audio/") -> "MP3" to 0xFFE08C3B
        mimeType.contains("zip") || mimeType.contains("rar") || mimeType.contains("tar") -> "ZIP" to 0xFF7C8894
        mimeType.startsWith("text/") -> "TXT" to 0xFF8E8E93
        else -> "FILE" to 0xFF8E8E93
    }
}
