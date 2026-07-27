---
last_updated: 2026-07-27
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
| 护栏 | `.github/workflows/` + `scripts/` | 中 | CI 门禁、分层校验、隔离验证 |

每个文档头部含 `last_updated` / `status` / `owner` 元信息。
`scripts/check-doc-freshness.sh` 以 `last_updated` 为唯一口径扫描过期设计文档（阈值 60 天），
PR 门禁与每周定时任务共用该脚本。改动设计文档时请一并更新 `last_updated`。

总入口见 `AGENTS.md`。
