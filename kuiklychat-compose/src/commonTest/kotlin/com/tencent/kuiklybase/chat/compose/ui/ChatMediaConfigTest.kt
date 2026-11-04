package com.tencent.kuiklybase.chat.compose.ui

import com.tencent.kuiklybase.chat.compose.core.ChatMediaContext
import com.tencent.kuiklybase.chat.compose.core.ChatMediaLoadEvent
import com.tencent.kuiklybase.chat.compose.core.ChatMediaLoader
import com.tencent.kuiklybase.chat.compose.core.ChatMediaRequest
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatMediaConfigTest {
    @Test
    fun defaultResolverUsesContextSource() {
        val user = ChatUser(id = "u1", avatar = " https://cdn.example/avatar.png ")
        val context = ChatMediaContext.Avatar(user)

        val request = ChatMediaRequestResolver.Default.resolve(context)

        assertEquals("https://cdn.example/avatar.png", request.source)
        assertEquals(request.source, request.cacheKey)
        assertTrue(request.headers.isEmpty())
    }

    @Test
    fun customResolverReceivesAttachmentContextAndPropagatesRequestOptions() {
        val attachment = MessagePart.Attachment(
            id = "image-1",
            uri = "https://cdn.example/full.png",
            previewUri = "https://cdn.example/thumb.png",
        )
        val message = ChatMessage("message-1", ChatUser("assistant"), listOf(attachment))
        val context = ChatMediaContext.AttachmentPreview(
            message = message,
            attachment = attachment,
            source = attachment.previewUri,
        )
        val resolver = ChatMediaRequestResolver { mediaContext ->
            val preview = mediaContext as ChatMediaContext.AttachmentPreview
            ChatMediaRequest(
                source = preview.source,
                cacheKey = "account-a:${preview.attachment.id}",
                headers = mapOf("Authorization" to "Bearer secret"),
            )
        }

        val request = resolver.resolve(context)

        assertEquals(attachment.previewUri, request.source)
        assertEquals("account-a:image-1", request.cacheKey)
        assertEquals("Bearer secret", request.headers["Authorization"])
        assertEquals("https://cdn.example/full.png", context.attachment.uri)
    }

    @Test
    fun mediaRequestToStringDoesNotLeakSourcesKeysOrHeaderValues() {
        val request = ChatMediaRequest(
            source = "https://cdn.example/image.png?token=secret",
            cacheKey = "tenant-secret:image",
            headers = mapOf("Authorization" to "Bearer secret"),
        )

        val rendered = request.toString()

        assertFalse("secret" in rendered)
        assertTrue("Authorization" in rendered)
    }

    @Test
    fun loaderHandleOnlyClosesOwnedLoaderAndIsIdempotent() {
        val owned = RecordingLoader()
        val ownedHandle = createChatMediaLoaderHandle(null) { owned }
        ownedHandle.close()
        ownedHandle.close()
        assertEquals(1, owned.closeCount)

        val injected = RecordingLoader()
        val injectedHandle = createChatMediaLoaderHandle(injected)
        injectedHandle.close()
        assertEquals(0, injected.closeCount)
    }

    @Test
    fun passthroughLoaderDelegatesSourceUntilClosed() = runTest {
        val loader = PassthroughChatMediaLoader()
        val request = ChatMediaRequest("https://cdn.example/image.png")

        assertEquals(
            ChatMediaLoadEvent.Ready(request.source),
            awaitTerminalMediaEvent(loader, request),
        )

        loader.close()
        loader.close()
        assertTrue(awaitTerminalMediaEvent(loader, request) is ChatMediaLoadEvent.Failed)
    }

    @Test
    fun terminalEventReturnsReadyAndCancelsRemainingWork() = runTest {
        var upstreamCancelled = false
        val loader = RecordingLoader(
            events = flow {
                try {
                    emit(ChatMediaLoadEvent.Progress(4L, 8L))
                    emit(ChatMediaLoadEvent.Ready("data:image/png;base64,abc"))
                    awaitCancellation()
                } finally {
                    upstreamCancelled = true
                }
            },
        )
        val observed = mutableListOf<ChatMediaLoadEvent>()

        val terminal = awaitTerminalMediaEvent(
            loader = loader,
            request = ChatMediaRequest("https://cdn.example/image.png"),
            onEvent = observed::add,
        )

        assertEquals(ChatMediaLoadEvent.Ready("data:image/png;base64,abc"), terminal)
        assertEquals(ChatMediaLoadEvent.Progress(4L, 8L), observed.first())
        assertTrue(upstreamCancelled)
    }

    @Test
    fun emptyLoaderFlowBecomesFailure() = runTest {
        val terminal = awaitTerminalMediaEvent(
            loader = RecordingLoader(events = emptyFlow()),
            request = ChatMediaRequest("https://cdn.example/image.png"),
        )

        assertTrue(terminal is ChatMediaLoadEvent.Failed)
    }

    @Test
    fun passthroughLoaderReturnsSourceUntilClosed() = runTest {
        val loader = PassthroughChatMediaLoader()
        val request = ChatMediaRequest("https://cdn.example/image.png")

        assertEquals(
            ChatMediaLoadEvent.Ready(request.source),
            awaitTerminalMediaEvent(loader, request),
        )

        loader.close()
        assertTrue(awaitTerminalMediaEvent(loader, request) is ChatMediaLoadEvent.Failed)
    }

    @Test
    fun progressRejectsNegativeByteCounts() {
        assertFailsWith<IllegalArgumentException> {
            ChatMediaLoadEvent.Progress(receivedBytes = -1L)
        }
        assertFailsWith<IllegalArgumentException> {
            ChatMediaLoadEvent.Progress(receivedBytes = 0L, totalBytes = -1L)
        }
    }

    @Test
    fun progressPercentHandlesUnknownAndClampsValues() {
        assertEquals(null, mediaProgressPercent(receivedBytes = 1L, totalBytes = null))
        assertEquals(null, mediaProgressPercent(receivedBytes = 1L, totalBytes = 0L))
        assertEquals(50, mediaProgressPercent(receivedBytes = 5L, totalBytes = 10L))
        assertEquals(100, mediaProgressPercent(receivedBytes = 15L, totalBytes = 10L))
    }

    private class RecordingLoader(
        private val events: Flow<ChatMediaLoadEvent> =
            flowOf(ChatMediaLoadEvent.Ready("https://cdn.example/image.png")),
    ) : ChatMediaLoader {
        var closeCount: Int = 0
            private set

        override fun load(request: ChatMediaRequest): Flow<ChatMediaLoadEvent> = events

        override fun close() {
            closeCount++
        }
    }
}
