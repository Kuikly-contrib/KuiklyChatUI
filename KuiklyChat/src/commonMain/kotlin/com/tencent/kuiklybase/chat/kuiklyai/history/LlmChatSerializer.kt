package com.tencent.kuiklybase.chat.kuiklyai.history

import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmAttachment
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmAttachmentKind
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmChatMessage
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmMessageOrigin
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmMessageState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * 会话历史的 JSON 序列化 / 反序列化。
 *
 * 对应 Flutter AI Toolkit 的 "chat serialization/deserialization"。
 * 只负责字符串与模型之间的转换，**不碰存储**——写文件、写数据库、
 * 上传云端都由宿主决定，这样 commonMain 不需要任何 IO 能力。
 *
 * 格式带 [VERSION] 字段，后续模型演进时可以做兼容处理，
 * 而不是让用户的历史对话在升级后直接读不出来。
 */
object LlmChatSerializer {

    /** 当前格式版本 */
    const val VERSION: Int = 1

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * 序列化为 JSON 字符串。
     */
    fun encode(messages: List<LlmChatMessage>): String {
        val root = buildJsonObject {
            put("version", VERSION)
            put("messages", buildJsonArray { messages.forEach { add(encodeMessage(it)) } })
        }
        return root.toString()
    }

    /**
     * 从 JSON 字符串恢复。
     *
     * 解析失败或版本不认识时返回空列表，而不是抛异常——
     * 恢复历史失败最多是"少了上下文"，不该让整个页面崩掉。
     */
    fun decode(text: String): List<LlmChatMessage> {
        if (text.isBlank()) return emptyList()
        val root = runCatching { json.parseToJsonElement(text) }.getOrNull() as? JsonObject
            ?: return emptyList()

        val version = root["version"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
        if (version > VERSION) return emptyList()

        val array = root["messages"] as? JsonArray ?: return emptyList()
        return array.mapNotNull { element ->
            runCatching { decodeMessage(element.jsonObject) }.getOrNull()
        }
    }

    private fun encodeMessage(message: LlmChatMessage): JsonObject = buildJsonObject {
        put("id", message.id)
        put("origin", message.origin.name)
        put("text", message.text)
        put("createdAt", message.createdAt)
        put("state", message.state.name)
        if (message.error.isNotEmpty()) put("error", message.error)
        if (message.attachments.isNotEmpty()) {
            put(
                "attachments",
                buildJsonArray { message.attachments.forEach { add(encodeAttachment(it)) } }
            )
        }
        if (message.extra.isNotEmpty()) {
            putJsonObject("extra") {
                message.extra.forEach { (key, value) -> put(key, value) }
            }
        }
    }

    private fun encodeAttachment(attachment: LlmAttachment): JsonObject = buildJsonObject {
        put("id", attachment.id)
        put("kind", attachment.kind.name)
        put("name", attachment.name)
        put("mimeType", attachment.mimeType)
        put("url", attachment.url)
        put("sizeBytes", attachment.sizeBytes)
        // bytesBase64 刻意不落盘：一张图能把历史撑到几 MB，
        // 而恢复对话时展示缩略图用 url 就够了。需要原始字节的场景由宿主自己存。
    }

    private fun decodeMessage(obj: JsonObject): LlmChatMessage {
        val attachments = (obj["attachments"] as? JsonArray)
            ?.mapNotNull { runCatching { decodeAttachment(it.jsonObject) }.getOrNull() }
            .orEmpty()

        val extra = (obj["extra"] as? JsonObject)
            ?.mapNotNull { (key, value) ->
                value.jsonPrimitive.contentOrNull?.let { key to it }
            }
            ?.toMap()
            .orEmpty()

        return LlmChatMessage(
            id = obj.string("id"),
            origin = enumOrDefault(obj.string("origin"), LlmMessageOrigin.LLM),
            text = obj.string("text"),
            attachments = attachments,
            createdAt = obj["createdAt"]?.jsonPrimitive?.longOrNull ?: 0L,
            // 恢复出来的历史一律视为终态：一条卡在 STREAMING 的消息
            // 会让 UI 永远转圈，而它对应的请求早就随进程结束了。
            state = restoredState(obj.string("state")),
            error = obj.string("error"),
            extra = extra
        )
    }

    private fun decodeAttachment(obj: JsonObject): LlmAttachment = LlmAttachment(
        id = obj.string("id"),
        kind = enumOrDefault(obj.string("kind"), LlmAttachmentKind.FILE),
        name = obj.string("name"),
        mimeType = obj.string("mimeType"),
        url = obj.string("url"),
        sizeBytes = obj["sizeBytes"]?.jsonPrimitive?.longOrNull ?: 0L
    )

    private fun restoredState(raw: String): LlmMessageState =
        when (enumOrDefault(raw, LlmMessageState.DONE)) {
            LlmMessageState.STREAMING -> LlmMessageState.CANCELED
            LlmMessageState.FAILED -> LlmMessageState.FAILED
            LlmMessageState.CANCELED -> LlmMessageState.CANCELED
            LlmMessageState.DONE -> LlmMessageState.DONE
        }

    private fun JsonObject.string(key: String): String =
        this[key]?.jsonPrimitive?.contentOrNull.orEmpty()

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == raw } ?: fallback
}
