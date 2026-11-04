package com.kuikly.kuiklychat.dsl.chat

/**
 * Best-effort fix for the bracket mistakes LLMs make in long single-line JSON:
 * an array left open before the next object key (`[{..},"key":..`), a mismatched
 * closer, or missing closers at the end. Returns null when the input is not
 * repairable by bracket balancing alone.
 */
internal fun repairJsonBrackets(line: String): String? {
    // `},"]` is never valid JSON: models emit it when closing an array of objects.
    val source = line.replace("},\"]", "}]")
    val out = StringBuilder(line.length + 8)
    val stack = ArrayList<Char>()
    var inString = false
    var escaped = false
    // Output index of the last comma at the current nesting level outside strings.
    var lastComma = -1
    for (ch in source) {
        if (inString) {
            out.append(ch)
            when {
                escaped -> escaped = false
                ch == '\\' -> escaped = true
                ch == '"' -> inString = false
            }
            continue
        }
        when (ch) {
            '"' -> { inString = true; out.append(ch) }
            '{', '[' -> { stack += ch; lastComma = -1; out.append(ch) }
            ',' -> { lastComma = out.length; out.append(ch) }
            ':' -> {
                // A key inside an array means the array was never closed.
                if (stack.lastOrNull() == '[') {
                    if (lastComma < 0) return null
                    out.insert(lastComma, ']')
                    stack.removeAt(stack.lastIndex)
                    lastComma = -1
                }
                out.append(ch)
            }
            '}', ']' -> {
                val expected = if (ch == '}') '{' else '['
                while (stack.isNotEmpty() && stack.last() != expected) {
                    out.append(if (stack.removeAt(stack.lastIndex) == '{') '}' else ']')
                }
                if (stack.isEmpty()) return null
                stack.removeAt(stack.lastIndex)
                lastComma = -1
                out.append(ch)
            }
            else -> out.append(ch)
        }
    }
    if (inString) out.append('"')
    while (stack.isNotEmpty()) out.append(if (stack.removeAt(stack.lastIndex) == '{') '}' else ']')
    return out.toString()
}
