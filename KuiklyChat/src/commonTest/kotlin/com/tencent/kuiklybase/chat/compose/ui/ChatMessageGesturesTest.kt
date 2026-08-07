package com.tencent.kuiklybase.chat.compose.ui

import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.compose.core.MessageStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatMessageGesturesTest {

    private val message = ChatMessage(
        id = "m1",
        author = ChatUser(id = "u1", name = "User"),
        parts = listOf(MessagePart.Text("hello")),
        status = MessageStatus.SENT,
        createdAt = 0L,
    )

    @Test
    fun messageLongPressEnabled_whenEitherCallbackSet() {
        assertFalse(messageLongPressEnabled(ChatCallbacks.Empty))
        assertTrue(
            messageLongPressEnabled(
                ChatCallbacks(onMessageLongPress = {}),
            ),
        )
        assertTrue(
            messageLongPressEnabled(
                ChatCallbacks(onMessageLongPressWithPosition = { _, _, _, _, _ -> }),
            ),
        )
    }

    @Test
    fun dispatchMessageLongPress_prefersPositionCallback() {
        var longPressCount = 0
        var positionCount = 0
        var lastBounds: List<Float>? = null
        val callbacks = ChatCallbacks(
            onMessageLongPress = { longPressCount++ },
            onMessageLongPressWithPosition = { _, x, y, w, h ->
                positionCount++
                lastBounds = listOf(x, y, w, h)
            },
        )
        val bounds = MessagePressBounds(x = 1f, y = 2f, width = 3f, height = 4f)

        dispatchMessageLongPress(message, callbacks, bounds)

        assertEquals(0, longPressCount)
        assertEquals(1, positionCount)
        assertEquals(listOf(1f, 2f, 3f, 4f), lastBounds)
    }

    @Test
    fun dispatchMessageLongPress_fallsBackWithoutBounds() {
        var longPressCount = 0
        var positionCount = 0
        val callbacks = ChatCallbacks(
            onMessageLongPress = { longPressCount++ },
            onMessageLongPressWithPosition = { _, _, _, _, _ -> positionCount++ },
        )

        dispatchMessageLongPress(message, callbacks, bounds = null)

        assertEquals(1, longPressCount)
        assertEquals(0, positionCount)
    }

    @Test
    fun dispatchMessageLongPress_fallsBackWhenOnlyLongPressSet() {
        var longPressCount = 0
        val callbacks = ChatCallbacks(onMessageLongPress = { longPressCount++ })

        dispatchMessageLongPress(
            message,
            callbacks,
            bounds = MessagePressBounds(0f, 0f, 10f, 10f),
        )

        assertEquals(1, longPressCount)
    }
}
