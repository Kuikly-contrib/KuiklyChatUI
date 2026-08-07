package com.kuikly.kuiklychat.dsl.agui

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.views.*
import com.tencent.kuiklybase.chat.bubble.MessageRendererFactory
import com.tencent.kuiklybase.chat.model.ChatMessage
import com.tencent.kuiklybase.chat.model.MessageContext
import com.tencent.kuiklybase.chat.model.MessageType
import com.tencent.kuiklybase.chat.session.ChatSessionConfig

/**
 * AG-UI 协议专属消息渲染器集合
 *
 * 为 [com.tencent.kuiklybase.chat.agui.AgUiSession] 在消息的 `extra["agUiKind"]` 字段上打的标记，
 * 提供差异化的视觉呈现：
 *
 * | agUiKind        | 渲染器                      | 视觉特征                              |
 * |-----------------|----------------------------|-------------------------------------|
 * | `"tool_result"` | [ToolResultRenderer]       | 紫色边框 + 🔧 图标 + 工具名徽章          |
 * | `"activity"`    | [ActivityRenderer]         | 蓝色渐变卡片 + 步骤清单 + 完成状态 ✅      |
 * | `"reasoning"`   | [ReasoningRenderer]        | 灰色虚线边框 + 🧠 标签 + 斜体            |
 *
 * 这些渲染器都是**独立的业务示例代码**，位于 shared 模块，不会污染 KuiklyChat 核心库。
 * 业务方可以自由修改或替换，也可以按照同样的模式为自己的事件类型添加渲染器。
 *
 * 注册方式：
 * ```
 * ChatSession({ session.observableMessages }) {
 *     messageRenderers.add(ToolResultRenderer())
 *     messageRenderers.add(ActivityRenderer())
 *     messageRenderers.add(ReasoningRenderer())
 * }
 * ```
 */

// ============================
// 1. 工具调用结果渲染器
// ============================

/**
 * 工具调用结果消息渲染器。
 *
 * 将 [ChatMessage.extra] 中 `agUiKind == "tool_result"` 的消息渲染为：
 * ```
 * ╭─────────────────────────────╮
 * │ 🔧  工具调用                  │
 * │ ┌──────────────────────────┐ │
 * │ │ [get_weather]            │ │
 * │ │ Beijing 22°C, sunny      │ │
 * │ └──────────────────────────┘ │
 * ╰─────────────────────────────╯
 * ```
 */
class ToolResultRenderer : MessageRendererFactory {
    override fun canRender(message: ChatMessage): Boolean {
        return message.extra["agUiKind"] == "tool_result"
    }

    override fun render(
        container: ViewContainer<*, *>,
        context: MessageContext,
        config: ChatSessionConfig
    ) {
        val msg = context.message
        val toolName = msg.extra["toolCallName"].orEmpty().ifEmpty { "tool" }
        // 内容去除前缀（若用户已带）
        val rawContent = msg.content
            .removePrefix("🔧 调用 $toolName → ")
            .removePrefix("🔧 $toolName: ")
            .ifEmpty { msg.content }

        container.apply {
            View {
                attr {
                    flexDirectionRow()
                    paddingLeft(12f)
                    paddingRight(12f)
                    paddingTop(4f)
                    paddingBottom(4f)
                }
                // 左侧留白对齐头像
                View { attr { width(48f) } }

                // 右侧卡片主体
                View {
                    attr {
                        flex(1f)
                        borderRadius(10f)
                        backgroundColor(Color(0xFFFAF5FF))
                        border(Border(1f, BorderStyle.SOLID, Color(0xFF9F7AEA)))
                        paddingTop(10f)
                        paddingBottom(10f)
                        paddingLeft(12f)
                        paddingRight(12f)
                    }

                    // 头部：图标 + 标题
                    View {
                        attr {
                            flexDirectionRow()
                            alignItemsCenter()
                            marginBottom(6f)
                        }
                        Text {
                            attr {
                                text("🔧")
                                fontSize(14f)
                                marginRight(6f)
                            }
                        }
                        Text {
                            attr {
                                text("工具调用")
                                fontSize(12f)
                                color(Color(0xFF6B46C1))
                                fontWeightSemiBold()
                                flex(1f)
                            }
                        }
                        // 工具名徽章
                        View {
                            attr {
                                borderRadius(4f)
                                backgroundColor(Color(0xFF9F7AEA))
                                paddingLeft(6f)
                                paddingRight(6f)
                                paddingTop(2f)
                                paddingBottom(2f)
                            }
                            Text {
                                attr {
                                    text(toolName)
                                    fontSize(10f)
                                    color(Color.WHITE)
                                }
                            }
                        }
                    }

                    // 分隔线
                    View {
                        attr {
                            height(1f)
                            backgroundColor(Color(0x33000000))
                            marginBottom(8f)
                        }
                    }

                    // 结果内容
                    Text {
                        attr {
                            text(rawContent)
                            fontSize(13f)
                            color(Color(0xFF2D3436))
                            lineHeight(18f)
                        }
                    }
                }
            }
        }
    }
}

// ============================
// 2. Activity 事件渲染器
// ============================

/**
 * Activity 活动消息渲染器。
 *
 * 将 `agUiKind == "activity"` 的消息渲染为带步骤清单的渐变卡片：
 * ```
 * ╭─ 📋 PLAN ──────────────────╮
 * │ 完成用户需求的三步计划       │
 * │                             │
 * │ ✓ 分析需求                  │
 * │ ○ 编写代码                  │
 * │ ○ 测试验收                  │
 * ╰─────────────────────────────╯
 * ```
 */
class ActivityRenderer : MessageRendererFactory {
    override fun canRender(message: ChatMessage): Boolean {
        return message.type == MessageType.CUSTOM && message.extra["agUiKind"] == "activity"
    }

    override fun render(
        container: ViewContainer<*, *>,
        context: MessageContext,
        config: ChatSessionConfig
    ) {
        val msg = context.message
        val activityType = msg.extra["activityType"].orEmpty().ifEmpty { "ACTIVITY" }
        val title = msg.content
            .removePrefix("[$activityType] ")
            .ifEmpty { activityType }
        // 从 extra 提取步骤完成度（Demo 场景下简化处理）
        val stepCount = msg.extra["stepCount"]?.toIntOrNull() ?: 0
        // 从 extra.activity.patch.N 字段计算完成步骤数（基于 AgUiSession 默认策略）
        val completedSteps = msg.extra.keys.count { it.startsWith("activity.patch.") }

        container.apply {
            View {
                attr {
                    flexDirectionRow()
                    paddingLeft(12f)
                    paddingRight(12f)
                    paddingTop(4f)
                    paddingBottom(4f)
                }
                View { attr { width(48f) } }

                View {
                    attr {
                        flex(1f)
                        borderRadius(10f)
                        backgroundLinearGradient(
                            Direction.TO_BOTTOM,
                            ColorStop(Color(0xFFEBF8FF), 0f),
                            ColorStop(Color(0xFFBEE3F8), 1f)
                        )
                        border(Border(1f, BorderStyle.SOLID, Color(0xFF63B3ED)))
                        paddingTop(10f)
                        paddingBottom(10f)
                        paddingLeft(12f)
                        paddingRight(12f)
                    }

                    // 头部：图标 + 活动类型徽章
                    View {
                        attr {
                            flexDirectionRow()
                            alignItemsCenter()
                            marginBottom(8f)
                        }
                        Text {
                            attr {
                                text("📋")
                                fontSize(14f)
                                marginRight(6f)
                            }
                        }
                        View {
                            attr {
                                borderRadius(4f)
                                backgroundColor(Color(0xFF3182CE))
                                paddingLeft(6f)
                                paddingRight(6f)
                                paddingTop(2f)
                                paddingBottom(2f)
                                marginRight(8f)
                            }
                            Text {
                                attr {
                                    text(activityType)
                                    fontSize(10f)
                                    color(Color.WHITE)
                                    fontWeightSemiBold()
                                }
                            }
                        }
                        if (stepCount > 0) {
                            Text {
                                attr {
                                    text("$completedSteps/$stepCount 完成")
                                    fontSize(10f)
                                    color(Color(0xFF4A5568))
                                }
                            }
                        }
                    }

                    // 标题
                    Text {
                        attr {
                            text(title)
                            fontSize(14f)
                            color(Color(0xFF2A4365))
                            fontWeightSemiBold()
                            marginBottom(6f)
                        }
                    }

                    // 进度条
                    if (stepCount > 0) {
                        val progress = completedSteps.toFloat() / stepCount.coerceAtLeast(1)
                        View {
                            attr {
                                height(4f)
                                borderRadius(2f)
                                backgroundColor(Color(0x33000000))
                                flexDirectionRow()
                                marginTop(4f)
                            }
                            View {
                                attr {
                                    height(4f)
                                    borderRadius(2f)
                                    backgroundColor(Color(0xFF3182CE))
                                    flex(progress.coerceIn(0f, 1f))
                                }
                            }
                            View {
                                attr {
                                    flex((1f - progress).coerceIn(0f, 1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================
// 3. Reasoning 推理渲染器
// ============================

/**
 * Reasoning 推理消息渲染器。
 *
 * 将 `agUiKind == "reasoning"` 的消息渲染为灰色虚线边框的思考块：
 * ```
 * ┊─ 🧠 推理过程 ─────────┊
 * ┊ 用户询问数学问题，     ┊
 * ┊ 需要计算 17 × 23...   ┊
 * ┊─────────────────────────┊
 * ```
 */
class ReasoningRenderer : MessageRendererFactory {
    override fun canRender(message: ChatMessage): Boolean {
        return message.extra["agUiKind"] == "reasoning"
    }

    override fun render(
        container: ViewContainer<*, *>,
        context: MessageContext,
        config: ChatSessionConfig
    ) {
        val msg = context.message
        container.apply {
            View {
                attr {
                    flexDirectionRow()
                    paddingLeft(12f)
                    paddingRight(12f)
                    paddingTop(4f)
                    paddingBottom(4f)
                }
                View { attr { width(48f) } }

                View {
                    attr {
                        flex(1f)
                        borderRadius(8f)
                        backgroundColor(Color(0xFFF7FAFC))
                        border(Border(1f, BorderStyle.DASHED, Color(0xFFA0AEC0)))
                        paddingTop(8f)
                        paddingBottom(8f)
                        paddingLeft(12f)
                        paddingRight(12f)
                    }

                    // 标签
                    View {
                        attr {
                            flexDirectionRow()
                            alignItemsCenter()
                            marginBottom(4f)
                        }
                        Text {
                            attr {
                                text("🧠")
                                fontSize(12f)
                                marginRight(4f)
                            }
                        }
                        Text {
                            attr {
                                text("推理过程")
                                fontSize(11f)
                                color(Color(0xFF718096))
                                fontWeightSemiBold()
                            }
                        }
                    }

                    // 推理内容（斜体）
                    Text {
                        attr {
                            text(msg.content)
                            fontSize(12f)
                            color(Color(0xFF4A5568))
                            fontStyleItalic()
                            lineHeight(17f)
                        }
                    }
                }
            }
        }
    }
}

// ============================
// 4. 事件时间轴小组件（浮层）
// ============================

/** 最近事件条目（用于时间轴展示） */
data class AgUiEventLogItem(
    val seq: Int,
    val type: String,
    val summary: String,
    val timestamp: Long
)

/**
 * 右下角浮动的"事件时间轴"小组件。
 *
 * 显示最近 N 个 AG-UI 事件的类型与摘要，带折叠/展开动画。
 * 作为调试/可视化工具，让协议事件流看得见、摸得着。
 */
fun ViewContainer<*, *>.AgUiEventTimeline(
    items: () -> List<AgUiEventLogItem>,
    expanded: () -> Boolean,
    onToggle: () -> Unit,
    maxVisible: Int = 5
) {
    val getItems = items
    val getExpanded = expanded

    // 将响应式依赖提升到构建阶段：getExpanded() 和 getItems() 在 View 创建时求值，
    // Kuikly 会自动追踪其中访问的 observable，变化时整个 View 重新构建
    val expandedVal = getExpanded()
    val itemList = getItems().takeLast(maxVisible).reversed()

    View {
        attr {
            positionAbsolute()
            right(12f)
            bottom(12f)
            width(if (expandedVal) 240f else 44f)
            borderRadius(12f)
            backgroundColor(Color(0xE61A202E))
            boxShadow(BoxShadow(0f, 4f, 12f, Color(0x66000000)))
        }

        // 折叠按钮 / 标题栏
        View {
            attr {
                flexDirectionRow()
                alignItemsCenter()
                paddingLeft(12f)
                paddingRight(12f)
                height(32f)
            }
            Text {
                attr {
                    text(if (expandedVal) "📊 事件时间轴" else "📊")
                    fontSize(12f)
                    color(Color.WHITE)
                    fontWeightSemiBold()
                    flex(1f)
                }
            }
            if (expandedVal) {
                Text {
                    attr {
                        text("×")
                        fontSize(16f)
                        color(Color(0xFFA0AEC0))
                    }
                }
            }
            event {
                click { onToggle() }
            }
        }

        // 展开态：列表
        if (expandedVal) {
            View {
                attr {
                    paddingBottom(8f)
                    paddingLeft(8f)
                    paddingRight(8f)
                }
                View {
                    attr {
                        height(1f)
                        backgroundColor(Color(0x44FFFFFF))
                        marginBottom(6f)
                    }
                }

                if (itemList.isEmpty()) {
                    View {
                        attr {
                            paddingLeft(6f)
                            paddingRight(6f)
                            paddingTop(6f)
                            paddingBottom(6f)
                        }
                        Text {
                            attr {
                                text("（暂无事件）")
                                fontSize(11f)
                                color(Color(0xFFA0AEC0))
                            }
                        }
                    }
                } else {
                    itemList.forEach { item ->
                        AgUiEventTimelineRow(item)
                    }
                }
            }
        }
    }
}

/** 时间轴单行 */
private fun ViewContainer<*, *>.AgUiEventTimelineRow(item: AgUiEventLogItem) {
    // 按事件类型给出不同配色
    val (emoji, color) = categorize(item.type)
    View {
        attr {
            flexDirectionRow()
            alignItems(FlexAlign.FLEX_START)
            paddingTop(3f)
            paddingBottom(3f)
        }
        // 序号
        View {
            attr {
                width(22f)
                alignItems(FlexAlign.CENTER)
            }
            Text {
                attr {
                    text("#${item.seq}")
                    fontSize(9f)
                    color(Color(0xFF718096))
                }
            }
        }
        // 图标
        Text {
            attr {
                text(emoji)
                fontSize(11f)
                marginRight(4f)
            }
        }
        // 类型
        Text {
            attr {
                text(shortenType(item.type))
                fontSize(10f)
                color(Color(color))
                fontWeightSemiBold()
                marginRight(6f)
                width(100f)
            }
        }
        // 摘要
        Text {
            attr {
                text(item.summary)
                fontSize(10f)
                color(Color(0xFFE2E8F0))
                flex(1f)
                lines(1)
            }
        }
    }
}

/** 事件类型分类 → (emoji, color) */
private fun categorize(type: String): Pair<String, Long> = when {
    type.startsWith("RUN_") -> "🚀" to 0xFF63B3EDL
    type.startsWith("STEP_") -> "🔄" to 0xFFF6AD55L
    type.startsWith("TEXT_") -> "💬" to 0xFF48BB78L
    type.startsWith("TOOL_") -> "🔧" to 0xFF9F7AEAL
    type.startsWith("ACTIVITY_") -> "📋" to 0xFF4299E1L
    type.startsWith("REASONING_") -> "🧠" to 0xFFA0AEC0L
    type == "META" -> "👍" to 0xFFF687B3L
    type == "CUSTOM" -> "✨" to 0xFFFBD38DL
    type.startsWith("STATE_") || type == "MESSAGES_SNAPSHOT" -> "💾" to 0xFF68D391L
    else -> "❔" to 0xFFA0AEC0L
}

/** 缩短事件类型名（TEXT_MESSAGE_CONTENT → TXT_CONTENT） */
private fun shortenType(type: String): String = type
    .replace("TEXT_MESSAGE_", "TXT_")
    .replace("TOOL_CALL_", "TOOL_")
    .replace("ACTIVITY_", "ACT_")
    .replace("REASONING_MESSAGE_", "REA_")
    .replace("REASONING_", "REA_")
