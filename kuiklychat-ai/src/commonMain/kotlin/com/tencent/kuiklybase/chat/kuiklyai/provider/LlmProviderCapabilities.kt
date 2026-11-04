package com.tencent.kuiklybase.chat.kuiklyai.provider

/**
 * Input types an LLM endpoint can actually encode and send.
 *
 * The default is permissive for backwards compatibility with custom v1
 * providers. Built-in providers override it with their real wire support.
 */
public data class LlmProviderCapabilities(
    val attachmentKinds: Set<LlmAttachmentKind> = setOf(
        LlmAttachmentKind.IMAGE,
        LlmAttachmentKind.FILE,
        LlmAttachmentKind.AUDIO
    ),
    val acceptedMimeTypes: Set<String> = emptySet(),
    val maxAttachmentBytes: Long? = null,
    val maxAttachmentCount: Int? = null
) {
    public fun supports(attachment: LlmAttachment): Boolean {
        if (attachment.kind !in attachmentKinds) return false
        if (maxAttachmentBytes != null && attachment.sizeBytes > maxAttachmentBytes) {
            return false
        }
        if (acceptedMimeTypes.isNotEmpty() &&
            attachment.mimeType.isNotEmpty() &&
            attachment.mimeType !in acceptedMimeTypes
        ) {
            return false
        }
        return true
    }
}
