package com.tencent.kuiklybase.chat.compose.agentcard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Compose 版 BouncingDots —— 三个圆点依次激活的波浪式动画。
 *
 * 1:1 对齐 [com.tencent.kuiklybase.chat.widget.BouncingDotsView]：
 * - 三个圆点通过 dotPhase (0→1→2→0) 循环
 * - 激活圆点放大 + 不透明，未激活缩小 + 半透明
 * - 默认间隔 200ms
 *
 * @param color 圆点颜色
 * @param dotSize 单个圆点直径（dp）
 * @param dotGap 圆点间距（dp）
 * @param activeScale 激活时缩放
 * @param inactiveScale 未激活时缩放
 * @param activeOpacity 激活时透明度
 * @param inactiveOpacity 未激活时透明度
 * @param animationInterval 动画切换间隔（ms）
 */
@Composable
fun BouncingDots(
    color: Color = Color(0xFF999999),
    dotSize: Float = 8f,
    dotGap: Float = 4f,
    activeScale: Float = 1.4f,
    inactiveScale: Float = 0.55f,
    activeOpacity: Float = 1f,
    inactiveOpacity: Float = 0.5f,
    animationInterval: Long = 200L,
    modifier: Modifier = Modifier
) {
    var phase by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(animationInterval)
            phase = (phase + 1) % 3
        }
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        for (i in 0..2) {
            val isActive = phase == i
            val scale = if (isActive) activeScale else inactiveScale
            val alpha = if (isActive) activeOpacity else inactiveOpacity
            val sz = dotSize * scale
            Box(
                modifier = Modifier
                    .size(sz.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color.copy(alpha = alpha))
            )
            if (i < 2) Spacer(modifier = Modifier.width(dotGap.dp))
        }
    }
}
