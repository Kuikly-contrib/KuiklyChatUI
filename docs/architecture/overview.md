---
last_updated: 2026-09-23
status: active
owner: @kuikly-chat-team
---

# 系统架构概览

KuiklyChat 拆成可按需依赖的 KMP 库（0.x experimental）。`:KuiklyChat` 只是不含 testing 的过渡伞。

## 发布模块

| 模块 | 职责 |
|------|------|
| `:kuiklychat-core` | 单一 `ChatMessage` + `MessagePart`、`ChatController`、共享 `AgentCard` 模型 |
| `:kuiklychat-compose` | Compose `ChatScreen` / 列表 / slots |
| `:kuiklychat-compat` | Kuikly DSL `ChatControllerSession` / `ChatDockComposer` / channel |
| `:kuiklychat-agui` | AG-UI 协议与 adapter |
| `:kuiklychat-agentcard` | 双 DSL 卡片**渲染**（模型在 core） |
| `:kuiklychat-ai` | KuiklyAI |
| `:kuiklychat-testing` | Preview / Mock，显式依赖 |
| `:KuiklyChat` | 过渡伞（`api()` 上表除 testing） |

宿主接入见 [`docs/api/host-integration.md`](../api/host-integration.md)。

## 分层

| 层 | 包（commonMain） | 职责 |
|----|----------------|------|
| 宿主层 | `androidApp/` `iosApp/` `ohosApp/` `shared/` `h5App/` `miniApp/` | 集成组件库、提供页面入口 |
| 会话编排层 | `chat/session/controller/`（compat） | `ChatControllerSession`，绑定 `ChatController` |
| Compose Chat UI Core | `chat/compose/core/`（core） | `ChatState`、`ChatIntent`、`ChatController`、消息模型 |
| Compose Chat UI | `chat/compose/ui/`（compose） | `ChatScreen`、消息列表、气泡、Composer 和 Slots |
| Compose 适配层 | `chat/compose/adapter/`（agui） | LLM、AG-UI、AgentCard 边界转换 |
| 协议层 | `chat/agui/`（agui） | AG-UI 事件协议、解析、状态机 |
| 富卡片层 | `chat/agentcard/` `chat/compose/agentcard/`（agentcard 渲染 + core 模型） | 双 DSL 渲染，一套 `AgentCard` |
| 频道 | compat 的 `channel/` | `ChatChannelList` 等列表 UI，不是第二套会话页 |
| Preview | testing 的 `ChatPreviewData` | 不进伞形 |

## 依赖方向

```text
按需宿主 → kuiklychat-compose / kuiklychat-compat / …
旧宿主   → :KuiklyChat（伞，不含 testing）

Kuikly DSL  → session → bubble / composer → core.ChatMessage
Compose     → compose/ui → compose/core
                              ↑
            compose/adapter ← agui / kuiklyai / AgentCard 渲染
```

双 DSL 是产品选择。债务是两套消息模型，已经拆掉。`MessagePart.AgentCardContent` 短期可接受，不要再加深。
