---
last_updated: 2026-08-05
status: template
owner: @kuikly-chat-team
---

# Feature: [功能名称]

## Status: Template（复制后改为 📝 Draft | 📋 Approved | 🚧 In Progress | ✅ Implemented）

## 目标
一句话描述这个功能要解决什么问题。

## 非目标
明确列出这次不做什么（防止 Agent 扩大范围）。
- 不……
- 不……

## 技术方案

### 涉及的模块
- `chat/model/`：新增 XXX 数据模型 / Repository 接口
- `chat/agui/`：新增 XXX 事件解析（如需对接协议）
- `chat/bubble/` 或 `chat/agentcard/`：新增 XXX UI 组件
- `chat/session/`：将新组件接入 `ChatSession`

### 数据模型变更
```kotlin
// 如有模型变更，写在这里（Kotlin / 跨端）
data class XxxConfig(
    val id: String,
    val enabled: Boolean,
)

sealed class XxxState {
    data object Loading : XxxState()
    data class Ready(val data: XxxConfig) : XxxState()
}
```

### API 变更（AG-UI / 内部协议）
```
POST /agent            # AG-UI Run 端点（SSE 流式）
  Request:  { threadId, runId, messages, tools }
  Response: text/event-stream（AgUiEvent 流）
```
> 若是内部 Repository 接口变更，用 Kotlin 函数签名描述即可。

## 验收标准
- 标准1：具体的、可验证的（如「流式文本逐字显示，无闪烁」）
- 标准2：具体的、可验证的（如「新增卡片在 Android / iOS / 鸿蒙三端表现一致」）
- 测试覆盖率：新增逻辑须有 `commonTest` 单测（见 `docs/conventions/testing.md`）

## 依赖
- 依赖 `feature-agui-protocol`（✅ 已实现）
- 依赖 `KuiklyMarkdown`（三方库）

---
> **用法**：复杂功能动手前先填此模板，审批（📋 Approved）后再写代码。
> 这是「明确意图」的工程化实现——Agent 拿到需求先落设计，避免跑偏。
