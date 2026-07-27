---
last_updated: 2026-07-27
status: active
owner: @kuikly-chat-team
---

# 主观品味 → 机械规则映射表

经验法则：**一条规则在 Code Review 中被提过 3 次以上，就写成可机械检查的护栏。**
下表记录 KuiklyChat 团队约定及其落地方式（对应原方法论 3.4）。

状态含义：**✅ 脚本已强制**＝本地跑 `bash scripts/check-layer-deps.sh` 就能复现并阻断；
**⚠️ 仅约定**＝无自动校验，靠 Code Review；**📋 待启用**＝工具尚未接入。

| 团队口头约定 | 机械规则 | 落地方式 | 状态 |
|------------|---------|---------|------|
| 分层依赖方向 | `agui` 不依赖 UI 包 | `check-layer-deps.sh` 规则 2 | ✅ 脚本已强制 |
| 双渲染层解耦 | `agentcard` 不依赖 `agentcard_compose` | `check-layer-deps.sh` 规则 3 | ✅ 脚本已强制 |
| 平台隔离 | `commonMain` 不引 `android./java./javax./platform./kotlinx.cinterop` | `check-layer-deps.sh` 规则 1 | ✅ 脚本已强制 |
| 日志要规范 | 禁止裸 `println` | `check-layer-deps.sh` 规则 4 | ✅ 脚本已强制 |
| 文件要短 | 单文件 ≤ 400 行 | `check-layer-deps.sh` 规则 5 + `scripts/file-size-baseline.txt` | ✅ 脚本已强制（存量豁免，新增阻断） |
| 文档不过期 | 设计文档 `last_updated` ≤ 60 天 | `scripts/check-doc-freshness.sh`（PR Gate 7 + 每周定时） | ✅ 脚本已强制（仅告警） |
| 组件要纯 | UI 组件不直接调后端 | 由「`agui` 不依赖 UI」反向保证；UI 直连后端目前无检查 | ⚠️ 仅约定 |
| API 调用要统一 | 禁止直接网络调用，走 `ChatRepository` | 无自动校验 | ⚠️ 仅约定 |
| 错误要有上下文 | Error 须带 code / 类型 | `AgUiEvent.RunError` + `docs/reference/error-codes.md` 分类 | ⚠️ 仅约定 |
| 主题不硬编码 | 不写死色值，走 `ChatTheme` / `AgentCardTheme` | 无自动校验 | ⚠️ 仅约定 |
| 函数要短 | 单函数 ≤ 50 行 | Detekt `LongMethod` | 📋 待启用 Detekt |
| 别用 any | 禁止 `kotlin.Any` 强转滥用 / 禁止 `!!` | Detekt `UnsafeCallOnNullableType` | 📋 待启用 Detekt |
| 测试要充分 | 覆盖率 ≥ 80% | Kover（CI Gate 4，`continue-on-error`） | 📋 待启用 Kover |

## 为什么校验集中在一个 shell 脚本里
早期方案是再配一份 Konsist 单测（`ArchitectureKonsistTest`）跑同样的规则，已移除，原因：
Konsist 只支持 JVM，放进 KMP 的 `commonTest` 会让 js / ios / macos 各 target 的测试编译直接失败；
而它校验的三条规则与 `check-layer-deps.sh` 完全重合。现在**分层校验只有 shell 脚本一个事实来源**，
零依赖、100ms 出结果、本地和 CI 跑的是同一条命令。

## 工作流
- 新约定先在 `docs/conventions/` 落文档 → 再固化进 `scripts/check-layer-deps.sh` 或 Detekt。
- 加规则时同步给出 ❌ 现象 / ✅ FIX / 📖 文档链接三要素，Agent 才能自助修复。
- 状态列必须如实反映当前是否真的有机械校验，不要把「仅约定」写成「已强制」。
- Detekt / Kover 启用方式见 `agent-guardrails.yml` 中对应 Gate 的注释。
