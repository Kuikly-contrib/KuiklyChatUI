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
import com.tencent.kuiklybase.chat.agentcard.SearchItem

/**
 * Compose 版搜索结果卡片。
 *
 * 1:1 对齐 DSL [com.tencent.kuiklybase.chat.agentcard.SearchResultCard]。
 */
@Composable
fun ComposeSearchResultCard(
    card: AgentCard.SearchResult,
    theme: ComposeCardTheme = ComposeCardTheme.Default,
    onItemClick: ((SearchItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = theme.cardMarginH.dp, vertical = theme.cardMarginV.dp)
            .clip(RoundedCornerShape(theme.cardRadius.dp))
            .border(1.dp, Color(theme.borderColor), RoundedCornerShape(theme.cardRadius.dp))
            .background(Color(theme.cardBackground))
    ) {
        // 头部
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(Color(0xFFEFF6FF))
                .padding(horizontal = theme.cardPaddingH.dp, vertical = theme.cardPaddingV.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 2.dp)
            ) {
                Text("🔍", fontSize = 13.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    "搜索", fontSize = 11.sp, color = Color(0xFF1D4ED8),
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)
                )
                Text(
                    "${card.totalCount} 条结果", fontSize = 11.sp,
                    color = Color(theme.mutedColor)
                )
            }
            Text(
                "「${card.query}」", fontSize = 13.sp, color = Color(theme.titleColor),
                fontWeight = FontWeight.Medium
            )
        }

        // 结果列表
        if (card.items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无搜索结果", fontSize = 12.sp, color = Color(theme.mutedColor))
            }
        } else {
            card.items.forEachIndexed { index, item ->
                if (index > 0) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF1F5F9)))
                }
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                        Text("${index + 1}.", fontSize = 11.sp, color = Color(theme.mutedColor))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            item.title, fontSize = 13.sp, color = Color(theme.linkColor),
                            fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        if (item.score > 0f) {
                            Text(
                                "⭐ ${formatScore(item.score)}",
                                fontSize = 10.sp, color = Color(theme.warningColor)
                            )
                        }
                    }
                    if (item.snippet.isNotEmpty()) {
                        Text(
                            item.snippet, fontSize = 12.sp, color = Color(theme.bodyColor),
                            lineHeight = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (item.url.isNotEmpty()) {
                        Text(
                            item.url, fontSize = 10.sp, color = Color(theme.mutedColor),
                            maxLines = 1, modifier = Modifier.padding(top = 3.dp),
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private fun formatScore(score: Float): String {
    val v = (score * 10).toInt() / 10.0
    return v.toString()
}
