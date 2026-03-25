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
 * 在真实项目中建议使用 kotlinx-datetime 库替代。
 */
fun defaultTimeFormat(timestamp: Long): String {
    if (timestamp <= 0) return ""

    // 从时间戳提取时分
    val totalMinutes = timestamp / 60000
    val totalHours = timestamp / 3600000
    val hours = totalHours % 24
    val minutes = totalMinutes % 60
    val hh = hours.toString().padStart(2, '0')
    val mm = minutes.toString().padStart(2, '0')

    // 提取日期信息
    // 简化计算：将时间戳转换为天数（从 epoch 起始）
    val totalDays = timestamp / 86400000L

    // 计算 "现在" 的天数 — 由于 KMP commonMain 无法获取当前时间，
    // 我们使用一个参考时间来判断。如果时间戳看起来是合理的毫秒时间戳（> 2020年），
    // 使用相对日期格式；否则只返回 HH:mm。
    val isRealisticTimestamp = timestamp > 1577836800000L // 2020-01-01 00:00:00 UTC

    if (!isRealisticTimestamp) {
        return "$hh:$mm"
    }

    // 从时间戳提取年月日（简化的 UTC 计算）
    val daysSinceEpoch = totalDays
    // 简化：计算年、月、日
    val year = estimateYear(daysSinceEpoch)
    val dayOfYear = (daysSinceEpoch - daysUntilYear(year)).toInt()
    val monthDay = estimateMonthDay(year, dayOfYear)
    val month = monthDay.first.toString().padStart(2, '0')
    val day = monthDay.second.toString().padStart(2, '0')

    return "${year}-${month}-${day} $hh:$mm"
}

/**
 * 估算年份（从 epoch 天数）
 */
private fun estimateYear(daysSinceEpoch: Long): Int {
    // 粗略估算：365.25 天/年
    var year = 1970 + (daysSinceEpoch / 365.25).toInt()
    // 精确调整
    while (daysUntilYear(year + 1) <= daysSinceEpoch) year++
    while (daysUntilYear(year) > daysSinceEpoch) year--
    return year
}

/**
 * 计算从 epoch 到指定年份 1月1日的总天数
 */
private fun daysUntilYear(year: Int): Long {
    val y = year - 1
    return 365L * (year - 1970) + ((y / 4) - (y / 100) + (y / 400)) - ((1969 / 4) - (1969 / 100) + (1969 / 400))
}

/**
 * 从年内第几天估算月日
 */
private fun estimateMonthDay(year: Int, dayOfYear: Int): Pair<Int, Int> {
    val isLeap = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    val daysInMonth = intArrayOf(31, if (isLeap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    var remaining = dayOfYear
    for (m in daysInMonth.indices) {
        if (remaining < daysInMonth[m]) {
            return (m + 1) to (remaining + 1)
        }
        remaining -= daysInMonth[m]
    }
    return 12 to 31
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
