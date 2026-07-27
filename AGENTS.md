# AGENTS.md

## 项目简介
KuiklyChat 是一套基于 Kuikly（Kotlin Multiplatform）的跨端 Chat UI 组件库，内置 AG-UI 协议接入，可被 Android / iOS / 鸿蒙 / H5 / 小程序等宿主复用。

## 快速导航
| 你想做什么 | 去哪里看 |
|-----------|---------|
| 了解模块构成、目录职责、使用方式 | `README.md` |
| 了解 AG-UI 协议与接入流程 | `AGUI_INTEGRATION_GUIDE.md` |
| 新增/修改消息卡片（Kuikly DSL 渲染层） | `KuiklyChat/src/commonMain/.../chat/agentcard/` |
| 新增/修改消息卡片（Compose DSL 渲染层） | `KuiklyChat/src/commonMain/.../chat/agentcard_compose/` |
| 处理 AG-UI 事件解析、会话编排 | `KuiklyChat/src/commonMain/.../chat/agui/` |
| 数据模型、主题、组件工厂、仓库 | `KuiklyChat/src/commonMain/.../chat/model/` |
| 会话页与配置（ChatSession） | `KuiklyChat/src/commonMain/.../chat/session/` |
| 气泡、频道、输入区、指示器、导航 | `.../chat/bubble/` `channel/` `composer/` `indicator/` `navigation/` |
| 构建/版本/发布到 Maven | `build.gradle.kts`、`buildSrc/`、`publish-maven.sh` |
| 各端宿主 App 集成示例 | `androidApp/` `iosApp/` `ohosApp/` `shared/` |
| 主观约定如何机械落地 | `docs/conventions/mechanical-rules.md` |
| 周期性卫生清理任务（Prompt） | `docs/plans/code-hygiene-cleanup.md` |
| 架构 + 文件大小门禁（本地可跑） | `scripts/check-layer-deps.sh` |
| 文档新鲜度检查（本地可跑） | `scripts/check-doc-freshness.sh` |
| CI 护栏配置（见下方「CI 现状」） | `.github/workflows/agent-guardrails.yml` |

> 路径前缀：`KuiklyChat/src/commonMain/kotlin/com/tencent/kuiklybase/chat/`

## 架构要点
- 核心库 `:KuiklyChat` 是 KMP library（js/android/ios/macos），通过 `:shared`、`:androidApp`、`:h5App`、`:miniApp` 及各端 App 复用。
- 消息卡片有**两套并行渲染层**：`agentcard/`（传统 Kuikly DSL）与 `agentcard_compose/`（Compose DSL），语义必须对齐。
- 数据流：`agui/`（事件解析）→ 卡片映射 → `session/` 会话渲染；后端协议细节只存在于 `agui/`，不向下游 UI 泄漏。

## 硬性规则（必须遵守）
1. `commonMain` 必须平台无关；平台相关逻辑只能放在 `androidMain` / `iosMain` / `macosMain`。
2. 新增消息卡片须**同时在 `agentcard/` 与 `agentcard_compose/` 实现**，并在对应映射器/工厂注册，缺一不可。
3. 所有后端协议对接走 `agui/` 包，禁止在 `bubble/`、`session/` 等 UI 层直接耦合 AG-UI 协议细节。
4. 单文件 ≤ 400 行；超大文件优先拆分为职责单一的子文件，而非继续堆砌。存量超标文件登记在 `scripts/file-size-baseline.txt`（仅告警），**新增超标文件直接阻断**。
5. 使用结构化日志，调试遗留的裸 `println` / `console.log` 不得合入。
6. 以上规则由 `scripts/check-layer-deps.sh` 强制校验（零依赖，本地与 CI 同一条命令），违规阻断合入。

## 提交规范
- `feat:` 新功能/新卡片
- `fix:` 修复
- `refactor:` 重构（不改变行为）
- `docs:` 文档
- `test:` 测试
- `chore:` 构建/依赖/发布

## 复现与验证
改动后按顺序跑这三条，全绿才提交（等价于 CI 门禁）：

```bash
bash scripts/check-layer-deps.sh                      # 架构 + 文件大小，~0.5s
./gradlew :KuiklyChat:compileDebugKotlinAndroid \
          :KuiklyChat:compileDebugUnitTestKotlinAndroid
./gradlew :KuiklyChat:testDebugUnitTest               # 81 个用例，报告见 build/reports/tests/
```

- 本模块**没有 `jvm()` target**（只有 android / js / iosX64 / iosArm64 / iosSimulatorArm64 / macosX64 / macosArm64），
  所以不存在 `compileKotlinJvm` 之类的任务，JVM 侧编译一律走 android target 的任务名。
- `commonTest` 是多平台共享源集，**只能用 `kotlin("test")`**；引入 JVM-only 依赖（JUnit、Konsist、Mockito 等）
  会让 js / ios / macos 各 target 的测试编译全部失败。
- 在隔离 worktree 中验证某个分支：`bash scripts/agent-verify.sh <branch>`
- 构建库：`./gradlew :KuiklyChat:build`
- 发布 Maven：`./publish-maven.sh`（参数 `mavenVersion` / `groupId` / `mavenRepoUrl` / `mavenUsername` / `mavenPassword`）
- 运行宿主：Android 用 `:androidApp`，其余见各端目录。

## 门禁在哪里执行（重要）
**当前不接 CI，门禁完全靠本地执行**——提交前跑上面「复现与验证」的三条命令即可，全绿才提交。

仓库 remote 是 `git.woa.com`（工蜂），`.github/workflows/` 下的 workflow 不会被触发，
保留它们只是作为门禁定义的书面参考（哪些 Gate、各自校验什么）。不要以为改了 yml 就会生效，
也不要因为「CI 会拦」而跳过本地校验。真正可执行的只有 `scripts/` 下的两个脚本。
