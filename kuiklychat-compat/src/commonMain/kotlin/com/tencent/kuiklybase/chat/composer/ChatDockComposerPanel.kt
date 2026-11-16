package com.tencent.kuiklybase.chat.composer

import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.BoxShadow
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.attr.AccessibilityRole
import com.tencent.kuikly.core.directives.vbind
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.TextArea
import com.tencent.kuikly.core.views.TextAreaEvent
import com.tencent.kuikly.core.views.View
internal fun ChatDockComposerView.renderDock(container: ViewContainer<*, *>) {
    val ctx = this
    container.View {
        attr {
            flexDirectionColumn()
            overflow(true)
            borderRadius(16f)
            backgroundColor(Color(ctx.dockAttr().config.theme.composerColor))
            border(Border(0.5f, BorderStyle.SOLID, Color(0x00000000)))
            boxShadow(BoxShadow(0f, 2f, 8f, Color(0x14000000)))
        }
        vif({ ctx.dockAttr().previewCount > 0 }) {
            ctx.attachmentPreviews(this)
        }
        // One field and one width for all drafts, including attachment mode.
        View {
            attr {
                flexDirectionRow()
                alignItems(FlexAlign.FLEX_END)
                paddingLeft(4f)
                paddingRight(4f)
                paddingTop(DOCK_VERTICAL_PADDING)
                paddingBottom(DOCK_VERTICAL_PADDING)
            }
            ctx.attachmentButton(this)
            ctx.composerField(this)
            ctx.sendButton(this)
        }
        vif({ ctx.dockAttr().sheetMounted }) {
            ctx.renderAttachmentTray(this)
        }
    }
}

private fun ChatDockComposerView.attachmentPreviews(container: ViewContainer<*, *>) {
    val ctx = this
    container.View {
        attr {
            marginTop(9f)
            marginLeft(14f)
            marginRight(14f)
            height(70f)
        }
        ChatAttachmentStrip(
            previewUris = { ctx.dockAttr().previews() },
            borderColor = ctx.dockAttr().config.theme.borderColor,
            recessedColor = ctx.dockAttr().config.theme.systemBackgroundColor,
            onRemove = { ctx.removePreview(it) },
            onPreview = { ctx.previewAttachment(it) },
        )
    }
}

private fun ChatDockComposerView.composerField(container: ViewContainer<*, *>) {
    val ctx = this
    container.TextArea {
        ref {
            ctx.textAreaRef = it
        }
        attr {
            val theme = ctx.dockAttr().config.theme
            // Leave height/lines unset so TextArea's shadow measures actual glyphs and wrapping.
            flex(1f)
            marginLeft(8f)
            marginRight(8f)
            minHeight(DOCK_ACTION_SIZE)
            maxHeight(composerMaxInputHeight(theme.fontSize))
            fontSize(theme.fontSize)
            lineHeight(composerInputLineHeight(theme.fontSize))
            color(Color(theme.incomingTextColor))
            placeholder(ctx.dockAttr().config.placeholder)
            placeholderColor(Color(theme.placeholderColor))
            returnKeyTypeSend()
        }
        event { ctx.bindComposerEvents(this) }
    }
}

private fun ChatDockComposerView.bindComposerEvents(event: TextAreaEvent) {
    val ctx = this
    event.textDidChange(isSyncEdit = true) { params ->
        ctx.updateDraft(params.text)
    }
    event.inputFocus { ctx.onFocusChanged(true) }
    event.inputBlur {
        ctx.updateDraft(it.text)
        ctx.onFocusChanged(false)
    }
    event.keyboardHeightChange { params ->
        ctx.onKeyboardChanged(params.height, params.height > 0.5f)
    }
    event.inputReturn { ctx.performSend() }
}

private fun ChatDockComposerView.attachmentButton(container: ViewContainer<*, *>) {
    val ctx = this
    container.apply {
        vif({ ctx.canAttach() }) {
            View {
                attr {
                    size(DOCK_ACTION_SIZE, DOCK_ACTION_SIZE)
                    allCenter()
                    accessibility(ctx.dockAttr().config.addAttachmentLabel)
                    accessibilityRole(AccessibilityRole.BUTTON)
                    accessibilityInfo(clickable = true, longClickable = false)
                }
                event { click { ctx.toggleAttachmentTray() } }
                Text {
                    attr {
                        text("+")
                        fontSize(24f)
                        color(Color(ctx.dockAttr().config.theme.incomingTextColor))
                    }
                }
            }
        }
    }
}

private fun ChatDockComposerView.sendButton(container: ViewContainer<*, *>) {
    val ctx = this
    container.View {
        attr {
            val theme = ctx.dockAttr().config.theme
            val canSend = ctx.dockAttr().config.sending() ||
                ctx.dockAttr().draftText.isNotBlank() ||
                ctx.dockAttr().previewCount > 0
            size(DOCK_ACTION_SIZE, DOCK_ACTION_SIZE)
            borderRadius(22f)
            allCenter()
            backgroundColor(Color(if (canSend) theme.accentColor else theme.borderColor))
            accessibility(if (ctx.dockAttr().config.sending()) ctx.dockAttr().config.stopLabel else ctx.dockAttr().config.sendLabel)
            accessibilityRole(AccessibilityRole.BUTTON)
            accessibilityInfo(clickable = true, longClickable = false)
        }
        event { click { ctx.performSend() } }
        vbind({
            val sending = ctx.dockAttr().config.sending()
            val ready = ctx.dockAttr().draftText.isNotBlank() || ctx.dockAttr().previewCount > 0
            if (sending) 2 else if (ready) 1 else 0
        }) {
            val theme = ctx.dockAttr().config.theme
            if (ctx.dockAttr().config.sending()) {
                View {
                    attr {
                        size(10f, 10f)
                        borderRadius(2f)
                        backgroundColor(Color(theme.outgoingTextColor))
                    }
                }
            } else {
                val ready = ctx.dockAttr().draftText.isNotBlank() || ctx.dockAttr().previewCount > 0
                arrowUpMark(
                    this,
                    if (ready) theme.outgoingTextColor else theme.placeholderColor,
                )
            }
        }
    }
}

/** Lucide arrow-up: m5 12 7-7 7 7 and M12 19V5, 24×24, stroke 2, round cap. */
private fun arrowUpMark(container: ViewContainer<*, *>, color: Long) {
    container.Canvas({
        attr { size(22f, 22f) }
    }) { context, width, height ->
        val scale = minOf(width, height) / 24f
        context.strokeStyle(Color(color))
        context.lineWidth(2f * scale)
        context.lineCapRound()
        context.beginPath()
        context.moveTo(5f * scale, 12f * scale)
        context.lineTo(12f * scale, 5f * scale)
        context.lineTo(19f * scale, 12f * scale)
        context.stroke()
        context.beginPath()
        context.moveTo(12f * scale, 19f * scale)
        context.lineTo(12f * scale, 5f * scale)
        context.stroke()
    }
}
