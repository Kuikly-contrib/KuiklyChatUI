package com.tencent.kuiklybase.chat.agentcard_compose

/**
 * Markdown block model used by [StreamingMarkdownView].
 *
 * Spec: openspec/changes/improve-streaming-text-rendering/specs/macapp-chat-ui/spec.md (M2)
 *
 * Design intent:
 *   - Pure Kotlin (no Compose dependency) so the parser can be unit-tested in
 *     commonTest without spinning up a Compose runtime.
 *   - Coarse-grained block types only — paragraph / code / heading / list /
 *     quote / hr cover ~95% of Markdown in chat replies. Tables / math blocks /
 *     task lists / raw HTML can be added later as new subclasses without
 *     breaking existing consumers.
 *   - `CodeBlock.closed = false` is a first-class state: the model is still
 *     emitting tokens after a ``` fence and we want to render the partial
 *     content with monospace styling instead of leaving it as a paragraph.
 */
sealed class MdBlock {
    /** Continuous non-blank lines collapsed into one paragraph. */
    data class Paragraph(val text: String) : MdBlock()

    /**
     * Fenced code block (` ```lang … ``` `).
     * @param closed true when a closing ``` has been seen; false during
     *               streaming when we only have the opening fence.
     */
    data class CodeBlock(val lang: String, val code: String, val closed: Boolean) : MdBlock()

    /** ATX heading: `# / ## / ### / …`; level ∈ 1..6. */
    data class Heading(val level: Int, val text: String) : MdBlock()

    /**
     * One list item.
     * @param bulletOrNum the literal bullet token (`-`, `*`, `+`) or the
     *                    numeric prefix (`1.`, `12.`) preserved verbatim so
     *                    the renderer can choose to render it as-is.
     * @param indent      nesting depth: 0 = top level, 1 = first child, etc.
     *                    Derived from leading whitespace (2 spaces / 1 tab per level).
     */
    data class ListItem(val bulletOrNum: String, val text: String, val indent: Int = 0) : MdBlock()

    /** Block quote (`> …`); multi-line quotes are collapsed. */
    data class Quote(val text: String) : MdBlock()

    /** Horizontal rule (`---` / `***` / `___` on its own line). */
    object HorizontalRule : MdBlock()

    /**
     * Markdown table.
     * @param headers  表头列文本
     * @param rows     数据行，每行是一个 List<String>（列数与 headers 对齐）
     */
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MdBlock()
}
