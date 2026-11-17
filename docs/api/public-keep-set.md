---
last_updated: 2026-11-17
status: active
owner: @kuikly-chat-team
---

# Public API keep-set

`scripts/public-api-baseline.txt` 是当前四个产品模块（`core` / `compose` / `compat` / `ai`）commonMain 顶层 public dump；testing 模块和 Android 原生模块不纳入这份 dump。
`scripts/check-public-api.sh` 会阻断新增 public 声明，并在收口后要求同步删除基线行。
各库模块（含 Ohos `build.ohos.gradle.kts`）默认严格 `explicitApi()`；`scripts/check-layer-deps.sh` 规则 14 阻断 Warning 模式与属性开关。规则 17 阻断伞形 `api()` testing。

**当前发布线是 1.0.x（当前 1.0.6），但 dump 仍是文本基线。** 不要把文本 dump 当成 binary / Native ABI 清单；版本变更与公开面变更仍分别由 `gradle.properties` 和 `scripts/public-api-baseline.txt` 校验。

发布构件：

| 模块 | 职责 | 宿主怎么用 |
|------|------|------------|
| `:kuiklychat-core` | `ChatMessage` / `MessagePart` / `ChatController` / `ChatTheme` / 共享 `AgentCard` 模型 | 一般被传递，不必直接写 |
| `:kuiklychat-compose` | Compose `ChatScreen`、slots、列表 | 只要 Compose UI |
| `:kuiklychat-compat` | Kuikly DSL `ChatControllerSession` / `ChatDockComposer` / channel | 只要 DSL 会话页 |
| `:kuiklychat-ai` | KuiklyAI provider 与运行时 | KuiklyAI 按需 |
| `:kuiklychat-testing` | Preview / Mock | 显式依赖 |
| `:KuiklyChatAndroid` | Android 原生图片与字节缓存 | Android 宿主按需 |

接入示例见 [`host-integration.md`](host-integration.md)。

## 四级政策

| 级别 | 含义 | 例子 |
|------|------|------|
| Stable（拟冻结候选） | 当前 1.0.x 可调用；冻结前仍允许小版本修正，冻结后变更走 major | `ChatController` / `ChatMessage` / `MessagePart` / `ChatState` / `ChatTheme` / `ChatStrings`；`ChatScreen`、`ChatControllerSession`、`ChatDockComposer`；`MessageRendererRegistry` |
| Evolving | 1.0.x 内可增补和调整，调用方应跟版本阅读文档 | attachments / media config、conversation directory、composer config、slots 细分、channel config |
| Provider / Experimental | 协议与平台扩展面，暂不承诺 1.x 语义稳定 | `LlmProvider`、`LlmTransport`、`SseDecoder`、`LlmChatMessage`；历史封装 `LlmChatController` / `rememberLlmChatController()` |
| Internal SPI | 跨模块但非 App 契约 | `SseDecoder` 内部路径、`UserCache.observe`、`toCore` / `persistParts` / `withExtra` |
| internal | 实现细节，禁止冻结 | View/Attr/Event 叶子、布局估算、`AttachmentDraft` |

Stable 候选仍受 `explicitApi()` 和 baseline dump 约束；文本 dump 不等于 binary ABI 承诺。

`:kuiklychat-testing` 的 `ChatPreviewData` / `MockChatRepository` 是 testing 构件上的 public，不进产品 dump。

## 必须保持 public

- Compose：`ChatScreen`、`ChatController`、`ChatIntent`、`ChatState`、`ChatTheme`、`ChatSlots`、`ChatCallbacks`、`compose.core.ChatMessage` / `MessagePart` / `ChatUser`
- 发送与恢复：`ChatController.sendMessage` / `sendText` / `setConnectionState` / `setDraft` / `setDraftAttachments` / `clearDraft`，以及 `ChatState.queuedMessages` / `connectionState` / `partialMessageIds` / `draftText` / `draftAttachments` 和 `ChatConnectionState`
- Kuikly DSL：`ChatControllerSession`（同一 `ChatController` 的会话页；`showHeader = false` / `showComposer = false` 可让宿主自带顶栏与输入区；空态标题和建议通过 `ChatSuggestion` 替换；传入 `ChatConversationHost` 后带会话抽屉）、`ChatDockComposer`（键盘跟随、附件、颜色走 `ChatTheme`；加号展开拍照与相册）、`ChatWelcomeState`、`ChatSuggestionBar`、`sessionChatMessage()`（`typealias ChatMessage = compose.core.ChatMessage`）、`ChatMessageHelper`
- 多段对话：`ChatConversationDirectory`、`ChatConversationStore`、`MemoryChatConversationStore`、`ChatConversationSummary`、`ChatConversationHost`，以及 `ChatController.showConversation` / `regenerateReply`。抽屉、贴底、打点和回答操作条留在会话页内部。
- 扩展点：Compose `MessageRendererRegistry`、`defaultChatRendererRegistry()`

## 禁止再冻成 public（由 `scripts/check-public-api.sh` 强制）

- `AttachmentDraft`
- `estimateComposerLineCount`（输入行数估算，留在 composer 内部）
- `ChatContentBlock`（已删除，统一到 `MessagePart`）
- 已删除的旧会话 UI：`ChatSession`、`ChatMessageComposer`、`ChatBubble*`、`AiMessageText`、`ChatAiTypingIndicator`、`KuiklyAiChatView`。不要把它们加回 public。
- Channel View/Attr/Event：宿主走 `ChatChannelList` / `ChatChannelHeader(ChatChannelHeaderConfig)`
- `ChatScrollToBottomButton*` 保持 internal；DSL 回底用 `ScrollDownFrost`

## 后续（明确不做 / 以后再做）

1. 不要再拆 Gradle 模块、不要再改一轮 Config API、不要膨胀 `ChatMessage` 字段。
2. `MessagePart.AgentCardContent` 先保持，不要加深；中期再考虑 `Custom` 或可选模块。
3. 文本 dump 换成 binary / Native 导出 ABI 清单：以后再做。
4. 工蜂已接 layer + Android + iosSimulatorArm64 + 一个 Ohos 库编译；全 Native target 与发布流水线仍待宿主 CI 扩容。
