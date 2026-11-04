package com.tencent.kuiklybase.chat.composer

import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.Translate
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.Modal
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmAttachmentSource

private const val SHEET_MOTION_KEY = "attachment-sheet"
private const val SCRIM_MOTION_KEY = "attachment-scrim"

internal fun ChatDockComposerView.renderAttachmentTray(container: ViewContainer<*, *>) {
    val ctx = this
    container.Modal(inWindow = true) {
        View {
            attr {
                absolutePositionAllZero()
                backgroundColor(Color(0x66000000))
                opacity(if (ctx.dockAttr().sheetPresented) 1f else 0f)
                animate(Animation.easeOut(0.2f, key = SCRIM_MOTION_KEY), ctx.dockAttr().sheetPresented)
            }
            event { click { ctx.closeAttachmentSheet() } }
            View {
                attr {
                    val theme = ctx.dockAttr().config.theme
                    val presented = ctx.dockAttr().sheetPresented
                    positionAbsolute()
                    left(0f)
                    right(0f)
                    bottom(0f)
                    backgroundColor(Color(theme.composerColor))
                    borderRadius(20f, 20f, 0f, 0f)
                    paddingTop(8f)
                    paddingLeft(16f)
                    paddingRight(16f)
                    paddingBottom(ctx.pagerData.safeAreaInsets.bottom + 8f)
                    flexDirection(FlexDirection.COLUMN)
                    alignItems(FlexAlign.CENTER)
                    transform(Translate(0f, if (presented) 0f else 1f))
                    animate(Animation.easeOut(0.26f, key = SHEET_MOTION_KEY), presented)
                }
                event {
                    click { }
                    animationCompletion {
                        if (!ctx.dockAttr().sheetPresented && !ctx.dockAttr().attachmentTrayOpen) {
                            ctx.dockAttr().sheetMounted = false
                        }
                    }
                }
                View {
                    attr {
                        width(36f)
                        height(4f)
                        marginTop(4f)
                        marginBottom(14f)
                        borderRadius(2f)
                        backgroundColor(Color(ctx.dockAttr().config.theme.borderColor))
                    }
                }
                Text {
                    attr {
                        alignSelfStretch()
                        text(ctx.dockAttr().config.attachmentTitle)
                        fontSize(15f)
                        fontWeightMedium()
                        color(Color(ctx.dockAttr().config.theme.incomingTextColor))
                        marginBottom(12f)
                        marginLeft(4f)
                    }
                }
                View {
                    attr {
                        alignSelfStretch()
                        backgroundColor(Color(ctx.dockAttr().config.theme.backgroundColor))
                        borderRadius(14f)
                        border(
                            Border(
                                0.5f,
                                BorderStyle.SOLID,
                                Color(ctx.dockAttr().config.theme.borderColor),
                            ),
                        )
                        flexDirection(FlexDirection.COLUMN)
                        overflow(true)
                    }
                    vif({ ctx.dockAttr().showCamera }) {
                        ctx.sheetAction(this, ctx.dockAttr().config.cameraLabel, camera = true) {
                            ctx.pickAttachment(LlmAttachmentSource.CAMERA)
                        }
                    }
                    vif({ ctx.dockAttr().showCamera && ctx.dockAttr().showGallery }) {
                        View {
                            attr {
                                height(0.5f)
                                marginLeft(52f)
                                backgroundColor(Color(ctx.dockAttr().config.theme.borderColor))
                            }
                        }
                    }
                    vif({ ctx.dockAttr().showGallery }) {
                        ctx.sheetAction(this, ctx.dockAttr().config.galleryLabel, camera = false) {
                            ctx.pickAttachment(LlmAttachmentSource.PHOTO_LIBRARY)
                        }
                    }
                }
                View {
                    attr {
                        alignSelfStretch()
                        height(52f)
                        marginTop(8f)
                        borderRadius(14f)
                        backgroundColor(Color(ctx.dockAttr().config.theme.backgroundColor))
                        border(
                            Border(
                                0.5f,
                                BorderStyle.SOLID,
                                Color(ctx.dockAttr().config.theme.borderColor),
                            ),
                        )
                        allCenter()
                    }
                    event { click { ctx.closeAttachmentSheet() } }
                    Text {
                        attr {
                            text("取消")
                            fontSize(16f)
                            fontWeightMedium()
                            color(Color(ctx.dockAttr().config.theme.incomingTextColor))
                        }
                    }
                }
            }
        }
    }
}

private fun ChatDockComposerView.sheetAction(
    container: ViewContainer<*, *>,
    label: String,
    camera: Boolean,
    onClick: () -> Unit,
) {
    val theme = dockAttr().config.theme
    container.View {
        attr {
            height(56f)
            flexDirection(FlexDirection.ROW)
            alignItems(FlexAlign.CENTER)
            paddingLeft(16f)
            paddingRight(16f)
        }
        event { click { onClick() } }
        if (camera) {
            cameraMark(this, theme.incomingTextColor)
        } else {
            galleryMark(this, theme.incomingTextColor)
        }
        Text {
            attr {
                text(label)
                fontSize(16f)
                color(Color(theme.incomingTextColor))
                marginLeft(12f)
            }
        }
    }
}

private fun cameraMark(container: ViewContainer<*, *>, color: Long) {
    container.Canvas({
        attr { size(24f, 24f) }
    }) { context, width, height ->
        val scale = minOf(width, height) / 24f
        context.strokeStyle(Color(color))
        context.lineWidth(1.8f * scale)
        context.lineCapRound()
        context.beginPath()
        context.moveTo(4f * scale, 8f * scale)
        context.lineTo(20f * scale, 8f * scale)
        context.lineTo(20f * scale, 19f * scale)
        context.lineTo(4f * scale, 19f * scale)
        context.closePath()
        context.stroke()
        context.beginPath()
        context.moveTo(9f * scale, 8f * scale)
        context.lineTo(10.5f * scale, 5.5f * scale)
        context.lineTo(13.5f * scale, 5.5f * scale)
        context.lineTo(15f * scale, 8f * scale)
        context.stroke()
        context.beginPath()
        context.arc(12f * scale, 13.5f * scale, 2.6f * scale, 0f, 6.283185f, false)
        context.stroke()
    }
}

private fun galleryMark(container: ViewContainer<*, *>, color: Long) {
    container.Canvas({
        attr { size(24f, 24f) }
    }) { context, width, height ->
        val scale = minOf(width, height) / 24f
        context.strokeStyle(Color(color))
        context.lineWidth(1.8f * scale)
        context.lineCapRound()
        context.beginPath()
        context.moveTo(4f * scale, 6f * scale)
        context.lineTo(20f * scale, 6f * scale)
        context.lineTo(20f * scale, 18f * scale)
        context.lineTo(4f * scale, 18f * scale)
        context.closePath()
        context.stroke()
        context.beginPath()
        context.arc(8.5f * scale, 9.5f * scale, 1.2f * scale, 0f, 6.283185f, false)
        context.stroke()
        context.beginPath()
        context.moveTo(5f * scale, 16f * scale)
        context.lineTo(9f * scale, 12f * scale)
        context.lineTo(12f * scale, 15f * scale)
        context.lineTo(16f * scale, 10f * scale)
        context.lineTo(19f * scale, 14f * scale)
        context.stroke()
    }
}
