package com.tencent.kuiklybase.chat.kuiklyai

import com.tencent.kuiklybase.chat.legacy.ai.LlmImageTransformState
import kotlin.test.Test
import kotlin.test.assertEquals

class LlmImageTransformStateTest {

    @Test
    fun doubleTapTogglesBetweenBaseAndPreviewScale() {
        val zoomed = LlmImageTransformState.Reset.doubleTap()
        assertEquals(2.5f, zoomed.scale)
        assertEquals(LlmImageTransformState.Reset, zoomed.doubleTap())
    }

    @Test
    fun zoomIsClampedAndPanReturnsInsideViewport() {
        val state = LlmImageTransformState.Reset
            .zoomBy(20f)
            .panBy(10_000f, -10_000f)
            .clamped(viewportWidth = 100f, viewportHeight = 100f)

        assertEquals(5f, state.scale)
        assertEquals(200f, state.offsetX)
        assertEquals(-200f, state.offsetY)
    }
}
