---
last_updated: 2026-07-27
status: active
owner: @kuikly-chat-team
---

# 系统架构概览

KuiklyChat 是一个 Kotlin Multiplatform 库（`:KuiklyChat`），提供跨端 Chat UI 组件与 AG-UI 协议支持，被 Android / iOS / 鸿蒙 / H5 / 小程序等宿主复用。

## 分层

| 层 | 包（commonMain） | 职责 |
|----|----------------|------|
| 宿主层 | `androidApp/` `iosApp/` `ohosApp/` `shared/` `h5App/` `miniApp/` | 集成组件库、提供页面入口 |
| 会话编排层 | `chat/session/` | `ChatSession` + `ChatSessionConfig`，组合各 UI 子组件 |
| 协议层 | `chat/agui/` | AG-UI 事件协议、解析、状态机、自定义事件扩展 |
| 富卡片层 | `chat/agentcard/` `chat/agentcard_compose/` | 17 种 Agent 卡片（Kuikly DSL / Compose DSL 双实现） |
| UI 组件层 | `chat/bubble/` `channel/` `composer/` `indicator/` `navigation/` `ai/` `widget/` | 原子/复合 UI 组件 |
| 模型层 | `chat/model/` | 数据模型、主题、组件工厂、Handler/Formatter、Repository 接口、Preview/Mock 数据 |

## 依赖方向

宿主 App → `:KuiklyChat` → `com.tencent.kuikly:core/compose` + `KuiklyMarkdown`。
库内部：`agui` / `session` 依赖 `model` 与 UI 组件；`agentcard` 依赖 `model`；UI 组件之间低耦合，统一由 `session` 编排。
