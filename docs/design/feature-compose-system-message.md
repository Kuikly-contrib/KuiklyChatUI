---
last_updated: 2026-08-04
status: draft
owner: @kuikly-chat-team
---

# Feature: Compose Chat 系统消息（System Message）

## Status: 📝 Draft

## 目标
支持居中灰色系统提示条（如「用户加入会话」「消息已撤回」），对齐 Flutter `FlyerChatSystemMessage`（见 `flutter-chat-ui-components.md` §10.5）。

## 非目标
- 不作为 AG-UI AgentCard 的替代品（协议事件仍走 adapter）。
- 不实现未读分隔线（「以下为新消息」可与 `feature-compose-list-chrome.md` 合并设计，本需求仅系统文案条）。

## 技术方案

### 涉及的模块
- `chat/compose/ui/renderer/SystemMessageRenderer.kt`
- `chat/compose/core/MessagePart`：新增 `System(text)` 或 `Custom(type = "system")` 约定
- `MessageRendererRegistry`：注册 `system` 类型

### 数据模型变更（二选一，实现时定一种）

**方案 A**：专用 part

```kotlin
data class System(val text: String) : MessagePart
```

**方案 B**：沿用 Custom

```kotlin
MessagePart.Custom(type = "system", payload = mapOf("text" to "..."))
```

### UI
- 全宽居中，小字号，浅灰背景圆角条，无头像、无气泡左右对齐。

## 验收标准
- Demo 可插入一条系统消息，列表中居中显示且不参与左右分组。
- 系统消息不影响 sticky-bottom 与 `unreadCount` 逻辑（或明确计入策略文档化）。
- 单测：renderer 路由、系统消息不参与 `groupStatus` 与相邻用户消息粘连。

## 依赖
- `feature-compose-message-row.md`（📋）：系统消息走独立 renderer，绕过默认行布局
- `feature-compose-theme-and-slots.md`（📋）：系统消息 typography/color token

---
> 参考：`FlyerChatSystemMessage`、`Message.system()`
