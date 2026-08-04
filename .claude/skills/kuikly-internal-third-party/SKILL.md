---
name: kuikly-internal-third-party
description: 查询 Kuikly 内部第三方组件库。帮助用户按关键词或平台查找内部 KMP 组件，查看组件详情（Git 地址、平台支持、动态化兼容性），并通过 clone 仓库阅读 README 和源码了解使用方法。适用场景：查找内部组件、内部三方库、KMP 组件、内网组件、存储/网络/数据库/日志/序列化/动画/路由等内部组件查询。需内网环境。
---

# Kuikly 内部第三方组件查询

## 使用事件上报（必须执行）

**每次 Skill 被调用时，必须首先执行：**

```bash
bash "${SKILL_DIR}/scripts/beacon-report.sh" &
```

## 工作流程

### 1. 明确需要查询的组件
明确用户要查询的是哪一个组件，获取 Git 地址。完整的组件列表请查阅 `references/components.md`。

### 2. 将组件 clone 到 skill 的 reference 目录下
找到 Git 地址后，在当前 skill 的 reference 目录下执行 `git clone`，如果已存在则执行 `git pull` 更新。

### 3. 阅读 README.md
在组件目录下找到 README.md，了解组件的基本使用方法和依赖引入方法。

### 4. 获取组件详细信息
如果还有不清楚的地方，通过阅读组件源码获取详细信息。

## 组件列表

完整的 40+ 内部组件信息（含 Git URL、平台支持、动态化兼容性等）请查阅 **`references/components.md`**。

### 支持动态化模式直接引用的组件
shrinker、kuiklyx-viewmodel、kuiklyx-bridge、json-mate、kotlinx.collections.immutable、okio、kotlinx-datetime、kotlinx.serialization、atomicfu

### 常用组件分类速查
- **存储**：MMKV-KMP、kmmkv、kvkmm、QBKVSDK
- **网络**：khttp、QBHttpSDK、pbservice
- **数据库**：VBSQLite、QBSQLiteSDK、QBDBHelperSDK、Androidx-Room
- **日志**：tmmlogger、kloger、QBDiagnoseLoggerSDK
- **序列化**：json-mate、kjson、kotlinx.serialization、TMMWire、jceTool
- **动画**：LottieKMM、PagKMM、QBLottieSDK
- **UI 组件**：QBUIComponentSDK、markdown-compose-renderer
- **路由**：kuiklyx-bridge、VBRouter、QBPageSDK
- **工具**：tmm-platform-utils、KotlinStdPlatformExt、Stately、tmm-concurrency、kotlinx-coroutines、QBPermission、QBClipBoard

## 使用说明
- 需要在内网环境使用
- 通过 `git clone` 获取组件代码
- 通过 `git pull` 更新组件代码
- 通过 README.md 了解基本使用方法
- 通过组件源码了解详细使用方法
