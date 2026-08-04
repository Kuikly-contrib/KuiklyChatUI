---
last_updated: 2026-08-03
status: active
owner: @kuikly-chat-team
---

# 当前迭代（Sprint）

> Compose DSL Chat UI SDK 首期落地。Status 取值：✅ Done / 🔄 In Progress / 📋 Planned。

## 目标
先完成 Compose DSL 的可运行 Chat UI Core、基础交互和 AG-UI/AgentCard 适配，再迁移到传统 Kuikly DSL。

## 任务
- ✅ 建立 `compose/core`：消息模型、状态、Intent、Effect、Controller、主题和 Renderer Registry
- ✅ 建立 `compose/ui`：`ChatScreen`、消息列表、气泡、Composer、Slots
- ✅ 建立 LLM/AG-UI/AgentCard 适配层和 Scripted SSE Demo
- ✅ 消息列表 stable key、sticky-bottom、历史锚点、`ChatEffect` 消费、回底按钮
- ✅ `AgUiLlmProvider` + `ScriptedSseLlmTransport` 端到端集成测试（174 个 Android 单测）
- ✅ JS 编译门禁（`KuiklyChat`、`shared`）
- 📋 人工 UI 验收（见 `docs/plans/compose-ui-acceptance.md`）
- 📋 Phase 4：Markdown、图片/文件附件和 Composer 附件入口
- 📋 真实生产 AG-UI 网络联调（宿主注入 `LlmTransport`）

## 验收
- Android 编译、`shared` Demo 编译、`testDebugUnitTest` 和 `check-layer-deps.sh` 全部通过。
- Phase 4 媒体能力与人工 UI 验收完成后，将 Compose 首期标记为 Done。
