# RichText 富文本

富文本组件，支持不同文本样式和图片，继承自 `Text` 组件。

```kotlin
import com.tencent.kuikly.core.views.RichText
```

**基本用法：**

```kotlin
RichText {
    attr {
        width(300f)
    }
    
    Span {
        text("红色文本")
        color(Color.RED)
        fontSize(16f)
    }
    
    Span {
        text("蓝色粗体")
        color(Color.BLUE)
        fontSize(20f)
        fontWeightBold()
    }
    
    ImageSpan {
        size(30f, 30f)
        src("emoji.png")
    }
    
    PlaceholderSpan {
        placeholderSize(50f, 50f)
        spanFrameDidChanged { frame ->
            // 获取占位区域位置，可配合叠加其他任意 View
        }
    }
}
```

## Span

添加一段样式可单独控制的文本，支持 Text 组件的所有属性。

| 参数 | 描述 | 类型 |
|-----|------|-----|
| `textSpanInit` | Span 的初始化闭包 | TextSpan.() -> Unit |

| 事件 | 描述 | 回调参数 |
|-----|------|---------| 
| `click { }` | 单击事件 | ClickParams |

## ImageSpan

添加一个图片，支持 Image 组件的相关属性。

| 参数 | 描述 | 类型 |
|-----|------|-----|
| `spanInit` | ImageSpan 的初始化闭包 | ImageSpan.() -> Unit |

| 属性方法 | 描述 | 参数类型 |
|---------|------|---------| 
| `size(width, height)` | 图片大小 | Float, Float |
| `src(src)` | 图片源 | String |
| `src(uri)` | Assets 资源 | ImageUri |
| `resizeCover()` | 等比缩放覆盖 | - |
| `resizeContain()` | 等比缩放包含 | - |
| `resizeStretch()` | 拉伸填充 | - |
| `borderRadius(radius)` | 圆角 | Float |
| `verticalAlignOffset(offset)` | 垂直方向偏移 | Float |
| `horizontalAlignOffset(offset)` | 水平方向偏移 | Float |

| 事件 | 描述 | 回调参数 |
|-----|------|---------| 
| `click { }` | 单击事件 | ClickParams |

## PlaceholderSpan

添加空白占位区域，可通过 `spanFrameDidChanged` 事件监听位置变化来配合叠加其他任意 View。

| 属性方法 | 描述 | 参数类型 |
|---------|------|---------|
| `placeholderSize(width, height)` | 占位大小 | Float, Float |

| 事件 | 描述 | 回调参数 |
|-----|------|---------|
| `spanFrameDidChanged { }` | 位置变化 | Frame |
