package com.tencent.kuiklybase.chat.compose.agentcard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.Canvas
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxHeight
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.draw.rotate
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.Path
import com.tencent.kuikly.compose.ui.graphics.StrokeCap
import com.tencent.kuikly.compose.ui.graphics.drawscope.Stroke
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.style.TextOverflow
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.chat.agentcard.AgentCard
import com.tencent.kuiklybase.chat.agentcard.TodoProgressItem
import kotlinx.coroutines.delay

// 设计 token（与 TaskProgressPill 同步：保持品牌一致）
private val SuccessGreen = Color(0xFF079669)
private val AccentPurple = Color(0xFF8B5CF6)
private val AccentPurpleLight = Color(0x338B5CF6)
private val TextPrimary = Color(0xFF0F172A)
private val TextSecondary = Color(0xFF334155)
private val TextTertiary = Color(0xFF64748B)
private val TextQuaternary = Color(0xFF94A3B8)
private val BorderSecondary = Color(0xFFCBD5E1)
private val ProgressTrack = Color(0xFFE2E8F0)

/**
 * TodoWrite 驱动的进度卡片（消息流内联）。
 *
 * 视觉对齐设计稿：
 * - 折叠态：`执行任务  ━━━━━━━━ N/M ⌄`（无前置 emoji，进度条贴在标题后）
 * - 展开态：每行 `<status icon> <content>`，icon 与 TaskProgressPill 同款
 *   - completed → 13×13 绿圆 + 白勾（来自用户提供的 SVG）
 *   - in_progress → 紫色 spinner（旋转弧）
 *   - pending → 灰色空心圆
 *
 * 默认折叠；用户可点击 toggle。
 */
@Composable
fun ComposeTodoProgressCard(
    card: AgentCard.TodoProgress,
    theme: ComposeCardTheme = ComposeCardTheme.Default,
    modifier: Modifier = Modifier,
) {
    val total = card.total.coerceAtLeast(0)
    val completed = card.completed.coerceIn(0, total.coerceAtLeast(1))
    val isDone = total > 0 && completed >= total

    var expanded by remember(card.id) { mutableStateOf(false) }

    val accent = if (isDone) SuccessGreen else AccentPurple

    Column(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = theme.cardMarginH.dp, vertical = 4.dp)
    ) {
        // ===== 折叠态行：title + 进度条 + 计数 + chevron =====
        Row(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "执行任务",
                fontSize = 13.sp,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.width(10.dp))
            // 进度条 — 弱化为细线，跟在标题后
            Box(
                modifier = Modifier.height(3.dp).width(140.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ProgressTrack),
            ) {
                if (total > 0 && completed > 0) {
                    val pct = (completed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier.fillMaxHeight()
                            .width((140.dp.value * pct).dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(accent),
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = "$completed/$total",
                fontSize = 12.sp,
                color = if (isDone) accent else TextTertiary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            // 折叠箭头：朝下 = 折叠，朝上 = 展开
            Box(
                modifier = Modifier.size(12.dp).rotate(if (expanded) 180f else 0f),
                contentAlignment = Alignment.Center,
            ) {
                ChevronDown(size = 12.dp, color = TextQuaternary)
            }
        }

        // ===== 展开态：todo 列表（每行 status icon + content）=====
        if (expanded && card.items.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 6.dp),
            ) {
                card.items.forEach { item ->
                    TodoRow(item)
                }
            }
        }
    }
}

@Composable
private fun TodoRow(item: TodoProgressItem) {
    val display = item.activeForm?.takeIf {
        item.status == "in_progress" && it.isNotBlank()
    } ?: item.content
    val textColor = when (item.status) {
        "completed" -> TextSecondary
        "in_progress" -> TextPrimary
        else -> TextQuaternary
    }
    val weight = if (item.status == "in_progress") FontWeight.SemiBold else FontWeight.Normal
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TodoStatusIcon(status = item.status, sizeDp = 13.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            text = display,
            fontSize = 13.sp,
            color = textColor,
            fontWeight = weight,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ──────────────────────────────────────────────────────────────
// 状态图标（与 TaskProgressPill 同款，自包含避免跨模块依赖）
// ──────────────────────────────────────────────────────────────

@Composable
private fun TodoStatusIcon(status: String, sizeDp: Dp) {
    when (status) {
        "completed" -> CheckCircleIcon(size = sizeDp)
        "in_progress" -> SpinnerCircleIcon(size = sizeDp)
        else -> PendingCircleIcon(size = sizeDp)
    }
}

/**
 * 完成图标 — 1:1 复刻用户提供的 13×13 SVG：
 *   外圆 fill #079669
 *   内勾 M4.875,6.5 → L5.958,7.583 → L8.125,5.417, white stroke
 */
@Composable
private fun CheckCircleIcon(size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val s = this.size.width / 13f
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val r = 5.4f * s
        drawCircle(color = SuccessGreen, radius = r, center = Offset(cx, cy))
        val checkStroke = (1.4f * s).coerceAtLeast(1.2f)
        val path = Path().apply {
            moveTo(4.875f * s, 6.5f * s)
            lineTo(5.958f * s, 7.583f * s)
            lineTo(8.125f * s, 5.417f * s)
        }
        drawPath(
            path = path,
            color = Color.White,
            style = Stroke(width = checkStroke, cap = StrokeCap.Round),
        )
    }
}

/** 加载中 — 紫色 spinner（旋转的高亮弧）。 */
@Composable
private fun SpinnerCircleIcon(size: Dp) {
    var rotation by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(16L)
            rotation = (rotation + 6f) % 360f
        }
    }
    Box(modifier = Modifier.size(size).rotate(rotation), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val r = this.size.width / 2f - 1.5f.dp.toPx()
            val stroke = 1.6f.dp.toPx()
            drawCircle(
                color = AccentPurpleLight,
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = stroke),
            )
            val headRadius = stroke * 1.2f
            drawCircle(
                color = AccentPurple,
                radius = headRadius,
                center = Offset(cx, cy - r),
            )
            val tailAngles = listOf(-25f, -50f, -75f)
            val alphas = listOf(0.7f, 0.4f, 0.2f)
            tailAngles.forEachIndexed { i, deg ->
                val rad = deg * kotlin.math.PI.toFloat() / 180f
                val px = cx + r * kotlin.math.sin(rad)
                val py = cy - r * kotlin.math.cos(rad)
                drawCircle(
                    color = AccentPurple.copy(alpha = alphas[i]),
                    radius = headRadius * (1f - i * 0.15f),
                    center = Offset(px, py),
                )
            }
        }
    }
}

/** 未完成 — 灰色空心圆（1.5dp 描边）。 */
@Composable
private fun PendingCircleIcon(size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val r = this.size.width / 2f - 1.dp.toPx()
        drawCircle(
            color = BorderSecondary,
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5f.dp.toPx()),
        )
    }
}

@Composable
private fun ChevronDown(size: Dp, color: Color) {
    Canvas(modifier = Modifier.size(size)) {
        val sx = this.size.width / 24f
        val sy = this.size.height / 24f
        val stroke = (2.4f * sx).coerceAtLeast(1.2f)
        val path = Path().apply {
            // M6 9 l6 6 l6 -6
            moveTo(6f * sx, 9f * sy)
            lineTo(12f * sx, 15f * sy)
            lineTo(18f * sx, 9f * sy)
        }
        drawPath(path = path, color = color, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
}
