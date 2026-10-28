package com.kuikly.kuiklychat.dsl.chat

import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * Kuikly's flex engine measures non-flex Row children without a width limit, so a
 * long Text in a Row is measured as one line, wraps onto its own flex line and then
 * overflows the card. Giving the main Text `flex: 1` lets the engine hand it the
 * remaining width, so it wraps beside its icon or label instead.
 */
internal fun normalizeA2uiLayout(line: String): String {
    val envelope = runCatching { JSONObject(line) }.getOrNull() ?: return line
    // Two-way bound inputs (Slider, CheckBox, ...) only report data changes back to the
    // host when the surface opts in; the host keeps that state so AI actions can use it.
    envelope.optJSONObject("createSurface")?.let { create ->
        create.put("sendDataModel", true)
        return envelope.toString()
    }
    val update = envelope.optJSONObject("updateComponents") ?: return line
    val components = update.optJSONArray("components") ?: return line
    val byId = HashMap<String, JSONObject>()
    for (index in 0 until components.length()) {
        val component = components.optJSONObject(index) ?: continue
        component.optString("id").takeIf { it.isNotEmpty() }?.let { byId[it] = component }
    }
    var changed = false
    for (row in byId.values) {
        if (row.optString("component") != "Row") continue
        val children = row.optJSONArray("children") ?: continue
        val target = flexTargetFor(row, children, byId) ?: continue
        val styles = target.optJSONObject("styles") ?: JSONObject().also { target.put("styles", it) }
        styles.put("flex", 1)
        changed = true
    }
    return if (changed) envelope.toString() else line
}

private fun flexTargetFor(row: JSONObject, children: JSONArray, byId: Map<String, JSONObject>): JSONObject? {
    val childComponents = (0 until children.length()).mapNotNull { index ->
        children.optString(index)?.let(byId::get)
    }
    if (childComponents.any { it.optJSONObject("styles")?.has("flex") == true }) return null
    val texts = childComponents.filter { it.optString("component") == "Text" }
    if (texts.isEmpty()) return null
    return when (row.optString("justify")) {
        "", "start" -> texts.last()
        // Stretch the leading label so trailing values stay right-aligned.
        "spaceBetween" -> texts.first()
        // Centered or end-aligned rows would lose their alignment if a Text filled them.
        else -> null
    }
}
