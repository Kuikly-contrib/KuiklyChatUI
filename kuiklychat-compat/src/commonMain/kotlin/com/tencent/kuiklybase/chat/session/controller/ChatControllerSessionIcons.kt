package com.tencent.kuiklybase.chat.session.controller

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.Canvas

internal fun ViewContainer<*, *>.scrollDownMark(color: Long) {
    Canvas({ attr { size(22f, 22f) } }) { context, width, height ->
        val scale = minOf(width, height) / 24f
        context.strokeStyle(Color(color))
        context.lineWidth(2f * scale)
        context.lineCapRound()
        context.beginPath()
        context.moveTo(7f * scale, 6f * scale)
        context.lineTo(12f * scale, 11f * scale)
        context.lineTo(17f * scale, 6f * scale)
        context.stroke()
        context.beginPath()
        context.moveTo(7f * scale, 13f * scale)
        context.lineTo(12f * scale, 18f * scale)
        context.lineTo(17f * scale, 13f * scale)
        context.stroke()
    }
}
