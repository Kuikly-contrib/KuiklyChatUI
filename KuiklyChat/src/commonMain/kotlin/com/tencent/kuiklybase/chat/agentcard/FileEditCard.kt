package com.tencent.kuiklybase.chat.agentcard

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.*

/**
 * 文件编辑卡片。
 *
 * 显示：
 * - 文件路径 + 操作类型（创建/更新/删除/重命名）
 * - +N / -N 增删行数徽章
 * - 可选的 diff 文本（行级 +/- 着色）
 *
 * 交互：
 * - 点击头部切换 diff 展开/折叠
 * - 默认展开；diff 超过 12 行时默认折叠
 */
fun ViewContainer<*, *>.FileEditCard(
    card: AgentCard.FileEdit,
    theme: AgentCardTheme = AgentCardTheme.Default
) {
    addChild(FileEditCardView()) {
        attr {
            cardData = card
            this.theme = theme
        }
    }
}

internal class FileEditCardView : ComposeView<FileEditCardAttr, ComposeEvent>() {
    override fun createAttr() = FileEditCardAttr()
    override fun createEvent() = ComposeEvent()

    /** Diff 展开状态 */
    private var expanded by observable(true)
    /** 是否已应用过初始折叠策略 */
    private var initialExpandApplied = false

    override fun created() {
        super.created()
        if (!initialExpandApplied) {
            val diff = attr.cardData.diffText
            val lines = if (diff.isEmpty()) 0 else diff.count { it == '\n' } + 1
            // Diff 超过 12 行默认折叠；短 diff 一眼可见
            expanded = lines <= 12
            initialExpandApplied = true
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            val card = ctx.attr.cardData
            val theme = ctx.attr.theme
            val (opLabel, opColor) = when (card.operation) {
                FileOp.CREATE -> "新建" to 0xFF10B981
                FileOp.UPDATE -> "修改" to 0xFF3B82F6
                FileOp.DELETE -> "删除" to 0xFFEF4444
                FileOp.RENAME -> "重命名" to 0xFFF59E0B
            }

            View {
                attr {
                    marginLeft(theme.cardMarginH)
                    marginRight(theme.cardMarginH)
                    marginTop(theme.cardMarginV)
                    marginBottom(theme.cardMarginV)
                    borderRadius(theme.cardRadius)
                    backgroundColor(Color(theme.cardBackground))
                    border(Border(1f, BorderStyle.SOLID, Color(theme.borderColor)))
                }

                // ===== 头部（可点击切换 diff 展开态）=====
                View {
                    attr {
                        flexDirectionRow()
                        alignItemsCenter()
                        paddingLeft(12f)
                        paddingRight(12f)
                        paddingTop(10f)
                        paddingBottom(10f)
                        backgroundColor(Color(0xFFF8FAFC))
                        borderRadius(BorderRectRadius(
                            theme.cardRadius, theme.cardRadius, 0f, 0f
                        ))
                    }
                    event {
                        click {
                            if (ctx.attr.cardData.diffText.isNotEmpty()) {
                                ctx.expanded = !ctx.expanded
                            }
                        }
                    }
                    // 操作徽章
                    View {
                        attr {
                            paddingLeft(6f)
                            paddingRight(6f)
                            paddingTop(2f)
                            paddingBottom(2f)
                            borderRadius(4f)
                            backgroundColor(Color(opColor))
                            marginRight(8f)
                        }
                        Text {
                            attr {
                                text(opLabel)
                                fontSize(10f)
                                color(Color.WHITE)
                                fontWeightSemiBold()
                            }
                        }
                    }
                    // 文件路径
                    Text {
                        attr {
                            text(card.filePath)
                            fontSize(13f)
                            color(Color(theme.titleColor))
                            fontWeightMedium()
                            flex(1f)
                            lines(1)
                        }
                    }
                    // +N
                    vif({ ctx.attr.cardData.additions > 0 }) {
                        Text {
                            attr {
                                text("+${ctx.attr.cardData.additions}")
                                fontSize(11f)
                                color(Color(theme.successColor))
                                fontWeightSemiBold()
                                marginRight(6f)
                            }
                        }
                    }
                    // -N
                    vif({ ctx.attr.cardData.deletions > 0 }) {
                        Text {
                            attr {
                                text("-${ctx.attr.cardData.deletions}")
                                fontSize(11f)
                                color(Color(theme.errorColor))
                                fontWeightSemiBold()
                            }
                        }
                    }
                }

                // ===== Diff 内容（展开态）=====
                vif({ ctx.expanded && ctx.attr.cardData.diffText.isNotEmpty() }) {
                    View {
                        attr {
                            paddingTop(8f)
                            paddingBottom(8f)
                        }
                        ctx.attr.cardData.diffText.split("\n").forEach { line ->
                            val (bg, fg) = when {
                                line.startsWith("+") && !line.startsWith("+++") ->
                                    theme.diffAddBg to theme.diffAddText
                                line.startsWith("-") && !line.startsWith("---") ->
                                    theme.diffRemoveBg to theme.diffRemoveText
                                line.startsWith("@@") ->
                                    0xFFEEF2FF to 0xFF4338CA
                                else ->
                                    theme.cardBackground to theme.bodyColor
                            }
                            View {
                                attr {
                                    backgroundColor(Color(bg))
                                    paddingLeft(12f)
                                    paddingRight(12f)
                                    paddingTop(2f)
                                    paddingBottom(2f)
                                }
                                Text {
                                    attr {
                                        text(line.ifEmpty { " " })
                                        fontSize(12f)
                                        color(Color(fg))
                                        lineHeight(17f)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

internal class FileEditCardAttr : ComposeAttr() {
    var cardData: AgentCard.FileEdit by observable(
        AgentCard.FileEdit(id = "", filePath = "")
    )
    var theme: AgentCardTheme by observable(AgentCardTheme.Default)
}
