package com.tencent.kuiklybase.chat.session.controller

import com.tencent.kuiklybase.chat.compose.core.ChatController
import com.tencent.kuiklybase.chat.compose.core.ChatConversationDirectory
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.showConversation

/**
 * Binds a [ChatConversationDirectory] to the visible [ChatController].
 * Switching saves the current messages, then [showConversation] drops the in-flight reply.
 */
public class ChatConversationHost(
    public val controller: ChatController,
    public val directory: ChatConversationDirectory,
) {
    public fun restore(seed: List<ChatMessage> = emptyList()) {
        directory.seed(seed)
        val stored = directory.messagesFor(directory.activeConversationId)
        if (stored.isNotEmpty() && stored != controller.state.messages) {
            controller.showConversation(stored)
        }
    }

    public fun open(conversationId: String) {
        if (conversationId == directory.activeConversationId) return
        val next = directory.open(conversationId, leavingMessages())
        controller.showConversation(next)
    }

    public fun startNew() {
        val generation = directory.generation
        val next = directory.startNew(leavingMessages())
        if (directory.generation == generation) return
        controller.showConversation(next)
    }

    public fun rename(conversationId: String, title: String) {
        directory.rename(conversationId, title)
    }

    public fun archive(conversationId: String) {
        val generation = directory.generation
        val next = directory.archive(conversationId, leavingMessages())
        if (directory.generation != generation) controller.showConversation(next)
    }

    public fun restoreArchived(conversationId: String) {
        directory.restoreArchived(conversationId)
    }

    public fun delete(conversationId: String) {
        val generation = directory.generation
        val next = directory.delete(conversationId, leavingMessages())
        if (directory.generation != generation) controller.showConversation(next)
    }

    public fun rememberCurrent() {
        directory.remember(controller.state.messages)
    }

    private fun leavingMessages(): List<ChatMessage> = controller.state.messages
}
