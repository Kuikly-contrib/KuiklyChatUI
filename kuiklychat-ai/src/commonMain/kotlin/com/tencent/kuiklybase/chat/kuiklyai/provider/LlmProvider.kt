package com.tencent.kuiklybase.chat.kuiklyai.provider

import kotlinx.coroutines.flow.Flow

/**
 * KuiklyAI 的唯一后端扩展点。
 *
 * 对齐 Flutter AI Toolkit 的 `LlmProvider`：UI 层只认这个接口，
 * 任何模型服务（AG-UI Agent、OpenAI 兼容端点、自研网关、本地模型）
 * 都以实现类的形式接入，协议细节不向 UI 泄漏。
 *
 * ## 与 Flutter AI Toolkit 的差异
 *
 * Flutter 官方只提供 `FirebaseProvider` 一个实现，且从 v0.9.0 起强制绑定
 * Firebase 项目——抽象在接口层成立，实现层却把用户锁死在单一云厂商。
 * KuiklyAI 刻意避开这一点：
 *
 * - 内置实现全部不依赖任何厂商 SDK，只依赖 [LlmTransport] 这层由宿主注入的传输契约
 * - [OpenAiCompatibleProvider] 的 `baseUrl` 可配，任何 OpenAI 兼容端点都能直连
 * - [EchoLlmProvider] 让 Demo / 单测在零网络环境下可跑
 *
 * ## 实现约定
 *
 * - [generateStream] 是**无状态**的一次性调用，不读也不写 [history]
 * - [sendMessageStream] 是**有状态**的多轮对话，需要把用户消息与模型回复写入 [history]
 * - 两者返回的 [Flow] 都是冷流，只有被 collect 时才真正发起请求；
 *   collect 被取消时应中断底层请求
 * - Flow 发射的是**增量文本片段**（token / chunk），不是累积全文
 */
public interface LlmProvider {

    /** Actual attachment types supported by the provider's wire format. */
    public val capabilities: LlmProviderCapabilities
        get() = LlmProviderCapabilities()

    /** 当前会话历史。实现方需保证返回只读快照，外部不可变更。 */
    public val history: List<LlmChatMessage>

    /**
     * 整体替换会话历史。
     *
     * 用于从持久化存储恢复对话（见
     * [com.tencent.kuiklybase.chat.kuiklyai.history.LlmChatSerializer]），
     * 或在切换会话时重置上下文。
     */
    public fun setHistory(messages: List<LlmChatMessage>)

    /**
     * 一次性生成，**不进历史**。
     *
     * 适合"总结这段文字""给这张图起个标题"这类脱离对话上下文的场景。
     */
    public fun generateStream(
        prompt: String,
        attachments: List<LlmAttachment> = emptyList()
    ): Flow<String>

    /**
     * 多轮对话发送，**读写历史**。
     *
     * 实现方应在 collect 开始时把用户消息追加进 [history]，
     * 并在流结束（或失败）后写入模型回复。
     */
    public fun sendMessageStream(
        prompt: String,
        attachments: List<LlmAttachment> = emptyList()
    ): Flow<String>

    /** 注册历史变更监听，返回取消注册的句柄。 */
    public fun addHistoryListener(listener: LlmHistoryListener): LlmSubscription
}

/** 历史变更回调。 */
public fun interface LlmHistoryListener {
    public fun onHistoryChanged(history: List<LlmChatMessage>)
}

/** 可取消的订阅句柄。 */
public fun interface LlmSubscription {
    public fun cancel()
}

/**
 * provider 抛出的结构化异常。
 *
 * 带 [code] 是为了让 UI 能区分"可重试"（超时、限流）和"不可重试"（鉴权失败、
 * 内容被拒），而不是只能展示一段裸文案。
 */
public class LlmProviderException(
    public val code: LlmErrorCode,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/** 错误分类。 */
public enum class LlmErrorCode {
    /** 网络不可达 / 连接中断 */
    NETWORK,

    /** 请求超时 */
    TIMEOUT,

    /** 鉴权失败（key 无效、额度耗尽） */
    AUTH,

    /** 被限流 */
    RATE_LIMIT,

    /** 请求参数非法 */
    INVALID_REQUEST,

    /** 模型或内容安全策略拒绝 */
    REJECTED,

    /** 响应无法解析 */
    MALFORMED_RESPONSE,

    /** Host transport could not keep up with the producer. */
    TRANSPORT_OVERFLOW,

    /** 服务端错误 */
    SERVER,

    /** 用户主动取消 */
    CANCELED,

    /** 其他 */
    UNKNOWN;

    /** 是否值得让用户点"重试" */
    public val retryable: Boolean
        get() = this == NETWORK || this == TIMEOUT || this == RATE_LIMIT || this == SERVER
}
