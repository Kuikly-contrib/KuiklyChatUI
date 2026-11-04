---
last_updated: 2026-09-23
status: active
owner: @kuikly-chat-team
---

# 数据流转

## 1. 常规聊天流

```
ChatMessageSender / ChatHistoryLoader
        │
        ▼
   ChatController  ──►  ChatState / ChatEffect
        │
        ├─ ChatControllerSession   → Kuikly DSL（欢迎态、列表、Dock、看大图）
        └─ ChatScreen              → Kuikly Compose DSL
```

## 2. AG-UI 协议流（文本 / 富卡片）

```
后端 SSE / WebSocket
        │  data: 行
        ▼
AgUiEventParser.parseEvent / parseSseData
        │
        ▼
AgUiSession.dispatch
        │
        ▼
List<ChatMessage>   或   AgentCard 列表
        │
        ├─ ChatController.replaceExternalMessages → ChatControllerSession / ChatScreen
        └─ AgentCardList / ComposeAgentCardList
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
- `AgentCardComposeBridge` 将 `AgUiCardSession` 的强类型 `AgentCard` 转换为 `MessagePart.AgentCardContent`（短期保留该 part，不要再往 Core 加卡片字段）。
- `MessageRendererRegistry` 将自定义消息交给对应 Renderer，UI 不读取 AG-UI 事件。
- `ChatComponentDemoPage`（`chat_component_demo`）用 `ChatController` + `ChatControllerSession` 做宿主演示。真实后端仍待宿主注入 `ChatMessageSender`。

## 关键解耦点
- 协议层产出 `ChatMessage` / `AgentCard`，UI 层只消费模型，不感知传输与协议细节。
- Compose Core 与 Adapter 分离，Provider/AG-UI 的替换不改变 `ChatScreen`。
- 断线重连：AG-UI `MESSAGES_SNAPSHOT` 仍由协议会话处理；Compose 快照同步 API 已预留，完整 UI 恢复验收待补齐。
