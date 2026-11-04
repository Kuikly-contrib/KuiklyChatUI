package com.tencent.kuiklybase.chat.session.controller

import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.View

internal fun ChatControllerSessionView.openSentImagePreview(uri: String) {
    sessionAttr().previewImageUri = uri
}

internal fun ChatControllerSessionView.imagePreviewOverlay(container: ViewContainer<*, *>) {
    val host = this
    container.View {
        attr {
            positionAbsolute()
            top(0f)
            left(0f)
            right(0f)
            bottom(0f)
            backgroundColor(Color.BLACK)
            zIndex(30)
        }
        ChatZoomImage {
            attr {
                positionAbsolute()
                top(host.pagerData.statusBarHeight)
                left(0f)
                right(0f)
                bottom(host.pagerData.safeAreaInsets.bottom)
                src(host.sessionAttr().previewImageUri)
            }
            event {
                close {
                    host.sessionAttr().previewImageUri = ""
                }
            }
        }
        View {
            attr {
                positionAbsolute()
                top(host.pagerData.statusBarHeight + 8f)
                left(16f)
                size(44f, 44f)
                borderRadius(22f)
                backgroundColor(Color(0x66000000))
                border(Border(1f, BorderStyle.SOLID, Color(0x66FFFFFF)))
                allCenter()
                zIndex(2)
            }
            event {
                click {
                    host.sessionAttr().previewImageUri = ""
                }
            }
            previewBackMark(this)
        }
    }
}

private fun previewBackMark(container: ViewContainer<*, *>) {
    container.Canvas({
        attr { size(22f, 22f) }
    }) { context, width, height ->
        val markScale = minOf(width, height) / 24f
        context.strokeStyle(Color.WHITE)
        context.lineWidth(2f * markScale)
        context.lineCapRound()
        context.beginPath()
        context.moveTo(15f * markScale, 6f * markScale)
        context.lineTo(8f * markScale, 12f * markScale)
        context.lineTo(15f * markScale, 18f * markScale)
        context.stroke()
    }
}
