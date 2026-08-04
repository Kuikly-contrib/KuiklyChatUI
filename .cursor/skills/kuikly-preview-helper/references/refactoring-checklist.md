# Kuikly 预览重构清单

本文档提供系统化的代码重构检查清单，帮助提升 Kuikly 业务代码的可预览性。

## 重构检查清单

### 一、页面级检查（Page / Pager）

#### 1.1 @KPreview 注解检查

- [ ] 所有 `@Page` 类是否都添加了 `@KPreview` 注解
- [ ] `@KPreview` 是否覆盖了目标设备的屏幕尺寸
- [ ] 是否有 `import com.tencent.kuikly.ui.tooling.KPreview`
- [ ] Compose DSL 页面是否有独立的 `@Composable` 预览函数
- [ ] 是否合理使用了 `group` 参数对预览进行分组
- [ ] 旧版本 SDK（< 2.16.0-beta）是否使用了预览类模式（`kuiklyPageClass`）

#### 1.2 supportInLocal 检查

- [ ] 所有需要预览的 `@Page` 类是否都添加了 `supportInLocal = true`

#### 1.3 生命周期检查

- [ ] `created()` / `willInit()` / `didInit()` 中是否有阻塞操作
- [ ] 是否所有的事件监听都在 `pageWillDestroy()` / `viewDestroyed()` 中清理

#### 1.4 inspectionMode 检查

- [ ] 传统 DSL 是否在需要时使用了 `pageData.inspectionMode`
- [ ] Compose DSL 是否在需要时使用了 `LocalInspectionMode.current`
- [ ] 预览分支是否提供了合理的 Mock 数据

#### 1.5 Module 注册检查

- [ ] `createExternalModules()` 是否注册了所有需要的 Module
- [ ] 是否继承了 `BasePager`（如是，Module 已在基类中统一注册）
- [ ] Module 调用处是否有空安全保护（`?.` 操作符）

#### 1.6 BasePager 基类安全检查

- [ ] `isNightMode()` 是否有安全的默认值（`?: false` 而非 `!!`）

### 二、组件级检查（ComposeView）

- [ ] `ComposeAttr` 是否定义了完整的输入属性
- [ ] `lateinit var` 是否可改为有默认值的 `var`
- [ ] 组件是否通过 attr 接收数据而非直接依赖全局状态
- [ ] 主题/语言变更监听是否正确注册和销毁（是否可抽取到基类）

### 三、数据层检查（核心：从源头 Mock）

> **核心原则**：Mock 位置越接近数据源头，改动范围越小，预览效果越真实。

#### 3.0 源头 Mock 策略检查

- [ ] 是否识别出了数据流向（UI → Manager → Module → 原生）
- [ ] 是否优先在 **Manager 层**做源头 Mock
- [ ] 如果没有统一 Manager，是否在 **Module 层**做源头 Mock
- [ ] 是否避免了「表面 Mock」（每个 UI 组件都写 `if (inspectionMode)` 分支）
- [ ] Mock 数据是否以与真实数据相同的格式提供

#### 3.1 资源和数据检查

- [ ] 网络图片 URL 是否为可公开访问的链接
- [ ] 本地 assets 路径是否正确
- [ ] 预览环境下列表数据是否为空（会导致空白）
- [ ] 用户信息是否依赖登录态

### 四、布局兼容性检查

- [ ] 是否使用了 `pagerData.pageViewWidth` 进行动态计算
- [ ] 是否有硬编码像素值
- [ ] `PlatformUtils.isIOS()` 分支是否有完整的 else 处理
- [ ] iOS 特有组件（SegmentedControlIOS、TabbarIOS）的 else 分支是否完善

## 典型重构案例

### 案例 1：将 Manager 耦合改为 attr 传入

```kotlin
// ❌ 重构前 - 直接依赖全局单例
val userInfo = UserManager.currentUser

// ✅ 重构后 - 通过 attr 传入
internal class UserProfileAttr : ComposeAttr() {
    var userInfo: AppUserInfo = MockUsers.USER_NORMAL  // 有默认值，预览安全
}
// 在 body() 中使用 ctx.attr.userInfo
```

### 案例 2：数据加载添加 Mock fallback

```kotlin
// ✅ 在 loadFeeds 中处理 Module 为空的情况
val bridgeModule = acquireModule<BridgeModule>(BridgeModule.MODULE_NAME)
bridgeModule?.readAssetFile("data.json") { json ->
    if (json != null) { /* 解析真实数据 */ }
} ?: run {
    feeds.addAll(MockFeeds.feedList(10))  // 预览 fallback
}
```

### 案例 3：ComposeView 基类抽取

> 参见 [global-dependency-injection.md](global-dependency-injection.md) 模式 2（ThemedComposeView）

### 案例 4：平台分支完整性

```kotlin
// ❌ 缺少 else 分支
if (PlatformUtils.isIOS()) { SegmentedControlIOS { } }

// ✅ 完整分支
if (PlatformUtils.isIOS() && PlatformUtils.isLiquidGlassSupported()) {
    SegmentedControlIOS { }
} else {
    Tabs { }  // 预览时可见
}
```

### 案例 5：从表面 Mock 重构为源头 Mock

> 详细代码示例参见 [global-dependency-injection.md](global-dependency-injection.md) 模式 3（Manager 层）和模式 4（Module 层）

**重构收益**：

| 指标 | 表面 Mock | 源头 Mock |
|------|----------|----------|
| 修改文件数 | N 个页面都要改 | 只改 1 个 Manager/Module |
| UI 层侵入 | 每页都有 if/else | UI 零侵入 |
| 新页面支持 | 需手动加 Mock | 自动获得 |

## 输出格式模板

在执行 Skill 后，应以以下格式输出重构建议：

```
## 📋 预览重构分析报告

### 文件：{文件路径}

#### ✅ 已完成
- [x] 已添加 @KPreview 注解（12 种设备配置）
- [x] 已添加 KPreview import 语句

#### ⚠️ 重构建议

| 优先级 | 类别 | 位置 | 问题 | 建议 |
|--------|------|------|------|------|
| P0 | 数据源头 | L42 | Manager 层有统一数据出口但未做源头 Mock | 在 Manager 内部检测 inspectionMode，直接返回 Mock 数据 |
| P0 | 可预览性 | L42 | BridgeModule 调用无空安全 | 添加 `?.` 安全调用 |
| P1 | 架构 | L30-55 | 主题监听重复代码 | 抽取到 ThemedComposeView 基类 |

#### 💡 全局依赖分析

| 依赖 | 类型 | 预览兼容性 | 处理方式 |
|------|------|-----------|---------|
| ThemeManager | 单例 | ✅ 安全 | 使用默认 light 主题 |
| BridgeModule | Module | ⚠️ 需保护 | 添加空安全调用或源头 Mock |
| pagerData | 框架注入 | ✅ 安全 | @KPreview 自动注入 |