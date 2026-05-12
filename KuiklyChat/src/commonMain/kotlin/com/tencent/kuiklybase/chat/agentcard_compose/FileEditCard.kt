package com.tencent.kuiklybase.chat.agentcard_compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.BorderStroke
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
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
import com.tencent.kuiklybase.chat.agentcard.FileOp

/**
 * Compose 版文件编辑卡片 —— 1:1 对齐 DSL 版
 * [com.tencent.kuiklybase.chat.agentcard.FileEditCard]。
 *
 * 显示：
 * - 文件路径 + 操作类型（创建/更新/删除/重命名）
 * - +N / -N 增删行数徽章
 * - 可选的 diff 文本（行级 +/- 着色）
 *
 * 交互：
 * - 点击头部切换 diff 展开/折叠
 * - diff 不超过 12 行默认展开，超过则默认折叠
 *
 * @param card 文件编辑卡片数据
 * @param theme Compose 版主题配置
 */
@Composable
fun FileEditCard(
    card: AgentCard.FileEdit,
    theme: ComposeCardTheme = ComposeCardTheme.Default
) {
    // 默认折叠 — 与设计参考图（image 3）一致，行头单行展示，需要看 diff 才展开。
    var expanded by remember { mutableStateOf(false) }

    val cardShape = RoundedCornerShape(theme.cardRadius.dp)

    // openspec: add-workflow-phase-progress (visual revamp)
    //   对齐 ToolCallCard 的 4dp 内 padding + 灰白 diff 风格。
    Column(
        modifier = Modifier
            .padding(horizontal = theme.cardMarginH.dp, vertical = 3.dp)
            .clip(cardShape)
    ) {

        // ===== 头部 — 单行 (与 ToolCallCard 视觉一致)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable {
                    if (card.diffText.isNotEmpty()) {
                        expanded = !expanded
                    }
                }
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 文件图标
            LucideSvgIcon(
                name = if (card.operation == FileOp.CREATE) "ic_file_plus_corner" else "ic_file_pen",
                size = 14.dp,
                tint = com.tencent.kuikly.compose.ui.graphics.Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.width(8.dp))

            // 文件名（仅末段；上方 ToolCallCard 已有全路径行）
            Text(
                card.filePath.substringAfterLast('/').ifEmpty { card.filePath },
                fontSize = 13.sp,
                color = Color(0xFF334155),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )

            // +N 增量
            if (card.additions > 0) {
                Text(
                    "+${card.additions}",
                    fontSize = 11.sp,
                    color = Color(theme.successColor),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }

            // -N 删除量
            if (card.deletions > 0) {
                Text(
                    "-${card.deletions}",
                    fontSize = 11.sp,
                    color = Color(theme.errorColor),
                    fontWeight = FontWeight.SemiBold
                )
            }
            // 展开/折叠指示
            if (card.diffText.isNotEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
                LucideSvgIcon(
                    name = if (expanded) "ic_chevrons_down_up" else "ic_chevrons_up_down",
                    size = 12.dp,
                    tint = Color(0xFF94A3B8)
                )
            }
        }

        // ===== Diff 内容（展开态，灰白风格）=====
        if (expanded && card.diffText.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp)
                    .clip(RoundedCornerShape(theme.innerRadius.dp))
                    .background(Color(0xFFFFFFFF))
            ) {
                card.diffText.split("\n").forEach { line ->
                    val (bg, fg) = when {
                        line.startsWith("+") && !line.startsWith("+++") ->
                            Color(0xFFE6FFEC) to Color(0xFF166534)
                        line.startsWith("-") && !line.startsWith("---") ->
                            Color(0xFFFFEBE9) to Color(0xFFB91C1C)
                        line.startsWith("@@") ->
                            Color(0xFFEEF2FF) to Color(0xFF4338CA)
                        else ->
                            Color(0xFFFFFFFF) to Color(0xFF1E293B)
                    }
                    Text(
                        line.ifEmpty { " " },
                        fontSize = 12.sp,
                        color = fg,
                        lineHeight = 17.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bg)
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
