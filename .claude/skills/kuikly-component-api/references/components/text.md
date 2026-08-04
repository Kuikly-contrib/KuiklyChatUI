# Text 文本

用于显示文本的组件。

```kotlin
import com.tencent.kuikly.core.views.Text
```

**基本用法：**

```kotlin
Text {
    attr {
        text("Hello Kuikly")
        fontSize(20f)
        color(Color.BLACK)
        fontWeightBold()
    }
}
```

**属性 API：**

| 属性方法 | 描述 | 参数类型 |
|---------|------|---------|
| `text(text)` | 设置文本内容 | String |
| `color(color)` | 字体颜色 | Color / Long |
| `fontSize(size)` | 字体大小 | Float |
| `fontWeight400()` | 字重 400 | - |
| `fontWeight500()` | 字重 500 | - |
| `fontWeight600()` | 字重 600 | - |
| `fontWeight700()` | 字重 700 | - |
| `fontWeightNormal()` | 正常字重 (400) | - |
| `fontWeightMedium()` | 中等字重 (500) | - |
| `fontWeightSemisolid()` | 半粗字重 (600) | - |
| `fontWeightBold()` | 粗体字重 (700) | - |
| `fontFamily(fontFamily)` | 字体名称 | String |
| `lines(lines)` | 最大行数 | Int |
| `lineBreakMargin(margin)` | 最后一行折叠距离 | Float |
| `textOverFlowMiddle()` | 省略号显示在中间 | - |
| `textOverFlowTail()` | 省略号显示在尾部 | - |
| `textOverFlowClip()` | 直接截断不显示省略号 | - |
| `textDecorationUnderLine()` | 下划线 | - |
| `textDecorationLineThrough()` | 删除线 | - |
| `textShadow(offsetX, offsetY, blur, color)` | 文字阴影 | Float, Float, Float, Color |
| `textAlignLeft()` | 左对齐 | - |
| `textAlignCenter()` | 居中对齐 | - |
| `textAlignRight()` | 右对齐 | - |
| `lineHeight(lineHeight)` | 行高 | Float |
| `lineSpacing(value)` | 行间距 | Float |
| `letterSpacing(value)` | 字间距 | Float |
| `fontStyleItalic()` | 斜体 | - |
| `firstLineHeadIndent(indent)` | 首行缩进 | Float |
| `useDpFontSizeDim()` | 使用 dp 作为字体单位 | - |
| `textStroke(color, width)` | 文字描边 | Color, Float |
