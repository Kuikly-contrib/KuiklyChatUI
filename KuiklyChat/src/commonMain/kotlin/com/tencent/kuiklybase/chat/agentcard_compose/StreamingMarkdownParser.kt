package com.tencent.kuiklybase.chat.agentcard_compose

/**
 * Minimal line-scanning markdown parser tuned for streaming chat content.
 *
 * Spec: openspec/changes/improve-streaming-text-rendering/specs/macapp-chat-ui/spec.md (M3)
 *
 * Design intent:
 *   - Pure function, no allocations beyond the result list. Cheap enough to
 *     re-run on every `content` update at the 10Hz throttle rate.
 *   - Tolerant: any internal exception falls back to a single Paragraph so a
 *     malformed model output never crashes the UI.
 *   - Partial-friendly: an unterminated ``` fence yields `CodeBlock(closed=false)`
 *     so the renderer can still show monospace styling while tokens stream in.
 *
 * Supported syntax (subset):
 *   - Fenced code blocks: ` ```lang\n…\n``` ` (lang optional)
 *   - ATX headings:       `# …` through `###### …`
 *   - Unordered lists:    `- `, `* `, `+ ` at line start (any indent)
 *   - Ordered lists:      `1. ` through `9999. ` at line start
 *   - Block quotes:       `> …` (consecutive lines collapsed)
 *   - Horizontal rules:   a line containing only `---`, `***`, or `___`
 *   - Paragraphs:         consecutive non-blank lines that match nothing else
 *
 * Out of scope (deliberately): tables, task lists, raw HTML, footnotes, math,
 * images. Add those by extending [MdBlock] later.
 *
 * Performance budget: 500 lines ≤ 8 ms (verified by spec N2).
 */
fun parseMarkdown(text: String): List<MdBlock> {
    if (text.isEmpty()) return emptyList()
    return try {
        parseInternal(text)
    } catch (t: Throwable) {
        // Defensive: any parser bug degrades to plain paragraph rendering.
        listOf(MdBlock.Paragraph(text))
    }
}

// ---------------------------------------------------------------------------
// Internals
// ---------------------------------------------------------------------------

private val FENCE_RE = Regex("""^\s*```\s*([A-Za-z0-9_+\-.#]*)\s*$""")
private val HEADING_RE = Regex("""^(#{1,6})\s+(.*)$""")
private val UL_RE = Regex("""^(\s*)([-*+])\s+(.*)$""")
private val OL_RE = Regex("""^(\s*)(\d{1,4}\.)\s+(.*)$""")
private val QUOTE_RE = Regex("""^\s*>\s?(.*)$""")
private val HR_RE = Regex("""^\s*([-*_])\1{2,}\s*$""")
private val TABLE_ROW_RE = Regex("""^\s*\|.*\|\s*$""")
private val TABLE_SEP_RE = Regex("""^\s*\|[\s:]*-{2,}[\s:]*(\|[\s:]*-{2,}[\s:]*)*\|\s*$""")

/** 计算 Markdown 列表缩进层级：每 2 个空格或 1 个 tab 算一级。 */
private fun countIndentLevel(leadingWhitespace: String): Int {
    var level = 0
    var i = 0
    while (i < leadingWhitespace.length) {
        if (leadingWhitespace[i] == '\t') {
            level++
            i++
        } else if (leadingWhitespace[i] == ' ') {
            // 2 spaces = 1 level; 4 spaces = 2 levels, etc.
            level++
            i += 2
        } else {
            i++
        }
    }
    return level
}

/** 解析一行 `| a | b | c |` 为 ["a", "b", "c"] */
private fun parseTableCells(line: String): List<String> {
    val trimmed = line.trim()
    val inner = if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
        trimmed.substring(1, trimmed.length - 1)
    } else trimmed
    return inner.split("|").map { it.trim() }
}

private fun parseInternal(text: String): List<MdBlock> {
    val out = ArrayList<MdBlock>(8)
    val lines = text.split('\n')

    var i = 0
    while (i < lines.size) {
        val line = lines[i]

        // ── Fenced code block (``` … ```) ───────────────────────────────────
        val fence = FENCE_RE.matchEntire(line)
        if (fence != null) {
            val lang = fence.groupValues[1]
            val codeLines = ArrayList<String>(8)
            i++
            var closed = false
            while (i < lines.size) {
                val cur = lines[i]
                if (FENCE_RE.matchEntire(cur) != null) {
                    closed = true
                    i++
                    break
                }
                codeLines.add(cur)
                i++
            }
            out.add(MdBlock.CodeBlock(
                lang = lang,
                code = codeLines.joinToString("\n"),
                closed = closed,
            ))
            continue
        }

        // ── Blank line → flush nothing, just advance ────────────────────────
        if (line.isBlank()) {
            i++
            continue
        }

        // ── Table: header row + separator row + data rows ──────────────────
        // 检测：当前行是 `| ... |` 且下一行是分隔行 `|---|---|`
        if (TABLE_ROW_RE.matches(line) && i + 1 < lines.size && TABLE_SEP_RE.matches(lines[i + 1])) {
            val headers = parseTableCells(line)
            i += 2 // 跳过 header + separator
            val rows = ArrayList<List<String>>()
            while (i < lines.size && TABLE_ROW_RE.matches(lines[i])) {
                rows.add(parseTableCells(lines[i]))
                i++
            }
            out.add(MdBlock.Table(headers, rows))
            continue
        }

        // ── Horizontal rule ─────────────────────────────────────────────────
        if (HR_RE.matchEntire(line) != null) {
            out.add(MdBlock.HorizontalRule)
            i++
            continue
        }

        // ── Heading ─────────────────────────────────────────────────────────
        val heading = HEADING_RE.matchEntire(line)
        if (heading != null) {
            val level = heading.groupValues[1].length
            val content = heading.groupValues[2].trim()
            out.add(MdBlock.Heading(level, content))
            i++
            continue
        }

        // ── List items (collapse consecutive items into separate ListItem
        //    blocks so the renderer can lay them out as a tight list).
        val ul = UL_RE.matchEntire(line)
        if (ul != null) {
            val indent = countIndentLevel(ul.groupValues[1])
            out.add(MdBlock.ListItem(ul.groupValues[2], ul.groupValues[3], indent))
            i++
            continue
        }
        val ol = OL_RE.matchEntire(line)
        if (ol != null) {
            val indent = countIndentLevel(ol.groupValues[1])
            out.add(MdBlock.ListItem(ol.groupValues[2], ol.groupValues[3], indent))
            i++
            continue
        }

        // ── Quote (consecutive `> …` lines join into one Quote block) ──────
        val quote = QUOTE_RE.matchEntire(line)
        if (quote != null) {
            val buf = StringBuilder(quote.groupValues[1])
            i++
            while (i < lines.size) {
                val q = QUOTE_RE.matchEntire(lines[i]) ?: break
                buf.append('\n').append(q.groupValues[1])
                i++
            }
            out.add(MdBlock.Quote(buf.toString()))
            continue
        }

        // ── Paragraph: consecutive non-blank, non-special lines ─────────────
        val pBuf = StringBuilder(line)
        i++
        while (i < lines.size) {
            val cur = lines[i]
            if (cur.isBlank()) break
            if (FENCE_RE.matchEntire(cur) != null) break
            if (HR_RE.matchEntire(cur) != null) break
            if (HEADING_RE.matchEntire(cur) != null) break
            if (UL_RE.matchEntire(cur) != null) break
            if (OL_RE.matchEntire(cur) != null) break
            if (QUOTE_RE.matchEntire(cur) != null) break
            pBuf.append('\n').append(cur)
            i++
        }
        out.add(MdBlock.Paragraph(pBuf.toString()))
    }
    return out
}
