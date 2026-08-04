# Kuikly 预览全局依赖注入指南

本文档详细说明在 Kuikly 预览环境中如何处理各类全局依赖的注入问题。

## 依赖分类

### 类别 A：自动可用的依赖（无需特殊处理）

| 依赖 | 原因 | 预览行为 |
|------|------|----------|
| `pagerData.pageViewWidth/Height` | 由 @KPreview 的 `widthDp`/`heightDp` 提供 | 自动注入 |
| `pagerData.statusBarHeight` | 预览环境提供默认值 | 通常为 0 或模拟值 |
| `pageData.inspectionMode` | 预览时自动为 `true` | 自动注入 |
| `LocalInspectionMode.current` | Compose DSL 预览标志 | 预览时为 `true` |
| `ThemeManager.getTheme()` | 使用默认 `lightColorScheme` | 返回 light 主题 |
| `LangManager.getCurrentResStrings()` | 使用默认语言初始化 | 返回默认语言字符串 |

### 类别 B：需要注册的依赖（Module 系统）

**推荐方式**：在 `BasePager` 基类中统一注册：

```kotlin
internal abstract class BasePager : Pager() {
    override fun createExternalModules(): Map<String, Module>? {
        val externalModules = hashMapOf<String, Module>()
        externalModules[BridgeModule.MODULE_NAME] = BridgeModule()
        externalModules[TDFTestModule.MODULE_NAME] = TDFTestModule()
        return externalModules
    }
}
```

**注意**：预览环境中 Module 的原生桥接方法不会有实际的原生端响应（同步调用可能返回空值，异步回调可能不触发），需在调用处做空安全保护。

### 类别 C：需要空安全保护的调用

> 如果已在 Manager/Module 层做了源头 Mock，以下保护可能不再必要，但作为防御性编程建议保留。

```kotlin
// C.1 BridgeModule - 带空保护和 fallback
val bridgeModule = PagerManager.getCurrentPager()
    .getModule<BridgeModule>(BridgeModule.MODULE_NAME)
if (bridgeModule != null) {
    bridgeModule.readAssetFile(path) { json ->
        if (json != null && json.optString("error").isEmpty()) {
            val feeds = parseJson(json)
        }
    }
}

// C.2 SharedPreferencesModule - 提供默认值
val theme = spModule.getString("colorTheme")
    .takeUnless { it.isEmpty() } ?: "light"

// C.3 RouterModule - 预览中路由跳转不会真正打开新页面，但不会崩溃
acquireModule<RouterModule>(RouterModule.MODULE_NAME).openPage(pageName, pageData)

// C.4 NotifyModule - 预览中事件注册不会崩溃，但事件不会被触发
themeEventCallbackRef = acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
    .addNotify(ThemeManager.SKIN_CHANGED_EVENT) { _ -> theme = ThemeManager.getTheme() }
```

### 类别 D：平台相关依赖

```kotlin
// D.1 PlatformUtils - 确保 else 分支完整
val isLiquidGlass = PlatformUtils.isIOS() && PlatformUtils.isLiquidGlassSupported()
if (isLiquidGlass) {
    tabBarIOS()  // 预览时不会走到这里
} else {
    tabBar()  // 预览时走这个分支，确保功能完整
}

// D.2 LocalActivity（Compose DSL）- 预览环境会提供 mock Activity
@Composable
fun YourScreen() {
    val activity = LocalActivity.current
    val statusBarHeight = activity.pageData.statusBarHeight
}

// D.3 BasePager 夜间模式安全
override fun isNightMode(): Boolean {
    if (nightModel == null) {
        nightModel = pageData.params.optBoolean(IS_NIGHT_MODE_KEY, false)
    }
    return nightModel ?: false  // 避免强制解包
}
```

### 类别 E：预览环境检测（inspectionMode）

```kotlin
// 传统 DSL
override fun created() {
    super.created()
    if (pageData.inspectionMode) {
        feeds.addAll(MockFeeds.feedList(5))
    } else {
        loadDataFromNetwork()
    }
}

// Compose DSL
@Composable
fun YourScreen() {
    val isPreview = LocalInspectionMode.current
    val feeds = if (isPreview) MockFeeds.feedList(5) else rememberLoadedData()
    LazyColumn { items(feeds) { feed -> FeedItem(feed) } }
}
```

适用场景：跳过网络请求、跳过原生 Module 调用、跳过埋点上报/性能监控、显示预览调试信息。

### 类别 F：数据源头 Mock（核心策略）

> **核心原则**：Mock 位置越接近数据源头，改动范围越小，预览效果越真实。

| 优先级 | Mock 层次 | 改动范围 | 说明 |
|--------|----------|---------|------|
| ⭐ P0 最优 | **Manager 层源头 Mock** | 改 1 处 | 所有 UI 自动生效 |
| ⭐ P0 次优 | **Module 层源头 Mock** | 改 1 处 | 所有 Manager 自动生效 |
| P1 中等 | ViewModel Mock | 每个 ViewModel 改 1 处 | - |
| P2 最差 | UI 层表面 Mock | 每个页面都要改 | 侵入性最强 |

### 补充：ComposeContainer 类型的预览处理

`ComposeContainer` 本质上是 Pager，内部通过 `setContent {}` 承载 Compose DSL 内容，预览方式与传统 Pager 一致：

```kotlin
@KPreview(widthDp = 360, heightDp = 640, name = "标准手机", density = 2.0f)
@Page("ComposeRoute", supportInLocal = true)
internal class ComposeRoutePager : ComposeContainer() {
    override fun setContent() {
        if (pageData.inspectionMode) { /* Mock data */ }
    }
}
```

- `createExternalModules()` 等机制同样适用
- `setContent {}` 内部可使用 `LocalInspectionMode.current`
- 如继承了自定义 `BasePager`，Module 注册在基类中统一处理

---

## 常见问题排查

| 问题 | 可能原因 | 解决方案 |
|------|---------|---------|
| 预览空白 | `created()` 数据加载依赖 Module 未返回 | 添加 Mock fallback |
| 预览崩溃 (NPE) | `lateinit var` 未初始化 / Module 返回 null | 用 `by observable(默认值)` / null check |
| 图片不显示 | 本地 assets 或需鉴权 URL | 使用 Mock 图片 URL |
| 主题不正确 | SP 读取配置 | ThemeManager 有默认 light 主题 |

## 重构模式示例

### 模式 1：将硬依赖改为 attr 属性传入

```kotlin
// ❌ 重构前
internal class FeedItemView : ComposeView<...>() {
    override fun body(): ViewBuilder {
        val feeds = AppFeedsManager.cachedFeeds  // 直接耦合全局管理器
    }
}

// ✅ 重构后 - 通过 attr 传入数据
internal class FeedItemView : ComposeView<FeedItemViewAttr, FeedItemViewEvent>() {
    override fun body(): ViewBuilder {
        val item = attr.item
    }
}
internal class FeedItemViewAttr : ComposeAttr() {
    lateinit var item: AppFeedModel
}
```

### 模式 2：将基类中的通用注册逻辑统一

```kotlin
// ✅ 在基类中统一处理主题监听，业务组件直接继承
internal abstract class ThemedComposeView<A : ComposeAttr, E : ComposeEvent> : ComposeView<A, E>() {
    protected var theme by observable(ThemeManager.getTheme())
    private lateinit var themeCallbackRef: CallbackRef

    override fun created() {
        super.created()
        themeCallbackRef = acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .addNotify(ThemeManager.SKIN_CHANGED_EVENT) { _ -> theme = ThemeManager.getTheme() }
    }

    override fun viewDestroyed() {
        super.viewDestroyed()
        acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .removeNotify(ThemeManager.SKIN_CHANGED_EVENT, themeCallbackRef)
    }
}
```

### 模式 3：源头 Mock — Manager 层拦截（最佳实践）

```kotlin
// ✅ 改 1 处 Manager，所有使用该 Manager 的页面自动获得预览数据，UI 层零修改
internal object AppFeedsManager {
    internal fun requestFeeds(type: AppFeedsType, page: Int, callback: (List<AppFeedModel>, String) -> Unit) {
        val pager = PagerManager.getCurrentPager()
        if (pager.pageData.inspectionMode) {
            callback(MockFeeds.feedList(10), "")
            return
        }
        // 原有逻辑不变
        val bridgeModule = pager.getModule<BridgeModule>(BridgeModule.MODULE_NAME)
        bridgeModule?.readAssetFile(getFileName(type, page)) { json ->
            if (json == null || json.optString("error").isNotEmpty()) {
                callback(listOf(), "error")
            } else {
                callback(parseJson(json), "")
            }
        }
    }
}
```

### 模式 4：源头 Mock — Module 层拦截

```kotlin
// ✅ 在 BridgeModule 层拦截，返回 Mock JSON，下游 Manager 的 parseJson() 也能被验证
internal class BridgeModule : Module() {
    fun readAssetFile(assetPath: String, callback: CallbackFn?) {
        if (getPager()?.pageData?.inspectionMode == true) {
            callback?.invoke(MockJsonProvider.getJsonForPath(assetPath))
            return
        }
        // 原有逻辑
        val params = JSONObject()
        params.put("assetPath", assetPath)
        syncCallNativeMethod(READ_ASSET_FILE, params, callback)
    }
}
```