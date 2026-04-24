package com.tencent.kuiklybase.chat.agentcard

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.*
import com.tencent.kuiklybase.chat.widget.BouncingDots

/**
 * 命令执行卡片（bash / python 等 shell 风格）。
 *
 * 视觉结构：
 * ```
 * ┌────────────────────────────────────┐
 * │ $ npm run build          ▼  1.4s   │  ← 命令行（可点击切换展开态）
 * │ ┌────────────────────────────────┐ │
 * │ │ total 16                       │ │  ← 输出（展开态）
 * │ │ drwx------+ 68 chen staff ...  │ │
 * │ └────────────────────────────────┘ │
 * │                          ✓ exit 0  │  ← 底部状态
 * └────────────────────────────────────┘
 * ```
 *
 * 交互：
 * - 点击命令行切换输出展开/折叠
 * - `SUCCESS` 默认折叠，`RUNNING` / `FAILED` 默认展开
 * - 输出超过 6 行时默认折叠
 */
fun ViewContainer<*, *>.CommandExecCard(
    card: AgentCard.CommandExec,
    theme: AgentCardTheme = AgentCardTheme.Default
) {
    addChild(CommandExecCardView()) {
        attr {
            cardData = card
            this.theme = theme
        }
    }
}

internal class CommandExecCardView : ComposeView<CommandExecCardAttr, ComposeEvent>() {
    override fun createAttr() = CommandExecCardAttr()
    override fun createEvent() = ComposeEvent()

    /** 输出展开/折叠状态（运行时状态） */
    private var expanded by observable(true)
    /** 是否已应用过初始折叠策略 */
    private var initialExpandApplied = false

    override fun created() {
        super.created()
        if (!initialExpandApplied) {
            val card = attr.cardData
            val outputLines = if (card.output.isEmpty()) 0 else card.output.count { it == '\n' } + 1
            // SUCCESS 默认折叠；RUNNING / FAILED / CANCELED 默认展开；
            // 另外只要输出 <= 6 行就默认展开，让短输出一眼可见
            expanded = when {
                card.state == ExecState.SUCCESS && outputLines > 6 -> false
                else -> true
            }
            initialExpandApplied = true
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            val card = ctx.attr.cardData
            val theme = ctx.attr.theme

            View {
                attr {
                    marginLeft(theme.cardMarginH)
                    marginRight(theme.cardMarginH)
                    marginTop(theme.cardMarginV)
                    marginBottom(theme.cardMarginV)
                    borderRadius(theme.cardRadius)
                    backgroundColor(Color(theme.terminalBackground))
                    border(Border(1f, BorderStyle.SOLID, Color(0xFF30363D)))
                    paddingLeft(12f)
                    paddingRight(12f)
                    paddingTop(10f)
                    paddingBottom(10f)
                }

                // ===== 命令行 + 执行状态 + 耗时 （可点击切换展开态）=====
                View {
                    attr {
                        flexDirectionRow()
                        alignItemsCenter()
                    }
                    event {
                        click {
                            // 只有存在输出时点击才有意义
                            if (ctx.attr.cardData.output.isNotEmpty()) {
                                ctx.expanded = !ctx.expanded
                            }
                        }
                    }
                    Text {
                        attr {
                            text("$ ")
                            fontSize(13f)
                            color(Color(0xFF10B981))
                            fontWeightBold()
                        }
                    }
                    Text {
                        attr {
                            text(card.command)
                            fontSize(13f)
                            color(Color(0xFFE6EDF3))
                            flex(1f)
                            lines(1)
                        }
                    }
                    // 执行中：跳动圆点
                    vif({ ctx.attr.cardData.state == ExecState.RUNNING }) {
                        BouncingDots {
                            attr {
                                dotColor = 0xFF8B949E
                                dotSize = 5f
                                dotGap = 3f
                                marginLeft(6f)
                            }
                        }
                    }
                    // 成功：✓
                    vif({
                        ctx.attr.cardData.state == ExecState.SUCCESS ||
                            (ctx.attr.cardData.exitCode != null && ctx.attr.cardData.exitCode == 0)
                    }) {
                        Text {
                            attr {
                                text("✓ success")
                                fontSize(12f)
                                color(Color(ctx.attr.theme.successColor))
                                fontWeightSemiBold()
                                marginLeft(6f)
                            }
                        }
                    }
                    // 失败：✗ exit N
                    vif({
                        ctx.attr.cardData.state == ExecState.FAILED ||
                            (ctx.attr.cardData.exitCode != null && ctx.attr.cardData.exitCode != 0)
                    }) {
                        Text {
                            attr {
                                val exit = ctx.attr.cardData.exitCode
                                text(if (exit != null) "✗ exit $exit" else "✗")
                                fontSize(11f)
                                color(Color(ctx.attr.theme.errorColor))
                                fontWeightSemiBold()
                                marginLeft(6f)
                            }
                        }
                    }
                    // 取消：⊘
                    vif({ ctx.attr.cardData.state == ExecState.CANCELED }) {
                        Text {
                            attr {
                                text("⊘")
                                fontSize(12f)
                                color(Color(ctx.attr.theme.mutedColor))
                                marginLeft(6f)
                            }
                        }
                    }
                    // 耗时
                    vif({ ctx.attr.cardData.durationMs != null }) {
                        Text {
                            attr {
                                text(formatDuration(ctx.attr.cardData.durationMs ?: 0L))
                                fontSize(11f)
                                color(Color(0xFF484F58))
                                marginLeft(8f)
                            }
                        }
                    }
                }

                // ===== 输出（展开态）=====
                vif({ ctx.expanded && ctx.attr.cardData.output.isNotEmpty() }) {
                    Text {
                        attr {
                            text(ctx.attr.cardData.output)
                            fontSize(12f)
                            color(Color(0xFF8B949E))
                            lineHeight(17f)
                            marginTop(8f)
                            lines(Int.MAX_VALUE)
                        }
                    }
                }
            }
        }
    }
}

internal class CommandExecCardAttr : ComposeAttr() {
    var cardData: AgentCard.CommandExec by observable(
        AgentCard.CommandExec(id = "", command = "")
    )
    var theme: AgentCardTheme by observable(AgentCardTheme.Default)
}

private fun formatDuration(ms: Long): String = when {
    ms < 1000 -> "${ms}ms"
    ms < 60_000 -> "${(ms / 100) / 10.0}s"
    else -> "${ms / 60_000}m ${(ms % 60_000) / 1000}s"
}
