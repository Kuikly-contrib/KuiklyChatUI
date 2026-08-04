---
name: kuikly-dynamic-page-generator
description: "Kuikly 动态化页面端到端生成器。输入需求文档，自动完成：模块定位 → 动态化检测 → 代码生成 → 编译打包 → 输出 DEX/JS 产物。当用户需要从需求文档生成完整的动态化页面并打包产物时使用。"
---

# Skill: Kuikly 动态化页面生成

## 目标

根据需求文档，端到端完成 Kuikly 动态化页面的全流程：**模块定位 → 动态化检测 → 资源处理 → 代码生成 → 编译打包 → 产物输出**。最终交付物是可部署的动态化产物文件（DEX/JS），而非代码或编译指引。

---

## 触发条件

用户提供以下输入时触发本 skill：
- 需求文档（文字描述、设计稿、PRD 等）
- 明确需要生成动态化页面并打包产物

---

## 输入

| 参数 | 说明 | 是否必须                               |
|------|------|------------------------------------|
| 需求文档 | 页面功能描述，包含 UI 结构、交互逻辑、数据源等 | ✅ 必须                               |
| 目标模块 | 指定代码放置的 Kuikly 业务模块名 | 可选（未指定时自动检测）                       |
| 目标平台 | Android DEX / iOS JS / 鸿蒙 JS / H5 JS | 可选（默认 DEX + JS 分包构建）               |
| 可用能力声明 | 声明页面可调用能力，如：`MyNetModule 负责网络请求`、`AppRouter 负责页面跳转`。可按模块名或能力维度描述 | 可选（未指定时自动扫描当前模块 Module 及内置 Module） |

---

## 输出

编译成功的动态化产物文件，并输出以下信息：

```
✅ 动态化产物构建完成

📦 产物信息：
- 平台：${platform}
- 产物路径：${absolutePath}
- 产物大小：${fileSize}
- 页面名称：${PageName}
```

---

## 执行步骤

### Step 0：模块定位

读取 `settings.gradle.kts`，识别所有 Kuikly 业务模块（含 `id("kuikly")` 插件声明）。

```
CHECK: 用户是否指定了目标模块？
  ├── YES → 直接使用指定模块
  └── NO  → 自动检测业务模块
              ├── 检测到 1 个 → 自动使用
              ├── 检测到多个 → 必须对话确认，列出模块名让用户选择
              └── 检测到 0 个 → 提示用户需要先创建 Kuikly 模块
```

确认后输出：
```
✅ 目标模块：${moduleName}
   模块路径：${modulePath}
   构建文件：${buildFileName}
```

---

### Step 1：动态化能力检测

读取目标模块的 `build.gradle.kts`，按产物类型分别检测所需配置。

#### 1.1 通用检测（必须通过）

| 检测项 | 检测关键字 | 缺失时的处理 |
|--------|-----------|-------------|
| kuikly 插件 | `id("kuikly")` + 根目录 classpath | ⚠️ 告知用户需要启用 kuikly 插件 |

#### 1.2 JS 产物检测（iOS / 鸿蒙 / H5）

| 检测项 | 检测关键字 | 缺失时的处理 |
|--------|-----------|-------------|
| JS target | `js(IR) { ... binaries.executable() }` | ⚠️ 告知用户需要添加，提供配置模板（见 [DYNAMIC_CONFIG.md](references/DYNAMIC_CONFIG.md)） |
| kuikly JS 配置 | `kuikly { js {} }` | 自动在 Step 4 中补充配置 |

#### 1.3 DEX 产物检测（Android）

| 检测项 | 检测关键字                                                                                         | 缺失时的处理                                                                                                                             |
|--------|-----------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------|
| Android target | `androidTarget()` 或 `android()`                                                               | ⚠️ 告知用户需要添加 Android target 配置                                                                                                      |
| kuikly DEX 配置 | `kuikly { dynamicApk {} }`                                                                    | 自动在 Step 4 中补充配置                                                                                                                   |
| 壳模块（APK Builder） | 存在 `kuikly-dynamic-apk-builder` 模块，**或** `dynamicApk { shellProjectName = "xxx" }` 中指定了自定义壳模块 | ⚠️ 告知用户需要引入壳模块 |

#### 1.4 检测结果处理

> 任一必要项缺失且无法自动补充时，**中止流程并告知用户修复方法**。
> 如果用户仅需要某一种产物类型（如仅 JS/DEX），则只需通过对应维度的检测即可。

---

### Step 2：资源处理

如果需求涉及本地图片、图标等资源文件，**必须先将资源放置到正确的目录**，再进行代码生成，确保代码中的资源引用路径与实际文件一致。

#### 2.1 判断是否需要资源处理

```
CHECK: 需求中是否涉及本地图片、图标、动画文件等资源？
  ├── YES → 执行资源放置
  └── NO  → 跳过本步骤，进入 Step 3
```

#### 2.2 资源放置规则

资源文件存放在目标模块的 `src/commonMain/assets` 目录下：

```
${moduleName}/src/commonMain/assets/
├── ${pageName}/          # 页面专属资源（推荐）
│   ├── icon_cart.png
│   └── bg_header.png
└── common/               # 多页面共享的公共资源
    └── logo.png
```

**放置原则**：
- **页面专属资源** → 放在 `assets/${pageName}/` 目录下，代码中通过 `ImageUri.pageAssets("xxx")` 加载
- **公共资源**（多页面复用）→ 放在 `assets/common/` 目录下，代码中通过 `ImageUri.commonAssets("xxx")` 加载
- 如果目录不存在，**自动创建**对应目录

#### 2.3 执行步骤

1. 从需求文档中识别所有需要的本地资源文件
2. 检查用户是否提供了资源文件（图片、图标等）
3. 如果用户提供了资源 → 将文件复制到对应的 assets 目录
4. 如果用户未提供资源 → 跳过
5. 记录已确认的资源文件名，供 Step 3 代码生成时引用

---

### Step 3：动态化页面代码约束（⚠️ 必须遵守）

动态化代码运行在 JS/DEX 引擎中，存在以下约束：

**禁止使用**：
- ❌ 没有 JS 实现的 KMP 库（检查依赖是否有 `jsMain` sourceSet）
- ❌ `expect/actual` 声明（动态化代码只在 commonMain 中编写）
- ❌ 平台特定 API（如 `android.*`、`platform.UIKit.*`）

**运行时隔离**：
- ⚠️ 动态化页面与内置页面运行在**独立的隔离环境**中，两者的运行时实例互不共享
- ⚠️ 单例对象（`object`）、全局变量、静态缓存等在内置页面和动态化页面中会各自持有**独立的一份实例**，数据互不可见
- ⚠️ 因此，不要依赖单例或全局状态在内置页面与动态化页面之间共享数据，跨页面通信应通过 `NotifyModule` 或自定义 Module 桥接实现

**可调用能力**：

> **能力选择优先级：用户声明 > 项目已有 > 内置兜底**
>
> 1. 若用户在输入中提供了「可用能力声明」，**严格按照用户声明的 Module 和能力范围生成代码**
> 2. 若用户未声明，扫描目标模块中已有的自定义 Module（搜索继承自 `Module` 的类），优先使用项目中已存在的 Module
> 3. 仅当以上都没有对应能力的 Module 时，才使用 Kuikly 内置 Module 兜底（`NetworkModule`、`RouterModule`、`CalendarModule` 等）
>
> **示例**：用户声明 `MyNetModule 负责网络请求, AppRouterModule 负责页面跳转` → 代码中网络请求使用 `MyNetModule`，页面跳转使用 `AppRouter`，其他未声明的能力按优先级 2→3 处理

---

### Step 4：更新构建配置

在目标模块的 `build.gradle.kts` 中，将新页面添加到分包列表：

```kotlin
kuikly {
    js {
        addSplitPages(listOf("${PageName}"))
    }
    dynamicApk {
        addSplitPages(listOf("${PageName}"))
    }
}
```

---

### Step 5：编译打包（必须实际执行）

> ⚠️ **必须使用 terminal 工具实际执行编译命令**，不能仅给出命令让用户自行执行。

#### 5.1 各平台编译命令

| 平台 | 产物类型 | Gradle 任务 |
|------|---------|------------|
| Android DEX | `.apk` | `:${moduleName}:packSplitApkRelease` |
| iOS/鸿蒙/H5 JS | `.js` | `:${moduleName}:packSplitJSBundleXX` |
| 全量 JS（开发） | `.js` | `:${moduleName}:packLocalJSBundleXX` |
| 全量 DEX（开发） | `.apk` | `:${moduleName}:distributeApkToStaticServerDebug` |

> `XX` 后缀根据构建类型替换（如 `Debug`/`Release`）

#### 5.2 执行流程

1. 根据目标平台确定 Gradle 任务（未指定时默认 JS 分包构建）
2. 使用 terminal 工具在项目根目录执行：
   ```bash
   ./gradlew :${moduleName}:packSplitJSBundleRelease
   ```
3. 检查命令退出码
4. 编译失败 → 分析错误日志并修复后重新执行（排查指引见 [DYNAMIC_CONFIG.md](references/DYNAMIC_CONFIG.md)）
5. 编译成功 → 进入 Step 6

---

### Step 6：产物验证与输出（必须完成）

编译成功后，**必须验证产物文件存在并向用户报告**：

1. 使用 terminal 工具检查产物目录：
   ```bash
   ls -la ${moduleName}/build/outputs/kuikly/
   ```

2. 预期产物位置：
   ```
   ${moduleName}/build/outputs/kuikly/
   ├── apk/release/split/final/    # Android DEX 产物
   │   └── kuikly_dynamic.zip
   ├── js/release/split/           # JS 产物
   │   └── ${outputName}.zip
   ```

3. 输出最终产物信息（格式见「输出」章节）

4. 产物不存在 → 排查原因（检查 Gradle 日志、kuikly 配置等），修复后重新编译直到产物成功生成

---

## 示例调用

**用户输入：**
> 开发一个动态化页面，可以实现点击图片掉金币，图片资源位于xxx

**执行流程：**
1. **模块定位**：读取 settings.gradle.kts → 发现 demo、demo_sub 两个业务模块 → 询问用户选择 → 用户选择 demo
2. **动态化检测**：检查 demo/build.gradle.kts → ✅ js(IR) 已配置 → ✅ androidTarget 已配置 → ✅ kuikly 插件已启用 → ✅ 壳模块已引入
3. **资源处理**：需求涉及图片 → 将用户提供的图片放置到 `demo/src/commonMain/assets/ImageCoinPage/`
4. **代码生成**：遵循 Step 3 动态化页面代码约束 → 生成 ImageCoinPage.kt → analyze_files 验证通过
5. **更新配置**：在 `kuikly { js { addSplitPages(...) }; dynamicApk { addSplitPages(...) } }` 中添加 "ImageCoinPage"
6. **编译打包（JS）**：执行 `./gradlew :demo:packSplitJSBundleRelease` → ✅ 编译成功
7. **编译打包（DEX）**：执行 `./gradlew :demo:packSplitApkRelease` → ✅ 编译成功
8. **产物输出**：
   - ✅ JS 产物：demo/build/outputs/kuikly/js/release/split/xxx.zip (128KB)
   - ✅ DEX 产物：demo/build/outputs/kuikly/apk/release/split/final/kuikly_dynamic.zip (256KB)

---

## 参考文件

- **动态化配置模板**：[DYNAMIC_CONFIG.md](references/DYNAMIC_CONFIG.md) — JS target 配置模板、kuikly 插件配置、常见编译问题
