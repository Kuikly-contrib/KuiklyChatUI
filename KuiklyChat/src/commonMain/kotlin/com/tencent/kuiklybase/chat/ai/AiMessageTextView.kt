package com.tencent.kuiklybase.chat.ai

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.reactive.ReactiveObserver
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.*
import com.tencent.kuiklybase.KuiklyStreamingMarkdown
import com.tencent.kuiklybase.config.MarkdownConfig
import com.tencent.kuiklybase.config.MarkdownColors
import com.tencent.kuiklybase.config.MarkdownTypography
import com.tencent.kuiklybase.config.TextStyleConfig
import com.tencent.kuiklybase.streaming.MarkdownBlock
import com.tencent.kuiklybase.streaming.MarkdownStreamingState

// ============================
// AI 打字状态枚举
// ============================

/**
 * AI 消息的打字状态
 *
 * 参考 Stream Chat 的 TypingState 设计，用于控制 AI 消息的逐字显示动画。
 */
sealed class AiTypingState {
    /**
     * 无动画状态 — 消息已完成生成，直接显示全部内容
     */
    object Nothing : AiTypingState()

    /**
     * 正在生成状态 — AI 正在流式输出，需要逐字显示动画
     *
     * @param messageId 正在生成的消息 ID，用于匹配当前消息是否需要动画
     */
    data class Generating(val messageId: String) : AiTypingState()
}

// ============================
// AiMessageText 组件
// ============================

/**
 * AiMessageTextView — AI 消息文本组件
 */
class AiMessageTextView : ComposeView<AiMessageTextAttr, AiMessageTextEvent>() {
    override fun createAttr(): AiMessageTextAttr = AiMessageTextAttr()
    override fun createEvent(): AiMessageTextEvent = AiMessageTextEvent()

    // ---- 内部状态 ----

    /** 当前显示的文本（逐字递增） */
    private var displayedText = ""

    /** 当前逐字动画的字符索引 */
    private var currentIndex = 0

    /** 是否正在执行逐字动画 */
    private var isAnimating = false

    /** 上一次处理的 content 值，用于检测内容变化 */
    private var lastContent = ""

    /** 上一次处理的 typingState，用于检测状态变化 */
    private var lastTypingState: AiTypingState = AiTypingState.Nothing

    /** 流式渲染状态管理器 */
    private val streamingState = MarkdownStreamingState()

    /**
     * 流式渲染的块列表（observableList）
     *
     * 使用 vfor 绑定到 UI，配合 diffUpdate 实现块级增量更新：
     * - 已完成的块：blockContent 不变 → id 不变 → diffUpdate 跳过，视图保持
     * - 正在输入的块：blockContent 持续变化 → id 变化 → 触发视图重建
     */
    private var blockList by observableList<MarkdownBlock>()

    /** 缓存的 MarkdownConfig 实例 */
    private var cachedConfig: MarkdownConfig? = null

    override fun body(): ViewBuilder {
        val ctx = this

        return {
            View {
                attr {
                    padding(
                        ctx.attr.paddingVertical,
                        ctx.attr.paddingHorizontal,
                        ctx.attr.paddingVertical,
                        ctx.attr.paddingHorizontal
                    )
                }

                // 使用 vfor + KuiklyStreamingMarkdown 实现流式增量渲染
                // 注意：vfor 闭包内必须有且仅有一个根子节点，需用 View 包裹
                vfor({ ctx.blockList }) { block ->
                    View {
                        KuiklyStreamingMarkdown(
                            state = ctx.streamingState,
                            block = block,
                            config = ctx.getOrCreateConfig(),
                        )
                    }
                }
            }
        }
    }

    override fun viewDidLoad() {
        super.viewDidLoad()
        // 使用 ReactiveObserver 监听 attr.content 和 attr.typingState 的变化
        // ComposeView 的 body() 只执行一次，attr 变化不会重新触发 body()，
        // 所以需要通过 bindValueChange 来监听属性变化并调用 checkAndUpdateDisplay()
        ReactiveObserver.bindValueChange(this) {
            // 读取 observable 属性以建立依赖关系
            val content = attr.content
            val state = attr.typingState
            val msgId = attr.messageId
            ReactiveObserver.addLazyTaskUtilEndCollectDependency {
                checkAndUpdateDisplay()
            }
        }
    }

    override fun viewDidLayout() {
        super.viewDidLayout()
    }

    override fun viewWillUnload() {
        super.viewWillUnload()
        // 解除 ReactiveObserver 绑定
        ReactiveObserver.unbindValueChange(this)
        stopAnimation()
    }

    /**
     * 检测属性变化并更新显示
     *
     * 由于 ComposeView 没有 viewDidUpdate 回调，
     * 在 body() 和 viewDidLayout() 中调用此方法来检测 content/typingState 的变化。
     */
    private fun checkAndUpdateDisplay() {
        val state = attr.typingState
        val fullText = attr.content

        val contentChanged = fullText != lastContent
        val stateChanged = state != lastTypingState

        if (!contentChanged && !stateChanged && isAnimating) {
            return
        }

        lastContent = fullText
        lastTypingState = state

        when {
            // 无动画状态：直接显示全部内容
            state is AiTypingState.Nothing -> {
                stopAnimation()
                displayedText = fullText
                currentIndex = fullText.length
                flushStreamingBlocks(fullText, force = true)
            }
            // 正在生成且匹配当前消息：启动/继续逐字动画
            state is AiTypingState.Generating && attr.messageId == state.messageId -> {
                if (!isAnimating) {
                    // 如果是从 Nothing 切换到 Generating，需要重置动画状态从头开始
                    if (stateChanged) {
                        currentIndex = 0
                        displayedText = ""
                        streamingState.reset()
                        blockList.clear()
                    }
                    startTypingAnimation()
                }
            }
            // 正在生成但不匹配当前消息：直接显示全部
            else -> {
                stopAnimation()
                displayedText = fullText
                currentIndex = fullText.length
                flushStreamingBlocks(fullText, force = true)
            }
        }
    }

    /**
     * 启动逐字打字动画
     */
    private fun startTypingAnimation() {
        isAnimating = true
        event.onAnimationStateChange?.invoke(true)
        tickAnimation()
    }

    /**
     * 动画 tick：每次显示下一个/多个字符
     */
    private fun tickAnimation() {
        if (!isAnimating) return

        val fullText = attr.content

        if (currentIndex >= fullText.length) {
            if (attr.typingState is AiTypingState.Nothing) {
                // 生成完毕，最终 flush 并停止动画
                flushStreamingBlocks(displayedText, force = true)
                stopAnimation()
            } else {
                // 还在生成中，等待新内容到达后继续
                setTimeout(pagerId, TYPING_CHECK_INTERVAL) {
                    tickAnimation()
                }
            }
            return
        }

        // 每次前进 1~N 个字符（根据速度配置）
        val step = attr.typingSpeed.coerceAtLeast(1)
        currentIndex = minOf(currentIndex + step, fullText.length)
        displayedText = fullText.substring(0, currentIndex)

        // 更新流式渲染块（增量 diff）
        flushStreamingBlocks(displayedText)

        // 调度下一帧
        setTimeout(pagerId, attr.typingInterval) {
            tickAnimation()
        }
    }

    /**
     * 停止逐字动画
     */
    private fun stopAnimation() {
        if (isAnimating) {
            isAnimating = false
            event.onAnimationStateChange?.invoke(false)
        }
    }

    /**
     * 将当前文本解析为块级列表，并通过 diffUpdate 增量更新 UI
     *
     * @param text 当前要渲染的 Markdown 文本
     * @param force 是否强制解析（流式结束时使用）
     */
    private fun flushStreamingBlocks(text: String, force: Boolean = false) {
        val newBlocks = streamingState.update(text, force) ?: return
        // 使用 diffUpdate 实现增量更新：
        // - 已完成的块 id 不变 → 视图保持
        // - 正在输入的块 id 变化 → 触发视图重建
        blockList.diffUpdate(newBlocks) { old, new -> old.id == new.id }
    }

    /**
     * 获取或创建 MarkdownConfig（缓存以避免重复创建）
     */
    private fun getOrCreateConfig(): MarkdownConfig {
        attr.markdownConfig?.let { return it }
        cachedConfig?.let { return it }

        val config = MarkdownConfig(
            colors = MarkdownColors(
                text = attr.textColor,
                codeBackground = attr.codeBackgroundColor,
                inlineCodeBackground = attr.inlineCodeBackgroundColor,
                linkColor = attr.linkColor,
            ),
            typography = MarkdownTypography(
                text = TextStyleConfig(fontSize = attr.fontSize),
                paragraph = TextStyleConfig(fontSize = attr.fontSize),
            ),
            onLinkClick = { url ->
                event.onLinkClick?.invoke(url)
            },
        )
        cachedConfig = config
        return config
    }

    companion object {
        /** 等待新内容的检查间隔（毫秒） */
        private const val TYPING_CHECK_INTERVAL = 50
    }
}

// ============================
// Attr 属性类
// ============================

class AiMessageTextAttr : ComposeAttr() {
    /** 消息唯一 ID（用于匹配 AiTypingState.Generating 的 messageId） */
    var messageId: String by observable("")

    /** 消息完整内容（Markdown 格式文本） */
    var content: String by observable("")

    /** AI 打字状态 */
    var typingState: AiTypingState by observable<AiTypingState>(AiTypingState.Nothing)

    /** 文字颜色 */
    var textColor: Long by observable(0xFF333333)

    /** 文字字号 */
    var fontSize: Float by observable(15f)

    /** 链接颜色 */
    var linkColor: Long by observable(0xFF1A73E8)

    /** 代码块背景色 */
    var codeBackgroundColor: Long by observable(0xFFF5F5F5)

    /** 行内代码背景色 */
    var inlineCodeBackgroundColor: Long by observable(0xFFE8E8E8)

    /** 逐字动画间隔（毫秒），值越小打字越快 */
    var typingInterval: Int by observable(DEFAULT_TYPING_INTERVAL)

    /** 每次动画 tick 前进的字符数 */
    var typingSpeed: Int by observable(1)

    /** 容器上下内边距 */
    var paddingVertical: Float by observable(8f)

    /** 容器左右内边距 */
    var paddingHorizontal: Float by observable(12f)

    /** 自定义 MarkdownConfig（设置后将忽略上面的颜色/字号属性） */
    var markdownConfig: MarkdownConfig? by observable(null)

    companion object {
        private const val DEFAULT_TYPING_INTERVAL = 10
    }
}

// ============================
// Event 事件类
// ============================

class AiMessageTextEvent : ComposeEvent() {
    /** 动画状态变化回调（true = 动画开始，false = 动画结束） */
    var onAnimationStateChange: ((Boolean) -> Unit)? = null

    /** 链接点击回调 */
    var onLinkClick: ((String) -> Unit)? = null

    /** 长按回调 */
    var onLongPress: (() -> Unit)? = null
}

// ============================
// 扩展函数（注册到 DSL）
// ============================

/**
 * AI 消息文本组件
 *
 * 在聊天气泡中显示 AI 生成的 Markdown 内容，支持逐字打字动画和流式增量渲染。
 *
 * @param init 组件配置 Lambda
 *
 * 示例：
 * ```kotlin
 * AiMessageText {
 *     attr {
 *         messageId = message.id
 *         content = message.content
 *         typingState = AiTypingState.Generating(message.id)
 *         textColor = 0xFF333333
 *         fontSize = 15f
 *     }
 *     event {
 *         onAnimationStateChange = { isAnimating ->
 *             // 动画状态变化时可控制滚动等行为
 *         }
 *         onLinkClick = { url ->
 *             // 处理链接点击
 *         }
 *     }
 * }
 * ```
 */
fun ViewContainer<*, *>.AiMessageText(init: AiMessageTextView.() -> Unit) {
    addChild(AiMessageTextView(), init)
}
