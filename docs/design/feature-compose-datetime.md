---
last_updated: 2026-08-07
status: implemented
owner: @kuikly-chat-team
---

# Feature: Compose Chat 跨端时间格式化与日期分隔

## Status: Implemented

## 目标

为 Compose Chat SDK 提供**可注入、跨平台**的时间展示能力，替代 `commonMain` 中手写的 UTC 取模逻辑，对齐 Flyer / 微信式 IM 的常见预期：

- 消息气泡旁时间按**设备本地时区**显示（默认 `HH:mm`）；
- 宿主可替换 formatter，统一控制消息时间、日期分隔文案与业务时区；
- 写入侧继续用 `ChatClock` 产出 epoch millis，展示侧与写入侧解耦；
- 依赖腾讯补丁版 `kotlinx-datetime`，覆盖 Android / iOS / macOS / JS / OpenHarmony；
- 日期分隔对齐 Flyer v2 **`ChatMessage.headerWidget`** 模式：渲染时比较 `createdAt`，跨自然日插入 header，**不改写消息列表数据**。

## 非目标

- 不在 SDK 内初始化 Android `AndroidThreeTen` 或鸿蒙 `@kuiklybase/timezone`（宿主应用职责，见下文）。
- 不引入 `java.time` 或平台专用日期 API 到 `commonMain`。
- 不支持任意 `DateFormat` 模式字符串；P0 仅保证气泡时间 `HH:mm`，其余模式回退到 `HH:mm`。
- 不实现 Legacy `ChatSession` 的「间隔 ≥ 5 分钟即插分隔条」逻辑（Compose 按**日历日**判断，见下文）。
- 日期分隔暂不拆成独立 `LazyColumn` entry（见 `feature-compose-list-chrome.md` 后续懒加载项）；当前挂在消息行上方，语义等同 Flyer `headerWidget`。

## 宿主需求分析

| 场景 | 宿主期望 | SDK 提供 |
|------|----------|----------|
| 默认聊天页 | 气泡时间显示本地 `14:30` | `LocalChatDateTimeFormatter`（`TimeZone.currentSystemDefault()`） |
| 国际化 / 固定时区 | 按业务时区或 locale 格式化 | `LocalChatDateTimeFormatter.withTimeZone("Asia/Shanghai")` 或自定义 `ChatDateTimeFormatter` |
| 与旧版 UTC 行为一致 | 不因升级突然变时区 | `UtcChatDateTimeFormatter` + `ChatScreenConfig(dateTimeFormatter = …)` |
| 单测 / 回放 | 固定「现在」与固定时区 | `ChatClock { … }` + `LocalChatDateTimeFormatter.withTimeZone("UTC")` |
| 日期分隔 | 「今天 14:30」「昨天 09:00」 | `showDateSeparators = true` + `dateSeparatorLabel()` |
| 消息写入 | `createdAt` 为 epoch millis | `ChatClock.System`（发送 / Upsert 默认） |
| 对话持久化 | 历史消息带服务端时间戳 | 宿主 DB / `ChatHistoryLoader`；SDK 不缓存格式化结果 |

**原则**：`createdAt` 始终存 **UTC epoch millis**；展示层通过 formatter 转本地或业务时区。用户切换时区后只需换 formatter 或触发重组，**不必重写历史 `createdAt`**。

## 技术方案

### 依赖

```kotlin
// KuiklyChat/build.gradle.kts — commonMain（随 SDK 传递，宿主一般无需重复声明）
implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0-RC.2-KBA-003")
```

仓库（根 `settings.gradle.kts`）：

```kotlin
maven { url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/") }
```

### 平台初始化（宿主）

| 平台 | 要求 |
|------|------|
| Android（API &lt; 26 等） | 应用 `Application.onCreate` 调用 `AndroidThreeTen.init(application)`，并依赖 `threetenabp` |
| OpenHarmony | `package.json` 添加 `@kuiklybase/timezone`，入口 `import "@kuiklybase/timezone"` |
| iOS / macOS / JS | 无额外 SDK 初始化 |

KuiklyChat 库**不**打包或调用上述初始化，仅在集成文档中说明。

### 核心 API

**`compose/core/ChatDateTimeFormatter.kt`**

```kotlin
interface ChatDateTimeFormatter {
    fun formatMessageTime(timestampMillis: Long, pattern: String = "HH:mm"): String
    /** 返回 null 表示当前消息前不显示日期 header。 */
    fun dateSeparatorLabel(
        previousCreatedAt: Long?,
        currentCreatedAt: Long,
        nowMillis: Long,
        strings: ChatStrings,
    ): String?
}
```

| 实现 | 用途 |
|------|------|
| `LocalChatDateTimeFormatter.Default` | 设备本地时区（默认） |
| `LocalChatDateTimeFormatter.withTimeZone(id)` | 固定业务时区 |
| `UtcChatDateTimeFormatter` | 兼容旧 UTC 气泡时间 |

**`dateSeparatorLabel` 默认文案规则**（本地时区下）：

| 条件 | 示例 |
|------|------|
| 与上一条同一天 | 不显示（`null`） |
| 首条消息 / 跨日 | 显示 |
| 当天 | `今天 HH:mm` |
| 昨天 | `昨天 HH:mm` |
| 同年更早 | `MM-dd HH:mm` |
| 跨年 | `yyyy-MM-dd HH:mm` |

### 配置与 UI 接入

```kotlin
ChatScreen(
    config = ChatScreenConfig.Default.copy(
        showDateSeparators = true,  // 默认 false
        dateTimeFormatter = LocalChatDateTimeFormatter.Default,
        dateSeparatorClock = ChatClock.System,
        timeFormat = "HH:mm",
    ),
    slots = ChatSlots.Default.copy(
        dateSeparatorContent = { timestampMillis, label ->
            // 可选：完全自定义样式；默认 ChatDateSeparator 居中圆角标签
        },
    ),
)
```

### 渲染链路（对齐 Flyer `headerWidget`）

```text
ChatMessageListItem
  ├─ resolveDateSeparatorLabel(previous, current)  // 纯函数，不修改 ChatController
  ├─ ChatMessageDateHeader / dateSeparatorContent  // 等同 Flyer headerWidget
  └─ ChatMessageRow / SystemMessage / CustomRenderer

跨日时 breaksMessageGroupingAtDateBoundary → groupStatus 的 previous/next 置 null
（避免午夜前后气泡仍被合并为同组）
```

`MessageTimeAndStatus` 使用 `config.dateTimeFormatter.formatMessageTime(createdAt, config.timeFormat)`。

顶层 `formatMessageTime()` 保留，委托 `UtcChatDateTimeFormatter`，供遗留测试与显式 UTC 调用。

### 与 Flyer 对照

| Flyer v2 | KuiklyChat Compose |
|----------|-------------------|
| `Chat.timeFormat: DateFormat?` | `dateTimeFormatter` + `timeFormat` |
| `ChatMessage.headerWidget` + 宿主比较 `createdAt` | `showDateSeparators` + `ChatMessageDateHeader` |
| `DateTime.now().toUtc()` 写入 | `ChatClock` + `createdAt` millis |
| 不持久化格式化字符串 | 同上，每次渲染即时计算 |
| `messagesGroupingMode` / 超时窗口 | `groupingWindowMillis`（气泡分组，与日期分隔独立） |

### 涉及文件

| 文件 | 职责 |
|------|------|
| `compose/core/ChatDateTimeFormatter.kt` | Formatter 接口与默认实现 |
| `compose/core/ChatClock.kt` | 写入 `createdAt` |
| `compose/ui/ChatDateSeparator.kt` | 日期 header UI 与判断辅助函数 |
| `compose/ui/ChatMessageListItem.kt` | 挂载 header、日期边界打断分组 |
| `compose/ui/MessageTimeAndStatus.kt` | 气泡旁时间 |
| `compose/ui/ChatSlots.kt` | `showDateSeparators`、`dateSeparatorClock`、`dateSeparatorContent` |
| `compose/core/ChatTheme.kt` | `dateSeparatorBgColor` / `dateSeparatorTextColor` |
| `compose/core/ChatStrings.kt` | `today` / `yesterday` |

## 验收标准

- `ChatScreenConfig.Default` 使用本地时区 formatter；`timeFormat = "HH:mm"` 在已知 instant + 时区下单测可验证。
- `UtcChatDateTimeFormatter` 行为与迁移前 `formatMessageTime` 一致（含 `createdAt <= 0` 返回空串）。
- `showDateSeparators = true` 时，跨自然日显示 header；同日不显示；首条消息显示。
- 日期边界打断 `MessageGroupStatus`（跨日同作者不合并气泡）。
- `commonTest`：`ChatDateTimeFormatterTest`、`ChatDateSeparatorLogicTest`。
- 门禁：`check-layer-deps.sh`、`:KuiklyChat:compileDebugKotlinAndroid`、`:KuiklyChat:testDebugUnitTest`。

## 兼容性

- **行为变更**：默认 formatter 由隐式 UTC 取模改为**本地时区** `HH:mm`。需 UTC 的宿主设置 `dateTimeFormatter = UtcChatDateTimeFormatter`。
- `showDateSeparators` 默认 `false`，不影响现有页面。
- `ChatScreenConfig` / `ChatSlots` 新增字段属公开 data class 构造参数扩展。

## 依赖

- `feature-compose-message-time-status.md`（✅ 气泡时间 UI）
- `feature-compose-list-chrome.md`（📋 独立 LazyColumn entry / typing 等待办）
- `kotlinx-datetime:0.6.0-RC.2-KBA-003`（腾讯 Maven，含 OHOS 补丁）

---
> Flyer 对照：存 UTC 时刻、展示时格式化；日期 header 由宿主配置开启，SDK 提供默认判断与 UI。
