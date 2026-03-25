package com.kuikly.kuiklychat

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.*
import com.kuikly.kuiklychat.base.BasePager
import com.kuikly.kuiklychat.base.BridgeModule
import com.tencent.kuiklybase.chat.*

/**
 * 聊天组件 Demo 页面
 *
 * 展示 ChatSession 组件的完整功能。
 * 消息数据从本地 JSON 文件读取，模拟真实的网络接口调用场景：
 * - 初始消息：加载 `chat_data/initial_messages.json`
 * - 历史消息：分页加载 `chat_data/history_page{N}.json`
 * - JSON 格式参考常见 IM 接口的标准响应格式
 */
@Page("chat", supportInLocal = true)
internal class ChatDemoPage : BasePager() {

    var messageList by observableList<ChatMessage>()

    // 持有 ChatSession 配置引用
    private var chatSessionConfig: ChatSessionConfig? = null

    // 加载历史消息 Demo 相关
    private var historyPageIndex = 0  // 已加载的历史页数
    private val maxHistoryPages = 3   // 最多加载 3 页历史

    // 是否正在加载初始数据
    private var isInitialLoading by observable(true)

    override fun created() {
        super.created()
        // 从本地 JSON 文件加载初始消息（模拟网络请求）
        loadInitialMessages()
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
                // ChatSession - 消息列表容器（含内置 MessageComposer + 内置操作菜单）
                // ============================
                ChatSession({ ctx.messageList }) {
                    title = chatTitle
                    showBackButton = true
                    selfAvatarUrl = USER_AVATAR

                    // ---- 启用内置 MessageComposer ----
                    showMessageComposer = true
                    composerPlaceholder = "输入消息..."
                    composerSafeAreaBottom = ctx.pagerData.safeAreaInsets.bottom
                    onSendMessage = { text ->
                        ctx.sendMessage(text)
                    }

                    // ---- 启用内置操作菜单 ----
                    enableBuiltInMessageOptions = true

                    // ---- 主题配置 ----
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
                        enableMessageGrouping = true
                        messageGroupingInterval = 2 * 60 * 1000L

                        // 加载历史消息配置
                        hasMoreEarlier = true
                        onLoadEarlier = {
                            ctx.loadEarlierMessages()
                        }
                    }

                    // ---- 事件回调 ----
                    onBackClick = {
                        ctx.acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
                    }
                    onMessageClick = { message ->
                        // 处理消息点击
                    }
                    onAvatarClick = { message ->
                        // 处理头像点击（如打开用户资料页）
                    }
                    onResend = { message ->
                        // 业务处理重发逻辑
                    }
                    onReactionClick = { message, reactionType ->
                        // 处理反应点击（Demo：切换反应）
                        val idx = ctx.messageList.indexOfFirst { it.id == message.id }
                        if (idx >= 0) {
                            val existingReactions = message.reactions.toMutableList()
                            val existIdx = existingReactions.indexOfFirst { it.type == reactionType }
                            if (existIdx >= 0) {
                                val existing = existingReactions[existIdx]
                                existingReactions[existIdx] = existing.copy(
                                    count = existing.count + 1,
                                    isOwnReaction = true
                                )
                            } else {
                                existingReactions.add(ReactionItem(reactionType, 1, true))
                            }
                            ctx.messageList[idx] = message.copy(reactions = existingReactions)
                        }
                    }
                    onThreadClick = { message ->
                        // 业务方处理打开线程视图
                    }
                    onMessageAction = { message, action ->
                        when (action.key) {
                            "copy" -> {
                                // 业务方处理复制到剪贴板
                            }
                            "delete" -> {
                                val idx = ctx.messageList.indexOfFirst { it.id == message.id }
                                if (idx >= 0) {
                                    ctx.messageList[idx] = message.copy(isDeleted = true)
                                }
                            }
                            "pin" -> {
                                val idx = ctx.messageList.indexOfFirst { it.id == message.id }
                                if (idx >= 0) {
                                    ctx.messageList[idx] = message.copy(isPinned = true)
                                }
                            }
                            "unpin" -> {
                                val idx = ctx.messageList.indexOfFirst { it.id == message.id }
                                if (idx >= 0) {
                                    ctx.messageList[idx] = message.copy(isPinned = false)
                                }
                            }
                        }
                    }

                    // 保存配置引用
                    ctx.chatSessionConfig = this
                }
            }
        }
    }

    // ============================
    // 数据加载（从本地 JSON 文件读取，模拟网络请求）
    // ============================

    /**
     * 加载初始消息列表。
     *
     * 通过 BridgeModule.readAssetFile 读取本地 JSON 文件，
     * 模拟真实场景中从服务端拉取最新消息的接口调用。
     */
    private fun loadInitialMessages() {
        val bridge = acquireModule<BridgeModule>(BridgeModule.MODULE_NAME)
        bridge.readAssetFile("chat_data/initial_messages.json") { response ->
            val result = response?.optString("result") ?: ""
            if (result.isNotEmpty()) {
                val apiResponse = ChatMessageParser.parseResponse(result)
                if (apiResponse.code == 0 && apiResponse.messages.isNotEmpty()) {
                    messageList.addAll(apiResponse.messages)
                    // 根据服务端返回决定是否还有更多历史
                    chatSessionConfig?.hasMoreEarlier = apiResponse.hasMore
                }
            }
            isInitialLoading = false
        }
    }

    /**
     * 加载更早的历史消息（分页）。
     *
     * 根据当前页码读取对应的 JSON 文件（history_page1.json / history_page2.json / ...），
     * 模拟真实场景中分页拉取历史消息的接口调用。
     */
    private fun loadEarlierMessages() {
        val cfg = chatSessionConfig ?: return
        if (cfg.isLoadingEarlier) return

        cfg.isLoadingEarlier = true
        historyPageIndex++

        val assetPath = "chat_data/history_page${historyPageIndex}.json"
        val bridge = acquireModule<BridgeModule>(BridgeModule.MODULE_NAME)

        // 添加延迟模拟网络耗时
        setTimeout(500) {
            bridge.readAssetFile(assetPath) { response ->
                val result = response?.optString("result") ?: ""
                if (result.isNotEmpty()) {
                    val apiResponse = ChatMessageParser.parseResponse(result)
                    if (apiResponse.code == 0 && apiResponse.messages.isNotEmpty()) {
                        messageList.addAll(0, apiResponse.messages)
                    }
                    // 服务端返回 has_more=false 或者已加载到最大页数
                    if (!apiResponse.hasMore || historyPageIndex >= maxHistoryPages) {
                        cfg.hasMoreEarlier = false
                    }
                } else {
                    // 读取失败，停止加载更多
                    cfg.hasMoreEarlier = false
                }
                cfg.isLoadingEarlier = false
            }
        }
    }

    // ============================
    // 发送消息
    // ============================

    private fun sendMessage(text: String) {
        val trimmedText = text.trim()
        if (trimmedText.isNotEmpty()) {
            // 自动携带引用回复
            val replyTo = chatSessionConfig?._replyingToMessage
            val userMessage = ChatMessageHelper.createTextMessage(
                content = trimmedText,
                isSelf = true,
                senderName = "我",
                status = MessageStatus.SENT
            ).let { msg ->
                if (replyTo != null) {
                    msg.copy(quotedMessage = replyTo)
                } else {
                    msg
                }
            }
            messageList.add(userMessage)

            // 发送后取消回复状态
            chatSessionConfig?.cancelReply()

            // 模拟自动回复
            setTimeout(1000) {
                val reply = createAutoReply(trimmedText)
                messageList.add(reply)
            }
        }
    }

    private fun createAutoReply(userMessage: String): ChatMessage {
        val replies = listOf(
            "收到你的消息：\"$userMessage\"",
            "好的，我知道了～",
            "这是一条自动回复消息",
            "Kuikly 框架太好用了！",
            "你说的很有道理",
            "让我想想...",
            "哈哈，有意思！"
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
