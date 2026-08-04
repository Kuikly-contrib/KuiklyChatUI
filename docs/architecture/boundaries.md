---
last_updated: 2026-08-03
status: active
owner: @kuikly-chat-team
---

# 模块边界与依赖规则

## 平台边界
- `commonMain` 必须平台无关；任何 `expect/actual`、平台 API 调用只能出现在 `androidMain` / `iosMain` / `macosMain`。
- 新增平台能力先定义 `commonMain` 接口，再在各端 `actual` 实现。

## 协议边界
- 所有后端协议（AG-UI 事件、SSE/WebSocket）只存在于 `chat/agui/` 包内。
- UI 层（`bubble/`、`session/` 等）禁止直接 import 协议类型或解析逻辑；通过 `ChatMessage` / `AgentCard` 模型解耦。

## 数据边界
- 业务后端通过 `ChatRepository` 接口接入（`chat/model/ChatRepository.kt`），组件库不绑定任何具体后端（参考 Stream Chat 强绑定的反面案例）。

## 双渲染层边界
- `agentcard/`（Kuikly DSL）与 `compose/agentcard/`（Compose DSL）必须语义对齐：新增一种卡片须两套都实现并在映射器/工厂注册。
- 两者不得互相依赖；新 `compose/adapter/AgentCardComposeBridge` 只复用 `agentcard` 的 `AgentCard` 数据契约和 `compose/agentcard` 的 Compose Renderer。
- 新 Compose Chat UI 与旧 `session/` UI 并行迁移，不能把旧 UI 类型反向引入 `compose/core`。

## 禁止项
- 禁止在 `commonMain` 使用 `android.*` / `platform.*` / `kotlinx.cinterop` 等平台符号。
- 禁止在 UI 层硬编码主题色值（应使用 `ChatTheme` / `AgentCardTheme` 注入）。

## 自动化校验
以上边界由 `scripts/check-layer-deps.sh` 强制校验——零依赖、可本地运行、CI 与本地跑的是同一条命令：

```bash
bash scripts/check-layer-deps.sh
```

- 规则 1：`commonMain` 不得 import 平台符号（`android.`/`java.`/`javax.`/`platform.`/`kotlinx.cinterop`，`androidx.compose` 除外）。
- 规则 2：`agui` 不得 import 任何 UI 包（bubble/session/channel/legacy/compose/navigation/indicator/ai/widget/agentcard）。
- 规则 3：`agentcard`（共享模型 + Kuikly DSL）不得 import `compose/agentcard`（Compose DSL）。
- 规则 4：`commonMain` 不得遗留裸 `println`。
- 规则 5：单文件 ≤ 400 行（存量超标文件登记在 `scripts/file-size-baseline.txt`，仅告警；新增违规阻断）。

> 注：现实中 `compose/agentcard` 会 import `agentcard` 的 `AgentCard` 数据类（共享数据契约），这是允许的；反向依赖（`agentcard` → `compose/agentcard`）才被禁止。

违规报错均带 ❌/✅ FIX/📖 三要素，Agent 可直接据此修复。
