---
name: kuikly-app-template
description: Kuikly App 模板生成器。生成完整的 Kuikly App 模板，包含底部 Tab 导航、主题系统（亮色/暗色/自定义换肤）、多语言国际化、Feed 列表页（下拉刷新/上拉加载）、设置页面等标准架构。适用场景：创建 App、新建 App、App 模板、App 示例、Tab 导航 App、kuikly app、生成项目模板、快速创建应用。
---

# Kuikly App 模板生成器

## 使用事件上报（必须执行）

**每次 Skill 被调用时，必须首先执行：**

```bash
bash "${SKILL_DIR}/scripts/beacon-report.sh" &
```

此 skill 用于生成完整的 Kuikly App 模板示例。当用户需要创建一个具有标准 App 架构的 Kuikly 应用时，使用此 skill 可以快速生成包含以下功能的完整代码：

- **底部 Tab 导航**：支持多个 Tab 页面切换
- **主题系统**：支持亮色/暗色/自定义主题切换
- **多语言国际化**：支持中文/英文等多语言切换
- **Feed 列表页**：支持下拉刷新、上拉加载更多
- **设置页面**：主题和语言切换设置

## 前置依赖

**⚠️ 重要：此 skill 依赖 `kuikly-ui-framework` skill**

在生成 App 模板代码时，必须先调用 `kuikly-ui-framework` skill 来获取 Kuikly UI 组件的正确用法。

## 使用流程

### Step 1: 调用 kuikly-ui-framework skill

在开始生成代码之前，先调用 `kuikly-ui-framework` skill 以确保：
- 获取最新的 Kuikly API 文档
- 确保使用正确的组件和属性名称
- 避免 API 幻觉问题

### Step 2: 了解用户需求

询问用户以下信息（可选，有默认值）：
- App 名称（默认：MyApp）
- 需要几个 Tab 页面（默认：5个）
- Tab 页面的名称（默认：首页、视频、发现、消息、我）
- 是否需要主题切换功能（默认：是）
- 是否需要多语言支持（默认：是）
- 是否需要 Feed 列表功能（默认：是）

### Step 3: 生成 App 模板代码

根据 `references/` 目录下的模板文件生成完整的 App 代码结构。

## 模板文件结构

生成的 App 模板包含以下文件结构：

```
app/
├── {AppName}TabPage.kt          # 主入口页面，包含底部 Tab 导航
├── {AppName}SettingPage.kt      # 设置页面
├── {AppName}EmptyPageView.kt    # 空白占位页面组件
├── common/                       # 通用组件
│   └── {AppName}NineGridView.kt # 九宫格图片组件
├── feed/                         # Feed 相关组件
│   ├── {AppName}FeedItemView.kt # Feed 列表项
│   ├── {AppName}FeedAuthorView.kt # 作者信息组件
│   ├── {AppName}FeedContentView.kt # 内容组件
│   └── {AppName}FeedBottomView.kt # 底部操作栏
├── home/                         # 首页相关
│   ├── {AppName}HomePageView.kt # 首页视图
│   └── {AppName}FeedListPage.kt # Feed 列表页
├── lang/                         # 多语言支持
│   ├── LangManager.kt           # 语言管理器
│   ├── MultiLingualPager.kt     # 多语言页面基类
│   └── ResString.kt             # 字符串资源
├── model/                        # 数据模型
│   ├── {AppName}FeedModel.kt    # Feed 数据模型
│   └── {AppName}FeedsManager.kt # Feed 数据管理
└── theme/                        # 主题系统
    ├── ThemeManager.kt          # 主题管理器
    ├── ThemeColors.kt           # 主题颜色定义
    └── ThemeTypography.kt       # 主题字体定义
```

## 参考文档

模板代码参考以下文档：

| 文件 | 说明 |
|------|------|
| `references/app-structure.md` | App 整体架构说明 |
| `references/tab-page-template.md` | Tab 页面模板 |
| `references/theme-system.md` | 主题系统实现 |
| `references/lang-system.md` | 多语言系统实现 |
| `references/feed-list-template.md` | Feed 列表模板 |
| `references/compose-view-template.md` | ComposeView 组件模板 |
| `references/follow_0.json` | Feed 列表示例数据 |
| `references/setting-page-template.md` | 设置页面模板 |

## 代码生成规则

### 1. 命名规范

- 类名使用 `{AppName}` 前缀，如 `MyAppTabPage`
- 包名使用小写，如 `com.example.myapp.pages.app`
- 文件名与类名一致

### 2. 必须使用的 Import 语句

```kotlin
// 页面基类
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder

// 响应式
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList

// 组件
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.PageList

// 模块
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.module.NotifyModule
import com.tencent.kuikly.core.module.SharedPreferencesModule

// 指令
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.directives.vfor
```

### 3. 响应式变量规则

- 普通变量使用 `observable`：`var count by observable(0)`
- List 变量使用 `observableList`：`var items by observableList<Item>()`
- **vfor 循环中的 List 必须使用 `observableList`**

### 4. 主题系统集成

每个页面/组件都应该：
1. 持有 `theme` 响应式变量
2. 在 `created()` 中注册主题变更监听
3. 在 `pageWillDestroy()` 或 `viewDestroyed()` 中移除监听

### 5. 多语言系统集成

继承 `MultiLingualPager` 的页面自动获得：
1. `resStrings` 响应式变量
2. 语言变更自动更新 UI

### 6. Feed 数据加载

**通过 BridgeModule 原生桥接读取本地 JSON 文件**

Feed 列表数据通过原生桥接方式从本地 assets 目录读取。数据加载流程：

1. `{AppName}FeedsManager.requestFeeds()` 调用 `BridgeModule.readAssetFile()`
2. 原生端读取 `assets/{AppName}/json/follow_0.json` 文件
3. **⚠️ 原生端需要将 JSON 数组包装成 `{"result": [...]}` 格式返回**
4. `parseJson()` 从 `json.optJSONArray("result")` 获取数据

```kotlin
// {AppName}FeedsManager.kt 示例
internal object {AppName}FeedsManager {
    internal fun requestFeeds(type: AppFeedsType, page: Int, callback: (List<AppFeedModel>, String) -> Unit) {
        val pathName = "{AppName}/json/${type.value}_$page.json"
        val bridgeModule = PagerManager.getCurrentPager().getModule<BridgeModule>(BridgeModule.MODULE_NAME)
        
        bridgeModule?.readAssetFile(pathName) { json ->
            if (json == null || json.optString("error").isNotEmpty()) {
                callback(listOf(), "error")
            } else {
                val feeds = parseJson(json)
                callback(feeds, "")
            }
        }
    }
    
    private fun parseJson(json: JSONObject): List<AppFeedModel> {
        // ⚠️ 从 "result" 字段获取数据数组
        val jsonArray = json.optJSONArray("result")
        val feeds = mutableListOf<AppFeedModel>()
        jsonArray?.let {
            for (i in 0 until it.length()) {
                it.optJSONObject(i)?.let { itemJson ->
                    AppFeedModel.fromJson(itemJson)?.let { model ->
                        feeds.add(model)
                    }
                }
            }
        }
        return feeds
    }
}
```

**原生端实现要求**：

```kotlin
// Android 原生端 readAssetFile 实现示例
fun readAssetFile(assetPath: String): JSONObject {
    val content = context.assets.open(assetPath).bufferedReader().use { it.readText() }
    // ⚠️ 关键：将数组包装成对象格式
    val result = JSONObject()
    result.put("result", JSONArray(content))
    return result
}
```

**JSON 文件位置**：
- Android: `app/src/main/assets/{AppName}/json/follow_0.json`
- iOS: Bundle Resources `{AppName}/json/follow_0.json`
- 鸿蒙: `entry/src/main/resources/rawfile/{AppName}/json/follow_0.json`

**数据格式**：参考 `references/follow_0.json`，每条 Feed 包含 `feed` 和 `user` 两个对象。

详细实现请参考 `references/feed-list-template.md`。

## 示例：生成一个简单的 App

用户请求："帮我创建一个 Kuikly App，有3个Tab：首页、发现、我"

生成步骤：
1. 调用 `kuikly-ui-framework` skill
2. 读取 `references/tab-page-template.md` 获取 Tab 页面模板
3. 读取 `references/theme-system.md` 获取主题系统代码
4. 读取 `references/lang-system.md` 获取多语言系统代码
5. 根据用户需求修改模板，生成完整代码

## 注意事项

1. **必须调用 kuikly-ui-framework skill**：确保 API 使用正确
2. **使用 references 目录下的模板**：不要凭记忆编写代码
3. **遵循 Kuikly 编码规范**：使用 FlexBox 布局，正确使用响应式变量
4. **保持代码完整性**：生成的代码应该可以直接运行
