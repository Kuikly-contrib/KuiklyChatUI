package com.tencent.kuiklybase.chat.kuiklyai.runtime

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmAttachmentPicker
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmAttachmentSource
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmClipboard
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

/** UI state holder between [LlmProvider] and Compose. */
class LlmChatController(
    private val provider: LlmProvider,
    private val scope: CoroutineScope,
    val attachmentPicker: LlmAttachmentPicker? = null,
    val speechRecognizer: LlmSpeechRecognizer? = null,
    val clipboard: LlmClipboard? = null
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

    /** Default editor state. Only the latest user message can enter this state. */
    var editState by mutableStateOf<LlmChatEditState>(LlmChatEditState.Idle)
        private set

    /** A one-shot UI event, consumed after it has been rendered. */
    var transientEvent by mutableStateOf<LlmChatUiEvent?>(null)
        private set

    /** 能否发送：有内容且当前不在生成中 */
    val canSend: Boolean
        get() = !isGenerating &&
            (inputText.isNotBlank() || _pendingAttachments.isNotEmpty())

    /** Attachment entry points that the picker and current provider both support. */
    val availableAttachmentSources: List<LlmAttachmentSource>
        get() = attachmentPicker?.supportedSources
            ?.filter { provider.capabilities.attachmentKinds.contains(attachmentPicker.kindOf(it)) }
            ?.sortedBy { it.ordinal }
            .orEmpty()

    private var streamJob: Job? = null
    private var clipboardJob: Job? = null
    private var speechSession: LlmSpeechSession? = null
    private val historySubscription: LlmSubscription

    init {
        _messages.addAll(provider.history)
        historySubscription = provider.addHistoryListener { history ->
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

    fun consumeTransientEvent() {
        transientEvent = null
    }

    // ── 发送 / 停止 ───────────────────────────────────────────────────

    /** Send the current composer contents and pending attachments. */
    fun send() {
        if (!canSend) return
        if (isListening) cancelListening()
        if (editState is LlmChatEditState.Editing) {
            submitEdit()
            return
        }
        val text = inputText.trim()
        val attachments = _pendingAttachments.toList()
        if (!validateAttachments(attachments)) return
        inputText = ""
        _pendingAttachments.clear()
        dispatchSend(text, attachments)
    }

    /** 直接发送指定内容，绕过输入框（用于点击建议提示）。 */
    fun sendPrompt(prompt: String, attachments: List<LlmAttachment> = emptyList()) {
        if (isGenerating || (prompt.isBlank() && attachments.isEmpty())) return
        if (!validateAttachments(attachments)) return
        dispatchSend(prompt.trim(), attachments)
    }

    /** Copy a message's raw text through the injected host clipboard. */
    fun copyMessage(message: LlmChatMessage) {
        val target = clipboard ?: return
        if (message.text.isEmpty()) return
        clipboardJob?.cancel()
        val job = scope.launch {
            try {
                target.copyText(
                    label = if (message.isUser) "User message" else "AI response",
                    text = message.text
                )
                transientEvent = LlmChatUiEvent.TextCopied(message.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                transientEvent = LlmChatUiEvent.PlatformOperationFailed(
                    e.message ?: "Unable to copy text"
                )
            } finally {
                if (clipboardJob === coroutineContext[Job]) clipboardJob = null
            }
        }
        clipboardJob = job
    }

    private fun dispatchSend(text: String, attachments: List<LlmAttachment>) {
        lastError = null
        isGenerating = true
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                provider.sendMessageStream(text, attachments).collect { }
            } catch (e: CancellationException) {
                throw e
            } catch (e: LlmProviderException) {
                lastError = LlmChatError.from(e.code, e.message.orEmpty())
            } catch (e: Throwable) {
                lastError = LlmChatError.from(LlmErrorCode.UNKNOWN, e.message.orEmpty())
            } finally {
                if (streamJob === coroutineContext[Job]) {
                    streamJob = null
                    isGenerating = false
                }
            }
        }
        streamJob = job
        job.start()
    }

    /** Stop generation and cancel the underlying stream. */
    fun stop() {
        val job = streamJob
        streamJob = null
        isGenerating = false
        job?.cancel()
    }

    // ── 重试 / 编辑重发 ───────────────────────────────────────────────

    /** Retry the latest user turn without retaining its failed response. */
    fun retryLast() {
        if (isGenerating) return
        val lastUserIndex = _messages.indexOfLast { it.isUser }
        if (lastUserIndex < 0) return
        val lastUser = _messages[lastUserIndex]
        if (!validateAttachments(lastUser.attachments)) return

        provider.setHistory(_messages.take(lastUserIndex))
        dispatchSend(lastUser.text, lastUser.attachments)
    }

    /** Low-level edit-and-resend operation for the latest user turn. */
    fun editAndResend(messageId: String, newText: String) {
        if (isGenerating) return
        val index = _messages.indexOfFirst { it.id == messageId }
        if (index < 0) return
        val original = _messages[index]
        if (!original.isUser) return
        if (newText.isBlank() && original.attachments.isEmpty()) return
        if (index != _messages.indexOfLast { it.isUser }) return
        if (!validateAttachments(original.attachments)) return

        provider.setHistory(_messages.take(index))
        dispatchSend(newText.trim(), original.attachments)
    }

    /** Enter the built-in editor for the latest user message. */
    fun beginEdit(messageId: String) {
        if (isGenerating || editState !is LlmChatEditState.Idle) return
        val index = _messages.indexOfFirst { it.id == messageId }
        if (index < 0 || index != _messages.indexOfLast { it.isUser }) return
        val message = _messages[index]
        if (!message.isUser) return

        editState = LlmChatEditState.Editing(
            messageId = message.id,
            originalInputText = inputText,
            originalPendingAttachments = _pendingAttachments.toList()
        )
        inputText = message.text
        _pendingAttachments.clear()
        _pendingAttachments.addAll(message.attachments)
        lastError = null
    }

    /** Cancel editing and restore the composer draft from before editing began. */
    fun cancelEdit() {
        val state = editState as? LlmChatEditState.Editing ?: return
        inputText = state.originalInputText
        _pendingAttachments.clear()
        _pendingAttachments.addAll(state.originalPendingAttachments)
        editState = LlmChatEditState.Idle
    }

    /** Submit the current composer as a replacement for the latest user message. */
    fun submitEdit() {
        val state = editState as? LlmChatEditState.Editing ?: return
        if (isGenerating || (inputText.isBlank() && _pendingAttachments.isEmpty())) return
        val index = _messages.indexOfFirst { it.id == state.messageId }
        if (index < 0 || index != _messages.indexOfLast { it.isUser }) {
            cancelEdit()
            return
        }
        val text = inputText.trim()
        val attachments = _pendingAttachments.toList()
        if (!validateAttachments(attachments)) return

        provider.setHistory(_messages.take(index))
        inputText = ""
        _pendingAttachments.clear()
        editState = LlmChatEditState.Idle
        dispatchSend(text, attachments)
    }

    /** 清空会话。 */
    fun clear() {
        stop()
        lastError = null
        transientEvent = null
        editState = LlmChatEditState.Idle
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
                val selected = picker.pick(source)
                val accepted = selected.filter { provider.capabilities.supports(it) }
                if (accepted.size != selected.size) {
                    transientEvent = LlmChatUiEvent.AttachmentRejected(
                        "One or more selected attachments are not supported by this model"
                    )
                }
                _pendingAttachments.addAll(accepted)
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

    private fun validateAttachments(attachments: List<LlmAttachment>): Boolean {
        val capabilities = provider.capabilities
        if (capabilities.maxAttachmentCount != null &&
            attachments.size > capabilities.maxAttachmentCount
        ) {
            transientEvent = LlmChatUiEvent.AttachmentRejected(
                "Too many attachments for this model"
            )
            return false
        }
        val unsupported = attachments.firstOrNull { !capabilities.supports(it) }
        if (unsupported != null) {
            transientEvent = LlmChatUiEvent.AttachmentRejected(
                "${unsupported.name.ifEmpty { unsupported.kind.name }} is not supported by this model"
            )
            return false
        }
        return true
    }

    // ── 语音 ──────────────────────────────────────────────────────────

    /** Start voice input and keep the final result in the composer. */
    fun startListening() {
        val recognizer = speechRecognizer ?: return
        if (!isSpeechAvailable || isListening || isGenerating) return

        val textBeforeListening = inputText
        isListening = true
        try {
            val session = recognizer.start(
                object : LlmSpeechListener {
                    override fun onResult(result: LlmSpeechResult) {
                        inputText = (textBeforeListening + result.text).trim()
                        if (result.isFinal) finishListening()
                    }

                    override fun onError(message: String) {
                        lastError = LlmChatError.from(LlmErrorCode.UNKNOWN, message)
                        finishListening()
                    }
                }
            )
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
        clipboardJob?.cancel()
        clipboardJob = null
        cancelListening()
        historySubscription.cancel()
    }
}
