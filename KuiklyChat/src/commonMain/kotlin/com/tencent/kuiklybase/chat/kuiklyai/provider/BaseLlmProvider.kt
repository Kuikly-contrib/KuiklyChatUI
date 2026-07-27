package com.tencent.kuiklybase.chat.kuiklyai.provider

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * commonMain 里没有系统时钟，时间戳由宿主注入。
 *
 * 默认返回 0——UI 层遇到 0 时不显示时间，而不是显示一个假的 1970。
 */
fun interface LlmClock {
    fun nowMillis(): Long

    companion object {
        val Unavailable = LlmClock { 0L }
    }
}

/**
 * 会话内唯一 ID 生成器。
 *
 * 高并发或跨端同步场景请由服务端下发 ID，这里只保证单会话内不重复。
 */
class LlmIdGenerator(private val prefix: String = "llm") {
    private var counter = 0L

    fun next(): String {
        counter++
        return "${prefix}_$counter"
    }
}

/**
 * [LlmProvider] 的骨架实现：把"历史簿记"和"流式累积"这两件所有 provider
 * 都要重复写的事收敛到一处，子类只需实现 [streamCompletion]。
 *
 * 子类拿到的是**已经拼好上下文的消息列表**，只负责翻译成自己的协议、
 * 发请求、把响应切成增量文本片段。历史的增删改、失败态标记、监听器通知
 * 全部由基类处理。
 */
abstract class BaseLlmProvider(
    protected val clock: LlmClock = LlmClock.Unavailable,
    idPrefix: String = "llm"
) : LlmProvider {

    private val messages = mutableListOf<LlmChatMessage>()
    private val listeners = mutableListOf<LlmHistoryListener>()
    protected val ids = LlmIdGenerator(idPrefix)

    override val history: List<LlmChatMessage> get() = messages.toList()

    override fun setHistory(messages: List<LlmChatMessage>) {
        this.messages.clear()
        this.messages.addAll(messages)
        notifyHistoryChanged()
    }

    override fun addHistoryListener(listener: LlmHistoryListener): LlmSubscription {
        listeners.add(listener)
        return LlmSubscription { listeners.remove(listener) }
    }

    /**
     * 子类实现：把上下文翻译成自己的协议并发起流式请求。
     *
     * @param context 完整对话上下文，最后一条一定是本轮的用户消息
     * @return 增量文本片段流（不是累积全文）
     */
    protected abstract fun streamCompletion(context: List<LlmChatMessage>): Flow<String>

    override fun generateStream(
        prompt: String,
        attachments: List<LlmAttachment>
    ): Flow<String> {
        val oneShot = userMessage(prompt, attachments)
        return streamCompletion(listOf(oneShot))
    }

    override fun sendMessageStream(
        prompt: String,
        attachments: List<LlmAttachment>
    ): Flow<String> = flow {
        val user = userMessage(prompt, attachments)
        appendMessage(user)

        // 上下文在插入占位回复**之前**取快照：否则会把一条空的 assistant
        // 消息也发给模型，多数端点会因此报 400 或产出空回复。
        val context = messages.toList()

        val replyId = nextMessageId()
        appendMessage(
            LlmChatMessage(
                id = replyId,
                origin = LlmMessageOrigin.LLM,
                createdAt = clock.nowMillis(),
                state = LlmMessageState.STREAMING
            )
        )

        val accumulated = StringBuilder()
        try {
            streamCompletion(context).collect { delta ->
                accumulated.append(delta)
                updateMessage(replyId) { it.copy(text = accumulated.toString()) }
                emit(delta)
            }
            updateMessage(replyId) {
                it.copy(text = accumulated.toString(), state = LlmMessageState.DONE)
            }
        } catch (e: CancellationException) {
            // 用户点了"停止生成"。必须显式落终态——否则这条消息永远停在
            // STREAMING，UI 上就是一个再也不会结束的加载动画。
            updateMessage(replyId) {
                it.copy(text = accumulated.toString(), state = LlmMessageState.CANCELED)
            }
            throw e
        } catch (e: LlmProviderException) {
            updateMessage(replyId) {
                it.copy(
                    text = accumulated.toString(),
                    state = LlmMessageState.FAILED,
                    error = e.message.orEmpty(),
                    extra = it.extra + (LlmChatMessage.ERROR_CODE_EXTRA_KEY to e.code.name)
                )
            }
            throw e
        } catch (e: Throwable) {
            // 宿主的 LlmTransport 实现未必只抛 LlmProviderException。
            // 兜底同样要落终态，理由同上。
            updateMessage(replyId) {
                it.copy(
                    text = accumulated.toString(),
                    state = LlmMessageState.FAILED,
                    error = e.message.orEmpty(),
                    extra = it.extra + (
                        LlmChatMessage.ERROR_CODE_EXTRA_KEY to LlmErrorCode.UNKNOWN.name
                    )
                )
            }
            throw e
        }
    }

    // ── 历史操作（供子类与基类共用） ──────────────────────────────────

    protected fun userMessage(
        prompt: String,
        attachments: List<LlmAttachment>
    ): LlmChatMessage = LlmChatMessage(
        id = nextMessageId(),
        origin = LlmMessageOrigin.USER,
        text = prompt,
        attachments = attachments,
        createdAt = clock.nowMillis(),
        state = LlmMessageState.DONE
    )

    /**
     * 恢复历史后生成器会从 1 重新计数，因此必须避开历史中已有的 ID。
     * 否则新回复可能更新到同 ID 的旧消息上。
     */
    private fun nextMessageId(): String {
        var candidate: String
        do {
            candidate = ids.next()
        } while (messages.any { it.id == candidate })
        return candidate
    }

    protected fun appendMessage(message: LlmChatMessage) {
        messages.add(message)
        notifyHistoryChanged()
    }

    protected fun updateMessage(id: String, transform: (LlmChatMessage) -> LlmChatMessage) {
        val index = messages.indexOfFirst { it.id == id }
        if (index < 0) return
        messages[index] = transform(messages[index])
        notifyHistoryChanged()
    }

    private fun notifyHistoryChanged() {
        val snapshot = messages.toList()
        listeners.toList().forEach { it.onHistoryChanged(snapshot) }
    }
}
