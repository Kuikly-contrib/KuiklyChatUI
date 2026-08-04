# ComposeView 组件模板

## 概述

`ComposeView` 是 Kuikly 中创建可复用组件的基类。通过继承 `ComposeView`，可以创建具有自定义属性和事件的组件。

## 基本结构

```kotlin
package com.example.app.pages.app.components

import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.reactive.handler.observable

// 1. 组件类
internal class MyComponent : ComposeView<MyComponentAttr, MyComponentEvent>() {

    // 组件内部状态
    private var internalState by observable("")

    // 创建属性实例
    override fun createAttr(): MyComponentAttr {
        return MyComponentAttr()
    }

    // 创建事件实例
    override fun createEvent(): MyComponentEvent {
        return MyComponentEvent()
    }

    // 组件 UI
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            // 使用 ctx.attr.xxx 访问外部传入的属性
            // 使用 ctx.event.xxx 触发事件回调
        }
    }
}

// 2. 属性类
internal class MyComponentAttr : ComposeAttr() {
    // 定义组件接收的属性
    var title by observable("")
    var subtitle by observable("")
    var isEnabled by observable(true)
}

// 3. 事件类
internal class MyComponentEvent : ComposeEvent() {
    // 定义组件触发的事件
    var onClick: (() -> Unit)? = null
    var onValueChanged: ((String) -> Unit)? = null
}

// 4. 扩展函数（方便在 DSL 中使用）
internal fun ViewContainer<*, *>.MyComponent(init: MyComponent.() -> Unit) {
    addChild(MyComponent(), init)
}
```

## 完整示例：用户信息卡片组件

```kotlin
package com.example.app.pages.app.components

import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.module.CallbackRef
import com.tencent.kuikly.core.module.NotifyModule
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.example.app.pages.app.theme.ThemeManager

internal class UserCardView : ComposeView<UserCardViewAttr, UserCardViewEvent>() {

    // 主题状态
    private var theme by observable(ThemeManager.getTheme())
    private lateinit var themeEventCallbackRef: CallbackRef

    // 生命周期：创建
    override fun created() {
        super.created()
        // 注册主题变更监听
        themeEventCallbackRef = acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .addNotify(ThemeManager.SKIN_CHANGED_EVENT) { _ ->
                theme = ThemeManager.getTheme()
            }
    }

    // 生命周期：销毁
    override fun viewDestroyed() {
        super.viewDestroyed()
        // 移除监听
        acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .removeNotify(ThemeManager.SKIN_CHANGED_EVENT, themeEventCallbackRef)
    }

    override fun createAttr(): UserCardViewAttr {
        return UserCardViewAttr()
    }

    override fun createEvent(): UserCardViewEvent {
        return UserCardViewEvent()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flexDirectionRow()
                padding(12f)
                backgroundColor(ctx.theme.colors.feedBackground)
                borderRadius(8f)
            }
            event {
                click {
                    // 触发点击事件
                    ctx.event.onCardClick?.invoke()
                }
            }
            
            // 头像
            Image {
                attr {
                    size(50f, 50f)
                    borderRadius(25f)
                    src(ctx.attr.avatarUrl)
                }
            }
            
            // 用户信息
            View {
                attr {
                    flex(1f)
                    marginLeft(12f)
                    flexDirectionColumn()
                    justifyContentCenter()
                }
                
                // 用户名
                Text {
                    attr {
                        text(ctx.attr.userName)
                        fontSize(16f)
                        fontWeightBold()
                        color(ctx.theme.colors.feedUserNameNormal)
                    }
                }
                
                // 签名
                Text {
                    attr {
                        text(ctx.attr.signature)
                        fontSize(12f)
                        color(ctx.theme.colors.feedUserSignature)
                        marginTop(4f)
                    }
                }
            }
            
            // 关注按钮
            View {
                attr {
                    padding(horizontal = 16f, vertical = 8f)
                    backgroundColor(ctx.theme.colors.feedUserFollowButton)
                    borderRadius(16f)
                    alignSelfCenter()
                }
                event {
                    click {
                        ctx.event.onFollowClick?.invoke(ctx.attr.userId)
                    }
                }
                Text {
                    attr {
                        text(if (ctx.attr.isFollowing) "已关注" else "关注")
                        fontSize(14f)
                        color(Color.WHITE)
                    }
                }
            }
        }
    }
}

// 属性定义
internal class UserCardViewAttr : ComposeAttr() {
    var userId by observable("")
    var userName by observable("")
    var avatarUrl by observable("")
    var signature by observable("")
    var isFollowing by observable(false)
}

// 事件定义
internal class UserCardViewEvent : ComposeEvent() {
    var onCardClick: (() -> Unit)? = null
    var onFollowClick: ((userId: String) -> Unit)? = null
}

// DSL 扩展函数
internal fun ViewContainer<*, *>.UserCard(init: UserCardView.() -> Unit) {
    addChild(UserCardView(), init)
}
```

## 使用组件

```kotlin
// 在页面或其他组件中使用
override fun body(): ViewBuilder {
    val ctx = this
    return {
        UserCard {
            attr {
                userId = "123"
                userName = "张三"
                avatarUrl = "https://example.com/avatar.png"
                signature = "这是我的签名"
                isFollowing = false
            }
            event {
                onCardClick = {
                    // 处理卡片点击
                }
                onFollowClick = { userId ->
                    // 处理关注点击
                }
            }
        }
    }
}
```

## 生命周期方法

| 方法 | 说明 |
|------|------|
| `created()` | 组件创建时调用，用于初始化 |
| `viewDidLoad()` | 视图加载完成后调用 |
| `viewWillUnload()` | 视图即将卸载时调用 |
| `viewDestroyed()` | 视图销毁时调用，用于清理资源 |

## 最佳实践

### 1. 主题集成

每个组件都应该集成主题系统：

```kotlin
private var theme by observable(ThemeManager.getTheme())
private lateinit var themeEventCallbackRef: CallbackRef

override fun created() {
    super.created()
    themeEventCallbackRef = acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
        .addNotify(ThemeManager.SKIN_CHANGED_EVENT) { _ ->
            theme = ThemeManager.getTheme()
        }
}

override fun viewDestroyed() {
    super.viewDestroyed()
    acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
        .removeNotify(ThemeManager.SKIN_CHANGED_EVENT, themeEventCallbackRef)
}
```

### 2. 属性默认值

使用 `observable` 为属性设置默认值：

```kotlin
internal class MyComponentAttr : ComposeAttr() {
    var title by observable("")           // 默认空字符串
    var count by observable(0)            // 默认 0
    var isVisible by observable(true)     // 默认 true
    var items by observableList<Item>()   // 默认空列表
}
```

### 3. 事件回调

使用可空函数类型定义事件：

```kotlin
internal class MyComponentEvent : ComposeEvent() {
    var onClick: (() -> Unit)? = null
    var onValueChanged: ((value: String) -> Unit)? = null
    var onItemSelected: ((index: Int, item: Item) -> Unit)? = null
}
```

### 4. 组件引用

如果需要从外部控制组件，使用 `ref`：

```kotlin
// 定义引用
private lateinit var cardRef: ViewRef<UserCardView>

// 使用组件时设置引用
UserCard {
    ref {
        cardRef = it
    }
    attr { ... }
}

// 通过引用调用组件方法
cardRef.view?.someMethod()
```

### 5. 条件渲染

在组件内部使用 `vif` 进行条件渲染：

```kotlin
override fun body(): ViewBuilder {
    val ctx = this
    return {
        vif({ ctx.attr.isVisible }) {
            View {
                // 可见时的内容
            }
        }
    }
}
```

### 6. 列表渲染

使用 `vfor` 渲染列表（注意：列表必须是 `observableList`）：

```kotlin
internal class MyComponentAttr : ComposeAttr() {
    var items by observableList<Item>()  // 必须使用 observableList
}

override fun body(): ViewBuilder {
    val ctx = this
    return {
        vfor({ ctx.attr.items }) { item, index ->
            View {
                // 列表项内容
            }
        }
    }
}
```
