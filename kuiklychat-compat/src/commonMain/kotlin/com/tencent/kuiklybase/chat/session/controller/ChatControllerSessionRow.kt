package com.tencent.kuiklybase.chat.session.controller

import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.directives.velse
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuiklybase.chat.agentcard.AgentCard
import com.tencent.kuiklybase.chat.compose.core.ChatIntent
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatStrings
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.compose.core.MessageStatus
import com.tencent.kuiklybase.chat.compose.core.systemNoticeOrNull

internal fun ViewContainer<*, *>.controllerSessionRow(
    host: ChatControllerSessionView,
    messageId: String,
) {
    View {
        attr {
            alignSelfStretch()
            flex(0f)
            // Opaque fill keeps this row from flattening into the list.
            // A flattened row lets flex(1) children grow and cover the messages.
            backgroundColor(Color(host.sessionAttr().theme.backgroundColor))
        }
        vif({ host.dateLabel(messageId) != null }) {
            View {
                attr {
                    allCenter()
                    marginTop(8f)
                    marginBottom(4f)
                }
                Text {
                    attr {
                        text(host.dateLabel(messageId).orEmpty())
                        fontSize(11f)
                        color(Color(host.sessionAttr().theme.timeTextColor))
                    }
                }
            }
        }
        vif({ host.noticeText(messageId) != null }) {
            Text {
                attr {
                    text(host.noticeText(messageId).orEmpty())
                    fontSize(13f)
                    textAlignCenter()
                    color(Color(host.sessionAttr().theme.systemTextColor))
                    marginTop(8f)
                    marginBottom(8f)
                    marginLeft(24f)
                    marginRight(24f)
                }
            }
        }
        velse {
            host.bubbleRow(this, messageId)
        }
    }
}

private fun ChatControllerSessionView.bubbleRow(
    container: ViewContainer<*, *>,
    messageId: String,
) {
    val host = this
    container.View {
        attr {
            flexDirection(FlexDirection.COLUMN)
            marginLeft(16f)
            marginRight(16f)
            marginBottom(16f)
            marginTop(4f)
        }
        event {
            click {
                val current = host.messageContext(messageId)?.first ?: return@click
                if (current.status == MessageStatus.FAILED) {
                    host.sessionAttr().controller.dispatch(ChatIntent.RetryMessage(current.id))
                }
            }
        }
        vif({ host.isMine(messageId) }) {
            View {
                attr {
                    flexDirection(FlexDirection.ROW)
                    justifyContent(FlexJustifyContent.FLEX_END)
                    alignSelfStretch()
                }
                View {
                    attr {
                        borderRadius(host.sessionAttr().theme.bubbleRadius)
                        backgroundColor(Color(host.sessionAttr().theme.outgoingBubbleColor))
                        border(
                            Border(
                                0.5f,
                                BorderStyle.SOLID,
                                Color(host.sessionAttr().theme.borderColor),
                            ),
                        )
                        padding(top = 12f, left = 16f, bottom = 12f, right = 16f)
                    }
                    host.messageBody(this, messageId, outgoing = true)
                }
            }
        }
        velse {
            host.messageBody(this, messageId, outgoing = false)
        }
        vif({ host.statusText(messageId).isNotEmpty() }) {
            Text {
                attr {
                    val placed = host.messageContext(messageId)?.first
                    text(host.statusText(messageId))
                    fontSize(12f)
                    marginTop(6f)
                    if (host.isMine(messageId)) {
                        alignSelfFlexEnd()
                    }
                    color(
                        Color(
                            if (placed?.status == MessageStatus.FAILED) host.sessionAttr().theme.errorColor
                            else host.sessionAttr().theme.timeTextColor
                        )
                    )
                }
            }
        }
    }
}

internal fun ChatControllerSessionView.dateLabel(messageId: String): String? {
    if (!sessionAttr().showDateSeparators) return null
    val context = messageContext(messageId) ?: return null
    return dateLabelBetween(context.second, context.first)
}

internal fun ChatControllerSessionView.noticeText(messageId: String): String? {
    return messageContext(messageId)?.first?.systemNoticeOrNull?.text
}

internal fun ChatControllerSessionView.messageContext(
    messageId: String,
): Triple<ChatMessage, ChatMessage?, ChatMessage?>? {
    val epoch = sessionAttr().snapshotEpoch
    if (epoch < 0) return null
    val messages = sessionAttr().messages
    val index = messages.indexOfFirst { it.id == messageId }
    if (index < 0) return null
    return Triple(messages[index], messages.getOrNull(index - 1), messages.getOrNull(index + 1))
}

internal fun ChatControllerSessionView.isMine(messageId: String): Boolean {
    val message = messageContext(messageId)?.first ?: return false
    return message.outgoing || message.author.id == sessionAttr().controller.currentUser.id
}

private fun ChatControllerSessionView.statusText(messageId: String): String {
    val message = messageContext(messageId)?.first ?: return ""
    return statusLabel(message, sessionAttr().strings)
}

private fun ChatControllerSessionView.dateLabelBetween(
    previous: ChatMessage?,
    current: ChatMessage,
): String? {
    if (!sessionAttr().showDateSeparators) return null
    return sessionAttr().dateTimeFormatter.dateSeparatorLabel(
        previousCreatedAt = previous?.createdAt,
        currentCreatedAt = current.createdAt,
        nowMillis = sessionAttr().dateSeparatorClock.nowMillis(),
        strings = sessionAttr().strings,
    )
}

private fun ChatControllerSessionView.messageBody(
    container: ViewContainer<*, *>,
    messageId: String,
    outgoing: Boolean,
) {
    val host = this
    container.View {
        attr { flexDirection(FlexDirection.COLUMN) }
        val message = host.messageContext(messageId)?.first
        val drewCustom = message != null &&
            host.sessionAttr().renderMessage?.invoke(this, message) == true
        vif({ host.imageSources(messageId).isNotEmpty() }) {
            host.messageImages(this, messageId)
        }
        vif({ !drewCustom && host.markdownText(messageId) != null }) {
            host.markdownBubble(this, messageId)
        }
        vif({
            !drewCustom &&
                host.markdownText(messageId) == null &&
                host.bubbleText(messageId).isNotEmpty()
        }) {
            Text {
                attr {
                    text(host.bubbleText(messageId))
                    fontSize(host.sessionAttr().theme.fontSize)
                    lineHeight(host.sessionAttr().theme.fontSize + 6f)
                    if (outgoing) {
                        maxWidth(host.pagerData.pageViewWidth - 96f)
                    }
                    color(
                        Color(
                            if (outgoing) host.sessionAttr().theme.outgoingTextColor
                            else host.sessionAttr().theme.incomingTextColor
                        )
                    )
                }
            }
        }
    }
}

private fun ChatControllerSessionView.messageImages(container: ViewContainer<*, *>, messageId: String) {
    val host = this
    container.addChild(MessageImageStripView()) {
        attr {
            val next = host.imageSources(messageId)
            if (sources.toList() != next) {
                sources.clear()
                sources.addAll(next)
            }
            onPreview = { host.openSentImagePreview(it) }
        }
    }
}

private class MessageImageStripView : ComposeView<MessageImageStripAttr, ComposeEvent>() {
    override fun createAttr(): MessageImageStripAttr = MessageImageStripAttr()
    override fun createEvent(): ComposeEvent = ComposeEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr { flexDirection(FlexDirection.COLUMN) }
                vfor({ ctx.attr.sources }) { source ->
                    Image {
                        attr {
                            size(180f, 120f)
                            borderRadius(12f)
                            resizeCover()
                        src(source)
                        marginBottom(8f)
                    }
                    event { click { ctx.attr.onPreview(source) } }
                    }
                }
            }
        }
    }
}

private class MessageImageStripAttr : ComposeAttr() {
    var sources by observableList<String>()
    var onPreview: (String) -> Unit = {}
}

private fun ChatControllerSessionView.imageSources(messageId: String): Collection<String> {
    val message = messageContext(messageId)?.first ?: return emptyList()
    return message.parts.filterIsInstance<MessagePart.Attachment>().mapNotNull { part ->
        val source = part.previewUri.ifEmpty { part.uri }
        val image = part.mimeType.startsWith("image/") || source.startsWith("data:image")
        if (source.isNotEmpty() && image) source else null
    }
}

private fun ChatControllerSessionView.bubbleText(messageId: String): String {
    val message = messageContext(messageId)?.first ?: return ""
    if (message.text.isNotEmpty()) return message.text
    if (imageSources(messageId).isNotEmpty()) return ""
    return bodyText(message)
}

private fun statusLabel(message: ChatMessage, strings: ChatStrings): String {
    return when (message.status) {
        MessageStatus.SENDING -> strings.sending
        MessageStatus.STREAMING -> strings.streaming
        MessageStatus.FAILED -> strings.retry
        MessageStatus.CANCELLED -> strings.cancelled
        MessageStatus.SENT -> ""
    }
}

private fun bodyText(message: ChatMessage): String {
    if (message.text.isNotEmpty()) return message.text
    val attachment = message.parts.filterIsInstance<MessagePart.Attachment>().firstOrNull()
    if (attachment != null) return attachment.name.ifEmpty { attachment.uri }
    val card = message.parts.filterIsInstance<MessagePart.AgentCardContent>().firstOrNull()
    if (card != null) return cardLabel(card.card)
    val custom = message.parts.filterIsInstance<MessagePart.Custom>().firstOrNull()
    if (custom != null) return custom.type
    return ""
}

private fun cardLabel(card: AgentCard): String {
    return when (card) {
        is AgentCard.TextMessage -> card.content
        is AgentCard.UserMessage -> card.content
        is AgentCard.CodeBlock -> card.filename.ifEmpty { "代码" }
        is AgentCard.ToolCall -> card.toolName
        is AgentCard.Thinking -> "思考"
        is AgentCard.Error -> card.message
        is AgentCard.Plan -> card.title
        is AgentCard.PhaseHeader -> card.phaseName
        is AgentCard.TodoProgress -> "进度 ${card.completed}/${card.total}"
        is AgentCard.Custom -> card.title.ifEmpty { card.type }
        is AgentCard.CommandExec -> card.command
        is AgentCard.FileEdit -> card.filePath
        is AgentCard.SearchResult -> card.query.ifEmpty { "搜索" }
        is AgentCard.Loading -> card.title
        is AgentCard.LinkPreview -> card.title.ifEmpty { card.url }
        is AgentCard.Table -> card.title.ifEmpty { "表格" }
        is AgentCard.Chart -> card.title.ifEmpty { "图表" }
    }
}
