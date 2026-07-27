package com.tencent.kuiklybase.chat.session

import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuiklybase.chat.model.*

// ============================
// Slot 类型定义（参考 Stream Chat Compose 的 Slot API 设计）
// ============================

/**
 * 消息气泡渲染 Slot（带上下文）
 *
 * 用于自定义单条消息的渲染方式。提供 MessageContext 以支持上下文感知渲染。
 * - container: 父容器，在其中添加自定义组件
 * - context: 消息上下文（含前后消息、分组信息）
 * - config: 当前聊天会话配置（可获取主题色等）
 */
typealias MessageBubbleSlot = (container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig) -> Unit

/**
 * 旧版气泡 Slot（兼容，不含上下文）
 */
typealias SimpleBubbleSlot = (container: ViewContainer<*, *>, message: ChatMessage, config: ChatSessionConfig) -> Unit

/**
 * 导航栏渲染 Slot
 *
 * 用于完全替换默认导航栏。
 * - container: 父容器
 * - config: 当前聊天会话配置
 */
typealias NavigationBarSlot = (container: ViewContainer<*, *>, config: ChatSessionConfig) -> Unit

/**
 * 通用渲染 Slot（用于导航栏右侧按钮、空消息占位等轻量定制场景）
 * - container: 父容器
 */
typealias ViewSlot = (container: ViewContainer<*, *>) -> Unit

/**
 * 时间标签格式化器
 *
 * 接收消息时间戳（毫秒），返回格式化后的时间字符串。
 * 默认实现会根据时间距离返回 "HH:mm"、"昨天 HH:mm"、"MM-dd HH:mm" 等。
 */
typealias TimeFormatter = (timestamp: Long) -> String

/**
 * 日期分隔符渲染 Slot
 *
 * 用于自定义日期分隔符的渲染方式。
 * - container: 父容器
 * - timestamp: 分隔位置的时间戳
 * - formattedDate: 格式化后的日期文字
 */
typealias DateSeparatorSlot = (container: ViewContainer<*, *>, timestamp: Long, formattedDate: String) -> Unit

/**
 * 正在输入指示器渲染 Slot
 *
 * 用于自定义输入指示器的渲染方式。
 * - container: 父容器
 * - typingUsers: 正在输入的用户名列表
 */
typealias TypingIndicatorSlot = (container: ViewContainer<*, *>, typingUsers: List<String>) -> Unit

/**
 * 消息反应栏渲染 Slot
 *
 * 用于自定义消息反应栏的渲染方式。
 * - container: 父容器
 * - reactions: 反应列表
 * - onReactionClick: 反应点击回调
 */
typealias ReactionBarSlot = (container: ViewContainer<*, *>, reactions: List<ReactionItem>, onReactionClick: (String) -> Unit) -> Unit

/**
 * 消息操作菜单渲染 Slot
 *
 * 用于自定义消息操作菜单的渲染方式。
 * - container: 父容器
 * - message: 目标消息
 * - actions: 操作列表
 * - onActionClick: 操作点击回调
 * - onDismiss: 关闭菜单回调
 */
typealias MessageOptionsSlot = (container: ViewContainer<*, *>, message: ChatMessage, actions: List<MessageAction>, onActionClick: (MessageAction) -> Unit, onDismiss: () -> Unit) -> Unit

// ============================
// MessageComposer Slot 类型定义（参考 Stream Chat Compose MessageComposer 的 Slot API）
// ============================

/**
 * 输入框整体替换 Slot
 *
 * 完全替换默认的 MessageComposer，由使用方自行实现整个输入框区域。
 * 参考 Stream Chat 的 stateless MessageComposer。
 * - container: 父容器
 * - state: 当前输入框状态
 * - onSendMessage: 发送消息回调（传入文本内容）
 */
typealias MessageComposerSlot = (container: ViewContainer<*, *>, state: MessageComposerState, onSendMessage: (String) -> Unit) -> Unit

/**
 * 输入框左侧集成按钮区域 Slot（参考 Stream Chat MessageComposer 的 integrations Slot）
 *
 * 默认为空，可以放置附件选择、语音输入等按钮。
 * - container: 父容器（Row 内部，水平排列）
 * - state: 当前输入框状态
 */
typealias ComposerIntegrationsSlot = (container: ViewContainer<*, *>, state: MessageComposerState) -> Unit

/**
 * 输入框核心输入区域 Slot（参考 Stream Chat MessageComposer 的 input Slot）
 *
 * 替换默认的 Input 组件。可以自定义输入框样式、多行输入、富文本等。
 * - container: 父容器
 * - state: 当前输入框状态
 * - onValueChange: 文本变化回调
 */
typealias ComposerInputSlot = (container: ViewContainer<*, *>, state: MessageComposerState, onValueChange: (String) -> Unit) -> Unit

/**
 * 输入框右侧内容 Slot（参考 Stream Chat MessageComposer 的 trailingContent Slot）
 *
 * 默认为发送按钮。可替换为语音/发送切换等复合组件。
 * - container: 父容器（Row 内部，水平排列）
 * - state: 当前输入框状态
 * - onSendMessage: 发送回调
 */
typealias ComposerTrailingSlot = (container: ViewContainer<*, *>, state: MessageComposerState, onSendMessage: (String) -> Unit) -> Unit

/**
 * 输入框顶部内容 Slot（参考 Stream Chat MessageComposer 的 headerContent Slot）
 *
 * 在输入框上方显示额外内容，例如「正在回复 xxx」、「正在编辑」提示条。
 * - container: 父容器
 * - state: 当前输入框状态
 */
typealias ComposerHeaderSlot = (container: ViewContainer<*, *>, state: MessageComposerState) -> Unit

/**
 * 输入框底部内容 Slot（参考 Stream Chat MessageComposer 的 footerContent Slot）
 *
 * 在输入框下方（安全区域上方）显示额外内容，例如工具栏、表情面板入口等。
 * - container: 父容器
 * - state: 当前输入框状态
 */
typealias ComposerFooterSlot = (container: ViewContainer<*, *>, state: MessageComposerState) -> Unit

/**
 * 消息输入框状态（参考 Stream Chat 的 MessageComposerState）
 *
 * 封装输入框当前的所有状态，传递给各 Slot 使用。
 */
data class MessageComposerState(
    /** 当前输入框中的文本 */
    val inputValue: String = "",
    /** 是否正在编辑某条消息（非 null 时表示编辑模式） */
    val editingMessage: ChatMessage? = null,
    /** 正在回复的消息（非 null 时表示回复模式） */
    val replyingToMessage: ChatMessage? = null,
    /** 附件列表（预留，当前未使用） */
    val attachments: List<String> = emptyList()
)
