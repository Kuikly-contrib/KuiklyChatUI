---
last_updated: 2026-07-27
status: active
owner: @kuikly-chat-team
---

# Feature: Agent 卡片系统
Status: ✅ Implemented

## 目标
独立于 `ChatSession`，用富卡片展示 AI Agent 的事件与状态（代码块、命令执行、文件编辑、工具调用、思考过程等 17 种）。

## 范围
- 数据模型 `AgentCard`（sealed 子类）与构造器 `RichCardPayload`。
- 双渲染层：`agentcard/`（Kuikly DSL 的 `AgentCardList`）与 `agentcard_compose/`（Compose 的 `ComposeAgentCardList`）。
- `AgUiCardSession`：AG-UI 事件流自动映射为 `AgentCard` 列表。
- 主题：`AgentCardTheme` / `ComposeCardTheme`。

## 新增卡片（三步 + 注册）
1. `RichMessageKind` 增加常量；2. `RichCardPayload` 增加构造器；3. 实现 `MessageRendererFactory`；4. 注册到 `agUiRichRenderers()`（双渲染层都要）。

## 非目标
- 不替代 `ChatSession` 的普通消息气泡渲染。
- 不做后端协议解析（由 `agui` 负责事件 → `AgentCard` 映射）。
- 不处理传输层。

## 依赖
- `feature-agui-protocol`（✅ 已实现）：`AgUiCardSession` 将 AG-UI 事件流映射为 `AgentCard`。

## 相关
`README.md`「Agent 卡片系统」、`AGUI_INTEGRATION_GUIDE.md` 第二节。
