---
name: kuikly-build-helper
description: Kuikly 跨端框架编译问题排查指引。排查编译错误、打包失败、热重载无法访问、页面找不到等问题。覆盖 Android、iOS、鸿蒙三端，包含 Gradle 配置、KSP/KAPT、动态化打包、Kotlin 版本兼容等解决方案。适用场景：编译报错、构建失败、build error、编译不过、打包出错、热更新问题、kuiklyCoreEntry 找不到、Task not found、pager not found。
---

# Kuikly 编译问题排查指引

## 使用事件上报（必须执行）

**每次 Skill 被调用时，必须首先执行：**

```bash
bash "${SKILL_DIR}/scripts/beacon-report.sh" &
```

## 概述

本 skill 用于帮助排查 Kuikly 跨端框架的编译和运行问题。Kuikly 支持 Android、iOS、鸿蒙三端，
编译问题通常涉及 Gradle 配置、Kotlin 版本、KSP 插件、动态化打包等方面。

## 问题排查流程

遇到 Kuikly 编译问题时，按以下步骤进行排查：

### 第一步：识别问题类型

根据错误信息，将问题归类为以下类型之一：

1. **Gradle 配置问题** - Task not found、依赖解析失败等
2. **Kotlin 版本问题** - 版本不兼容、K2 模式冲突等
3. **KSP/KAPT 问题** - 入口类找不到、页面收集失败等
4. **动态化打包问题** - dex/js 打包失败、分页不生效等
5. **热重载问题** - 无法访问、端口映射等
6. **平台特定问题** - Android/iOS/鸿蒙各端特有问题

### 第二步：查阅参考文档

根据问题类型，查阅 `references/build-errors.md` 获取详细的问题描述和解决方案。

### 第三步：执行排查步骤

按照参考文档中的解决方案逐步排查，常见的通用排查步骤包括：

1. 清理构建缓存：
   ```bash
   ./gradlew clean
   ./gradlew --stop
   # Android Studio: Build → Clean Project
   ```

2. 检查依赖版本一致性：
   ```bash
   ./gradlew <module>:dependencies
   ```

3. 检查 Gradle 配置文件 (`build.gradle.kts`, `gradle.properties`)

4. 检查 Kuikly 插件配置

## 诊断表

| 错误关键词 | 可能原因 | 参考章节 |
|-----------|---------|---------|
| `kuiklyCoreEntry` 找不到 | KSP 未正确配置或缓存问题 | KSP/KAPT 问题 |
| `Task not found` | Gradle 配置问题 | Gradle 配置问题 |
| `pager not found` | core-render-android 依赖配置错误 | 动态化打包问题 |
| `IR lowering failed` | KMP 依赖冲突 | 鸿蒙编译问题 |
| `K2 mode` | Android Studio 插件兼容性 | Kotlin 版本问题 |
| `Not mapped` | so 和头文件不匹配 | 鸿蒙编译问题 |
| `kotlinStoreYarnLock` | yarn.lock 缓存问题 | 鸿蒙编译问题 |
| 热重载无法访问 | 网络/端口问题 | 热重载问题 |

## 资源文件说明

- `references/build-errors.md` - 详细的编译错误分类和解决方案
