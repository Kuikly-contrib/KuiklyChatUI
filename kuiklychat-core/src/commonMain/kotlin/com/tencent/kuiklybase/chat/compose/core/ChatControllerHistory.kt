package com.tencent.kuiklybase.chat.compose.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch

internal fun ChatController.refreshInternal() {
    if (closed) return
    if (historyLoader == null || state.isInitialLoading) return
    cancelHistoryOperation()
    launchHistory { loadInitial(historyEpoch, force = true) }
}

internal fun ChatController.loadEarlierInternal() {
    if (closed) return
    val loader = historyLoader ?: return
    if (state.isLoadingEarlier || !state.hasMoreEarlier) return

    historyJob?.cancel()
    val epoch = ++historyEpoch
    val beforeId = state.messages.firstOrNull()?.id
    _state.value = state.copy(isLoadingEarlier = true, error = null)
    val job = controllerScope.launch(start = CoroutineStart.LAZY) {
        try {
            val page = loader.loadEarlier(beforeId)
            if (isHistoryEpochActive(epoch)) {
                val existingIds = state.messages.mapTo(HashSet()) { it.id }
                val newMessages = page.messages.filter { existingIds.add(it.id) }
                _state.value = state.copy(
                    messages = newMessages + state.messages,
                    hasMoreEarlier = page.hasMoreEarlier,
                    isLoadingEarlier = false,
                )
            }
        } catch (cancelled: CancellationException) {
            if (isHistoryEpochActive(epoch)) {
                _state.value = state.copy(isLoadingEarlier = false)
            }
            throw cancelled
        } catch (error: Throwable) {
            if (isHistoryEpochActive(epoch)) {
                failHistory(error.message ?: "Failed to load earlier messages")
            }
        } finally {
            if (historyJob === currentCoroutineContext()[Job]) historyJob = null
        }
    }
    historyJob = job
    job.start()
}

internal fun ChatController.launchHistory(block: suspend () -> Unit) {
    val job = controllerScope.launch(start = CoroutineStart.LAZY) { block() }
    historyJob = job
    job.start()
}

internal fun ChatController.cancelHistoryOperation() {
    historyEpoch += 1
    historyJob?.cancel()
    historyJob = null
    _state.value = state.copy(isInitialLoading = false, isLoadingEarlier = false)
}

internal suspend fun ChatController.loadInitial(epoch: Long, force: Boolean = false) {
    val loader = historyLoader ?: return
    if (!isHistoryEpochActive(epoch) || state.isInitialLoading) return
    if (!force && state.messages.isNotEmpty()) return

    _state.value = state.copy(isInitialLoading = true, error = null)
    try {
        val page = loader.loadEarlier(beforeMessageId = null)
        if (isHistoryEpochActive(epoch)) {
            val merged = mergeInitialMessages(page.messages)
            _state.value = state.copy(
                messages = merged,
                hasMoreEarlier = page.hasMoreEarlier,
                isInitialLoading = false,
                lastMessageId = merged.lastOrNull()?.id,
            )
            emitHistoryEffect(ChatEffect.ScrollToBottom)
        }
    } catch (cancelled: CancellationException) {
        if (isHistoryEpochActive(epoch)) {
            _state.value = state.copy(isInitialLoading = false)
        }
        throw cancelled
    } catch (error: Throwable) {
        if (isHistoryEpochActive(epoch)) {
            failHistory(error.message ?: "Failed to load messages")
        }
    } finally {
        if (historyJob === currentCoroutineContext()[Job]) historyJob = null
    }
}

private fun ChatController.failHistory(message: String) {
    val error = ChatError(message, "history")
    _state.value = state.copy(
        isInitialLoading = false,
        isLoadingEarlier = false,
        error = error,
    )
    emitHistoryEffect(ChatEffect.ShowError(message))
}

private fun ChatController.isHistoryEpochActive(epoch: Long): Boolean =
    !closed && epoch == historyEpoch

private fun ChatController.mergeInitialMessages(incoming: List<ChatMessage>): List<ChatMessage> {
    val incomingIds = incoming.mapTo(HashSet()) { it.id }
    val localPending = state.messages.filter {
        it.id !in incomingIds &&
            (it.status == MessageStatus.SENDING || it.status == MessageStatus.STREAMING)
    }
    return incoming + localPending
}

private fun ChatController.emitHistoryEffect(effect: ChatEffect) {
    emit(effect)
}
