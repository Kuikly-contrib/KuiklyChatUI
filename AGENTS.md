# AGENTS.md

## 项目简介
KuiklyChat 是一套基于 Kuikly（Kotlin Multiplatform）的跨端 Chat UI 组件库，内置 AG-UI 协议接入，可被 Android / iOS / 鸿蒙 / H5 / 小程序等宿主复用。

当前发布线是 **1.0.x**，当前版本是 `1.0.5`；public dump 仍是文本基线，不是 binary ABI 承诺。宿主按需依赖发布模块；接入说明：`docs/api/host-integration.md`，使用指南：`docs/api/usage.md`。

## 快速导航
| 你想做什么 | 去哪里看 |
|-----------|---------|
| 了解模块构成、目录职责、使用方式 | `README.md`、`docs/api/host-integration.md`、`docs/api/usage.md`、`docs/api/chat-ui.md` |
| 了解 AG-UI 协议设计 | `docs/design/feature-agui-protocol.md` |
| 新增/修改消息卡片（Kuikly DSL 渲染层） | `kuiklychat-compat/src/commonMain/.../session/controller/` |
| 新增/修改 Compose Chat SDK | `kuiklychat-compose/` + `kuiklychat-core/` |
| 新增/修改消息卡片（Compose DSL 渲染层） | `kuiklychat-compose/src/commonMain/.../compose/agentcard/` |
| 处理流式传输与 provider | `kuiklychat-ai/src/commonMain/.../kuiklyai/` |
| 数据模型、主题、仓库 | `kuiklychat-core/`、`kuiklychat-compat/.../model/` |
| Kuikly DSL 会话页 | `kuiklychat-compat/.../session/controller/`（`ChatControllerSession`） |
| 频道列表 | `kuiklychat-compat` 的 `channel/` |
| 构建/版本/发布到 Maven | `build.gradle.kts`、`buildSrc/`、`publish-maven.sh` |
| 各端宿主 App 集成示例 | `androidApp/` `iosApp/` `ohosApp/` `shared/` |
| 主观约定如何机械落地 | `docs/conventions/mechanical-rules.md` |
| 工蜂门禁怎么开 | `docs/conventions/ci.md`、`.tgit-ci.yml` |
| 周期性卫生清理任务（Prompt） | `docs/plans/code-hygiene-cleanup.md` |
| 架构 + 文件大小门禁 | `scripts/check-layer-deps.sh` |
| 文档新鲜度检查 | `scripts/check-doc-freshness.sh` |

> 库源码前缀：各 `kuiklychat-*` 模块的 `src/commonMain/kotlin/com/tencent/kuiklybase/chat/`

## 架构要点
- 发布模块：`:kuiklychat-core` / `:kuiklychat-compose` / `:kuiklychat-compat` / `:kuiklychat-ai` / `:kuiklychat-testing`，以及 Android 原生模块 `:KuiklyChatAndroid`。
- 消息模型只有一套：共用 `compose.core.ChatMessage` + `MessagePart.AgentCardContent` / `AgentCard`。新卡片只扩这一套模型。
- `MessagePart.AgentCardContent` 短期保留，不要再往 Core 塞卡片字段。
- 数据流：`kuiklychat-ai` 的 provider / transport → `ChatController` → Kuikly DSL 或 Compose UI；后端协议细节不得进入 UI 层。

## 硬性规则（必须遵守）
1. `commonMain` 必须平台无关；平台相关逻辑只能放在 `androidMain` / `iosMain` / `macosMain`。
2. 新增共享消息卡片字段时，必须同时使用 `compose.core.ChatMessage` / `MessagePart`，并在 Kuikly DSL 与 Compose 两套 UI 中验证渲染。
3. 所有后端协议对接走 `kuiklychat-ai` provider / transport 或宿主注入边界，禁止在 `session/controller/`、`channel/`、`compose/ui/` 等 UI 层直接耦合协议细节。
4. 单文件 ≤ 400 行；超大文件优先拆分为职责单一的子文件，而非继续堆砌。存量超标文件登记在 `scripts/file-size-baseline.txt`（仅告警），**新增超标文件直接阻断**。
5. 使用结构化日志，调试遗留的裸 `println` / `console.log` 不得合入。
6. 以上规则由 `scripts/check-layer-deps.sh` 强制校验（零依赖，本地与工蜂同一条命令），违规阻断合入。

## 提交规范
- `feat:` 新功能/新卡片
- `fix:` 修复
- `refactor:` 重构（不改变行为）
- `docs:` 文档
- `test:` 测试
- `chore:` 构建/依赖/发布

## 复现与验证
改动后跑工蜂同一套入口（开发机默认 layer + android）：

```bash
bash scripts/ci-gates.sh            # layer + Android compile/test
# 本机有工具链时再跑：
bash scripts/ci-gates.sh ios        # iosSimulatorArm64
bash scripts/ci-gates.sh ohos       # :kuiklychat-core Ohos 编译
```

- 本模块**没有 `jvm()` target**（只有 android / js / iosX64 / iosArm64 / iosSimulatorArm64 / macosX64 / macosArm64），
  所以不存在 `compileKotlinJvm` 之类的任务，JVM 侧编译一律走 android target 的任务名。
- `commonTest` 是多平台共享源集，**只能用 `kotlin("test")`**；引入 JVM-only 依赖（JUnit、Konsist、Mockito 等）
  会让 js / ios / macos 各 target 的测试编译全部失败。
- 在隔离 worktree 中验证某个分支：`bash scripts/agent-verify.sh <branch>`
- 构建库：按模块使用 `./gradlew :kuiklychat-core:build` 等任务
- 发布 Maven：`./publish-maven.sh`（参数 `mavenVersion` / `groupId` / `mavenRepoUrl` / `mavenUsername` / `mavenPassword`）；会发布五个 `kuiklychat-*` 模块与 `KuiklyChatAndroid`
- 运行宿主：Android 用 `:androidApp`，其余见各端目录。

## 门禁在哪里执行
工蜂配置文件是 [`.tgit-ci.yml`](.tgit-ci.yml)，打开方式见 [`docs/conventions/ci.md`](docs/conventions/ci.md)。

`.github/workflows/` 不会被工蜂触发，只作书面对照。本地未配工蜂 runner 时，提交前仍须跑 `bash scripts/ci-gates.sh`。
