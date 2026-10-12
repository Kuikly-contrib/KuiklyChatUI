package com.tencent.kuiklybase.chat.compose.core

internal fun appendTextDelta(
    parts: List<MessagePart>,
    delta: String,
    format: TextFormat,
): List<MessagePart> {
    val updated = parts.toMutableList()
    val textIndex = updated.indexOfLast { it is MessagePart.Text }
    if (textIndex < 0) {
        updated += MessagePart.Text(delta, format)
    } else {
        val current = updated[textIndex] as MessagePart.Text
        updated[textIndex] = current.copy(value = current.value + delta)
    }
    return updated
}

internal fun validateOutgoingAttachments(
    attachments: List<MessagePart.Attachment>,
): Boolean {
    val ids = mutableSetOf<String>()
    return attachments.all { attachment ->
        attachment.id.isNotBlank() &&
            attachment.uri.isNotBlank() &&
            ids.add(attachment.id)
    }
}
