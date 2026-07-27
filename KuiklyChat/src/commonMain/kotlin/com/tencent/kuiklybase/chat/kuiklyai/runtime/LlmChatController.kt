package com.tencent.kuiklybase.chat.kuiklyai.runtime

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmAttachmentPicker
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmAttachmentSource
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmSpeechListener
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmSpeechRecognizer
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmSpeechResult
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmSpeechSession
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmAttachment
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmChatMessage
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmErrorCode
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmProvider
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmProviderException
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmSubscription
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * KuiklyAI 的 UI 状态持有者。
 *
 * 站在 [LlmProvider]（协议）与 Compose UI（渲染）之间：
 * - 向下只调用 [LlmProvider] 的四个方法，不知道后端是什么
 * - 向上暴露 Compose 快照状态，UI 只读状态不碰 provider
 *
 * 这层的存在是为了让"发送 / 停止 / 重试 / 编辑重发 / 附件暂存 / 语音回填"
 * 这些交互逻辑有个归属地，不散落进 Composable 函数里。
 *
 * @param provider 后端实现
 * @param scope 协程作用域，通常由 `rememberCoroutineScope()` 提供
 * @param attachmentPicker 为 null 时输入区不显示附件按钮
 * @param speechRecognizer 为 null 时输入区不显示麦克风按钮
 */
class LlmChatController(
    private val provider: LlmProvider,
    private val scope: CoroutineScope,
    val attachmentPicker: LlmAttachmentPicker? = null,
    val speechRecognizer: LlmSpeechRecognizer? = null
) {

    /** 会话消息，随 provider 历史变化自动刷新 UI */
    private val _messages = mutableStateListOf<LlmChatMessage>()
    val messages: List<LlmChatMessage> get() = _messages

    /** 输入框当前文本 */
    var inputText by mutableStateOf("")
        private set

    /** 已选中、尚未随消息发出的附件 */
    private val _pendingAttachments = mutableStateListOf<LlmAttachment>()
    val pendingAttachments: List<LlmAttachment> get() = _pendingAttachments

    /** 是否正在生成回复（决定发送按钮变成"停止"） */
    var isGenerating by mutableStateOf(false)
        private set

    /** 是否正在录音 */
    var isListening by mutableStateOf(false)
        private set

    /** 宿主已注入且当前设备确认可用时才展示语音入口。 */
    var isSpeechAvailable by mutableStateOf(false)
        private set

    /** 最近一次错误，展示后由 UI 调用 [dismissError] 清除 */
    var lastError by mutableStateOf<LlmChatError?>(null)
        private set

    /** 能否发送：有内容且当前不在生成中 */
    val canSend: Boolean
        get() = !isGenerating &&
            (inputText.isNotBlank() || _pendingAttachments.isNotEmpty())

    private var streamJob: Job? = null
    private var speechSession: LlmSpeechSession? = null
    private val historySubscription: LlmSubscription

    init {
        _messages.addAll(provider.history)
        historySubscription = provider.addHistoryListener { history ->
            // 整体替换而非 diff：单会话消息量在百级，全量替换的开销远小于
            // 维护增量逻辑的出错成本。Compose 自己会做结构比较。
            _messages.clear()
            _messages.addAll(history)
        }
        refreshSpeechAvailability()
    }

    // ── 输入 ──────────────────────────────────────────────────────────

    fun onInputChange(text: String) {
        inputText = text
    }

    fun dismissError() {
        lastError = null
    }

    // ── 发送 / 停止 ───────────────────────────────────────────────────

    /**
     * 发送当前输入框内容与暂存附件。
     *
     * 调用后立即清空输入区——用户的心智模型是"消息已经发出去了"，
     * 等流式结束再清会让人以为没发成功而重复点击。
     */
    fun send() {
        if (!canSend) return
        if (isListening) cancelListening()
        val text = inputText.trim()
        val attachments = _pendingAttachments.toList()
        inputText = ""
        _pendingAttachments.clear()
        dispatchSend(text, attachments)
    }

    /** 直接发送指定内容，绕过输入框（用于点击建议提示）。 */
    fun sendPrompt(prompt: String, attachments: List<LlmAttachment> = emptyList()) {
        if (isGenerating || prompt.isBlank()) return
        dispatchSend(prompt.trim(), attachments)
    }

    private fun dispatchSend(text: String, attachments: List<LlmAttachment>) {
        lastError = null
        isGenerating = true
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                // collect 本身就在推进流式；provider 负责把增量写进历史，
                // 历史监听器负责刷 UI，所以这里不需要对 delta 做任何事。
                provider.sendMessageStream(text, attachments).collect { }
            } catch (e: CancellationException) {
                // 用户主动停止，provider 已把消息标为 CANCELED，不算错误
                throw e
            } catch (e: LlmProviderException) {
                lastError = LlmChatError.from(e.code, e.message.orEmpty())
            } catch (e: Throwable) {
                lastError = LlmChatError.from(LlmErrorCode.UNKNOWN, e.message.orEmpty())
            } finally {
                // stop() 后可能立刻开始下一轮；旧 job 的 finally 不能清掉新 job 的状态。
                if (streamJob === coroutineContext[Job]) {
                    streamJob = null
                    isGenerating = false
                }
            }
        }
        streamJob = job
        job.start()
    }

    /**
     * 停止生成。
     *
     * 取消 collect 会一路传导到 [com.tencent.kuiklybase.chat.kuiklyai.provider.LlmTransport]
     * 的 `cancel()`，真正断掉底层连接——而不是只在 UI 上停止刷新、
     * 后台继续烧 token。
     */
    fun stop() {
        val job = streamJob
        streamJob = null
        isGenerating = false
        job?.cancel()
    }

    // ── 重试 / 编辑重发 ───────────────────────────────────────────────

    /**
     * 重试最后一轮对话。
     *
     * 做法是把历史截断到最后一条用户消息**之前**，再用同样的内容重发。
     * 不保留失败的助手回复——留着它会让下一轮的上下文里多一段半截的
     * 或报错的 assistant 消息，影响模型输出质量。
     */
    fun retryLast() {
        if (isGenerating) return
        val lastUserIndex = _messages.indexOfLast { it.isUser }
        if (lastUserIndex < 0) return
        val lastUser = _messages[lastUserIndex]

        provider.setHistory(_messages.take(lastUserIndex))
        dispatchSend(lastUser.text, lastUser.attachments)
    }

    /**
     * 编辑某条用户消息并重新发送，该消息之后的内容全部丢弃。
     *
     * @param messageId 要编辑的用户消息 id
     * @param newText 编辑后的内容
     */
    fun editAndResend(messageId: String, newText: String) {
        if (isGenerating || newText.isBlank()) return
        val index = _messages.indexOfFirst { it.id == messageId }
        if (index < 0) return
        val original = _messages[index]
        if (!original.isUser) return

        provider.setHistory(_messages.take(index))
        dispatchSend(newText.trim(), original.attachments)
    }

    /** 清空会话。 */
    fun clear() {
        stop()
        lastError = null
        _pendingAttachments.clear()
        inputText = ""
        provider.setHistory(emptyList())
    }

    // ── 附件 ──────────────────────────────────────────────────────────

    /** 发起一次附件选择，结果进入 [pendingAttachments]。 */
    fun pickAttachment(source: LlmAttachmentSource) {
        val picker = attachmentPicker ?: return
        scope.launch {
            try {
                _pendingAttachments.addAll(picker.pick(source))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                lastError = LlmChatError.from(LlmErrorCode.UNKNOWN, e.message.orEmpty())
            }
        }
    }

    fun removePendingAttachment(id: String) {
        _pendingAttachments.removeAll { it.id == id }
    }

    // ── 语音 ──────────────────────────────────────────────────────────

    /**
     * 开始语音输入。识别结果实时回填到输入框，最终结果落定后自动停止。
     *
     * 刻意**不自动发送**：ASR 误识别很常见，让用户在发送前有机会改一下。
     */
    fun startListening() {
        val recognizer = speechRecognizer ?: return
        if (!isSpeechAvailable || isListening || isGenerating) return

        val textBeforeListening = inputText
        isListening = true
        try {
            val session = recognizer.start(
                object : LlmSpeechListener {
                    override fun onResult(result: LlmSpeechResult) {
                        // 每次都基于录音前的文本拼接，而不是累加到当前值上——
                        // 中间结果会反复修正，累加会得到"今天今天天气今天天气不错"
                        inputText = (textBeforeListening + result.text).trim()
                        if (result.isFinal) finishListening()
                    }

                    override fun onError(message: String) {
                        lastError = LlmChatError.from(LlmErrorCode.UNKNOWN, message)
                        finishListening()
                    }
                }
            )
            // 某些同步实现可能在 start() 返回前就回调最终结果。
            if (isListening) speechSession = session
        } catch (e: Throwable) {
            finishListening()
            lastError = LlmChatError.from(LlmErrorCode.UNKNOWN, e.message.orEmpty())
        }
    }

    private fun refreshSpeechAvailability() {
        val recognizer = speechRecognizer ?: return
        scope.launch {
            isSpeechAvailable = try {
                recognizer.isAvailable()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                false
            }
        }
    }

    fun stopListening() {
        val session = speechSession
        speechSession = null
        isListening = false
        session?.stop()
    }

    fun cancelListening() {
        val session = speechSession
        speechSession = null
        isListening = false
        session?.cancel()
    }

    private fun finishListening() {
        speechSession = null
        isListening = false
    }

    // ── 生命周期 ──────────────────────────────────────────────────────

    /** 页面销毁时调用，释放监听与进行中的请求。 */
    fun dispose() {
        stop()
        cancelListening()
        historySubscription.cancel()
    }
}
