---
last_updated: 2026-08-03
status: active
owner: @kuikly-chat-team
---

# 命名规范

- **类 / 接口 / 枚举 / 密封类**：PascalCase，`ChatSession`、`MessageType`、`ChatComponentFactory`。
- **函数 / 变量 / 属性**：camelCase，`sendMessage`、`messageList`。
- **常量 / 配置键**：包级 `const val` 用 UPPER_SNAKE；枚举取值用 PascalCase。
- **包名**：全小写反向域名 `com.tencent.kuiklybase.chat.<feature>`。
- **Compose DSL 目录**：新 Compose SDK 统一放在 `compose/`，按 `core/`（状态与模型）、`ui/`（页面与组件）、`adapter/`（协议/Provider 适配）、`agentcard/`（卡片渲染）划分。
- **遗留目录**：待迁移的传统 Kuikly DSL 和旧 AI UI 放在 `legacy/`；`composer` 专指传统 DSL 输入组件，不与 `compose` 并列。
- **文件**：一个文件一个主类，文件名与类名一致；测试文件 `XxxTest.kt`。
- **DSL 配置块**：以 receiver 形式扩展 `ChatSessionConfig.() -> Unit`；属性用名词，回调用 `on` 前缀（`onSendMessage`）。
- **组件工厂方法**：`renderXxx` 命名（如 `renderAvatar`）。
- **ID 生成**：统一走 `ChatMessageHelper.generateId()` / `ChatChannelHelper.generateId()`，禁止随机拼接。
