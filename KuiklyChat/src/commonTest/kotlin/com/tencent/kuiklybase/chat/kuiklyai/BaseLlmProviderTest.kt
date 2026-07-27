package com.tencent.kuiklybase.chat.kuiklyai

import com.tencent.kuiklybase.chat.kuiklyai.provider.BaseLlmProvider
import com.tencent.kuiklybase.chat.kuiklyai.provider.EchoLlmProvider
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmChatMessage
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmErrorCode
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmMessageOrigin
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmMessageState
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmProviderException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * [BaseLlmProvider] 的历史簿记与流式累积测试。
 *
 * 这层逻辑所有 provider 共用，出错会影响全部后端，所以单独覆盖。
 */
class BaseLlmProviderTest {

    /** 可控 provider：直接指定要吐出的片段或抛出的异常。 */
    private class ScriptedProvider(
        private val chunks: List<String> = emptyList(),
        private val failWith: LlmProviderException? = null
    ) : BaseLlmProvider(idPrefix = "test") {
        /** 记录每次请求收到的上下文，用于断言快照时机 */
        val seenContexts = mutableListOf<List<LlmChatMessage>>()

        override fun streamCompletion(context: List<LlmChatMessage>): Flow<String> = flow {
            seenContexts.add(context)
            chunks.forEach { emit(it) }
            failWith?.let { throw it }
        }
    }

    // ── 历史簿记 ──────────────────────────────────────────────────────

    @Test
    fun sendMessageAppendsUserThenAssistant() = runTest {
        val provider = ScriptedProvider(listOf("Hel", "lo"))
        provider.sendMessageStream("hi").toList()

        assertEquals(2, provider.history.size)
        assertEquals(LlmMessageOrigin.USER, provider.history[0].origin)
        assertEquals("hi", provider.history[0].text)
        assertEquals(LlmMessageOrigin.LLM, provider.history[1].origin)
        assertEquals("Hello", provider.history[1].text)
        assertEquals(LlmMessageState.DONE, provider.history[1].state)
    }

    @Test
    fun flowEmitsIncrementalDeltasNotAccumulatedText() = runTest {
        val provider = ScriptedProvider(listOf("a", "b", "c"))
        assertEquals(listOf("a", "b", "c"), provider.sendMessageStream("x").toList())
    }

    @Test
    fun contextExcludesEmptyAssistantPlaceholder() = runTest {
        // 回归测试：占位回复若混进上下文，多数端点会报 400 或产出空回复
        val provider = ScriptedProvider(listOf("ok"))
        provider.sendMessageStream("first").toList()

        val context = provider.seenContexts.single()
        assertEquals(1, context.size)
        assertEquals(LlmMessageOrigin.USER, context.single().origin)
        assertTrue(context.none { it.isEmpty })
    }

    @Test
    fun secondTurnCarriesFullPriorContext() = runTest {
        val provider = ScriptedProvider(listOf("r"))
        provider.sendMessageStream("q1").toList()
        provider.sendMessageStream("q2").toList()

        val secondContext = provider.seenContexts[1]
        // user(q1) + assistant(r) + user(q2)
        assertEquals(3, secondContext.size)
        assertEquals("q1", secondContext[0].text)
        assertEquals("r", secondContext[1].text)
        assertEquals("q2", secondContext[2].text)
    }

    @Test
    fun generateStreamDoesNotTouchHistory() = runTest {
        val provider = ScriptedProvider(listOf("one-shot"))
        provider.generateStream("summarize this").toList()
        assertEquals(emptyList(), provider.history)
    }

    // ── 失败处理 ──────────────────────────────────────────────────────

    @Test
    fun failureMarksAssistantMessageAndKeepsPartialText() = runTest {
        val provider = ScriptedProvider(
            chunks = listOf("partial "),
            failWith = LlmProviderException(LlmErrorCode.SERVER, "boom")
        )

        assertFailsWith<LlmProviderException> {
            provider.sendMessageStream("hi").toList()
        }

        val reply = provider.history.last()
        assertEquals(LlmMessageState.FAILED, reply.state)
        // 已经吐出来的内容要留着——全清掉会让用户觉得白等了
        assertEquals("partial ", reply.text)
        assertEquals("boom", reply.error)
        assertEquals(LlmErrorCode.SERVER, reply.errorCode)
        assertTrue(reply.isRetryableFailure)
    }

    @Test
    fun authFailureIsNotRetryable() = runTest {
        val provider = ScriptedProvider(
            failWith = LlmProviderException(LlmErrorCode.AUTH, "bad key")
        )

        assertFailsWith<LlmProviderException> {
            provider.sendMessageStream("hi").toList()
        }

        assertEquals(LlmErrorCode.AUTH, provider.history.last().errorCode)
        assertTrue(!provider.history.last().isRetryableFailure)
    }

    @Test
    fun cancellationMarksMessageCanceledInsteadOfLeavingItStreaming() = runTest {
        // 回归测试：早先版本只捕获 LlmProviderException，取消时这条消息
        // 会永远停在 STREAMING，UI 上是一个不会结束的加载动画。
        val provider = ScriptedProvider(listOf("half"))

        assertFailsWith<CancellationException> {
            provider.sendMessageStream("hi").collect {
                throw CancellationException("user pressed stop")
            }
        }

        val reply = provider.history.last()
        assertEquals(LlmMessageState.CANCELED, reply.state)
        assertEquals("half", reply.text)
    }

    @Test
    fun nonProviderExceptionAlsoReachesTerminalState() = runTest {
        // 宿主实现的 LlmTransport 未必只抛 LlmProviderException
        val provider = object : BaseLlmProvider(idPrefix = "raw") {
            override fun streamCompletion(context: List<LlmChatMessage>): Flow<String> = flow {
                emit("x")
                throw IllegalStateException("transport blew up")
            }
        }

        assertFailsWith<IllegalStateException> {
            provider.sendMessageStream("hi").toList()
        }

        val reply = provider.history.last()
        assertEquals(LlmMessageState.FAILED, reply.state)
        assertEquals("transport blew up", reply.error)
    }

    // ── setHistory / 监听 ─────────────────────────────────────────────

    @Test
    fun setHistoryReplacesEverything() {
        val provider = ScriptedProvider()
        val restored = listOf(
            LlmChatMessage(id = "r1", origin = LlmMessageOrigin.USER, text = "old"),
            LlmChatMessage(id = "r2", origin = LlmMessageOrigin.LLM, text = "answer")
        )
        provider.setHistory(restored)
        assertEquals(restored, provider.history)

        provider.setHistory(emptyList())
        assertEquals(emptyList(), provider.history)
    }

    @Test
    fun historyListenerFiresOnEveryMutation() = runTest {
        val provider = ScriptedProvider(listOf("a", "b"))
        var notifications = 0
        var lastSeen: List<LlmChatMessage> = emptyList()
        provider.addHistoryListener { history ->
            notifications++
            lastSeen = history
        }

        provider.sendMessageStream("hi").toList()

        // 用户消息 + 占位回复 + 每个 delta 一次更新 + 终态一次
        assertTrue(notifications >= 4, "实际收到 $notifications 次通知")
        assertEquals("ab", lastSeen.last().text)
    }

    @Test
    fun cancelledSubscriptionStopsReceivingUpdates() {
        val provider = ScriptedProvider()
        var notifications = 0
        val subscription = provider.addHistoryListener { notifications++ }

        provider.setHistory(listOf(LlmChatMessage(id = "a", origin = LlmMessageOrigin.USER)))
        assertEquals(1, notifications)

        subscription.cancel()
        provider.setHistory(emptyList())
        assertEquals(1, notifications, "取消订阅后不该再收到通知")
    }

    @Test
    fun historyIsDefensiveCopy() {
        val provider = ScriptedProvider()
        provider.setHistory(listOf(LlmChatMessage(id = "a", origin = LlmMessageOrigin.USER)))
        val snapshot = provider.history

        provider.setHistory(emptyList())
        // 拿到的快照不该跟着内部状态变
        assertEquals(1, snapshot.size)
    }

    @Test
    fun restoredIdsAreNotReused() = runTest {
        val provider = ScriptedProvider(listOf("new answer"))
        provider.setHistory(
            listOf(
                LlmChatMessage(id = "test_1", origin = LlmMessageOrigin.USER, text = "old"),
                LlmChatMessage(id = "test_2", origin = LlmMessageOrigin.LLM, text = "old answer")
            )
        )

        provider.sendMessageStream("new").toList()

        assertEquals(4, provider.history.size)
        assertEquals(4, provider.history.map { it.id }.distinct().size)
        assertNotEquals("test_2", provider.history.last().id)
        assertEquals("old answer", provider.history[1].text)
    }

    // ── EchoLlmProvider ───────────────────────────────────────────────

    @Test
    fun echoProviderStreamsBackUserTextWithoutNetwork() = runTest {
        val provider = EchoLlmProvider(chunkSize = 4, delayMillis = 0L)
        val deltas = provider.sendMessageStream("你好世界").toList()

        assertTrue(deltas.isNotEmpty())
        val full = deltas.joinToString("")
        assertTrue(full.contains("你好世界"), "回声内容应包含原文，实际：$full")
        assertEquals(LlmMessageState.DONE, provider.history.last().state)
    }

    @Test
    fun echoProviderRespectsCustomResponder() = runTest {
        val provider = EchoLlmProvider(
            chunkSize = 100,
            delayMillis = 0L,
            responder = { "固定回复" }
        )
        assertEquals("固定回复", provider.sendMessageStream("任意输入").toList().joinToString(""))
    }
}
