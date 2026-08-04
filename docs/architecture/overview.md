---
last_updated: 2026-08-03
status: active
owner: @kuikly-chat-team
---

# 系统架构概览

KuiklyChat 是一个 Kotlin Multiplatform 库（`:KuiklyChat`），提供跨端 Chat UI 组件与 AG-UI 协议支持，被 Android / iOS / 鸿蒙 / H5 / 小程序等宿主复用。

## 分层

| 层 | 包（commonMain） | 职责 |
|----|----------------|------|
| 宿主层 | `androidApp/` `iosApp/` `ohosApp/` `shared/` `h5App/` `miniApp/` | 集成组件库、提供页面入口 |
| 会话编排层 | `chat/session/` | 旧版 `ChatSession` + `ChatSessionConfig`，组合传统 UI 子组件 |
| Compose Chat UI Core | `chat/compose/core/` | 新版 `ChatState`、`ChatIntent`、`ChatController`、消息模型、主题和 Renderer 契约 |
| Compose Chat UI | `chat/compose/ui/` | 首发 Compose DSL 的 `ChatScreen`、消息列表、气泡、Composer 和 Slots |
| Compose 适配层 | `chat/compose/adapter/` | LLM、AG-UI、AgentCard 和旧消息模型到 Compose Core 的边界转换 |
| 协议层 | `chat/agui/` | AG-UI 事件协议、解析、状态机、自定义事件扩展 |
| 富卡片层 | `chat/agentcard/` `chat/compose/agentcard/` | 17 种 Agent 卡片（Kuikly DSL / Compose DSL 双实现） |
| 传统 UI 组件层 | `chat/bubble/` `channel/` `legacy/composer/` `legacy/ai/` `indicator/` `navigation/` `ai/` `widget/` | 待迁移的 Kuikly DSL 与旧 AI UI 组件 |
| 模型层 | `chat/model/` | 旧版数据模型、主题、组件工厂、Handler/Formatter、Repository 接口、Preview/Mock 数据 |

## 依赖方向

宿主 App → `:KuiklyChat` → `com.tencent.kuikly:core/compose` + `KuiklyMarkdown`。

库内部存在两条并行路径：

```text
旧宿主 → session → bubble/legacy/composer/agentcard
新宿主 → compose/ui → compose/core
                         ↑
       compose/adapter ← agui / kuiklyai/provider / agentcard
```

`compose/core`、`compose/ui` 与 `compose/agentcard` 共同构成新 Compose SDK；协议、Provider 和旧模型只能在 `compose/adapter` 转换后进入 Core。传统 `agentcard` 只承载共享卡片模型和 Kuikly DSL 渲染，不反向依赖 Compose 包。
