# 设置页面模板

## 概述

设置页面是 App 中常见的功能页面，包含：
- 顶部导航栏（带返回按钮）
- 主题切换
- 语言切换
- 加载中弹窗

## AppSettingPage.kt

```kotlin
package com.example.app.pages.app

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.Scale
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.attr.ImageUri
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.module.NotifyModule
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.module.SharedPreferencesModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.ActivityIndicator
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Modal
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.TransitionType
import com.tencent.kuikly.core.views.TransitionView
import com.tencent.kuikly.core.views.View
import com.example.app.pages.app.lang.MultiLingualPager
import com.example.app.pages.app.lang.LangManager
import com.example.app.pages.app.theme.ThemeManager

@Page("AppSettingPage")
internal class AppSettingPage : MultiLingualPager() {

    // 主题和语言状态
    private var theme by observable(ThemeManager.getTheme())
    private var lang by observable(LangManager.getCurrentLanguage())
    private lateinit var settingLangHint: String
    
    // 弹窗显示状态
    private var showModal by observable(false)

    // 模块引用
    private lateinit var spModule: SharedPreferencesModule
    private lateinit var notifyModule: NotifyModule

    override fun created() {
        super.created()
        spModule = acquireModule(SharedPreferencesModule.MODULE_NAME)
        notifyModule = acquireModule(NotifyModule.MODULE_NAME)
    }

    // 顶部导航栏
    private fun topNavBar(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    paddingTop(ctx.pagerData.statusBarHeight)
                    backgroundColor(ctx.theme.colors.topBarBackground)
                }
                View {
                    attr {
                        height(44f)
                        allCenter()
                    }

                    // 标题
                    Text {
                        attr {
                            text(ctx.resStrings.setting)
                            color(ctx.theme.colors.topBarTextFocused)
                            fontSize(17f)
                            fontWeightSemiBold()
                        }
                    }
                }

                // 返回按钮
                Image {
                    attr {
                        absolutePosition(12f + getPager().pageData.statusBarHeight, 12f, 12f, 12f)
                        size(10f, 17f)
                        // 返回箭头图标（Base64）
                        src("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAsAAAASBAMAAAB/WzlGAAAAElBMVEUAAAAAAAAAAAAAAAAAAAAAAADgKxmiAAAABXRSTlMAIN/PELVZAGcAAAAkSURBVAjXYwABQTDJqCQAooSCHUAcVROCHBiFECTMhVoEtRYA6UMHzQlOjQIAAAAASUVORK5CYII=")
                        tintColor(ctx.theme.colors.topBarTextFocused)
                    }
                    event {
                        click {
                            // 关闭当前页面
                            getPager().acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
                        }
                    }
                }
            }
        }
    }

    // 主题颜色选择
    private fun skinChooseView(): ViewBuilder {
        val ctx = this
        return {
            Text {
                attr {
                    margin(16f)
                    fontSize(20f)
                    text(ctx.resStrings.themeHint)
                    color(ctx.theme.colors.backgroundElement)
                    fontWeightBold()
                }
            }

            View {
                attr {
                    flexDirectionRow()
                }
                for ((name, theme) in ThemeManager.COLOR_SCHEME_MAP) {
                    View {
                        attr {
                            size(100f, 64f)
                            marginLeft(12f)
                            border(Border(1f, BorderStyle.SOLID, ctx.theme.colors.backgroundElement))
                            borderRadius(12f)
                            backgroundColor(theme.primary)
                            allCenter()
                        }
                        // 显示已选中标记
                        vif({ ctx.theme.colors == theme }) {
                            Text {
                                attr {
                                    margin(12f)
                                    fontSize(16f)
                                    text(ctx.resStrings.chosen)
                                    color(theme.topBarTextFocused)
                                }
                            }
                        }
                        event {
                            click {
                                if (theme != ctx.theme.colors) {
                                    // 切换主题
                                    ThemeManager.changeColorScheme(name)
                                    ctx.theme = ThemeManager.getTheme()
                                    // 持久化保存
                                    ctx.spModule.setString(ThemeManager.PREF_KEY_COLOR, name)
                                    // 通知其他页面
                                    ctx.notifyModule.postNotify(ThemeManager.SKIN_CHANGED_EVENT, JSONObject())
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 资源主题选择
    private fun assetChooseView(): ViewBuilder {
        val ctx = this
        return {
            Text {
                attr {
                    margin(16f)
                    fontSize(20f)
                    text(ctx.resStrings.themeHint + ": " + ctx.theme.asset)
                    color(ctx.theme.colors.backgroundElement)
                    fontWeightBold()
                }
            }
            View {
                attr {
                    flexDirectionRow()
                }
                for (name in ThemeManager.ASSET_SCHEME_LIST) {
                    View {
                        attr {
                            marginLeft(16f)
                            allCenter()
                            flexDirectionColumn()
                        }

                        Image {
                            attr {
                                size(40f, 40f)
                                src(ThemeManager.getAssetUri(name, "tabbar_home.png"))
                                tintColor(ctx.theme.colors.backgroundElement)
                            }
                        }

                        Text {
                            attr {
                                fontSize(16f)
                                marginTop(6f)
                                text(name)
                                color(ctx.theme.colors.backgroundElement)
                            }
                        }
                        event {
                            click {
                                if (name != ctx.theme.asset) {
                                    ThemeManager.changeAssetScheme(name)
                                    ctx.theme = ThemeManager.getTheme()
                                    ctx.spModule.setString(ThemeManager.PREF_KEY_ASSET, name)
                                    ctx.notifyModule.postNotify(ThemeManager.SKIN_CHANGED_EVENT, JSONObject())
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 语言选择
    private fun langChooseView(): ViewBuilder {
        val ctx = this
        return {
            Text {
                attr {
                    margin(16f)
                    fontSize(20f)
                    text(ctx.resStrings.languageHint)
                    color(ctx.theme.colors.backgroundElement)
                    fontWeightBold()
                }
            }

            List {
                attr {
                    flex(1f)
                    flexDirectionColumn()
                    bouncesEnable(false)
                    scrollEnable(false)
                    justifyContentFlexStart()
                    alignItemsCenter()
                }
                for ((displayName, langCode) in LangManager.SUPPORTED_LANGUAGES) {
                    View {
                        attr {
                            height(44f)
                            backgroundColor(ctx.theme.colors.feedBackground)
                            flexDirectionRow()
                            alignItemsCenter()
                        }
                        Text {
                            attr {
                                marginLeft(12f)
                                fontSize(16f)
                                color(ctx.theme.colors.feedContentText)
                                text(displayName)
                            }
                        }
                        // 选中标记
                        vif ({ ctx.lang == langCode }) {
                            Image {
                                attr {
                                    absolutePosition(right = 12f, top = 10f, bottom = 10f)
                                    src(ImageUri.commonAssets("ic_chosen.png"))
                                    tintColor(Color.GREEN)
                                    size(24f, 24f)
                                }
                            }
                        }
                        event {
                            click {
                                if (LangManager.getCurrentLanguage() != langCode) {
                                    LangManager.changeLanguage(langCode)
                                    ctx.settingLangHint = LangManager.SETTING_HINTS[langCode] ?: "Setting..."
                                    ctx.showModal = true
                                    // 延迟关闭弹窗
                                    setTimeout(500) {
                                        ctx.lang = LangManager.getCurrentLanguage()
                                        ctx.spModule.setString(LangManager.KEY_PREF_LANGUAGE, langCode)
                                        ctx.notifyModule.postNotify(LangManager.LANG_CHANGED_EVENT, JSONObject())
                                        ctx.showModal = false
                                    }
                                }
                            }
                        }
                    }
                    // 分割线
                    View {
                        attr {
                            margin(left = 12.0f)
                            height(0.5f)
                            backgroundColor(ctx.theme.colors.feedContentDivider)
                        }
                    }
                }
            }
        }
    }

    // 加载中弹窗
    private fun modalView(): ViewBuilder {
        val ctx = this
        return {
            Modal(true) {
                attr {
                    allCenter()
                }
                // 背景遮罩（淡入淡出）
                TransitionView(type = TransitionType.FADE_IN_OUT) {
                    attr {
                        absolutePositionAllZero()
                        backgroundColor(Color(0x88000000))
                    }
                }
                // 弹窗内容（缩放动画）
                TransitionView(type = TransitionType.CUSTOM) {
                    attr {
                        padding(top = 30f, bottom = 20f)
                        transitionAppear(ctx.showModal)
                        size(160f, 120f)
                        borderRadius(16f)
                        backgroundColor(0xFF404040)
                        flexDirectionColumn()
                        justifyContentFlexStart()
                        alignItemsCenter()
                        // 动画配置
                        customBeginAnimationAttr {
                            opacity(0f)
                            transform(scale = Scale(0.7f, 0.7f))
                        }
                        customEndAnimationAttr {
                            opacity(1f)
                            transform(scale = Scale(1f, 1f))
                        }
                        customAnimation(Animation.springEaseInOut(0.3f, 0.8f, 0.9f))
                    }
                    // 加载指示器
                    ActivityIndicator {
                        attr {
                            zIndex(0)
                            isGrayStyle(false)
                            transform(Scale(1.5f, 1.5f))
                        }
                    }
                    // 提示文字
                    Text {
                        attr {
                            marginTop(20f)
                            text(ctx.settingLangHint)
                            fontSize(15f)
                            color(Color.WHITE)
                        }
                    }
                }
            }
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            // 顶部导航栏
            ctx.topNavBar().invoke(this)

            // 内容区域
            View {
                attr {
                    flex(1f)
                    backgroundColor(ctx.theme.colors.background)
                    paddingTop(12f)
                    flexDirectionColumn()
                }
                ctx.skinChooseView().invoke(this)
                ctx.assetChooseView().invoke(this)
                ctx.langChooseView().invoke(this)
            }

            // 加载中弹窗
            vif({ ctx.showModal }) {
                ctx.modalView().invoke(this)
            }
        }
    }
}
```

## 关键点说明

### 1. 顶部导航栏

- 使用 `absolutePosition` 定位返回按钮
- 使用 `RouterModule.closePage()` 关闭页面
- 考虑状态栏高度 `pagerData.statusBarHeight`

### 2. 主题切换

- 遍历 `ThemeManager.COLOR_SCHEME_MAP` 显示所有主题
- 使用 `vif` 显示当前选中状态
- 切换后调用 `postNotify` 通知其他页面

### 3. 语言切换

- 遍历 `LangManager.SUPPORTED_LANGUAGES` 显示所有语言
- 切换时显示加载弹窗
- 使用 `setTimeout` 延迟关闭弹窗

### 4. Modal 弹窗

- 使用 `Modal` 组件创建模态弹窗
- 使用 `TransitionView` 添加动画效果
- `FADE_IN_OUT` 类型用于背景遮罩
- `CUSTOM` 类型用于自定义动画

### 5. 动画配置

```kotlin
customBeginAnimationAttr {
    opacity(0f)
    transform(scale = Scale(0.7f, 0.7f))
}
customEndAnimationAttr {
    opacity(1f)
    transform(scale = Scale(1f, 1f))
}
customAnimation(Animation.springEaseInOut(0.3f, 0.8f, 0.9f))
```

- `customBeginAnimationAttr` 定义动画开始状态
- `customEndAnimationAttr` 定义动画结束状态
- `customAnimation` 定义动画曲线
