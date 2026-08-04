# WaterfallList 瀑布流

瀑布流列表组件，可根据项目的大小和位置自动调整项目的位置，继承自 `List`。

```kotlin
import com.tencent.kuikly.core.views.WaterfallList
import com.tencent.kuikly.core.directives.vforLazy
```

**基本用法：**

```kotlin
WaterfallList {
    attr {
        flex(1f)
        listWidth(pagerData.pageViewWidth)
        columnCount(2)
        itemSpaceing(10f)
        lineSpacing(10f)
    }
    
    vforLazy({ items }) { item, _ ->
        View {
            attr {
                width((pagerData.pageViewWidth - 30f) / 2)
                height(item.height)
            }
        }
    }
}
```

**属性 API：**

| 属性方法 | 描述 | 参数类型 |
|---------|------|---------|
| `listWidth(width)` | 列表宽度（必须设置） | Float |
| `columnCount(count)` | 列数，默认 `1` | Int |
| `itemSpaceing(spacing)` | 列间距，默认 `0` | Float |
| `lineSpacing(spacing)` | 行间距，默认 `0` | Float |
| `contentPadding(padding)` | 内部间隔，默认 `0` | Float |

继承 List 的所有属性、事件和方法。
