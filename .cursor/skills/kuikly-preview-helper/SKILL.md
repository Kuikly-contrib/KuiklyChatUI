---
name: kuikly-preview-helper
description: Kuikly 预览辅助工具。自动为 Kuikly Pager（传统 DSL）和 Kuikly Compose DSL 添加 @KPreview 注解，提供预览数据Mock思路（内置常用Mock数据），并协助重构提升可预览性。适用场景：Kuikly 预览、Kuikly Compose 预览、Kuikly 预览异常，Kuikly 预览Mock
---

# Kuikly 预览辅助工具（Preview Helper）

## 使用事件上报（必须执行）

**每次 Skill 被调用时，必须首先执行：**

```bash
bash "${SKILL_DIR}/scripts/beacon-report.sh" &
```

## 🚨 核心原则：从源头 Mock

> **CRITICAL**: Mock 的位置越接近数据源头，改动范围越小，预览效果越真实。
> 
> **黄金法则**：在 Manager 层或 Module 层做一次拦截，所有下游 UI 自动受益，零改动。

| 优先级 | Mock 层次 | 改动范围 | 说明 |
|--------|----------|---------|------|
| ⭐ P0 最优 | **Manager 层源头 Mock** | 改 1 处 | 所有 UI 自动生效 |
| ⭐ P0 次优 | **Module 层源头 Mock** | 改 1 处 | 所有 Manager 自动生效 |
| P1 中等 | ViewModel / 数据加载函数 Mock | 每个 ViewModel 改 1 处 | - |
| P2 最差 | UI 层表面 Mock | 每个页面都要改 | 侵入性最强 |

**示例（Manager 层源头 Mock）：**

```kotlin
internal object AppFeedsManager {
    internal fun requestFeeds(type: AppFeedsType, page: Int, callback: (List<AppFeedModel>, String) -> Unit) {
        // ======= 源头 Mock：检测预览环境 =======
        val pager = PagerManager.getCurrentPager()
        if (pager.pageData.inspectionMode) {
            callback(MockFeeds.feedList(10), "")
            return
        }
        // ======= 原有真实数据加载逻辑 =======
        // ...
    }
}
```

## 概述

此 Skill 帮助开发者快速为 Kuikly 页面和组件添加预览支持，覆盖以下核心场景：

1. **自动添加 `@KPreview` 注解** —— 支持传统 Kuikly DSL（Pager）和 Compose DSL（@Composable）
2. **Mock 全局变量注入** —— 解决 ThemeManager、LangManager、Module 等全局依赖在预览环境下的问题
3. **内置 Mock 数据** —— 提供网络图片、视频、用户信息、时间日期、文本变体、数字统计等常用测试数据
4. **重构建议** —— 分析代码并给出提升可预览性的改进方案

## ⚠️ Agent 行为约束（必须严格遵守）

> **这些规则的优先级高于你的默认行为。违反任何一条都是错误。**

1. **不要重复添加注解**：如果文件已有 `@KPreview` 注解，跳过或提示用户
2. **保持注解顺序**：`@KPreview` 注解放在 `@Page` 或 `@Composable` 之前
3. **确保 import 正确**：必须添加 `import com.tencent.kuikly.ui.tooling.KPreview`
4. **默认使用标准 12 设备配置**：除非用户明确要求精简版
5. **Compose DSL 必须有独立预览函数**：不能直接在 `private` 的 Composable 上加注解
7. **无参 Composable 要求**：预览要求是无参 Composable 函数，有参函数需要二次封装
8. **失败即停止**：遇到无法解决的问题，立即终止并反馈错误，不自动跳过
9. **输出格式规范**：完成分析后按照 [references/refactoring-checklist.md](references/refactoring-checklist.md) 末尾的「输出格式模板」格式输出重构建议报告

## 前置条件

> 📖 **详细的 IDE 环境配置和依赖检查请参考 [ENV-SETUP.md](ENV-SETUP.md)**

### Kotlin 版本要求

**预览功能要求 Kotlin 1.7.20 或更高版本**，低于此版本的项目无法使用预览功能，请先升级 Kotlin 版本。

### 接入 Gradle 依赖

> ⚠️ **Kuikly DSL 和 Compose DSL 的接入方式不同，请根据项目实际使用的 DSL 类型选择。**

#### 1. Kuikly DSL（传统 DSL）

**支持 SDK 不升级接入**。只需接入 `ui-tooling` 注解库即可：

```kotlin
// 1.7.20 为 Kotlin 版本，请更换为项目所需要的 Kotlin 版本，目前只支持 1.7.20 以上
implementation("com.tencent.kuikly:ui-tooling:2.16.0-beta-1.7.20")
```

#### 2. Compose DSL

需要**完整升级 SDK 版本到 2.16.0-beta**，并接入 `ui-tooling` 库，并通过 KSP 开启 Preview：

```kotlin
// 依赖：版本格式 {kuiklyVersion}-{kotlinVersion}
implementation("com.tencent.kuikly:ui-tooling:${Version.getKuiklyVersion()}")
```

**KSP 配置（仅 Compose DSL 需要）：**

```kotlin
ksp {
    arg("enablePreview", "true")
    // ... 其他 KSP 配置
}
```

## 执行流程

复制此清单跟踪进度：

```
预览接入进度：
- [ ] Step 1: 扫描可预览元素
- [ ] Step 2: 添加 @KPreview 注解
- [ ] Step 3: 分析并解决全局变量注入问题
- [ ] Step 4: 输出重构建议
- [ ] Step 5: 编译验证预览效果
```

---

### Step 1: 扫描可预览元素

扫描用户指定的文件或目录，识别以下可预览元素：

**传统 Kuikly DSL：**
- 带有 `@Page` 注解的类（继承 `Pager`、`ComposeContainer` 或自定义 BasePager）
- 独立的 `ComposeView` 子类组件

**Compose DSL：**
- 带有 `@Composable` 注解的顶层函数或 `ComposeContainer.setContent {}` 中的可组合函数
- 注意 Compose DSL 预览需要包装一个 `@Composable` 的预览函数

### Step 2: 添加 @KPreview 注解

根据元素类型，添加标准的多设备预览注解集：

```kotlin
import com.tencent.kuikly.ui.tooling.KPreview
```

#### @KPreview 注解参数说明

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `widthDp` | Int | 360 | 预览宽度（dp） |
| `heightDp` | Int | 640 | 预览高度（dp） |
| `density` | Float | 2.0f | 屏幕密度 |
| `name` | String | "" | 预览名称，显示在预览面板上 |
| `group` | String | "" | 分组名称，用于对预览进行分类显示 |
| `kuiklyPageClass` | KClass | - | 仅旧版本 SDK（< 2.16.0-beta）使用，指定要预览的 Pager 类 |

#### 标准预览配置集（8 种设备）

```kotlin
@KPreview(widthDp = 320, heightDp = 568, name = "小屏手机", density = 2.0f)
@KPreview(widthDp = 360, heightDp = 640, name = "标准手机", density = 2.0f)
@KPreview(widthDp = 393, heightDp = 851, name = "现代旗舰", density = 2.75f)
@KPreview(widthDp = 430, heightDp = 932, name = "超大屏手机", density = 3.0f)
@KPreview(widthDp = 673, heightDp = 841, name = "折叠屏内屏", density = 2.5f)
@KPreview(widthDp = 640, heightDp = 360, name = "横屏模式", density = 2.0f)
@KPreview(widthDp = 768, heightDp = 1024, name = "标准平板", density = 2.0f)
@KPreview(widthDp = 1194, heightDp = 834, name = "大屏平板横屏", density = 2.0f)
```

#### 对传统 DSL（Pager）添加注解 — 新版本（SDK ≥ 2.16.0-beta）

```kotlin
@KPreview
@Page("router")
internal class ComposeRoutePager : BasePager() {
    // pageData.inspectionMode == 是预览
}
```

#### 对传统 DSL（Pager）添加注解 — 旧版本（SDK < 2.16.0-beta）

> ℹ️ **说明**：旧版本是指 Kuikly SDK 版本为 2.16.0-beta 以前。接入 2.16.0-beta 版本的 `ui-tooling` 库后，并安装 IDE Plugin 就可以支持预览。

```kotlin
@KPreview(kuiklyPageClass = TargetKuiklyPage::class, density = 8.0f, widthDp = 360, heightDp = 360)
class TargetKuiklyPagePreview

@Page("router")
internal class TargetKuiklyPage : BasePager()
```

#### 对 Compose DSL 添加注解

```kotlin
import com.tencent.kuikly.ui.tooling.KPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode  // Compose DSL 预览环境检测

@KPreview(widthDp = 800, heightDp = 1024, name = "sdfd22", density = 3f, group = "test")
@Composable
fun AccessibilityDemo() {
    // LocalInspectionMode.current 是预览标志
}
```

### Step 3: 分析并解决全局变量注入问题

> 📖 **详细的依赖注入解决方案请参考 [references/global-dependency-injection.md](references/global-dependency-injection.md)**

按以下分类逐一排查并处理全局依赖：

| 类别 | 依赖类型 | 处理策略 | 详见 |
|------|---------|---------|------|
| A | PagerData、ThemeManager、LangManager | **无需处理**，预览自动注入或有默认值 | [依赖注入指南 - 类别 A](references/global-dependency-injection.md) |
| B | BridgeModule、TDFTestModule 等 Module | 在 `BasePager.createExternalModules()` 中统一注册 | [依赖注入指南 - 类别 B](references/global-dependency-injection.md) |
| C | Module 调用返回值 | 添加 `?.` 空安全保护 + 默认值 fallback | [依赖注入指南 - 类别 C](references/global-dependency-injection.md) |
| D | PlatformUtils、LocalActivity | 确保 else 分支完整 | [依赖注入指南 - 类别 D](references/global-dependency-injection.md) |
| E | inspectionMode 环境检测 | 传统 DSL 用 `pageData.inspectionMode`，Compose 用 `LocalInspectionMode.current` | [依赖注入指南 - 类别 E](references/global-dependency-injection.md) |
| F | 数据源头 Mock 策略 | **优先 Manager 层**，其次 Module 层做源头 Mock | [依赖注入指南 - 类别 F](references/global-dependency-injection.md) |

### Step 4: 输出重构建议

> 📖 **详细的重构检查清单请参考 [references/refactoring-checklist.md](references/refactoring-checklist.md)**
> 📖 **输出格式模板请参考 [references/refactoring-checklist.md](references/refactoring-checklist.md) 末尾的「输出格式模板」章节**

分析代码后输出以下两个类别的重构建议：

**类别 1：可预览性改进**（P0: 缺注解/Module 无空安全；P1: ComposeView 依赖外部数据/无独立预览函数；P2: 硬编码 URL）

**类别 2：架构改进**（P0: 业务+UI 混合；P1: 全局单例硬耦合/事件监听未清理；P2: 重复模式可抽基类）

### Step 5: 编译验证预览效果

完成注解添加和全局依赖处理后，进行编译验证：

1. **保存文件**：Ctrl+S / Command+S 保存所有修改过的文件
2. **触发全量编译**：首次预览需点击 **"Recompile"** 按钮触发全量 JavaC 编译
3. **检查预览面板**：确认预览面板是否正常显示内容
4. **验证多设备**：检查标准配置集（12 种设备）的预览效果，关注以下要点：
   - 小屏/大屏下布局是否正常（文字截断、元素溢出等）
   - 横屏模式下布局是否自适应
   - 平板尺寸下内容是否合理填充
5. **验证 Mock 数据**：确认 Mock 数据在预览中正确展示（图片是否加载、列表是否有内容）
6. **增量编译测试**：修改少量代码后保存，验证增量编译是否正常刷新预览

> ⚠️ 如果预览出现问题，请参考 [TROUBLESHOOTING.md](TROUBLESHOOTING.md) 中的排查流程。

---

## 内置 Mock 数据

> 📖 **完整的 Mock 数据定义和使用示例请参考 [references/mock-data.md](references/mock-data.md)**

Mock 数据包括：MockImages（头像/封面/九宫格）、MockVideos、MockUsers、MockFeeds、MockTimes、MockTexts、MockNumbers、MockColors。

使用时直接引用对应的 object 即可，如 `MockFeeds.feedList(5)`、`MockImages.AVATAR_1`、`MockUsers.USER_VIP` 等。

---

## 使用指南

1. **首次全量编译**：打开带 `@KPreview` 的文件 → 点击 **"Recompile"** 触发全量编译 → 预览面板自动显示
2. **增量编译**：修改代码后 Ctrl+S / Command+S 保存即触发增量编译，如不刷新多保存几次
3. **预览面板**：点击右上角预览区域引出面板，通过右下角 "+" "-" 调整缩放
4. **渐进式接入**：先选 1-2 个简单页面试点，通过后再逐步扩展

## 故障排查

> 📖 **详细的故障排查指南请参考 [TROUBLESHOOTING.md](TROUBLESHOOTING.md)**

### 常见问题速查

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 预览空白 | 数据加载依赖原生 Module | 使用 `inspectionMode` + Mock 数据 |
| 预览崩溃 (NPE) | `lateinit var` 未初始化 | 使用 `by observable(默认值)` |
| 图片不显示 | 本地 assets 路径错误 | 使用 Mock 图片 URL |
| 编辑后不刷新 | 增量编译未触发 | 多保存几次（Ctrl+S / Command+S） |
| 预览面板未出现 | IDE 未初始化完成 | 关闭并重新打开文件 |
| Recompile 后仍异常 | 需要反馈问题 | 访问 [Kuikly 预览文档](https://kuikly.woa.com/DevGuide/preview-ui.html) 提供反馈 |
| 新增文件不生效 | 首次需全量编译 | 点击 "Recompile" 或 Build > Make Project |

---

## 体验 Demo

> ⚠️ **注意：以下为内网 Git 仓库链接，需在公司内网环境下访问。**

```
https://git.woa.com/zhenhuachen/KuiklyUIMirror-Demo.git
```

项目包含 Kuikly DSL / Compose DSL 多屏幕预览示例、多实例预览示例、跨文件增量编译验证示例。

## 注意事项

1. **避免表面 Mock**：优先在 Manager/Module 层做源头 Mock，而不是每个页面判断 `inspectionMode`
2. **环境隔离**：Mock 数据只用于预览环境（`inspectionMode` / `LocalInspectionMode.current` 分支），生产环境不受影响，`@KPreview` 注解仅在 IDE 预览时生效
3. **版本兼容**：SDK ≥ 2.16.0-beta 可直接在 Pager 上加 `@KPreview`，旧版本需使用预览类模式
4. **Kuikly DSL 和 Compose DSL 可混用预览**：只要确保 `ui-tooling` 依赖已添加，两种注解可共存（KSP `enablePreview` 仅 Compose DSL 需要）
5. **预览性能优化**：列表 Mock 数据量建议 5-10 条；调试阶段可用精简配置集（4 种设备）
6. **多模块项目**：`ui-tooling` 依赖应添加在包含预览目标的 `commonMain` sourceSet 中
7. **团队协作**：建议将 ENV-SETUP.md 加入项目 README 或新人入职文档

## 参考文档

| 文档 | 说明 |
|------|------|
| [ENV-SETUP.md](ENV-SETUP.md) | IDE 环境配置详细步骤 |
| [TROUBLESHOOTING.md](TROUBLESHOOTING.md) | 故障排查速查指南 |
| [references/global-dependency-injection.md](references/global-dependency-injection.md) | 全局依赖注入解决方案 |
| [references/mock-data.md](references/mock-data.md) | Mock 数据定义与使用示例 |
| [references/refactoring-checklist.md](references/refactoring-checklist.md) | 重构检查清单 |
