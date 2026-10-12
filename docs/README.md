---
last_updated: 2026-09-23
status: active
owner: @kuikly-chat-team
---

# KuiklyChat 文档库

按「稳定 / 规范 / 设计 / 计划 / 参考」分层组织，避免单一超大文件。

| 层 | 目录 | 更新频率 | 内容 |
|----|------|---------|------|
| 稳定 | `architecture/` | 低 | 架构、边界、数据流 |
| 规范 | `conventions/` | 中 | 命名/错误处理/测试/日志/机械规则 |
| 设计 | `design/` | 中 | 按功能的设计文档（含 Status） |
| 计划 | `plans/` | 高 | 当前迭代、待办、卫生清理 |
| 参考 | `reference/` | 低 | API 规范、错误码 |
| 接入 | `api/host-integration.md` | 中 | 按需模块坐标、0.x experimental |
| 护栏 | `.tgit-ci.yml` + `scripts/ci-gates.sh` | 中 | 工蜂门禁；`.github/workflows/` 仅书面对照 |

每个文档头部含 `last_updated` / `status` / `owner` 元信息。
当前研发进度更新时间：2026-09-23。
`scripts/check-doc-freshness.sh` 以 `last_updated` 为唯一口径扫描过期设计文档（阈值 60 天），
PR 门禁与每周定时任务共用该脚本。改动设计文档时请一并更新 `last_updated`。

## 当前研发入口

Compose DSL Chat UI SDK 当前处于首期开发中，进度与下一阶段任务以以下文档为准：

- 宿主按需接入（0.x experimental）：`api/host-integration.md`
- 当前聊天 UI 入口：`api/chat-ui.md`
- Public keep-set：`api/public-keep-set.md`
- 当前设计与完成度：`design/feature-kuikly-chat-ui-sdk-redesign.md`
- 当前迭代任务：`plans/current-sprint.md`
- 数据流：`architecture/data-flow.md`
- 模块边界：`architecture/boundaries.md`
- Flutter Chat UI 对照（历史参考，不是现行 API）：`architecture/kuikly-chat-ui-sdk-design.md`、`design/flutter-chat-ui-components.md`

已删除、不要再当现行文档：`AGUI_INTEGRATION_GUIDE.md`、`feature-kuiklyai-toolkit.md`、`feature-streaming-ai-text.md`、`reference/kuiklyai-platform-services.md`。它们描述的 `ChatSession`、`KuiklyAiChatView`、`AiMessageText` 已经不在代码里。

总入口见 `AGENTS.md`。
