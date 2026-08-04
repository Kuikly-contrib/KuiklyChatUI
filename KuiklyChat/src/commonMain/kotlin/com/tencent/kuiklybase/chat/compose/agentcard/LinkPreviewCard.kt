package com.tencent.kuiklybase.chat.compose.agentcard

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
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
import com.tencent.kuikly.compose.ui.text.style.TextOverflow
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.chat.agentcard.AgentCard

/**
 * Compose 版链接预览卡片。
 *
 * 1:1 对齐 DSL [com.tencent.kuiklybase.chat.agentcard.LinkPreviewCard]。
 */
@Composable
fun ComposeLinkPreviewCard(
    card: AgentCard.LinkPreview,
    theme: ComposeCardTheme = ComposeCardTheme.Default,
    onClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = theme.cardMarginH.dp, vertical = theme.cardMarginV.dp)
            .clip(RoundedCornerShape(theme.cardRadius.dp))
            .border(1.dp, Color(theme.borderColor), RoundedCornerShape(theme.cardRadius.dp))
            .background(Color(theme.cardBackground))
            .padding(horizontal = theme.cardPaddingH.dp, vertical = theme.cardPaddingV.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标
        Box(
            modifier = Modifier.size(44.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center
        ) {
            Text("🔗", fontSize = 20.sp)
        }
        Spacer(Modifier.width(10.dp))
        // 右侧信息
        Column(modifier = Modifier.weight(1f)) {
            if (card.siteName.isNotEmpty()) {
                Text(
                    card.siteName, fontSize = 10.sp, color = Color(theme.mutedColor),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
            Text(
                card.title.ifEmpty { card.url },
                fontSize = 13.sp, color = Color(theme.linkColor),
                fontWeight = FontWeight.SemiBold, maxLines = 2,
                lineHeight = 17.sp, overflow = TextOverflow.Ellipsis
            )
            if (card.description.isNotEmpty()) {
                Text(
                    card.description, fontSize = 11.sp, color = Color(theme.bodyColor),
                    maxLines = 2, lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 3.dp), overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                card.url, fontSize = 10.sp, color = Color(theme.mutedColor),
                maxLines = 1, modifier = Modifier.padding(top = 4.dp),
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
