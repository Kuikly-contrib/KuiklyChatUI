---
last_updated: 2026-08-03
status: active
owner: @kuikly-chat-team
---

# 数据流转

## 1. 常规聊天流

```
ChatRepository / ObservableList<ChatMessage>
        │  (响应式更新)
        ▼
   ChatSession  ──► 分组/日期/排版 Handler ──► Bubble 渲染
        │
        ├─ messageListOptions（自动滚动 / 历史加载）
        └─ 事件回调（onSend / onResend / onReaction ...）
```

## 2. AG-UI 协议流（文本 / 富卡片）

```
后端 SSE / WebSocket
        │  data: 行
        ▼
AgUiSession.dispatchSseData / dispatchJson
        │  事件状态机（16+ 事件）
        ▼
ObservableList<ChatMessage>   或   AgentCard 列表
        │
        ├─ ChatSession({ session.messages })        → 气泡渲染
        └─ AgentCardList / ComposeAgentCardList      → 富卡片渲染
              ↑ AgUiCardSession.dispatchSseData
```

## 3. Compose-first Chat UI 流

```text
LlmProvider / EchoLlmProvider
        │
        ▼
LlmChatAdapter ───────────────┐
        │                      │
        ▼                      │ AgUiComposeBridge
ChatController                 │ AgentCardComposeBridge
        │                      ▼
        ├──────────────► MessagePart.AgentCardContent
        ▼
ChatState → ChatScreen → ChatMessageList / ChatComposer
```

- `LlmChatAdapter` 将历史消息和流式文本转换为 `ChatStreamEvent`。
- `AgUiComposeBridge` 镜像结构化 AG-UI 消息；普通文本仍由 LLM Adapter 负责，避免重复渲染。
- `AgentCardComposeBridge` 将 `AgUiCardSession` 的强类型 `AgentCard` 转换为 `MessagePart.AgentCardContent`。
- `MessageRendererRegistry` 将自定义消息交给对应 Renderer，UI 不读取 AG-UI 事件。
- `ComposeChatSdkDemoPage` 当前使用 Echo Provider 和本地 AG-UI 事件流验证，真实后端仍待宿主注入和联调。

## 关键解耦点
- 协议层产出 `ChatMessage` / `AgentCard`，UI 层只消费模型，不感知传输与协议细节。
- Compose Core 与 Adapter 分离，Provider/AG-UI 的替换不改变 `ChatScreen`。
- 断线重连：AG-UI `MESSAGES_SNAPSHOT` 仍由协议会话处理；Compose 快照同步 API 已预留，完整 UI 恢复验收待补齐。
