package com.tencent.kuiklybase.chat

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.*

// ============================
// 正在输入指示器组件
// ============================

/**
 * ChatTypingIndicatorView - 正在输入指示器组件
 *
 * 在消息列表底部显示 "XXX 正在输入..." 或三个跳动圆点动画。
 * 当 typingUsers 非空时显示，为空时隐藏。
 *
 * 使用方式：
 * ```
 * ChatTypingIndicator {
 *     attr {
 *         typingText = "Alice 正在输入..."
 *         dotColor = 0xFF999999
 *         textColor = 0xFF999999
 *     }
 * }
 * ```
 */
class ChatTypingIndicatorView : ComposeView<ChatTypingIndicatorAttr, ComposeEvent>() {
    override fun createAttr(): ChatTypingIndicatorAttr = ChatTypingIndicatorAttr()
    override fun createEvent(): ComposeEvent = ComposeEvent()

    // 三个圆点的动画阶段（0, 1, 2 循环）
    private var dotPhase by observable(0)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    flexDirectionRow()
                    alignItems(FlexAlign.CENTER)
                    padding(8f, 16f, 8f, 16f)
                }

                // 三个跳动圆点
                for (i in 0..2) {
                    View {
                        attr {
                            size(6f, 6f)
                            borderRadius(3f)
                            backgroundColor(Color(ctx.attr.dotColor))
                            marginRight(3f)
                            // 根据 dotPhase 设置不同圆点的 opacity 模拟跳动效果
                            val isActive = ctx.dotPhase == i
                            if (isActive) {
                                opacity(1f)
                                transform(Scale(1.3f, 1.3f))
                            } else {
                                opacity(0.4f)
                                transform(Scale(1f, 1f))
                            }
                            animate(Animation.easeInOut(0.3f), ctx.dotPhase)
                        }
                    }
                }

                // 文字
                if (ctx.attr.typingText.isNotEmpty()) {
                    Text {
                        attr {
                            text(ctx.attr.typingText)
                            fontSize(12f)
                            color(Color(ctx.attr.textColor))
                            marginLeft(4f)
                        }
                    }
                }
            }
        }
    }

    override fun viewDidLayout() {
        super.viewDidLayout()
        // 启动圆点动画循环
        startDotAnimation()
    }

    private fun startDotAnimation() {
        setTimeout(400) {
            dotPhase = (dotPhase + 1) % 3
            startDotAnimation()
        }
    }
}

class ChatTypingIndicatorAttr : ComposeAttr() {
    /** 显示的输入提示文字（如 "Alice 正在输入..."） */
    var typingText: String by observable("")
    /** 圆点颜色 */
    var dotColor: Long by observable(0xFF999999)
    /** 文字颜色 */
    var textColor: Long by observable(0xFF999999)
}

fun ViewContainer<*, *>.ChatTypingIndicator(init: ChatTypingIndicatorView.() -> Unit) {
    addChild(ChatTypingIndicatorView(), init)
}

/**
 * 根据正在输入的用户列表生成显示文字
 *
 * @param typingUsers 正在输入的用户名列表
 * @return 格式化后的文字，如 "Alice 正在输入..."、"Alice, Bob 正在输入..."
 */
fun formatTypingText(typingUsers: List<String>): String {
    if (typingUsers.isEmpty()) return ""
    return when (typingUsers.size) {
        1 -> "${typingUsers[0]} 正在输入..."
        2 -> "${typingUsers[0]}、${typingUsers[1]} 正在输入..."
        else -> "${typingUsers[0]}、${typingUsers[1]} 等${typingUsers.size}人正在输入..."
    }
}
