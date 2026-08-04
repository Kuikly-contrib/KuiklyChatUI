# SliderPage 轮播图

轮播图组件。

```kotlin
import com.tencent.kuikly.core.views.compose.SliderPage
```

**基本用法：**

```kotlin
SliderPage {
    ref { sliderPageRef = it }
    attr {
        size(pagerData.pageViewWidth, 200f)
        itemCount(images.size)
        loopPlayIntervalTimeMs(3000)
        defaultPageIndex(0)
        isHorizontal(true)
        scrollEnable(true)
    }
    event {
        pageIndexDidChanged { index ->
            // 页面切换
        }
    }
    
    initSliderItems(images) { image, _ ->
        Image {
            attr {
                flex(1f)
                src(image.url)
                resizeCover()
            }
        }
    }
}
```

**属性 API：**

| 属性方法 | 描述 | 参数类型 |
|---------|------|---------|
| `defaultPageIndex` | 初始页面索引 | Int |
| `isHorizontal` | 是否水平方向（默认 true） | Boolean |
| `pageItemWidth` | 页面宽度 | Float |
| `pageItemHeight` | 页面高度 | Float |
| `loopPlayIntervalTimeMs` | 轮播间隔（毫秒），0 表示不轮播（默认 3000） | Int |
| `scrollEnable` | 是否允许滑动（默认 true） | Boolean |
| `itemCount` | 页面数量 | Int |
| `initSliderItems(dataList) { }` | 初始化数据和创建器 | List, SliderItemCreator |

**事件：**

| 事件 | 描述 | 回调参数 |
|-----|------|---------|
| `pageIndexDidChanged { }` | 页面切换 | Int |

**方法：**

| 方法 | 描述 | 参数 |
|-----|------|-----|
| `startLoopPlayIfNeed()` | 启动循环播放 | - |
| `stopLoopPlayIfNeed()` | 停止循环播放 | - |
| `scrollToPage(index, animation)` | 滚动到指定页 | Int, Boolean |
