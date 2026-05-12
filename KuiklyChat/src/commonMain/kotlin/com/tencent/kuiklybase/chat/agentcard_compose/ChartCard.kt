package com.tencent.kuiklybase.chat.agentcard_compose

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.style.TextOverflow
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.chat.agentcard.AgentCard
import com.tencent.kuiklybase.chat.agentcard.ChartType

/**
 * Compose 版图表卡片（条形图）。
 *
 * 1:1 对齐 DSL [com.tencent.kuiklybase.chat.agentcard.ChartCard]。
 */
@Composable
fun ComposeChartCard(
    card: AgentCard.Chart,
    theme: ComposeCardTheme = ComposeCardTheme.Default,
    modifier: Modifier = Modifier
) {
    val maxValue = card.data.maxOfOrNull { it.value } ?: 1f
    val colors = listOf(
        0xFF6366F1L, 0xFF8B5CF6L, 0xFFEC4899L, 0xFFEF4444L,
        0xFFF59E0BL, 0xFF10B981L, 0xFF06B6D4L, 0xFF3B82F6L
    )

    Column(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = theme.cardMarginH.dp, vertical = theme.cardMarginV.dp)
            .clip(RoundedCornerShape(theme.cardRadius.dp))
            .border(1.dp, Color(theme.borderColor), RoundedCornerShape(theme.cardRadius.dp))
            .background(Color(theme.cardBackground))
            .padding(horizontal = theme.cardPaddingH.dp, vertical = 12.dp)
    ) {
        // 标题
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            Text("📈", fontSize = 14.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                card.title.ifEmpty { "Chart" },
                fontSize = 13.sp, color = Color(theme.titleColor),
                fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)
            )
            Text(
                when (card.chartType) {
                    ChartType.BAR -> "条形"
                    ChartType.LINE -> "折线"
                    ChartType.PIE -> "饼图"
                },
                fontSize = 10.sp, color = Color(theme.mutedColor)
            )
        }

        // 条形图主体
        card.data.forEachIndexed { index, point ->
            val color = colors[index % colors.size]
            val ratio = if (maxValue > 0) point.value / maxValue else 0f

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 标签
                Text(
                    point.label, fontSize = 11.sp, color = Color(theme.bodyColor),
                    modifier = Modifier.width(64.dp), maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // 条形
                Box(
                    modifier = Modifier.weight(1f).height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFF1F5F9))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ratio.coerceIn(0.01f, 1f))
                            .height(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(color))
                    )
                }
                Spacer(Modifier.width(6.dp))
                // 数值
                Text(
                    formatChartValue(point.value),
                    fontSize = 11.sp, color = Color(theme.titleColor),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(52.dp)
                )
            }
        }
    }
}

private fun formatChartValue(v: Float): String {
    if (v == v.toInt().toFloat()) return v.toInt().toString()
    return ((v * 10).toInt() / 10.0).toString()
}
