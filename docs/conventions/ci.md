---
last_updated: 2026-09-21
status: active
owner: @kuikly-chat-team
---

# 工蜂门禁

本地与工蜂跑同一条入口：`bash scripts/ci-gates.sh`。`.github/workflows/` 只是门禁定义的书面对照，工蜂不会触发 GitHub Actions。

## 四项 Gate

| Gate | 命令 | Runner | 阻断 |
|------|------|--------|------|
| 架构 / 文件大小 / public keep-set | `bash scripts/ci-gates.sh layer` | Linux | 是 |
| Android 编译 + 单测 | `bash scripts/ci-gates.sh android` | Linux + JDK 17 | 是 |
| iosSimulatorArm64 编译 | `bash scripts/ci-gates.sh ios` | macOS + Xcode / K/N | 是 |
| 一个 Ohos 库编译 | `bash scripts/ci-gates.sh ohos` | 带 Ohos KMP 工具链 | 是 |

开发机默认 `bash scripts/ci-gates.sh`（layer + android）。本机有 iOS / Ohos 工具链时用 `all`。

## 在工蜂打开

1. 仓库 **Settings → CI/CD** 打开流水线。
2. **CI configuration file** 设为 `.tgit-ci.yml`。
3. 登记两类 runner，tag 与 yaml 里 `ANDROID_RUNNER_TAG` / `MACOS_RUNNER_TAG` 一致（默认 `linux` / `macos`）。
4. Ohos job 目前和 Android 共用 Linux runner；机器上必须能跑 `-c settings.ohos.gradle.kts :kuiklychat-core:compileKotlinOhosArm64`。没有工具链就把该 job 的 tag 改到已装 Harmony KMP 的机器。
5. 若团队走蓝盾而不是工蜂 GitLab CI：新建流水线，四个 Job 分别执行上表命令，不要再复制一份 Gradle 任务列表。

未完成（刻意不做）：binary / Native 导出面 ABI dump、全 Native target、发布流水线。当前 keep-set 仍是文本 dump。
