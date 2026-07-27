package com.tencent.kuiklybase.chat.bubble

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.views.*
import com.tencent.kuiklybase.chat.agui.CommandInfo
import com.tencent.kuiklybase.chat.agui.CommandState
import com.tencent.kuiklybase.chat.agui.LoadingStyle
import com.tencent.kuiklybase.chat.agui.RichMessageKind
import com.tencent.kuiklybase.chat.model.ChatMessage
import com.tencent.kuiklybase.chat.model.MessageContext
import com.tencent.kuiklybase.chat.session.ChatSessionConfig

/**
 * 命令执行卡片渲染器。
 *
 * 渲染样式：
 * ```
 * ┌──────────────────────────────────────┐
 * │ ● running  ~/work                    │
 * │ $ ls -la                             │
 * │ total 8                              │
 * │ drwx... ...                          │
 * └──────────────────────────────────────┘
 * ```
 */
class CommandExecCardRenderer : MessageRendererFactory {
    override fun canRender(message: ChatMessage): Boolean =
        RichMessageKind.matches(message, RichMessageKind.COMMAND_EXEC)

    override fun render(container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig) {
        val info = CommandInfo.from(context.message)
        val statusColor = when (info.state) {
            CommandState.SUCCESS -> Color(0xFF10B981)
            CommandState.FAILED -> Color(0xFFEF4444)
            else -> Color(0xFFFBBF24)
        }
        val statusText = when (info.state) {
            CommandState.SUCCESS -> "● success"
            CommandState.FAILED -> "● failed"
            else -> "◌ running"
        }

        container.View {
            attr {
                flexDirectionRow()
                alignSelfStretch()
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
                    backgroundColor(Color(0xFF0F172A))
                    border(Border(1f, BorderStyle.SOLID, Color(0xFF1E293B)))
                }

                // Header: 状态 + CWD + 耗时
                View {
                    attr {
                        flexDirectionRow()
                        alignItemsCenter()
                        paddingLeft(10f)
                        paddingRight(10f)
                        paddingTop(6f)
                        paddingBottom(6f)
                    }
                    Text {
                        attr {
                            text(statusText)
                            fontSize(10f)
                            color(statusColor)
                            fontWeightSemiBold()
                            marginRight(8f)
                        }
                    }
                    if (info.cwd.isNotEmpty()) {
                        Text {
                            attr {
                                text(info.cwd)
                                fontSize(10f)
                                color(Color(0xFF64748B))
                                flex(1f)
                            }
                        }
                    } else {
                        View { attr { flex(1f) } }
                    }
                    info.durationMs?.let { ms ->
                        Text {
                            attr {
                                text("${ms}ms")
                                fontSize(10f)
                                color(Color(0xFF64748B))
                                marginRight(6f)
                            }
                        }
                    }
                    info.exitCode?.let { code ->
                        Text {
                            attr {
                                text("exit $code")
                                fontSize(10f)
                                color(if (code == 0) Color(0xFF10B981) else Color(0xFFEF4444))
                            }
                        }
                    }
                }

                // 分隔线
                View {
                    attr {
                        height(1f)
                        backgroundColor(Color(0xFF1E293B))
                    }
                }

                // Body: 命令 + 输出
                View {
                    attr {
                        paddingLeft(12f)
                        paddingRight(12f)
                        paddingTop(8f)
                        paddingBottom(8f)
                    }
                    Text {
                        attr {
                            text("$ ${info.command}")
                            fontSize(12f)
                            color(Color(0xFF93C5FD))
                            lineHeight(17f)
                            marginBottom(4f)
                        }
                    }
                    if (info.output.isNotEmpty()) {
                        Text {
                            attr {
                                text(info.output)
                                fontSize(11f)
                                color(Color(0xFFCBD5E1))
                                lineHeight(16f)
                            }
                        }
                    } else if (info.isRunning) {
                        View {
                            attr {
                                flexDirectionRow()
                                alignItemsCenter()
                                marginTop(2f)
                            }
                            LoadingIndicator(LoadingStyle.DOTS, Color(0xFFFBBF24))
                            Text {
                                attr {
                                    text("  执行中...")
                                    fontSize(11f)
                                    color(Color(0xFF64748B))
                                    marginLeft(6f)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
