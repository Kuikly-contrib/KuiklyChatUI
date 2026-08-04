package com.tencent.kuiklybase.chat.bubble

import com.tencent.kuiklybase.chat.session.ChatSessionConfig

// ============================
// AG-UI 富卡片渲染器统一注册入口
// ============================
//
// 各卡片的渲染实现按类型拆分在同包下的独立文件中：
//   共享外壳/指示器   RichCardShell.kt
//   loading          LoadingCardRenderer.kt
//   code_block       CodeBlockCardRenderer.kt
//   diff             DiffCardRenderer.kt
//   command_exec     CommandExecCardRenderer.kt
//   file_edit/read   FileEditCardRenderer.kt
//   tool_result/err  ToolInvocationCardRenderer.kt
//   error            ErrorCardRenderer.kt
//   suggestions      SuggestionsCardRenderer.kt
//   table            TableCardRenderer.kt
//   quote            QuoteCardRenderer.kt
//   collapsible      CollapsibleCardRenderer.kt
//   checklist        ChecklistCardRenderer.kt
//
// 新增卡片类型时：建一个 XxxCardRenderer.kt，并在下面的列表里注册；
// 同时别忘了在 compose/agentcard/ 补上语义对齐的 Compose 实现（见 AGENTS.md 硬性规则 2）。

/**
 * 返回一组 AG-UI 富卡片渲染器，可直接追加到 [ChatSessionConfig.messageRenderers]。
 *
 * 用法：
 * ```
 * ChatSession({ session.observableMessages }) {
 *     messageRenderers.addAll(agUiRichRenderers())
 * }
 * ```
 *
 * @param onCopyCode 代码块"复制"按钮点击回调
 * @param onSuggestionClick 建议选项卡片中某项点击回调
 */
fun agUiRichRenderers(
    onCopyCode: ((String) -> Unit)? = null,
    onSuggestionClick: ((String) -> Unit)? = null
): List<MessageRendererFactory> = listOf(
    LoadingCardRenderer(),
    CodeBlockCardRenderer(onCopyClick = onCopyCode),
    DiffCardRenderer(),
    CommandExecCardRenderer(),
    FileEditCardRenderer(),
    ToolInvocationCardRenderer(),
    ErrorCardRenderer(),
    SuggestionsCardRenderer(onSuggestionClick = onSuggestionClick),
    TableCardRenderer(),
    QuoteCardRenderer(),
    CollapsibleCardRenderer(),
    ChecklistCardRenderer()
)
