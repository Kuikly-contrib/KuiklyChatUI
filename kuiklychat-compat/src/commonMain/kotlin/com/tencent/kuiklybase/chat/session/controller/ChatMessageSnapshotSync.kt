package com.tencent.kuiklybase.chat.session.controller

import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatState

/** Streaming text for the row that stays mounted. The list item itself is not replaced. */
internal class SessionListSync {
    var liveMessageId: String = ""
    var liveText: String = ""
}

/**
 * Copies [next] into [target] by message id.
 * Inserts and deletes keep the relative order of ids that still exist.
 */
internal fun ChatControllerSessionView.publishMessageSnapshot(state: ChatState) {
    val attr = sessionAttr()
    if (applyMessageSnapshot(attr.messages, state.messages)) {
        attr.snapshotEpoch = attr.snapshotEpoch + 1
    }
}

internal fun applyMessageSnapshot(
    target: MutableList<ChatMessage>,
    next: List<ChatMessage>,
    live: SessionListSync = SessionListSync(),
): Boolean {
    live.liveMessageId = ""
    live.liveText = ""
    if (sameSnapshot(target, next)) return false
    val nextIds = HashSet<String>(next.size)
    for (message in next) {
        nextIds.add(message.id)
    }
    var changed = false
    var index = target.lastIndex
    while (index >= 0) {
        if (target[index].id !in nextIds) {
            target.removeAt(index)
            changed = true
        }
        index--
    }
    var cursor = 0
    for (message in next) {
        if (cursor < target.size && target[cursor].id == message.id) {
            if (target[cursor] != message) {
                target[cursor] = message
                changed = true
            }
            cursor++
        } else {
            target.add(cursor, message)
            changed = true
            cursor++
        }
    }
    while (target.size > next.size) {
        target.removeAt(target.lastIndex)
        changed = true
    }
    return changed
}

private fun sameSnapshot(target: List<ChatMessage>, next: List<ChatMessage>): Boolean {
    if (target.size != next.size) return false
    for (index in target.indices) {
        if (target[index] != next[index]) return false
    }
    return true
}
