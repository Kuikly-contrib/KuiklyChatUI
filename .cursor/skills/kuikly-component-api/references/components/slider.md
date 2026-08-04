# Slider 滑块

滑动选择器组件，可用于拖动进度条场景。

```kotlin
import com.tencent.kuikly.core.views.Slider
```

**基本用法：**

```kotlin
Slider {
    attr {
        width(300f)
        height(30f)
        currentProgress(0.5f)
        progressColor(Color.BLUE)
        trackColor(Color.GRAY)
        thumbColor(Color.WHITE)
    }
    event {
        progressDidChanged { progress ->
            // 进度变化
        }
    }
}
```

**属性 API：**

| 属性方法 | 描述 | 参数类型 |
|---------|------|---------|
| `currentProgress(progress)` | 当前进度百分比 [0, 1] | Float |
| `progressColor(color)` | 进度颜色 | Color |
| `trackColor(color)` | 轨道颜色 | Color |
| `thumbColor(color)` | 滑块颜色 | Color |
| `trackThickness(thickness)` | 轨道厚度 | Float |
| `thumbSize(size)` | 滑块大小（高度和宽度） | Size |
| `sliderDirection(horizontal)` | 滑动方向，默认横向 | Boolean |
| `progressViewCreator { }` | 自定义进度 View | ViewCreator |
| `trackViewCreator { }` | 自定义轨道 View | ViewCreator |
| `thumbViewCreator { }` | 自定义滑块 View | ViewCreator |
| `enableGlassEffect(enabled)` | iOS 26+ 液态玻璃效果 | Boolean |

> 注：启用液态玻璃效果后，`progressViewCreator`、`trackViewCreator`、`thumbViewCreator` 等自定义属性将不再生效。

**事件：**

| 事件 | 描述 | 回调参数 |
|-----|------|---------|
| `progressDidChanged { }` | 进度变化 | Float |
| `beginDragSlider { }` | 开始拖动 | PanGestureParams |
| `endDragSlider { }` | 结束拖动 | PanGestureParams |
