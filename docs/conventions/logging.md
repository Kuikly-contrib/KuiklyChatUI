---
last_updated: 2026-07-27
status: active
owner: @kuikly-chat-team
---

# 日志规范

- **结构化日志**：使用项目日志能力（如 Kuikly 日志模块）输出带 tag/级别的结构化信息，禁止裸 `println` / `console.log` 合入主干。
- **级别**：`DEBUG`（协议分片/排版细节）、`INFO`（会话生命周期）、`WARN`（可恢复异常如解析容错）、`ERROR`（RUN_ERROR / 工具失败）。
- **tag 约定**：`AgUiSession`、`AgUiParser`、`ChatSession`、`AgentCard` 等按模块冠名，便于过滤。
- **敏感信息**：不得记录 token / 完整消息体原文到 INFO 以上级别。
- **调试开关**：调试日志需由编译/运行时开关控制，release 不输出 DEBUG。

## 自动化校验
`scripts/check-layer-deps.sh` 会扫描 `commonMain` 中的裸 `println(` 调用（KDoc 注释中的示例除外），命中即报错。新增代码不得遗留调试打印；单次排查可用 `git grep 'println(' KuiklyChat/src/commonMain`。
