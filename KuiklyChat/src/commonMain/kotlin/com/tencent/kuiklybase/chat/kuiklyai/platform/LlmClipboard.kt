package com.tencent.kuiklybase.chat.kuiklyai.platform

/** Host clipboard capability used by the default message actions. */
fun interface LlmClipboard {
    suspend fun copyText(label: String, text: String)
}

/** Optional host capabilities used by the chat composer and message actions. */
data class LlmChatPlatformServices(
    val clipboard: LlmClipboard? = null,
    val attachmentPicker: LlmAttachmentPicker? = null,
    val speechRecognizer: LlmSpeechRecognizer? = null
)

/**
 * Registry used by host demos rendered inside a Kuikly page. Library consumers
 * should normally pass [LlmChatPlatformServices] directly.
 *
 * @deprecated Pass [LlmChatPlatformServices] directly to `KuiklyAiChatView`.
 * The registry is retained for existing host demos and will be removed in a future
 * incompatible release.
 */
@Deprecated(
    message = "Pass LlmChatPlatformServices directly to KuiklyAiChatView",
    level = DeprecationLevel.WARNING
)
object LlmChatPlatformRegistry {
    private var installedServices: LlmChatPlatformServices? = null

    val current: LlmChatPlatformServices?
        get() = installedServices

    fun install(services: LlmChatPlatformServices) {
        installedServices = services
    }

    fun clear() {
        installedServices = null
    }
}
