package com.tencent.kuiklybase.chat.session

import com.tencent.kuiklybase.chat.model.*


/**
 * Slot 配置（参考 Stream Chat 的 Slot API / Content 自定义层级）
 *
 * 集中管理所有可替换渲染插槽。
 * 对应 Stream Chat 的 itemContent / emptyContent / loadingContent / helperContent 等概念。
 */
class ChatSlotOptions {
    // ---- 消息类型独立 Slot（参考 Stream Chat 的 attachmentFactories） ----

    /**
     * 自定义文本消息气泡渲染（带上下文）。
     *
     * 参考 Stream Chat 的 itemContent Slot：提供 MessageContext 以支持上下文感知渲染
     * （连续消息合并头像、缩小间距等）。
     */
    var textBubble: MessageBubbleSlot? = null

    /**
     * 自定义图片消息渲染（带上下文）。
     *
     * 默认渲染：带圆角的图片 + 加载占位。
     * 设置后替换默认图片消息渲染。
     */
    var imageBubble: MessageBubbleSlot? = null

    /**
     * 自定义消息类型渲染（MessageType.CUSTOM）。
     *
     * 用于渲染语音消息、文件消息、卡片消息等自定义类型。
     * 必须设置此 Slot 才能渲染 CUSTOM 类型消息。
     */
    var customBubble: MessageBubbleSlot? = null

    /**
     * 统一消息气泡渲染（设置后覆盖 textBubble/imageBubble，所有非系统消息都使用此 Slot）。
     *
     * 参考 Stream Chat MessageList 的 itemContent：完全自定义消息项渲染。
     * 优先级高于 textBubble/imageBubble/customBubble。
     */
    var messageBubble: MessageBubbleSlot? = null

    /**
     * 自定义系统消息渲染。
     */
    var systemMessage: SimpleBubbleSlot? = null

    // ---- 列表状态 Slot（参考 Stream Chat MessageList 的对应 Slot） ----

    /**
     * 消息列表为空时的占位渲染。
     *
     * 参考 Stream Chat MessageList 的 emptyContent。
     */
    var emptyContent: ViewSlot? = null

    /**
     * 列表加载中（首次加载）时的渲染。
     *
     * 参考 Stream Chat MessageList 的 loadingContent。
     */
    var loadingContent: ViewSlot? = null

    /**
     * 加载更多历史消息时的顶部加载指示器。
     *
     * 参考 Stream Chat MessageList 的 loadingMoreContent。
     */
    var loadingMoreContent: ViewSlot? = null

    /**
     * 列表辅助内容（如"滚动到底部"悬浮按钮、"N条新消息"提示）。
     *
     * 参考 Stream Chat MessageList 的 helperContent。
     */
    var helperContent: ViewSlot? = null

    // ---- 导航栏 Slot ----

    /**
     * 自定义导航栏渲染。设置此 Slot 后将完全替换默认导航栏。
     */
    var navigationBar: NavigationBarSlot? = null

    /**
     * 导航栏右侧操作区域 Slot（如"更多"按钮、搜索按钮等）。
     */
    var navigationBarTrailing: ViewSlot? = null

    // ---- 新增 Slot（P0 功能） ----

    /**
     * 自定义日期分隔符渲染。
     */
    var dateSeparator: DateSeparatorSlot? = null

    /**
     * 自定义正在输入指示器渲染。
     */
    var typingIndicator: TypingIndicatorSlot? = null

    /**
     * 自定义消息反应栏渲染。
     */
    var reactionBar: ReactionBarSlot? = null

    /**
     * 自定义消息操作菜单渲染。
     */
    var messageOptions: MessageOptionsSlot? = null

    // ---- MessageComposer Slots（参考 Stream Chat MessageComposer 的 Slot API） ----

    /**
     * 完全替换默认 MessageComposer。
     * 设置后，所有其他 composer* Slot 失效，由使用方自行实现整个输入框。
     * 参考 Stream Chat 的 stateless MessageComposer 模式。
     */
    var messageComposer: MessageComposerSlot? = null

    /**
     * 输入框左侧集成按钮区域（附件选择、语音等）。
     * 参考 Stream Chat MessageComposer 的 integrations Slot。
     * 默认为空（不显示左侧按钮）。
     */
    var composerIntegrations: ComposerIntegrationsSlot? = null

    /**
     * 核心输入区域。替换默认的 Input 组件。
     * 参考 Stream Chat MessageComposer 的 input Slot。
     */
    var composerInput: ComposerInputSlot? = null

    /**
     * 输入框右侧内容（默认为发送按钮）。
     * 参考 Stream Chat MessageComposer 的 trailingContent Slot。
     */
    var composerTrailing: ComposerTrailingSlot? = null

    /**
     * 输入框顶部内容（如"正在回复..."、"正在编辑..."提示条）。
     * 参考 Stream Chat MessageComposer 的 headerContent Slot。
     */
    var composerHeader: ComposerHeaderSlot? = null

    /**
     * 输入框底部内容（如工具栏、表情面板入口）。
     * 参考 Stream Chat MessageComposer 的 footerContent Slot。
     */
    var composerFooter: ComposerFooterSlot? = null
}
