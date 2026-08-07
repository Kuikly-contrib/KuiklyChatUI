package com.kuikly.kuiklychat.dsl.ai

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.views.*
import com.kuikly.kuiklychat.base.BasePager
import com.tencent.kuiklybase.chat.ai.ChatAiTypingIndicator

/**
 * AI 打字效果指示器 Demo 页面
 *
 * 在屏幕正中间展示 ChatAiTypingIndicator 组件，
 * 方便预览动画效果。
 */
@Page("ai_typing_demo", supportInLocal = true)
internal class AiTypingIndicatorDemoPage : BasePager() {

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
                                text("AI 打字指示器 Demo")
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

                // ===== 主内容区域：居中展示组件 =====
                View {
                    attr {
                        flex(1f)
                        justifyContent(FlexJustifyContent.CENTER)
                        alignItems(FlexAlign.CENTER)
                    }

                    // ---------- 样式一：默认样式 ----------
                    View {
                        attr {
                            backgroundColor(Color.WHITE)
                            borderRadius(16f)
                            padding(12f, 20f, 12f, 20f)
                            marginBottom(24f)
                            boxShadow(BoxShadow(0f, 2f, 12f, Color(0x1A000000)))
                        }
                        Text {
                            attr {
                                text("默认样式")
                                fontSize(12f)
                                color(Color(0xFF999999))
                                marginBottom(8f)
                            }
                        }
                        ChatAiTypingIndicator {
                            attr {
                                text = "AI 正在思考"
                            }
                        }
                    }

                    // ---------- 样式二：主题色圆点 ----------
                    View {
                        attr {
                            backgroundColor(Color.WHITE)
                            borderRadius(16f)
                            padding(12f, 20f, 12f, 20f)
                            marginBottom(24f)
                            boxShadow(BoxShadow(0f, 2f, 12f, Color(0x1A000000)))
                        }
                        Text {
                            attr {
                                text("主题色样式")
                                fontSize(12f)
                                color(Color(0xFF999999))
                                marginBottom(8f)
                            }
                        }
                        ChatAiTypingIndicator {
                            attr {
                                text = "正在生成回复"
                                dotColor = 0xFF4F8FFF
                                textColor = 0xFF4F8FFF
                                dotSize = 10f
                                textFontSize = 16f
                            }
                        }
                    }

                    // ---------- 样式三：紫色渐变风格 ----------
                    View {
                        attr {
                            backgroundColor(Color(0xFF6C5CE7))
                            borderRadius(16f)
                            padding(12f, 20f, 12f, 20f)
                            boxShadow(BoxShadow(0f, 4f, 16f, Color(0x336C5CE7)))
                        }
                        Text {
                            attr {
                                text("深色背景样式")
                                fontSize(12f)
                                color(Color(0x99FFFFFF))
                                marginBottom(8f)
                            }
                        }
                        ChatAiTypingIndicator {
                            attr {
                                text = "Thinking"
                                dotColor = 0xFFFFFFFF
                                textColor = 0xFFFFFFFF
                                dotSize = 9f
                                textFontSize = 15f
                            }
                        }
                    }
                }
            }
        }
    }
}
