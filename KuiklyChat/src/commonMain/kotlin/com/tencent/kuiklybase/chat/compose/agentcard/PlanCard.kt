package com.tencent.kuiklybase.chat.compose.agentcard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.style.TextDecoration
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.chat.agentcard.AgentCard
import com.tencent.kuiklybase.chat.agentcard.PlanStep
import com.tencent.kuiklybase.chat.agentcard.PlanStepStatus

/**
 * Compose 版任务规划卡片。
 *
 * 1:1 对齐 DSL [com.tencent.kuiklybase.chat.agentcard.PlanCard]。
 *
 * 视觉：淡紫色主题，"📋 规划" + 标题 + 进度徽章。
 * 清单：每行一个圆形状态图标 + 步骤标题。
 * 交互：点击头部切换展开/折叠。
 */
@Composable
fun ComposePlanCard(
    card: AgentCard.Plan,
    theme: ComposeCardTheme = ComposeCardTheme.Default,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(true) }
    val done = card.steps.count { it.status == PlanStepStatus.DONE || it.status == PlanStepStatus.SKIPPED }
    val total = card.steps.size
    val allDone = total > 0 && done == total

    Column(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = theme.cardMarginH.dp, vertical = theme.cardMarginV.dp)
            .clip(RoundedCornerShape(theme.cardRadius.dp))
            .border(1.dp, Color(0xFFDDD6FE), RoundedCornerShape(theme.cardRadius.dp))
            .background(Color(0xFFFAF5FF))
    ) {
        // ===== 头部 =====
        Row(
            modifier = Modifier.fillMaxWidth()
                .clickable { if (card.steps.isNotEmpty()) expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = theme.cardPaddingV.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📋", fontSize = 15.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                card.title, fontSize = 13.sp, color = Color(0xFF6B21A8),
                fontWeight = FontWeight.SemiBold
            )
            if (card.summary.isNotEmpty()) {
                Text(
                    " · ${card.summary}", fontSize = 11.sp, color = Color(0xFF8B5CF6)
                )
            }
            Spacer(Modifier.weight(1f))
            // 进度徽章
            if (card.steps.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (allDone) Color(theme.successColor) else Color(0xFF8B5CF6))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        if (allDone) "✓ 已完成" else "$done / $total",
                        fontSize = 10.sp, color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // ===== 步骤清单 =====
        if (expanded && card.steps.isNotEmpty()) {
            Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp)) {
                card.steps.forEachIndexed { index, step ->
                    val isLast = index == card.steps.size - 1
                    ComposePlanStepRow(step, isLast, theme)
                }
            }
        }
    }
}

/**
 * 单个 Plan 步骤行。
 *
 * 左侧：状态图标（✓ / BouncingDots / ✗ / — / 空）
 * 右侧：标题（DONE 时加删除线）+ 可选详情
 */
@Composable
private fun ComposePlanStepRow(
    step: PlanStep,
    isLast: Boolean,
    theme: ComposeCardTheme
) {
    Row(
        modifier = Modifier.padding(top = 6.dp, bottom = if (isLast) 0.dp else 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 状态图标容器
        Box(
            modifier = Modifier.size(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    when (step.status) {
                        PlanStepStatus.DONE -> Color(theme.successColor)
                        PlanStepStatus.RUNNING -> Color(theme.warningColor)
                        PlanStepStatus.FAILED -> Color(theme.errorColor)
                        PlanStepStatus.SKIPPED -> Color(0xFFCBD5E1)
                        PlanStepStatus.PENDING -> Color(0xFFE9D5FF)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when (step.status) {
                PlanStepStatus.DONE -> Text(
                    "✓", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold
                )
                PlanStepStatus.RUNNING -> BouncingDots(
                    color = Color.White, dotSize = 3f, dotGap = 2f
                )
                PlanStepStatus.FAILED -> Text(
                    "✗", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold
                )
                PlanStepStatus.SKIPPED -> Text(
                    "—", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold
                )
                PlanStepStatus.PENDING -> Text(
                    "", fontSize = 1.sp
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        // 文本区
        Column(modifier = Modifier.weight(1f)) {
            Text(
                step.title, fontSize = 13.sp, lineHeight = 19.sp,
                color = when (step.status) {
                    PlanStepStatus.DONE -> Color(0xFF6E7681)
                    PlanStepStatus.SKIPPED -> Color(0xFF94A3B8)
                    PlanStepStatus.FAILED -> Color(theme.errorColor)
                    PlanStepStatus.RUNNING -> Color(0xFF0F172A)
                    PlanStepStatus.PENDING -> Color(0xFF475569)
                },
                fontWeight = if (step.status == PlanStepStatus.RUNNING) FontWeight.SemiBold else FontWeight.Normal,
                textDecoration = if (step.status == PlanStepStatus.DONE || step.status == PlanStepStatus.SKIPPED) TextDecoration.LineThrough else TextDecoration.None
            )
            if (step.detail.isNotEmpty()) {
                Text(
                    step.detail, fontSize = 11.sp, color = Color(0xFF94A3B8),
                    lineHeight = 16.sp, modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
