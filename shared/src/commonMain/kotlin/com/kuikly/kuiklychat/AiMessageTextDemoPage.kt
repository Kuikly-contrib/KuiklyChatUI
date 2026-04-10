package com.kuikly.kuiklychat

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.directives.velse
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.*
import com.kuikly.kuiklychat.base.BasePager
import com.tencent.kuiklybase.chat.ai.AiMessageText
import com.tencent.kuiklybase.chat.ai.AiTypingState

/**
 * AI 消息文本组件 Demo 页面
 *
 * 展示 AiMessageText 组件的流式输出 + Markdown 渲染效果。
 * 模拟 AI 对话场景，外部逐步追加文本，组件内部逐字动画显示。
 */
@Page("ai_message_text_demo", supportInLocal = true)
internal class AiMessageTextDemoPage : BasePager() {

    // ---- 模拟流式输出 ----
    private var streamContent by observable("")
    private var typingState3 by observable<AiTypingState>(AiTypingState.Nothing)
    private var isStreaming by observable(false)
    private var streamIndex = 0

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    flex(1f)
                    flexDirection(FlexDirection.COLUMN)
                    backgroundColor(Color(0xFFF0F2F5))
                }

                // ===== 顶部导航栏 =====
                View {
                    attr {
                        paddingTop(ctx.pagerData.statusBarHeight)
                        backgroundColor(Color.WHITE)
                    }
                    View {
                        attr {
                            height(44f)
                            allCenter()
                        }
                        Text {
                            attr {
                                text("AI 消息文本 Demo")
                                fontSize(17f)
                                fontWeightSemisolid()
                                color(Color(0xFF333333))
                            }
                        }
                    }
                    // 返回按钮
                    View {
                        attr {
                            positionAbsolute()
                            top(ctx.pagerData.statusBarHeight + 10f)
                            left(12f)
                            size(24f, 24f)
                            allCenter()
                        }
                        Text {
                            attr {
                                text("←")
                                fontSize(20f)
                                color(Color(0xFF333333))
                            }
                        }
                        event {
                            click {
                                ctx.acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
                            }
                        }
                    }
                }

                // ===== 可滚动内容区域 =====
                Scroller {
                    attr {
                        flex(1f)
                    }
                    View {
                        attr {
                            padding(16f)
                        }

                        // ========== 模拟流式输出 ==========
                        Text {
                            attr {
                                text("模拟 AI 流式输出")
                                fontSize(14f)
                                fontWeightSemisolid()
                                color(Color(0xFF666666))
                                marginBottom(8f)
                            }
                        }
                        // 控制按钮
                        View {
                            attr {
                                flexDirectionRow()
                                marginBottom(8f)
                            }
                            View {
                                attr {
                                    backgroundColor(Color(0xFF6C5CE7))
                                    borderRadius(8f)
                                    padding(6f, 16f, 6f, 16f)
                                    marginRight(8f)
                                }
                                Text {
                                    attr {
                                        text("🤖 开始生成")
                                        fontSize(13f)
                                        color(Color.WHITE)
                                    }
                                }
                                event {
                                    click {
                                        ctx.startStreaming()
                                    }
                                }
                            }
                            View {
                                attr {
                                    backgroundColor(Color(0xFF999999))
                                    borderRadius(8f)
                                    padding(6f, 16f, 6f, 16f)
                                }
                                Text {
                                    attr {
                                        text("🔄 重置")
                                        fontSize(13f)
                                        color(Color.WHITE)
                                    }
                                }
                                event {
                                    click {
                                        ctx.resetStreaming()
                                    }
                                }
                            }
                            vif({ ctx.isStreaming }) {
                                View {
                                    attr {
                                        marginLeft(8f)
                                        alignSelf(FlexAlign.CENTER)
                                    }
                                    Text {
                                        attr {
                                            text("🔄 生成中...")
                                            fontSize(12f)
                                            color(Color(0xFF6C5CE7))
                                        }
                                    }
                                }
                            }
                        }
                        View {
                            attr {
                                backgroundColor(Color.WHITE)
                                borderRadius(12f)
                                marginBottom(24f)
                                boxShadow(BoxShadow(0f, 2f, 8f, Color(0x1A000000)))
                                minHeight(60f)
                            }
                            vif({ ctx.streamContent.isNotEmpty() }) {
                                AiMessageText {
                                    attr {
                                        messageId = "stream_001"
                                        content = ctx.streamContent
                                        typingState = ctx.typingState3
                                        textColor = 0xFF333333
                                        fontSize = 15f
                                        typingSpeed = 2
                                    }
                                }
                            }
                            velse {
                                View {
                                    attr {
                                        allCenter()
                                        padding(20f)
                                    }
                                    Text {
                                        attr {
                                            text("点击「开始生成」模拟 AI 流式输出")
                                            fontSize(14f)
                                            color(Color(0xFF999999))
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

    /**
     * 模拟 AI 流式输出：每隔一段时间追加一段文本
     */
    private fun startStreaming() {
        if (isStreaming) return
        streamContent = ""
        streamIndex = 0
        isStreaming = true
        typingState3 = AiTypingState.Generating("stream_001")
        appendStreamChunk()
    }

    private fun appendStreamChunk() {
        if (streamIndex >= STREAM_CHUNKS.size) {
            // 流式输出完毕
            isStreaming = false
            typingState3 = AiTypingState.Nothing
            return
        }

        streamContent += STREAM_CHUNKS[streamIndex]
        streamIndex++

        // 模拟网络延迟，每 200~500ms 追加一段
        val delay = 200 + (streamIndex % 3) * 100
        setTimeout(delay) {
            appendStreamChunk()
        }
    }

    private fun resetStreaming() {
        isStreaming = false
        streamContent = ""
        streamIndex = 0
        typingState3 = AiTypingState.Nothing
    }

    companion object {
        /** 流式输出的分块内容 */
        private val STREAM_CHUNKS = listOf(
            "## 什么是",
            " Kotlin Multiplatform?\n\n",
            "Kotlin Multiplatform（KMP）是 ",
            "JetBrains 推出的**跨平台开发技术**",
            "，允许开发者使用 Kotlin 语言",
            "编写可在多个平台共享的代码。\n\n",
            "### 主要优势\n\n",
            "- 🔄 **代码共享** — ",
            "业务逻辑一次编写，多端复用\n",
            "- 🎯 **原生性能** — ",
            "编译为各平台原生代码\n",
            "- 🛠️ **渐进式采用** — ",
            "可以逐步迁移现有项目\n\n",
            "### 示例代码\n\n",
            "```kotlin\n",
            "expect fun platformName(): String\n\n",
            "fun greeting(): String {\n",
            "    return \"Hello from \${platformName()}!\"\n",
            "}\n```\n\n",
            "> KMP 已被 Google 官方推荐",
            "为 Android 多平台开发方案。",
        )
    }
}
