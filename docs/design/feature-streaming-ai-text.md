---
last_updated: 2026-07-27
status: active
owner: @kuikly-chat-team
---

# Feature: AI 流式文本与 Markdown 渲染
Status: ✅ Implemented

## 目标
在聊天气泡中渲染 AI 流式 Markdown 内容，支持逐字打字动画与增量渲染。

## 范围
- `AiMessageTextView`（`chat/ai/`）：`AiTypingState.Generating(id)` 触发逐字动画，`typingInterval`/`typingSpeed` 可调。
- `ChatAiTypingIndicatorView`：AI 思考指示器（波浪圆点 + 微光）。
- Compose 侧：`StreamingMarkdownView` / `StreamingMarkdownParser` / `InlineMarkdown`。
- 链接点击、长按、动画状态回调。

## 验收
- 流式输出平滑、不闪烁；Markdown（粗体/代码/链接）正确解析；动画结束回调可驱动滚动。

## 非目标
- 不做协议解析（文本流由 `AgUiSession` 提供）。
- 不处理非 Markdown 富媒体（图片/音频等由气泡组件负责）。

## 依赖
- `KuiklyMarkdown` 三方库（Markdown 渲染）。
- `feature-agui-protocol`（✅ 已实现）：流式文本来源于 `AgUiSession` 的 `TEXT_MESSAGE_CONTENT` 事件。

## 相关
`README.md`「AiMessageText — AI 消息文本」、`AGUI_INTEGRATION_GUIDE.md` 第三节。
