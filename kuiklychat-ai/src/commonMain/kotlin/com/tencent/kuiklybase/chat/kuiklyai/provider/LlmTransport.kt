package com.tencent.kuiklybase.chat.kuiklyai.provider

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * 一次流式 HTTP 请求的描述。
 */
public data class LlmHttpRequest(
    val url: String,
    val method: String = "POST",
    val headers: Map<String, String> = emptyMap(),
    val body: String = "",
    val timeoutMs: Long = 60_000L
)

/**
 * 传输层回调。实现方（宿主）在收到网络数据时逐块回调。
 *
 * [onChunk] 收到的是**原始字节流片段**，不保证按行对齐——
 * 分帧交给 [SseDecoder] 处理。
 */
public interface LlmStreamSink {
    /** A complete text-decoded transport chunk; chunk boundaries are arbitrary. */
    public fun onChunk(data: String)
    public fun onComplete()
    public fun onError(code: LlmErrorCode, message: String)
}

/**
 * 传输契约：KuiklyAI 与网络之间唯一的接触面。
 *
 * commonMain 里没有 HTTP 能力（Kuikly 的 `HttpRequestModule` 需要 Pager 上下文），
 * 更重要的是——**把传输做成注入点，库本身就不会绑死任何网络栈**。
 * 宿主可以用 Kuikly HttpRequestModule、Ktor、OkHttp、URLSession，
 * 甚至在测试里塞一个纯内存的假实现。
 *
 * 这是 KuiklyAI 与 Flutter AI Toolkit 最本质的区别：后者的
 * `FirebaseProvider` 直接依赖 `firebase_ai` SDK，用户必须先有一个 Firebase 项目。
 */
public fun interface LlmTransport {
    /**
     * 发起流式请求。实现方需立即返回一个可取消句柄，
     * 调用 [LlmSubscription.cancel] 时应中断底层连接。
     */
    public fun openStream(request: LlmHttpRequest, sink: LlmStreamSink): LlmSubscription
}

/**
 * 把 [LlmTransport] 的回调式接口桥接成冷 [Flow]。
 *
 * Flow 被取消时自动调用 [LlmSubscription.cancel]，
 * 所以上层只要 `collect` 挂在一个可取消的协程里，"停止生成"就能真正断掉连接。
 *
 * @return 逐块发射网络原始数据的 Flow
 */
public fun LlmTransport.streamAsFlow(request: LlmHttpRequest): Flow<String> = callbackFlow {
    val subscription = openStream(
        request,
        object : LlmStreamSink {
            override fun onChunk(data: String) {
                if (!trySend(data).isSuccess) {
                    close(
                        LlmProviderException(
                            LlmErrorCode.TRANSPORT_OVERFLOW,
                            "LLM transport buffer overflow",
                        ),
                    )
                }
            }

            override fun onComplete() {
                close()
            }

            override fun onError(code: LlmErrorCode, message: String) {
                close(LlmProviderException(code, message))
            }
        }
    )
    awaitClose { subscription.cancel() }
}

/**
 * SSE（Server-Sent Events）分帧解码器。
 *
 * 网络层给到的 chunk 边界是任意的——一个 `data:` 行可能被拆进两个 chunk，
 * 也可能一个 chunk 里塞了五行。直接按 chunk 解析 JSON 必然偶发失败，
 * 且这类 bug 只在弱网 / 长响应时复现，很难排查。
 *
 * 用法：把每个网络 chunk 喂给 [decode]，它只在攒够完整行时才吐出 payload。
 *
 * 注意：这个类**有内部状态**，每次请求都要新建一个实例。
 */
public class SseDecoder(
    /** 结束标记，收到后 [decode] 不再吐出任何内容 */
    private val doneMarker: String = "[DONE]"
) {
    private val buffer = StringBuilder()
    private val eventData = StringBuilder()
    private var firstLine = true
    private var finished = false

    /** 是否已收到结束标记 */
    public val isFinished: Boolean get() = finished

    /**
     * 喂入一个网络 chunk，返回其中已完整的 `data:` 载荷（可能是 0 到多条）。
     */
    public fun decode(chunk: String): List<String> {
        if (finished) return emptyList()
        buffer.append(chunk)

        val payloads = mutableListOf<String>()
        while (true) {
            val line = takeLine() ?: break
            consumeLine(line)?.let { payload ->
                if (payload == doneMarker) {
                    finished = true
                    buffer.clear()
                    return payloads
                }
                payloads += payload
            }
        }
        return payloads
    }

    /**
     * 流结束时调用，冲刷缓冲区里最后一行没有换行符结尾的数据。
     */
    public fun flush(): List<String> {
        if (finished) return emptyList()
        if (buffer.isNotEmpty()) {
            consumeLine(buffer.toString())
        }
        buffer.clear()
        val payload = dispatchEvent() ?: return emptyList()
        if (payload == doneMarker) {
            finished = true
            return emptyList()
        }
        return listOf(payload)
    }

    private fun takeLine(): String? {
        var index = 0
        while (index < buffer.length) {
            if (buffer[index] == '\n' || buffer[index] == '\r') {
                val line = buffer.substring(0, index)
                val skip = if (buffer[index] == '\r' && index + 1 < buffer.length && buffer[index + 1] == '\n') 2 else 1
                buffer.deleteRange(0, index + skip)
                return line
            }
            index++
        }
        return null
    }

    private fun consumeLine(rawLine: String): String? {
        var line = rawLine
        if (firstLine) {
            line = line.removePrefix("\uFEFF")
            firstLine = false
        }
        if (line.isEmpty()) return dispatchEvent()
        if (line.startsWith(":")) return null

        val separator = line.indexOf(':')
        val field = if (separator < 0) line else line.substring(0, separator)
        var value = if (separator < 0) "" else line.substring(separator + 1)
        if (value.startsWith(' ')) value = value.drop(1)
        if (field == "data") {
            if (eventData.isNotEmpty()) eventData.append('\n')
            eventData.append(value)
        }
        return null
    }

    private fun dispatchEvent(): String? {
        if (eventData.isEmpty()) return null
        val payload = eventData.toString()
        eventData.clear()
        return payload
    }
}
