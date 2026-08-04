# Button 按钮

按钮组件。

```kotlin
import com.tencent.kuikly.core.views.compose.Button
```

**基本用法：**

```kotlin
Button {
    attr {
        size(100f, 40f)
        backgroundColor(Color.BLUE)
        borderRadius(8f)
        highlightBackgroundColor(Color(0xFF0000AA))
        titleAttr {
            text("点击")
            color(Color.WHITE)
            fontSize(16f)
        }
        imageAttr {
            src("icon.png")
            size(20f, 20f)
        }
    }
    event {
        click { }
    }
}
```

**属性 API：**

| 属性方法 | 描述 | 参数类型 |
|---------|------|---------|
| `titleAttr { }` | 文本属性 | TextAttr |
| `imageAttr { }` | 图片属性 | ImageAttr |
| `highlightBackgroundColor(color)` | 按下态颜色 | Color |
| `flexDirection(direction)` | 图标和文本排列方向 | FlexDirection |
| `glassEffectIOS(enable, interactive, tintColor, style)` | iOS 26+ 液态玻璃效果 | Boolean... |
