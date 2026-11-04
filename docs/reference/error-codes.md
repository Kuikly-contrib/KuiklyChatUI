---
last_updated: 2026-07-27
status: draft
owner: @kuikly-chat-team
---

# 错误码约定

> 草案：组件库当前以状态枚举 + 文案映射处理错误，尚未定义全局数字错误码。建议如下分类，待评审后固化。

| 类别 | 标识 | 触发场景 | UI 表现 |
|------|------|---------|---------|
| 网络 | `NETWORK_ERROR` | SSE/HTTP 连接失败、超时 | 错误卡片 + 重试 |
| 协议解析 | `PARSE_ERROR` | 事件 JSON 非法、缺字段 | 落 `Unknown`，日志 WARN |
| 运行 | `RUN_ERROR` | Agent 运行异常（`RUN_ERROR` 事件） | 错误卡片 + 错误码徽章 |
| 工具 | `TOOL_ERROR` | 工具调用失败 | `tool_error` 卡片 |
| 消息发送 | `SEND_FAILED` | 消息发送失败 | 气泡 `FAILED` + 重发按钮 |
| 未知 | `UNKNOWN` | 未识别事件类型 | 忽略 / 透传，保留 `rawEvent` |

## 约定
- 错误码与文案解耦：组件只认类别，文案由业务/后端映射。
- 协议层错误优先走 `AgUiEvent.RunError`，避免自定义数字码穿透 UI。
