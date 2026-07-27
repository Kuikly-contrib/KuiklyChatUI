package com.tencent.kuiklybase.chat.bubble

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.views.*
import com.tencent.kuiklybase.chat.agui.AgUiExtraKeys
import com.tencent.kuiklybase.chat.agui.RichMessageKind
import com.tencent.kuiklybase.chat.model.ChatMessage
import com.tencent.kuiklybase.chat.model.MessageContext
import com.tencent.kuiklybase.chat.session.ChatSessionConfig

/**
 * 任务列表（Checklist / TODO list）渲染器。
 *
 * 渲染为带复选框的列表：
 * ```
 * ☑️ 已完成项
 * ☐  未完成项
 * ```
 */
class ChecklistCardRenderer : MessageRendererFactory {
    override fun canRender(message: ChatMessage): Boolean =
        RichMessageKind.matches(message, RichMessageKind.CHECKLIST)

    override fun render(container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig) {
        val msg = context.message
        val items = parseChecklistItems(msg.extra[AgUiExtraKeys.CHECKLIST_ITEMS].orEmpty())
        val completed = msg.extra[AgUiExtraKeys.CHECKLIST_COMPLETED]?.toIntOrNull() ?: 0

        container.agUiCardShell(
            accentColor = Color(0xFF10B981),
            borderColor = Color(0xFFA7F3D0),
            backgroundColor = Color(0xFFF0FDF4),
            header = {
                Text {
                    attr {
                        text("✅")
                        fontSize(14f)
                        marginRight(6f)
                    }
                }
                Text {
                    attr {
                        text("任务清单")
                        fontSize(12f)
                        color(Color(0xFF065F46))
                        fontWeightSemiBold()
                        flex(1f)
                    }
                }
                Text {
                    attr {
                        text("$completed / ${items.size}")
                        fontSize(11f)
                        color(Color(0xFF059669))
                        fontWeightSemiBold()
                    }
                }
            },
            body = {
                items.forEach { (text, checked) ->
                    View {
                        attr {
                            flexDirectionRow()
                            alignItemsCenter()
                            marginBottom(4f)
                        }
                        Text {
                            attr {
                                text(if (checked) "☑️" else "⬜")
                                fontSize(14f)
                                marginRight(8f)
                            }
                        }
                        Text {
                            attr {
                                text(text)
                                fontSize(12f)
                                color(if (checked) Color(0xFF94A3B8) else Color(0xFF334155))
                                lineHeight(18f)
                            }
                        }
                    }
                }
            }
        )
    }

    private fun parseChecklistItems(json: String): List<Pair<String, Boolean>> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.optJSONObject(i)
                val text = obj?.optString("text", "") ?: ""
                val checked = obj?.optBoolean("checked", false) ?: false
                text to checked
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
