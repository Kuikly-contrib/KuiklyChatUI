# 多语言系统实现

## 概述

Kuikly App 的多语言系统支持：
- 多种语言切换（中文简体/繁体、英文、德文等）
- 实时切换，无需重启
- 支持从 JSON 文件加载语言资源
- 与主题系统类似的事件通知机制

## 文件结构

```
lang/
├── LangManager.kt       # 语言管理器
├── MultiLingualPager.kt # 多语言页面基类
└── ResString.kt         # 字符串资源定义
```

## LangManager.kt

```kotlin
package com.example.app.pages.app.lang

import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.manager.PagerManager
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

object LangManager {
    private const val LANG_MANAGER_TAG = "LangManager"
    
    // 持久化存储的 Key
    const val KEY_PREF_LANGUAGE = "lang"
    
    // 系统语言参数 Key
    const val KEY_PARAM_SYSTEM_LANGUAGE = "sysLang"
    
    // 语言变更事件名称
    const val LANG_CHANGED_EVENT = "langChanged"
    
    // 支持的语言列表
    val SUPPORTED_LANGUAGES = mapOf(
        "简体中文" to "zh-Hans",
        "繁體中文" to "zh-Hant",
        "English" to "en",
        "Deutsch" to "de"
    )
    
    // 设置中的提示文案
    val SETTING_HINTS = mapOf(
        "zh-Hans" to "正在设置语言...",
        "zh-Hant" to "正在設置語言...",
        "en" to "Setting...",
        "de" to "Einstellung..."
    )

    // 当前语言和资源
    private val defaultLanguage = "zh-Hans"
    private val defaultResStrings = SimplifiedChinese
    private var language: String = defaultLanguage
    private var resStrings: ResStrings = defaultResStrings

    /**
     * 获取资源字符串
     */
    private fun getResString(lang: String): ResStrings {
        return when (lang) {
            "zh-Hans" -> SimplifiedChinese
            "zh-Hant" -> TraditionalChinese
            "en" -> ResStrings.fromJson(JSONObject(enJsonString))
            "de" -> ResStrings.fromJson(JSONObject(deJsonString))
            else -> defaultResStrings
        }
    }

    /**
     * 获取当前资源字符串
     */
    fun getCurrentResStrings(): ResStrings = resStrings

    /**
     * 获取当前语言
     */
    fun getCurrentLanguage(): String = language

    /**
     * 切换语言
     */
    fun changeLanguage(lang: String) {
        if (lang !in SUPPORTED_LANGUAGES.values) {
            language = defaultLanguage
            resStrings = defaultResStrings
            KLog.e(LANG_MANAGER_TAG, "Unsupported language: $lang")
            return
        }

        language = lang
        resStrings = getResString(lang)
        KLog.d(LANG_MANAGER_TAG, "Language changed to: $lang")
    }
}
```

## MultiLingualPager.kt

```kotlin
package com.example.app.pages.app.lang

import com.tencent.kuikly.core.module.CallbackRef
import com.tencent.kuikly.core.module.NotifyModule
import com.tencent.kuikly.core.module.SharedPreferencesModule
import com.tencent.kuikly.core.reactive.handler.observable
import com.example.app.pages.base.BasePager

/**
 * 多语言页面基类
 * 继承此类的页面自动获得多语言支持
 */
internal abstract class MultiLingualPager : BasePager() {
    
    // 响应式的字符串资源
    var resStrings by observable(LangManager.getCurrentResStrings())
    
    private lateinit var langEventCallbackRef: CallbackRef

    override fun created() {
        super.created()

        // 获取当前语言（优先从持久化存储读取，其次从系统参数获取）
        val lang = acquireModule<SharedPreferencesModule>(SharedPreferencesModule.MODULE_NAME)
            .getString(LangManager.KEY_PREF_LANGUAGE)
            .takeUnless { it.isEmpty() } ?: pagerData.params.optString(LangManager.KEY_PARAM_SYSTEM_LANGUAGE)

        // 切换全局语言并更新当前页面的字符串资源
        LangManager.changeLanguage(lang)
        resStrings = LangManager.getCurrentResStrings()

        // 注册语言切换事件监听
        langEventCallbackRef = acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .addNotify(LangManager.LANG_CHANGED_EVENT) { _ ->
                langEventCallbackFn()
            }
    }

    /**
     * 语言切换回调
     * 子类可重写此方法来更新动态文案（如 observableList 中的文案）
     */
    open fun langEventCallbackFn() {
        resStrings = LangManager.getCurrentResStrings()
    }

    override fun pageWillDestroy() {
        super.pageWillDestroy()
        acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .removeNotify(LangManager.LANG_CHANGED_EVENT, langEventCallbackRef)
    }
}
```

## ResString.kt

```kotlin
package com.example.app.pages.app.lang

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * 字符串资源定义
 */
data class ResStrings (
    val language: String,

    // 顶部导航栏
    val topBarFollow: String,
    val topBarTrend: String,
    val topBarRecommend: String,
    val topBarNearby: String,
    val topBarRanking: String,
    val topBarCelebrity: String,
    val topBarEntertain: String,
    val topBarSociety: String,
    val topBarTest: String,

    // 底部导航栏
    val btmBarHome: String,
    val btmBarVideo: String,
    val btmBarDiscover: String,
    val btmBarMessage: String,
    val btmBarMe: String,

    // 加载状态
    val loading: String,
    val refreshing: String,
    val refreshDone: String,
    val pullToRefresh: String,
    val releaseToRefresh: String,
    val loadMore: String,
    val noMoreData: String,
    val tapToRetry: String,
    val loadingFailed: String,

    // 设置页面
    val setting: String,
    val chosen: String,
    val themeHint: String,
    val languageHint: String,
) {
    companion object {
        fun fromJson(json: JSONObject): ResStrings {
            return ResStrings(
                language = json.optString("language"),
                topBarFollow = json.optString("topBarFollow"),
                topBarTrend = json.optString("topBarTrend"),
                topBarRecommend = json.optString("topBarRecommend"),
                topBarNearby = json.optString("topBarNearby"),
                topBarRanking = json.optString("topBarRanking"),
                topBarCelebrity = json.optString("topBarCelebrity"),
                topBarEntertain = json.optString("topBarEntertain"),
                topBarSociety = json.optString("topBarSociety"),
                topBarTest = json.optString("topBarTest"),
                btmBarHome = json.optString("btmBarHome"),
                btmBarVideo = json.optString("btmBarVideo"),
                btmBarDiscover = json.optString("btmBarDiscover"),
                btmBarMessage = json.optString("btmBarMessage"),
                btmBarMe = json.optString("btmBarMe"),
                loading = json.optString("loading"),
                refreshing = json.optString("refreshing"),
                refreshDone = json.optString("refreshDone"),
                pullToRefresh = json.optString("pullToRefresh"),
                releaseToRefresh = json.optString("releaseToRefresh"),
                loadMore = json.optString("loadMore"),
                noMoreData = json.optString("noMoreData"),
                tapToRetry = json.optString("tapToRetry"),
                loadingFailed = json.optString("loadingFailed"),
                setting = json.optString("setting"),
                chosen = json.optString("chosen"),
                themeHint = json.optString("themeHint"),
                languageHint = json.optString("languageHint")
            )
        }
    }
}

// 简体中文
val SimplifiedChinese = ResStrings(
    language = "zh-Hans",
    topBarFollow = "关注",
    topBarTrend = "热门",
    topBarRecommend = "推荐",
    topBarNearby = "附近",
    topBarRanking = "榜单",
    topBarCelebrity = "明星",
    topBarEntertain = "搞笑",
    topBarSociety = "社会",
    topBarTest = "测试",
    btmBarHome = "首页",
    btmBarVideo = "视频",
    btmBarDiscover = "发现",
    btmBarMessage = "消息",
    btmBarMe = "我",
    loading = "加载中...",
    refreshing = "正在刷新",
    refreshDone = "刷新成功",
    pullToRefresh = "下拉刷新",
    releaseToRefresh = "松手即可刷新",
    loadMore = "加载更多",
    noMoreData = "无更多数据",
    tapToRetry = "点击重试",
    loadingFailed = "加载失败",
    setting = "设置",
    chosen = "已选中",
    themeHint = "选择资源主题",
    languageHint = "请选择语言"
)

// 繁体中文
val TraditionalChinese = ResStrings(
    language = "zh-Hant",
    topBarFollow = "關注",
    topBarTrend = "熱門",
    topBarRecommend = "推薦",
    topBarNearby = "附近",
    topBarRanking = "榜單",
    topBarCelebrity = "明星",
    topBarEntertain = "搞笑",
    topBarSociety = "社會",
    topBarTest = "測試",
    btmBarHome = "首頁",
    btmBarVideo = "視頻",
    btmBarDiscover = "發現",
    btmBarMessage = "消息",
    btmBarMe = "我",
    loading = "加載中...",
    refreshing = "正在刷新",
    refreshDone = "刷新成功",
    pullToRefresh = "下拉刷新",
    releaseToRefresh = "松手即可刷新",
    loadMore = "加載更多",
    noMoreData = "無更多數據",
    tapToRetry = "點擊重試",
    loadingFailed = "加載失敗",
    setting = "設置",
    chosen = "已選中",
    themeHint = "選擇資源主題",
    languageHint = "請選擇語言"
)

// 英文 JSON 字符串
val enJsonString = """
    {
      "language": "en",
      "topBarFollow": "Following",
      "topBarTrend": "Trending",
      "topBarRecommend": "Recommend",
      "topBarNearby": "Nearby",
      "topBarRanking": "Ranking",
      "topBarCelebrity": "Celebrity",
      "topBarEntertain": "Entertainment",
      "topBarSociety": "Society",
      "topBarTest": "Test",
      "btmBarHome": "Home",
      "btmBarVideo": "Video",
      "btmBarDiscover": "Discover",
      "btmBarMessage": "Messages",
      "btmBarMe": "Me",
      "loading": "Loading...",
      "refreshing": "Refreshing",
      "refreshDone": "Refresh successful",
      "pullToRefresh": "Pull to refresh",
      "releaseToRefresh": "Release to refresh",
      "loadMore": "Load more",
      "noMoreData": "No more data",
      "tapToRetry": "Tap to retry",
      "loadingFailed": "Loading failed",
      "setting": "Settings",
      "chosen": "Selected",
      "themeHint": "Select theme",
      "languageHint": "Please select language"
    }
""".trimIndent()

// 德文 JSON 字符串
val deJsonString = """
    {
      "language": "de",
      "topBarFollow": "Folgen",
      "topBarTrend": "Trends",
      "topBarRecommend": "Empfehlungen",
      "topBarNearby": "In der Nähe",
      "topBarRanking": "Rangliste",
      "topBarCelebrity": "Prominente",
      "topBarEntertain": "Unterhaltung",
      "topBarSociety": "Gesellschaft",
      "topBarTest": "Test",
      "btmBarHome": "Startseite",
      "btmBarVideo": "Videos",
      "btmBarDiscover": "Entdecken",
      "btmBarMessage": "Nachrichten",
      "btmBarMe": "Ich",
      "loading": "Wird geladen...",
      "refreshing": "Aktualisierung",
      "refreshDone": "Aktualisierung erfolgreich",
      "pullToRefresh": "Zum Aktualisieren ziehen",
      "releaseToRefresh": "Zum Aktualisieren loslassen",
      "loadMore": "Mehr laden",
      "noMoreData": "Keine weiteren Daten",
      "tapToRetry": "Zum Wiederholen tippen",
      "loadingFailed": "Laden fehlgeschlagen",
      "setting": "Einstellungen",
      "chosen": "Ausgewählt",
      "themeHint": "Thema auswählen",
      "languageHint": "Sprache auswählen"
    }
""".trimIndent()
```

## 使用方式

### 1. 页面继承 MultiLingualPager

```kotlin
@Page("MyPage")
internal class MyPage : MultiLingualPager() {
    
    // 如果有动态文案（如 observableList），重写此方法
    override fun langEventCallbackFn() {
        super.langEventCallbackFn()
        // 更新动态文案
        titles[0] = resStrings.btmBarHome
        titles[1] = resStrings.btmBarVideo
    }
    
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Text {
                attr {
                    // 使用 resStrings 获取本地化字符串
                    text(ctx.resStrings.btmBarHome)
                }
            }
        }
    }
}
```

### 2. 在组件中使用多语言

```kotlin
internal class MyComponent : ComposeView<MyComponentAttr, MyComponentEvent>() {
    private var resStrings by observable(LangManager.getCurrentResStrings())
    private lateinit var langEventCallbackRef: CallbackRef

    override fun created() {
        super.created()
        langEventCallbackRef = acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .addNotify(LangManager.LANG_CHANGED_EVENT) { _ ->
                resStrings = LangManager.getCurrentResStrings()
            }
    }

    override fun viewDestroyed() {
        super.viewDestroyed()
        acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .removeNotify(LangManager.LANG_CHANGED_EVENT, langEventCallbackRef)
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Text {
                attr {
                    text(ctx.resStrings.loading)
                }
            }
        }
    }
}
```

### 3. 切换语言

```kotlin
// 切换语言
LangManager.changeLanguage("en")

// 持久化保存
spModule.setString(LangManager.KEY_PREF_LANGUAGE, "en")

// 通知所有页面更新
notifyModule.postNotify(LangManager.LANG_CHANGED_EVENT, JSONObject())
```

## 添加新语言

1. 在 `SUPPORTED_LANGUAGES` 中添加语言映射
2. 在 `SETTING_HINTS` 中添加设置提示
3. 创建对应的 `ResStrings` 实例或 JSON 字符串
4. 在 `getResString()` 中添加对应的 case
