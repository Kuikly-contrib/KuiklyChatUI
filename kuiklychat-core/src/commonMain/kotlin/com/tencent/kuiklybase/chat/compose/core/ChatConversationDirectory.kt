package com.tencent.kuiklybase.chat.compose.core

/** One saved conversation in the directory. Messages live in [ChatConversationStore]. */
public data class ChatConversationSummary(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val archived: Boolean = false,
)

/** Host-owned persistence. The directory does not know about files or databases. */
public interface ChatConversationStore {
    public fun loadConversations(): List<ChatConversationSummary>
    public fun loadArchivedConversations(): List<ChatConversationSummary>
    public fun loadMessages(conversationId: String): List<ChatMessage>
    public fun save(summary: ChatConversationSummary, messages: List<ChatMessage>)
    public fun rename(conversationId: String, title: String)
    public fun setArchived(conversationId: String, archived: Boolean)
    public fun delete(conversationId: String)
}

/** Process-local store for tests and hosts that persist elsewhere. */
public class MemoryChatConversationStore : ChatConversationStore {
    private val summaries = linkedMapOf<String, ChatConversationSummary>()
    private val messages = linkedMapOf<String, List<ChatMessage>>()

    override fun loadConversations(): List<ChatConversationSummary> {
        return summaries.values.filterNot { it.archived }.sortedByDescending { it.updatedAt }
    }

    override fun loadArchivedConversations(): List<ChatConversationSummary> {
        return summaries.values.filter { it.archived }.sortedByDescending { it.updatedAt }
    }

    override fun loadMessages(conversationId: String): List<ChatMessage> {
        return messages[conversationId].orEmpty()
    }

    override fun save(summary: ChatConversationSummary, messages: List<ChatMessage>) {
        summaries[summary.id] = summary.copy(archived = summaries[summary.id]?.archived == true)
        this.messages[summary.id] = messages.toList()
    }

    override fun rename(conversationId: String, title: String) {
        summaries[conversationId]?.let { summaries[conversationId] = it.copy(title = title) }
    }

    override fun setArchived(conversationId: String, archived: Boolean) {
        summaries[conversationId]?.let { summaries[conversationId] = it.copy(archived = archived) }
    }

    override fun delete(conversationId: String) {
        summaries.remove(conversationId)
        messages.remove(conversationId)
    }
}

/**
 * Owns conversation identity. Switching saves the visible messages, bumps [generation],
 * and returns the messages the UI should show. Call [ChatController.showConversation]
 * with that list so the in-flight reply is dropped.
 */
public class ChatConversationDirectory(
    private val store: ChatConversationStore,
    private val clock: ChatClock = ChatClock.System,
) {
    public var activeConversationId: String = ""
        private set

    public var conversations: List<ChatConversationSummary> = emptyList()
        private set

    public var archivedConversations: List<ChatConversationSummary> = emptyList()
        private set

    public var generation: Int = 0
        private set

    public val activeTitle: String
        get() = conversations.firstOrNull { it.id == activeConversationId }?.title ?: "新对话"

    public fun reload() {
        conversations = store.loadConversations()
        archivedConversations = store.loadArchivedConversations()
        val known = (conversations + archivedConversations).mapNotNull { it.id.substringAfterLast('-').toIntOrNull() }
        known.maxOrNull()?.let { sequence = maxOf(sequence, it) }
        if (activeConversationId.isEmpty()) {
            activeConversationId = conversations.firstOrNull()?.id ?: newId()
        }
    }

    /** Saves [seed] as the first conversation when the store is empty. */
    public fun seed(seed: List<ChatMessage>) {
        reload()
        if (conversations.isNotEmpty() || archivedConversations.isNotEmpty()) {
            activeConversationId = conversations.firstOrNull()?.id ?: activeConversationId
            return
        }
        if (activeConversationId.isEmpty()) activeConversationId = newId()
        if (seed.isNotEmpty()) {
            saveActive(seed)
            reload()
        }
    }

    public fun messagesFor(conversationId: String): List<ChatMessage> = store.loadMessages(conversationId)

    public fun open(conversationId: String, current: List<ChatMessage>): List<ChatMessage> {
        if (conversationId == activeConversationId) return current
        commit(current)
        activeConversationId = conversationId
        generation += 1
        reload()
        return store.loadMessages(conversationId)
    }

    public fun startNew(current: List<ChatMessage>): List<ChatMessage> {
        val unsaved = conversations.none { it.id == activeConversationId }
        if (current.isEmpty() && unsaved) return emptyList()
        commit(current)
        activeConversationId = newId()
        generation += 1
        reload()
        return emptyList()
    }

    public fun rename(conversationId: String, title: String) {
        val trimmed = title.trim().take(TITLE_LENGTH)
        if (trimmed.isEmpty()) return
        store.rename(conversationId, trimmed)
        reload()
    }

    public fun archive(conversationId: String, current: List<ChatMessage>): List<ChatMessage> {
        commit(current)
        store.setArchived(conversationId, true)
        reload()
        if (conversationId != activeConversationId) return current
        generation += 1
        activeConversationId = conversations.firstOrNull()?.id ?: newId()
        return messagesIfStored(activeConversationId)
    }

    public fun restoreArchived(conversationId: String) {
        store.setArchived(conversationId, false)
        reload()
    }

    public fun delete(conversationId: String, current: List<ChatMessage>): List<ChatMessage> {
        val removingActive = conversationId == activeConversationId
        if (!removingActive) commit(current)
        store.delete(conversationId)
        reload()
        if (!removingActive) return current
        generation += 1
        activeConversationId = conversations.firstOrNull()?.id ?: newId()
        return messagesIfStored(activeConversationId)
    }

    public fun remember(current: List<ChatMessage>) {
        commit(current)
        reload()
    }

    private fun messagesIfStored(conversationId: String): List<ChatMessage> {
        if (conversations.none { it.id == conversationId }) return emptyList()
        return store.loadMessages(conversationId)
    }

    private fun commit(current: List<ChatMessage>) {
        if (activeConversationId.isEmpty() || current.isEmpty()) return
        saveActive(freezeConversationMessages(current))
    }

    private fun saveActive(messages: List<ChatMessage>) {
        val previous = store.loadConversations().firstOrNull { it.id == activeConversationId }
            ?: store.loadArchivedConversations().firstOrNull { it.id == activeConversationId }
        store.save(
            ChatConversationSummary(
                id = activeConversationId,
                title = previous?.title?.takeIf { it.isNotBlank() && it != "新对话" }
                    ?: chatConversationTitle(messages),
                updatedAt = clock.nowMillis(),
                archived = previous?.archived == true,
            ),
            messages,
        )
    }

    private var sequence = 0

    private fun newId(): String {
        sequence += 1
        return "conversation-$sequence"
    }

    private companion object {
        const val TITLE_LENGTH = 16
    }
}

internal fun chatConversationTitle(messages: List<ChatMessage>): String {
    val source = messages.firstOrNull { it.outgoing && it.text.isNotBlank() }?.text
        ?: messages.firstOrNull { it.text.isNotBlank() }?.text
    val line = source?.lineSequence()?.firstOrNull()?.trim().orEmpty()
    return line.take(16).ifBlank { "新对话" }
}

internal fun freezeConversationMessages(messages: List<ChatMessage>): List<ChatMessage> {
    return messages.map { message ->
        if (message.status == MessageStatus.STREAMING || message.status == MessageStatus.SENDING) {
            message.copy(status = MessageStatus.CANCELLED)
        } else {
            message
        }
    }
}
