---
last_updated: 2026-09-23
status: active
owner: @kuikly-chat-team
---

# 当前迭代（Sprint）

> Compose DSL Chat UI SDK 首期落地。Status 取值：✅ Done / 🔄 In Progress / 📋 Planned。

## 目标
先完成 Compose DSL 的 Chat UI，再提供同一 `ChatController` 的 Kuikly DSL 会话页 `ChatControllerSession`。宿主 Demo 入口是 `ChatComponentDemoPage`（`chat_component_demo`）。

## 任务
- ✅ 建立 `compose/core`：消息模型、状态、Intent、Effect、Controller、主题和 Renderer Registry
- ✅ 建立 `compose/ui`：`ChatScreen`、消息列表、气泡、Composer、Slots
- ✅ 建立 LLM/AG-UI/AgentCard 适配层和 Scripted SSE Demo
- ✅ 消息列表 stable key、sticky-bottom、历史锚点、`ChatEffect` 消费、回底按钮
- ✅ `AgUiLlmProvider` + `ScriptedSseLlmTransport` 端到端集成测试（210 个 Android 单测）
- ✅ JS 编译门禁（`KuiklyChat`、`shared`）
- 📋 人工 UI 验收（见 `docs/plans/compose-ui-acceptance.md`）
- 📋 **Compose UI 对齐 Flyer**（设计稿已拆分，见下表）
- 📋 Phase 4：Markdown、图片/文件附件和 Composer 附件入口
- 📋 真实生产 AG-UI 网络联调（宿主注入 `LlmTransport`）

## Compose UI 对齐设计（Draft）

| 优先级 | 文档 | 说明 |
|--------|------|------|
| P0 | [feature-compose-chat-avatar.md](feature-compose-chat-avatar.md) | 用户头像 |
| P0 | [feature-compose-message-time-status.md](feature-compose-message-time-status.md) | 时间与发送状态 |
| P0 | [feature-compose-message-row.md](feature-compose-message-row.md) | 消息行外壳布局 |
| P0 | [feature-compose-bubble-grouping-shape.md](feature-compose-bubble-grouping-shape.md) | 分组气泡圆角 |
| P1 | [feature-compose-list-chrome.md](feature-compose-list-chrome.md) | 日期分隔、FAB、LoadMore、正在输入 |
| P1 | [feature-compose-text-markdown-renderer.md](feature-compose-text-markdown-renderer.md) | 文本 / Markdown |
| P1 | [feature-compose-image-file-messages.md](feature-compose-image-file-messages.md) | 图片 / 文件 + `ChatCrossCache` ✅ |
| P1 | [feature-compose-resolve-user-cache.md](feature-compose-resolve-user-cache.md) | resolveUser + UserCache + 头像加载 ✅ |
| P1 | [feature-compose-system-message.md](feature-compose-system-message.md) | 系统消息 ✅ |
| P1 | [feature-compose-composer-enhancement.md](feature-compose-composer-enhancement.md) | 输入区增强 |
| P2 | [feature-compose-theme-and-slots.md](feature-compose-theme-and-slots.md) | 主题分层与 Slots 对照 |


## 验收
- Android 编译、`shared` Demo 编译、`testDebugUnitTest` 和 `check-layer-deps.sh` 全部通过。
- Phase 4 媒体能力与人工 UI 验收完成后，将 Compose 首期标记为 Done。
