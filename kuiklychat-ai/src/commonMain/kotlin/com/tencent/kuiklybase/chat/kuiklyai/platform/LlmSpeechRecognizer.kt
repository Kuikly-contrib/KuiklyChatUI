package com.tencent.kuiklybase.chat.kuiklyai.platform

/**
 * 语音识别的中间结果。
 *
 * @param text 到目前为止识别出的文本
 * @param isFinal 是否为本段语音的最终结果
 */
public data class LlmSpeechResult(
    val text: String,
    val isFinal: Boolean
)

/**
 * 语音识别监听。
 */
public interface LlmSpeechListener {
    /** 识别出中间/最终文本 */
    public fun onResult(result: LlmSpeechResult)

    /** 识别失败（无权限、无网络、引擎不可用等） */
    public fun onError(message: String)
}

/**
 * 语音输入：由宿主实现的平台能力钩子。
 *
 * 与 [LlmAttachmentPicker] 同理——语音识别的选型（系统 ASR、微信同声传译、
 * 腾讯云语音、自建流式 ASR）是业务决策，涉及计费、合规与音质取舍，
 * 库内不做默认实现，也不引入任何语音 SDK。
 *
 * 未提供实现时输入区不显示麦克风按钮。
 */
public interface LlmSpeechRecognizer {

    /** 当前设备/宿主是否具备语音识别能力（含权限已授予） */
    public suspend fun isAvailable(): Boolean

    /**
     * 开始录音识别。
     *
     * 实现方需在识别过程中持续回调 [LlmSpeechListener.onResult]，
     * 让输入框能实时回显——一次性等到最终结果才显示的体验很差。
     *
     * @return 停止录音的句柄
     */
    public fun start(listener: LlmSpeechListener): LlmSpeechSession
}

/**
 * 一次录音会话的控制句柄。
 */
public interface LlmSpeechSession {
    /** 正常结束，等待最终结果 */
    public fun stop()

    /** 放弃本次录音，丢弃已识别内容 */
    public fun cancel()
}
