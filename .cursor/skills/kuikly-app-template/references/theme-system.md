# 主题系统实现

## 概述

Kuikly App 的主题系统支持：
- 颜色主题切换（亮色/暗色/自定义）
- 资源主题切换（不同风格的图标）
- 字体主题切换
- 实时切换，无需重启

## 文件结构

```
theme/
├── ThemeManager.kt      # 主题管理器
├── ThemeColors.kt       # 颜色定义
└── ThemeTypography.kt   # 字体定义
```

## ThemeManager.kt

```kotlin
package com.example.app.pages.app.theme

import com.tencent.kuikly.core.base.attr.ImageUri
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

data class Theme(
    var colors: ThemeColors,
    var asset: String,
    var typo: ThemeTypography
)

object ThemeManager {

    // 主题配置
    private val theme: Theme = Theme(
        colors = lightColorScheme,
        asset = "default",
        typo = defaultTypography
    )
    private var assetJSON = JSONObject()

    // 主题常量
    private const val ASSET_PATH_PREFIX = "themes/"
    
    // 预定义的颜色主题
    val COLOR_SCHEME_MAP = mapOf(
        "light" to lightColorScheme,
        "dark" to darkColorScheme,
        "blue" to blueColorScheme,
    )
    
    // 预定义的字体主题
    val TYPO_SCHEME_MAP = mapOf("default" to defaultTypography)
    
    // 预定义的资源主题
    val ASSET_SCHEME_LIST = listOf("default", "game")
    
    // 主题变更事件名称
    const val SKIN_CHANGED_EVENT = "skinChanged"

    // 持久化存储的 Key
    const val PREF_KEY_COLOR = "colorTheme"
    const val PREF_KEY_ASSET = "assetTheme"
    const val PREF_KEY_TYPO = "typoTheme"

    enum class ThemeType { COLOR, ASSET, TYPOGRAPHY }

    // 公共API：获取当前主题
    fun getTheme(): Theme {
        return theme.copy()
    }

    // 切换颜色主题
    fun changeColorScheme(theme: String) {
        changeTheme(ThemeType.COLOR, theme)
    }

    // 切换资源主题
    fun changeAssetScheme(theme: String) {
        changeTheme(ThemeType.ASSET, theme)
    }

    // 切换字体主题
    fun changeTypoScheme(theme: String) {
        changeTheme(ThemeType.TYPOGRAPHY, theme)
    }

    // 从 JSON 加载颜色主题（支持远程下发）
    fun loadColorFromJson(json: JSONObject) {
        theme.colors = ThemeColors.fromJson(json)
    }

    // 获取主题资源 URI
    fun getAssetUri(theme: String, asset: String): ImageUri {
        return ImageUri.commonAssets("$ASSET_PATH_PREFIX$theme/$asset")
    }

    private fun changeTheme(type: ThemeType, theme: String) {
        when (type) {
            ThemeType.COLOR -> {
                this.theme.colors = COLOR_SCHEME_MAP[theme] ?: run {
                    // 自定义主题加载（如远程下载）
                    ThemeColors.fromJson(JSONObject())
                }
            }
            ThemeType.ASSET -> {
                this.theme.asset = theme
                if (theme !in ASSET_SCHEME_LIST) {
                    // 自定义资源加载（如远程下载）
                    loadAssetFromJson(JSONObject())
                }
            }
            ThemeType.TYPOGRAPHY -> {
                this.theme.typo = TYPO_SCHEME_MAP[theme] ?: run {
                    ThemeTypography.fromJson(JSONObject())
                }
            }
        }
    }

    private fun loadAssetFromJson(json: JSONObject) {
        assetJSON = json
    }
}
```

## ThemeColors.kt

```kotlin
package com.example.app.pages.app.theme

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

data class ThemeColors (
    // 主题色
    val primary: Color,
    val background: Color,
    val backgroundElement: Color,

    // 顶部栏
    val topBarBackground: Color,
    val topBarTextUnfocused: Color,
    val topBarTextFocused: Color,
    val topBarIndicator: Color,
    val topBarNestedBackground: Color,
    val topBarNestedTextUnfocused: Color,
    val topBarNestedTextFocused: Color,

    // 底部栏
    val tabBarBackground: Color,
    val tabBarTextUnfocused: Color,
    val tabBarTextFocused: Color,
    val tabBarIconUnfocused: Color,
    val tabBarIconFocused: Color,

    // Feed 相关
    val feedBackground: Color,
    val feedUserNameNormal: Color,
    val feedUserNameMember: Color,
    val feedUserSignature: Color,
    val feedUserDevice: Color,
    val feedUserFollowButton: Color,
    val feedContentText: Color,
    val feedContentQuoteText: Color,
    val feedContentQuoteBackground: Color,
    val feedContentDivider: Color,
    val feedBottomText: Color,
    val feedBottomIcon: Color
) {
    companion object {
        // 从 JSON 解析颜色主题
        fun fromJson(json: JSONObject): ThemeColors {
            return ThemeColors(
                primary = Color(json.optString("primary")),
                background = Color(json.optString("background")),
                backgroundElement = Color(json.optString("backgroundElement")),
                topBarBackground = Color(json.optString("topBarBackground")),
                topBarTextUnfocused = Color(json.optString("topBarTextUnfocused")),
                topBarTextFocused = Color(json.optString("topBarTextFocused")),
                topBarIndicator = Color(json.optString("topBarIndicator")),
                topBarNestedBackground = Color(json.optString("topBarNestedBackground")),
                topBarNestedTextUnfocused = Color(json.optString("topBarNestedTextUnfocused")),
                topBarNestedTextFocused = Color(json.optString("topBarNestedTextFocused")),
                tabBarBackground = Color(json.optString("tabBarBackground")),
                tabBarTextUnfocused = Color(json.optString("tabBarTextUnfocused")),
                tabBarTextFocused = Color(json.optString("tabBarTextFocused")),
                tabBarIconUnfocused = Color(json.optString("tabBarIconUnfocused")),
                tabBarIconFocused = Color(json.optString("tabBarIconFocused")),
                feedBackground = Color(json.optString("feedBackground")),
                feedUserNameNormal = Color(json.optString("feedUserName")),
                feedUserNameMember = Color(json.optString("feedUserNameMembership")),
                feedUserSignature = Color(json.optString("feedUserPostTime")),
                feedUserDevice = Color(json.optString("feedUserDevice")),
                feedUserFollowButton = Color(json.optString("feedUserFollowButton")),
                feedContentText = Color(json.optString("feedContentText")),
                feedContentQuoteText = Color(json.optString("feedContentQuoteText")),
                feedContentQuoteBackground = Color(json.optString("feedContentQuoteBackground")),
                feedContentDivider = Color(json.optString("feedContentDivider")),
                feedBottomText = Color(json.optString("feedBottomText")),
                feedBottomIcon = Color(json.optString("feedBottomIcon"))
            )
        }
    }
}

// 亮色主题
val lightColorScheme = ThemeColors(
    primary = Color.WHITE,
    background = Color(0xFFEFEFEF),
    backgroundElement = Color.BLACK,
    topBarBackground = Color(0xFFF9F9F9),
    topBarTextUnfocused = Color.GRAY,
    topBarTextFocused = Color.BLACK,
    topBarIndicator = Color.RED,
    topBarNestedBackground = Color.WHITE,
    topBarNestedTextUnfocused = Color.GRAY,
    topBarNestedTextFocused = Color.RED,
    tabBarBackground = Color(0xFFFAFAFA),
    tabBarTextUnfocused = Color.BLACK,
    tabBarTextFocused = Color.RED,
    tabBarIconUnfocused = Color.GRAY,
    tabBarIconFocused = Color.RED,
    feedBackground = Color.WHITE,
    feedUserNameNormal = Color.BLACK,
    feedUserNameMember = Color(0xFFF86119),
    feedUserSignature = Color(0xFF808080),
    feedUserDevice = Color(0xFF5B778D),
    feedUserFollowButton = Color(0xFFFB8C00),
    feedContentText = Color.BLACK,
    feedContentQuoteText = Color(0xFF5B778D),
    feedContentQuoteBackground = Color(0xFFF7F7F7),
    feedContentDivider = Color(0xFFDBDBDB),
    feedBottomText = Color.BLACK,
    feedBottomIcon = Color.GRAY
)

// 暗色主题
val darkColorScheme = ThemeColors(
    primary = Color(0xFF111318),
    background = Color(0xFF191C20),
    backgroundElement = Color(0xFFE2E2E9),
    topBarBackground = Color(0xFF111318),
    topBarTextUnfocused = Color(0xFFC4C6D0),
    topBarTextFocused = Color(0xFFAAC7FF),
    topBarIndicator = Color(0xFFAAC7FF),
    topBarNestedBackground = Color(0xFF111318),
    topBarNestedTextUnfocused = Color(0xFFC4C6D0),
    topBarNestedTextFocused = Color(0xFFAAC7FF),
    tabBarBackground = Color(0xFF111318),
    tabBarTextUnfocused = Color(0xFFC4C6D0),
    tabBarTextFocused = Color(0xFFAAC7FF),
    tabBarIconUnfocused = Color(0xFFC4C6D0),
    tabBarIconFocused = Color(0xFFAAC7FF),
    feedBackground = Color(0xFF111318),
    feedUserNameNormal = Color(0xFFE2E2E9),
    feedUserNameMember = Color(0xFFF86119),
    feedUserSignature = Color(0xFFC4C6D0),
    feedUserDevice = Color(0xFF5B778D),
    feedUserFollowButton = Color(0xFFFB8C00),
    feedContentText = Color(0xFFE2E2E9),
    feedContentQuoteText = Color(0xFF5B778D),
    feedContentQuoteBackground = Color(0xFF191C20),
    feedContentDivider = Color(0xFFC4C6D0),
    feedBottomText = Color(0xFFC4C6D0),
    feedBottomIcon = Color(0xFFC4C6D0)
)

// 蓝色主题
val blueColorScheme = ThemeColors(
    primary = Color(0xFF415F91),
    background = Color(0xFFF3F3FA),
    backgroundElement = Color(0xFF191C20),
    topBarBackground = Color(0xFF415F91),
    topBarTextUnfocused = Color(0xFFD6E3FF),
    topBarTextFocused = Color(0xFFFFFFFF),
    topBarIndicator = Color(0xFFFFFFFF),
    topBarNestedBackground = Color(0xFF415F91),
    topBarNestedTextUnfocused = Color(0xFFD6E3FF),
    topBarNestedTextFocused = Color(0xFFFFFFFF),
    tabBarBackground = Color(0xFF415F91),
    tabBarTextUnfocused = Color(0xFFD6E3FF),
    tabBarTextFocused = Color(0xFFFFFFFF),
    tabBarIconUnfocused = Color(0xFFD6E3FF),
    tabBarIconFocused = Color(0xFFFFFFFF),
    feedBackground = Color(0xFFD6E3FF),
    feedUserNameNormal = Color(0xFF191C20),
    feedUserNameMember = Color(0xFFF86119),
    feedUserSignature = Color(0xFF44474E),
    feedUserDevice = Color(0xFF5B778D),
    feedUserFollowButton = Color(0xFFFB8C00),
    feedContentText = Color(0xFF191C20),
    feedContentQuoteText = Color(0xFF5B778D),
    feedContentQuoteBackground = Color(0xFFDAE2F9),
    feedContentDivider = Color(0xFF284777),
    feedBottomText = Color(0xFF284777),
    feedBottomIcon = Color(0xFF284777)
)
```

## ThemeTypography.kt

```kotlin
package com.example.app.pages.app.theme

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

data class ThemeTypography(
    val titleLarge: Float,
    val titleMedium: Float,
    val titleSmall: Float,
    val bodyLarge: Float,
    val bodyMedium: Float,
    val bodySmall: Float,
    val labelLarge: Float,
    val labelMedium: Float,
    val labelSmall: Float
) {
    companion object {
        fun fromJson(json: JSONObject): ThemeTypography {
            return ThemeTypography(
                titleLarge = json.optDouble("titleLarge", 22.0).toFloat(),
                titleMedium = json.optDouble("titleMedium", 18.0).toFloat(),
                titleSmall = json.optDouble("titleSmall", 16.0).toFloat(),
                bodyLarge = json.optDouble("bodyLarge", 16.0).toFloat(),
                bodyMedium = json.optDouble("bodyMedium", 14.0).toFloat(),
                bodySmall = json.optDouble("bodySmall", 12.0).toFloat(),
                labelLarge = json.optDouble("labelLarge", 14.0).toFloat(),
                labelMedium = json.optDouble("labelMedium", 12.0).toFloat(),
                labelSmall = json.optDouble("labelSmall", 10.0).toFloat()
            )
        }
    }
}

val defaultTypography = ThemeTypography(
    titleLarge = 22f,
    titleMedium = 18f,
    titleSmall = 16f,
    bodyLarge = 16f,
    bodyMedium = 14f,
    bodySmall = 12f,
    labelLarge = 14f,
    labelMedium = 12f,
    labelSmall = 10f
)
```

## 使用方式

### 1. 在页面/组件中集成主题

```kotlin
internal class MyPage : BasePager() {
    private var theme by observable(ThemeManager.getTheme())
    private lateinit var themeEventCallbackRef: CallbackRef

    override fun created() {
        super.created()
        // 注册主题变更监听
        themeEventCallbackRef = acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .addNotify(ThemeManager.SKIN_CHANGED_EVENT) { _ ->
                theme = ThemeManager.getTheme()
            }
    }

    override fun pageWillDestroy() {
        super.pageWillDestroy()
        // 移除监听
        acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .removeNotify(ThemeManager.SKIN_CHANGED_EVENT, themeEventCallbackRef)
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    backgroundColor(ctx.theme.colors.background)
                }
                Text {
                    attr {
                        color(ctx.theme.colors.feedContentText)
                        fontSize(ctx.theme.typo.bodyMedium)
                    }
                }
            }
        }
    }
}
```

### 2. 切换主题

```kotlin
// 切换颜色主题
ThemeManager.changeColorScheme("dark")

// 持久化保存
spModule.setString(ThemeManager.PREF_KEY_COLOR, "dark")

// 通知所有页面更新
notifyModule.postNotify(ThemeManager.SKIN_CHANGED_EVENT, JSONObject())
```

### 3. 使用主题资源

```kotlin
Image {
    attr {
        src(ThemeManager.getAssetUri(theme.asset, "icon.png"))
        tintColor(theme.colors.tabBarIconFocused)
    }
}
```
