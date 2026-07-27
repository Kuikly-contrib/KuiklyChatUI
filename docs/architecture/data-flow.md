---
last_updated: 2026-07-27
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

## 关键解耦点
- 协议层产出 `ChatMessage` / `AgentCard`，UI 层只消费模型，不感知传输与协议细节。
- 断线重连：`MESSAGES_SNAPSHOT` 全量覆盖消息列表，保证一致性。
