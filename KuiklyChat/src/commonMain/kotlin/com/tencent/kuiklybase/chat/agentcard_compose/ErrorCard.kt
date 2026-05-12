package com.tencent.kuiklybase.chat.agentcard_compose

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
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

/**
 * Action button declaration for [ComposeErrorCard]. Mirrors macApp `ErrorAction`.
 *
 * `action` is an enum string routed by the host ViewModel:
 *   `retry` / `open_settings` / `new_conversation` / `open_docs` /
 *   `copy_span_id` / `contact_admin`
 */
data class ComposeErrorAction(val label: String, val action: String, val value: String? = null)

/**
 * Compose 版错误提示卡片 — 升级版 (openspec: add-error-classification-and-hints).
 *
 * Visual contract (macapp-chat-ui spec A3):
 *   - 2dp colored border + severity-tinted background (warning/error/fatal palette)
 *   - 24sp emoji on the left
 *   - 16sp Bold message line
 *   - 14sp hint line beneath the message
 *   - Optional code chip + actions row (0-3 buttons)
 *
 * Backwards compatibility:
 *   - `severity` / `icon` / `actions` / `onAction` are all optional with sane
 *     defaults so older call sites that only pass `card` + `onRetry` keep working.
 *   - `onRetry` is preserved; if `actions` is empty and `onRetry != null`, a
 *     single "重试" button is synthesised.
 */
@Composable
fun ComposeErrorCard(
    card: AgentCard.Error,
    theme: ComposeCardTheme = ComposeCardTheme.Default,
    onRetry: (() -> Unit)? = null,
    severity: String = "error",
    icon: String = "",
    actions: List<ComposeErrorAction> = emptyList(),
    onAction: ((ComposeErrorAction) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    // Severity palette — warning (amber) / error (red) / fatal (deep red)
    // 视觉减重：1dp 边框（之前 2dp）+ 较浅文字 + 14sp Bold（之前 16sp）
    val (bgColor, borderColor, textColor) = when (severity) {
        "warning" -> Triple(Color(0xFFFEF7E6), Color(0xFFFBBF24), Color(0xFF92400E))
        "fatal"   -> Triple(Color(0xFFFEF2F2), Color(0xFFDC2626), Color(0xFF7F1D1D))
        else      -> Triple(Color(0xFFFEF2F2), Color(0xFFFCA5A5), Color(0xFFB91C1C))
    }

    val effectiveActions = if (actions.isEmpty() && onRetry != null)
        listOf(ComposeErrorAction(label = "重试", action = "retry"))
    else
        actions

    Row(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = theme.cardMarginH.dp, vertical = theme.cardMarginV.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Left icon — 16sp（之前 24sp 太抢戏），仅在非空时显示
        if (icon.isNotEmpty()) {
            Text(text = icon, fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp, top = 1.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            // 1) Headline — 14sp Bold（之前 16sp）
            Text(
                text = card.message,
                fontSize = 14.sp,
                color = textColor,
                fontWeight = FontWeight.Bold,
                lineHeight = 20.sp,
            )

            // 2) Hint / detail — 12sp（之前 14sp）
            if (card.detail.isNotEmpty()) {
                Text(
                    text = card.detail,
                    fontSize = 12.sp,
                    color = textColor,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            // 3) Error code chip — 11sp Medium with severity-tinted background
            if (card.code.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(borderColor.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = card.code,
                        fontSize = 11.sp,
                        color = textColor,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // 4) Action buttons — 0-3 horizontal buttons, 32dp tall
            if (effectiveActions.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    effectiveActions.take(3).forEach { act ->
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(borderColor)
                                .clickable {
                                    // Honour both the new typed callback and the
                                    // legacy onRetry shortcut for action="retry".
                                    onAction?.invoke(act)
                                    if (act.action == "retry") onRetry?.invoke()
                                }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = act.label,
                                fontSize = 13.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}

