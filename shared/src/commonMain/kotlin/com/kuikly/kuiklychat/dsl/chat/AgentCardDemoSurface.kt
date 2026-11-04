package com.kuikly.kuiklychat.dsl.chat

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.Text

/**
 * Renders the accumulated A2UI JSONL for one assistant message.
 *
 * Android renders with the published KuiklyUI-A2UI SDK (`A2UISurface`). Other
 * platforms draw a plain text fallback. [onAction] receives the
 * raw A2UI action JSON (`{"event":{...}}` or `{"functionCall":{...}}`).
 *
 * [onSurface] hands back a writer that pushes extra JSONL lines into the live
 * surface (used for `local.*` updates). [onDataModelChanged] reports the full data
 * model after two-way bound inputs change it.
 */
internal expect fun ViewContainer<*, *>.renderA2uiSurface(
    jsonl: String,
    width: Float,
    onAction: (actionJson: String) -> Unit,
    onSurface: (writeLine: (String) -> Unit) -> Unit = {},
    onDataModelChanged: (dataModelJson: String) -> Unit = {},
)

/** Plain text summary for hosts without the A2UI SDK; literal `text` values only. */
internal fun ViewContainer<*, *>.renderA2uiFallback(jsonl: String, width: Float) {
    val texts = Regex("\"text\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").findAll(jsonl).map { it.groupValues[1] }
    Text {
        attr {
            width(width)
            text(texts.joinToString("\n").ifEmpty { "当前平台未接入 A2UI 渲染" })
            fontSize(14f)
            lineHeight(20f)
            color(Color(0xFF24313A))
        }
    }
}
