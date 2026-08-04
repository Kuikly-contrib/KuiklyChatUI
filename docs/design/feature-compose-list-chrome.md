---
last_updated: 2026-08-04
status: draft
owner: @kuikly-chat-team
---

# Feature: Compose Chat 列表附属 UI（List Chrome）

## Status: 📝 Draft

## 目标
补齐消息列表周围的附属 UI，对齐 Flutter `ScrollToBottom`、`LoadMore`、`EmptyChatList`、`IsTypingIndicator` 及日期分隔能力，提升列表「完整度」而非仅消息行本身。

## 非目标
- 不实现 `ChatAnimatedList` 的 insert/remove 动画。
- 不实现底部「加载更新消息」分页（`onStartReached`）。
- 不改造 Controller 分页协议（沿用 `LoadEarlier`）。

## 技术方案

### 涉及的模块
- `chat/compose/ui/ScrollToBottomButton.kt`：升级为 FAB 样式（对齐 §6.9）
- `chat/compose/ui/ChatMessageList.kt`：插入 `DateSeparator`、`LoadMoreIndicator`
- `chat/compose/ui/`：新增 `DateSeparator.kt`、`LoadMoreIndicator.kt`、`IsTypingIndicator.kt`
- `chat/compose/ui/ChatScreen.kt`：SafeArea / 键盘与 Composer 高度协调
- `chat/compose/ui/ChatSlots.kt`：`dateSeparatorContent`、`typingIndicatorContent`

### 子功能清单

| 子功能 | Flutter 对照 | 说明 |
|--------|-------------|------|
| 回底按钮 | `ScrollToBottom` | FAB + 未读角标；相对 Composer 偏移 |
| 加载更早 | `LoadMore` | `CircularProgressIndicator` 替代纯文字 |
| 日期分隔 | （Flyer 列表惯例） | 按天插入「今天 / 昨天 / 日期」 |
| 正在输入 | `IsTypingIndicator` | 三点动画，由 slot 或 `state.isTyping` 驱动 |
| 空列表 | `EmptyChatList` | 增强现有 `EmptyChatContent` |
| 安全区 | `handleSafeArea` | 列表底与 Composer 适配刘海/键盘 |

### 数据模型变更

```kotlin
data class ChatState(
    // ...
    val isTyping: Boolean = false,        // 对方正在输入（宿主或 Controller 更新）
    val typingUserId: String? = null,
)

data class ChatScreenConfig(
    val scrollToBottomAppearanceThresholdPx: Int = 120,
    val useComposerHeightForScrollOffset: Boolean = true,
)
```

### DateSeparator 逻辑
- 遍历 `messages`，当 `createdAt` 跨自然日时在项前插入分隔 composable。
- 依赖 `feature-compose-message-time-status.md` 保证 `createdAt` 有效。

## 验收标准
- 上滑离开底部后出现 FAB 式回底按钮；点击贴底并清空 `unreadCount`。
- 加载历史时顶部显示标准 loading，而非仅文字链。
- 跨天消息之间显示日期分隔条。
- `isTyping = true` 时列表底部（Composer 上方）显示输入指示动画。
- Android + JS 编译通过；列表 chrome 行为记入 `compose-ui-acceptance.md`。

## 依赖
- `feature-kuikly-chat-ui-sdk-redesign`（✅ 列表滚动基础已有）
- `feature-compose-message-time-status.md`（📋）：日期分隔依赖时间戳）

---
> 参考：`flutter-chat-ui-components.md` §6.8–6.11、§13
