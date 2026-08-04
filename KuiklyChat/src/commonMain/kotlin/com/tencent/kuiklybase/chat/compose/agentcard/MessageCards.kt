package com.tencent.kuiklybase.chat.compose.agentcard

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.layout.Arrangement
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
import com.tencent.kuikly.compose.ui.graphics.Brush
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.chat.agentcard.AgentCard

// ═══════════════════════════════════════════════════════════════════
// Compose 版 MessageCards
//
// 视觉 1:1 对齐 Kuikly DSL 版本（agentcard/MessageCards.kt）
// 包含：TextMessageCard / UserMessageCard / CustomCard
// ═══════════════════════════════════════════════════════════════════

/**
 * Agent 文本消息卡片。
 *
 * - role == "assistant"：左侧渐变头像 + 右侧白色气泡（1:1 对齐 DSL 版）
 * - role == "system"：居中灰色小字，轻量系统通知样式
 */
@Composable
fun TextMessageCard(
    card: AgentCard.TextMessage,
    theme: ComposeCardTheme = ComposeCardTheme.Default,
    modifier: Modifier = Modifier
) {
    // system 消息：居中、灰色小字、无气泡
    if (card.role == "system") {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = theme.cardMarginH.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = card.content,
                fontSize = 11.sp,
                color = Color(theme.mutedColor),
                lineHeight = 16.sp
            )
        }
        return
    }

    // assistant 消息：
    //   - 流式中（streaming=true）：走原生 NativeMarkdownView 实现打字机效果
    //   - 已完成（streaming=false）：走 StreamingMarkdownView（Compose）
    //     理由：已完成消息稳定不变，无需原生 View 的测量/事件回传开销；
    //     Compose 侧可被 LazyColumn 复用，滚动更顺滑。长对话（20+ 卡片）
    //     时能显著降低原生视图数量和主线程事件压力。
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = theme.cardMarginH.dp, vertical = theme.cardMarginV.dp)
            .padding(horizontal = 4.dp)
    ) {
        // 流式/已完成统一走 Compose 渲染：
        // 纯 Compose 布局高度自动计算，不存在原生异步回传导致的重叠问题。
        StreamingMarkdownView(
            content = card.content,
            streaming = false,
            theme = theme,
        )
    }
}

/**
 * 用户消息卡片（右对齐，渐变蓝紫）。
 *
 * 1:1 对齐 DSL 版 [com.tencent.kuiklybase.chat.agentcard.UserMessageCard]。
 */
@Composable
fun UserMessageCard(
    card: AgentCard.UserMessage,
    theme: ComposeCardTheme = ComposeCardTheme.Default,
    modifier: Modifier = Modifier
) {
    val bubbleShape = RoundedCornerShape(
        topStart = 10.dp,
        topEnd = 2.dp,
        bottomStart = 10.dp,
        bottomEnd = 10.dp
    )
    val bubbleGradient = Brush.linearGradient(
        listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = theme.cardMarginH.dp, vertical = theme.cardMarginV.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .clip(bubbleShape)
                .background(bubbleGradient)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = card.content,
                fontSize = 14.sp,
                color = Color.White,
                lineHeight = 20.sp
            )
        }
    }
}

/**
 * 业务自定义卡片（兜底）。
 *
 * 1:1 对齐 DSL 版 [com.tencent.kuiklybase.chat.agentcard.CustomCard]。
 *
 * Specialized renderings (openspec: add-error-classification-and-hints):
 *   - `type == "api_retry"` — large yellow banner with attempt counter +
 *     countdown text. Replaces the legacy 12sp grey status-text so users
 *     actually notice the SDK is retrying after a transient failure.
 */
@Composable
fun CustomCard(
    card: AgentCard.Custom,
    theme: ComposeCardTheme = ComposeCardTheme.Default,
    modifier: Modifier = Modifier
) {
    // Specialized renderer for api_retry — visual contract macapp-chat-ui A4.
    if (card.type == "api_retry") {
        ApiRetryBanner(card, modifier)
        return
    }

    val cardShape = RoundedCornerShape(theme.cardRadius.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = theme.cardMarginH.dp, vertical = theme.cardMarginV.dp)
            .clip(cardShape)
            .background(Color(theme.cardBackground))
            .border(1.dp, Color(theme.borderColor), cardShape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        // 头部行：emoji + type 徽章 + 标题
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                bottom = if (card.payload.isEmpty()) 0.dp else 6.dp
            )
        ) {
            Spacer(Modifier.width(6.dp))
            // type 徽章 — 中文友好化（参考 ToolCallCard 的 toolDisplayLabel）
            val (badgeLabel, badgeBg, badgeFg) = when (card.type) {
                "skill_load" -> {
                    // status: "running" -> "读取skill中"; otherwise -> "skill"
                    val st = card.payload["status"]
                    if (st == "running") Triple("读取skill中", 0xFFEDE9FE, 0xFF6B21A8)
                    else Triple("skill", 0xFFEDE9FE, 0xFF6B21A8)
                }
                else -> Triple(card.type, 0xFFFEF3C7, 0xFF92400E)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(badgeBg))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badgeLabel,
                    fontSize = 10.sp,
                    color = Color(badgeFg),
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.width(8.dp))
            // 标题
            Text(
                text = card.title.ifEmpty { "Custom" },
                fontSize = 13.sp,
                color = Color(theme.titleColor),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }

        // payload 键值对
        // card.payload.forEach { (key, value) ->
        //     Row(modifier = Modifier.padding(vertical = 3.dp)) {
        //         Text(
        //             text = "$key:",
        //             fontSize = 11.sp,
        //             color = Color(theme.mutedColor),
        //             modifier = Modifier.width(80.dp)
        //         )
        //         Text(
        //             text = value,
        //             fontSize = 11.sp,
        //             color = Color(theme.bodyColor),
        //             modifier = Modifier.weight(1f)
        //         )
        //     }
        // }
    }
}

/**
 * Yellow inline banner shown when the SDK is auto-retrying a failed HTTP call.
 *
 * Spec: openspec/changes/add-error-classification-and-hints/specs/macapp-chat-ui/spec.md (A4)
 *
 * Visual contract:
 *   - 2dp amber border + light yellow background, 8dp radius
 *   - 20sp ⏳ icon on the left
 *   - 14sp Bold main line: "服务器繁忙，正在重试 N/M"
 *   - 12sp secondary line: "X 秒后重试..."
 *   - Total height ≥ 56dp so it can't be confused with a thin status line
 */
@Composable
private fun ApiRetryBanner(
    card: AgentCard.Custom,
    modifier: Modifier = Modifier,
) {
    val attempt = card.payload["attempt"]?.toIntOrNull() ?: 0
    val maxRetries = card.payload["maxRetries"]?.toIntOrNull() ?: 0
    val retryDelayMs = card.payload["retryDelayMs"]?.toIntOrNull() ?: 0
    val seconds = retryDelayMs / 1000

    val borderColor = Color(0xFFFBBF24)
    val bgColor = Color(0xFFFEF3C7)
    val textColor = Color(0xFF92400E)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "\u23F3", fontSize = 20.sp, modifier = Modifier.padding(end = 10.dp))
        Column {
            Text(
                text = "服务器繁忙，正在重试 $attempt/$maxRetries",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
            )
            if (seconds > 0) {
                Text(
                    text = "${seconds} 秒后重试...",
                    fontSize = 12.sp,
                    color = textColor,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
