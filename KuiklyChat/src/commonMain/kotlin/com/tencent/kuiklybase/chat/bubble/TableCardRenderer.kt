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
 * 表格卡片渲染器。
 *
 * 将 Markdown 表格渲染为带边框的网格布局：
 * ```
 * ┌─────────┬─────────┬─────────┐
 * │  Header │  Header │  Header │
 * ├─────────┼─────────┼─────────┤
 * │  Cell   │  Cell   │  Cell   │
 * └─────────┴─────────┴─────────┘
 * ```
 */
class TableCardRenderer : MessageRendererFactory {
    override fun canRender(message: ChatMessage): Boolean =
        RichMessageKind.matches(message, RichMessageKind.TABLE)

    override fun render(container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig) {
        val msg = context.message
        val headers = parseJsonStringArray(msg.extra[AgUiExtraKeys.TABLE_HEADERS].orEmpty())
        val rows = parseJson2DArray(msg.extra[AgUiExtraKeys.TABLE_ROWS].orEmpty())
        val caption = msg.extra[AgUiExtraKeys.TABLE_CAPTION].orEmpty()

        container.agUiCardShell(
            accentColor = Color(0xFF3B82F6),
            borderColor = Color(0xFFBFDBFE),
            backgroundColor = Color(0xFFFFFFFF),
            header = {
                Text {
                    attr {
                        text("📊")
                        fontSize(14f)
                        marginRight(6f)
                    }
                }
                Text {
                    attr {
                        text(caption.ifEmpty { "表格" })
                        fontSize(12f)
                        color(Color(0xFF1E3A5F))
                        fontWeightSemiBold()
                        flex(1f)
                    }
                }
            },
            body = {
                if (headers.isNotEmpty() || rows.isNotEmpty()) {
                    val allCols = if (headers.isNotEmpty()) headers.size else rows.firstOrNull()?.size ?: 0

                    // 表头行
                    if (headers.isNotEmpty()) {
                        View {
                            attr {
                                flexDirectionRow()
                                backgroundColor(Color(0xFFEBF5FF))
                                borderRadius(6f, 6f, 0f, 0f)
                            }
                            headers.forEach { h ->
                                View {
                                    attr {
                                        if (allCols > 0) flex(1f / allCols) else flex(1f)
                                        padding(8f)
                                        borderRight(Border(1f, BorderStyle.SOLID, Color(0xFFBFDBFE)))
                                    }
                                    Text {
                                        attr {
                                            text(h)
                                            fontSize(11f)
                                            color(Color(0xFF1E3A5F))
                                            fontWeightSemiBold()
                                            lines(1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // 数据行
                    rows.forEachIndexed { rowIdx, row ->
                        View {
                            attr {
                                flexDirectionRow()
                                if (rowIdx == rows.size - 1) borderRadius(0f, 0f, 6f, 6f)
                                backgroundColor(if (rowIdx % 2 == 0) Color(0xFFFFFFFF) else Color(0xFFF8FAFC))
                            }
                            row.forEach { cell ->
                                View {
                                    attr {
                                        if (allCols > 0) flex(1f / allCols) else flex(1f)
                                        padding(8f)
                                        borderRight(Border(1f, BorderStyle.SOLID, Color(0xFFE2E8F0)))
                                        borderBottom(Border(1f, BorderStyle.SOLID, Color(0xFFE2E8F0)))
                                    }
                                    Text {
                                        attr {
                                            text(cell)
                                            fontSize(11f)
                                            color(Color(0xFF334155))
                                            lineHeight(16f)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text {
                        attr {
                            text("（空表格）")
                            fontSize(12f)
                            color(Color(0xFF94A3B8))
                        }
                    }
                }
            }
        )
    }

    private fun parseJsonStringArray(json: String): List<String> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.optString(it, "") ?: "" }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseJson2DArray(json: String): List<List<String>> {
        if (json.isBlank()) return emptyList()
        return try {
            val outer = JSONArray(json)
            (0 until outer.length()).map { i ->
                val inner = outer.optJSONArray(i)
                (0 until (inner?.length() ?: 0)).map { j ->
                    inner?.optString(j, "") ?: ""
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
