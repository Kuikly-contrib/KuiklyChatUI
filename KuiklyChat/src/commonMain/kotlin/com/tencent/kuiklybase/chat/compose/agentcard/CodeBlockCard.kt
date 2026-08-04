package com.tencent.kuiklybase.chat.compose.agentcard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
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
import com.tencent.kuikly.compose.ui.text.SpanStyle
import com.tencent.kuikly.compose.ui.text.buildAnnotatedString
import com.tencent.kuikly.compose.ui.text.font.FontFamily
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.style.TextOverflow
import com.tencent.kuikly.compose.ui.text.withStyle
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.chat.agentcard.AgentCard
import com.tencent.kuiklybase.elements.CodeHighlighter
import kotlinx.coroutines.delay

/**
 * Compose DSL 版代码块卡片 —— 1:1 对齐 Kuikly DSL 版
 * [com.tencent.kuiklybase.chat.agentcard.CodeBlockCard]。
 *
 * 特性：
 * - GitHub Dark 风格头部（三色灯 + 文件名 + 行数 + 展开/折叠 + 复制）
 * - 行号显示（深色背景）
 * - 超过 10 行或 [AgentCard.CodeBlock.collapsedByDefault] 为 true 时默认折叠
 * - 复制按钮带 1.5 秒视觉反馈
 * - 代码区**不再封顶高度**（openspec: improve-streaming-text-rendering, M8.2）
 *   外层 LazyColumn + sticky-bottom 接管滚动，代码长度由 collapsedByDefault
 *   折叠机制控制；用户主动点"展开"即视为愿意看完整内容。
 *
 * @param card      代码块数据
 * @param theme     主题（默认 [ComposeCardTheme.Default]）
 * @param maxHeight (deprecated) 已忽略；保留参数以兼容旧调用
 * @param onCopy    点击复制按钮时的回调，参数为代码内容
 */
@Composable
fun CodeBlockCard(
    card: AgentCard.CodeBlock,
    theme: ComposeCardTheme = ComposeCardTheme.Default,
    @Suppress("UNUSED_PARAMETER") maxHeight: Float = 360f,
    onCopy: ((String) -> Unit)? = null
) {
    val lines = card.code.split("\n")
    // 默认展开：让用户看到 AI 实时写入过程
    var expanded by remember { mutableStateOf(true) }
    var copied by remember { mutableStateOf(false) }

    // 写完自动折叠：code 长度停止增长时触发
    var prevCodeLen by remember(card.id) { mutableStateOf(0) }
    val codeLen = card.code.length
    if (codeLen != prevCodeLen) {
        // 还在写入中，保持展开
        prevCodeLen = codeLen
    } else if (codeLen > 0 && prevCodeLen > 0 && expanded) {
        // 长度没变（写完了），自动折叠
        expanded = false
    }

    // 复制状态 1.5s 后自动重置
    if (copied) {
        LaunchedEffect(Unit) {
            delay(1500)
            copied = false
        }
    }

    val hasBody = card.code.isNotEmpty()

    // ===== 外层容器（轻量：与 ToolCallCard 视觉对齐）=====
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = theme.cardMarginH.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(theme.cardRadius.dp))
    ) {
        // ===== 行头：📄 文件名 + 行数 + 展开/收起 + 复制（浅色风格）=====
        Row(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .let { if (hasBody) it.clickable { expanded = !expanded } else it }
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LucideSvgIcon(name = "ic_notebook_text", size = 14.dp, tint = Color(0xFF64748B))
            Spacer(Modifier.width(8.dp))

            // -- 文件名 --
            Text(
                text = card.filename.ifEmpty { card.language.ifEmpty { "code" } },
                fontSize = 13.sp,
                color = Color(0xFF334155),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // -- 行数标签 --
            Text(
                text = "${lines.size} 行",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
            Spacer(Modifier.width(8.dp))

            // -- 展开/折叠箭头 --
            if (hasBody) {
                LucideSvgIcon(
                    name = if (expanded) "ic_chevrons_down_up" else "ic_chevrons_up_down",
                    size = 12.dp,
                    tint = Color(0xFF94A3B8)
                )
            }
        }

        // ===== 代码主体（展开态）=====
        if (expanded && hasBody) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(theme.innerRadius.dp))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(theme.innerRadius.dp))
                    .background(Color(0xFFFFFFFF))
            ) {
                // 复制条
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = card.language.ifEmpty { "" },
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (copied) Color(0xFF22C55E) else Color(0xFFE2E8F0))
                            .clickable {
                                onCopy?.invoke(card.code)
                                copied = true
                            }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (copied) "✓ 已复制" else "复制",
                            fontSize = 11.sp,
                            color = if (copied) Color.White else Color(0xFF475569),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE2E8F0)))

                // 代码主体：直接显示 card.code（后端推多少显示多少，无打字机动画）
                val segments = remember(card.code, card.language) {
                    CodeHighlighter.highlight(
                        code = card.code,
                        language = card.language.ifEmpty { null },
                        darkTheme = false,
                    )
                }
                val hasHighlight = segments.any { it.color != null || it.bold }

                if (hasHighlight) {
                    val annotated = remember(segments) {
                        buildAnnotatedString {
                            segments.forEach { segment ->
                                withStyle(SpanStyle(
                                    color = Color(segment.color ?: 0xFF24292FL),
                                    fontWeight = if (segment.bold) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                )) {
                                    append(segment.text)
                                }
                            }
                        }
                    }
                    Text(
                        text = annotated,
                        lineHeight = 18.sp,
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                } else {
                    Text(
                        text = card.code,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF24292F),
                        lineHeight = 18.sp,
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}
