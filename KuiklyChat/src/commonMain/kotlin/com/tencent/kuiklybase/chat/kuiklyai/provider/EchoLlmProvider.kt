package com.tencent.kuiklybase.chat.kuiklyai.provider

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 零依赖的本地 provider：不联网，把输入按块回显成流式响应。
 *
 * 存在的意义不只是"玩具"——它保证了三件事：
 * 1. Demo 页在没有任何后端、没有 API key 的情况下可以跑起来
 * 2. UI 层的流式渲染、取消、错误态可以脱离网络做单测
 * 3. 证明 [LlmProvider] 这层抽象确实不依赖网络，而不是"名义上可插拔"
 *
 * @param responder 根据上下文产出完整回复文本，默认回显最后一条用户消息
 * @param chunkSize 每次发射的字符数，模拟 token 粒度
 * @param delayMillis 每块之间的间隔，模拟网络延迟；测试时传 0
 */
class EchoLlmProvider(
    clock: LlmClock = LlmClock.Unavailable,
    private val chunkSize: Int = 6,
    private val delayMillis: Long = 24L,
    private val responder: (List<LlmChatMessage>) -> String = ::defaultEchoResponse
) : BaseLlmProvider(clock, idPrefix = "echo") {

    override val capabilities: LlmProviderCapabilities = LlmProviderCapabilities()

    override fun streamCompletion(context: List<LlmChatMessage>): Flow<String> = flow {
        val fullText = responder(context)
        if (fullText.isEmpty()) return@flow

        var index = 0
        while (index < fullText.length) {
            val end = minOf(index + chunkSize, fullText.length)
            // 先 delay 再 emit：让第一块也有"思考中"的观感，
            // 顺便给 UI 一个可取消点——取消时不会已经吐出半句话。
            if (delayMillis > 0) delay(delayMillis)
            emit(fullText.substring(index, end))
            index = end
        }
    }

    companion object {
        private fun defaultEchoResponse(context: List<LlmChatMessage>): String {
            val lastUser = context.lastOrNull { it.isUser } ?: return ""
            val attachmentNote = if (lastUser.attachments.isEmpty()) {
                ""
            } else {
                val names = lastUser.attachments.joinToString("、") {
                    it.name.ifEmpty { it.kind.name.lowercase() }
                }
                "\n\n另外收到 ${lastUser.attachments.size} 个附件：$names。"
            }
            return "你刚才说的是：\n\n> ${lastUser.text}$attachmentNote"
        }
    }
}
