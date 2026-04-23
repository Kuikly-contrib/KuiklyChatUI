package com.tencent.kuiklybase.chat.agui

import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuiklybase.chat.model.ChatMessage
import com.tencent.kuiklybase.chat.model.MessageStatus
import com.tencent.kuiklybase.chat.model.MessageType

/**
 * Markdown 混合内容拆分器。
 *
 * AI 通常返回一条长 Markdown 文本，里面混合了：
 * 普通段落、代码块、表格、引用块、折叠内容、diff、任务列表等。
 *
 * `KuiklyMarkdown` 库对表格、引用块、折叠等高级语法支持有限，
 * 通过此拆分器将混合文本**按类型切分**为多条 [ChatMessage]，
 * 每条消息携带合适的 `agUiKind`，从而可以用专门的 [MessageRendererFactory] 渲染。
 *
 * 拆分顺序（保证原文顺序）：
 *  1. 代码块 / diff 块
 *  2. 表格
 *  3. 折叠内容（details/summary）
 *  4. 引用块
 *  5. 任务列表（checklist）
 *  6. 普通文本（剩余部分）
 *
 * 用法：
 * ```kotlin
 * val blocks = MarkdownBlockExtractor.extract(
 *     markdown = aiResponseText,
 *     baseId = msgId,
 *     senderName = session.aiSenderName,
 *     senderAvatar = session.aiAvatarUrl
 * )
 * blocks.forEach { session.messages.add(it) }
 * ```
 */
object MarkdownBlockExtractor {

    /**
     * 将单条 Markdown 文本拆分为多条类型化的 [ChatMessage]。
     *
     * @param markdown 原始 Markdown 文本
     * @param baseId 基础消息 ID（拆分出的消息会在此基础上加后缀）
 * @param senderName 发送者名称
 * @param senderAvatar 发送者头像 URL
 * @param timestamp 时间戳
 * @return 按原文顺序排列的消息列表
 */
fun extract(
    markdown: String,
    baseId: String,
    senderName: String = "",
    senderAvatar: String = "",
    timestamp: Long = 0L
): List<ChatMessage> {
    val blocks = parseBlocks(markdown)
    return blocks.mapIndexed { index, block ->
        block.toMessage(
            id = "${baseId}_$index",
            senderName = senderName,
            senderAvatar = senderAvatar,
            timestamp = timestamp
        )
    }
}

/**
 * 将 Markdown 文本解析为内部块列表。
 */
internal fun parseBlocks(text: String): List<MarkdownBlock> {
    val lines = text.lines()
    val result = mutableListOf<MarkdownBlock>()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        // 跳过空行（但保留段落间的空行语义）
        if (line.isBlank() && i < lines.size - 1) {
            i++
            continue
        }

        // ---- 1. 代码块 / diff 块 ----
        if (line.trimStart().startsWith("```")) {
            val (block, nextIdx) = parseCodeBlock(lines, i)
            result.add(block)
            i = nextIdx
            continue
        }

        // ---- 2. 表格 ----
        if (line.trimStart().startsWith("|") && line.contains("|", ignoreCase = false)) {
            val (block, nextIdx) = parseTable(lines, i)
            result.add(block)
            i = nextIdx
            continue
        }

        // ---- 3. 折叠内容 <details> ----
        if (line.trimStart().startsWith("<details", ignoreCase = true)) {
            val (block, nextIdx) = parseCollapsible(lines, i)
            result.add(block)
            i = nextIdx
            continue
        }

        // ---- 4. 引用块 ----
        if (line.trimStart().startsWith(">")) {
            val (block, nextIdx) = parseQuote(lines, i)
            result.add(block)
            i = nextIdx
            continue
        }

        // ---- 5. 任务列表 ----
        val trimmed = line.trimStart()
        if ((trimmed.startsWith("- [ ]") || trimmed.startsWith("- [x]") || trimmed.startsWith("- [X]")) &&
            isChecklistRegion(lines, i)
        ) {
            val (block, nextIdx) = parseChecklist(lines, i)
            result.add(block)
            i = nextIdx
            continue
        }

        // ---- 6. 普通文本段落 ----
        val (block, nextIdx) = parseText(lines, i)
        if (block.content.isNotBlank()) {
            result.add(block)
        }
        i = nextIdx
    }

    return result
}

// ============================
// 各类型解析器
// ============================

private fun parseCodeBlock(lines: List<String>, start: Int): Pair<MarkdownBlock, Int> {
    val opener = lines[start].trimStart()
    val lang = opener.removePrefix("```").trim()
    val buffer = StringBuilder()
    var i = start + 1
    while (i < lines.size) {
        if (lines[i].trimStart() == "```") {
            i++
            break
        }
        if (buffer.isNotEmpty()) buffer.append("\n")
        buffer.append(lines[i])
        i++
    }
    val code = buffer.toString()
    val kind = if (lang == "diff" || looksLikeDiff(code)) MarkdownBlockKind.DIFF else MarkdownBlockKind.CODE_BLOCK
    return MarkdownBlock(kind, code, language = lang) to i
}

private fun looksLikeDiff(code: String): Boolean {
    val lines = code.lines()
    return lines.any { it.startsWith("+") || it.startsWith("-") || it.startsWith("@@") }
}

private fun parseTable(lines: List<String>, start: Int): Pair<MarkdownBlock, Int> {
    val rows = mutableListOf<String>()
    var i = start
    while (i < lines.size) {
        val l = lines[i].trimStart()
        if (!l.startsWith("|")) break
        // 跳过分隔行 |---|---|
        if (l.matches(Regex("^\\|[-:]+\\|.*"))) {
            i++
            continue
        }
        rows.add(l)
        i++
    }
    // 解析为 headers + rows
    val headers = if (rows.isNotEmpty()) parseTableRow(rows.first()) else emptyList()
    val dataRows = rows.drop(1).map { parseTableRow(it) }
    return MarkdownBlock(
        kind = MarkdownBlockKind.TABLE,
        content = rows.joinToString("\n"),
        tableHeaders = headers,
        tableRows = dataRows
    ) to i
}

private fun parseTableRow(row: String): List<String> {
    return row.trimStart('|').trimEnd('|').split("|").map { it.trim() }
}

private fun parseCollapsible(lines: List<String>, start: Int): Pair<MarkdownBlock, Int> {
    var i = start
    var summary = ""
    val buffer = StringBuilder()
    var inside = false
    while (i < lines.size) {
        val l = lines[i]
        if (l.trimStart().startsWith("<details", ignoreCase = true)) {
            // 提取 summary 属性（如果写在开始标签里）
            val sumMatch = Regex("<summary>(.*?)</summary>", RegexOption.IGNORE_CASE).find(l)
            if (sumMatch != null) summary = sumMatch.groupValues[1].trim()
            inside = true
            i++
            continue
        }
        if (l.trimStart().startsWith("</details>", ignoreCase = true)) {
            i++
            break
        }
        if (inside) {
            // 提取 <summary> 标签
            val sumMatch = Regex("<summary>(.*?)</summary>", RegexOption.IGNORE_CASE).find(l)
            if (sumMatch != null) {
                summary = sumMatch.groupValues[1].trim()
            } else {
                if (buffer.isNotEmpty()) buffer.append("\n")
                buffer.append(l)
            }
        }
        i++
    }
    return MarkdownBlock(
        kind = MarkdownBlockKind.COLLAPSIBLE,
        content = buffer.toString(),
        summary = summary
    ) to i
}

private fun parseQuote(lines: List<String>, start: Int): Pair<MarkdownBlock, Int> {
    val buffer = StringBuilder()
    var i = start
    while (i < lines.size) {
        val l = lines[i]
        if (!l.trimStart().startsWith(">")) break
        if (buffer.isNotEmpty()) buffer.append("\n")
        buffer.append(l.trimStart().removePrefix(">").trimStart())
        i++
    }
    return MarkdownBlock(kind = MarkdownBlockKind.QUOTE, content = buffer.toString()) to i
}

private fun isChecklistRegion(lines: List<String>, start: Int): Boolean {
    // 往后看几行，如果连续出现 checklist 项，则认为是 checklist 区域
    var count = 0
    for (j in start until minOf(start + 5, lines.size)) {
        val t = lines[j].trimStart()
        if (t.startsWith("- [ ]") || t.startsWith("- [x]") || t.startsWith("- [X]")) count++
    }
    return count >= 2
}

private fun parseChecklist(lines: List<String>, start: Int): Pair<MarkdownBlock, Int> {
    val items = mutableListOf<Pair<String, Boolean>>()
    var i = start
    while (i < lines.size) {
        val t = lines[i].trimStart()
        if (t.startsWith("- [ ]")) {
            items.add(t.removePrefix("- [ ]").trim() to false)
        } else if (t.startsWith("- [x]")) {
            items.add(t.removePrefix("- [x]").trim() to true)
        } else if (t.startsWith("- [X]")) {
            items.add(t.removePrefix("- [X]").trim() to true)
        } else if (t.startsWith("- [") && t.contains("]")) {
            val checked = t.substringAfter("[").substringBefore("]") != " "
            items.add(t.substringAfter("]").trim() to checked)
        } else {
            break
        }
        i++
    }
    return MarkdownBlock(
        kind = MarkdownBlockKind.CHECKLIST,
        content = items.joinToString("\n") { (text, checked) -> "${if (checked) "[x]" else "[ ]"} $text" },
        checklistItems = items
    ) to i
}

private fun parseText(lines: List<String>, start: Int): Pair<MarkdownBlock, Int> {
    val buffer = StringBuilder()
    var i = start
    while (i < lines.size) {
        val l = lines[i]
        // 遇到其他块类型就停止
        if (l.trimStart().startsWith("```") ||
            l.trimStart().startsWith("|") ||
            l.trimStart().startsWith("<details", ignoreCase = true) ||
            l.trimStart().startsWith(">") ||
            l.trimStart().startsWith("- [") ||
            l.trimStart().startsWith("#")
        ) {
            break
        }
        if (buffer.isNotEmpty()) buffer.append("\n")
        buffer.append(l)
        i++
    }
    return MarkdownBlock(kind = MarkdownBlockKind.TEXT, content = buffer.toString().trim()) to i
}

// ============================
// 内部块模型
// ============================

internal enum class MarkdownBlockKind {
    TEXT, CODE_BLOCK, DIFF, TABLE, QUOTE, COLLAPSIBLE, CHECKLIST
}

internal data class MarkdownBlock(
    val kind: MarkdownBlockKind,
    val content: String,
    val language: String = "",
    val tableHeaders: List<String> = emptyList(),
    val tableRows: List<List<String>> = emptyList(),
    val summary: String = "",
    val checklistItems: List<Pair<String, Boolean>> = emptyList()
) {
    fun toMessage(
        id: String,
        senderName: String,
        senderAvatar: String,
        timestamp: Long
    ): ChatMessage {
        val extra = mutableMapOf<String, String>()
        val (kindStr, type) = when (kind) {
            MarkdownBlockKind.TEXT -> RichMessageKind.TEXT_STREAM to MessageType.TEXT
            MarkdownBlockKind.CODE_BLOCK -> RichMessageKind.CODE_BLOCK to MessageType.CUSTOM
            MarkdownBlockKind.DIFF -> RichMessageKind.DIFF to MessageType.CUSTOM
            MarkdownBlockKind.TABLE -> RichMessageKind.TABLE to MessageType.CUSTOM
            MarkdownBlockKind.QUOTE -> RichMessageKind.QUOTE to MessageType.CUSTOM
            MarkdownBlockKind.COLLAPSIBLE -> RichMessageKind.COLLAPSIBLE to MessageType.CUSTOM
            MarkdownBlockKind.CHECKLIST -> RichMessageKind.CHECKLIST to MessageType.CUSTOM
        }
        extra[AgUiExtraKeys.KIND] = kindStr

        when (kind) {
            MarkdownBlockKind.CODE_BLOCK, MarkdownBlockKind.DIFF -> {
                extra[AgUiExtraKeys.CODE_LANGUAGE] = language.ifEmpty { "text" }
                extra[AgUiExtraKeys.CODE_LINE_COUNT] = content.lines().size.toString()
            }
            MarkdownBlockKind.TABLE -> {
                val headerArr = JSONArray()
                tableHeaders.forEach { headerArr.put(it) }
                extra[AgUiExtraKeys.TABLE_HEADERS] = headerArr.toString()
                val rowArr = JSONArray()
                tableRows.forEach { row ->
                    val inner = JSONArray()
                    row.forEach { inner.put(it) }
                    rowArr.put(inner)
                }
                extra[AgUiExtraKeys.TABLE_ROWS] = rowArr.toString()
            }
            MarkdownBlockKind.QUOTE -> {
                extra[AgUiExtraKeys.QUOTE_LEVEL] = "1"
            }
            MarkdownBlockKind.COLLAPSIBLE -> {
                extra[AgUiExtraKeys.COLLAPSIBLE_SUMMARY] = summary
                extra[AgUiExtraKeys.COLLAPSIBLE_OPEN] = "false"
            }
            MarkdownBlockKind.CHECKLIST -> {
                val arr = JSONArray()
                checklistItems.forEach { (text, checked) ->
                    arr.put(JSONObject().apply {
                        put("text", text)
                        put("checked", checked)
                    })
                }
                extra[AgUiExtraKeys.CHECKLIST_ITEMS] = arr.toString()
                extra[AgUiExtraKeys.CHECKLIST_COMPLETED] = checklistItems.count { it.second }.toString()
            }
            else -> Unit
        }

        return ChatMessage(
            id = id,
            content = when (kind) {
                MarkdownBlockKind.CODE_BLOCK, MarkdownBlockKind.DIFF -> content
                else -> content
            },
            type = type,
            isSelf = false,
            senderName = senderName,
            senderAvatar = senderAvatar,
            senderId = "ai",
            timestamp = timestamp,
            status = MessageStatus.SENT,
            extra = extra
        )
    }
}
}
