package com.tencent.kuiklybase.chat.agui

import com.tencent.kuiklybase.chat.model.ChatMessage

/**
 * 事件 → UI 卡片 的显式映射扩展点。
 *
 * ### 解决的问题
 *
 * AG-UI 协议的事件种类丰富，但不同平台常常有**非标准的事件语义**：
 * - 有的平台把"正在搜索文件"放在 `CUSTOM` 事件
 * - 有的平台把"代码块"放在 `ACTIVITY_SNAPSHOT`
 * - 有的平台自定义一套 `TOOL_CALL_*` 子类型
 *
 * 直接修改 [AgUiSession] 内部分支显然不现实，因此提供 [EventToCardMapper] 作为**可插拔的事件拦截层**。
 *
 * ### 职责边界
 *
 * - **Mapper** 负责：事件 → 可能的 ChatMessage（单条）或跳过
 * - **Session** 负责：默认状态机（流式合并、工具缓冲、生命周期等）
 *
 * 当某个事件被 Mapper 消费（返回非 null）后，Session 的默认处理**被跳过**，由 Mapper 产生的 ChatMessage 直接入列表。
 *
 * ### 使用示例
 *
 * ```kotlin
 * // 把 ACTIVITY_SNAPSHOT 中 activityType="FILE_SEARCH" 渲染为自定义搜索卡片
 * session.registerMapper(EventToCardMapper { event, ctx ->
 *     if (event is AgUiEvent.ActivitySnapshot && event.activityType == "FILE_SEARCH") {
 *         RichCardPayload.aiMessage(
 *             id = event.messageId,
 *             kind = "file_search",
 *             content = event.content["query"]?.toString().orEmpty()
 *         )
 *     } else null
 * })
 *
 * // 把 CUSTOM 事件 "code_generation" 渲染为代码块卡片
 * session.registerMapper(EventToCardMapper { event, _ ->
 *     if (event is AgUiEvent.Custom && event.name == "code_generation") {
 *         val map = event.value as? Map<*, *> ?: return@EventToCardMapper null
 *         RichCardPayload.codeBlock(
 *             id = map["id"]?.toString() ?: "code_${event.timestamp}",
 *             code = map["code"]?.toString().orEmpty(),
 *             language = map["language"]?.toString().orEmpty(),
 *             filename = map["filename"]?.toString().orEmpty()
 *         )
 *     } else null
 * })
 * ```
 */
fun interface EventToCardMapper {
    /**
     * 尝试将事件映射为一条 ChatMessage。
     *
     * @param event 原始 AG-UI 事件
     * @param context 当前会话上下文（可查询 state / messages / currentStep 等）
     * @return 非 null 表示此事件已被消费，Session 应跳过默认处理并 upsert 该消息；null 表示跳过本 Mapper。
     */
    fun map(event: AgUiEvent, context: AgUiMappingContext): ChatMessage?
}

/**
 * Mapper 执行时的只读上下文。
 *
 * Mapper 可能需要读取当前 run / step / 消息列表来决定如何映射，通过此上下文暴露。
 */
interface AgUiMappingContext {
    /** 当前 Run ID（未运行时空串） */
    val currentRunId: String

    /** 当前 Step（未在 step 内时空串） */
    val currentStep: String

    /** 会话是否在运行中 */
    val isRunning: Boolean

    /**
     * 只读消息列表快照（命名为 `messagesView` 避免与 Session 的可变 messages 字段冲突）。
     */
    val messagesView: List<ChatMessage>

    /** 只读 Agent state 快照（命名为 `stateView` 避免与 Session 的 `getState()` 方法冲突） */
    val stateView: Map<String, Any?>

    /** 查找指定 ID 的消息 */
    fun findMessage(id: String): ChatMessage? = messagesView.firstOrNull { it.id == id }
}

/**
 * Mapper 责任链：按注册顺序依次调用，第一个返回非 null 的 mapper 消费事件。
 */
class EventToCardMapperChain {
    private val mappers = mutableListOf<EventToCardMapper>()

    fun register(mapper: EventToCardMapper) {
        mappers.add(mapper)
    }

    fun unregister(mapper: EventToCardMapper) {
        mappers.remove(mapper)
    }

    fun clear() {
        mappers.clear()
    }

    /**
     * 依次调用链中每个 mapper，直到某一个返回非 null。
     *
     * @return 命中的 ChatMessage，或 null（所有 mapper 都跳过）
     */
    fun apply(event: AgUiEvent, context: AgUiMappingContext): ChatMessage? {
        for (m in mappers) {
            val result = m.map(event, context)
            if (result != null) return result
        }
        return null
    }

    val size: Int get() = mappers.size
}
