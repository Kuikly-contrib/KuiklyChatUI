---
last_updated: 2026-08-07
status: implemented
owner: @kuikly-chat-team
---

# Feature: Compose Chat 消息时间与发送状态（TimeAndStatus）

## Status: ✅ Implemented

## 目标
在消息气泡旁或内部展示发送时间与状态指示，对齐 Flutter `SimpleTextMessage` 的 `showTime` / `showStatus` 与 `TimeAndStatus` 子组件（见 `flutter-chat-ui-components.md` §6.7）。

## 非目标
- 不实现 `deliveredAt` / `seenAt` / 双勾已读（模型字段可预留，UI 后置）。
- 列表顶部**日期分隔 header** 见 [`feature-compose-datetime.md`](feature-compose-datetime.md)（`showDateSeparators`，对齐 Flyer `headerWidget`）。
- 不在本需求内做 Markdown 或流式动画（见 `feature-compose-text-markdown-renderer.md`）。

## 技术方案

### 涉及的模块
- `chat/compose/ui/`：新增 `MessageTimeAndStatus.kt`
- `chat/compose/ui/ChatScreenConfig.kt`：`showTime`、`showStatus`、`timeAndStatusPosition`
- `chat/compose/core/ChatTheme.kt`：时间戳字号/颜色 token
- `chat/compose/core/ChatStrings.kt`：发送中/已发送等文案（图标不可用时的 fallback）
- `chat/compose/core/ChatController.kt` / `chat/compose/adapter/`：发送路径写入真实 `createdAt`

### 数据模型变更
首期不扩展 `ChatMessage` 字段，使用现有：

```kotlin
data class ChatMessage(
    // ...
    val createdAt: Long = 0L,  // 毫秒时间戳，发送时必须写入
    val status: MessageStatus,
)
```

可选后续扩展（本需求不实现 UI）：

```kotlin
// metadata 或独立字段，P2
// sentAt, deliveredAt, seenAt
```

### 配置与格式化

```kotlin
enum class TimeAndStatusPosition {
    BELOW_BUBBLE,   // 气泡下方（Flyer 默认之一）
    INSIDE_BUBBLE,  // 气泡内右下角
}

data class ChatScreenConfig(
    val showTime: Boolean = true,
    val showStatus: Boolean = true,
    val timeAndStatusPosition: TimeAndStatusPosition = TimeAndStatusPosition.BELOW_BUBBLE,
    val timeFormat: String = "HH:mm",
    val dateTimeFormatter: ChatDateTimeFormatter = LocalChatDateTimeFormatter.Default,
    val hideTimeWhenGrouped: Boolean = true,
)
```

时间与 formatter 详见 [`feature-compose-datetime.md`](feature-compose-datetime.md)。

### 状态展示映射

| `MessageStatus` | UI |
|-----------------|-----|
| `SENDING` | 发送中指示（文字或图标） |
| `STREAMING` | 可选「生成中」或不显示 |
| `SENT` | 默认隐藏或 subtle 勾 |
| `FAILED` | 保留现有「重试」入口，与时间区不重复 |
| `CANCELLED` | 可选「已取消」文案 |

### Controller 变更
- `sendText` 创建用户消息时：`createdAt = clock.now()`（或注入 `ChatClock`）。
- `applyTextDelta` 创建 assistant 消息时写入 `createdAt`。
- 历史/Adapter 映射路径保证 `createdAt` 非 0。

## 验收标准
- 每条消息在配置开启时显示格式化时间（默认 `HH:mm`）。
- 用户消息发送完成后状态从 `SENDING` 变为 `SENT`，时间区与状态一致。
- 流式 assistant 消息可在 `SENT` 后显示时间；流式过程中策略与配置一致。
- `showTime = false` / `showStatus = false` 时不渲染对应区域。
- `ChatSlots` 可提供 `timeAndStatusContent` 完全替换（与消息行文档协同）。
- 单测：`createdAt` 写入、时间格式化纯函数、status→展示文案映射。

## 依赖
- `feature-kuikly-chat-ui-sdk-redesign`（✅）
- `feature-compose-message-row.md`（📋）：时间区在消息行内排版
- `feature-compose-datetime.md`（✅）：`ChatDateTimeFormatter`、分组时间显隐

---
> 参考：Flutter `timeFormat`、`getIconForStatus`、`TimeAndStatus`
