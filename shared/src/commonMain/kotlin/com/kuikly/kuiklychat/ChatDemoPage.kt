package com.kuikly.kuiklychat

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.*
import com.kuikly.kuiklychat.base.BasePager
import com.tencent.kuiklybase.chat.*

/**
 * 聊天组件 Demo 页面
 *
 * 演示 KuiklyChatComponent 组件的完整功能：
 * - 消息列表展示（支持发送方/接收方区分）
 * - 文本消息发送
 * - 系统消息展示
 * - 自动回复模拟
 * - 键盘弹出时自动滚动
 */
@Page("chat", supportInLocal = true)
internal class ChatDemoPage : BasePager() {


    var messageList by observableList<ChatMessage>()

    init {
        messageList.addAll(createInitialMessages())
    }

    // ChatSession 组件引用
    private lateinit var chatSessionRef: ViewRef<ChatSessionView>

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flex(1f)
            }

            // 使用聊天会话组件
            ChatSession {
                ref { ctx.chatSessionRef = it }
                attr {
                    title = ctx.pageData.params.optString("chatTitle").ifEmpty { "KuiklyChat" }
                    showNavigationBar = true
                    showBackButton = true
                    messages = ctx.messageList
                    inputPlaceholder = "输入消息..."
                    // 主题色配置
                    primaryColor = 0xFF4F8FFF
                    primaryGradientEndColor = 0xFF6C5CE7
                    backgroundColor = 0xFFF0F2F5
                }
                event {
                    onSendMessage = { text ->
                        ctx.onSendMessage(text)
                    }
                    onBackClick = {
                        ctx.acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
                    }
                    onMessageLongPress = { message ->
                        // 可扩展长按功能（如复制、删除等）
                    }
                }
            }
        }
    }

    /**
     * 处理发送消息
     */
    private fun onSendMessage(text: String) {
        // 添加用户消息
        val userMessage = ChatMessageHelper.createTextMessage(
            content = text,
            isSelf = true,
            senderName = "我"
        )
        messageList.add(userMessage)

        // 滚动到底部
        setTimeout(100) {
            chatSessionRef.view?.scrollToBottom(true)
        }

        // 模拟自动回复（延迟 1 秒）
        setTimeout(1000) {
            val reply = createAutoReply(text)
            messageList.add(reply)
            setTimeout(100) {
                chatSessionRef.view?.scrollToBottom(true)
            }
        }
    }

    /**
     * 创建初始消息列表
     */
    private fun createInitialMessages(): List<ChatMessage> {
        return listOf(
            ChatMessageHelper.createTextMessage(
                content = "你好！欢迎使用 KuiklyChat 💬",
                isSelf = false,
                senderName = "小助手",
                senderAvatar = ASSISTANT_AVATAR
            ),
            ChatMessageHelper.createTextMessage(
                content = "这是一个基于 KuiklyUI 框架构建的聊天组件示例",
                isSelf = false,
                senderName = "小助手",
                senderAvatar = ASSISTANT_AVATAR
            ),
            ChatMessageHelper.createTextMessage(
                content = "你好！这个聊天界面看起来不错 👍",
                isSelf = true,
                senderName = "我"
            ),
            ChatMessageHelper.createTextMessage(
                content = "支持文本消息发送和接收，还有系统提示消息哦～",
                isSelf = false,
                senderName = "小助手",
                senderAvatar = ASSISTANT_AVATAR
            ),
            ChatMessageHelper.createTextMessage(
                content = "Kuikly 跨端框架真的很强大！",
                isSelf = true,
                senderName = "我"
            )
        )
    }

    /**
     * 创建自动回复消息
     */
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
            senderAvatar = ASSISTANT_AVATAR
        )
    }

    companion object {
        const val ASSISTANT_AVATAR =
            "https://vfiles.gtimg.cn/wuji_dashboard/wupload/xy/starter/62394e19.png"
    }
}
