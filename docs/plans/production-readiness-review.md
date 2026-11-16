---
last_updated: 2026-11-14
status: active
owner: @kuikly-chat-team
---

# KuiklyChatUI 生产级组件调研与改进建议

本文对比成熟聊天组件的通用形态（Flyer Chat、Flutter AI Toolkit、生产 AI 聊天
UI 清单）与当前仓库实现，按「必须补齐、应该增强、可以观察」三档给出改进项。
每个条目都附了当前实现位置或证据，后续落地时逐条更新状态即可。

## 结论

当前组件的分层是对的：`ChatController` 统一消息状态，Compose 与 Kuikly DSL
两套 UI 复用同一 Controller；AG-UI / LLM provider 在 UI 外部注入；附件上传、
媒体缓存和用户目录都走宿主注入边界。这符合生产级 chat 组件「数据协议与 UI
解耦」的基本要求。

距离生产级主要差在三类能力：

1. **可靠性闭环仍需增强**——发送队列、离线恢复、流式部分内容状态和草稿上收
   已在 2026-11-14 落地；AG-UI 断线续传的幂等合并仍待协议层接入。
2. **两套 UI 的一致性和质量不对齐**——Compose 有 LazyColumn + stable key +
   历史锚点，Kuikly DSL 的 diff 优化和滚动锚点还在基础 `vfor` 层面；输入框
   的草稿、光标和多行行为刚经历过一次高度跳变修复。
3. **一致性和可维护性仍有技术债**——双 controller 入口文档已收敛，但状态
   仍未合并；结构化日志 hook 与两套 UI 可访问性基线已落地。

## 分层对比

| 生产组件需要的能力 | 当前状态 | 证据 | 结论 |
|-----|-----|-----|-----|
| 统一状态源 + UI 不持协议 | `ChatController` 为两套 UI 共享；AG-UI 转换在 `agui/` 层 | `ChatController.kt`、`ChatState.kt`、`feature-agui-protocol.md` | ✅ 保留 |
| 消息状态机 | `SENDING / STREAMING / SENT / FAILED / CANCELLED`，有错误重试 | `ChatMessage.kt`、`ChatController.kt` | ✅ 基本符合 |
| 发送与恢复 | `queuedMessages` + `ChatConnectionState`，发送中/断线自动排队，恢复按序重发 | `ChatController.kt`、`ChatControllerStream.kt` | ✅ 已落地，协议层续传待接入 |
| 流式渲染 | `TextDelta` / `ReplaceMessage` / `Completed` / `Failed`；失败保留文本并标记 `STREAMING_PARTIAL` | `ChatStreamEvent`、`ChatControllerStream.kt` | ✅ 已落地，幂等续传合并待接入 |
| 滚动策略 | Compose 有 stable key、sticky-bottom、回底按钮、历史锚点 | `ChatMessageList.kt` | ✅ Compose 基本符合 |
| 输入区 | `TextArea` 实测换行 + 固定按钮；草稿和附件保存在 `ChatController` | `ChatDockComposerPanel.kt`、`ChatComposer.kt`、`ChatState.kt` | ✅ 已落地 |
| 附件 | 附件数据模型 + picker 注入 + cross cache | `LlmAttachment`、`ChatCrossCache` | ✅ 边界正确 |
| 多会话 | `ChatConversationDirectory` 宿主持久化 | `ChatConversationDirectory.kt` | ✅ 边界正确 |
| 可观测性 | `ChatLogger` / `ChatLogLevel` 可由宿主注入，发送链路输出结构化事件 | `ChatState.kt`、`ChatControllerStream.kt` | ✅ 已落地 |
| 可访问性 | Compose 与 DSL 消息、发送/停止、附件、错误、加载状态已补语义 | `ChatMessageGestures.kt`、`ChatPressable.kt`、`ChatControllerSessionRow.kt`、`ChatDockComposerPanel.kt` | ✅ 已落地，听感验收待执行 |

## 改进建议

### P0：影响用户信任和基本可靠性

#### 1. 发送队列 + 离线恢复（已实现）

**2026-11-14 结果**：发送中和断线时自动进入 `sendQueue`，`queuedMessages` 供 UI 展示；恢复连接后按序重发，不产生重复消息。手动重试时清掉重复队列项并恢复连接态。

**建议**：

```kotlin
public enum class MessageDelivery {
    SENDING, SENT, FAILED, CANCELLED, QUEUED
}

public data class ChatState(
    // ...
    val queuedMessages: List<ChatMessage> = emptyList(),
    val connectionState: ConnectionState = ConnectionState.CONNECTED,
)
```

`ChatController` 内部维护一个发送队列，在 `connectionState == CONNECTED` 时
逐个发出；断网或 sender 抛 `NETWORK_ERROR` 时自动转 `QUEUED`。UI 只需要
展示「待发送」状态，不需要自己管理队列。

**验收**：

- 断网时连续发送 3 条 → 全部进队列、UI 显示 3 条「待发送」。
- 网络恢复后自动按序发出，不产生重复消息。
- 用户可以手动删除队列中某一条。
- `retryMessage` 仍然适用于 `FAILED` 消息，与队列解耦。

#### 2. 流式部分内容保护 + 断线续传（部分实现）

**2026-11-14 结果**：流失败或用户停止时保留已收文本，消息状态转为 `STREAMING_PARTIAL`，`partialMessageIds` 供 UI 显示「内容不完整」；手动重试会清掉旧 partial 标记。AG-UI snapshot 的 messageId 幂等合并仍未实现。

**建议**：

```kotlin
public enum class MessageStatus {
    // ...
    STREAMING_PARTIAL  // 新增：有内容但不完整
}

public data class ChatState(
    // ...
    val partialMessageIds: List<String> = emptyList(),
)
```

流失败或用户主动 `StopGeneration` 时：

- 保留已收到的 `TextDelta` 内容，不删除。
- 状态转 `STREAMING_PARTIAL`，UI 显示「内容不完整」标签。
- AG-UI 断线重连走 `MESSAGES_SNAPSHOT`，由协议层把快照和本地已收到的
  `TextDelta` 做幂等合并（以 messageId 为 key）。
- 宿主可以选择丢弃或继续，SDK 不替宿主做业务决策。

**验收**：

- 流式中途断开 → 已收到的文本保留，UI 显示部分内容提示。
- 重连后 AG-UI snapshot 到达 → 不丢已有文本，不重复追加。
- 用户点停止 → 状态转 `STREAMING_PARTIAL`，不是 `SENT`。

#### 3. 输入草稿放到 ChatController（已实现）

**2026-11-14 结果**：Compose 与 Kuikly DSL 的输入文本和附件都读写 `ChatState.draftText` / `draftAttachments`，发送成功后由 Controller 统一清空；`clearDraft()` 支持宿主主动清理。

**建议**：把 draft 从 UI 层移到 `ChatController`：

```kotlin
public class ChatController {
    public fun setDraft(text: String)
    public fun setPendingAttachments(attachments: List<MessagePart.Attachment>)
    public fun setDraftFocus(focused: Boolean)
}

public data class ChatState(
    // ...
    val draftText: String = "",
    val draftAttachments: List<MessagePart.Attachment> = emptyList(),
)
```

两套 UI 的 composer 只订阅 `ChatState.draftText`，不再各自持有本地草稿。
发送成功后 `ChatController` 统一清空，UI 不再重复写逻辑。

**验收**：

- Compose 输入一半后切到另一会话再切回，草稿保留。
- Kuikly DSL 输入一半后页面旋转/重建，草稿恢复。
- 两套 UI 同时使用时草稿状态一致。

#### 4. 发布前最低生产验收清单（文档已补齐）

**2026-11-14 结果**：`docs/plans/compose-ui-acceptance.md` 已扩展为真实网络下 Android / iOS / 鸿蒙的最低验证集，覆盖队列恢复、长会话、附件、草稿和屏幕阅读器。

**建议**：按下面的 checklist 发布前过一遍，每条留截图或录屏证据。

- 16 条消息（含不同长度中文/数字/代码块）逐条流式渲染。
- 1000 条历史消息加载不卡顿。
- 8 条待发消息进队列，断网/恢复不影响顺序。
- 流式中途断网 → 已收到内容保留。
- iOS / Android / 鸿蒙各跑一次真实网络联调。
- 每个会话切换 10 次后草稿不丢。
- 附件发送、预览、删除、重试各一次。
- 键盘弹出时列表不遮挡输入区，收起后不残留底部 inset。
- 暗色模式切换一次（有暗色主题时）。
- 每条消息和按钮用系统屏幕阅读器朗读一次。

### P1：一致性、输入质量和可观测性

#### 5. 统一 LlmChatController 和 ChatController（文档已收敛）

**2026-11-14 结果**：README 和 chat-ui 文档已明确推荐 `ChatController` + `ChatMessageSender`；`LlmChatController` / `rememberLlmChatController()` 标记为历史便捷封装，不建议与新 UI 混用。`LlmProvider` 保持 provider 能力层定位。状态合并和 `ChatProviderAdapter` 拆分仍是后续技术债。

#### 6. 结构化日志 / 埋点 hook（已实现）

**2026-11-14 结果**：`ChatLogger` / `ChatLogLevel` 已进入 Core，`ChatController` 支持 logger 注入；当前发送链路输出 `send_started` / `stream_first_token` / `send_completed` / `send_failed` / `stream_cancelled`，attributes 只含 `message_id`，不含消息原文或附件 URI。

**建议**：

```kotlin
public interface ChatLogger {
    public fun log(
        level: ChatLogLevel,
        event: String,
        attributes: Map<String, String>,
        throwable: Throwable? = null,
    )
}

public class ChatController(
    // ...
    public val logger: ChatLogger? = null,
)
```

埋点事件至少覆盖：

- `send_started` / `send_completed` / `send_failed`
- `stream_first_token` / `stream_completed` / `stream_cancelled`
- `history_loaded` / `load_earlier_completed`
- `attachment_added` / `attachment_removed`
- `conversation_switched`

**验收**：宿主注入一个 fake logger 后，单测能按序拿到上述事件；
文档说明 `attributes` 里禁止放消息原文和附件 URI。

#### 7. 可访问性基线（已实现）

**2026-11-14 结果**：Compose 已补消息气泡、发送/停止、附件、移除附件、失败重试和加载更早语义；Kuikly DSL 已补消息气泡描述、发送/停止与附件按钮角色和可点击信息、错误横幅、加载更早/加载中状态，并保留回底按钮。屏幕阅读器真机/模拟器听感仍按验收清单人工执行。

**建议**：

- Compose：给每条消息 bubble 加 `contentDescription`（作者 + 状态 + 内容摘要）。
  发送按钮加 `send message`，停止加 `stop generating`，附件加 `add attachment`，
  错误横幅加 `send failed`，列表加 `chat transcript`。
- Kuikly DSL：给所有可交互节点补 `accessibility()`。消息 row 至少给 bubble
  或内容节点加标签；错误横幅和加载中加状态说明。
- 屏幕阅读器触发「新消息」时不要抢焦点，滚动到回底按钮不改变焦点。

**验收**：iOS VoiceOver / Android TalkBack 朗读一轮完整会话，用户能分辨
「我发的」「对方发的」「失败了」「生成中」。

#### 8. 跨端一致性测试（已实现基础契约）

**2026-11-14 结果**：Compose `statusLabel` 已覆盖 FAILED 映射为重试；Kuikly DSL 侧新增共享状态文案契约测试，对齐发送中 / 生成中 / partial / 重试 / 取消文案。快照 diff 已有 by-id 更新契约测试。时间格式和附件源的完整渲染对照仍待后续补齐。

**建议**：

- 在 `commonTest` 中抽出一层「渲染期望」：给定消息 + 主题 + 状态，期望两套
  UI 的 `statusLabel`、`dateLabelBetween`、`bubbleText`、`imageSources` 等
  逻辑函数输出一致。
- 每个 P0/P1 UI 改动必须同时在两套 UI 中手工验证一次（不是只改其中一套）。
- 后续引入图像快照对比工具时，可以把 Kuikly DSL 输出图和 Compose 输出图做
  相似度对照（先不作为硬门禁）。

**验收**：跨端一致性测试在 `ci-gates` 里跑，同一输入在两套 UI 的关键输出
（状态文案、时间、附件源）一致。

#### 9. 输入区配置补齐（已实现）

**2026-11-14 结果**：Compose `ChatScreenConfig` 新增 `maxTextLength` 与 `autofocus`，输入变更先安全截断再写入 Controller；Kuikly DSL `ChatDockComposerConfig` 新增 `maxTextLength`，`updateDraft` 统一截断。当前采用保守的安全截断策略，宿主自定义截断策略暂未暴露。

**验收**：

- 设置 `maxTextLength` 后超长输入会被截断，不会破坏控制器状态。
- `autofocus = true` 时 Compose 输入框首次渲染请求焦点。

### P2：可维护性和性能

#### 10. 清理两个超标文件 + 调整文件行数门禁（已实现）

**2026-11-14 结果**：`RichCardPayload.kt` 的五个 Info parser 拆到 `RichCardPayloadInfo.kt`，`ChatPreviewData.kt` 的 `MockChatRepository` 拆到独立文件。两个源文件均低于 400 行，`scripts/file-size-baseline.txt` 已清空；public API 路径同步更新。backlog 中早前的过期文件条目也已核对清理。

**验收**：`bash scripts/check-layer-deps.sh` 通过；baseline 为空。

#### 11. Kuikly DSL 列表 diff 优化（已查证并落地）

**2026-11-14 结果**：查证 Kuikly `ObservableList.diffUpdate(newList) { old, new -> old.id == new.id }` 支持 Myers diff 且可按 id 判同。`ChatMessageSnapshotSync.applyMessageSnapshot` 在目标是 `ObservableList<ChatMessage>` 时改用 `diffUpdate`，普通 list 测试路径保留 by-id 更新。快照行为契约测试覆盖同 id 替换、插入、删除、无变化。设备掉帧性能验证仍需按验收清单执行。

#### 12. 公共 API 面收敛（文档分层已落地）

**2026-11-14 结果**：`docs/api/public-keep-set.md` 已明确 Stable 候选 / Evolving / Provider-Experimental / Internal SPI / internal 五级。`scripts/public-api-baseline.txt` 仍是全量 public dump 机械门禁；`ChatLogger`、`ChatConnectionState`、输入区配置等本轮新增 API 已登记。Stable 层的 binary ABI dump 与冻结开关仍待后续完成。

## 落地顺序建议

1. **先修影响信任的 P0**：
   - 发送队列 + 连接状态（`ChatState` + `ChatController` 队列）。
   - 流式 partial 保护。
   - draft 状态上收。
   - 跨端/真实网络验收 checklist。
2. **再补 P1 一致性**：
   - 统一 Controller 入口（先文档收敛，后代码收敛）。
   - logger hook + 可访问性基线。
   - 输入区 max length / autofocus / IME 截断。
3. **最后做 P2 技术债**：
   - 拆超标文件。
   - Kuikly DSL diff 优化（先查证再动代码）。
   - API 分层与 dump 强化。

这个顺序刻意把「用户能感知的可靠性」放在「代码结构完美」之前，避免技术债
清理优先级高于生产可用性。
