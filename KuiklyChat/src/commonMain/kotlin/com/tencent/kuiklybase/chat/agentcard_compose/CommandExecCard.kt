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
import com.tencent.kuikly.compose.foundation.layout.padding
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
import com.tencent.kuiklybase.chat.agentcard.ExecState

/**
 * Compose 版命令执行卡片 —— 1:1 对齐 DSL 版
 * [com.tencent.kuiklybase.chat.agentcard.CommandExecCard]。
 *
 * 视觉结构：
 * ```
 * ┌────────────────────────────────────┐
 * │ $ npm run build          ▼  1.4s   │  ← 命令行（可点击切换展开态）
 * │ ┌────────────────────────────────┐ │
 * │ │ total 16                       │ │  ← 输出（展开态）
 * │ │ drwx------+ 68 chen staff ...  │ │
 * │ └────────────────────────────────┘ │
 * │                          ✓ exit 0  │  ← 底部状态
 * └────────────────────────────────────┘
 * ```
 *
 * 交互：
 * - 点击命令行切换输出展开/折叠
 * - `SUCCESS` 且输出超过 6 行时默认折叠，其余默认展开
 *
 * @param card 命令执行卡片数据
 * @param theme Compose 版主题配置
 */
@Composable
fun CommandExecCard(
    card: AgentCard.CommandExec,
    theme: ComposeCardTheme = ComposeCardTheme.Default
) {
    // ---- 展开/折叠状态（仅初始化一次，之后由用户点击控制）----
    var expanded by remember {
        val outputLines = if (card.output.isEmpty()) 0 else card.output.count { it == '\n' } + 1
        mutableStateOf(!(card.state == ExecState.SUCCESS && outputLines > 6))
    }

    val shape = RoundedCornerShape(theme.cardRadius.dp)
    val cb = theme.codeBlock

    // ===== 外层容器（无外框）=====
    Column(
        modifier = Modifier
            .padding(horizontal = theme.cardMarginH.dp, vertical = theme.cardMarginV.dp)
            .clip(shape)
            .background(Color(cb.background))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {

        // ===== 命令行 + 执行状态 + 耗时（可点击切换展开态）=====
        Row(
            modifier = Modifier.clickable {
                if (card.output.isNotEmpty()) {
                    expanded = !expanded
                }
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "$ " 前缀
            Text(
                "$ ",
                fontSize = 13.sp,
                color = Color(theme.successColor),
                fontWeight = FontWeight.Bold
            )

            // 命令文本
            Text(
                card.command,
                fontSize = 13.sp,
                color = Color(theme.bodyColor),
                modifier = Modifier.weight(1f),
                maxLines = 1
            )

            // 执行中：跳动圆点
            if (card.state == ExecState.RUNNING) {
                BouncingDots(
                    color = Color(0xFF8B949E),
                    dotSize = 5f,
                    dotGap = 3f,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            // 成功
            if (
                card.state == ExecState.SUCCESS ||
                (card.exitCode != null && card.exitCode == 0)
            ) {
                LucideSvgIcon(name = "ic_circle_check", size = 14.dp, tint = Color(theme.successColor))
                Text(
                    " success",
                    fontSize = 12.sp,
                    color = Color(theme.successColor),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // 失败
            if (
                card.state == ExecState.FAILED ||
                (card.exitCode != null && card.exitCode != 0)
            ) {
                val exitText = if (card.exitCode != null) " exit ${card.exitCode}" else ""
                LucideSvgIcon(name = "ic_circle_x", size = 14.dp, tint = Color(theme.errorColor))
                Text(
                    exitText,
                    fontSize = 11.sp,
                    color = Color(theme.errorColor),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            // 取消：⊘
            if (card.state == ExecState.CANCELED) {
                Text(
                    "\u2298",
                    fontSize = 12.sp,
                    color = Color(theme.mutedColor),
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            // 耗时
            if (card.durationMs != null) {
                Text(
                    fmtDuration(card.durationMs),
                    fontSize = 11.sp,
                    color = Color(theme.mutedColor),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        // ===== 输出（展开态）=====
        if (expanded && card.output.isNotEmpty()) {
            Text(
                card.output,
                fontSize = 12.sp,
                color = Color(theme.mutedColor),
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

// ============================
// 辅助函数
// ============================

/**
 * 格式化耗时毫秒数为人类可读字符串。
 *
 * - < 1 000 ms → "320ms"
 * - < 60 000 ms → "1.4s"
 * - >= 60 000 ms → "2m 13s"
 */
private fun fmtDuration(ms: Long): String = when {
    ms < 1000 -> "${ms}ms"
    ms < 60_000 -> "${(ms / 100) / 10.0}s"
    else -> "${ms / 60_000}m ${(ms % 60_000) / 1000}s"
}
