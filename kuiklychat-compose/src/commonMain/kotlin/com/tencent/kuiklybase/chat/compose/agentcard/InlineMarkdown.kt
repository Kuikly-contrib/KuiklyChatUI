package com.tencent.kuiklybase.chat.compose.agentcard

import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.AnnotatedString
import com.tencent.kuikly.compose.ui.text.SpanStyle
import com.tencent.kuikly.compose.ui.text.buildAnnotatedString
import com.tencent.kuikly.compose.ui.text.font.FontFamily
import com.tencent.kuikly.compose.ui.text.font.FontStyle
import com.tencent.kuikly.compose.ui.text.font.FontWeight

/**
 * 极简行内 markdown 解析器 —— 把段落 / 列表项 / 标题等 block 内部的字符串
 * 拆成 [AnnotatedString]，支持以下记号：
 *
 *   `code`        → 浅灰底 + 等宽字体
 *   **bold**      → FontWeight.Bold
 *   *italic*      → FontStyle.Italic
 *   _italic_      → FontStyle.Italic
 *
 * 设计原则：
 * - 单遍线性扫描，O(n)；不引入第三方依赖
 * - 无法闭合的标记（如未配对的 `*`）按原样输出，避免吞字符
 * - 转义：`\*`、`\``、`\_`、`\\` 仅按原字符输出
 *
 * 解析层级：先 backtick code（最高优先级，内部不再继续解析），
 * 再 bold（`**` / `__`），最后 italic（单 `*` / `_`）。
 */
public fun parseInlineMarkdown(text: String): AnnotatedString {
    if (text.isEmpty()) return AnnotatedString("")
    return try {
        buildAnnotatedString {
            val n = text.length
            var i = 0
            while (i < n) {
                val c = text[i]

                // ── 转义 ─────────────────────────────────────────────────────
                if (c == '\\' && i + 1 < n) {
                    val nx = text[i + 1]
                    if (nx == '*' || nx == '_' || nx == '`' || nx == '\\') {
                        append(nx)
                        i += 2
                        continue
                    }
                }

                // ── `code` ──────────────────────────────────────────────────
                if (c == '`') {
                    val end = text.indexOf('`', i + 1)
                    if (end > i + 1) {
                        val code = text.substring(i + 1, end)
                        pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = Color(0xFFF1F5F9), // slate-100
                                color = Color(0xFFB91C1C),       // red-700，与背景区分
                            )
                        )
                        append(code)
                        pop()
                        i = end + 1
                        continue
                    }
                    // 无配对 → 当普通字符
                    append(c); i++; continue
                }

                // ── **bold** / __bold__ ──────────────────────────────────────
                if ((c == '*' || c == '_') && i + 1 < n && text[i + 1] == c) {
                    val marker = "$c$c"
                    val end = text.indexOf(marker, i + 2)
                    if (end > i + 2) {
                        val inner = text.substring(i + 2, end)
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        // 内部仍允许 `code` / italic — 递归一次
                        append(parseInlineMarkdown(inner))
                        pop()
                        i = end + 2
                        continue
                    }
                }

                // ── *italic* / _italic_ ──────────────────────────────────────
                if (c == '*' || c == '_') {
                    // 简单启发式：要求 marker 紧跟非空白字符且能在剩余文本里找到对应单字符闭合
                    if (i + 1 < n && !text[i + 1].isWhitespace()) {
                        val end = findMatchingItalic(text, i + 1, c)
                        if (end > i + 1) {
                            val inner = text.substring(i + 1, end)
                            pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                            append(parseInlineMarkdown(inner))
                            pop()
                            i = end + 1
                            continue
                        }
                    }
                }

                append(c)
                i++
            }
        }
    } catch (_: Throwable) {
        // 防御：解析任何内部错误降级为纯文本，避免 UI 崩
        AnnotatedString(text)
    }
}

/** 查找 italic 闭合位置：跳过双字符（双星号 / 双下划线）和 backtick 内容。 */
private fun findMatchingItalic(text: String, from: Int, marker: Char): Int {
    var j = from
    val n = text.length
    while (j < n) {
        val ch = text[j]
        if (ch == '\\' && j + 1 < n) { j += 2; continue }
        if (ch == '`') {
            val close = text.indexOf('`', j + 1)
            j = if (close < 0) n else close + 1
            continue
        }
        // 跳过 ** 双标记
        if (ch == marker && j + 1 < n && text[j + 1] == marker) { j += 2; continue }
        if (ch == marker) return j
        j++
    }
    return -1
}
