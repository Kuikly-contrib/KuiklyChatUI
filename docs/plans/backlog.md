---
last_updated: 2026-09-23
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

门禁入口：`bash scripts/ci-gates.sh`。工蜂配置：`.tgit-ci.yml`（见 `docs/conventions/ci.md`）。
`.github/workflows/` 不会被工蜂触发，只作书面对照。

- [x] 工蜂接 layer + Android compile/test + iosSimulatorArm64 + 一个 Ohos 库编译（yaml 已落地；仓库 Settings 需打开 CI 并登记 runner）
- [ ] 启用 Detekt（函数长度、`!!` 滥用），见 `agent-guardrails.yml` Gate 2 注释
- [ ] 启用 Kover 覆盖率阈值，见 `agent-guardrails.yml` Gate 4 注释
- [ ] 用 binary dump / Native 导出替换文本 public dump

## 超长文件拆分（目标：清空 `scripts/file-size-baseline.txt`）

每拆完一个就从 baseline 删掉对应行；门禁会校验清单不腐化（文件已达标却仍在清单里会报错）。

已从产品里删除，不再拆分：

- 旧 Kuikly 会话 UI：`ChatSession`、`bubble/`、`legacy/composer/`、`legacy/ai/`、`indicator/`、`navigation/`、`AiMessageText`

仍在基线里、需要以后拆的存量文件：

- [ ] `agui/AgUiSession.kt`
- [ ] `agui/AgUiSessionTest.kt`
- [ ] `agui/AgUiEventParserTest.kt`
- [ ] `model/RichCardPayload.kt`（compat）
- [ ] `model/ChatPreviewData.kt`（testing）
- [ ] `compose/agentcard/ToolCallCard.kt`
