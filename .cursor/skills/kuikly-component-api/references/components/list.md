# List 列表

用于展示一系列项目的视图组件，支持垂直和水平方向，继承自 `Scroller`。

```kotlin
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.directives.vforLazy
```

**基本用法：**

```kotlin
List {
    attr {
        flex(1f)
        flexDirection(FlexDirection.COLUMN)
    }
    event {
        scroll { params ->
            val offsetY = params.offsetY
        }
    }
    
    // 使用 vfor 循环渲染
    vfor({ dataList }) { item, index ->
        Text {
            attr {
                text(item.title)
                height(50f)
            }
        }
    }
}
```

**属性 API：**

| 属性方法 | 描述 | 参数类型 |
|---------|------|---------|
| `scrollEnable(value)` | 是否允许滑动，默认 `true` | Boolean |
| `bouncesEnable(value)` | 是否开启回弹，默认 `true` | Boolean |
| `showScrollerIndicator(value)` | 是否显示滚动条，默认 `true` | Boolean |
| `flexDirection(direction)` | 排版方向 | FlexDirection |
| `pagingEnable(enable)` | 是否开启分页 | Boolean |
| `firstContentLoadMaxIndex(maxIndex)` | 首次加载最大条数，优化首屏耗时 | Int |
| `visibleAreaIgnoreTopMargin(margin)` | 计算可见性时忽略顶部距离 | Float |
| `visibleAreaIgnoreBottomMargin(margin)` | 计算可见性时忽略底部距离 | Float |
| `preloadViewDistance(distance)` | 预加载距离，默认为 List 高度 | Float |
| `scrollToPosition(index, offset, animate)` | 滚动到指定位置 | Int, Float, Boolean |

**事件：**

| 事件 | 描述 | 回调参数 |
|-----|------|---------|
| `scroll { }` | 滚动事件 | ScrollParams |
| `scrollEnd { }` | 滚动结束 | ScrollParams |
| `dragBegin { }` | 开始拖拽 | ScrollParams |
| `dragEnd { }` | 停止拖拽 | ScrollParams |
| `contentSizeChanged { }` | 内容尺寸变化 | width, height |

**ScrollParams：**

| 参数 | 描述 | 类型 |
|-----|------|-----|
| `offsetX` | 横轴偏移量 | Float |
| `offsetY` | 纵轴偏移量 | Float |
| `contentWidth` | 内容总宽度 | Float |
| `contentHeight` | 内容总高度 | Float |
| `viewWidth` | View 宽度 | Float |
| `viewHeight` | View 高度 | Float |
| `isDragging` | 是否拖拽中 | Boolean |

**方法：**

| 方法 | 描述 | 参数 |
|-----|------|-----|
| `setContentOffset(offsetX, offsetY, animated, springAnimation)` | 滚动到指定位置 | Float, Float, Boolean, SpringAnimation? |
| `setContentInset(top, left, bottom, right, animated)` | 设置内容边距 | Float... |
| `getFirstVisiblePosition()` | 获取第一个可见 item 位置 | - |
