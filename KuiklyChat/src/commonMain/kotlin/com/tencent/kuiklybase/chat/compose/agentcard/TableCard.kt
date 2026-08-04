package com.tencent.kuiklybase.chat.compose.agentcard

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

/**
 * Compose 版表格卡片。
 *
 * 1:1 对齐 DSL [com.tencent.kuiklybase.chat.agentcard.TableCard]。
 *
 * 支持标题、表头深色、数据行交替底色。
 * 注意：DSL 版用 Scroller 支持横向滚动，Compose 版用 weight 均分列宽。
 */
@Composable
fun ComposeTableCard(
    card: AgentCard.Table,
    theme: ComposeCardTheme = ComposeCardTheme.Default,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = theme.cardMarginH.dp, vertical = theme.cardMarginV.dp)
            .clip(RoundedCornerShape(theme.cardRadius.dp))
            .border(1.dp, Color(theme.borderColor), RoundedCornerShape(theme.cardRadius.dp))
            .background(Color(theme.cardBackground))
    ) {
        // 标题
        if (card.title.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📊", fontSize = 13.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    card.title, fontSize = 13.sp, color = Color(theme.titleColor),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 表头
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(Color(0xFFF1F5F9))
                .padding(vertical = 8.dp)
        ) {
            card.headers.forEach { header ->
                Text(
                    header, fontSize = 12.sp, color = Color(theme.titleColor),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
                )
            }
        }

        // 数据行
        card.rows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(
                        if (rowIndex % 2 == 0) Color(theme.cardBackground)
                        else Color(0xFFFAFBFC)
                    )
                    .padding(vertical = 8.dp)
            ) {
                row.forEach { cell ->
                    Text(
                        cell, fontSize = 12.sp, color = Color(theme.bodyColor),
                        lineHeight = 17.sp,
                        modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
                    )
                }
            }
        }
    }
}
