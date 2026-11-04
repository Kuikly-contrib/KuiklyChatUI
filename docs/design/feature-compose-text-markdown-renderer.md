---
last_updated: 2026-08-06
status: implemented
owner: @kuikly-chat-team
---

# Feature: Compose Chat 文本与 Markdown 消息渲染

## Status: ✅ Implemented

## 目标
将默认气泡内的 `MessagePart.Text` 升级为可配置的 part 级文本渲染，
对齐 Flutter `FlyerChatTextMessage` / `SimpleTextMessage`（见
`flutter-chat-ui-components.md` §6.7、§10.1），支持纯文本、Markdown、宿主链接回调及明确的流式策略。

## 非目标
- 不实现链接预览卡片（`flutter_link_previewer`，独立需求）。
- 不实现 `TextStreamMessage` 逐块淡入动画；Kuikly 的 fade `AnimatedVisibility` 仍有已知问题。
- 不支持原始 HTML、脚本、远程图片、数学公式和任意嵌入组件；未知语法按文本显示。
- 不替换 AgentCard 渲染（继续走 `MessageRendererRegistry`）。
- 不把传统 Kuikly DSL 的 `KuiklyMarkdown` 组件直接嵌入 Compose。

## 现状与问题

- `TextBubbleContent.kt` 已按原始顺序遍历全部 part，但 `Text` 始终用普通 `Text` 渲染。
- `MessageRendererRegistry.rendererFor()` 只按整条 `AgentCardContent` 或第一个
  `MessagePart.Custom.type` 路由，不识别 `MessagePart.Text`。把 `"text"` 注册为整消息 renderer
  会吞掉同一消息中的附件和其他 part。
- `compose/agentcard/StreamingMarkdownView.kt` 已有 Compose 原生 block parser/renderer，
  但当前直接依赖 `ComposeCardTheme`、没有链接解析，且 `streaming` 参数实际未显示光标。
- 该组件每次 content 变化都会全量解析；注释依赖的「上游 10Hz throttle」并不存在于
  `ChatController` / `LlmChatAdapter` 发送链路，不能原样用于高频 token。
- `LlmChatAdapter` 与 `ChatController.applyTextDelta()` 当前都创建默认
  `TextFormat.PLAIN`，即使 UI 支持 Markdown 也不会进入该分支。

## 技术方案

### 涉及的模块
- `chat/compose/markdown/ChatMarkdownTheme.kt`：将 `ChatTheme` 映射为 `MarkdownConfig`。
- `chat/compose/ui/TextPartContent.kt`：按 format/status 选择纯文本或 `MarkdownView`。
- `chat/compose/core/ChatState.kt`：让 `ChatStreamEvent.TextDelta` 携带文本格式。
- `chat/compose/adapter/LlmChatAdapter.kt`：历史与流式 assistant 文本显式标记格式。
- `chat/compose/ui/ChatSlots.kt`：新增 `textPartContent` 与 `onLinkClick`。

### 数据与流式格式契约

沿用现有 part：

```kotlin
sealed interface MessagePart {
    data class Text(
        val value: String,
        val format: TextFormat = TextFormat.PLAIN,
    ) : MessagePart
}

sealed interface ChatStreamEvent {
    data class TextDelta(
        val messageId: String? = null,
        val text: String,
        val format: TextFormat = TextFormat.PLAIN,
    ) : ChatStreamEvent
}
```

- `TextFormat` 只描述内容语法；`MessageStatus.STREAMING` 只描述生命周期，二者不得混为一个枚举。
- `ChatController.applyTextDelta()` 创建或更新文本 part 时保留 `event.format`；同一流后续 delta
  必须保持格式一致，格式变化时以 `ReplaceMessage` 明确替换。
- `LlmChatAdapter` 增加 `assistantTextFormat`（默认 `MARKDOWN`）：
  历史 LLM 消息和发出的 assistant delta 使用该格式，用户消息仍为 `PLAIN`。
- 其他 `ChatMessageSender` 不受 LLM 约定影响；未显式提供 format 时继续安全回退为纯文本。
- `AgUiChatMessageMapper` 只在 adapter 边界根据已有 metadata/kind 映射格式，
  不让 AG-UI 常量进入 `compose/ui`。

新增渲染配置：

```kotlin
enum class StreamingMarkdownMode {
    PLAIN_UNTIL_COMPLETE,
    RENDER_EACH_UPDATE,
}

data class ChatTextRenderConfig(
    val streamingMarkdownMode: StreamingMarkdownMode =
        StreamingMarkdownMode.PLAIN_UNTIL_COMPLETE,
    val showStreamingCursor: Boolean = true,
)
```

`ChatTextRenderConfig` 按 `feature-compose-theme-and-slots.md` 定义的统一参数后缀接入两个现有
`ChatScreen` 入口，不新增默认参数重叠的 overload。

- `PLAIN` 始终走普通 `Text`。
- `MARKDOWN + SENT` 走通用 Markdown renderer。
- `MARKDOWN + STREAMING` 默认先按纯文本展示并追加静态光标，完成后一次解析；
  宿主显式选择 `RENDER_EACH_UPDATE` 才在每次增量后全量 Markdown 渲染。
- `SENDING / FAILED / CANCELLED` 按 part.format 渲染，但不显示流式光标。

默认策略避免把不存在的 throttle 当成性能保证，也不会缓存旧 content 导致丢字符。

### Part slot 与整消息 renderer

```kotlin
data class ChatSlots(
    // 现有字段保持不变
    val textPartContent: (
        @Composable (
            message: ChatMessage,
            part: MessagePart.Text,
            isMine: Boolean,
        ) -> Unit
    )? = null,
)

data class ChatCallbacks(
    // 现有字段保持不变
    val onLinkClick: ((message: ChatMessage, url: String) -> Unit)? = null,
)
```

`textPartContent` 只在默认气泡遍历到文本 part 时生效；整消息
`MessageRendererRegistry`、`messageContent` 或 `systemMessageContent` 已命中时不会重复调用。
气泡颜色、shape、padding 和整消息点击仍由 `TextBubbleContent` 负责，文本 part renderer
不得再次套一层气泡。

### Markdown 与链接

从现有 `StreamingMarkdownParser` / `InlineMarkdown` 抽取通用实现，首期支持：

- 段落、ATX 标题、引用、水平线、有序/无序列表；
- fenced code block、行内 code、粗体、斜体和现有表格子集；
- Markdown 显式链接 `[label](url)`；不自动执行 HTML，也不加载 Markdown 图片。

解析失败或未闭合语法必须原样降级，不能吞字符。代码块保持由外层消息列表滚动，不创建同方向
内层滚动容器。

链接使用 Kuikly 的 `AnnotatedString` +
`LinkAnnotation.Clickable`，listener 只调用 `ChatCallbacks.onLinkClick`。
不得使用无 listener 的 `LinkAnnotation.Url` 自动打开 URI；callback 为空时链接只着色、不响应，
协议白名单、跳转和 WebView 均由宿主决定。

纯 parser 只返回 URL/range，不把 callback 捕获进缓存 AST。Composable listener 通过
`rememberUpdatedState` 读取最新 callback（或把 callback/message id 纳入 remember key），
避免流式重组或列表复用后点击仍调用旧宿主闭包。

通用 renderer 从 `ChatTheme` 映射正文、链接、代码背景/边框等 token；
现有 `ComposeCardTheme` wrapper 继续服务 AgentCard/legacy 调用，避免包迁移破坏已有 API。

## 验收标准

- 同一消息中的文本、附件、第二段文本按原 part 顺序渲染；文本 renderer 不吞其他 part。
- Demo 覆盖 PLAIN、完成态 Markdown、流式 Markdown 两种策略、畸形 Markdown 和链接。
- callback 为空时链接不自动打开；存在 callback 时一次点击只回调一次原始 URL。
- 默认流式策略不在每个 token 上全量解析；切换 `RENDER_EACH_UPDATE` 后内容仍不丢字符。
- 通用 parser/renderer 不再依赖 `ComposeCardTheme`；旧 `StreamingMarkdownView` 调用仍可编译。
- `commonTest` 覆盖格式传播、渲染模式选择、block/inline/link 解析、未闭合语法 fallback，
  并用 500 行输入做功能回归；耗时预算放在独立 benchmark，避免把墙钟断言写进多平台单测。
- 按 `AGENTS.md` 三条门禁验证，并补跑 JS 编译与 Android/H5 链接人工验收。

## 兼容性

`ChatSlots`、`ChatCallbacks`、`ChatTextRenderConfig`、`ChatStreamEvent.TextDelta`、
`LlmChatAdapter` 构造器及 `ChatScreen` 都是公开 API；
新增默认参数只保证多数源码调用可继续编译，不保证 KMP 二进制兼容。与主题/slots 分层一起按
major 版本交付，并保留旧 `StreamingMarkdownView` forwarding wrapper。

## 依赖

- `feature-compose-message-row.md`、`feature-compose-bubble-grouping-shape.md`（✅）。
- Compose 文本渲染见本文；旧 `AiMessageText` DSL 组件已删除。
- `feature-compose-theme-and-slots.md`（统一文本 slot 与 Markdown token）。
- `com.tencent.kuiklybase:KuiklyMarkdownCompose`（`MarkdownView` + `MarkdownConfig`）。

---
> 参考：`FlyerChatTextMessage`、`FlyerChatTextStreamMessage`；KuiklyUI
> `compose/src/commonMain/.../ui/text/LinkAnnotation.kt`
