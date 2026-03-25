package com.tencent.kuiklybase.chat

import com.tencent.kuikly.core.base.ViewContainer

/**
 * 消息渲染工厂接口（对标 Stream Chat 的 AttachmentFactory）
 *
 * 业务方可注册自定义工厂来扩展消息类型渲染，无需修改组件内部代码。
 * 渲染分发逻辑遍历 renderers 列表，第一个 canRender() 返回 true 的工厂执行渲染。
 *
 * 优先级链：messageBubble Slot > 类型独立 Slot > MessageRendererFactory > 默认内置渲染
 */
interface MessageRendererFactory {
    /**
     * 判断此工厂是否能渲染该消息
     */
    fun canRender(message: ChatMessage): Boolean

    /**
     * 执行渲染
     *
     * @param container 父容器，在其中添加组件
     * @param context 消息上下文（含前后消息、分组信息）
     * @param config 当前聊天会话配置
     */
    fun render(container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig)
}

/**
 * 内置文本消息渲染器
 *
 * 处理 MessageType.TEXT 类型消息的默认渲染。
 */
class TextMessageRenderer : MessageRendererFactory {
    override fun canRender(message: ChatMessage): Boolean {
        return message.type == MessageType.TEXT
    }

    override fun render(container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig) {
        renderDefaultBubble(container, context, config)
    }
}

/**
 * 内置图片消息渲染器
 *
 * 处理 MessageType.IMAGE 类型消息的默认渲染。
 */
class ImageMessageRenderer : MessageRendererFactory {
    override fun canRender(message: ChatMessage): Boolean {
        return message.type == MessageType.IMAGE
    }

    override fun render(container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig) {
        renderDefaultImageBubble(container, context, config)
    }
}

/**
 * 内置系统消息渲染器
 *
 * 处理 MessageType.SYSTEM 类型消息的默认渲染。
 */
class SystemMessageRenderer : MessageRendererFactory {
    override fun canRender(message: ChatMessage): Boolean {
        return message.type == MessageType.SYSTEM
    }

    override fun render(container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig) {
        container.ChatSystemMessage {
            attr {
                this.message = context.message.content
            }
        }
    }
}

/**
 * 内置视频消息渲染器
 *
 * 处理 MessageType.VIDEO 类型消息的默认渲染。
 * 渲染为带播放按钮覆盖的缩略图。
 */
class VideoMessageRenderer : MessageRendererFactory {
    override fun canRender(message: ChatMessage): Boolean {
        return message.type == MessageType.VIDEO
    }

    override fun render(container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig) {
        // 视频消息默认使用图片渲染器（显示缩略图），未来可扩展
        renderDefaultImageBubble(container, context, config)
    }
}

/**
 * 内置文件消息渲染器
 *
 * 处理 MessageType.FILE 类型消息的默认渲染。
 * 渲染为文件图标 + 文件名 + 文件大小。
 */
class FileMessageRenderer : MessageRendererFactory {
    override fun canRender(message: ChatMessage): Boolean {
        return message.type == MessageType.FILE
    }

    override fun render(container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig) {
        // 文件消息默认使用文本气泡渲染器（显示文件名），未来可扩展为卡片样式
        renderDefaultBubble(container, context, config)
    }
}

/**
 * 创建默认的内置渲染器列表
 *
 * 包含所有内置消息类型的渲染器。
 * 业务方可在此基础上追加自定义渲染器。
 */
fun defaultMessageRenderers(): MutableList<MessageRendererFactory> = mutableListOf(
    TextMessageRenderer(),
    ImageMessageRenderer(),
    SystemMessageRenderer(),
    VideoMessageRenderer(),
    FileMessageRenderer()
)
