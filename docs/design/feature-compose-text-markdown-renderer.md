---
last_updated: 2026-08-04
status: draft
owner: @kuikly-chat-team
---

# Feature: Compose Chat 文本与 Markdown 消息渲染

## Status: 📝 Draft

## 目标
将默认纯文本气泡升级为可配置文本渲染器，对齐 Flutter `FlyerChatTextMessage` / `SimpleTextMessage`（见 `flutter-chat-ui-components.md` §6.7、§10.1），支持 Markdown、链接点击与流式文本展示策略。

## 非目标
- 不实现链接预览卡片（`flutter_link_previewer`，独立需求）。
- 不实现 `TextStreamMessage` 逐块淡入动画的全部 Flyer 能力（首期：流式 append + 可选 Markdown 全量渲染）。
- 不替换 AgentCard 渲染（继续走 `MessageRendererRegistry`）。

## 技术方案

### 涉及的模块
- `chat/compose/ui/renderer/TextMessageRenderer.kt`（新建，或 `compose/ui/message/`）
- `chat/compose/core/MessageRenderer.kt`：注册默认 text renderer
- `chat/compose/agentcard/StreamingMarkdownView.kt` 等：评估复用 vs 迁移
- `chat/compose/ui/ChatCallbacks.kt`：`onLinkClick`

### 数据模型变更
使用现有：

```kotlin
data class MessagePart.Text(
    val value: String,
    val format: TextFormat = TextFormat.PLAIN, // PLAIN | MARKDOWN
)
```

### Renderer 行为

| `TextFormat` | 行为 |
|--------------|------|
| `PLAIN` | 现有 `Text` 气泡 |
| `MARKDOWN` | 调用 KuiklyMarkdown / 现有 Streaming 组件 |
| 流式 `STREAMING` status | 增量文本 + 可选尾部光标 |

```kotlin
class TextMessageRenderer : MessageRenderer {
    @Composable
    override fun Render(message, groupStatus, theme, modifier) {
        // 应用 BubbleShape + 内容
    }
}
```

### 与 MessageRendererRegistry 关系
- 默认 `defaultChatRendererRegistry()` 对「仅含 Text part、无 AgentCard」消息注册 text renderer。
- 宿主可通过 `MessageRendererRegistry.plus` 覆盖。

## 验收标准
- 纯文本、Markdown（粗体/代码/链接）在 Demo 中正确显示。
- 流式 assistant 消息边收边渲染，无明显整泡闪烁。
- 链接点击走 `ChatCallbacks.onLinkClick`，不硬编码 WebView。
- 单测：Markdown 模式选择、PLAIN fallback；流式 append 不丢字符。

## 依赖
- `feature-compose-message-row.md`（📋）
- `feature-compose-bubble-grouping-shape.md`（📋）
- `feature-streaming-ai-text.md`（✅）：可复用 Streaming Markdown 实现
- `KuiklyMarkdown` 或项目内已有 markdown 模块

---
> 参考：`FlyerChatTextMessage`、`FlyerChatTextStreamMessage`
