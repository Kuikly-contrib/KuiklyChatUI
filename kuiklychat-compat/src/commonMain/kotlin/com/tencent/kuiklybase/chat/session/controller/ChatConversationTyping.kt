package com.tencent.kuiklybase.chat.session.controller

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.timer.Timer
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuiklybase.chat.compose.core.MessageStatus

internal fun ChatControllerSessionView.syncTypingDots(waiting: Boolean) {
    if (waiting && typingTimer == null) {
        sessionAttr().typingPhase = 0
        typingTimer = Timer().also { timer ->
            timer.schedule(0, 320) {
                val attr = sessionAttr()
                attr.typingPhase = (attr.typingPhase + 1) % 3
            }
        }
    } else if (!waiting && typingTimer != null) {
        typingTimer?.cancel()
        typingTimer = null
    }
}

internal fun ChatControllerSessionView.cancelTypingDots() {
    typingTimer?.cancel()
    typingTimer = null
}

internal fun ChatControllerSessionView.awaitingFirstToken(messageId: String): Boolean {
    val message = messageContext(messageId)?.first ?: return false
    if (message.status != MessageStatus.STREAMING) return false
    return displayText(messageId).isEmpty()
}

internal fun ChatControllerSessionView.typingDots(container: ViewContainer<*, *>) {
    val host = this
    container.View {
        attr {
            flexDirection(FlexDirection.ROW)
            height(22f)
        }
        for (index in 0 until 3) {
            Text {
                attr {
                    text("●")
                    fontSize(10f)
                    marginRight(5f)
                    color(Color(host.sessionAttr().theme.placeholderColor))
                    opacity(if (host.sessionAttr().typingPhase == index) 1f else 0.28f)
                }
            }
        }
    }
}
