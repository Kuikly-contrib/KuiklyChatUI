# Canvas 画布

自绘画布组件。

```kotlin
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.CanvasLinearGradient
import com.tencent.kuikly.core.views.TextAlign
import com.tencent.kuikly.core.views.FontStyle
import com.tencent.kuikly.core.views.FontWeight
```

**基本用法：**

```kotlin
Canvas({
    attr {
        size(300f, 300f)
    }
}) { context, width, height ->
    // 绘制直线
    context.beginPath()
    context.strokeStyle(Color.RED)
    context.lineWidth(2f)
    context.moveTo(0f, 0f)
    context.lineTo(width, height)
    context.stroke()
    
    // 绘制圆形
    context.beginPath()
    context.fillStyle(Color.BLUE)
    context.arc(width / 2, height / 2, 50f, 0f, (2 * PI).toFloat(), false)
    context.fill()
    
    // 绘制文本
    context.fillStyle(Color.BLACK)
    context.font(size = 20f, weight = FontWeight.BOLD)
    context.fillText("Hello", 10f, 50f)
    
    // 绘制线性渐变
    val gradient = context.createLinearGradient(0f, 0f, width, 0f)
    gradient.addColorStop(0f, Color.RED)
    gradient.addColorStop(1f, Color.BLUE)
    context.fillStyle(gradient)
    context.fillRect(10f, 100f, 100f, 50f)
}
```

**CanvasContext API：**

**路径操作：**

| 方法 | 描述 | 参数 |
|-----|------|-----|
| `beginPath()` | 新建路径 | - |
| `moveTo(x, y)` | 移动到坐标 | Float, Float |
| `lineTo(x, y)` | 绘制直线到坐标 | Float, Float |
| `arc(centerX, centerY, radius, startAngle, endAngle, counterclockwise)` | 绘制圆弧 | Float... |
| `closePath()` | 闭合路径 | - |
| `quadraticCurveTo(cpx, cpy, x, y)` | 二次贝塞尔曲线 | Float... |
| `bezierCurveTo(cp1x, cp1y, cp2x, cp2y, x, y)` | 三次贝塞尔曲线 | Float... |

**描边与填充：**

| 方法 | 描述 | 参数 |
|-----|------|-----|
| `stroke()` | 描边路径 | - |
| `strokeStyle(color)` | 描边颜色 | Color |
| `strokeStyle(linearGradient)` | 描边渐变 | CanvasLinearGradient |
| `fill()` | 填充路径 | - |
| `fillStyle(color)` | 填充颜色 | Color |
| `fillStyle(linearGradient)` | 填充渐变 | CanvasLinearGradient |

**线条样式：**

| 方法 | 描述 | 参数 |
|-----|------|-----|
| `lineWidth(width)` | 线条宽度（默认 0f） | Float |
| `setLineDash(intervals)` | 虚线样式（空数组切换至实线模式） | List\<Float\> |
| `lineCapRound()` | 圆形线端 | - |
| `lineCapButt()` | 平直线端 | - |
| `lineCapSquare()` | 方形线端 | - |

**渐变：**

| 方法 | 描述 | 参数 |
|-----|------|-----|
| `createLinearGradient(x0, y0, x1, y1)` | 创建线性渐变 | Float... |
| `createRadialGradient(x0, y0, r0, x1, y1, r1, alpha, vararg colors)` | 创建径向渐变（仅 iOS） | Float..., Color... |

**CanvasLinearGradient 方法：**

| 方法 | 描述 | 参数 |
|-----|------|-----|
| `addColorStop(stopIn01, color)` | 添加颜色停点 [0, 1] | Float, Color |

**文本：**

| 方法 | 描述 | 参数 |
|-----|------|-----|
| `textAlign(align)` | 文本对齐 | TextAlign |
| `font(size, family)` | 文本样式（简化版） | Float, String |
| `font(style, weight, size, family)` | 文本样式（完整版） | FontStyle?, FontWeight?, Float?, String? |
| `measureText(value)` | 测量文本，返回 TextMetrics | String |
| `fillText(text, x, y)` | 填充文本 | String, Float, Float |
| `strokeText(text, x, y)` | 描边文本 | String, Float, Float |

**TextMetrics：**

| 属性 | 描述 | 类型 |
|-----|------|-----|
| `width` | 文本宽度 | Float |
| `actualBoundingBoxLeft` | 左边界 | Float |
| `actualBoundingBoxRight` | 右边界 | Float |
| `actualBoundingBoxAscent` | 上边界 | Float |
| `actualBoundingBoxDescent` | 下边界 | Float |

**图像：**

| 方法 | 描述 | 参数 |
|-----|------|-----|
| `drawImage(image, dx, dy)` | 绘制图像 | ImageRef, Float, Float |
| `drawImage(image, dx, dy, dWidth, dHeight)` | 绘制图像（指定大小） | ImageRef, Float... |
| `drawImage(image, sx, sy, sWidth, sHeight, dx, dy, dWidth, dHeight)` | 绘制图像（裁剪） | ImageRef, Float... |

**变换：**

| 方法 | 描述 | 参数 |
|-----|------|-----|
| `translate(x, y)` | 平移 | Float, Float |
| `scale(x, y)` | 缩放 | Float, Float |
| `rotate(angle)` | 旋转 | Float |
| `skew(x, y)` | 倾斜 | Float, Float |
| `transform(array)` | 矩阵变换（9 元素数组） | FloatArray |

**状态管理：**

| 方法 | 描述 | 参数 |
|-----|------|-----|
| `save()` | 保存状态 | - |
| `saveLayer(x, y, width, height)` | 保存并创建图层 | Float... |
| `restore()` | 恢复状态 | - |
| `clip(intersect)` | 裁剪（intersect 默认 true） | Boolean |
| `clipPathIntersect()` | 裁剪（交集） | - |
| `clipPathDifference()` | 裁剪（差集） | - |
