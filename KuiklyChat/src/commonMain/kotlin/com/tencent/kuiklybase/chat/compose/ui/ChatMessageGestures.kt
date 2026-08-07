package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.gestures.detectTapGestures
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.input.pointer.pointerInput
import com.tencent.kuikly.compose.ui.layout.boundsInRoot
import com.tencent.kuikly.compose.ui.layout.onGloballyPositioned
import com.tencent.kuiklybase.chat.compose.core.ChatMessage

/** Bubble bounds in window coordinates for context-menu anchoring (aligns with legacy long-press). */
internal data class MessagePressBounds(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
)

internal fun messageLongPressEnabled(callbacks: ChatCallbacks): Boolean =
    callbacks.onMessageLongPress != null || callbacks.onMessageLongPressWithPosition != null

internal fun dispatchMessageLongPress(
    message: ChatMessage,
    callbacks: ChatCallbacks,
    bounds: MessagePressBounds?,
) {
    val withPosition = callbacks.onMessageLongPressWithPosition
    if (withPosition != null && bounds != null) {
        withPosition.invoke(message, bounds.x, bounds.y, bounds.width, bounds.height)
    } else {
        callbacks.onMessageLongPress?.invoke(message)
    }
}

/**
 * Tap and long-press on a message bubble (Flyer `onMessageTap` / `onMessageLongPress`).
 *
 * When [onTap] is null, only long-press is registered (when [enableLongPress] is true).
 * Attachment nodes pass their own [onTap] and should set [enableLongPress] to false so the
 * parent bubble receives a single long-press for the whole message.
 */
@Composable
internal fun Modifier.chatMessageGestures(
    message: ChatMessage,
    callbacks: ChatCallbacks,
    onTap: (() -> Unit)?,
    enableLongPress: Boolean = true,
): Modifier {
    val longPress = enableLongPress && messageLongPressEnabled(callbacks)
    if (onTap == null && !longPress) {
        return this
    }
    val trackBounds = longPress && callbacks.onMessageLongPressWithPosition != null
    var bounds by remember(message.id) { mutableStateOf<MessagePressBounds?>(null) }
    return this
        .then(
            if (trackBounds) {
                Modifier.onGloballyPositioned { coordinates ->
                    val rect = coordinates.boundsInRoot()
                    bounds = MessagePressBounds(
                        x = rect.left,
                        y = rect.top,
                        width = rect.width,
                        height = rect.height,
                    )
                }
            } else {
                Modifier
            },
        )
        .pointerInput(
            message.id,
            onTap,
            callbacks.onMessageLongPress,
            callbacks.onMessageLongPressWithPosition,
        ) {
            detectTapGestures(
                onTap = { onTap?.invoke() },
                onLongPress = if (longPress) {
                    {
                        dispatchMessageLongPress(message, callbacks, bounds)
                    }
                } else {
                    null
                },
            )
        }
}
