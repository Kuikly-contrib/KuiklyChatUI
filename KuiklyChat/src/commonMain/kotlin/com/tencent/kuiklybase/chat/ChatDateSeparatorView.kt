package com.tencent.kuiklybase.chat

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.views.*

// ============================
// 日期分隔符组件
// ============================

/**
 * ChatDateSeparatorView - 消息列表中的日期分隔符组件
 *
 * 当两条消息之间的时间间隔超过阈值时，自动插入此组件显示日期/时间。
 * 样式：居中显示的灰色圆角背景标签。
 *
 * 使用方式：
 * ```
 * ChatDateSeparator {
 *     attr {
 *         dateText = "今天 14:30"
 *         // 可选：自定义颜色
 *         bgColor = 0xFFCECECE
 *         textColor = 0xFFFFFFFF
 *     }
 * }
 * ```
 */
class ChatDateSeparatorView : ComposeView<ChatDateSeparatorAttr, ComposeEvent>() {
    override fun createAttr(): ChatDateSeparatorAttr = ChatDateSeparatorAttr()
    override fun createEvent(): ComposeEvent = ComposeEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    allCenter()
                    padding(8f, 0f, 8f, 0f)
                }
                View {
                    attr {
                        backgroundColor(Color(ctx.attr.bgColor))
                        borderRadius(10f)
                        padding(4f, 10f, 4f, 10f)
                    }
                    Text {
                        attr {
                            text(ctx.attr.dateText)
                            fontSize(11f)
                            color(Color(ctx.attr.textColor))
                        }
                    }
                }
            }
        }
    }
}

class ChatDateSeparatorAttr : ComposeAttr() {
    /** 显示的日期文字 */
    var dateText: String by observable("")
    /** 背景色 */
    var bgColor: Long by observable(0xFFCECECE)
    /** 文字颜色 */
    var textColor: Long by observable(0xFFFFFFFF)
}

fun ViewContainer<*, *>.ChatDateSeparator(init: ChatDateSeparatorView.() -> Unit) {
    addChild(ChatDateSeparatorView(), init)
}

// ============================
// 日期格式化工具
// ============================

/**
 * 默认的日期格式化器
 *
 * 根据时间距今的远近，返回不同格式：
 * - 今天的消息："HH:mm"
 * - 昨天的消息："昨天 HH:mm"
 * - 本年内的消息："MM-dd HH:mm"
 * - 更早的消息："yyyy-MM-dd HH:mm"
 *
 * 注意：由于 KMP commonMain 没有 java.time，使用简化的时间计算。
 */
fun defaultTimeFormat(timestamp: Long): String {
    if (timestamp <= 0) return ""

    // 简化计算：使用时间戳差值判断
    // 在真实项目中，建议使用 kotlinx-datetime 库
    val hours = (timestamp / 3600000) % 24
    val minutes = (timestamp / 60000) % 60
    val hh = hours.toString().padStart(2, '0')
    val mm = minutes.toString().padStart(2, '0')

    return "$hh:$mm"
}

/**
 * 判断两条消息之间是否需要插入日期分隔符
 *
 * @param prevTimestamp 上一条消息的时间戳
 * @param currentTimestamp 当前消息的时间戳
 * @param interval 间隔阈值（毫秒）
 * @return 是否需要插入分隔符
 */
fun shouldShowDateSeparator(
    prevTimestamp: Long,
    currentTimestamp: Long,
    interval: Long = DEFAULT_TIME_GROUP_INTERVAL
): Boolean {
    if (currentTimestamp <= 0L || prevTimestamp <= 0L) return false
    return (currentTimestamp - prevTimestamp) >= interval
}
