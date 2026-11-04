---
last_updated: 2026-08-04
status: implemented
owner: @kuikly-chat-team
---

# Feature: Compose Chat 消息行外壳（ChatMessageRow）

## Status: ✅ Implemented

## 目标
将当前扁平的 `DefaultChatMessageItem` 升级为独立的消息行组件，对齐 Flutter `ChatMessage` 外层容器（见 `flutter-chat-ui-components.md` §6.3），统一承载头像、昵称、气泡内容、时间与对齐策略。

## 非目标
- 不替换各类消息内容渲染（文本 Markdown、图片等由各 Renderer 负责）。
- 不实现列表 insert/remove 动画（`ChatOperation` 后置）。
- 不在此文件实现头像/时间细节（见 `feature-compose-chat-avatar.md`、`feature-compose-message-time-status.md`），本需求负责 **编排与布局契约**。

## 技术方案

### 涉及的模块
- `chat/compose/ui/ChatMessageRow.kt`（新建）
- `chat/compose/ui/ChatMessageItem.kt`：瘦身为行内「默认内容」或迁移至 `TextBubbleContent.kt`
- `chat/compose/ui/ChatMessageList.kt`：调用 `ChatMessageRow` 而非直接 `DefaultChatMessageItem`
- `chat/compose/ui/ChatSlots.kt`：新增 `messageRowContent`（整条行替换，对齐 `chatMessageBuilder`）

### 布局结构（对齐 Flyer）

```text
ChatMessageRow
├── [Avatar]（可选，group 感知）
├── Column
│   ├── Username（入站 FIRST/SINGLE，可选）
│   ├── Row
│   │   ├── MessageContent（Renderer / slot / 默认气泡）
│   │   └── RetryAction（FAILED 时）
│   └── MessageTimeAndStatus（可选）
└── [Avatar]（己方，可选）
```

### 对齐与分组

| 参数 | 说明 |
|------|------|
| `isMine` | `message.author.id == controller.currentUser.id` |
| `groupStatus` | 已有 `MessageGroupStatus`，驱动头像显隐、昵称、圆角（见 bubble-shape 文档） |
| `sentAlignment` / `receivedAlignment` | 首期：`End` / `Start`，与现有一致 |

### API（Slots）

```kotlin
data class ChatSlots(
    // ...
    /** 替换整条消息行（对齐 Flutter chatMessageBuilder） */
    val messageRowContent: (
        @Composable (
            ChatMessage,
            MessageGroupStatus,
            MessageContent: @Composable () -> Unit
        ) -> Unit
    )? = null,
)
```

默认实现：内置 `DefaultChatMessageRow`，组合 Avatar + Username + content + TimeAndStatus。

## 验收标准
- 入站/出站消息左右对齐正确；与头像、时间组件联调后视觉接近 Flyer 默认 IM。
- `messageRowContent` slot 可包裹自定义内容而不破坏列表 key/滚动。
- AgentCard / Custom Renderer 在行内宽度与对齐正确（不全宽顶格）。
- 单测：行布局纯逻辑（是否显示昵称/头像）可测；Android Demo 人工验收截图对比 Flyer 示例。

## 依赖
- `feature-compose-chat-avatar.md`（📋）
- `feature-compose-message-time-status.md`（📋）
- `feature-compose-bubble-grouping-shape.md`（📋，圆角由内容区或行内 bubble 应用）

---
> 参考：Flutter `ChatMessage`、`leadingWidget` / `trailingWidget` / `headerWidget`
