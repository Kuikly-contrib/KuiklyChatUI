package com.kuikly.kuiklychat.dsl.chat

import com.tencent.kuikly.core.base.ViewContainer

internal actual fun ViewContainer<*, *>.renderA2uiSurface(
    jsonl: String,
    width: Float,
    onAction: (actionJson: String) -> Unit,
    onSurface: (writeLine: (String) -> Unit) -> Unit,
    onDataModelChanged: (dataModelJson: String) -> Unit,
) = renderA2uiFallback(jsonl, width)

