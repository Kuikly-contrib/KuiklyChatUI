package com.kuikly.kuiklychat.dsl.chat

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuiklybase.chat.compose.core.ChatController
import com.tencent.kuiklybase.chat.compose.core.ChatIntent
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.MessagePart

/**
 * Routes A2UI card actions for the demo page.
 *
 * - `local.*` events update the card's data model on the client: the new model is
 *   written into the live surface and saved on the card part, nothing goes to the LLM.
 * - Any other `event` becomes the next LLM turn, with the card's current data model
 *   attached so the model sees values the user changed locally.
 */
internal class AgentCardDemoActions(
    private val controller: ChatController,
    private val sendToLlm: (label: String, prompt: String) -> Boolean,
) {
    /** A card is one A2UI part of a message; a reply can hold several. */
    private data class CardKey(val messageId: String, val surfaceId: String)

    private val writers = HashMap<CardKey, (String) -> Unit>()
    /** Latest data model per card from bound inputs; held in memory while the user edits. */
    private val pendingStates = HashMap<CardKey, String>()

    fun attach(messageId: String, surfaceId: String, writeLine: (String) -> Unit) {
        writers[CardKey(messageId, surfaceId)] = writeLine
    }

    fun handle(messageId: String, surfaceId: String, actionJson: String) {
        logAgentCardDemo("a2ui_action", actionJson)
        val event = runCatching { JSONObject(actionJson).optJSONObject("event") }.getOrNull() ?: return
        val name = event.optString("name").ifEmpty { return }
        val context = event.optJSONObject("context")
        val key = CardKey(messageId, surfaceId)
        if (name.startsWith(LOCAL_ACTION_PREFIX)) {
            handleLocal(key, name, context)
        } else {
            handleAi(key, name, context, actionJson)
        }
    }

    /**
     * Two-way bound inputs already updated the surface, so the snapshot is only kept in
     * memory here. Writing it to the message right away would rebuild the card on every
     * Slider move and cancel the drag; [flushStates] persists it before it is needed.
     */
    fun saveState(messageId: String, surfaceId: String, dataModelJson: String) {
        // The embedded Slider can report NaN before it has a measured width; keep the old state.
        if (dataModelJson.contains("NaN")) return
        pendingStates[CardKey(messageId, surfaceId)] = dataModelJson
    }

    /** Saves bound-input changes onto their messages (call before saving the conversation). */
    fun flushStates() {
        val pending = pendingStates.toMap()
        pendingStates.clear()
        pending.forEach { (key, model) -> persistState(key, model) }
    }

    private fun persistState(key: CardKey, dataModelJson: String) {
        val message = message(key.messageId) ?: return
        val next = normalizeModel(dataModelJson) ?: return
        if (normalizeModel(stateOf(message, key.surfaceId)) == next) return
        controller.dispatch(ChatIntent.UpsertMessage(withState(message, key.surfaceId, next)))
    }

    private fun handleLocal(key: CardKey, name: String, context: JSONObject?) {
        flushStates()
        val message = message(key.messageId) ?: return
        val model = currentModel(message, key.surfaceId)
        if (!applyLocalA2uiAction(model, name, context)) return
        val snapshot = model.toString()
        writers[key]?.invoke(a2uiStateLine(key.surfaceId, snapshot))
        logAgentCardDemo("a2ui_local", snapshot)
        controller.dispatch(ChatIntent.UpsertMessage(withState(message, key.surfaceId, snapshot)))
    }

    private fun handleAi(key: CardKey, name: String, context: JSONObject?, actionJson: String) {
        if (controller.state.isSending) return
        flushStates()
        val model = message(key.messageId)?.let { currentModel(it, key.surfaceId) }?.toString() ?: "{}"
        val prompt = buildString {
            append("用户在上一张卡片上触发了 A2UI action：")
            append(actionJson)
            append("\n卡片当前的数据模型（包含用户在本地修改后的值）：")
            append(model)
            append("\n请根据这个操作和当前数据继续回复，按需要使用文字和新的卡片。")
        }
        logAgentCardDemo("action_prompt", prompt)
        val label = context?.optString("label").orEmpty().ifEmpty { name }
        sendToLlm("操作：$label", prompt)
    }

    private fun message(id: String): ChatMessage? = controller.state.messages.firstOrNull { it.id == id }

    private fun currentModel(message: ChatMessage, surfaceId: String): JSONObject {
        val state = stateOf(message, surfaceId)
        if (state.isNotEmpty()) runCatching { JSONObject(state) }.getOrNull()?.let { return it }
        return foldA2uiDataModel(jsonlOf(message, surfaceId))
    }
}

internal fun a2uiParts(message: ChatMessage): List<MessagePart.Custom> =
    message.parts.filterIsInstance<MessagePart.Custom>().filter { it.type == A2UI_PART_TYPE }

/** surfaceId of a card part; older saved messages only carry the JSONL. */
internal fun a2uiSurfaceOf(part: MessagePart.Custom): String =
    part.payload[A2UI_SURFACE_KEY] ?: a2uiSurfaceId(part.payload[A2UI_JSONL_KEY].orEmpty())

private fun a2uiPart(message: ChatMessage, surfaceId: String): MessagePart.Custom? =
    a2uiParts(message).firstOrNull { a2uiSurfaceOf(it) == surfaceId }

private fun jsonlOf(message: ChatMessage, surfaceId: String): String =
    a2uiPart(message, surfaceId)?.payload?.get(A2UI_JSONL_KEY).orEmpty()

private fun stateOf(message: ChatMessage, surfaceId: String): String =
    a2uiPart(message, surfaceId)?.payload?.get(A2UI_STATE_KEY).orEmpty()

private fun normalizeModel(json: String): String? =
    json.takeIf { it.isNotEmpty() }?.let { runCatching { JSONObject(it).toString() }.getOrNull() }

private fun withState(message: ChatMessage, surfaceId: String, state: String): ChatMessage = message.copy(
    parts = message.parts.map { part ->
        if (part is MessagePart.Custom && part.type == A2UI_PART_TYPE && a2uiSurfaceOf(part) == surfaceId) {
            part.copy(payload = part.payload + (A2UI_STATE_KEY to state))
        } else {
            part
        }
    },
)
