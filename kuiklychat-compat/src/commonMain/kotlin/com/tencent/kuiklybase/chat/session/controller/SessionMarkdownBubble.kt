package com.tencent.kuiklybase.chat.session.controller

import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.View
import com.tencent.kuiklybase.KuiklyStreamingMarkdown
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.config.MarkdownConfig
import com.tencent.kuiklybase.chat.compose.core.TextFormat
import com.tencent.kuiklybase.streaming.MarkdownBlock
import com.tencent.kuiklybase.streaming.MarkdownStreamingState

internal fun ChatControllerSessionView.displayText(messageId: String): String {
    val attr = sessionAttr()
    if (messageId.isNotEmpty() && messageId == attr.liveMessageId) return attr.liveText
    return messageContext(messageId)?.first?.text.orEmpty()
}

internal fun ChatControllerSessionView.markdownText(messageId: String): String? {
    val message = messageContext(messageId)?.first ?: return null
    val text = message.parts.filterIsInstance<MessagePart.Text>().firstOrNull() ?: return null
    if (text.format != TextFormat.MARKDOWN) return null
    val value = displayText(messageId)
    if (value.isEmpty()) return null
    return value
}

internal fun ChatControllerSessionView.markdownBubble(
    container: ViewContainer<*, *>,
    messageId: String,
) {
    val host = this
    container.addChild(SessionMarkdownView()) {
        bind(host, messageId)
        attr {
            layoutWidth = (host.pagerData.pageViewWidth - 96f).coerceAtLeast(200f)
            markdownConfig = sessionMarkdownConfig(
                theme = host.sessionAttr().theme,
                outgoing = host.isMine(messageId),
            )
            val next = host.displayText(messageId)
            if (source != next) source = next
        }
    }
}

internal class SessionMarkdownView : ComposeView<SessionMarkdownAttr, ComposeEvent>() {
    private var boundHost: ChatControllerSessionView? = null
    private var boundMessageId: String = ""

    internal fun bind(host: ChatControllerSessionView, messageId: String) {
        boundHost = host
        boundMessageId = messageId
        host.markdownViews[messageId] = this
    }

    internal fun pushText(text: String) {
        if (attr.source == text) return
        attr.source = text
    }

    override fun viewWillUnload() {
        val host = boundHost
        if (host != null && host.markdownViews[boundMessageId] === this) {
            host.markdownViews.remove(boundMessageId)
        }
        super.viewWillUnload()
    }
    private val streamingState = MarkdownStreamingState()
    private var blocks by observableList<MarkdownBlock>()

    override fun createAttr(): SessionMarkdownAttr {
        return SessionMarkdownAttr().also { created ->
            created.onSource = { text ->
                val next = streamingState.update(text, force = true)
                if (next != null) {
                    (blocks as ObservableList<MarkdownBlock>).diffUpdate(next) { old, new ->
                        old.id == new.id
                    }
                }
            }
        }
    }

    override fun createEvent(): ComposeEvent = ComposeEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    flexDirection(FlexDirection.COLUMN)
                    width(ctx.attr.layoutWidth)
                }
                vfor({ ctx.blocks }) { block ->
                    View {
                        KuiklyStreamingMarkdown(
                            state = ctx.streamingState,
                            block = block,
                            config = ctx.attr.markdownConfig,
                            components = sessionMarkdownComponents(),
                        )
                    }
                }
            }
        }
    }
}

internal class SessionMarkdownAttr : ComposeAttr() {
    var source: String = ""
        set(value) {
            if (field == value) return
            field = value
            onSource(value)
        }
    var layoutWidth: Float = 280f
    var markdownConfig: MarkdownConfig = MarkdownConfig.Default
    var onSource: (String) -> Unit = {}
}
