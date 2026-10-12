---
last_updated: 2026-09-21
status: active
owner: @kuikly-chat-team
---

# 编码规范总览

| 规范 | 文档 | 适用范围 |
|------|------|---------|
| 命名 | `naming.md` | 类/函数/常量/包/文件 |
| 错误处理 | `error-handling.md` | 消息失败、协议错误、Repository |
| 测试 | `testing.md` | commonTest 单测、运行命令 |
| 日志 | `logging.md` | 结构化日志、禁止裸打印 |
| 工蜂门禁 | `ci.md` | `scripts/ci-gates.sh` + `.tgit-ci.yml` |
| 架构/分层 | `../architecture/boundaries.md` + `scripts/check-layer-deps.sh` | 依赖方向、平台隔离 |

详见各子文档。`AGENTS.md` 中的「硬性规则」与本文一致，CI / Review 按此校验。
