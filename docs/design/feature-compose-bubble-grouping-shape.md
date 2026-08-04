---
last_updated: 2026-08-04
status: implemented
owner: @kuikly-chat-team
---

# Feature: Compose Chat 分组气泡圆角（Bubble Grouping Shape）

## Status: ✅ Implemented

## 目标
根据 `MessageGroupStatus`（SINGLE / FIRST / MIDDLE / LAST）调整气泡圆角与间距，对齐 Flyer 连续消息「粘连气泡」视觉，替代当前仅调整 `topPadding` 的实现。

## 非目标
- 不修改 `MessageGroupStatus` 计算逻辑（`ChatMessage.groupStatus` 已存在）。
- 不改变分组时间窗口算法（仍用 `ChatScreenConfig.groupingWindowMillis`）。
- 不涉及头像显隐（见 avatar 文档）。

## 技术方案

### 涉及的模块
- `chat/compose/ui/BubbleShape.kt`（新建）：`groupedBubbleShape(groupStatus, isMine, theme)`
- `chat/compose/ui/ChatMessageItem.kt` 或 `TextBubbleContent.kt`：应用 shape
- `chat/compose/core/ChatTheme.kt`：可选 `bubbleRadiusSmall`（组内小圆角）

### 数据模型变更
无。复用 `MessageGroupStatus` 与 `ChatTheme.bubbleRadius`。

### 圆角策略（示例，与 Flyer 常见 IM 一致）

| groupStatus | 入站（左侧尖角侧） | 出站（右侧尖角侧） |
|-------------|-------------------|-------------------|
| SINGLE | 四角大圆角 | 四角大圆角 |
| FIRST | 顶部大圆角 + 底部小圆角 | 同上 |
| MIDDLE | 上下小圆角 | 同上 |
| LAST | 底部大圆角 + 顶部小圆角 | 同上 |

```kotlin
fun groupedBubbleShape(
    groupStatus: MessageGroupStatus,
    isMine: Boolean,
    largeRadius: Dp,
    smallRadius: Dp = 4.dp,
): RoundedCornerShape
```

### 间距
- FIRST/SINGLE：`topPadding = 8dp`
- MIDDLE/LAST：`topPadding = 2dp`（保持现有逻辑，可迁入本模块）

## 验收标准
- 同一作者连续 3+ 条消息，气泡在视觉上「成组」，末条与首条圆角明显大于中间条。
- 不同作者之间组间距明显大于组内间距。
- 入站/出站尖角方向正确（己方靠右、对方靠左）。
- 单测：`groupedBubbleShape` 四角半径组合表驱动测试（纯 Kotlin，不依赖 Compose 绘制）。

## 依赖
- `feature-compose-message-row.md`（📋）：气泡在行内渲染

---
> 参考：Flutter 各 `flyer_chat_*` 消息的 `borderRadius` + `MessageGroupStatus`
