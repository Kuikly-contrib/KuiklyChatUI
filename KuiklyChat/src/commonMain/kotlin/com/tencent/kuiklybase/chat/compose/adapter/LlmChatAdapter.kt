package com.tencent.kuiklybase.chat.compose.adapter

import com.tencent.kuiklybase.chat.compose.core.ChatError
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatMessageSender
import com.tencent.kuiklybase.chat.compose.core.ChatStreamEvent
import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.compose.core.MessageStatus
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmAttachment
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmAttachmentKind
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmChatMessage
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmMessageOrigin
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmMessageState
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmProvider
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmProviderException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.collect

/**
 * Adapts the existing provider contract to the new Compose Chat UI core.
 * AG-UI and provider details stay outside ChatController and ChatScreen.
 */
class LlmChatAdapter(
    private val provider: LlmProvider,
    private val currentUser: ChatUser = ChatUser("user", "You"),
    private val assistant: ChatUser = ChatUser("assistant", "Assistant")
) {
    val initialMessages: List<ChatMessage>
        get() = provider.history.map { it.toChatMessage(currentUser, assistant) }

    val sender: ChatMessageSender = ChatMessageSender { userMessage ->
        send(userMessage)
    }

    private fun send(userMessage: ChatMessage): Flow<ChatStreamEvent> = flow {
        val assistantId = "assistant-${userMessage.id}"
        emit(ChatStreamEvent.TextDelta(messageId = assistantId, text = ""))
        try {
            provider.sendMessageStream(
                prompt = userMessage.text,
                attachments = userMessage.parts.toLlmAttachments()
            ).collect { delta ->
                emit(ChatStreamEvent.TextDelta(messageId = assistantId, text = delta))
            }
            emit(ChatStreamEvent.Completed)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: LlmProviderException) {
            emit(ChatStreamEvent.Failed(error.toChatError()))
        } catch (error: Throwable) {
            emit(ChatStreamEvent.Failed(ChatError(error.message ?: "Request failed", "unknown")))
        }
    }
}

private fun LlmChatMessage.toChatMessage(
    currentUser: ChatUser,
    assistant: ChatUser
): ChatMessage {
    val author = if (origin == LlmMessageOrigin.USER) {
        currentUser
    } else {
        assistant
    }
    val status = when (state) {
        LlmMessageState.STREAMING -> MessageStatus.STREAMING
        LlmMessageState.DONE -> MessageStatus.SENT
        LlmMessageState.FAILED -> MessageStatus.FAILED
        LlmMessageState.CANCELED -> MessageStatus.CANCELLED
    }
    val parts = buildList {
        if (text.isNotEmpty()) add(MessagePart.Text(text))
        addAll(attachments.map { it.toMessagePart() })
    }
    return ChatMessage(
        id = id,
        author = author,
        parts = parts,
        status = status,
        createdAt = createdAt,
        metadata = extra
    )
}

private fun LlmAttachment.toMessagePart(): MessagePart.Attachment {
    val displayMimeType = mimeType.ifEmpty {
        when (kind) {
            LlmAttachmentKind.IMAGE -> "image/unknown"
            LlmAttachmentKind.AUDIO -> "audio/unknown"
            else -> "application/octet-stream"
        }
    }
    val sourceUri = when {
        url.isNotEmpty() -> url
        bytesBase64.isNotEmpty() -> AttachmentUriCodec.toDataUri(displayMimeType, bytesBase64)
        else -> ""
    }
    val previewUri = if (kind == LlmAttachmentKind.IMAGE && thumbnailBase64.isNotEmpty()) {
        AttachmentUriCodec.toDataUri(displayMimeType, thumbnailBase64)
    } else {
        ""
    }
    return MessagePart.Attachment(
        id = id,
        uri = sourceUri,
        mimeType = displayMimeType,
        name = name,
        sizeBytes = sizeBytes,
        previewUri = previewUri,
    )
}

private fun List<MessagePart>.toLlmAttachments(): List<LlmAttachment> {
    return filterIsInstance<MessagePart.Attachment>().map { it.toLlmAttachment() }
}

private fun MessagePart.Attachment.toLlmAttachment(): LlmAttachment {
    val parsedUri = AttachmentUriCodec.parseDataUri(uri.trim())
    val thumbnailFromPreview = previewUri.trim().let { preview ->
        AttachmentUriCodec.parseDataUri(preview)?.base64Payload.orEmpty()
    }
    if (parsedUri != null) {
        return LlmAttachment(
            id = id,
            kind = llmKindFromMime(mimeType.ifEmpty { parsedUri.mimeType }),
            name = name,
            mimeType = mimeType.ifEmpty { parsedUri.mimeType },
            url = "",
            bytesBase64 = parsedUri.base64Payload,
            thumbnailBase64 = thumbnailFromPreview,
            sizeBytes = sizeBytes,
        )
    }
    return LlmAttachment(
        id = id,
        kind = llmKindFromMime(mimeType),
        name = name,
        mimeType = mimeType,
        url = uri,
        bytesBase64 = "",
        thumbnailBase64 = thumbnailFromPreview,
        sizeBytes = sizeBytes,
    )
}

private fun llmKindFromMime(mimeType: String): LlmAttachmentKind = when {
    mimeType.startsWith("image/") -> LlmAttachmentKind.IMAGE
    mimeType.startsWith("audio/") -> LlmAttachmentKind.AUDIO
    else -> LlmAttachmentKind.FILE
}

private fun LlmProviderException.toChatError(): ChatError {
    return ChatError(
        message = message ?: "Request failed",
        code = code.name,
        retryable = code.retryable
    )
}
