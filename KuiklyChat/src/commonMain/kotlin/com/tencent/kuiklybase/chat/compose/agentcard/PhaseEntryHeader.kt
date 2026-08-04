package com.tencent.kuiklybase.chat.compose.agentcard

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.style.TextOverflow
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.chat.agentcard.AgentCard

/**
 * 内联阶段进入头（5 阶段：WORKSPACE / PLAN / DESIGN / LINT / COMPILE）。
 *
 * DEVELOPMENT 阶段不渲染本组件 —— 由 [ComposeTodoProgressCard] 统一承担。
 *
 * 视觉：浅紫底 + 圆角 + 紫色边 + 单行 13sp Bold + 箭头 icon。
 *
 * openspec: add-workflow-phase-progress
 */
@Composable
fun ComposePhaseEntryHeader(
    card: AgentCard.PhaseHeader,
    theme: ComposeCardTheme = ComposeCardTheme.Default,
    modifier: Modifier = Modifier,
) {
    // 视觉减重：取消前置 → / ✓ / ✗ icon；阶段名直接 13sp SemiBold + reason 浅灰
    Row(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = theme.cardMarginH.dp, vertical = 6.dp)
            .padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = card.phaseName,
            fontSize = 13.sp,
            color = Color(0xFF334155),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (card.reason.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = card.reason,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
