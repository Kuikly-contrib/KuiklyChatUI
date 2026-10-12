package com.tencent.kuiklybase.chat.kuiklyai

import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmSpeechListener
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmClipboard
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmSpeechRecognizer
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmSpeechResult
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmSpeechSession
import com.tencent.kuiklybase.chat.kuiklyai.provider.EchoLlmProvider
import com.tencent.kuiklybase.chat.kuiklyai.provider.BaseLlmProvider
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmChatMessage
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmMessageState
import com.tencent.kuiklybase.chat.kuiklyai.runtime.LlmChatController
import com.tencent.kuiklybase.chat.kuiklyai.runtime.LlmChatEditState
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceTimeBy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LlmChatControllerTest {

    @Test
    fun sendClearsComposerAndCompletesHistory() = runTest {
        val controller = LlmChatController(
            provider = EchoLlmProvider(chunkSize = 100, delayMillis = 0L),
            scope = this
        )
        controller.onInputChange("hello")

        controller.send()
        runCurrent()

        assertEquals("", controller.inputText)
        assertFalse(controller.isGenerating)
        assertEquals(2, controller.messages.size)
        assertEquals(LlmMessageState.DONE, controller.messages.last().state)
        controller.dispose()
    }

    @Test
    fun unavailableRecognizerStaysHidden() = runTest {
        val recognizer = FakeSpeechRecognizer(available = false)
        val controller = LlmChatController(
            provider = EchoLlmProvider(delayMillis = 0L),
            scope = this,
            speechRecognizer = recognizer
        )

        runCurrent()

        assertFalse(controller.isSpeechAvailable)
        controller.startListening()
        assertEquals(0, recognizer.startCount)
        controller.dispose()
    }

    @Test
    fun finalSpeechResultFinishesWithoutStoppingSessionAgain() = runTest {
        val recognizer = FakeSpeechRecognizer(available = true)
        val controller = LlmChatController(
            provider = EchoLlmProvider(delayMillis = 0L),
            scope = this,
            speechRecognizer = recognizer
        )
        runCurrent()
        assertTrue(controller.isSpeechAvailable)

        controller.onInputChange("prefix ")
        controller.startListening()
        recognizer.listener?.onResult(LlmSpeechResult("draft", isFinal = false))
        assertEquals("prefix draft", controller.inputText)
        assertTrue(controller.isListening)

        recognizer.listener?.onResult(LlmSpeechResult("final", isFinal = true))

        assertEquals("prefix final", controller.inputText)
        assertFalse(controller.isListening)
        assertEquals(0, recognizer.session.stopCount)
        controller.dispose()
    }

    @Test
    fun canceledJobCannotClearNextRequestState() = runTest {
        val provider = object : BaseLlmProvider(idPrefix = "race") {
            override fun streamCompletion(context: List<LlmChatMessage>): Flow<String> = flow {
                awaitCancellation()
            }
        }
        val controller = LlmChatController(provider = provider, scope = this)
        controller.onInputChange("first")
        controller.send()
        runCurrent()
        assertTrue(controller.isGenerating)

        controller.stop()
        controller.onInputChange("second")
        controller.send()
        runCurrent()

        assertTrue(controller.isGenerating)
        controller.stop()
        runCurrent()
        assertFalse(controller.isGenerating)
        controller.dispose()
    }

    @Test
    fun latestUserMessageCanBeEditedAndResubmitted() = runTest {
        val controller = LlmChatController(
            provider = EchoLlmProvider(chunkSize = 100, delayMillis = 0L),
            scope = this
        )
        controller.onInputChange("before")
        controller.send()
        runCurrent()

        val latestUser = controller.messages.last { it.isUser }
        controller.onInputChange("draft outside editor")
        controller.beginEdit(latestUser.id)
        assertEquals("before", controller.inputText)
        assertTrue(controller.editState is LlmChatEditState.Editing)

        controller.onInputChange("after")
        controller.send()
        runCurrent()

        assertEquals(LlmChatEditState.Idle, controller.editState)
        assertEquals("after", controller.messages.first { it.isUser }.text)
        assertEquals(2, controller.messages.size)
        controller.dispose()
    }

    @Test
    fun cancelEditRestoresComposerDraft() = runTest {
        val controller = LlmChatController(
            provider = EchoLlmProvider(delayMillis = 0L),
            scope = this
        )
        controller.onInputChange("outside")
        controller.send()
        runCurrent()
        val latestUser = controller.messages.last { it.isUser }

        controller.onInputChange("unsent draft")
        controller.beginEdit(latestUser.id)
        controller.cancelEdit()

        assertEquals("unsent draft", controller.inputText)
        assertEquals(LlmChatEditState.Idle, controller.editState)
        controller.dispose()
    }

    @Test
    fun copyUsesHostClipboardAndKeepsVisualFeedbackOnly() = runTest {
        val clipboard = RecordingClipboard()
        val controller = LlmChatController(
            provider = EchoLlmProvider(delayMillis = 0L),
            scope = this,
            clipboard = clipboard
        )
        controller.onInputChange("copy me")
        controller.send()
        runCurrent()
        val message = controller.messages.first { it.isUser }

        controller.copyMessage(message)
        runCurrent()

        assertEquals("copy me", clipboard.text)
        assertEquals(null, controller.transientEvent)
        assertEquals(message.id, controller.copiedMessageId)
        advanceTimeBy(2_999)
        runCurrent()
        assertEquals(message.id, controller.copiedMessageId)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(null, controller.copiedMessageId)
        controller.dispose()
    }

    private class FakeSpeechRecognizer(
        private val available: Boolean
    ) : LlmSpeechRecognizer {
        var listener: LlmSpeechListener? = null
        var startCount: Int = 0
        val session = FakeSpeechSession()

        override suspend fun isAvailable(): Boolean = available

        override fun start(listener: LlmSpeechListener): LlmSpeechSession {
            startCount++
            this.listener = listener
            return session
        }
    }

    private class RecordingClipboard : LlmClipboard {
        var text: String = ""

        override suspend fun copyText(label: String, text: String) {
            this.text = text
        }
    }

    private class FakeSpeechSession : LlmSpeechSession {
        var stopCount: Int = 0
        var cancelCount: Int = 0

        override fun stop() {
            stopCount++
        }

        override fun cancel() {
            cancelCount++
        }
    }
}
