package com.tencent.kuiklybase.chat.kuiklyai.provider

/**
 * 消息来源。对齐 Flutter AI Toolkit 的 `MessageOrigin`。
 */
public enum class LlmMessageOrigin {
    /** 用户输入 */
    USER,

    /** 模型回复 */
    LLM
}

/**
 * 附件类型。
 *
 * 只描述"是什么"，不描述"从哪来"——采集由宿主实现的
 * [com.tencent.kuiklybase.chat.kuiklyai.platform.LlmAttachmentPicker] 负责，
 * 这样 commonMain 不需要任何平台 API。
 */
public enum class LlmAttachmentKind {
    IMAGE,
    FILE,
    AUDIO
}

/**
 * 一个待发送 / 已发送的附件。
 *
 * [bytesBase64] 与 [url] 至少提供其一：
 * - 走多模态推理时用 [bytesBase64]（内联进请求体）
 * - 仅做界面回显、或后端支持按 URL 拉取时用 [url]
 */
public data class LlmAttachment(
    val id: String,
    val kind: LlmAttachmentKind,
    val name: String = "",
    val mimeType: String = "",
    val url: String = "",
    val bytesBase64: String = "",
    /** UI 展示用缩略图，不参与模型请求；为空时回退到 [url] 或 [bytesBase64]。 */
    val thumbnailBase64: String = "",
    /**
     * 本地可加载的预览地址（`file://` / `content://`）。
     * Kuikly Image 对 `data:image` 只读内存缓存，相册缩略图走这个地址才能画出来。
     */
    val localPreviewUri: String = "",
    val sizeBytes: Long = 0L
) {
    val hasInlineBytes: Boolean get() = bytesBase64.isNotEmpty()
}

/**
 * 一条对话消息。
 *
 * 这是 **provider 层** 的模型，刻意与 UI 层的
 * [com.tencent.kuiklybase.chat.model.ChatMessage] 和
 * [com.tencent.kuiklybase.chat.agentcard.AgentCard] 分开：
 * provider 只关心"谁说了什么、带了什么附件、当前是什么状态"，
 * 渲染形态（气泡 / 卡片 / Markdown 块）完全由 UI 层决定。
 */
public data class LlmChatMessage(
    val id: String,
    val origin: LlmMessageOrigin,
    val text: String = "",
    val attachments: List<LlmAttachment> = emptyList(),
    val createdAt: Long = 0L,
    val state: LlmMessageState = LlmMessageState.DONE,
    /** 失败时的错误描述，[state] 为 [LlmMessageState.FAILED] 时有意义 */
    val error: String = "",
    /** 业务自定义扩展位，序列化时原样保留 */
    val extra: Map<String, String> = emptyMap()
) {
    val isUser: Boolean get() = origin == LlmMessageOrigin.USER
    val isStreaming: Boolean get() = state == LlmMessageState.STREAMING
    val isFailed: Boolean get() = state == LlmMessageState.FAILED

    /** Provider 写入的结构化错误码；旧历史没有该字段时返回 null。 */
    val errorCode: LlmErrorCode?
        get() = extra[ERROR_CODE_EXTRA_KEY]?.let { raw ->
            LlmErrorCode.values().firstOrNull { it.name == raw }
        }

    /** 失败消息是否值得向用户提供重试操作。 */
    val isRetryableFailure: Boolean
        get() = isFailed && errorCode?.retryable == true

    /** 空文本且无附件——流式刚开始时的占位消息 */
    val isEmpty: Boolean get() = text.isEmpty() && attachments.isEmpty()

    public companion object {
        /** [extra] 中由 toolkit 保留的错误码字段。 */
        public const val ERROR_CODE_EXTRA_KEY: String = "kuiklyai.error_code"
    }
}

/**
 * 消息生命周期状态。
 */
public enum class LlmMessageState {
    /** 正在流式接收 token */
    STREAMING,

    /** 已完成 */
    DONE,

    /** 失败（网络错误、模型报错、被拒绝等） */
    FAILED,

    /** 被用户主动取消 */
    CANCELED
}
