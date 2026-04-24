package com.tencent.kuiklybase.chat.agentcard

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.*
import com.tencent.kuiklybase.elements.CodeHighlighter

/**
 * 代码块卡片。
 *
 * 特性：
 * - 文件名 + 语言标签头
 * - 行号显示（深色背景）
 * - 超过 10 行默认折叠（可展开）
 * - 右上角复制按钮
 * - 代码区支持固定最大高度，超出部分可上下滚动（[maxHeight] 控制）
 *
 * @param card 代码块数据
 * @param theme 主题（默认 [AgentCardTheme.Default]）
 * @param maxHeight 代码区的最大高度（单位 dp）。默认 `360f`，超过此高度时代码区会滚动查看；
 *                  传入 `0f` 或负值代表不限高。
 * @param onCopy 点击复制按钮时的回调，参数为代码内容
 */
fun ViewContainer<*, *>.CodeBlockCard(
    card: AgentCard.CodeBlock,
    theme: AgentCardTheme = AgentCardTheme.Default,
    maxHeight: Float = 360f,
    onCopy: ((String) -> Unit)? = null
) {
    addChild(CodeBlockCardView()) {
        attr {
            cardData = card
            this.theme = theme
            this.maxCodeHeight = maxHeight
        }
        event {
            this.onCopyClick = onCopy
        }
    }
}

internal class CodeBlockCardView : ComposeView<CodeBlockCardAttr, CodeBlockCardEvent>() {
    override fun createAttr() = CodeBlockCardAttr()
    override fun createEvent() = CodeBlockCardEvent()

    /** 展开/折叠状态（运行时状态） */
    private var expanded by observable(true)
    /** 复制按钮的反馈状态 */
    private var copied by observable(false)
    /** copied 状态的去抖动 seq */
    private var copiedSeq = 0
    /** 是否已应用过一次初始折叠判定（避免 viewDidLoad 后反复覆盖用户操作） */
    private var initialExpandApplied = false

    override fun created() {
        super.created()
        // 仅在首次创建时按策略决定初始折叠态；后续 cardData 更新时不再覆盖用户交互
        if (!initialExpandApplied) {
            val lines = attr.cardData.code.lines().size
            expanded = !(attr.cardData.collapsedByDefault || lines > 10)
            initialExpandApplied = true
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            val card = ctx.attr.cardData
            val theme = ctx.attr.theme
            val lines = card.code.split("\n")

            View {
                attr {
                    marginLeft(theme.cardMarginH)
                    marginRight(theme.cardMarginH)
                    marginTop(theme.cardMarginV)
                    marginBottom(theme.cardMarginV)
                    borderRadius(theme.cardRadius)
                    border(Border(1f, BorderStyle.SOLID, Color(0xFF30363D)))
                    backgroundColor(Color(theme.codeBackground))
                    overflow(true)
                }

                // ===== 头部：文件名 + 语言 + 操作按钮 =====
                // GitHub Dark 风格：头部用 #161B22 作为 #0D1117 的上浮层，底部 1px 分隔线
                View {
                    attr {
                        flexDirectionRow()
                        alignItemsCenter()
                        paddingLeft(12f)
                        paddingRight(8f)
                        paddingTop(8f)
                        paddingBottom(8f)
                        backgroundColor(Color(0xFF161B22))
                        borderBottom(Border(1f, BorderStyle.SOLID, Color(0xFF30363D)))
                        borderRadius(BorderRectRadius(
                            theme.cardRadius, theme.cardRadius, 0f, 0f
                        ))
                    }
                    // 三色灯
                    View {
                        attr {
                            flexDirectionRow()
                            alignItemsCenter()
                            marginRight(10f)
                        }
                        View { attr { size(10f, 10f); borderRadius(5f); backgroundColor(Color(0xFFFF5F56)); marginRight(5f) } }
                        View { attr { size(10f, 10f); borderRadius(5f); backgroundColor(Color(0xFFFFBD2E)); marginRight(5f) } }
                        View { attr { size(10f, 10f); borderRadius(5f); backgroundColor(Color(0xFF27C93F)) } }
                    }
                    // 文件名
                    Text {
                        attr {
                            text(card.filename.ifEmpty { card.language.ifEmpty { "code" } })
                            fontSize(13f)
                            color(Color(0xFFE6EDF3))
                            fontWeightSemiBold()
                            flex(1f)
                            lines(1)
                        }
                    }
                    // 行数标签
                    Text {
                        attr {
                            text("${lines.size} 行")
                            fontSize(11f)
                            color(Color(0xFF8B949E))
                            marginRight(8f)
                        }
                    }
                    // 展开/折叠按钮
                    View {
                        attr {
                            paddingLeft(6f)
                            paddingRight(6f)
                            paddingTop(3f)
                            paddingBottom(3f)
                            borderRadius(4f)
                            backgroundColor(Color(0xFF21262D))
                            border(Border(1f, BorderStyle.SOLID, Color(0xFF30363D)))
                            marginRight(6f)
                        }
                        Text {
                            attr {
                                text(if (ctx.expanded) "收起" else "展开")
                                fontSize(11f)
                                color(Color(0xFFC9D1D9))
                            }
                        }
                        event {
                            click { ctx.expanded = !ctx.expanded }
                        }
                    }
                    // 复制按钮
                    View {
                        attr {
                            paddingLeft(8f)
                            paddingRight(8f)
                            paddingTop(3f)
                            paddingBottom(3f)
                            borderRadius(4f)
                            backgroundColor(
                                if (ctx.copied) Color(theme.successColor)
                                else Color(0xFF238636)
                            )
                        }
                        Text {
                            attr {
                                text(if (ctx.copied) "✓ 已复制" else "复制")
                                fontSize(11f)
                                color(Color.WHITE)
                                fontWeightSemiBold()
                            }
                        }
                        event {
                            click {
                                ctx.event.onCopyClick?.invoke(card.code)
                                ctx.copied = true
                                ctx.copiedSeq++
                                val mySeq = ctx.copiedSeq
                                com.tencent.kuikly.core.timer.setTimeout(1500) {
                                    if (mySeq == ctx.copiedSeq) ctx.copied = false
                                }
                            }
                        }
                    }
                }

                // ===== 代码主体（折叠态 / 展开态）=====
                // 注意：Kuikly 的 `if/else` 在 body() 里是静态代码路径，切换时不会重建子树；
                // 必须用 `vif` 指令做响应式条件渲染。
                vif({ ctx.expanded }) {
                    // 固定高度的 Scroller —— 由业务通过 [maxCodeHeight] 传入
                    // 高度 <= 0 时不限高，Scroller 自然撑开不滚动
                    val lineHeight = 18f
                    val maxCodeHeight = ctx.attr.maxCodeHeight

                    Scroller {
                        attr {
                            flexDirectionColumn()
                            backgroundColor(Color(theme.codeBackground))
                            borderRadius(BorderRectRadius(
                                0f, 0f, theme.cardRadius, theme.cardRadius
                            ))
                            paddingLeft(12f)
                            paddingRight(12f)
                            paddingTop(10f)
                            paddingBottom(10f)
                            if (maxCodeHeight > 0f) {
                                height(maxCodeHeight)
                            }
                        }
                        // 将行号拼入代码，保证对齐
                        val padWidth = lines.size.toString().length
                        val numberedCode = lines.mapIndexed { index, line ->
                            val num = (index + 1).toString().padStart(padWidth)
                            "$num  $line"
                        }.joinToString("\n")

                        // 使用 CodeHighlighter 进行语法高亮
                        val segments = CodeHighlighter.highlight(
                            code = numberedCode,
                            language = card.language.ifEmpty { null },
                            darkTheme = true,
                        )
                        val hasHighlight = segments.any { it.color != null || it.bold }

                        if (hasHighlight) {
                            RichText {
                                attr { lines(Int.MAX_VALUE) }
                                segments.forEach { segment ->
                                    Span {
                                        text(segment.text)
                                        fontSize(12f)
                                        lineHeight(lineHeight)
                                        color(Color(segment.color ?: theme.codeColor))
                                        if (segment.bold) fontWeightBold()
                                    }
                                }
                            }
                        } else {
                            Text {
                                attr {
                                    text(numberedCode)
                                    fontSize(12f)
                                    color(Color(theme.codeColor))
                                    lineHeight(lineHeight)
                                    lines(Int.MAX_VALUE)
                                }
                            }
                        }
                    }
                }
                vif({ !ctx.expanded }) {
                    // 折叠态
                    View {
                        attr {
                            paddingLeft(12f)
                            paddingRight(12f)
                            paddingTop(10f)
                            paddingBottom(10f)
                            backgroundColor(Color(0xFF161B22))
                            borderRadius(BorderRectRadius(
                                0f, 0f, theme.cardRadius, theme.cardRadius
                            ))
                            flexDirectionRow()
                            alignItemsCenter()
                        }
                    }
                }
            }
        }
    }

}

internal class CodeBlockCardAttr : ComposeAttr() {
    var cardData: AgentCard.CodeBlock by observable(
        AgentCard.CodeBlock(id = "", code = "")
    )
    var theme: AgentCardTheme by observable(AgentCardTheme.Default)
    /** 代码区最大高度（dp）。`<= 0` 表示不限高。 */
    var maxCodeHeight: Float by observable(360f)
}

internal class CodeBlockCardEvent : ComposeEvent() {
    var onCopyClick: ((String) -> Unit)? = null
}
