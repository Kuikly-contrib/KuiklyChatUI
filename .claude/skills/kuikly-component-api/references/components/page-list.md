# PageList 分页列表

分页列表组件，每个 Item 的宽高与 PageList 相同，滑动是以分页进行滑动。继承自 `List`，间接继承自 `Scroller`。

```kotlin
import com.tencent.kuikly.core.views.PageList
```

**基本用法：**

```kotlin
PageList {
    attr {
        flex(1f)
        pageDirection(true) // 横向
        defaultPageIndex(0)
    }
    event {
        pageIndexDidChanged { it ->
            val index = (it as JSONObject).optInt("index")
            // 页面切换
        }
    }
    
    vfor({ pages }) { page, _ ->
        View { /* 页面内容 */ }
    }
}
```

**属性 API：**

| 属性方法 | 描述 | 参数类型 |
|---------|------|---------|
| `pageItemWidth(width)` | Item 宽度 | Float |
| `pageItemHeight(height)` | Item 高度 | Float |
| `pageDirection(isHorizontal)` | 排列方向 | Boolean |
| `defaultPageIndex(index)` | 默认定位到的页数 | Int |
| `keepItemAlive(alive)` | 不可见的 Item 是否常驻内存 | Boolean |
| `offscreenPageLimit(value)` | 离屏 page 个数，仅当 `keepItemAlive` 为 `false` 时有效，默认 `2` | Int |

**事件：**

| 事件 | 描述 | 回调参数 |
|-----|------|---------|
| `pageIndexDidChanged { }` | 页面切换 | 为 JSONObject 类型，包含 index 字段 |

```kotlin
PageList {
    ...
    event {
        pageIndexDidChanged {
            val index = (it as JSONObject).optInt("index")
            ...
        }
    }
    ...
}
```

**方法：**

| 方法 | 描述 | 参数 |
|-----|------|-----|
| `scrollToPageIndex(index, animated, springAnimation)` | 滚动到指定页 | Int, Boolean, SpringAnimation? |
