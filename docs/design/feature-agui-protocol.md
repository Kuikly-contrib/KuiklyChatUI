---
last_updated: 2026-07-27
status: active
owner: @kuikly-chat-team
---

# Feature: AG-UI 协议集成
Status: ✅ Implemented

## 目标
对接 AG-UI（Agent-User Interaction Protocol）标准事件流，零侵入地让 Chat UI 消费任意合规 AI Agent 后端。

## 范围
- 16 种标准事件 + Activity / Reasoning / Meta 扩展（sealed class `AgUiEvent`）。
- `AgUiEventParser`：JSON / SSE 行解析，自动跳过空行与 `[DONE]`。
- `AgUiSession`：事件状态机，产出 `ObservableList<ChatMessage>`。
- `AgUiHandlerRegistry`：Activity / Custom / Meta 三类扩展点。
- 传输无关：解析器只处理 JSON 事件对象，SSE/WebSocket 由调用方对接。

## 验收
- 流式文本逐字显示；工具调用参数分片不丢失；断线重连经 `MESSAGES_SNAPSHOT` 恢复；`RunError` → 失败气泡可重发。
- 三层验证：单测（81）、官方规范对齐、端到端联调。

## 非目标
- 不绑定任何具体后端实现（通过 `ChatRepository` 接口接入）。
- 不负责传输层连接管理（SSE/WebSocket 由宿主或调用方对接）。
- 不直接提供 UI 组件（只产出 `ChatMessage` 模型）。

## 依赖
- 无（基础设施层，被 `session/` 与 `agentcard/` 依赖）。

## 相关
`README.md`「AG-UI 协议集成」、`AGUI_INTEGRATION_GUIDE.md`。
