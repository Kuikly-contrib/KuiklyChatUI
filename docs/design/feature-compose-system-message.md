---
last_updated: 2026-08-05
status: implemented
owner: @kuikly-chat-team
---

# Feature: Compose Chat 系统消息（System Message）

## Status: ✅ Implemented

## 目标
为 Compose Chat 增加专用的居中系统提示条（如「用户加入会话」「消息已撤回」），
对齐 Flutter `FlyerChatSystemMessage`（见 `flutter-chat-ui-components.md` §10.5），
并保证它不会被当成 assistant 普通气泡或协议 system prompt。

## 非目标
- 不把 LLM/provider 的隐藏 system prompt 自动展示给用户；只有 adapter 明确产生的 UI notice 才渲染。
- 不把 Tool、Reasoning、Activity 等结构化 AG-UI 内容统一降级为系统条；已有
  `agUiKind` / AgentCard 映射优先。
- 不实现未读分隔线（「以下为新消息」可与 `feature-compose-list-chrome.md` 合并设计，本需求仅系统文案条）。
- 不提供发送、重试、头像、昵称或消息状态操作。

## 现状与问题

- Legacy 模型已有 `MessageType.SYSTEM`、`ChatMessageHelper.createSystemMessage()` 和传统 Kuikly DSL
  `SystemMessageRenderer`。
- `AgUiChatMessageMapper.toComposeChatMessage()` 当前只区分 `CUSTOM/agUiKind` 与普通文本，
  因此 plain legacy `SYSTEM` 会变成 assistant 的 `MessagePart.Text` 并显示普通气泡。
- Compose `MessageRendererRegistry` 只识别 `AgentCardContent` / `Custom.type`；
  用 `Custom(type = "system")` 会与宿主自定义 key 冲突，也无法提供类型安全。
- `ChatMessage.groupStatus()` 目前只比较作者和时间。若系统消息复用同一 author，
  其前后普通消息可能被错误合并。

## 技术方案

### 涉及的模块
- `chat/compose/core/ChatMessage.kt`：新增 `MessagePart.SystemNotice` 与分类 helper。
- `chat/compose/adapter/AgUiChatMessageMapper.kt`：映射 plain legacy `MessageType.SYSTEM`。
- `chat/compose/ui/SystemMessageContent.kt`：默认居中提示条。
- `chat/compose/ui/ChatMessageList.kt`：在普通消息行之前处理纯系统 notice。
- `chat/compose/ui/TextBubbleContent.kt`：为 malformed mixed-parts 提供不丢内容的文本 fallback。
- `chat/compose/ui/ChatSlots.kt`：新增专用 `systemMessageContent`。
- `shared/.../ComposeChatSdkDemoPage.kt`：加入普通系统提示及相邻分组示例。

### 数据模型

```kotlin
sealed interface MessagePart {
    // 现有类型保持不变
    data class SystemNotice(val text: String) : MessagePart
}

internal val ChatMessage.systemNoticeOrNull: MessagePart.SystemNotice?
    get() = parts.singleOrNull() as? MessagePart.SystemNotice

internal val ChatMessage.containsSystemNotice: Boolean
    get() = parts.any { it is MessagePart.SystemNotice }
```

使用 `SystemNotice` 而不是含义模糊的 `System`，明确它是可见 UI 内容，不是 provider 指令。
专用 part 是已确定方案，不再保留 `Custom(type = "system")` 二选一。

标准系统消息必须只包含一个非空 `SystemNotice`。宿主直接构造 malformed mixed-parts 时，
列表不得吞掉其他内容：它回退普通消息行，`TextBubbleContent` 将 notice.text 当普通文本显示；
但 `containsSystemNotice` 仍使该消息不参与分组。

`ChatMessage.author` 现阶段仍是必填字段。adapter 使用稳定内部用户
`ChatUser(id = "__system__")` 填充，默认系统 UI 不展示该用户；本需求不为此扩大
`ChatMessage` 的 author 可空范围。

### Adapter 规则

`AgUiChatMessageMapper` 按以下顺序规范化 legacy 消息：

1. `MessageType.CUSTOM` 或带 `agUiKind` 的内容继续映射 `MessagePart.Custom` /
   AgentCard，避免 Tool/Reasoning 卡片退化。
2. plain `MessageType.SYSTEM` 且 content 非空时映射唯一 `SystemNotice`。
3. 其他类型维持各自 Text/Attachment 映射。

协议层的 `AgUiRole.SYSTEM` 是否可见仍由 `AgUiSession` 决定；`compose/ui` 不读取角色字符串。

### UI、slot 与路由优先级

```kotlin
data class ChatSlots(
    // 现有字段保持不变
    val systemMessageContent: (
        @Composable (
            message: ChatMessage,
            notice: MessagePart.SystemNotice,
        ) -> Unit
    )? = null,
)
```

- 整消息 `MessageRendererRegistry` 已命中时优先，避免 mixed Custom/AgentCard 被系统分支截断。
- `systemNoticeOrNull != null` 时调用专用 slot 或默认 `SystemMessageContent`，
  并绕过 `messageContent`、`messageRowContent`、头像、昵称、retry 和 TimeAndStatus。
- 纯系统 notice 应在 `rememberResolvedMessage` 之前按 raw message 分类，不能为内部 system user
  发起 `ChatUserResolver` / `UserCache` 请求。
- 默认 UI 使用全宽容器居中、内容宽度自适应、小字号、语义背景/文字色和圆角；
  文本可换行，不能固定单行导致长通知截断。
- 默认系统条不注册点击；宿主需要交互时在 `systemMessageContent` 中自行处理。
- 列表 key 仍使用 `message.id`，不额外拼接 index。

### 分组、滚动与未读

- `ChatMessage.canGroupWith()` 遇到任一 `containsSystemNotice` 时返回 false，
  因而系统消息自身为 `SINGLE`，其前后普通消息也被分隔。
- 系统条是正常列表内容，会改变尾部高度并参与 sticky-bottom。
- 系统 notice 默认不增加 `unreadCount`；新增统一 `countsAsUnread` 分类 helper 后，
  Controller/外部 bridge 必须使用同一规则。未读分隔线仍不在本需求范围。
- `MessageStatus` 与 `createdAt` 可保留用于排序/审计，但默认系统条不显示发送状态或时间。

## 验收标准

- Demo 可插入普通系统 notice，居中显示且不出现头像、昵称、时间、状态或 retry。
- 同一作者的两条普通消息被系统 notice 隔开后，两侧均重新计算为独立分组。
- plain legacy `MessageType.SYSTEM` 映射 `SystemNotice`；带 `agUiKind` 的 reasoning/tool
  仍走已有 Custom/AgentCard 路径。
- malformed mixed-parts 不崩溃、不吞文本或附件，也不与相邻消息分组。
- 新系统 notice 在用户阅读历史时不抢滚动、不增加 unread；原本近底时保持 sticky-bottom。
- `commonTest` 覆盖 adapter 优先级、纯 notice 分类、mixed fallback、分组边界和 unread 分类。
- 按 `AGENTS.md` 三条门禁验证，并补跑 JS 编译及 Android/H5 长文本人工验收。

## 兼容性

向公开 sealed `MessagePart` 增加子类型会使宿主 exhaustive `when` 需要重新编译；
所有仓库内 `when (part)` 必须同步增加分支。向公开 `ChatSlots` 增加字段也会改变 data class ABI，
与主题/slots 分层一起按 major 版本交付。

## 依赖

- `feature-compose-message-row.md`（✅；系统 notice 明确绕过默认行布局）。
- `feature-compose-theme-and-slots.md`（统一系统消息 token 与 slot）。
- `feature-compose-list-chrome.md`（复用统一列表 entry/尾部滚动语义，不建立未读分隔线依赖）。

---
> 参考：`FlyerChatSystemMessage`、`Message.system()`
