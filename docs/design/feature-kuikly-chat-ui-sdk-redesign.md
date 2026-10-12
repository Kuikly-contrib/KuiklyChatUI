---
title: Kuikly Chat UI SDK 重构设计：Compose DSL 优先
status: in_progress
last_updated: 2026-09-23
---

# Kuikly Chat UI SDK 重构设计：Compose DSL 优先

- **状态**：🔄 Compose DSL 首期开发中
- **版本**：v2.1
- **日期**：2026-08-03
- **优先级**：P0
- **首要交付物**：Compose DSL Chat UI SDK
- **后续交付物**：Kuikly DSL 渲染层迁移

## 当前进度（2026-08-03）

当前已从纯设计进入 **Compose DSL Phase 1–3 完成阶段**。P0 列表、发送闭环与 Scripted AG-UI 端到端已落地：

- ✅ Core：`ChatMessage`、`MessagePart`、`MessageStatus`、`ChatState`、`ChatIntent`、`ChatEffect`、`ChatController`（含 `unreadCount`、历史首屏加载、retry 语义修正）。
- ✅ 基础 UI：`ChatScreen`、`ChatMessageList`、`ChatMessageItem`、sticky-bottom、历史锚点、回底按钮、扩展 `ChatSlots`。
- ✅ `ChatEffect` 统一消费：`ChatEffectHandler`（滚动、错误条、剪贴板 callback）。
- ✅ Provider 适配：`LlmChatAdapter`、`rememberLlmChatUiController`（可选 `historyLoader`）。
- ✅ AG-UI 适配：`AgUiComposeBridge`、`AgentCardComposeBridge`、`ScriptedSseLlmTransport`、`AgUiLlmProvider` 端到端集成测试。
- ✅ Demo：`ComposeChatSdkDemoPage` 通过同一条 SSE 流驱动文本 + Tool Call，不再 `LaunchedEffect` 伪造事件。
- ✅ 验证：174 个 Android 单测、`KuiklyChat`/`shared` JS 编译、架构门禁通过。
- 📋 Phase 4 未开始：Markdown、图片/文件、Composer 附件；真实生产 AG-UI 网络联调留宿主接线。


## 1. 背景与目标

当前 KuiklyChat 已有一套 Chat UI SDK，但组件职责、会话状态、消息模型和渲染实现耦合较深，实际效果与可复用性不足；同时项目需要补齐一套 Compose DSL 实现。若直接在现有组件上继续叠加功能，容易把已有问题复制到第二套渲染层。

本次不以“把现有页面改成 Compose”为目标，而是重新建立一套**数据驱动、可替换渲染、可测试、可渐进迁移**的 Chat UI SDK：

1. **先完成 Compose DSL**：以 Kuikly Compose DSL 为唯一首发渲染实现，优先保证视觉、交互和状态行为完整。
2. **对照 Flutter Chat UI 的产品抽象**：借鉴其 `ChatController`、消息操作、消息分组、Builder/slot、自定义主题和 Composer 分层，但不复制 Flutter API。
3. **协议与 UI 解耦**：AG-UI 仅负责事件解析与会话编排，Chat UI 只消费通用消息状态。
4. **为 Kuikly DSL 保留迁移边界**：Compose 与传统 Kuikly DSL 共享模型、状态、用例、主题语义和组件契约；两套实现不共享渲染代码。
5. **首期控制范围**：先交付文本消息、消息列表、气泡、输入区、发送/重试/停止、加载更多和基础主题；复杂卡片与高级媒体能力后置。

### 1.1 非目标

本阶段不做以下事项：

- 不将 AG-UI 事件类型直接暴露给 Compose UI。
- `KuiklyAiChatView` 与旧 `ChatSession` 已从代码删除。现行入口是 `ChatController` + `ChatScreen`，以及同一控制器的 `ChatControllerSession`。
- 不先实现传统 Kuikly DSL 再反向适配 Compose。
- 不在首期承诺完整媒体编辑、语音通话、富文本编辑器、复杂动画和所有 Flutter 插件能力。
- 不为了兼容旧 API 而让新核心模型承担旧模型的全部历史包袱；兼容适配放在边界层。

## 2. 设计原则

### 2.1 四层职责

```text
宿主页面 / Compose DSL
          ↓ 用户意图与渲染
Chat UI Presentation（ChatScreen、MessageList、Composer）
          ↓ 读取状态、派发 UiIntent
Chat UI Core（ChatState、ChatController、MessageRepository）
          ↓ 通用消息与会话能力
Transport / AG-UI Adapter（事件解析、请求、流式增量）
```

- **Presentation**：只处理布局、交互和可视状态，不解析 AG-UI 事件。
- **Core**：维护单一真相源，处理消息增删改、发送、停止、重试、分页和滚动意图。
- **Transport / Adapter**：将网络响应、AG-UI 事件或宿主自定义数据转换成 Core 可消费的操作。
- **宿主层**：注入用户信息、发送策略、导航、业务卡片和平台能力。

### 2.2 数据单向流动

```text
MessageSource / UserIntent
          ↓
ChatController.dispatch(intent)
          ↓
ChatState（不可变快照）
          ↓
ChatScreen 重组
          ↓
ChatCallbacks / HostEffects
```

禁止 UI 组件直接修改消息列表，也禁止 Transport 直接持有 UI 引用。所有状态变更都通过 `ChatController` 或其内部 `ChatOperation` 完成。

### 2.3 Compose 优先、双 DSL 兼容

首期实现以下共享契约：

- `ChatMessage`、`ChatUser`、`MessagePart`、`MessageStatus`。
- `ChatState`、`ChatIntent`、`ChatEffect`。
- `ChatController`、`MessageRepository`、`MessageOperation`。
- `MessageGroupStatus`、`ChatTheme`、`ChatStrings`。
- `MessageRenderer`、`ComposerConfig`、`ChatCallbacks`。

Compose DSL 负责第一套渲染实现；后续 Kuikly DSL 只实现同一组契约对应的 View/组件，不改变 Core API。若两套 DSL 对某个能力支持度不同，必须在能力矩阵中显式标注，不允许静默改变语义。

## 3. Flutter Chat UI 对照与取舍

参考 Flutter Chat UI 的有效抽象：

| Flutter 抽象 | Kuikly Chat UI 对应设计 | 取舍 |
|---|---|---|
| `User` | `ChatUser` | 保留 `id`、`name`、`avatar`、`metadata`；使用 Kotlin `data class` |
| 多态消息模型 | `ChatMessage` + `MessagePart` | 首期文本为主，附件、图片、卡片采用可扩展 part，不把 UI 类型写进模型 |
| `ChatController` | `ChatController` | 保留消息查询、插入、更新、删除、分页和监听；使用 Compose 可观察状态 |
| `ChatOperation` | `ChatIntent` / `MessageOperation` | 将用户意图和状态操作分开，便于 AG-UI 流式事件映射 |
| `ChatAnimatedList` | `ChatMessageList` | 首期采用稳定 key、倒序/底部锚定和增量更新；动画按 Kuikly 支持度后置 |
| `MessageGroupStatus` | `MessageGroupStatus` | 用于气泡圆角、头像和时间信息显示，避免渲染层重复推导 |
| Builder / slot | `ChatSlots` + `MessageRendererRegistry` | 用 Kotlin lambda/注册表表达，不照搬 Flutter Widget builder API |
| Composer | `ChatComposer` | 文本输入、发送、停止、附件入口和状态提示分层实现 |
| Theme | `ChatTheme` | 主题只描述语义 token，具体 Compose 组件样式由渲染层落地 |
| `Chat` 总组件 | `ChatScreen` | 组合消息列表、空态、加载态、Composer 和 overlay；保持可拆分和可替换 |

### 3.1 对 Flutter 能力的优先级

**P0（首期必须具备）**：

- 消息列表及稳定消息 key。
- 文本消息和基础自定义消息入口。
- 当前用户/其他用户的消息对齐和气泡样式。
- 连续消息分组、头像/时间显示策略。
- 发送、发送中、发送失败、重试、停止生成。
- 流式文本增量更新。
- 上拉加载历史消息、底部锚定、跳转到底部。
- Composer 文本输入、发送按钮、禁用/加载态。
- 空态、加载态、错误态、基础主题和国际化文案。

**P1（首期后）**：

- 图片/文件附件消息及预览。
- Markdown/代码块/链接点击。
- 引用回复、编辑、删除、复制、长按菜单。
- 输入区附件面板、草稿恢复、键盘适配优化。
- 消息列表进入/更新动画、未读分隔线。
- AG-UI Agent 卡片通过 `MessageRendererRegistry` 注册。

**P2（稳定性后）**：

- 语音/音频消息。
- 多频道和多人会话。
- 消息搜索、置顶、收藏、批量操作。
- 离线缓存、断线续传、发送队列。
- 更复杂的媒体编辑与平台原生能力。

## 4. 目标模块与目录

路径前缀：`KuiklyChat/src/commonMain/kotlin/com/tencent/kuiklybase/chat/`

```text
chat/
├── model/                         # 已有模型/主题/仓库能力，逐步收敛为共享核心
│   ├── ChatUser.kt
│   ├── ChatMessage.kt
│   ├── MessagePart.kt
│   ├── MessageStatus.kt
│   └── ChatTheme.kt
├── core/                          # 新增：与渲染 DSL 无关的会话状态和操作
│   ├── ChatState.kt
│   ├── ChatIntent.kt
│   ├── ChatEffect.kt
│   ├── ChatController.kt
│   ├── MessageRepository.kt
│   └── MessageGroupCalculator.kt
├── transport/                     # 可选命名空间；AG-UI 适配仍归 agui
├── agui/                          # AG-UI 事件解析、流式增量和协议适配
├── presentation/
│   ├── compose/                   # 首期唯一渲染实现
│   │   ├── ChatScreen.kt
│   │   ├── ChatMessageList.kt
│   │   ├── ChatMessageItem.kt
│   │   ├── ChatBubble.kt
│   │   ├── ChatComposer.kt
│   │   ├── ChatStates.kt
│   │   └── ChatSlots.kt
│   └── kuikly/                    # 后续实现，不在首期交付
├── renderer/
│   ├── MessageRenderer.kt          # 与 DSL 无关的渲染注册契约
│   └── MessageRendererRegistry.kt
└── compatibility/                 # 旧 API → 新 Core 的过渡适配
```

当前代码已经落在以下实际路径（设计中的 `core/`、`presentation/compose/`、`renderer/` 为逻辑分层名）：

```text
chat/
├── compose/                     # 新 Compose DSL Chat UI SDK，唯一 Compose 入口
│   ├── core/                    # 状态、模型、Controller、Renderer 契约
│   ├── ui/                      # ChatScreen、消息列表、Composer、Slots
│   ├── adapter/                 # LLM、AG-UI、旧模型适配
│   └── agentcard/               # Compose DSL AgentCard 渲染器
├── agentcard/                   # AgentCard 共享模型 + 传统 Kuikly DSL 渲染
└── legacy/                      # 待迁移的传统 Kuikly DSL/旧 AI UI
    ├── composer/
    └── ai/
```

命名规则：看到 `compose/` 即表示新 Compose DSL；看到 `legacy/` 即表示待迁移旧实现；`composer` 专指传统 DSL 输入组件，不再与 Compose SDK 并列放在根目录。

现有 `compose/agentcard/` 和 `agentcard/` 不立即删除：

- `compose/agentcard/` 继续提供已有 17 种卡片的 Compose 渲染实现，由 `compose/adapter/AgentCardMessageRenderer.kt` 接入新 SDK。
- `agentcard/` 继续维护传统 Kuikly DSL 和共享 `AgentCard` 数据契约，实现与 Compose 卡片语义对齐。
- 新的基础文本消息归入 `compose/ui`，不再继续堆在 `compose/agentcard/`。
- `compose/adapter/` 是协议适配边界；`compose/core` 与 `compose/ui` 不直接依赖 AG-UI 事件类型。

### 4.1 文件约束

- 新文件保持不超过 400 行；一个文件只承担一个主要职责。
- Compose 页面组合与状态逻辑分离，避免出现新的巨型 `ChatView`。
- `commonMain` 不得引用 Android/iOS/Harmony 平台实现。
- 不使用裸 `println` 或 `console.log`；日志由现有结构化日志能力统一输出。

## 5. 核心领域模型

### 5.1 用户

`ChatUser` 至少包含：

- `id: String`：稳定唯一标识。
- `name: String?`：展示名称。
- `avatar: String?`：资源或 URL，由图片加载层解释。
- `metadata: Map<String, String>`：宿主扩展字段。

### 5.2 消息

`ChatMessage` 是 UI 无关的数据模型，建议包含：

- `id: String`：稳定 key，更新同一条消息时保持不变。
- `authorId: String`。
- `createdAt: Long`：跨平台时间戳。
- `parts: List<MessagePart>`。
- `status: MessageStatus`。
- `replyToId: String?`。
- `metadata: Map<String, String>`。

`MessagePart` 首期包括：

- `Text(text: String, format: TextFormat = Plain)`。
- `Attachment(id, uri, mimeType, name, size)`，首期只保留模型和占位渲染。
- `Custom(type, payload)`，用于兼容扩展消息和宿主自定义内容。
- `AgentCardContent(card)`，用于保留完整 `AgentCard` 类型并交给 Compose Renderer。

`MessageStatus` 至少包括：

- `Sending`
- `Streaming`
- `Sent`
- `Failed(errorCode, message)`
- `Cancelled`

不要把“当前用户/机器人”硬编码为消息类型；通过 `ChatUser` 与 `currentUserId` 判断消息方向。

### 5.3 会话状态

`ChatState` 为 Compose 消费的不可变快照，至少包含：

- `messages: List<ChatMessage>`。
- `currentUserId: String`。
- `isInitialLoading: Boolean`。
- `isLoadingEarlier: Boolean`。
- `hasMoreEarlier: Boolean`。
- `isSending: Boolean`。
- `composerText: String`（若采用受控 Composer）。
- `error: ChatError?`。
- `unreadCount: Int`。
- `lastMessageId: String?`。

如后续需要更高性能，可将列表存储替换为持久化/快照结构，但对 UI 暴露的仍是稳定可观察语义，不提前绑定具体集合实现。

## 6. Controller 与操作契约

### 6.1 Controller 职责

`ChatController` 是 SDK 的核心入口，不负责绘制 UI。它负责：

- 暴露可观察的 `ChatState`。
- 接收 `ChatIntent` 并串行化状态变更。
- 提供消息查询和稳定操作。
- 调用宿主注入的 `ChatMessageSender`、`HistoryLoader`。
- 将发送结果、流式增量和错误转换为状态。
- 管理生命周期，避免页面销毁后继续更新状态。

建议的意图集合：

- `SendText(text)`
- `RetryMessage(messageId)`
- `StopGeneration`
- `LoadEarlier`
- `Refresh`
- `DeleteMessage(messageId)`
- `EditMessage(messageId, text)`（P1）
- `CopyMessage(messageId)`（转为 Effect）
- `ClearError`

### 6.2 发送与流式更新

发送流程：

```text
SendText
  → 校验并创建本地 Sending 消息
  → 调用 ChatMessageSender
  → 创建/定位 assistant Streaming 消息
  → 应用增量 TextDelta
  → 完成后标记 Sent
  → 失败标记 Failed，保留 Retry 能力
```

AG-UI 的 `RUN_STARTED`、`TEXT_MESSAGE_CONTENT`、`RUN_FINISHED`、错误事件必须在 `agui/` 内转换为上述 Core 操作；`ChatMessageList` 不得感知这些协议事件。

### 6.3 Effect 与副作用

一次性事件不要塞入 `ChatState`，通过 `ChatEffect` 发出，例如：

- `ScrollToBottom`
- `RequestFocusComposer`
- `ShowError(message)`
- `CopyToClipboard(text)`
- `OpenAttachment(uri)`

这样可以避免 Compose 重组时重复执行复制、Toast 或滚动。

## 7. Compose DSL 首期设计

### 7.1 对外入口

目标 API 形态：

```kotlin
@Composable
fun ChatScreen(
    controller: ChatController,
    modifier: Modifier = Modifier,
    theme: ChatTheme = ChatTheme.Default,
    config: ChatScreenConfig = ChatScreenConfig.Default,
    slots: ChatSlots = ChatSlots.Default,
    callbacks: ChatCallbacks = ChatCallbacks.Empty,
)
```

这只是设计目标，实际命名以实现阶段的 Kotlin/Kuikly Compose API 验证为准。Kuikly Compose 非 Runtime 的 import 必须使用 `com.tencent.kuikly.compose.*`，不能直接引入 Android 原生 Compose UI 包。

### 7.2 `ChatScreen` 组合结构

```text
ChatScreen
├── ChatBackground
├── ChatMessageList
│   ├── LoadingEarlierIndicator
│   ├── DateSeparator（可选）
│   ├── ChatMessageItem
│   │   ├── AvatarSlot
│   │   ├── MessageContentSlot / MessageRenderer
│   │   ├── StatusSlot
│   │   └── MessageActionsSlot
│   ├── EmptyStateSlot
│   └── ErrorStateSlot
├── ScrollToBottomButton（可选）
└── ChatComposer
    ├── ComposerLeadingSlot
    ├── TextInput
    ├── ComposerTrailingSlot
    └── Send / Stop Action
```

### 7.3 列表策略

首期以 Kuikly Compose 已支持并验证的列表组件为准，优先使用带稳定 key 的 Lazy 列表能力。必须明确：

- 新消息追加时，用户位于底部才自动滚动；用户正在阅读历史时不抢滚动。
- 流式更新只更新对应消息，不重建整个消息列表的业务状态。
- 历史消息加载后保持当前可视锚点，避免页面跳动。
- 列表项 key 使用 `message.id`，不能使用 index。
- 预留 `MessageGroupStatus`，根据相邻消息的作者和时间窗口计算首/中/尾。

### 7.4 Composer 策略

`ChatComposer` 首期提供：

- 单行/多行文本输入配置。
- 空文本禁用发送。
- `Sending` / `Streaming` 状态展示停止按钮。
- 输入法 action 与发送行为映射。
- 最大长度、清空、失败后重试入口。
- `ComposerSlots` 扩展附件入口，但不在首期实现附件业务。

输入框状态应明确是受控还是非受控。首期推荐由 Composer 内部维护编辑态、提交时向 Controller 发送 `SendText`，避免每次输入都触发完整 `ChatState` 列表重组；若宿主需要草稿保存，再提供 `onTextChanged` 与初始文本配置。

### 7.5 可替换渲染

消息渲染不采用 `when` 无限增长：

- `Text` 使用内置 `TextMessageRenderer`。
- `Custom(type)` 通过 `MessageRendererRegistry` 查找 renderer。
- 未注册类型显示稳定的 fallback，而不是崩溃。
- renderer 只接收通用消息、分组状态、主题和 callbacks。
- renderer 不直接调用网络；需要副作用时通过 callback/Effect 回到 Controller 或宿主。

## 8. 主题、国际化和可访问性

### 8.1 主题

`ChatTheme` 采用语义 token，而不是把颜色散落在组件中：

- `backgroundColor`
- `surfaceColor`
- `outgoingBubbleColor`
- `incomingBubbleColor`
- `outgoingTextColor`
- `incomingTextColor`
- `errorColor`
- `dividerColor`
- `bubbleShape`
- `spacing`
- `typography`

组件不得直接依赖业务品牌色。宿主可以提供完整主题或只覆盖局部 token。

### 8.2 文案

发送、重试、停止、加载更多、空态、错误态等文案集中在 `ChatStrings`，不在 Composable 内硬编码。首期至少支持中文和英文默认值，并允许宿主注入。

### 8.3 可访问性

设计阶段保留：

- 消息作者和发送时间的语义描述。
- 发送中、失败、已取消状态的可读提示。
- 点击、长按和发送按钮的语义 label。
- 不用颜色作为唯一状态表达。

具体可访问性能力以 Kuikly Compose 当前跨端支持矩阵为准，并在实现阶段单独验证各端差异。

## 9. AG-UI 与现有能力迁移

### 9.1 目标数据流

```text
AG-UI Event Stream
  → agui/EventParser
  → Session/Run Mapper
  → ChatController.dispatch(MessageOperation)
  → ChatState
  → Compose ChatScreen
```

`session/` 负责会话生命周期和运行控制；`agui/` 负责协议解析；新 Chat UI Core 只处理通用消息和操作。UI 层不得读取 `RunStartedEvent` 等协议对象。

### 9.2 现有模块处理

- `legacy/ai/`、`bubble/`、`legacy/composer/`、`indicator/`、`navigation/` 已删除，不再做适配层。
- `compose/agentcard/ComposeAgentCardList.kt`：保留现有 Agent 卡片能力，逐步拆出为 renderer，并接入新消息列表。
- `agentcard/`：继续作为 Kuikly DSL 卡片渲染层，和 Compose renderer 语义对齐。
- `model/`：收敛为跨 DSL 的领域模型、主题、仓库和工厂，禁止引入 Compose UI 类型。

## 10. 分阶段实施计划

### Phase 0：基线与契约冻结 ✅ 已完成

**产出**：模型和行为契约，不改变线上旧入口。

- 盘点现有 `model/`、`session/`、`agui/`、`compose/agentcard/` 的职责与重复模型。
- 建立 `ChatMessage`、`MessagePart`、`MessageStatus`、`ChatState`、`ChatIntent`、`ChatEffect` 草案。
- 定义 Flutter 对照能力矩阵和 P0 验收用例。
- 确认 Kuikly Compose 列表、输入框、滚动、状态和动画可用 API。

**完成标准**：旧 `KuiklyAiChatView` 已删除，核心逻辑不再依赖它。

### Phase 1：Compose Core + 基础消息列表 ✅ 已完成

**产出**：可运行的 Compose DSL 基础 Chat UI。

- 实现 Core 状态容器和 Controller。
- 实现文本消息、消息分组、入站/出站气泡。
- 实现 `ChatScreen`、`ChatMessageList`、`ChatMessageItem`、空态/加载态。
- 完成底部锚定、稳定 key（`key(message.id)`）、加载历史和基础滚动。
- 使用内存仓库和 fake sender 完成跨平台单元测试。

### Phase 2：Compose Composer + 发送闭环 ✅ 已完成

**产出**：可交互的 Compose DSL Chat UI。

- 实现文本 Composer、发送、发送中、停止、失败和重试。
- 接入结构化错误和 `ChatEffect`（`ChatEffectHandler`）。
- 完成主题 token、文案注入；Composer 会话级错误展示。
- JS 编译门禁通过；人工 UI 验收见 `docs/plans/compose-ui-acceptance.md`。

### Phase 3：AG-UI 适配与可扩展 renderer ✅ 已完成（Scripted E2E）

**产出**：新 Compose SDK 接入 AG-UI 会话（Scripted 端到端）。

- 将 AG-UI 事件映射到 Core 操作。
- 将 Agent 卡片接入 `MessageRendererRegistry` + `unknownCustomRenderer` fallback。
- `AgUiLlmProvider` + `ScriptedSseLlmTransport` 集成测试与 Demo 接线。
- 旧入口兼容适配器仍待 Phase 5；真实生产网络联调由宿主注入 `LlmTransport` 完成。

### Phase 4：P1 消息能力 📋 Planned

- 图片/文件模型和基础渲染。
- Markdown/代码块/链接。
- 复制、编辑、删除、引用和长按菜单。
- 未读分隔线、日期分隔线和基础动画。

### Phase 5：Kuikly DSL 迁移

**前置条件**：Compose DSL 的 Core、主题、renderer 契约稳定，P0 用例全绿。

- 使用同一 `ChatController`、模型、仓库和 renderer 语义实现传统 DSL。
- 为无法共享的滚动、输入法和动画差异提供 DSL-specific adapter。
- Compose 与 Kuikly DSL 运行同一套状态/操作/模型测试。
- 双 DSL 的视觉差异通过截图或人工验收清单记录，不在 Core 层加入平台判断。

## 11. 测试与验收

### 11.1 Core 单元测试

至少覆盖：

- 空消息、空白文本和超长文本校验。
- 发送成功、失败、取消、重试。
- 流式增量追加、消息完成和乱序/重复事件防护。
- 消息插入、更新、删除和稳定 id。
- 历史分页去重、边界和 `hasMoreEarlier`。
- 消息分组的首/中/尾判断。
- Controller 生命周期结束后不再更新状态。

### 11.2 Compose 组件验收

- 消息方向、头像、气泡、状态和分组样式正确。
- 新消息在底部时自动滚动，不在底部时不抢焦点。
- 历史加载不造成可视位置跳动。
- 流式消息只更新目标项。
- Composer 在空文本、发送中、失败和停止状态下行为正确。
- 主题 token 和文案替换生效。
- 至少覆盖 Android 和一个非 Android 宿主；其他目标按工程可运行性补齐。

### 11.3 工程门禁

每个阶段完成后执行：

```bash
bash scripts/check-layer-deps.sh
./gradlew :KuiklyChat:compileDebugKotlinAndroid \
          :KuiklyChat:compileDebugUnitTestKotlinAndroid
./gradlew :KuiklyChat:testDebugUnitTest
```

新增文件必须遵守单文件不超过 400 行、commonMain 平台无关、禁止裸日志等工程约束。测试只能使用 KMP 兼容的 `kotlin("test")`，不得引入 JVM-only 测试依赖。

## 12. 风险与应对

| 风险 | 表现 | 应对 |
|---|---|---|
| Compose API 跨端差异 | 列表/输入/滚动在不同端行为不一致 | 先以 Kuikly Compose 文档和 Demo 已验证 API 为准；每阶段建立平台矩阵 |
| 状态粒度过大 | 流式输出导致整个页面频繁重组 | Controller 状态不可变但拆分可观察区域；消息项使用稳定 key，后续做重组分析 |
| 旧模型迁移成本高 | 旧 Agent 卡片、Bubble 与新模型不一致 | 建立 compatibility adapter，不把旧字段直接污染新 Core |
| renderer 无限膨胀 | `when` 分支和页面文件持续变大 | registry + 单 renderer 文件 + fallback；新文件遵守 400 行门禁 |
| 滚动体验不稳定 | 加载历史/流式更新时跳动 | 先定义可观测滚动状态和锚点策略，再实现 UI；把滚动行为列入 P0 验收 |
| AG-UI 与 UI 耦合 | 协议事件泄漏到 Composable | 只允许 `agui` 转换为 Core operation，架构脚本和 review 检查边界 |
| 双 DSL 语义漂移 | Compose 与 Kuikly DSL 体验不同 | 共享模型/操作/主题/renderer 契约，共享 Core 测试和能力矩阵 |

## 13. 决策记录

1. **首发 DSL**：Compose DSL。理由是当前明确需要补齐 Compose，并且更适合先建立组合式、可插槽的 UI 结构。
2. **核心入口**：`ChatController` + `ChatScreen`，DSL 会话页是 `ChatControllerSession`。
3. **消息扩展方式**：`MessagePart` + `MessageRendererRegistry`，避免每新增消息类型都修改主列表。
4. **协议边界**：AG-UI 只在 `agui/` 和适配层出现，UI 消费通用消息状态。
5. **迁移顺序**：先 Core，再 Compose 基础 UI，再发送闭环，再 AG-UI，再高级消息，最后 Kuikly DSL。
6. **兼容策略**：旧 SDK 入口保留适配器，不阻塞新 SDK 设计，也不让旧 API 决定新 Core 的模型。

## 14. 当前完成度与下一步任务

### 已完成（Phase 1–3）

1. 列表：`key(message.id)`、sticky-bottom、`ChatEffect` 滚底、历史锚点、回底 FAB、`unreadCount`。
2. Controller：user `SENT`、retry 重发 user prompt、stop/cancel、`loadInitial`/`Refresh`。
3. `ChatEffectHandler`、`ChatSlots` 扩展、`defaultChatRendererRegistry()`。
4. `ScriptedSseLlmTransport`、`AgUiScriptedScenarios`、`AgUiLlmProvider` 集成测试。
5. `ComposeChatSdkDemoPage` Scripted 全链路 Demo。
6. 174 个 Android 单元测试、JS 编译、架构门禁通过。

### 下一步（Phase 4+）

1. **P1**：Markdown、代码块、图片/文件附件默认渲染和 Composer 附件入口。
2. **P1**：各宿主人工 UI 验收（见 `docs/plans/compose-ui-acceptance.md`）。
3. **P2**：真实生产 `AgUiLlmProvider` + 宿主 `LlmTransport` 网络联调。
4. **P2**：传统 Kuikly DSL 渲染层迁移（Phase 5）。
