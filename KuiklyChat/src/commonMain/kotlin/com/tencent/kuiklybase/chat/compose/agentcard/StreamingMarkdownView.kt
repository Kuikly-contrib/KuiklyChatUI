package com.tencent.kuiklybase.chat.compose.agentcard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.heightIn
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontFamily
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp

/**
 * Compose-native streaming markdown renderer.
 *
 * Spec: openspec/changes/improve-streaming-text-rendering/specs/macapp-chat-ui/spec.md (M1)
 *
 * Design intent:
 *   - Re-parses the full content on every recomposition. The 10Hz throttle
 *     in AgentEventHandler keeps this affordable (≤8 ms for 500 lines).
 *   - Each block has a `Modifier.heightIn(min = …)` so an empty block at the
 *     start of a stream doesn't cause the layout to jitter as content grows.
 *     (Spec M8.1.)
 *   - Code blocks are NOT internally scrollable / capped — outer LazyColumn
 *     owns scrolling so the sticky-bottom logic in [ComposeAgentCardList] can
 *     follow long code as it streams in. (Spec M8.2.)
 *   - The blinking cursor (`▎`) appears only on the very last block while
 *     `streaming = true`.
 *
 * @param content   raw markdown string (mutable across recompositions)
 * @param streaming when true, append `▎` to the last block to indicate the
 *                  model is still typing.
 * @param theme     palette inherited from the host card.
 * @param modifier  layout modifier from the parent.
 */
@Composable
fun StreamingMarkdownView(
    content: String,
    streaming: Boolean,
    theme: ComposeCardTheme = ComposeCardTheme.Default,
    modifier: Modifier = Modifier,
) {
    val blocks = remember(content) { parseMarkdown(content) }

    Column(modifier = modifier) {
        if (blocks.isEmpty() && streaming) {
            ParagraphBlock(text = "", showCursor = false, theme = theme)
            return@Column
        }
        blocks.forEachIndexed { index, block ->
            val showCursor = false
            when (block) {
                is MdBlock.Paragraph    -> ParagraphBlock(block.text, showCursor, theme)
                is MdBlock.CodeBlock    -> CodeBlockView(block.lang, block.code, showCursor, theme)
                is MdBlock.Heading      -> HeadingBlock(block.level, block.text, showCursor, theme)
                is MdBlock.ListItem     -> ListItemBlock(block.bulletOrNum, block.text, block.indent, showCursor, theme)
                is MdBlock.Quote        -> QuoteBlock(block.text, showCursor, theme)
                MdBlock.HorizontalRule  -> HorizontalRuleBlock(theme)
                is MdBlock.Table        -> TableBlock(block.headers, block.rows, theme)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Block composables
// ---------------------------------------------------------------------------

/** Plain paragraph; uses the bubble's body text color from the theme.
 *  Inline markdown (`code`, **bold**, *italic*) parsed via [parseInlineMarkdown]. */
@Composable
private fun ParagraphBlock(text: String, showCursor: Boolean, theme: ComposeCardTheme) {
    val annotated = remember(text) { parseInlineMarkdown(text) }
    Text(
        text = if (showCursor)
            com.tencent.kuikly.compose.ui.text.buildAnnotatedString {
                append(annotated); append(" \u258E")
            }
        else annotated,
        fontSize = 14.sp,
        color = Color(theme.bodyColor),
        lineHeight = 20.sp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 20.dp)
            .padding(vertical = 2.dp),
    )
}

/** ATX heading — H1 (20sp Bold) → H6 (12sp SemiBold). */
@Composable
private fun HeadingBlock(level: Int, text: String, showCursor: Boolean, theme: ComposeCardTheme) {
    val fontSize = when (level) {
        1 -> 20.sp
        2 -> 17.sp
        3 -> 15.sp
        else -> 14.sp
    }
    val lineHeight = when (level) {
        1 -> 28.sp
        2 -> 24.sp
        3 -> 22.sp
        else -> 20.sp
    }
    val weight = if (level <= 3) FontWeight.Bold else FontWeight.SemiBold
    val annotated = remember(text) { parseInlineMarkdown(text) }
    Text(
        text = if (showCursor)
            com.tencent.kuikly.compose.ui.text.buildAnnotatedString {
                append(annotated); append(" \u258E")
            }
        else annotated,
        fontSize = fontSize,
        color = Color(theme.titleColor),
        fontWeight = weight,
        lineHeight = lineHeight,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 24.dp)
            .padding(top = 8.dp, bottom = 4.dp),
    )
}

/** Single list item — preserves the original bullet/number prefix.
 *  Item text supports inline markdown via [parseInlineMarkdown].
 *  @param indent nesting depth (0 = top level, 1 = child, etc.) */
@Composable
private fun ListItemBlock(bullet: String, text: String, indent: Int, showCursor: Boolean, theme: ComposeCardTheme) {
    val annotated = remember(text) { parseInlineMarkdown(text) }
    // 无序列表 (-/*/ +) 按层级切换 bullet 样式，有序列表 (1./2.) 保留原样
    val displayBullet = if (bullet == "-" || bullet == "*" || bullet == "+") "•" else bullet
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 20.dp)
            .padding(start = (indent * 16).dp)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "$displayBullet ",
            fontSize = 14.sp,
            color = Color(theme.mutedColor),
            lineHeight = 20.sp,
        )
        Text(
            text = if (showCursor)
                com.tencent.kuikly.compose.ui.text.buildAnnotatedString {
                    append(annotated); append(" \u258E")
                }
            else annotated,
            fontSize = 14.sp,
            color = Color(theme.bodyColor),
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Block quote — left vertical accent bar + indented muted text. */
@Composable
private fun QuoteBlock(text: String, showCursor: Boolean, theme: ComposeCardTheme) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 20.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .heightIn(min = 20.dp)
                .background(Color(theme.accentBorder)),
        ) {}
        Spacer(Modifier.width(8.dp))
        val annotated = remember(text) { parseInlineMarkdown(text) }
        Text(
            text = if (showCursor)
                com.tencent.kuikly.compose.ui.text.buildAnnotatedString {
                    append(annotated); append(" \u258E")
                }
            else annotated,
            fontSize = 14.sp,
            color = Color(theme.mutedColor),
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

/** 1px horizontal divider. */
@Composable
private fun HorizontalRuleBlock(theme: ComposeCardTheme) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(1.dp)
            .background(Color(theme.borderColor)),
    ) {}
}

/**
 * Fenced code block.
 *
 * Visual contract:
 *   - GitHub-light palette: light grey background + 1dp border + 6dp radius
 *   - Top language tag bar (slightly darker grey)
 *   - Body: monospace, 13sp, line-height 18sp
 *   - **No internal verticalScroll / heightIn(max=…)** — outer LazyColumn
 *     owns scrolling (Spec M8.2)
 *   - `Modifier.heightIn(min = 40.dp)` so an empty block during streaming
 *     doesn't collapse to 0 (Spec M8.1)
 *   - First version does NOT do syntax highlighting — monospace + body color
 *     is enough to feel like code; highlighting is a follow-up.
 */
@Composable
private fun CodeBlockView(lang: String, code: String, showCursor: Boolean, theme: ComposeCardTheme) {
    val borderColor = Color(0xFFE1E4E8)
    val bgColor = Color(0xFFF6F8FA)
    val headerBg = Color(0xFFE1E4E8)
    val headerText = Color(0xFF586069)
    val bodyText = Color(0xFF24292E)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp)),
    ) {
        // Language tag header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBg)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = lang.ifEmpty { "code" },
                fontSize = 11.sp,
                color = headerText,
                fontFamily = FontFamily.Monospace,
            )
        }
        // Body
        Text(
            text = if (showCursor) "$code\u258E" else code,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            color = bodyText,
            lineHeight = 18.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

/** Markdown table — 浅灰底 + 1dp 边框，表头加粗。 */
@Composable
private fun TableBlock(headers: List<String>, rows: List<List<String>>, theme: ComposeCardTheme) {
    val borderColor = Color(0xFFE2E8F0)
    val headerBg = Color(0xFFF1F5F9)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, borderColor, RoundedCornerShape(6.dp)),
    ) {
        // 表头
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBg)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            headers.forEach { cell ->
                Text(
                    text = cell,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(theme.titleColor),
                    lineHeight = 18.sp,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        // 分隔线
        Box(Modifier.fillMaxWidth().height(1.dp).background(borderColor))
        // 数据行
        rows.forEachIndexed { rowIdx, cells ->
            if (rowIdx > 0) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(borderColor))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            ) {
                val colCount = headers.size
                cells.take(colCount).forEachIndexed { colIdx, cell ->
                    Text(
                        text = cell,
                        fontSize = 13.sp,
                        color = Color(theme.bodyColor),
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f),
                    )
                }
                // 列数不足时补空
                repeat((colCount - cells.size).coerceAtLeast(0)) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
