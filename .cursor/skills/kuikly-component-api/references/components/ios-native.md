# iOS 原生组件

## iOSSwitch 原生开关

iOS 原生 UISwitch 组件，支持玻璃效果。

```kotlin
import com.tencent.kuikly.core.views.ios.iOSSwitch
```

**基本用法：**

```kotlin
iOSSwitch {
    attr {
        isOn(ctx.switchOn)
        onColor(Color(0xFF34C759))
        unOnColor(Color(0xFF8E8E93))
        thumbColor(Color.WHITE)
        enabled(true)
    }
    event {
        switchOnChanged { params ->
            ctx.switchOn = params.value
        }
    }
}
```

**属性 API：**

| 属性方法 | 描述 | 参数类型 |
|---------|------|---------|
| `isOn(isOn)` | 是否打开 | Boolean |
| `enabled(enabled)` | 是否启用 | Boolean |
| `onColor(color)` | 打开时背景色 | Color |
| `unOnColor(color)` | 关闭时背景色 | Color |
| `thumbColor(color)` | 滑块颜色 | Color |

**事件：**

| 事件 | 描述 | 回调参数 |
|-----|------|---------|
| `switchOnChanged { }` | 开关变化 | SwitchValueChangedParams |

---

## iOSSlider 原生滑块

iOS 原生 UISlider 组件，支持玻璃效果。

```kotlin
import com.tencent.kuikly.core.views.ios.iOSSlider
```

**基本用法：**

```kotlin
iOSSlider {
    attr {
        width(300f)
        height(30f)
        currentProgress(0.5f)
        minValue(0f)
        maxValue(1f)
        progressColor(Color.BLUE)
        trackColor(Color.GRAY)
        thumbColor(Color.WHITE)
        continuous(true)
    }
    event {
        onValueChanged { params ->
            // params.value
        }
        onTouchDown { params ->
            // 开始拖动
        }
        onTouchUp { params ->
            // 结束拖动
        }
    }
}
```

**属性 API：**

| 属性方法 | 描述 | 参数类型 |
|---------|------|---------|
| `currentProgress(value)` | 当前进度 [0, 1] | Float |
| `minValue(minValue)` | 最小值 | Float |
| `maxValue(maxValue)` | 最大值 | Float |
| `thumbColor(color)` | 滑块颜色 | Color |
| `trackColor(color)` | 轨道颜色 | Color |
| `progressColor(color)` | 进度颜色 | Color |
| `continuous(continuous)` | 是否连续发送事件 | Boolean |
| `trackThickness(thickness)` | 轨道厚度 | Float |
| `thumbSize(size)` | 滑块大小 | Size |
| `sliderDirection(isHorizontal)` | 滑动方向 | Boolean |

**事件：**

| 事件 | 描述 | 回调参数 |
|-----|------|---------|
| `onValueChanged { }` | 值变化 | SliderValueChangedParams |
| `onTouchDown { }` | 开始触摸 | SliderTouchParams |
| `onTouchUp { }` | 结束触摸 | SliderTouchParams |

---

## SegmentedControlIOS 分段控制器

iOS 原生 UISegmentedControl 组件。

```kotlin
import com.tencent.kuikly.core.views.ios.SegmentedControlIOS
```

**基本用法：**

```kotlin
SegmentedControlIOS {
    attr {
        titles(listOf("选项1", "选项2", "选项3"))
        selectedIndex(0)
    }
    event {
        onValueChanged { params ->
            val index = params.index
        }
    }
}
```

**属性 API：**

| 属性方法 | 描述 | 参数类型 |
|---------|------|---------|
| `titles(titles)` | 分段标题列表 | List\<String\> |
| `selectedIndex(index)` | 选中索引 | Int |

**事件：**

| 事件 | 描述 | 回调参数 |
|-----|------|---------|
| `onValueChanged { }` | 选中变化 | ValueChangedParams |

---

## LiquidGlass 液态玻璃

iOS 26+ 液态玻璃效果组件。

```kotlin
import com.tencent.kuikly.core.views.LiquidGlass
import com.tencent.kuikly.core.views.GlassEffectStyle
```

**基本用法：**

```kotlin
LiquidGlass {
    attr {
        size(200f, 100f)
        glassEffectStyle(GlassEffectStyle.REGULAR)
        glassEffectTintColor(Color(0x80FFFFFF))
        glassEffectInteractive(true)
    }
    
    Text { attr { text("玻璃效果内容") } }
}
```

**属性 API：**

| 属性方法 | 描述 | 参数类型 |
|---------|------|---------|
| `glassEffectStyle(style)` | 玻璃效果样式 | GlassEffectStyle |
| `glassEffectTintColor(color)` | 玻璃效果染色 | Color |
| `glassEffectInteractive(isInteractive)` | 是否响应交互 | Boolean |

**GlassEffectStyle：**

| 值 | 描述 |
|----|------|
| `REGULAR` | 常规样式 |
| `CLEAR` | 透明样式 |

---

## GlassEffectContainer 玻璃效果容器

iOS 26+ 玻璃效果容器组件，用于组合多个玻璃效果元素。

```kotlin
import com.tencent.kuikly.core.views.GlassEffectContainer
```

**基本用法：**

```kotlin
GlassEffectContainer {
    attr {
        spacing(10f)
    }
    
    LiquidGlass { /* 玻璃元素1 */ }
    LiquidGlass { /* 玻璃元素2 */ }
}
```

**属性 API：**

| 属性方法 | 描述 | 参数类型 |
|---------|------|---------|
| `spacing(spacing)` | 元素间距 | Float |
