# Hover 悬停置顶

用于列表下的自动悬停视图组件，列表滚动时可自动悬浮置顶。

```kotlin
import com.tencent.kuikly.core.views.Hover
```

**基本用法：**

```kotlin
List {
    attr { flex(1f) }
    
    // 列表内容
    Text { attr { height(500f) } }
    
    // 悬停视图
    Hover {
        attr {
            absolutePosition(top = 300f, left = 0f, right = 0f)
            height(50f)
            backgroundColor(Color.RED)
            bringIndex(1)
            hoverMarginTop(0f)
        }
        
        Text { attr { text("悬停标题") } }
    }
}
```

**属性 API：**

| 属性方法 | 描述 | 参数类型 |
|---------|------|---------|
| `bringIndex(index)` | 置顶层级（同一列表多个 HoverView，值越大层级越高） | Int |
| `hoverMarginTop(offset)` | 悬停距离列表顶部距离，默认 0 | Float |
