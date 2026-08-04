# Tab 页面模板

## 完整的 TabPage 模板

以下是一个完整的底部 Tab 导航页面模板：

```kotlin
package com.example.app.pages.app

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.module.CallbackRef
import com.tencent.kuikly.core.module.NotifyModule
import com.tencent.kuikly.core.module.SharedPreferencesModule
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.utils.PlatformUtils
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.PageList
import com.tencent.kuikly.core.views.PageListView
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
// 导入自定义组件和主题
import com.example.app.pages.app.lang.MultiLingualPager
import com.example.app.pages.app.theme.ThemeManager

@Page("AppTabPage")
internal class AppTabPage : MultiLingualPager() {

    // 当前选中的 Tab 索引
    private var selectedTabIndex: Int by observable(0)
    
    // Tab 标题列表（响应式，支持多语言切换）
    private var pageTitles by observableList<String>()
    
    // Tab 图标（未选中状态）
    private val pageIcons = listOf<String>(
        "tabbar_home.png",
        "tabbar_video.png",
        "tabbar_discover.png",
        "tabbar_message_center.png",
        "tabbar_profile.png"
    )
    
    // Tab 图标（选中状态）
    private val pageIconsHighlight = listOf<String>(
        "tabbar_home_highlighted.png",
        "tabbar_video_highlighted.png",
        "tabbar_discover_highlighted.png",
        "tabbar_message_center_highlighted.png",
        "tabbar_profile_highlighted.png"
    )
    
    // PageList 引用，用于控制页面切换
    private var pageListRef: ViewRef<PageListView<*, *>>? = null
    
    // 主题状态
    private var theme by observable(ThemeManager.getTheme())
    private lateinit var themeEventCallbackRef: CallbackRef

    // 语言切换时更新 Tab 标题
    override fun langEventCallbackFn() {
        super.langEventCallbackFn()
        pageTitles[0] = resStrings.btmBarHome
        pageTitles[1] = resStrings.btmBarVideo
        pageTitles[2] = resStrings.btmBarDiscover
        pageTitles[3] = resStrings.btmBarMessage
        pageTitles[4] = resStrings.btmBarMe
    }

    override fun created() {
        super.created()
        
        // 初始化 Tab 标题
        pageTitles.addAll(listOf(
            resStrings.btmBarHome,
            resStrings.btmBarVideo,
            resStrings.btmBarDiscover,
            resStrings.btmBarMessage,
            resStrings.btmBarMe
        ))
        
        val spModule = acquireModule<SharedPreferencesModule>(SharedPreferencesModule.MODULE_NAME)

        // 获取当前设置的主题
        val colorTheme = spModule.getString(ThemeManager.PREF_KEY_COLOR)
            .takeUnless { it.isEmpty() } ?: "light"
        val assetTheme = spModule.getString(ThemeManager.PREF_KEY_ASSET)
            .takeUnless { it.isEmpty() } ?: "default"
        val typoTheme = spModule.getString(ThemeManager.PREF_KEY_TYPO)
            .takeUnless { it.isEmpty() } ?: "default"

        // 切换主题并更新当前页面的样式
        ThemeManager.changeColorScheme(colorTheme)
        ThemeManager.changeAssetScheme(assetTheme)
        ThemeManager.changeTypoScheme(typoTheme)
        theme = ThemeManager.getTheme()

        // 注册换肤相关的事件监听
        themeEventCallbackRef = acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .addNotify(ThemeManager.SKIN_CHANGED_EVENT) { _ ->
                theme = ThemeManager.getTheme()
            }
    }

    override fun pageWillDestroy() {
        super.pageWillDestroy()
        acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .removeNotify(ThemeManager.SKIN_CHANGED_EVENT, themeEventCallbackRef)
    }

    // 底部 Tab 栏构建
    private fun tabBar(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    height(TAB_BOTTOM_HEIGHT)
                    flexDirectionRow()
                    turboDisplayAutoUpdateEnable(false)
                    backgroundColor(ctx.theme.colors.tabBarBackground)
                }
                for (i in 0 until ctx.pageTitles.size) {
                    View {
                        attr {
                            flex(1f)
                            allCenter()
                        }
                        event {
                            click {
                                ctx.selectedTabIndex = i
                                ctx.pageListRef?.view?.scrollToPageIndex(i)
                            }
                        }
                        Image {
                            attr {
                                size(30f, 30f)
                                if (i == ctx.selectedTabIndex) {
                                    src(ThemeManager.getAssetUri(ctx.theme.asset, ctx.pageIconsHighlight[i]))
                                    tintColor(ctx.theme.colors.tabBarIconFocused)
                                } else {
                                    src(ThemeManager.getAssetUri(ctx.theme.asset, ctx.pageIcons[i]))
                                    tintColor(ctx.theme.colors.tabBarIconUnfocused)
                                }
                            }
                        }
                        Text {
                            attr {
                                text(ctx.pageTitles[i])
                                color(if (i == ctx.selectedTabIndex) ctx.theme.colors.tabBarTextFocused
                                else ctx.theme.colors.tabBarTextUnfocused)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        val tabBottomHeight = TAB_BOTTOM_HEIGHT

        return {
            // 顶部状态栏占位
            View {
                attr {
                    height(pagerData.statusBarHeight)
                    backgroundColor(ctx.theme.colors.topBarBackground)
                }
            }

            // 内容区域 - PageList 实现页面切换
            PageList {
                ref {
                    ctx.pageListRef = it
                }
                attr {
                    flexDirectionRow()
                    pageItemWidth(pagerData.pageViewWidth)
                    pageItemHeight(pagerData.pageViewHeight - pagerData.statusBarHeight - tabBottomHeight)
                    defaultPageIndex(0)
                    showScrollerIndicator(false)
                    scrollEnable(false)  // 禁用滑动切换，只能点击 Tab 切换
                    keepItemAlive(true)  // 保持页面状态
                }
                
                // 首页
                AppHomePage { }
                
                // 其他 Tab 页面（使用空白占位页）
                for (i in 1 until ctx.pageTitles.size) {
                    AppEmptyPage {
                        attr {
                            title = ctx.pageTitles[i]
                        }
                    }
                }
            }
            
            // 底部 Tab 栏
            ctx.tabBar().invoke(this)
        }
    }

    companion object {
        const val TAB_BOTTOM_HEIGHT = 80f
    }
}
```

## 空白占位页面组件

```kotlin
package com.example.app.pages.app

import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.module.CallbackRef
import com.tencent.kuikly.core.module.NotifyModule
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Text
import com.example.app.pages.app.theme.ThemeManager

internal class AppEmptyPageView(): ComposeView<AppEmptyPageViewAttr, AppEmptyPageViewEvent>() {

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

    override fun createEvent(): AppEmptyPageViewEvent {
        return AppEmptyPageViewEvent()
    }

    override fun createAttr(): AppEmptyPageViewAttr {
        return AppEmptyPageViewAttr()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                allCenter()
                flex(1f)
                backgroundColor(ctx.theme.colors.background)
            }
            Text {
                attr {
                    text(ctx.attr.title)
                    color(ctx.theme.colors.backgroundElement)
                }
            }
        }
    }
}

internal class AppEmptyPageViewAttr : ComposeAttr() {
    var title by observable("")
}

internal class AppEmptyPageViewEvent : ComposeEvent()

internal fun ViewContainer<*, *>.AppEmptyPage(init: AppEmptyPageView.() -> Unit) {
    addChild(AppEmptyPageView(), init)
}
```

## 关键点说明

### 1. PageList 使用

- `pageItemWidth` 和 `pageItemHeight` 设置每个页面的尺寸
- `defaultPageIndex` 设置默认显示的页面索引
- `scrollEnable(false)` 禁用滑动切换
- `keepItemAlive(true)` 保持页面状态不被销毁
- 使用 `scrollToPageIndex()` 方法切换页面

### 2. Tab 栏实现

- 使用 `flexDirectionRow()` 水平排列
- 每个 Tab 项使用 `flex(1f)` 平均分配宽度
- 根据 `selectedTabIndex` 切换图标和文字颜色

### 3. 主题集成

- 所有颜色使用 `theme.colors.xxx` 获取
- 图标使用 `ThemeManager.getAssetUri()` 获取主题资源
- 监听 `SKIN_CHANGED_EVENT` 事件更新 UI

### 4. 多语言集成

- 继承 `MultiLingualPager` 自动获得多语言支持
- 使用 `resStrings.xxx` 获取本地化字符串
- 重写 `langEventCallbackFn()` 更新动态文案
