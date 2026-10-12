package com.tencent.kuiklybase.chat.model

import kotlin.random.Random

/**
 * 待发送附件草稿（Composer 预览与上传载荷分离）。
 *
 * - [previewUri]：本地预览（file:// 或 content URI）
 * - [uploadUri]：发送请求用（常为 base64 data URI 或远端 URL）
 */
internal data class AttachmentDraft(
    val id: String = newDraftId(),
    val previewUri: String,
    val uploadUri: String = previewUri,
    val mimeType: String = "",
    val thumbnailUri: String = previewUri,
    val fileName: String = "",
    val fileSize: Long = 0L,
) {
    fun toAttachment(): Attachment {
        val type = when {
            mimeType.startsWith("image/") -> AttachmentType.IMAGE
            mimeType.startsWith("video/") -> AttachmentType.VIDEO
            else -> AttachmentType.FILE
        }
        val url = uploadUri.ifEmpty { previewUri }
        return Attachment(
            type = type,
            url = url,
            title = fileName,
            mimeType = mimeType,
            fileSize = fileSize,
            thumbnailUrl = thumbnailUri.ifEmpty { previewUri },
        )
    }

    companion object {
        fun newDraftId(): String = "draft_${Random.nextLong().toString(36)}_${Random.nextInt(1_000_000)}"
    }
}

internal fun List<AttachmentDraft>.toAttachments(): List<Attachment> = map { it.toAttachment() }

internal fun Attachment.toDraft(): AttachmentDraft = AttachmentDraft(
    previewUri = thumbnailUrl.ifEmpty { url },
    uploadUri = url.ifEmpty { thumbnailUrl },
    mimeType = mimeType,
    thumbnailUri = thumbnailUrl.ifEmpty { url },
    fileName = title,
    fileSize = fileSize,
)
