---
last_updated: 2026-08-07
status: draft
owner: @kuikly-chat-team
---

# Feature: Compose Chat 列表附属 UI（List Chrome）

## Status: 📝 Draft（日期分隔 header 已落地，见 `feature-compose-datetime.md`）

## 目标
在现有 sticky-bottom、历史加载、空态和未读计数基础上，补齐可注入时区/本地化策略的日期分隔
与正在输入提示，并升级加载态、回底按钮和空态视觉，对齐 Flutter `ScrollToBottom`、
`LoadMore`、`EmptyChatList`、`IsTypingIndicator`。

## 非目标
- 不实现 `ChatAnimatedList` 的 insert/remove 动画。
- 不实现底部「加载更新消息」分页（`onStartReached`）。
- 不改造 `ChatHistoryLoader` / `ChatIntent.LoadEarlier` 分页协议。
- 不处理 Composer 安全区、键盘避让或高度测量；这些由 `feature-compose-composer-enhancement.md` 负责。
- 不在 `commonMain` 直接使用 `java.time` 或平台日期 API。

## 已实现（2026-08-07）

日期分隔已按 Flyer **`ChatMessage.headerWidget`** 模式接入（非独立 LazyColumn item）：

- `showDateSeparators` + `ChatDateTimeFormatter.dateSeparatorLabel()`；
- `ChatMessageListItem` 在消息行上方渲染 `ChatDateSeparator` / `dateSeparatorContent`；
- 跨自然日打断 `MessageGroupStatus`。

详见 [`feature-compose-datetime.md`](feature-compose-datetime.md)。

## 现状与改动边界

`ChatMessageList.kt` 已有以下能力，本需求只做增强，不重复建设：

- 距顶部 `loadEarlierThresholdPx` 时自动派发 `LoadEarlier`，并保留手动「加载更早」入口。
- `loadEarlierContent`、`emptyContent`、`scrollToBottomContent` 三个 slot。
- 离开底部后展示回底按钮，点击后滚底并清空 `unreadCount`。
- `ChatScreen` 使用 `Column` 将消息列表约束在 Composer 上方，因此回底按钮应继续放在列表内部，无需新增 Composer 高度偏移配置。
- 当前 `LazyColumn` 只有一个 `__chat_messages__` item，所有消息再放进内部 `Column`。
  因而它没有按消息懒加载，`firstVisibleItemIndex` 基本恒为 0，也无法按 message key 精确恢复历史锚点。

## 技术方案

### 涉及的模块
- `chat/compose/ui/ChatMessageList.kt`：将统一 entry 作为真正的 LazyColumn items 渲染，
  插入 typing 并修正锚点（**日期 header 已在 `ChatMessageListItem` 落地，见 datetime 文档**）。
- `chat/compose/ui/ChatListScroll.kt`：让近底判断显式接收距离阈值。
- `chat/compose/ui/ChatListEntry.kt`：把消息、日期和 typing 预先归一为可测试的 entry 列表（日期 entry 可后续从 header 模式迁移）。
- `chat/compose/ui/ScrollToBottomButton.kt`：升级为 mini FAB + 独立未读角标。
- `chat/compose/ui/ChatMessageItem.kt`：保留默认空态内容。
- `chat/compose/ui/`：✅ `ChatDateSeparator.kt`；📋 `TypingIndicator.kt`。
- `chat/compose/ui/MessageTimeAndStatus.kt`：与日期分隔共用同一时区/格式化契约。
- `chat/compose/ui/ChatSlots.kt`：实际承载 `ChatSlots`、`ChatScreenConfig`，在此增加日期/typing slot 与静态配置。
- `chat/compose/core/ChatTheme.kt`：补充日期分隔、typing、未读角标所需 token 和文案。

### 子功能清单

| 子功能 | Flutter 对照 | 说明 |
|--------|-------------|------|
| 回底按钮 | `ScrollToBottom` | mini FAB + 独立未读角标；继续锚定在列表底部 |
| 加载更早 | `LoadMore` | 空闲时保留可点击文案，加载中改用 `CircularProgressIndicator` |
| 日期分隔 | Flyer `headerWidget` | ✅ 消息行上方 header；📋 可选迁移为独立 LazyColumn entry |
| 正在输入 | `IsTypingIndicator` | 支持多个用户；默认三点动画，可由 slot 替换 |
| 空列表 | `EmptyChatList` | 默认内容在列表可用区域内居中 |

### 动态状态与公开 API

Typing 是宿主提供的在线态，不属于消息仓库，也不应写成只能表达单人的
`ChatState.isTyping + typingUserId`。新增独立 UI 状态：

```kotlin
data class ChatListChromeState(
    val typingUsers: List<ChatUser> = emptyList(),
) {
    companion object {
        val Empty = ChatListChromeState()
    }
}

interface ChatDateTimeFormatter {
    fun formatTime(timestampMillis: Long, pattern: String): String

    /** 返回 null 表示当前消息前不插入分隔项。 */
    fun dateSeparatorLabel(
        previousCreatedAt: Long?,
        currentCreatedAt: Long,
        nowMillis: Long,
        strings: ChatStrings,
    ): String?
}

data class ChatSlots(
    // 现有字段保持不变
    val dateSeparatorContent: (@Composable (timestampMillis: Long, label: String) -> Unit)? = null,
    val typingIndicatorContent: (@Composable (typingUsers: List<ChatUser>) -> Unit)? = null,
)

data class ChatScreenConfig(
    // 现有字段保持不变
    val nearBottomThresholdPx: Int = 120,
    val showDateSeparators: Boolean = false,
    val dateTimeFormatter: ChatDateTimeFormatter = LocalChatDateTimeFormatter.Default,
    val dateSeparatorClock: ChatClock = ChatClock.System,
)
```

> 以上字段已在 `ChatSlots.kt` / `ChatScreenConfig` 实现；默认 `showDateSeparators = false`。

`ChatListChromeState` 按 `feature-compose-theme-and-slots.md` 的统一后缀接入两个现有
`ChatScreen` 入口；不要新增一个其余参数全部有默认值的平行 overload。新增函数默认参数和
data class 字段都只保持源码兼容，不保持 KMP ABI。

渲染前按 `ChatUser.id` 去重 typing 用户并过滤 `controller.currentUser`。列表只消费该快照，
空 ID 视为无效并过滤；不新增 `ChatIntent.SetTyping`，也不把短生命周期在线态持久化到
`ChatState`。

### 日期与分组逻辑

- `createdAt <= 0L` 视为未知时间，不插入分隔项。
- `showDateSeparators = false` 时不插入日期。默认 formatter 为 `LocalChatDateTimeFormatter`（本地时区），与气泡时间一致。
- 宿主 formatter 同时负责消息时间、日期边界和文案，可使用
  `ChatStrings.today/yesterday`，从而避免「日期按本地时区、气泡时间仍按 UTC」的不一致。
- 首条有效时间消息以及 formatter 判定跨日时显示日期。`nowMillis` 来自可注入
  `ChatClock`，测试不得依赖真实时钟。
- `kotlinx-datetime`（✅ `0.6.0-RC.2-KBA-003`，见 `feature-compose-datetime.md`）在默认
  `LocalChatDateTimeFormatter` 中用于本地时区；宿主仍可注入 `UtcChatDateTimeFormatter`。
- 日期边界必须同时打断 `MessageGroupStatus`：计算当前项的 previous/next 时，将分隔边界一侧视为 `null`，
  避免气泡跨日期条仍被判为同组。
- 所有 entry key 都带类型 namespace，例如 `message:<id>`、`date:<nextMessageId>`、
  `chrome:typing`、`chrome:load-earlier`；不要把业务 message id 与 chrome 保留字符串放在同一 key 空间。

先构建统一 entry，再渲染：

```kotlin
sealed interface ChatListEntry {
    val key: String

    data class LoadEarlier(/* loading, hasMore, key */) : ChatListEntry
    data class DateSeparator(/* timestamp, label, key */) : ChatListEntry
    data class Message(/* message, groupStatus, key */) : ChatListEntry
    data class Typing(/* users, key */) : ChatListEntry
}
```

加载头、消息、日期和 typing 都生成独立保留 key，并作为真正的 `LazyColumn.item(s)` 渲染，
不再包在单个 `__chat_messages__` Column 中；空态仍由列表外层 Box 覆盖并居中。KuiklyUI 官方列表文档明确支持
`items(..., key = ...)`；这使长会话恢复懒加载，也让日期边界、tail signature 和锚点使用同一份结果。

历史加载开始时记录 nullable anchor（首条可见 message entry key 与 viewport offset，offset 可以是 0），
不能继续用 `anchorScrollOffset > 0` 当「已记录」哨兵。完成后按 key 在新 entry 列表中找到新 index，
先依赖 LazyColumn stable key 保持位置，并在未保持时按 API 的 offset 语义恢复同一 viewport 位置；
这样不再依赖可变高度消息的估算，也自然包含日期项净变化。
若 anchor key 被删除，回退到最接近的旧 message key，而不是跳到列表顶。

旧 `estimatedMessageHeightPx` 在迁移完成后不再参与锚点算法；按 major 策略移除或 deprecated，
不得同时保留两套恢复路径。

### 滚动与尾项

- `isNearBottom` 显式接收 `nearBottomThresholdPx`，使用「剩余可滚距离 <= threshold」，
  同时驱动回底按钮和未读清零语义，不与已有
  `loadEarlierThresholdPx` 混用；配置值必须大于等于 0。
- 新实现根据 `layoutInfo.totalItemsCount`、最后可见 item 的 index/offset/size 与 viewport end
  计算剩余距离；不能继续拿「第一个可见 item 的 size」当整段内容高度。
- `scrollToBottom` 定位最后一个 entry，不再对 index 0 传 `Int.MAX_VALUE / 2`。
- typing 尾项位于可滚动内容末尾、Composer 上方；其出现/消失加入 tail signature。
- tail signature 至少包含最后消息 id、parts、status 和规范化 typing user ids，不能只看
  `lastMessage.text.length`。
- `ChatListScrollState` 记录内容变化前最后一次 near-bottom 状态；只有该状态为 true 时，
  新 delta/附件/typing 导致的高度变化才强制贴底。不得在内容增长后重新测量再决定，否则大 delta
  可能把用户从 sticky 区域推出并停止跟随。
- 空态在 `ChatMessageList` 外层 `Box` 中按可用区域居中，不再依赖列表项内的固定 `64.dp` padding。
- 不新增 `useComposerHeightForScrollOffset`：列表本身已经由 `Modifier.weight(1f)` 约束在 Composer 上方。
- 小型浮动按钮使用圆形 `Box` + `Text`/本地 `Image` 组合；不使用 Kuikly 不支持的通用
  `Icon`，也不依赖当前存在已知问题的 fade `AnimatedVisibility`。

## 验收标准

- 超过配置阈值后出现 FAB 式回底按钮；未读数使用独立角标，点击贴底并清空 `unreadCount`。
- 用户手动滚回近底区域时也清空 `unreadCount`，与现有 Controller 语义一致。
- `isLoadingEarlier = true` 时顶部显示 `CircularProgressIndicator`；空闲且仍有历史时保留可点击入口。
- 开启日期分隔并注入 formatter 后按其业务时区显示；关闭配置和未知时间均不显示；
  日期边界不会继续合并气泡。
- typing 支持 0/1/N 个用户；当前用户与重复 ID 被过滤，默认内容和 slot 收到相同规范化列表。
- typing 出现时仅在原本近底的情况下保持贴底，阅读历史时位置不变化。
- 前插发生在 offset=0、消息高度不一或包含日期分隔净变化时，仍按稳定 key 保持同一可视 entry。
- 大段流式文本、附件加载后增高与 typing 出现时均遵循「变化前是否 near-bottom」。
- `commonTest` 覆盖 entry 构建、时间/日期 formatter、日期边界分组、typing 规范化、保留 key、阈值判断、
  offset=0 anchor key 重定位及 anchor 删除 fallback。
- 按 `AGENTS.md` 三条门禁验证，并补跑 JS 编译；人工项写入
  `docs/plans/compose-ui-acceptance.md`。

## 兼容性

`ChatSlots`、`ChatScreenConfig` 都是公开 `data class`，增加构造参数会改变 `copy/componentN`
及构造器 ABI；`ChatScreen` 新增末尾默认参数也会改变函数 ABI。与主题/slots 分层一起按
major 版本交付，不能通过一个会产生调用歧义的平行 overload 假装兼容。

## 依赖
- `feature-kuikly-chat-ui-sdk-redesign.md`（✅ 列表滚动基础已有；本需求修正单一 lazy item 限制）
- `feature-compose-message-time-status.md`（✅ `createdAt` 写入链路已实现）
- `feature-compose-theme-and-slots.md`（同步新增 slot 对照）
- `ChatClock`（✅ commonMain 可注入 now）；首期不新增未验证 OHOS 的日期三方依赖。

---
> 参考：`flutter-chat-ui-components.md` §6.8–6.11、§13；KuiklyUI
> `docs/Compose/list-and-scroll.md`（`items` + stable key）、
> `compose/.../foundation/lazy/LazyListState.kt`（按 key 保持首个可见项）
