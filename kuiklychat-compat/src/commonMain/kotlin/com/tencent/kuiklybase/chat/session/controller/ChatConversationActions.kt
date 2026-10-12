package com.tencent.kuiklybase.chat.session.controller

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuiklybase.chat.compose.core.ChatIntent
import com.tencent.kuiklybase.chat.compose.core.MessageStatus
import com.tencent.kuiklybase.chat.compose.core.regenerateReply

internal fun ChatControllerSessionView.copyMessage(messageId: String) {
    val text = displayText(messageId)
    if (text.isEmpty()) return
    sessionAttr().onCopyToClipboard?.invoke(text)
}

internal fun ChatControllerSessionView.replyActions(container: ViewContainer<*, *>, messageId: String) {
    val host = this
    container.View {
        attr { flexDirection(FlexDirection.COLUMN) }
        vif({
            val message = host.messageContext(messageId)?.first
            message != null &&
                !host.isMine(messageId) &&
                message.status != MessageStatus.STREAMING &&
                message.status != MessageStatus.SENDING &&
                host.displayText(messageId).isNotEmpty()
        }) {
            View {
                attr {
                    flexDirection(FlexDirection.ROW)
                    marginTop(8f)
                }
                actionLabel(host, "复制") { host.copyMessage(messageId) }
                actionLabel(host, "重新生成") {
                    if (!host.sessionAttr().isSending) {
                        host.sessionAttr().controller.regenerateReply(messageId)
                    }
                }
                actionLabel(host, "更多") {
                    val attr = host.sessionAttr()
                    attr.actionMessageId = if (attr.actionMessageId == messageId) "" else messageId
                }
            }
            vif({ host.sessionAttr().actionMessageId == messageId }) {
                View {
                    attr {
                        flexDirection(FlexDirection.ROW)
                        marginTop(6f)
                    }
                    actionLabel(host, "删除此条") {
                        host.sessionAttr().actionMessageId = ""
                        host.sessionAttr().controller.dispatch(ChatIntent.DeleteMessage(messageId))
                        host.sessionAttr().conversationHost?.rememberCurrent()
                    }
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.actionLabel(
    host: ChatControllerSessionView,
    label: String,
    onClick: () -> Unit,
) {
    View {
        attr {
            marginRight(8f)
            padding(top = 4f, bottom = 4f, left = 8f, right = 8f)
            borderRadius(8f)
            backgroundColor(Color(host.sessionAttr().theme.systemBackgroundColor))
        }
        event { click { onClick() } }
        Text {
            attr {
                text(label)
                fontSize(12f)
                color(Color(host.sessionAttr().theme.timeTextColor))
            }
        }
    }
}
