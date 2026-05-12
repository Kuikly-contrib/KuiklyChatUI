package com.tencent.kuiklybase.chat.agentcard_compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.chat.agentcard.AgentCard
import com.tencent.kuiklybase.chat.agentcard.LoadingStyle
import kotlinx.coroutines.delay

/**
 * Compose 版加载中卡片。
 *
 * 1:1 对齐 DSL [com.tencent.kuiklybase.chat.agentcard.LoadingCard]。
 *
 * 支持四种样式：DOTS（BouncingDots）、SPINNER（旋转字符）、PROGRESS（进度条）、SKELETON（骨架屏）
 */
@Composable
fun ComposeLoadingCard(
    card: AgentCard.Loading,
    theme: ComposeCardTheme = ComposeCardTheme.Default,
    modifier: Modifier = Modifier
) {
    var dotPhase by remember { mutableStateOf(0) }
    val needsAnim = card.style == LoadingStyle.SPINNER || card.style == LoadingStyle.PROGRESS
    if (needsAnim) {
        LaunchedEffect(Unit) {
            while (true) { delay(400); dotPhase = (dotPhase + 1) % 3 }
        }
    }

    // openspec: add-workflow-phase-progress (visual revamp)
    //   去掉 border + background + 大 padding 和 BrainIcon prefix，让 "正在思考"
    //   文字直接左对齐到 ToolCallCard / FileEditCard 行（4dp 内 padding）的边缘。
    Row(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = theme.cardMarginH.dp, vertical = 6.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 仅 SPINNER / PROGRESS / SKELETON 保留小 icon；DOTS（"正在思考"）不前置图标
        when (card.style) {
            LoadingStyle.DOTS -> {
                // 不渲染前置 icon — 文字与其他工具行左对齐
            }
            LoadingStyle.SPINNER -> {
                Text(
                    when (dotPhase) { 0 -> "◐"; 1 -> "◓"; else -> "◑" },
                    fontSize = 14.sp, color = Color(0xFF6366F1)
                )
                Spacer(Modifier.width(8.dp))
            }
            LoadingStyle.PROGRESS, LoadingStyle.SKELETON -> {
                Text("◌", fontSize = 14.sp, color = Color(theme.warningColor))
                Spacer(Modifier.width(8.dp))
            }
        }

        // 右侧文字
        Column(modifier = Modifier.weight(1f)) {
            // 标题 + 右侧三点跳动（DOTS 样式）
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    card.title,
                    fontSize = 13.sp,
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.SemiBold,
                )
                if (card.style == LoadingStyle.DOTS) {
                    Spacer(Modifier.width(8.dp))
                    BouncingDots(
                        color = Color(0xFF8B5CF6), dotSize = 5f, dotGap = 3f
                    )
                }
            }
            if (card.subtitle.isNotEmpty()) {
                Text(
                    card.subtitle, fontSize = 11.sp, color = Color(theme.mutedColor),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (card.target.isNotEmpty()) {
                Text(
                    " ${card.target}", fontSize = 10.sp, color = Color(theme.linkColor),
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
            // 进度条
            if (card.style == LoadingStyle.PROGRESS) {
                val fraction = when (dotPhase) { 0 -> 0.3f; 1 -> 0.6f; else -> 0.9f }
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(Color(0xFFE2E8F0))
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(fraction)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(Color(0xFF6366F1))
                    )
                }
            }
        }
    }
}
