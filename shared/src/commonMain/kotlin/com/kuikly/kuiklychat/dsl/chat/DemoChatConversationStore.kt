package com.kuikly.kuiklychat.dsl.chat

import com.tencent.kuikly.core.module.SharedPreferencesModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuiklybase.chat.compose.core.ChatConversationStore
import com.tencent.kuiklybase.chat.compose.core.ChatConversationSummary
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.compose.core.MessageStatus
import com.tencent.kuiklybase.chat.compose.core.TextFormat

/** Demo persistence for [ChatConversationStore]. Text and custom parts round-trip. */
internal class DemoChatConversationStore(
    private val prefs: () -> SharedPreferencesModule,
) : ChatConversationStore {
    override fun loadConversations(): List<ChatConversationSummary> {
        return read()
            .asSequence()
            .filterNot { it.summary.archived }
            .map { it.summary }
            .sortedByDescending { it.updatedAt }
            .toList()
    }

    override fun loadArchivedConversations(): List<ChatConversationSummary> {
        return read()
            .asSequence()
            .filter { it.summary.archived }
            .map { it.summary }
            .sortedByDescending { it.updatedAt }
            .toList()
    }

    override fun loadMessages(conversationId: String): List<ChatMessage> {
        return read().firstOrNull { it.summary.id == conversationId }?.messages.orEmpty()
    }

    override fun save(summary: ChatConversationSummary, messages: List<ChatMessage>) {
        val next = read().filterNot { it.summary.id == summary.id } + Stored(summary, messages)
        write(next)
    }

    override fun rename(conversationId: String, title: String) {
        write(read().map { if (it.summary.id == conversationId) it.copy(summary = it.summary.copy(title = title)) else it })
    }

    override fun setArchived(conversationId: String, archived: Boolean) {
        write(read().map { if (it.summary.id == conversationId) it.copy(summary = it.summary.copy(archived = archived)) else it })
    }

    override fun delete(conversationId: String) {
        write(read().filterNot { it.summary.id == conversationId })
    }

    private fun read(): List<Stored> {
        val raw = prefs().getString(KEY)
        if (raw.isEmpty()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index -> array.optJSONObject(index)?.let(::decodeStored) }
        }.getOrDefault(emptyList())
    }

    private fun write(items: List<Stored>) {
        val array = JSONArray()
        items.forEach { array.put(encodeStored(it)) }
        prefs().setString(KEY, array.toString())
    }

    private companion object {
        const val KEY = "chat_component_demo_conversations"
    }
}

private data class Stored(val summary: ChatConversationSummary, val messages: List<ChatMessage>)

internal fun Pager.demoChatConversationStore(): DemoChatConversationStore {
    return DemoChatConversationStore { acquireModule(SharedPreferencesModule.MODULE_NAME) }
}

private fun decodeStored(json: JSONObject): Stored {
    val summary = ChatConversationSummary(
        id = json.optString("id"),
        title = json.optString("title"),
        updatedAt = json.optLong("updatedAt"),
        archived = json.optBoolean("archived"),
    )
    val messages = json.optJSONArray("messages")
    val decoded = mutableListOf<ChatMessage>()
    if (messages != null) {
        for (index in 0 until messages.length()) {
            messages.optJSONObject(index)?.let { decoded.add(decodeMessage(it)) }
        }
    }
    return Stored(summary, decoded)
}

private fun encodeStored(stored: Stored): JSONObject {
    val messages = JSONArray()
    stored.messages.forEach { messages.put(encodeMessage(it)) }
    return JSONObject()
        .put("id", stored.summary.id)
        .put("title", stored.summary.title)
        .put("updatedAt", stored.summary.updatedAt)
        .put("archived", stored.summary.archived)
        .put("messages", messages)
}

private fun decodeMessage(json: JSONObject): ChatMessage {
    val partsJson = json.optJSONArray("parts")
    val parts = mutableListOf<MessagePart>()
    if (partsJson != null) {
        for (index in 0 until partsJson.length()) {
            decodePart(partsJson.optJSONObject(index))?.let { parts.add(it) }
        }
    }
    val status = runCatching { MessageStatus.valueOf(json.optString("status")) }.getOrDefault(MessageStatus.SENT)
    return ChatMessage(
        id = json.optString("id"),
        author = ChatUser(id = json.optString("authorId"), name = json.optString("authorName")),
        parts = parts,
        status = status,
        createdAt = json.optLong("createdAt"),
        outgoing = json.optBoolean("outgoing"),
    )
}

private fun encodeMessage(message: ChatMessage): JSONObject {
    val parts = JSONArray()
    message.parts.forEach { part -> encodePart(part)?.let { parts.put(it) } }
    return JSONObject()
        .put("id", message.id)
        .put("authorId", message.author.id)
        .put("authorName", message.author.name)
        .put("status", message.status.name)
        .put("createdAt", message.createdAt)
        .put("outgoing", message.outgoing)
        .put("parts", parts)
}

private fun decodePart(json: JSONObject?): MessagePart? {
    if (json == null) return null
    return when (json.optString("kind")) {
        "text" -> MessagePart.Text(
            value = json.optString("value"),
            format = runCatching { TextFormat.valueOf(json.optString("format")) }.getOrDefault(TextFormat.PLAIN),
        )
        "custom" -> {
            val payloadJson = json.optJSONObject("payload")
            val payload = mutableMapOf<String, String>()
            payloadJson?.keys()?.let { keys ->
                while (keys.hasNext()) {
                    val key = keys.next()
                    payload[key] = payloadJson.optString(key)
                }
            }
            MessagePart.Custom(type = json.optString("type"), payload = payload)
        }
        "notice" -> MessagePart.SystemNotice(json.optString("text"))
        "attachment" -> MessagePart.Attachment(
            id = json.optString("id"),
            uri = json.optString("uri"),
            mimeType = json.optString("mimeType"),
            name = json.optString("name"),
            sizeBytes = json.optLong("sizeBytes"),
            previewUri = json.optString("previewUri"),
        )
        else -> null
    }
}

private fun encodePart(part: MessagePart): JSONObject? {
    return when (part) {
        is MessagePart.Text -> JSONObject().put("kind", "text").put("value", part.value).put("format", part.format.name)
        is MessagePart.Custom -> {
            val payload = JSONObject()
            part.payload.forEach { (key, value) -> payload.put(key, value) }
            JSONObject().put("kind", "custom").put("type", part.type).put("payload", payload)
        }
        is MessagePart.SystemNotice -> JSONObject().put("kind", "notice").put("text", part.text)
        is MessagePart.Attachment -> JSONObject()
            .put("kind", "attachment")
            .put("id", part.id)
            .put("uri", part.uri)
            .put("mimeType", part.mimeType)
            .put("name", part.name)
            .put("sizeBytes", part.sizeBytes)
            .put("previewUri", part.previewUri)
        else -> null
    }
}
