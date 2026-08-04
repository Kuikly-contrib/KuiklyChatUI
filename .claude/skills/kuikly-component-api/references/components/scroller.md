# Scroller 滚动容器

用于展示不确定高度的内容的滚动容器，可以将一系列不确定高度的子组件装到一个确定高度的容器中。

```kotlin
import com.tencent.kuikly.core.views.Scroller
```

**基本用法：**

```kotlin
Scroller {
    attr {
        flex(1f)
        flexDirection(FlexDirection.COLUMN)
    }
    
    // 子组件
    View { attr { height(500f) } }
    View { attr { height(500f) } }
}
```

**属性 API：**

| 属性方法 | 描述 | 参数类型 |
|---------|------|---------|
| `scrollEnable(enable)` | 是否允许滑动，默认 `true` | Boolean |
| `bouncesEnable(enable)` | 是否开启回弹，默认 `true` | Boolean |
| `showScrollerIndicator(show)` | 是否显示滚动条，默认 `true` | Boolean |
| `flexDirection(direction)` | 排版方向 | FlexDirection |
| `pagingEnable(enable)` | 是否开启分页 | Boolean |
| `nestedScroll(forward, backward)` | 嵌套滚动模式 | KRNestedScrollMode |
| `visibleAreaIgnoreTopMargin(margin)` | 计算可见性时忽略顶部距离 | Float |
| `visibleAreaIgnoreBottomMargin(margin)` | 计算可见性时忽略底部距离 | Float |

**KRNestedScrollMode：**

| 值 | 描述 |
|----|------|
| `SELF_ONLY` | 仅当前控件处理滚动，不传递给父控件 |
| `SELF_FIRST` | 当前控件优先处理，未消费完传递给父控件 |
| `PARENT_FIRST` | 父控件优先处理，未消费完传递给当前控件 |

**事件：**

| 事件 | 描述 | 回调参数 |
|-----|------|---------|
| `scroll { }` | 滚动事件 | ScrollParams |
| `scrollEnd { }` | 滚动结束 | ScrollParams |
| `dragBegin { }` | 开始拖拽 | ScrollParams |
| `dragEnd { }` | 停止拖拽 | ScrollParams |
| `scrollToTop { }` | iOS 点击状态栏回到顶部 | - |
| `contentSizeChanged { }` | 内容尺寸变化 | width, height |

**方法：**

| 方法 | 描述 | 参数 |
|-----|------|-----|
| `setContentOffset(offsetX, offsetY, animated, springAnimation)` | 滚动到指定位置 | Float, Float, Boolean, SpringAnimation? |
| `setContentInset(top, left, bottom, right, animated)` | 设置内容边距 | Float... |
| `setContentInsetWhenEndDrag(top, left, bottom, right)` | 设置 OverScroll 时停留边距 | Float... |
