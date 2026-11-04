package com.tencent.kuiklybase.chat.composer

import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.BoxShadow
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
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
                val theme = ctx.dockAttr().config.theme
                val count = ctx.dockAttr().previewCount
                val lines = ctx.dockAttr().lineCount.coerceIn(1, DOCK_MAX_INPUT_LINES)
                val expanded = ctx.dockAttr().expanded || count > 0
                height(composerPanelHeight(expanded, lines, count > 0, ctx.dockAttr().textAboveIcons))
                overflow(true)
                borderRadius(16f)
                backgroundColor(Color(theme.composerColor))
                border(Border(0.5f, BorderStyle.SOLID, Color(theme.borderColor)))
                boxShadow(BoxShadow(0f, 2f, 8f, Color(0x14000000)))
            }
            event { click { } }
        vif({ ctx.dockAttr().previewCount > 0 }) {
            ctx.attachmentStack(this)
        }
        vif({ ctx.dockAttr().previewCount == 0 }) {
            ctx.compactField(this)
            ctx.compactToolbar(this)
        }
        vif({ ctx.dockAttr().sheetMounted }) {
            ctx.renderAttachmentTray(this)
        }
    }
}

private fun ChatDockComposerView.attachmentStack(container: ViewContainer<*, *>) {
    val ctx = this
    container.View {
        attr { flexDirection(FlexDirection.COLUMN) }
        View {
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
        View {
            attr {
                val lines = ctx.dockAttr().lineCount.coerceIn(1, DOCK_MAX_INPUT_LINES)
                marginTop(DOCK_ATTACHMENT_TEXT_GAP)
                marginLeft(16f)
                marginRight(16f)
                height(15f + DOCK_INPUT_LINE_HEIGHT * lines)
            }
            ctx.composerField(this)
        }
        View {
            attr {
                marginTop(DOCK_TEXT_TOOLBAR_GAP)
                marginLeft(4f)
                marginRight(4f)
                marginBottom(DOCK_COMPACT_TOOLBAR_BOTTOM)
                height(DOCK_TOOLBAR_HEIGHT)
                flexDirection(FlexDirection.ROW)
                alignItems(FlexAlign.CENTER)
            }
            ctx.dockActions(this)
        }
    }
}

private fun ChatDockComposerView.compactField(container: ViewContainer<*, *>) {
    val ctx = this
    container.View {
        attr {
            val lines = ctx.dockAttr().lineCount.coerceIn(1, DOCK_MAX_INPUT_LINES)
            val textAbove = ctx.dockAttr().textAboveIcons || lines > 1
            val collapsedTextTop = (DOCK_COLLAPSED_HEIGHT - DOCK_INPUT_LINE_HEIGHT) / 2f
            absolutePosition(
                top = if (textAbove) DOCK_MULTILINE_TEXT_TOP else collapsedTextTop,
                left = if (textAbove) 16f else 56f,
                right = if (textAbove) 16f else 56f,
            )
            height(15f + DOCK_INPUT_LINE_HEIGHT * lines)
            zIndex(2)
        }
        ctx.composerField(this)
    }
}

private fun ChatDockComposerView.composerField(container: ViewContainer<*, *>) {
    val ctx = this
    container.TextArea {
        ref {
            ctx.textAreaRef = it
            val draft = ctx.dockAttr().draftText
            if (draft.isNotEmpty()) {
                it.view?.setText(draft)
            }
        }
        attr {
            val theme = ctx.dockAttr().config.theme
            absolutePosition(top = 0f, left = 0f, right = 0f)
            height(15f + DOCK_INPUT_LINE_HEIGHT * ctx.dockAttr().lineCount.coerceIn(1, DOCK_MAX_INPUT_LINES))
            fontSize(theme.fontSize)
            lineHeight(DOCK_INPUT_LINE_HEIGHT)
            color(Color(theme.incomingTextColor))
            placeholder(ctx.dockAttr().config.placeholder)
            placeholderColor(Color(theme.placeholderColor))
            lines(ctx.dockAttr().lineCount.coerceIn(1, DOCK_MAX_INPUT_LINES))
            returnKeyTypeSend()
        }
        event { ctx.bindComposerEvents(this) }
    }
}

private fun ChatDockComposerView.bindComposerEvents(event: TextAreaEvent) {
    val ctx = this
    event.textDidChange(isSyncEdit = true) { params ->
        val reflow = ctx.syncDraftLayout(params.text)
        if (reflow) {
            val cursor = params.text.length
            com.tencent.kuikly.core.timer.setTimeout(ctx.pagerId, 32) {
                ctx.textAreaRef?.view?.setCursorIndex(cursor)
            }
        }
    }
    event.inputFocus { ctx.onFocusChanged(true) }
    event.inputBlur {
        ctx.syncDraftLayout(it.text)
        ctx.onFocusChanged(false)
    }
    event.keyboardHeightChange { params ->
        ctx.onKeyboardChanged(params.height, params.height > 0.5f)
    }
    event.inputReturn { ctx.performSend() }
}

private fun ChatDockComposerView.compactToolbar(container: ViewContainer<*, *>) {
    val ctx = this
    container.View {
        attr {
            val lines = ctx.dockAttr().lineCount.coerceIn(1, DOCK_MAX_INPUT_LINES)
            val textAbove = ctx.dockAttr().textAboveIcons || lines > 1
            val panelHeight = composerPanelHeight(false, lines, false, textAbove)
            val toolbarHeight = DOCK_COMPACT_TOOLBAR_HEIGHT
            absolutePosition(
                left = 4f,
                right = 4f,
                top = panelHeight - DOCK_COMPACT_TOOLBAR_BOTTOM - toolbarHeight,
            )
            height(toolbarHeight)
            zIndex(6)
            flexDirection(FlexDirection.ROW)
            alignItems(FlexAlign.CENTER)
        }
        ctx.dockActions(this)
    }
}

private fun ChatDockComposerView.dockActions(container: ViewContainer<*, *>) {
    val ctx = this
    container.View {
        attr {
            flex(1f)
            flexDirection(FlexDirection.ROW)
            alignItems(FlexAlign.CENTER)
        }
        vif({ ctx.canAttach() }) {
            View {
                attr {
                    size(44f, 44f)
                    allCenter()
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
        View { attr { flex(1f) } }
        View {
            attr {
                val theme = ctx.dockAttr().config.theme
                val canSend = ctx.dockAttr().config.sending() ||
                    ctx.dockAttr().draftText.isNotBlank() ||
                    ctx.dockAttr().previewCount > 0
                size(44f, 44f)
                marginLeft(8f)
                borderRadius(22f)
                allCenter()
                backgroundColor(Color(if (canSend) theme.accentColor else theme.borderColor))
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
