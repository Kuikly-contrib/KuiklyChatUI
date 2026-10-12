package com.tencent.kuiklybase.chat.compose.ui

/** True when content needs block-level markdown layout (lists, headings, code fences, etc.). */
internal fun hasBlockMarkdownSyntax(content: String): Boolean {
    return content.lineSequence().any { line ->
        val trimmed = line.trimStart()
        trimmed.startsWith("#") ||
            trimmed.startsWith("```") ||
            trimmed.startsWith(">") ||
            trimmed.startsWith("- ") ||
            trimmed.startsWith("* ") ||
            trimmed.matches(BLOCK_ORDERED_LIST_REGEX) ||
            trimmed.count { it == '|' } >= 2
    }
}

private val BLOCK_ORDERED_LIST_REGEX = Regex("""\d+\.\s+\S.*""")
