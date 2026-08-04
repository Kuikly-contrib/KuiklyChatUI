package com.tencent.kuiklybase.chat.legacy.ai

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
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

/**
 * 空会话时的欢迎区：标题 + 副标题 + 建议提示。
 *
 * 对齐 Flutter AI Toolkit 的 welcome message 与 suggestions。
 * 空列表直接留白会让人不知道能问什么，建议提示是最省事的引导方式。
 *
 * @param suggestions 建议提示文案，点击后直接作为 prompt 发送
 */
@Composable
internal fun LlmWelcomeSection(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    style: LlmChatViewStyle = LlmChatViewStyle.Default,
    strings: LlmChatViewStrings = LlmChatViewStrings.Default,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = strings.welcomeTitle,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(style.titleColor)
        )

        if (strings.welcomeSubtitle.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = strings.welcomeSubtitle,
                fontSize = 13.sp,
                color = Color(style.mutedColor)
            )
        }

        if (suggestions.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            // 竖排而非流式布局：建议提示通常是整句话，横排会被挤成两三个字换行
            suggestions.forEach { suggestion ->
                LlmSuggestionChip(
                    text = suggestion,
                    style = style,
                    onClick = { onSuggestionClick(suggestion) }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun LlmSuggestionChip(
    text: String,
    style: LlmChatViewStyle,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(style.suggestionBackgroundColor))
            .border(1.dp, Color(style.suggestionBorderColor), shape)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = Color(style.suggestionTextColor)
        )
    }
}
