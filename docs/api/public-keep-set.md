---
last_updated: 2026-09-23
status: active
owner: @kuikly-chat-team
---

# Public API keep-set

`scripts/public-api-baseline.txt` 是当前 **产品模块** commonMain 顶层 public dump（不含 `:kuiklychat-testing`、不含空伞 `:KuiklyChat` 源码）。
`scripts/check-public-api.sh` 会阻断新增 public 声明，并在收口后要求同步删除基线行。
各库模块（含 Ohos `build.ohos.gradle.kts`）默认严格 `explicitApi()`；`scripts/check-layer-deps.sh` 规则 14 阻断 Warning 模式与属性开关。规则 17 阻断伞形 `api()` testing。

**现在不要把 dump 标成 Stable。** 先发 0.x experimental，等真实宿主按模块坐标接入后再砍 keep-set。也不要用 binary / Native 导出假装已经锁了 ABI——当前仍是文本 dump。

发布构件：

| 模块 | 职责 | 宿主怎么用 |
|------|------|------------|
| `:kuiklychat-core` | `ChatMessage` / `MessagePart` / `ChatController` / `ChatTheme` / 共享 `AgentCard` 模型 | 一般被传递，不必直接写 |
| `:kuiklychat-compose` | Compose `ChatScreen`、slots、列表 | 只要 Compose UI |
| `:kuiklychat-compat` | Kuikly DSL `ChatControllerSession` / `ChatDockComposer` / channel | 只要 DSL 会话页 |
| `:kuiklychat-agui` | AG-UI 协议与 Compose adapter | 协议层按需 |
| `:kuiklychat-agentcard` | 双 DSL 卡片渲染（可选） | 卡片按需 |
| `:kuiklychat-ai` | KuiklyAI provider 与运行时 | KuiklyAI 按需 |
| `:kuiklychat-testing` | Preview / Mock（**不进伞形**） | 显式依赖 |
| `:KuiklyChat` | 过渡伞：`api()` 上表除 testing 外的模块 | 旧坐标兼容 |

接入示例见 [`host-integration.md`](host-integration.md)。

## 四级政策

| 级别 | 含义 | 例子 |
|------|------|------|
| 将来的 Stable | 宿主可依赖，变更走 major。**现在没有** | 等宿主验证后再标：`ChatScreen`、`ChatController`、`ChatMessage`、`ChatControllerSession` |
| Experimental | 可调用，不承诺 ABI | 当前全部产品 public 面，含 Slot、Renderer 注册、AG-UI 映射 |
| Internal SPI | 跨模块但非 App 契约 | `SseDecoder`、`UserCache.observe`、`toCore` / `persistParts` / `withExtra` |
| internal | 实现细节，禁止冻结 | View/Attr/Event 叶子、布局估算、`AttachmentDraft` |

`:kuiklychat-testing` 的 `ChatPreviewData` / `MockChatRepository` 是 testing 构件上的 public，不进产品 dump，不冻结。

## 必须保持 public

- Compose：`ChatScreen`、`ChatController`、`ChatIntent`、`ChatState`、`ChatTheme`、`ChatSlots`、`ChatCallbacks`、`compose.core.ChatMessage` / `MessagePart` / `ChatUser`
- Kuikly DSL：`ChatControllerSession`（同一 `ChatController` 的会话页；空态标题和建议通过 `ChatSuggestion` 替换；传入 `ChatConversationHost` 后带会话抽屉）、`ChatDockComposer`（键盘跟随、附件、颜色走 `ChatTheme`；加号展开拍照与相册）、`ChatWelcomeState`、`ChatSuggestionBar`、`sessionChatMessage()`（`typealias ChatMessage = compose.core.ChatMessage`）、`ChatMessageHelper`
- 多段对话：`ChatConversationDirectory`、`ChatConversationStore`、`MemoryChatConversationStore`、`ChatConversationSummary`、`ChatConversationHost`，以及 `ChatController.showConversation` / `regenerateReply`。抽屉、贴底、打点和回答操作条留在会话页内部。
- 协议边界：`AgUiEventParser`、`AgUiSession.dispatch`、`compose/adapter` 的映射函数
- 扩展点：Compose `MessageRendererRegistry`、`agentCardRendererRegistry()`

## 禁止再冻成 public（由 `scripts/check-public-api.sh` 强制）

- `AttachmentDraft`
- `estimateComposerLineCount`（输入行数估算，留在 composer 内部）
- `ChatContentBlock`（已删除，统一到 `MessagePart`）
- 已删除的旧会话 UI：`ChatSession`、`ChatMessageComposer`、`ChatBubble*`、`AiMessageText`、`ChatAiTypingIndicator`、`KuiklyAiChatView`。不要把它们加回 public。
- Channel / BouncingDots 的 View/Attr/Event：宿主走 `ChatChannelList` / `ChatChannelHeader(ChatChannelHeaderConfig)` / `BouncingDots(BouncingDotsConfig)`
- `ChatScrollToBottomButton*` 保持 internal；DSL 回底用 `ScrollDownFrost`

## 后续（明确不做 / 以后再做）

1. 不要再拆 Gradle 模块、不要再改一轮 Config API、不要膨胀 `ChatMessage` 字段。
2. `MessagePart.AgentCardContent` 先保持，不要加深；中期再考虑 `Custom` 或可选模块。
3. 文本 dump 换成 binary / Native 导出 ABI 清单：以后再做。
4. 工蜂已接 layer + Android + iosSimulatorArm64 + 一个 Ohos 库编译；全 Native target 与发布流水线仍待宿主 CI 扩容。
