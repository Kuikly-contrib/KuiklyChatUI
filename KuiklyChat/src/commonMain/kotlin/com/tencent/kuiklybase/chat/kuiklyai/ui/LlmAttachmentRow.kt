package com.tencent.kuiklybase.chat.kuiklyai.ui

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyRow
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.style.TextOverflow
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmAttachment
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmAttachmentKind

/**
 * 附件横排。
 *
 * 两处复用：输入区的待发送附件（[onRemove] 非空，带删除按钮）、
 * 已发出消息的附件回显（[onRemove] 为空，只读）。
 *
 * 用 [LazyRow] 而不是 `Row` + `horizontalScroll`——Kuikly Compose
 * 不支持 `Modifier.horizontalScroll`，官方建议的平替就是 LazyRow。
 */
@Composable
fun LlmAttachmentRow(
    attachments: List<LlmAttachment>,
    style: LlmChatViewStyle = LlmChatViewStyle.Default,
    strings: LlmChatViewStrings = LlmChatViewStrings.Default,
    onRemove: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (attachments.isEmpty()) return

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(attachments.size) { index ->
            LlmAttachmentChip(
                attachment = attachments[index],
                style = style,
                strings = strings,
                onRemove = onRemove
            )
        }
    }
}

/**
 * 单个附件胶囊：类型图标 + 文件名（超长省略）+ 可选删除按钮。
 */
@Composable
private fun LlmAttachmentChip(
    attachment: LlmAttachment,
    style: LlmChatViewStyle,
    strings: LlmChatViewStrings,
    onRemove: ((String) -> Unit)?
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(Color(style.suggestionBackgroundColor))
            .border(1.dp, Color(style.suggestionBorderColor), shape)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = iconFor(attachment.kind), fontSize = 13.sp)
        Spacer(Modifier.width(5.dp))
        Text(
            text = attachment.name.ifEmpty { strings.emptyAttachmentName },
            fontSize = 12.sp,
            color = Color(style.suggestionTextColor),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(96.dp)
        )
        if (onRemove != null) {
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onRemove(attachment.id) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\u00D7",
                    fontSize = 14.sp,
                    color = Color(style.mutedColor)
                )
            }
        }
    }
}

private fun iconFor(kind: LlmAttachmentKind): String = when (kind) {
    LlmAttachmentKind.IMAGE -> "\uD83D\uDDBC"
    LlmAttachmentKind.AUDIO -> "\uD83C\uDFA7"
    LlmAttachmentKind.FILE -> "\uD83D\uDCC4"
}
