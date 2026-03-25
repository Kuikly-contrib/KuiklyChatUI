package com.kuikly.kuiklychat

import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuiklybase.chat.*

/**
 * 聊天消息 JSON 解析器
 *
 * 负责将服务端返回的 JSON 数据解析为 ChatMessage 列表。
 * JSON 格式参考常见的 IM 接口设计：
 * ```json
 * {
 *   "code": 0,
 *   "message": "success",
 *   "data": {
 *     "session_id": "xxx",
 *     "has_more": true,
 *     "messages": [ ... ]
 *   }
 * }
 * ```
 */
object ChatMessageParser {

    /**
     * 解析接口响应 JSON
     *
     * @param jsonString JSON 字符串
     * @return ChatApiResponse 解析结果
     */
    fun parseResponse(jsonString: String): ChatApiResponse {
        val root = JSONObject(jsonString)
        val code = root.optInt("code", -1)
        val message = root.optString("message")

        if (code != 0) {
            return ChatApiResponse(
                code = code,
                message = message,
                sessionId = "",
                hasMore = false,
                messages = emptyList()
            )
        }

        val data = root.optJSONObject("data") ?: return ChatApiResponse(
            code = -1,
            message = "data 字段缺失",
            sessionId = "",
            hasMore = false,
            messages = emptyList()
        )

        val sessionId = data.optString("session_id")
        val hasMore = data.optBoolean("has_more", false)
        val messagesArray = data.optJSONArray("messages") ?: JSONArray()

        val messages = mutableListOf<ChatMessage>()
        for (i in 0 until messagesArray.length()) {
            val msgJson = messagesArray.optJSONObject(i) ?: continue
            val chatMessage = parseMessage(msgJson)
            if (chatMessage != null) {
                messages.add(chatMessage)
            }
        }

        return ChatApiResponse(
            code = code,
            message = message,
            sessionId = sessionId,
            hasMore = hasMore,
            messages = messages
        )
    }

    /**
     * 解析单条消息 JSON
     */
    private fun parseMessage(json: JSONObject): ChatMessage? {
        val id = json.optString("id")
        if (id.isEmpty()) return null

        val typeStr = json.optString("type", "TEXT")
        val type = parseMessageType(typeStr)
        val content = json.optString("content")
        val isSelf = json.optBoolean("is_self", false)
        val senderName = json.optString("sender_name")
        val senderId = json.optString("sender_id")
        val senderAvatar = json.optString("sender_avatar")
        val timestamp = json.optLong("timestamp", 0L)
        val statusStr = json.optString("status", "SENT")
        val status = parseMessageStatus(statusStr)

        // 可选字段
        val isEdited = json.optBoolean("is_edited", false)
        val isDeleted = json.optBoolean("is_deleted", false)
        val isPinned = json.optBoolean("is_pinned", false)
        val threadCount = json.optInt("thread_count", 0)

        // 解析反应列表
        val reactions = parseReactions(json.optJSONArray("reactions"))

        // 解析附件列表
        val attachments = parseAttachments(json.optJSONArray("attachments"))

        // 解析引用消息
        val quotedMessageJson = json.optJSONObject("quoted_message")
        val quotedMessage = if (quotedMessageJson != null) parseMessage(quotedMessageJson) else null

        // 解析 readBy
        val readByArray = json.optJSONArray("read_by")
        val readBy = mutableListOf<String>()
        if (readByArray != null) {
            for (i in 0 until readByArray.length()) {
                readByArray.optString(i)?.let { readBy.add(it) }
            }
        }

        // 构建 extra map（从附件中提取尺寸信息，兼容旧的渲染逻辑）
        val extra = buildExtraFromAttachments(type, attachments)

        return ChatMessage(
            id = id,
            content = content,
            isSelf = isSelf,
            type = type,
            status = status,
            senderName = senderName,
            senderAvatar = senderAvatar,
            timestamp = timestamp,
            extra = extra,
            senderId = senderId,
            reactions = reactions,
            threadCount = threadCount,
            isEdited = isEdited,
            isDeleted = isDeleted,
            isPinned = isPinned,
            readBy = readBy,
            attachments = attachments,
            quotedMessage = quotedMessage
        )
    }

    /**
     * 解析消息类型
     */
    private fun parseMessageType(typeStr: String): MessageType {
        return when (typeStr.uppercase()) {
            "TEXT" -> MessageType.TEXT
            "IMAGE" -> MessageType.IMAGE
            "VIDEO" -> MessageType.VIDEO
            "FILE" -> MessageType.FILE
            "SYSTEM" -> MessageType.SYSTEM
            "CUSTOM" -> MessageType.CUSTOM
            else -> MessageType.TEXT
        }
    }

    /**
     * 解析消息状态
     */
    private fun parseMessageStatus(statusStr: String): MessageStatus {
        return when (statusStr.uppercase()) {
            "SENDING" -> MessageStatus.SENDING
            "SENT" -> MessageStatus.SENT
            "FAILED" -> MessageStatus.FAILED
            "READ" -> MessageStatus.READ
            else -> MessageStatus.SENT
        }
    }

    /**
     * 解析反应列表
     */
    private fun parseReactions(reactionsArray: JSONArray?): List<ReactionItem> {
        if (reactionsArray == null) return emptyList()
        val reactions = mutableListOf<ReactionItem>()
        for (i in 0 until reactionsArray.length()) {
            val rJson = reactionsArray.optJSONObject(i) ?: continue
            reactions.add(
                ReactionItem(
                    type = rJson.optString("type"),
                    count = rJson.optInt("count", 1),
                    isOwnReaction = rJson.optBoolean("is_own_reaction", false)
                )
            )
        }
        return reactions
    }

    /**
     * 解析附件列表
     */
    private fun parseAttachments(attachmentsArray: JSONArray?): List<Attachment> {
        if (attachmentsArray == null) return emptyList()
        val attachments = mutableListOf<Attachment>()
        for (i in 0 until attachmentsArray.length()) {
            val aJson = attachmentsArray.optJSONObject(i) ?: continue
            val typeStr = aJson.optString("type", "FILE")
            val attachmentType = when (typeStr.uppercase()) {
                "IMAGE" -> AttachmentType.IMAGE
                "VIDEO" -> AttachmentType.VIDEO
                "FILE" -> AttachmentType.FILE
                "GIPHY" -> AttachmentType.GIPHY
                "LINK_PREVIEW" -> AttachmentType.LINK_PREVIEW
                else -> AttachmentType.FILE
            }
            attachments.add(
                Attachment(
                    type = attachmentType,
                    url = aJson.optString("url"),
                    title = aJson.optString("title"),
                    mimeType = aJson.optString("mime_type"),
                    fileSize = aJson.optLong("file_size", 0L),
                    width = aJson.optInt("width", 0),
                    height = aJson.optInt("height", 0),
                    duration = aJson.optDouble("duration", 0.0).toFloat(),
                    thumbnailUrl = aJson.optString("thumbnail_url")
                )
            )
        }
        return attachments
    }

    /**
     * 从附件中提取 extra 信息（兼容旧渲染逻辑）
     */
    private fun buildExtraFromAttachments(type: MessageType, attachments: List<Attachment>): Map<String, String> {
        if (attachments.isEmpty()) return emptyMap()
        val attachment = attachments.first()
        return when (type) {
            MessageType.IMAGE -> mapOf(
                "width" to attachment.width.toString(),
                "height" to attachment.height.toString()
            )
            MessageType.VIDEO -> mapOf(
                "width" to attachment.width.toString(),
                "height" to attachment.height.toString(),
                "duration" to attachment.duration.toString(),
                "thumbnailUrl" to attachment.thumbnailUrl
            )
            MessageType.FILE -> mapOf(
                "fileUrl" to attachment.url,
                "fileName" to attachment.title,
                "mimeType" to attachment.mimeType,
                "fileSize" to attachment.fileSize.toString()
            )
            else -> emptyMap()
        }
    }
}

/**
 * 聊天接口响应数据模型
 *
 * 模拟服务端返回的标准响应结构。
 */
data class ChatApiResponse(
    /** 响应码，0 表示成功 */
    val code: Int,
    /** 响应消息 */
    val message: String,
    /** 会话 ID */
    val sessionId: String,
    /** 是否还有更多历史消息 */
    val hasMore: Boolean,
    /** 消息列表 */
    val messages: List<ChatMessage>
)
