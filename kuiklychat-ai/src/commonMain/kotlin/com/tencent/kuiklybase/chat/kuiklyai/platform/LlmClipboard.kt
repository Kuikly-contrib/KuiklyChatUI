package com.tencent.kuiklybase.chat.kuiklyai.platform

/** Host clipboard capability used by the default message actions. */
public fun interface LlmClipboard {
    public suspend fun copyText(label: String, text: String)
}

/** Optional host capabilities used by the chat composer and message actions. */
public data class LlmChatPlatformServices(
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
public object LlmChatPlatformRegistry {
    private var installedServices: LlmChatPlatformServices? = null

    public val current: LlmChatPlatformServices?
        get() = installedServices

    public fun install(services: LlmChatPlatformServices) {
        installedServices = services
    }

    public fun clear() {
        installedServices = null
    }
}
