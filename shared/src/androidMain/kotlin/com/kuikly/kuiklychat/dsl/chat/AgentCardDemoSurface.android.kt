package com.kuikly.kuiklychat.dsl.chat

import com.tencent.kuikly.a2ui.dsl.embed.A2UISurface
import com.tencent.kuikly.core.base.ViewContainer

internal actual fun ViewContainer<*, *>.renderA2uiSurface(
    jsonl: String,
    width: Float,
    onAction: (actionJson: String) -> Unit,
    onSurface: (writeLine: (String) -> Unit) -> Unit,
    onDataModelChanged: (dataModelJson: String) -> Unit,
) {
    // The chat row is rebuilt whenever the message grows, so each surface
    // receives the complete JSONL so far and replays it from scratch.
    A2UISurface {
        val surface = this
        attr { width(width) }
        event {
            action { _, _, actionJson -> onAction(actionJson) }
            dataModelChanged { dataModelJson, _ -> onDataModelChanged(dataModelJson) }
        }
        writeStream(jsonl)
        finishStream()
        onSurface { line ->
            surface.writeStream(line)
            surface.finishStream()
        }
    }
}
