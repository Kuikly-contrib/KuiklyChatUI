package com.kuikly.kuiklychat.dsl.chat

import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/** Events named `local.*` only change card data on the client and never reach the LLM. */
internal const val LOCAL_ACTION_PREFIX: String = "local."

/** Message payload key holding the card's latest data model JSON (local edits included). */
internal const val A2UI_STATE_KEY: String = "state"

/** Rebuilds the data model a surface ends up with after replaying every `updateDataModel` line. */
internal fun foldA2uiDataModel(jsonl: String): JSONObject {
    var model = JSONObject()
    jsonl.lineSequence().forEach { line ->
        val update = runCatching { JSONObject(line) }.getOrNull()?.optJSONObject("updateDataModel") ?: return@forEach
        val path = update.optString("path").ifEmpty { "/" }
        val value = update.opt("value")
        if (path == "/") {
            if (value is JSONObject) model = value
        } else {
            setA2uiPath(model, path, value)
        }
    }
    return model
}

internal fun a2uiSurfaceId(jsonl: String): String {
    val first = jsonl.lineSequence().firstOrNull { it.contains("createSurface") } ?: return ""
    return runCatching { JSONObject(first).optJSONObject("createSurface")?.optString("surfaceId") }
        .getOrNull().orEmpty()
}

/** A full-replace `updateDataModel` line that restores [model] when appended to the card JSONL. */
internal fun a2uiStateLine(surfaceId: String, model: String): String {
    val value = runCatching { JSONObject(model) }.getOrNull() ?: return ""
    val update = JSONObject().put("surfaceId", surfaceId).put("path", "/").put("value", value)
    return JSONObject().put("version", "v0.9").put("updateDataModel", update).toString()
}

/**
 * JSONL used to (re)build a card. With a saved [state], the model's own data updates
 * are dropped and the saved model is applied once, so bound inputs never see the
 * stale values first (a Slider would echo them back and overwrite the saved state).
 */
internal fun a2uiReplayJsonl(jsonl: String, state: String): String {
    if (state.isEmpty()) return jsonl
    val stateLine = a2uiStateLine(a2uiSurfaceId(jsonl), state).ifEmpty { return jsonl }
    val structure = jsonl.lineSequence().filterNot { it.contains("\"updateDataModel\"") }
    return (structure + stateLine).joinToString("\n")
}

/**
 * Applies one `local.*` event to [model] and returns true when something changed.
 * The target path lives in `context.target`: a `path` key would be resolved to the
 * current value by the SDK before the action reaches the host.
 *
 * - `local.increment`: `step` (default 1, negative to decrement), optional `min`/`max`.
 * - `local.set`: `value`.
 * - `local.toggle`: flips a boolean.
 */
internal fun applyLocalA2uiAction(model: JSONObject, name: String, context: JSONObject?): Boolean {
    val target = context?.optString("target").orEmpty()
    if (!target.startsWith("/")) return false
    val current = getA2uiPath(model, target)
    val next: Any? = when (name.removePrefix(LOCAL_ACTION_PREFIX)) {
        "increment" -> {
            val base = (current as? Number ?: context?.opt("value") as? Number)?.toDouble() ?: 0.0
            var value = base + (context?.optDouble("step", 1.0) ?: 1.0)
            if (context?.has("min") == true) value = maxOf(value, context.optDouble("min"))
            if (context?.has("max") == true) value = minOf(value, context.optDouble("max"))
            if (value % 1.0 == 0.0) value.toLong() else value
        }
        "set" -> context?.opt("value")
        "toggle" -> current != true
        else -> return false
    }
    if (next == current) return false
    setA2uiPath(model, target, next)
    return true
}

private fun pathSegments(path: String): List<String> = path.trim('/').split('/').filter { it.isNotEmpty() }

private fun getA2uiPath(model: JSONObject, path: String): Any? {
    var node: Any? = model
    for (segment in pathSegments(path)) {
        node = when (node) {
            is JSONObject -> node.opt(segment)
            is JSONArray -> segment.toIntOrNull()?.let(node::opt)
            else -> return null
        }
    }
    return node
}

private fun setA2uiPath(model: JSONObject, path: String, value: Any?) {
    val segments = pathSegments(path)
    if (segments.isEmpty()) return
    var node: Any = model
    for (segment in segments.dropLast(1)) {
        node = when (node) {
            is JSONObject -> node.optJSONObject(segment) ?: node.optJSONArray(segment)
                ?: JSONObject().also { node.put(segment, it) }
            is JSONArray -> segment.toIntOrNull()?.let(node::opt) ?: return
            else -> return
        }
    }
    val leaf = segments.last()
    when (node) {
        is JSONObject -> node.put(leaf, value)
        is JSONArray -> {
            val index = leaf.toIntOrNull() ?: return
            if (index !in 0 until node.length()) return
            replaceArrayItem(node, index, value)
        }
    }
}

/** Kuikly's JSONArray has no indexed setter, so rebuild it in place. */
private fun replaceArrayItem(array: JSONArray, index: Int, value: Any?) {
    val items = (0 until array.length()).map { if (it == index) value else array.opt(it) }
    while (array.length() > 0) array.remove(array.length() - 1)
    items.forEach { array.put(it) }
}
