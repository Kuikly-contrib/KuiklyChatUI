package com.kuikly.kuiklychat

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.*
import com.tencent.kuikly.core.views.layout.Row
import com.tencent.kuikly.core.directives.vif
import com.kuikly.kuiklychat.base.BasePager
import com.tencent.kuiklybase.chat.*

/**
 * 聊天组件 Demo 页面
 *
 * 展示 ChatSession 组件的完整功能：
 * - theme {} DSL 配置主题（浅色/深色双主题切换）
 * - 消息分组（连续同一发送者合并头像）
 * - 图片消息 / 文本消息 / 系统消息
 * - 消息反应（Reactions）
 * - 消息编辑/删除标记
 * - 消息置顶标记
 * - 日期分隔符
 * - 正在输入指示器
 * - 长按消息操作菜单
 * - 消息渲染工厂（MessageRendererFactory）
 */
@Page("chat", supportInLocal = true)
internal class ChatDemoPage : BasePager() {

    var messageList by observableList<ChatMessage>()

    // 输入框
    private var currentInputText: String = ""
    private lateinit var inputViewRef: ViewRef<InputView>

    // 持有 ChatSession 配置引用
    private var chatSessionConfig: ChatSessionConfig? = null



    // 消息操作菜单状态
    private var showMessageOptions by observable(false)
    private var optionsTargetMessage: ChatMessage? = null
    // 被长按消息在页面坐标系中的位置
    private var targetMsgX: Float = 0f
    private var targetMsgY: Float = 0f
    private var targetMsgW: Float = 0f
    private var targetMsgH: Float = 0f

    override fun created() {
        super.created()
        messageList.addAll(createInitialMessages())
    }

    override fun body(): ViewBuilder {
        val ctx = this
        val chatTitle = pageData.params.optString("chatTitle").ifEmpty { "KuiklyChat" }
        return {
            View {
                attr {
                    flex(1f)
                    flexDirection(FlexDirection.COLUMN)
                }

                // ============================
                // ChatSession - 消息列表容器
                // ============================
                ChatSession({ ctx.messageList }) {
                    title = chatTitle
                    showBackButton = true
                    selfAvatarUrl = USER_AVATAR

                    // ---- 主题配置（新 DSL 方式，参考 Stream Chat 的 ChatTheme） ----
                    theme {
                        primaryColor = 0xFF6C5CE7
                        primaryGradientEndColor = 0xFFA29BFE
                        otherBubbleColor = 0xFFF5F0FF
                        otherTextColor = 0xFF2D3436
                        selfTextColor = 0xFFFFFFFF
                    }

                    // ---- 消息列表行为配置 ----
                    messageListOptions {
                        autoScrollToBottom = true
                        showTimeGroup = true
                        timeGroupInterval = 3 * 60 * 1000L
                        // 启用消息分组（连续同一发送者的消息合并头像、缩小间距）
                        enableMessageGrouping = true
                        messageGroupingInterval = 2 * 60 * 1000L
                    }

                    // ---- 事件回调 ----
                    onBackClick = {
                        ctx.acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
                    }
                    onMessageClick = { message ->
                        // 处理消息点击
                    }
                    onMessageLongPress = { message ->
                        // 长按消息弹出操作菜单（兜底，无坐标信息时使用屏幕中央）
                        if (message.type != MessageType.SYSTEM) {
                            ctx.optionsTargetMessage = message
                            ctx.targetMsgX = 0f
                            ctx.targetMsgY = ctx.pagerData.pageViewHeight * 0.3f
                            ctx.targetMsgW = ctx.pagerData.pageViewWidth
                            ctx.targetMsgH = 50f
                            ctx.showMessageOptions = true
                        }
                    }
                    onMessageLongPressWithPosition = { message, x, y, w, h ->
                        // 长按消息弹出操作菜单（带位置信息）
                        if (message.type != MessageType.SYSTEM) {
                            ctx.optionsTargetMessage = message
                            ctx.targetMsgX = x
                            ctx.targetMsgY = y
                            ctx.targetMsgW = w
                            ctx.targetMsgH = h
                            ctx.showMessageOptions = true
                        }
                    }
                    onResend = { message ->
                        // 业务处理重发逻辑
                    }
                    onReactionClick = { message, reactionType ->
                        // 处理反应点击（Demo：切换反应）
                    }

                    // 保存配置引用
                    ctx.chatSessionConfig = this
                }


                View {
                    attr {
                        backgroundColor(Color(0xFFF8F8F8))
                        border(Border(0.5f, BorderStyle.SOLID, Color(0xFFE0E0E0)))
                    }
                    Row {
                        attr {
                            padding(8f, 12f, 8f, 12f)
                            alignItems(FlexAlign.CENTER)
                        }

                        // 输入框
                        View {
                            attr {
                                flex(1f)
                                height(36f)
                                backgroundColor(Color.WHITE)
                                borderRadius(18f)
                                border(Border(0.5f, BorderStyle.SOLID, Color(0xFFE0E0E0)))
                                flexDirectionRow()
                                alignItemsCenter()
                            }
                            Input {
                                ref { ctx.inputViewRef = it }
                                attr {
                                    flex(1f)
                                    height(36f)
                                    fontSize(15f)
                                    color(Color(0xFF333333))
                                    placeholder("输入消息...")
                                    placeholderColor(Color(0xFFBBBBBB))
                                    marginLeft(14f)
                                    marginRight(14f)
                                    returnKeyTypeSend()
                                }
                                event {
                                    textDidChange { params ->
                                        ctx.currentInputText = params.text
                                    }
                                    inputReturn { params ->
                                        if (params.text.isNotBlank()) {
                                            ctx.currentInputText = params.text
                                        }
                                        ctx.sendMessage()
                                    }
                                }
                            }
                        }

                        // 发送按钮
                        View {
                            attr {
                                minWidth(60f)
                                height(36f)
                                marginLeft(4f)
                                borderRadius(18f)
                                backgroundLinearGradient(
                                    Direction.TO_RIGHT,
                                    ColorStop(Color(0xFF6C5CE7), 0f),
                                    ColorStop(Color(0xFFA29BFE), 1f)
                                )
                                allCenter()
                                padding(0f, 12f, 0f, 12f)
                            }
                            Text {
                                attr {
                                    text("发送")
                                    fontSize(14f)
                                    fontWeightMedium()
                                    color(Color.WHITE)
                                }
                            }
                            event {
                                click {
                                    ctx.sendMessage()
                                }
                            }
                        }
                    }

                    // 底部安全区域占位
                    View {
                        attr {
                            height(ctx.pagerData.safeAreaInsets.bottom)
                            backgroundColor(Color(0xFFF8F8F8))
                        }
                    }
                }

                // ============================
                // 消息操作菜单浮层（positionAbsolute 覆盖整个页面）
                // ============================
                vif({ ctx.showMessageOptions }) {
                    View {
                        attr {
                            positionAbsolute()
                            top(0f)
                            left(0f)
                            width(ctx.pagerData.pageViewWidth)
                            height(ctx.pagerData.pageViewHeight)
                        }
                        ChatMessageOptions {
                            attr {
                                message = ctx.optionsTargetMessage
                                actions = defaultMessageActions()
                                // 传递被长按消息的位置信息
                                targetX = ctx.targetMsgX
                                targetY = ctx.targetMsgY
                                targetWidth = ctx.targetMsgW
                                targetHeight = ctx.targetMsgH
                                screenWidth = ctx.pagerData.pageViewWidth
                                screenHeight = ctx.pagerData.pageViewHeight
                                // 传递气泡样式信息（用于镂空效果）
                                bubblePrimaryColor = 0xFF6C5CE7
                                bubblePrimaryGradientEndColor = 0xFFA29BFE
                                bubbleOtherBubbleColor = 0xFFF5F0FF
                                bubbleOtherTextColor = 0xFF2D3436
                                bubbleSelfTextColor = 0xFFFFFFFF
                            }
                            event {
                                onActionClick = { action ->
                                    ctx.optionsTargetMessage?.let { msg ->
                                        ctx.chatSessionConfig?.onMessageAction?.invoke(msg, action)
                                    }
                                }
                                onReactionSelect = { emoji ->
                                    // Demo: 简单添加反应到目标消息
                                    ctx.optionsTargetMessage?.let { msg ->
                                        val idx = ctx.messageList.indexOfFirst { it.id == msg.id }
                                        if (idx >= 0) {
                                            val existingReactions = msg.reactions.toMutableList()
                                            val existIdx = existingReactions.indexOfFirst { it.type == emoji }
                                            if (existIdx >= 0) {
                                                val existing = existingReactions[existIdx]
                                                existingReactions[existIdx] = existing.copy(
                                                    count = existing.count + 1,
                                                    isOwnReaction = true
                                                )
                                            } else {
                                                existingReactions.add(ReactionItem(emoji, 1, true))
                                            }
                                            ctx.messageList[idx] = msg.copy(reactions = existingReactions)
                                        }
                                    }
                                }
                                onDismiss = {
                                    ctx.showMessageOptions = false
                                    ctx.optionsTargetMessage = null
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    private fun sendMessage() {
        val text = currentInputText.trim()
        if (text.isNotEmpty()) {
            val userMessage = ChatMessageHelper.createTextMessage(
                content = text,
                isSelf = true,
                senderName = "我",
                status = MessageStatus.SENT
            )
            messageList.add(userMessage)
            currentInputText = ""
            inputViewRef.view?.setText("")

            // 模拟自动回复（延迟 1 秒）
            setTimeout(1000) {
                val reply = createAutoReply(text)
                messageList.add(reply)
            }
        }
    }

    private fun createInitialMessages(): List<ChatMessage> {
        val baseTime = 1711267200000L

        // 先创建一些消息用于后续引用
        val welcomeMsg = ChatMessageHelper.createTextMessage(
            content = "你好！欢迎使用 KuiklyChat 🎉",
            isSelf = false,
            senderName = "小助手",
            senderId = "assistant_001",
            timestamp = baseTime,
            reactions = listOf(
                ReactionItem("👍", 3, true),
                ReactionItem("❤️", 1, false)
            )
        )

        val featureMsg = ChatMessageHelper.createTextMessage(
            content = "这是一个基于 KuiklyUI 框架构建的聊天组件示例，现已支持消息反应、引用回复、日期分隔、输入指示器等功能",
            isSelf = false,
            senderName = "小助手",
            senderId = "assistant_001",
            timestamp = baseTime + 30_000L
        )

        return listOf(
            // ---- 系统消息 + 日期分隔触发（baseTime 很早，自然会显示日期分隔符） ----
            ChatMessageHelper.createSystemMessage("以下是新的聊天", timestamp = baseTime - 1000L),

            // ---- 带反应的欢迎消息 ----
            welcomeMsg,

            // ---- 连续消息分组 Demo：同一发送者连续消息合并头像 ----
            featureMsg,

            // ---- 普通文本消息 ----
            ChatMessageHelper.createTextMessage(
                content = "你好！这个聊天界面看起来不错，功能非常丰富 👍",
                isSelf = true,
                senderName = "我",
                senderId = "user_001",
                timestamp = baseTime + 60_000L
            ),

            // ---- 时间间隔 > 3分钟，触发日期分隔符 ----
            ChatMessageHelper.createTextMessage(
                content = "支持背景图、自定义气泡颜色、时间分组、消息重发等功能哦～",
                isSelf = false,
                senderName = "小助手",
                senderId = "assistant_001",
                timestamp = baseTime + 5 * 60_000L,
                reactions = listOf(ReactionItem("🔥", 2, true))
            ),

            // ---- 连续消息分组 + 图片消息 ----
            ChatMessageHelper.createTextMessage(
                content = "现在还支持图片消息了！来看看 👇",
                isSelf = false,
                senderName = "小助手",
                senderId = "assistant_001",
                timestamp = baseTime + 5 * 60_000L + 10_000L
            ),
            ChatMessageHelper.createImageMessage(
                imageUrl = "https://picsum.photos/600/400",
                isSelf = false,
                senderName = "小助手",
                senderId = "assistant_001",
                width = 600,
                height = 400,
                timestamp = baseTime + 5 * 60_000L + 20_000L
            ),

            // ---- 已编辑消息 Demo ----
            ChatMessageHelper.createTextMessage(
                content = "Kuikly 跨端框架真的很强大！支持 Android、iOS、鸿蒙、H5 四端运行。",
                isSelf = true,
                senderName = "我",
                senderId = "user_001",
                timestamp = baseTime + 6 * 60_000L
            ).copy(isEdited = true),

            // ---- 自己发的带反应的图片 ----
            ChatMessageHelper.createImageMessage(
                imageUrl = "https://picsum.photos/400/600",
                isSelf = true,
                senderName = "我",
                senderId = "user_001",
                width = 400,
                height = 600,
                timestamp = baseTime + 6 * 60_000L + 10_000L,
                reactions = listOf(
                    ReactionItem("😍", 2, false),
                    ReactionItem("👏", 1, true)
                )
            ),

            // ---- 置顶消息 Demo ----
            ChatMessageHelper.createTextMessage(
                content = "📌 重要提醒：长按消息可以弹出操作菜单，支持复制、回复、引用、编辑、删除、置顶等操作！",
                isSelf = false,
                senderName = "小助手",
                senderId = "assistant_001",
                timestamp = baseTime + 10 * 60_000L
            ).copy(isPinned = true),

            // ---- 反应 Demo ----
            ChatMessageHelper.createTextMessage(
                content = "太实用了！这些功能组合起来体验很好 ✨",
                isSelf = true,
                senderName = "我",
                senderId = "user_001",
                timestamp = baseTime + 10 * 60_000L + 30_000L,
                reactions = listOf(ReactionItem("💯", 1, false))
            )
        )
    }

    private fun createAutoReply(userMessage: String): ChatMessage {
        val replies = listOf(
            "收到你的消息：\"$userMessage\" 😊",
            "好的，我知道了～",
            "这是一条自动回复消息",
            "Kuikly 框架太好用了！",
            "你说的很有道理 👏",
            "让我想想... 🤔",
            "哈哈，有意思！😄"
        )
        return ChatMessageHelper.createTextMessage(
            content = replies.random(),
            isSelf = false,
            senderName = "小助手",
        )
    }

    companion object {
        const val USER_AVATAR =
            "https://picsum.photos/800/1600"
    }
}
