package com.tencent.kuiklybase.chat.kuiklyai.ui

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
import com.tencent.kuikly.compose.ui.graphics.Brush
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.chat.agentcard_compose.BouncingDots
import com.tencent.kuiklybase.chat.agentcard_compose.StreamingMarkdownView
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmChatMessage
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmMessageState

/**
 * 用户消息气泡：右对齐渐变气泡，附件在气泡上方成排展示。
 *
 * 视觉与 `agentcard_compose/UserMessageCard` 保持一致，
 * 避免同一个 App 里两套 AI 界面长得不一样。
 */
@Composable
fun LlmUserBubble(
    message: LlmChatMessage,
    style: LlmChatViewStyle = LlmChatViewStyle.Default,
    strings: LlmChatViewStrings = LlmChatViewStrings.Default,
    onEditClick: ((LlmChatMessage) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bubbleShape = RoundedCornerShape(
        topStart = style.bubbleRadius.dp,
        topEnd = 2.dp,
        bottomStart = style.bubbleRadius.dp,
        bottomEnd = style.bubbleRadius.dp
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = style.contentPaddingH.dp,
                vertical = (style.messageSpacing / 2).dp
            ),
        horizontalAlignment = Alignment.End
    ) {
        if (message.attachments.isNotEmpty()) {
            LlmAttachmentRow(
                attachments = message.attachments,
                style = style,
                strings = strings,
                onRemove = null
            )
            Spacer(Modifier.height(4.dp))
        }

        if (message.text.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.End) {
                Box(
                    modifier = Modifier
                        .clip(bubbleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(style.userBubbleGradientStart),
                                    Color(style.userBubbleGradientEnd)
                                )
                            )
                        )
                        .then(
                            if (onEditClick != null) {
                                Modifier.clickable { onEditClick(message) }
                            } else {
                                Modifier
                            }
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = message.text,
                        fontSize = style.fontSize.sp,
                        color = Color(style.userTextColor),
                        lineHeight = style.lineHeight.sp
                    )
                }
            }
        }
    }
}

/**
 * 模型回复气泡。
 *
 * 正文走 `agentcard_compose/StreamingMarkdownView` —— 该组件已处理
 * 段落、标题、列表、引用、代码块、表格，并且经过流式渲染的性能打磨。
 * 这里不重写 Markdown，只负责气泡外壳与状态态（思考中 / 失败 / 已取消）。
 */
@Composable
fun LlmResponseBubble(
    message: LlmChatMessage,
    style: LlmChatViewStyle = LlmChatViewStyle.Default,
    strings: LlmChatViewStrings = LlmChatViewStrings.Default,
    onRetryClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = style.contentPaddingH.dp,
                vertical = (style.messageSpacing / 2).dp
            )
    ) {
        // 流式刚开始、还没有任何 token 时显示"正在思考"，
        // 而不是一个空气泡——空气泡会让人以为界面卡死了。
        if (message.text.isEmpty() && message.state == LlmMessageState.STREAMING) {
            LlmThinkingIndicator(style = style, strings = strings)
            return@Column
        }

        if (message.text.isNotEmpty()) {
            StreamingMarkdownView(
                content = message.text,
                streaming = message.state == LlmMessageState.STREAMING,
                theme = style.cardTheme.copy(bodyColor = style.llmTextColor)
            )
        }

        when (message.state) {
            LlmMessageState.FAILED -> LlmMessageErrorRow(
                text = message.error.ifEmpty { strings.retryLabel },
                style = style,
                strings = strings,
                onRetryClick = onRetryClick
            )
            LlmMessageState.CANCELED -> Text(
                text = strings.canceledLabel,
                fontSize = 11.sp,
                color = Color(style.mutedColor),
                modifier = Modifier.padding(top = 4.dp)
            )
            else -> Unit
        }
    }
}

/**
 * "正在思考"指示器：跳动圆点 + 文字。复用 `agentcard_compose/BouncingDots`。
 */
@Composable
private fun LlmThinkingIndicator(
    style: LlmChatViewStyle,
    strings: LlmChatViewStrings
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BouncingDots(color = Color(style.mutedColor))
        Spacer(Modifier.width(8.dp))
        Text(
            text = strings.thinkingLabel,
            fontSize = 12.sp,
            color = Color(style.mutedColor)
        )
    }
}

/**
 * 失败行：错误文案 + 可选重试按钮。
 *
     * 重试按钮只在 [onRetryClick] 非空时出现——由消息携带的
     * [com.tencent.kuiklybase.chat.kuiklyai.provider.LlmErrorCode.retryable] 决定，
 * 鉴权失败这类重试无用的错误不给按钮。
 */
@Composable
private fun LlmMessageErrorRow(
    text: String,
    style: LlmChatViewStyle,
    strings: LlmChatViewStrings,
    onRetryClick: (() -> Unit)?
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .padding(top = 6.dp)
            .clip(shape)
            .background(Color(style.errorBackgroundColor))
            .border(1.dp, Color(style.errorColor), shape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = Color(style.errorColor)
        )
        if (onRetryClick != null) {
            Spacer(Modifier.width(10.dp))
            Text(
                text = strings.retryLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(style.errorColor),
                modifier = Modifier.clickable { onRetryClick() }
            )
        }
    }
}
