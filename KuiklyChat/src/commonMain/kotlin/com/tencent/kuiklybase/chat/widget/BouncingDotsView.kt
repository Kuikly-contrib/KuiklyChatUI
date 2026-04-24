package com.tencent.kuiklybase.chat.widget

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.*

// ============================
// 跳动圆点动画组件（可复用）
// ============================

/**
 * BouncingDotsView - 可复用的三个跳动圆点动画组件
 *
 * 从 ChatAiTypingIndicatorView 中抽离的波浪式跳动圆点动画，
 * 可被 LoadingCard、ChatAiTypingIndicatorView、ChatTypingIndicatorView 等复用。
 *
 * 特性：
 * - 三个圆点依次激活，形成波浪式跳动效果
 * - 激活时放大 + 不透明，未激活时缩小 + 半透明
 * - 支持自定义颜色、大小、间距、动画参数
 *
 * 使用方式：
 * ```
 * BouncingDots {
 *     attr {
 *         dotColor = 0xFF6366F1
 *         dotSize = 8f
 *         dotGap = 4f
 *         animationInterval = 200
 *     }
 * }
 * ```
 */
class BouncingDotsView : ComposeView<BouncingDotsAttr, ComposeEvent>() {
    override fun createAttr(): BouncingDotsAttr = BouncingDotsAttr()
    override fun createEvent(): ComposeEvent = ComposeEvent()

    /** 当前激活的圆点索引（0→1→2→0 循环） */
    private var dotPhase by observable(0)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    flexDirectionRow()
                    alignItems(FlexAlign.CENTER)
                }

                for (i in 0..2) {
                    View {
                        attr {
                            val dotSize = ctx.attr.dotSize
                            size(dotSize, dotSize)
                            borderRadius(dotSize / 2f)
                            backgroundColor(Color(ctx.attr.dotColor))
                            if (i < 2) {
                                marginRight(ctx.attr.dotGap)
                            }

                            // 波浪式缩放动画
                            val isActive = (ctx.dotPhase % 3) == i
                            if (isActive) {
                                opacity(ctx.attr.activeOpacity)
                                transform(Scale(ctx.attr.activeScale, ctx.attr.activeScale))
                            } else {
                                opacity(ctx.attr.inactiveOpacity)
                                transform(Scale(ctx.attr.inactiveScale, ctx.attr.inactiveScale))
                            }
                            animate(Animation.easeInOut(0.25f), ctx.dotPhase)
                        }
                    }
                }
            }
        }
    }

    override fun viewDidLayout() {
        super.viewDidLayout()
        startDotAnimation()
    }

    /** 圆点波浪动画：按设定间隔循环切换 dotPhase */
    private fun startDotAnimation() {
        setTimeout(attr.animationInterval) {
            dotPhase = (dotPhase + 1) % 3
            startDotAnimation()
        }
    }
}

// ============================
// Attr 属性类
// ============================

class BouncingDotsAttr : ComposeAttr() {
    /** 圆点颜色 */
    var dotColor: Long by observable(0xFF999999)
    /** 单个圆点直径 */
    var dotSize: Float by observable(8f)
    /** 圆点之间的间距 */
    var dotGap: Float by observable(4f)
    /** 动画切换间隔（毫秒） */
    var animationInterval: Int by observable(200)
    /** 激活时的缩放倍数 */
    var activeScale: Float by observable(1.4f)
    /** 未激活时的缩放倍数 */
    var inactiveScale: Float by observable(0.55f)
    /** 激活时的透明度 */
    var activeOpacity: Float by observable(1f)
    /** 未激活时的透明度 */
    var inactiveOpacity: Float by observable(0.5f)
}

// ============================
// 扩展函数（注册到 DSL）
// ============================

/**
 * 跳动圆点动画
 *
 * 三个圆点依次激活形成波浪式跳动效果，可用于 loading 指示器、打字指示器等场景。
 *
 * @param init 组件配置 Lambda
 */
fun ViewContainer<*, *>.BouncingDots(init: BouncingDotsView.() -> Unit) {
    addChild(BouncingDotsView(), init)
}
