# Kuikly App 架构说明

## 整体架构

一个完整的 Kuikly App 通常包含以下层次结构：

```
App
├── 入口页面 (TabPage)
│   ├── 顶部状态栏
│   ├── 内容区域 (PageList)
│   │   ├── 首页 (HomePage)
│   │   ├── 其他 Tab 页面...
│   │   └── 个人中心页面
│   └── 底部 Tab 栏
├── 主题系统 (theme/)
│   ├── ThemeManager - 主题管理器
│   ├── ThemeColors - 颜色定义
│   └── ThemeTypography - 字体定义
├── 多语言系统 (lang/)
│   ├── LangManager - 语言管理器
│   ├── ResStrings - 字符串资源
│   └── MultiLingualPager - 多语言页面基类
└── 数据层 (model/)
    ├── 数据模型
    └── 数据管理器
```

## 核心概念

### 1. 页面 (Pager)

Kuikly 中的页面继承自 `BasePager` 或 `Pager`：

```kotlin
@Page("MyPage")
internal class MyPage : BasePager() {
    override fun body(): ViewBuilder {
        return {
            // UI 构建
        }
    }
}
```

### 2. 组件 (ComposeView)

自定义可复用组件继承自 `ComposeView`：

```kotlin
internal class MyComponent : ComposeView<MyComponentAttr, MyComponentEvent>() {
    override fun createAttr(): MyComponentAttr = MyComponentAttr()
    override fun createEvent(): MyComponentEvent = MyComponentEvent()
    
    override fun body(): ViewBuilder {
        return {
            // 组件 UI
        }
    }
}

internal class MyComponentAttr : ComposeAttr() {
    var title by observable("")
}

internal class MyComponentEvent : ComposeEvent()

// 扩展函数，方便在 DSL 中使用
internal fun ViewContainer<*, *>.MyComponent(init: MyComponent.() -> Unit) {
    addChild(MyComponent(), init)
}
```

### 3. 响应式状态

使用 `observable` 和 `observableList` 管理状态：

```kotlin
// 普通变量
private var count by observable(0)
private var name by observable("")
private var isLoading by observable(false)

// List 变量（用于 vfor 循环）
private var items by observableList<Item>()
```

### 4. 生命周期

**页面生命周期：**
- `created()` - 页面创建时调用
- `viewDidLoad()` - 视图加载完成
- `pageDidAppear()` - 页面显示
- `pageDidDisappear()` - 页面隐藏
- `pageWillDestroy()` - 页面销毁前

**组件生命周期：**
- `created()` - 组件创建
- `viewDidLoad()` - 视图加载完成
- `viewWillUnload()` - 视图即将卸载
- `viewDestroyed()` - 视图销毁

### 5. 事件通知

使用 `NotifyModule` 进行跨页面/组件通信：

```kotlin
// 注册监听
val callbackRef = acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
    .addNotify("eventName") { data ->
        // 处理事件
    }

// 发送通知
acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
    .postNotify("eventName", JSONObject())

// 移除监听
acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
    .removeNotify("eventName", callbackRef)
```

### 6. 本地存储

使用 `SharedPreferencesModule` 进行本地数据持久化：

```kotlin
val spModule = acquireModule<SharedPreferencesModule>(SharedPreferencesModule.MODULE_NAME)

// 存储
spModule.setString("key", "value")

// 读取
val value = spModule.getString("key")
```

### 7. 页面导航

使用 `RouterModule` 进行页面跳转：

```kotlin
val router = acquireModule<RouterModule>(RouterModule.MODULE_NAME)

// 打开页面
router.openPage("PageName")

// 关闭当前页面
router.closePage()
```

## 目录结构规范

```
pages/app/
├── {App}TabPage.kt              # 主入口，@Page 注解
├── {App}SettingPage.kt          # 设置页，@Page 注解
├── {App}EmptyPageView.kt        # 空白占位组件
├── common/                       # 通用组件
├── feed/                         # Feed 相关组件
├── home/                         # 首页相关
├── lang/                         # 多语言
├── model/                        # 数据模型
└── theme/                        # 主题系统
```

## 最佳实践

1. **页面与组件分离**：页面负责整体布局和数据获取，组件负责 UI 展示
2. **主题响应式**：所有颜色、图标使用主题系统管理
3. **多语言支持**：所有文案使用 ResStrings 管理
4. **事件解耦**：使用 NotifyModule 进行跨组件通信
5. **内存管理**：在销毁时移除所有监听器
