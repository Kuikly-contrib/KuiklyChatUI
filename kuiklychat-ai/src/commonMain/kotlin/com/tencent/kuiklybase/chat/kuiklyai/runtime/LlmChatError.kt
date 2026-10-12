package com.tencent.kuiklybase.chat.kuiklyai.runtime

import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmErrorCode

/**
 * 呈现给用户的错误。
 *
 * 与 [LlmErrorCode] 的区别：后者是协议层分类，这里多了「给用户看什么文案」
 * 和「该不该显示重试按钮」两个 UI 决策。
 *
 * 之所以要区分——鉴权失败让用户点十次重试也没用，只会让人以为是网络抖动；
 * 而限流是等一下就能好的，藏起重试按钮反而多余。
 */
public data class LlmChatError(
    val code: LlmErrorCode,
    /** 面向用户的简短说明 */
    val message: String,
    /** 原始错误详情，折叠展示或写日志用 */
    val detail: String = ""
) {
    /** 是否展示"重试"按钮 */
    val retryable: Boolean get() = code.retryable

    public companion object {
        /**
         * 把协议层错误码翻译成用户能看懂的话。
         *
         * 刻意不直接把服务端的 message 抛给用户——OpenAI 兼容端点的原始报错
         * 常常是英文技术术语（"insufficient_quota"），对终端用户毫无意义。
         * 原文保留在 [detail] 里给开发者排查。
         */
        public fun from(code: LlmErrorCode, raw: String): LlmChatError {
            val friendly = when (code) {
                LlmErrorCode.NETWORK -> "网络连接失败，请检查网络后重试"
                LlmErrorCode.TIMEOUT -> "请求超时，稍后重试"
                LlmErrorCode.AUTH -> "鉴权失败，请检查密钥配置或账户额度"
                LlmErrorCode.RATE_LIMIT -> "请求过于频繁，稍等一下再试"
                LlmErrorCode.INVALID_REQUEST -> "请求参数有误，请联系开发者"
                LlmErrorCode.REJECTED -> "内容未通过安全策略，换个说法试试"
                LlmErrorCode.MALFORMED_RESPONSE -> "响应格式异常，无法解析"
                LlmErrorCode.TRANSPORT_OVERFLOW -> "响应速度过快，数据未能完整接收"
                LlmErrorCode.SERVER -> "服务端出错了，稍后重试"
                LlmErrorCode.CANCELED -> "已停止生成"
                LlmErrorCode.UNKNOWN -> "出了点问题，稍后重试"
            }
            return LlmChatError(code = code, message = friendly, detail = raw)
        }
    }
}
