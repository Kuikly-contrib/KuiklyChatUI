package com.tencent.kuiklybase.chat.compose.agentcard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
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
import com.tencent.kuikly.compose.ui.text.font.FontStyle
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.style.TextOverflow
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.chat.agentcard.AgentCard

/**
 * Compose 版思考过程卡片。
 *
 * 1:1 对齐 DSL [com.tencent.kuiklybase.chat.agentcard.ThinkingCard]。
 *
 * 视觉：灰色虚线边框，斜体文本，表示 Agent 的内部推理。
 * 特性：
 * - 流式中默认展开
 * - 非流式、内容较长时默认折叠（>80字）
 * - 点击整张卡片切换展开/收起
 */
@Composable
fun ComposeThinkingCard(
    card: AgentCard.Thinking,
    theme: ComposeCardTheme = ComposeCardTheme.Default,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(card.streaming || card.content.length <= 80) }

    Column(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            // DSL 用 DASHED border，Compose 无 DASHED，用 1dp 实线灰边模拟
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
            .background(Color(0xFFF8FAFC))
            .clickable { expanded = !expanded }
            .padding(horizontal = theme.cardPaddingH.dp, vertical = theme.cardPaddingV.dp)
    ) {
        // 头部
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrainIcon(size = 14.dp, color = Color(theme.mutedColor))
            Spacer(Modifier.width(6.dp))
            Text(
                "深度思考", fontSize = 11.sp, color = Color(theme.mutedColor),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(8.dp))
            // 折叠时显示内容预览
            if (!expanded) {
                Text(
                    card.content.take(30).let { if (card.content.length > 30) "$it…" else it },
                    fontSize = 11.sp, color = Color(0xFF94A3B8),
                    fontStyle = FontStyle.Italic, modifier = Modifier.weight(1f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            if (card.streaming) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BrainIcon(size = 12.dp, color = Color(theme.warningColor))
                    Spacer(Modifier.width(4.dp))
                    Text("思考中", fontSize = 10.sp, color = Color(theme.warningColor))
                }
                Spacer(Modifier.width(4.dp))
            }
            // 展开/收起指示
            LucideSvgIcon(
                name = if (expanded) "ic_chevrons_down_up" else "ic_chevrons_up_down",
                size = 12.dp,
                tint = Color(0xFF94A3B8)
            )
        }

        // 展开态内容
        if (expanded) {
            Spacer(Modifier.height(6.dp))
            Text(
                card.content + if (card.streaming) " ▎" else "",
                fontSize = 13.sp, color = Color(0xFF475569),
                fontStyle = FontStyle.Italic, lineHeight = 19.sp
            )
        }
    }
}
