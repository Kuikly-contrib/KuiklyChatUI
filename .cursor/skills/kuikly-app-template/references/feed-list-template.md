# Feed 列表模板

## 概述

Feed 列表是 App 中常见的内容展示形式，包含：
- 下拉刷新
- 上拉加载更多
- 列表项组件
- 数据管理

## 文件结构

```
feed/
├── AppFeedItemView.kt      # Feed 列表项
├── AppFeedAuthorView.kt    # 作者信息组件
├── AppFeedContentView.kt   # 内容组件
├── AppFeedBottomView.kt    # 底部操作栏
└── AppFeedVideoView.kt     # 视频组件

home/
├── AppFeedListPage.kt      # Feed 列表页

model/
├── AppFeedModel.kt         # Feed 数据模型
└── AppFeedsManager.kt      # Feed 数据管理
```

## AppFeedListPage.kt - Feed 列表页

```kotlin
package com.example.app.pages.app.home

import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.directives.velse
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.module.CallbackRef
import com.tencent.kuikly.core.module.NotifyModule
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.FooterRefresh
import com.tencent.kuikly.core.views.FooterRefreshEndState
import com.tencent.kuikly.core.views.FooterRefreshState
import com.tencent.kuikly.core.views.FooterRefreshView
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Refresh
import com.tencent.kuikly.core.views.RefreshView
import com.tencent.kuikly.core.views.RefreshViewState
import com.tencent.kuikly.core.views.Text
import com.example.app.pages.app.feed.AppFeedItem
import com.example.app.pages.app.lang.LangManager
import com.example.app.pages.app.model.AppFeedModel
import com.example.app.pages.app.model.AppFeedsManager
import com.example.app.pages.app.model.AppFeedsType
import com.example.app.pages.app.theme.ThemeManager

internal class AppFeedListPageView(
    private val type: AppFeedsType
): ComposeView<AppFeedListPageViewAttr, AppFeedListPageViewEvent>() {

    // Feed 数据列表（必须使用 observableList）
    private var feeds by observableList<AppFeedModel>()
    
    // 下拉刷新组件引用
    private lateinit var refreshRef: ViewRef<RefreshView>
    
    // 当前页码
    private var curPage by observable(0)
    
    // 上拉加载组件引用
    private lateinit var footerRefreshRef: ViewRef<FooterRefreshView>
    
    // 是否已加载首屏数据
    private var didLoadFirstFeeds = false
    
    // 主题和语言
    private var theme by observable(ThemeManager.getTheme())
    private var resStrings by observable(LangManager.getCurrentResStrings())
    
    // 刷新文案
    private var refreshText by observable(LangManager.getCurrentResStrings().pullToRefresh)
    private var footerRefreshText by observable(LangManager.getCurrentResStrings().loadMore)
    
    private lateinit var themeEventCallbackRef: CallbackRef
    private lateinit var langEventCallbackRef: CallbackRef

    override fun created() {
        super.created()
        themeEventCallbackRef = acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .addNotify(ThemeManager.SKIN_CHANGED_EVENT) { _ ->
                theme = ThemeManager.getTheme()
            }
        langEventCallbackRef = acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .addNotify(LangManager.LANG_CHANGED_EVENT) { _ ->
                resStrings = LangManager.getCurrentResStrings()
            }
    }

    override fun viewWillUnload() {
        super.viewWillUnload()
        acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .removeNotify(ThemeManager.SKIN_CHANGED_EVENT, themeEventCallbackRef)
        acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .removeNotify(LangManager.LANG_CHANGED_EVENT, langEventCallbackRef)
    }

    override fun createEvent(): AppFeedListPageViewEvent {
        return AppFeedListPageViewEvent()
    }

    override fun createAttr(): AppFeedListPageViewAttr {
        return AppFeedListPageViewAttr()
    }
    
    // 加载首屏数据
    internal fun loadFirstFeeds() {
        if (didLoadFirstFeeds) {
            return
        }
        didLoadFirstFeeds = true
        requestFeeds(curPage) {}
    }
    
    // 请求数据
    private fun requestFeeds(page: Int, complete: () -> Unit) {
        if (page > 9) {
            complete()
            return
        }
        AppFeedsManager.requestFeeds(type, page) { feedList, error ->
            if (error.isEmpty()) {
                if (page == 0) {
                    feeds.clear()
                }
                feeds.addAll(feedList)
            }
            complete()
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flex(1f)
                backgroundColor(ctx.theme.colors.background)
            }
            
            // 加载中状态
            vif({ ctx.feeds.isEmpty() }) {
                Text {
                    attr {
                        text(ctx.resStrings.loading)
                        color(ctx.theme.colors.feedContentText)
                    }
                }
            }
            
            // 列表内容
            velse {
                List {
                    attr {
                        flex(1f)
                        firstContentLoadMaxIndex(4)  // 首屏最多渲染4个
                    }
                    
                    // 下拉刷新
                    Refresh {
                        ref {
                            ctx.refreshRef = it
                        }
                        attr {
                            height(50f)
                            allCenter()
                        }
                        event {
                            refreshStateDidChange {
                                when(it) {
                                    RefreshViewState.REFRESHING -> {
                                        ctx.refreshText = ctx.resStrings.refreshing
                                        ctx.requestFeeds(0) {
                                            ctx.refreshRef.view?.endRefresh()
                                            ctx.refreshText = ctx.resStrings.refreshDone
                                            ctx.footerRefreshRef.view?.resetRefreshState()
                                        }
                                    }
                                    RefreshViewState.IDLE -> ctx.refreshText = ctx.resStrings.pullToRefresh
                                    RefreshViewState.PULLING -> ctx.refreshText = ctx.resStrings.releaseToRefresh
                                }
                            }
                        }
                        Text {
                            attr {
                                color(ctx.theme.colors.feedContentText)
                                text(ctx.refreshText)
                            }
                        }
                    }
                    
                    // Feed 列表项
                    vfor({ ctx.feeds }) {
                        AppFeedItem {
                            attr {
                                item = it
                            }
                        }
                    }

                    // 上拉加载更多
                    vif({ ctx.feeds.isNotEmpty() }) {
                        FooterRefresh {
                            ref {
                                ctx.footerRefreshRef = it
                            }
                            attr {
                                preloadDistance(600f)  // 预加载距离
                                allCenter()
                                height(60f)
                            }
                            event {
                                refreshStateDidChange {
                                    when(it) {
                                        FooterRefreshState.REFRESHING -> {
                                            ctx.footerRefreshText = ctx.resStrings.loading
                                            ctx.curPage++
                                            ctx.requestFeeds(ctx.curPage) {
                                                val state = if (ctx.curPage == 9) 
                                                    FooterRefreshEndState.NONE_MORE_DATA 
                                                else 
                                                    FooterRefreshEndState.SUCCESS
                                                ctx.footerRefreshRef.view?.endRefresh(state)
                                            }
                                        }
                                        FooterRefreshState.IDLE -> ctx.footerRefreshText = ctx.resStrings.loadMore
                                        FooterRefreshState.NONE_MORE_DATA -> ctx.footerRefreshText = ctx.resStrings.noMoreData
                                        FooterRefreshState.FAILURE -> ctx.footerRefreshText = ctx.resStrings.tapToRetry
                                        else -> {}
                                    }
                                }
                                click {
                                    // 点击重试
                                    ctx.footerRefreshRef.view?.beginRefresh()
                                }
                            }
                            Text {
                                attr {
                                    color(ctx.theme.colors.feedContentText)
                                    text(ctx.footerRefreshText)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

internal class AppFeedListPageViewAttr : ComposeAttr()

internal class AppFeedListPageViewEvent : ComposeEvent()

internal fun ViewContainer<*, *>.AppFeedListPage(type: AppFeedsType, init: AppFeedListPageView.() -> Unit) {
    addChild(AppFeedListPageView(type), init)
}
```

## AppFeedItemView.kt - Feed 列表项

```kotlin
package com.example.app.pages.app.feed

import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.module.CallbackRef
import com.tencent.kuikly.core.module.NotifyModule
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.View
import com.example.app.pages.app.model.AppFeedModel
import com.example.app.pages.app.theme.ThemeManager

internal class AppFeedItemView : ComposeView<AppFeedItemViewAttr, AppFeedItemViewEvent>() {

    private var theme by observable(ThemeManager.getTheme())
    private lateinit var eventCallbackRef: CallbackRef

    override fun created() {
        super.created()
        eventCallbackRef = acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .addNotify(ThemeManager.SKIN_CHANGED_EVENT) { _ ->
                theme = ThemeManager.getTheme()
            }
    }

    override fun viewDestroyed() {
        super.viewDestroyed()
        acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .removeNotify(ThemeManager.SKIN_CHANGED_EVENT, eventCallbackRef)
    }

    override fun createAttr(): AppFeedItemViewAttr {
        return AppFeedItemViewAttr()
    }

    override fun createEvent(): AppFeedItemViewEvent {
        return AppFeedItemViewEvent()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(ctx.theme.colors.feedBackground)
            }
            
            // 作者信息
            AppFeedItemAuthor {
                attr {
                    userInfo = ctx.attr.item.userInfo
                    tail = ctx.attr.item.tail
                    createTime = ctx.attr.item.createTime
                }
            }
            
            // 文字内容
            AppFeedContent {
                attr {
                    content = ctx.attr.item.content
                }
            }
            
            // 图片九宫格
            AppNineGrid {
                attr {
                    picUrls = ctx.attr.item.picUrl
                }
            }
            
            // 分割线
            View {
                attr {
                    margin(left = 15.0f, right = 15.0f, bottom = 10.0f, top = 0.0f)
                    height(0.5f)
                    backgroundColor(ctx.theme.colors.feedContentDivider)
                }
            }
            
            // 底部操作栏（转发、评论、点赞）
            AppFeedBottom {
                attr {
                    retweetNum = ctx.attr.item.forwardNum
                    commentNum = ctx.attr.item.commentNum
                    likeNum = ctx.attr.item.likeNum
                    likeStatus = ctx.attr.item.likeStatus
                }
            }
            
            // 间隔
            View {
                attr {
                    marginTop(10.0f)
                    height(12.0f)
                    backgroundColor(ctx.theme.colors.background)
                }
            }
        }
    }
}

internal class AppFeedItemViewAttr : ComposeAttr() {
    lateinit var item: AppFeedModel
}

internal class AppFeedItemViewEvent : ComposeEvent()

internal fun ViewContainer<*, *>.AppFeedItem(init: AppFeedItemView.() -> Unit) {
    addChild(AppFeedItemView(), init)
}
```

## AppFeedModel.kt - 数据模型

```kotlin
package com.example.app.pages.app.model

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

data class AppFeedModel(
    val appId: String,
    val categoryId: String,
    val content: String,
    var userInfo: AppUserInfo,
    val picUrl: List<String>,
    val forwardContent: String?,
    val forwardNick: String?,
    val forwardUserId: String?,
    val forwardPicUrl: List<String>?,
    val forwardAppId: String?,
    val forwardVideoUrl: String?,
    val containForward: Boolean,
    val videoUrl: String,
    val tail: String,
    val createTime: String,
    val likeStatus: Int,
    val forwardNum: Int,
    val likeNum: Int,
    val commentNum: Int
) {
    companion object {
        fun fromJson(jsonObject: JSONObject): AppFeedModel? {
            val feedObj = jsonObject.optJSONObject("feed")
            val userObj = jsonObject.optJSONObject("user")
            if (feedObj == null || userObj == null) {
                return null
            }

            return AppFeedModel(
                appId = feedObj.optString("appId", ""),
                categoryId = feedObj.optString("categoryId", ""),
                content = feedObj.optString("content", ""),
                picUrl = parsePicUrls(feedObj.optJSONArray("picurl")),
                userInfo = AppUserInfo.fromJson(userObj),
                forwardContent = feedObj.optString("zfContent"),
                forwardNick = feedObj.optString("zfNick"),
                forwardUserId = feedObj.optString("zfUserId"),
                forwardAppId = feedObj.optString("zfAppId"),
                containForward = feedObj.optBoolean("containZf"),
                forwardPicUrl = parsePicUrls(feedObj.optJSONArray("zfPicurl")),
                forwardVideoUrl = "",
                videoUrl = feedObj.optString("vediourl"),
                tail = feedObj.optString("tail"),
                createTime = feedObj.optString("createtime"),
                likeStatus = 0,
                forwardNum = feedObj.optInt("zhuanfaNum"),
                likeNum = feedObj.optInt("likeNum"),
                commentNum = feedObj.optInt("commentNum")
            )
        }
        
        private fun parsePicUrls(jsonArray: JSONArray?): List<String> {
            // 解析图片 URL 列表
            return jsonArray?.let {
                (0 until it.length()).map { i -> it.optString(i) }
            } ?: emptyList()
        }
    }
}

data class AppUserInfo(
    val userId: String,
    val nick: String,
    val avatar: String,
    val signature: String,
    val isMember: Boolean
) {
    companion object {
        fun fromJson(json: JSONObject): AppUserInfo {
            return AppUserInfo(
                userId = json.optString("userId"),
                nick = json.optString("nick"),
                avatar = json.optString("avatar"),
                signature = json.optString("signature"),
                isMember = json.optBoolean("isMember")
            )
        }
    }
}
```

## AppFeedsManager.kt - 数据管理

**⚠️ 重要：通过 BridgeModule 原生桥接读取本地 JSON 文件**

数据加载流程：
1. `AppFeedsManager.requestFeeds()` 调用 `BridgeModule.readAssetFile()`
2. 原生端读取 `assets` 目录下的 JSON 文件
3. **原生端需要将 JSON 数组包装成 `{"result": [...]}` 格式返回**
4. `parseJson()` 从 `json.optJSONArray("result")` 获取数据

```kotlin
package com.example.app.pages.app.model

import com.tencent.kuikly.core.datetime.DateTime
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.manager.PagerManager
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.example.app.pages.base.BridgeModule

enum class AppFeedsType(val value: String) {
    Follow("follow"),      // 关注
    Recommend("recommend"), // 推荐
    Nearby("nearby"),      // 附近
    Top("top"),            // 榜单
    Star("star"),          // 明星
    Laugh("laugh"),        // 搞笑
    Society("society"),    // 社会
    Test("test"),          // 测试
}

internal object AppFeedsManager {
    private var callbackMap: HashMap<Pair<AppFeedsType, Int>, Any> = hashMapOf()

    private fun genKey(type: AppFeedsType, page: Int): Pair<AppFeedsType, Int> {
        return type to page
    }

    internal fun requestFeeds(type: AppFeedsType, page: Int, callback: (List<AppFeedModel>, String) -> Unit) {
        val key = genKey(type, page)
        
        // 防止重复请求
        if (callbackMap.containsKey(key)) {
            callback(listOf(), "in request")
            return
        }
        
        // 模拟分页限制（只有 page 0-2 有数据）
        if (page >= 3) {
            callback(listOf(), "")
            return
        }

        callbackMap[key] = callback
        
        // 获取文件路径
        val pathName = getFileName(type, page)
        
        // 通过 BridgeModule 原生桥接读取本地 JSON 文件
        val bridgeModule = PagerManager.getCurrentPager().getModule<BridgeModule>(BridgeModule.MODULE_NAME)
        
        if (bridgeModule != null) {
            val startMills = DateTime.currentTimestamp()
            KLog.d("AppDemo", "requestFeeds startMills $startMills")
            
            bridgeModule.readAssetFile(pathName) { json ->
                val cost = DateTime.currentTimestamp() - startMills
                KLog.d("AppDemo", "requestFeeds readAssetPath cost: $cost")
                
                if (json == null || json.optString("error").isNotEmpty()) {
                    callback(listOf(), "error")
                } else {
                    val feeds = parseJson(json)
                    callback(feeds, "")
                }
                callbackMap.remove(key)
            }
        } else {
            callbackMap.remove(key)
            callback(listOf(), "BridgeModule not found")
        }
    }
    
    /**
     * 解析 JSON 数据
     * 
     * ⚠️ 注意：原生端返回的 JSON 格式必须是 {"result": [...]}
     * 原始 follow_0.json 是数组格式 [...]，原生端需要包装成对象格式
     */
    private fun parseJson(json: JSONObject): List<AppFeedModel> {
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

    /**
     * 获取 JSON 文件路径
     * 文件位于 assets/{AppName}/json/{type}_{page}.json
     */
    private fun getFileName(type: AppFeedsType, page: Int): String {
        return "{AppName}/json/${type.value}_$page.json"
    }
}
```

## BridgeModule.kt - 原生桥接模块

```kotlin
package com.example.app.pages.base

import com.tencent.kuikly.core.module.CallbackFn
import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

internal class BridgeModule : Module() {

    override fun moduleName(): String {
        return MODULE_NAME
    }

    /**
     * 读取 assets 目录下的文件内容
     * 
     * @param assetPath 文件路径，如 "AppTabPage/json/follow_0.json"
     * @param callback 回调函数，返回 JSONObject
     * 
     * ⚠️ 原生端实现要求：
     * 1. 读取 assets 目录下的文件
     * 2. 如果文件内容是 JSON 数组 [...]，需要包装成 {"result": [...]} 格式返回
     * 3. 如果读取失败，返回 {"error": "错误信息"}
     */
    fun readAssetFile(assetPath: String, callback: CallbackFn?) {
        val params = JSONObject()
        params.put("assetPath", assetPath)
        syncCallNativeMethod(READ_ASSET_FILE, params, callback)
    }

    private fun syncCallNativeMethod(
        methodName: String,
        data: JSONObject?,
        callbackFn: CallbackFn?
    ): String {
        return toNative(
            false,
            methodName,
            data?.toString(),
            callbackFn,
            true
        ).toString()
    }

    companion object {
        const val MODULE_NAME = "HRBridgeModule"
        const val READ_ASSET_FILE = "readAssetFile"
    }
}
```

## 原生端实现说明

### Android 端实现示例

```kotlin
// Android 原生端需要实现 readAssetFile 方法
fun readAssetFile(assetPath: String): JSONObject {
    return try {
        val inputStream = context.assets.open(assetPath)
        val content = inputStream.bufferedReader().use { it.readText() }
        inputStream.close()
        
        // ⚠️ 关键：将数组包装成对象格式
        val result = JSONObject()
        result.put("result", JSONArray(content))
        result
    } catch (e: Exception) {
        val error = JSONObject()
        error.put("error", e.message ?: "Unknown error")
        error
    }
}
```

### iOS 端实现示例

```swift
// iOS 原生端需要实现 readAssetFile 方法
func readAssetFile(assetPath: String) -> [String: Any] {
    guard let path = Bundle.main.path(forResource: assetPath, ofType: nil),
          let data = FileManager.default.contents(atPath: path),
          let jsonArray = try? JSONSerialization.jsonObject(with: data) as? [[String: Any]] else {
        return ["error": "File not found or parse error"]
    }
    
    // ⚠️ 关键：将数组包装成对象格式
    return ["result": jsonArray]
}
```

## JSON 文件位置

将 `follow_0.json` 放置在以下位置：

- **Android**: `app/src/main/assets/{AppName}/json/follow_0.json`
- **iOS**: 添加到 Bundle Resources，路径为 `{AppName}/json/follow_0.json`
- **鸿蒙**: `entry/src/main/resources/rawfile/{AppName}/json/follow_0.json`

## JSON 数据格式

`follow_0.json` 是一个 JSON 数组，每个元素包含 `feed` 和 `user` 两个对象：

```json
[
  {
    "feed": {
      "appId": "4930203040506140",
      "id": "5902087663",
      "nick": "晨间漫步者",
      "content": "清晨的阳光洒在窗台上...",
      "zhuanfaNum": "22",
      "commentNum": "50",
      "likeNum": "400",
      "createtime": "2023-08-06 07:30",
      "tail": "MacBook Pro",
      "picurl": [],
      "containZf": false
    },
    "user": {
      "id": "5902087663",
      "nick": "晨间漫步者",
      "decs": "用脚步丈量城市，用心灵感受自然",
      "ismember": "0",
      "isvertify": "False",
      "headurl": "https://example.com/avatar.png"
    }
  }
]
```

**⚠️ 重要提醒：**
- 示例数据请参考 `references/follow_0.json`
- 原生端读取后必须包装成 `{"result": [...]}` 格式返回

## 关键点说明

### 1. 下拉刷新 (Refresh)

- 使用 `Refresh` 组件包裹刷新 UI
- 监听 `refreshStateDidChange` 事件处理刷新状态
- 刷新完成后调用 `endRefresh()` 结束刷新

### 2. 上拉加载 (FooterRefresh)

- 使用 `FooterRefresh` 组件
- `preloadDistance` 设置预加载触发距离
- 加载完成后调用 `endRefresh(state)` 并传入状态
- 支持 `NONE_MORE_DATA` 状态表示没有更多数据

### 3. 列表优化

- `firstContentLoadMaxIndex(4)` 限制首屏渲染数量
- 使用 `vfor` 循环渲染列表项
- 列表数据使用 `observableList` 确保响应式更新

### 4. 组件化设计

- 每个 Feed 项拆分为多个子组件
- 子组件通过 `attr` 接收数据
- 保持组件的独立性和可复用性
