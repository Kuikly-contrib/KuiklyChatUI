---
last_updated: 2026-08-03
status: active
owner: @kuikly-chat-team
---

# 待办（Backlog）

> 未排期需求池，按优先级滚动到迭代。

- [ ] 更多富卡片类型（如思维导图、时序图）
- [ ] Compose 渲染层与 Kuikly DSL 渲染层的性能对比与对齐
- [ ] Compose Chat UI 的真实 AG-UI 后端端到端联调
- [ ] Compose Chat UI 跨端（Android / iOS / 鸿蒙）视觉与交互验收
- [ ] Compose Chat UI 消息列表锚点、重组性能与滚动体验优化
- [ ] 主题系统支持运行时换肤（多套预设）
- [ ] AG-UI 断线自动重连与补偿策略增强
- [ ] 各端宿主示例补齐（小程序 / H5 深度联调）
- [x] docs/ 过期扫描自动化（`scripts/check-doc-freshness.sh` + doc-gardening）

## 护栏相关

当前策略：**不接 CI，门禁只在本地执行**（`scripts/check-layer-deps.sh` + `scripts/check-doc-freshness.sh`）。
`.github/workflows/` 保留为门禁定义的书面参考，不会被工蜂触发。

- [ ] （暂缓）把门禁接到工蜂 CI / 蓝盾。等团队确有需要时，按 `agent-guardrails.yml` 的 Gate 逐条搬运。
- [ ] 启用 Detekt（函数长度、`!!` 滥用），见 `agent-guardrails.yml` Gate 2 注释
- [ ] 启用 Kover 覆盖率阈值，见 `agent-guardrails.yml` Gate 4 注释

## 超长文件拆分（目标：清空 `scripts/file-size-baseline.txt`）

每拆完一个就从 baseline 删掉对应行；门禁会校验清单不腐化（文件已达标却仍在清单里会报错）。

已完成：

- [x] `bubble/RichCardRenderers.kt` 1398 → 56（12 个渲染器各自成文件 + `RichCardShell.kt` 共享外壳）
- [x] `session/ChatSessionView.kt` 1196 → 352（消息项、默认气泡、操作菜单浮层分离）
- [x] `session/ChatSessionConfig.kt` 1009 → 305（Slot 类型 / 三组 Options / 旧 API 兼容层分离）
- [x] `model/ChatComponentFactory.kt` 578 → 216（接口与默认实现分离）
- [x] `model/ChatMessage.kt` 422 → 62（枚举 / 附件 / 消息模型 / Helper 分离）

待办：

- [ ] `agui/AgUiSession.kt`（939 行）——**需独立 PR**。939 行几乎全是同一个类的事件处理成员，
      要拆必须把状态机重构成按域划分的子处理器（text / tool / activity / reasoning / state）
      并共享一个可变上下文。它背后有 31 个单测，值得单独设计、单独评审，不要顺手改。
- [ ] `bubble/ChatBubbleView.kt`（708 行）
- [ ] `agui/AgUiSessionTest.kt`（583 行，测试；可按事件域拆成多个测试类）
- [ ] `agui/RichCardPayload.kt`（505 行）——单个 `object`，拆分只能改成扩展函数，会影响
      跨模块调用方的 import，需先确认是否接受该 API 变化。
- [ ] `bubble/ChatMessageOptionsView.kt`（504 行）
- [ ] `agui/AgUiEventParserTest.kt`（501 行，测试）
- [ ] `model/ChatPreviewData.kt`（496 行）——同样是单个 `object`，约束同 `RichCardPayload`。
- [ ] `compose/agentcard/ToolCallCard.kt`（419 行，仅超 19 行）——纯函数辅助已拆到
      `ToolCallCardHelpers.kt`，剩下是单个 Composable，继续拆需要把行头/展开区抽成子 Composable。
