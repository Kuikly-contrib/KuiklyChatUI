package com.kuikly.kuiklychat.dsl.chat

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.compose.core.MessageStatus
import com.tencent.kuiklybase.chat.compose.core.TextFormat

/** Payload key holding the surfaceId of one A2UI card part. */
internal const val A2UI_SURFACE_KEY: String = "surface"

private val ENVELOPE_KEYS = listOf("createSurface", "updateComponents", "updateDataModel", "deleteSurface")
private const val HISTORY_TURNS = 8
private const val HISTORY_CARD_CHARS = 600

/**
 * Recent turns before [currentId], oldest first. Text goes back as-is; each card is
 * reduced to its data model so the model remembers what it showed without the
 * cost of resending the whole JSONL.
 */
internal fun kimiHistory(messages: List<ChatMessage>, currentId: String): List<KimiTurn> {
    val before = messages.takeWhile { it.id != currentId }
    return before
        .filter { it.status != MessageStatus.FAILED && it.status != MessageStatus.CANCELLED }
        .mapNotNull { message ->
            val role = if (message.outgoing || message.author.id == "me") "user" else "assistant"
            val content = historyContent(message).trim()
            content.takeIf { it.isNotEmpty() }?.let { KimiTurn(role, it) }
        }
        .takeLast(HISTORY_TURNS)
}

private fun historyContent(message: ChatMessage): String = buildString {
    message.parts.forEach { part ->
        when {
            part is MessagePart.Text -> appendLine(part.value)
            part is MessagePart.Custom && part.type == A2UI_PART_TYPE -> {
                val model = part.payload[A2UI_STATE_KEY]?.takeIf { it.isNotEmpty() }
                    ?: foldA2uiDataModel(part.payload[A2UI_JSONL_KEY].orEmpty()).toString()
                appendLine("[已展示卡片 ${part.payload[A2UI_SURFACE_KEY].orEmpty()}，数据：${model.take(HISTORY_CARD_CHARS)}]")
            }
        }
    }
}

/**
 * Splits a streamed LLM reply into ordered text and A2UI card segments.
 *
 * Each line is either an A2UI envelope (`{"version":"v0.9", ...}`) or Markdown
 * text. Envelopes are routed to the card with the same surfaceId, text lines
 * join the current text segment. The unfinished last line is shown as text
 * while it streams unless it already looks like an envelope.
 */
internal class A2uiMixedReplyParser {
    private sealed interface Segment
    private class TextSegment(val text: StringBuilder = StringBuilder()) : Segment
    private class CardSegment(val surfaceId: String, val lines: MutableList<String> = mutableListOf()) : Segment

    private val segments = mutableListOf<Segment>()
    private val cards = HashMap<String, CardSegment>()
    private val buffer = StringBuilder()
    private var inFence = false

    fun feed(chunk: String) {
        buffer.append(chunk)
        while (true) {
            val newline = buffer.indexOf("\n")
            if (newline < 0) break
            consumeLine(buffer.substring(0, newline))
            buffer.deleteRange(0, newline + 1)
        }
    }

    fun finish() {
        if (buffer.isNotEmpty()) consumeLine(buffer.toString())
        buffer.clear()
    }

    val hasContent: Boolean
        get() = parts().isNotEmpty()

    /** Ordered message parts for the reply so far (blank text segments are skipped). */
    fun parts(): List<MessagePart> {
        val result = mutableListOf<MessagePart>()
        segments.forEach { segment ->
            when (segment) {
                is TextSegment -> addText(result, segment.text.toString())
                is CardSegment -> if (segment.lines.size > 1) result += cardPart(segment)
            }
        }
        // The unfinished last line streams into the trailing text segment.
        addText(result, pendingText())
        return result
    }

    private fun addText(result: MutableList<MessagePart>, raw: String) {
        val text = raw.trim('\n')
        if (text.isBlank()) return
        val last = result.lastOrNull() as? MessagePart.Text
        if (last != null) {
            result[result.lastIndex] = last.copy(value = last.value + "\n" + text)
        } else {
            result += MessagePart.Text(text, TextFormat.MARKDOWN)
        }
    }

    private fun pendingText(): String {
        val tail = buffer.toString()
        val trimmed = tail.trimStart()
        return if (trimmed.startsWith("{") || trimmed.startsWith("```")) "" else tail
    }

    private fun consumeLine(raw: String) {
        val trimmed = raw.trim()
        // Models sometimes wrap the JSONL in a code fence; hide the fence lines themselves.
        if (trimmed.startsWith("```")) {
            inFence = !inFence
            return
        }
        val envelope = envelopeOrNull(trimmed)
        if (envelope == null) {
            if (inFence && trimmed.isEmpty()) return
            // A broken envelope is protocol noise; never show raw JSON as reply text.
            if (looksLikeEnvelope(trimmed)) {
                logAgentCardDemo("a2ui_invalid_jsonl", trimmed)
                return
            }
            appendText(raw)
            return
        }
        val (surfaceId, line) = envelope
        val card = cards[surfaceId] ?: CardSegment(surfaceId).also {
            cards[surfaceId] = it
            segments += it
        }
        card.lines += line
    }

    private fun appendText(line: String) {
        val segment = segments.lastOrNull() as? TextSegment ?: TextSegment().also { segments += it }
        if (segment.text.isNotEmpty()) segment.text.append('\n')
        segment.text.append(line)
    }

    private fun envelopeOrNull(line: String): Pair<String, String>? {
        if (!line.startsWith("{")) return null
        var source = line
        var json = runCatching { JSONObject(source) }.getOrNull()
        if (json == null && looksLikeEnvelope(line)) {
            source = repairJsonBrackets(line) ?: return null
            json = runCatching { JSONObject(source) }.getOrNull() ?: return null
            logAgentCardDemo("a2ui_repaired_jsonl", source)
        }
        if (json == null) return null
        val key = ENVELOPE_KEYS.firstOrNull { json.optJSONObject(it) != null } ?: return null
        val surfaceId = json.optJSONObject(key)?.optString("surfaceId").orEmpty()
        logAgentCardDemo("a2ui_jsonl", source)
        return surfaceId to normalizeA2uiLayout(json.toString())
    }

    private fun looksLikeEnvelope(line: String): Boolean =
        line.startsWith("{") && line.contains("\"version\"") &&
            ENVELOPE_KEYS.any { line.contains("\"$it\"") }

    private fun cardPart(card: CardSegment) = MessagePart.Custom(
        type = A2UI_PART_TYPE,
        payload = mapOf(A2UI_JSONL_KEY to card.lines.joinToString("\n"), A2UI_SURFACE_KEY to card.surfaceId),
    )
}
