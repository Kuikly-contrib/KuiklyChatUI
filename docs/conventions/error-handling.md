---
last_updated: 2026-07-27
status: active
owner: @kuikly-chat-team
---

# 错误处理规范

## 消息级
- 发送失败：`ChatMessage.status = MessageStatus.FAILED`，UI 渲染「发送失败，点击重试」+ 重发按钮，走 `onResend`。
- 软删除：`isDeleted = true`，气泡替换为「此消息已被删除」。

## 协议级（AG-UI）
- `RUN_ERROR` 事件 → 错误卡片（红色 + 错误码徽章）；未闭合的消息标记为完成。
- 工具调用失败 → `tool_error` 卡片（⚠️ + 错误信息）。
- 未知事件 → 落入 `AgUiEvent.Unknown`，由 `handlerRegistry.unknownEventHandler` 兜底；保留 `rawEvent` 便于排查。

## Repository 级
- 所有操作为 `suspend fun`，异常向上抛；调用方必须 try/catch 并转换为 UI 状态（loading / error）。
- 历史加载失败：`hasMoreEarlier` 不变，回滚插入并提示。

## 原则
- 不吞异常：必须让错误可见（UI 提示或日志）。
- 不依赖具体后端错误文案，统一映射为组件可识别的状态。
