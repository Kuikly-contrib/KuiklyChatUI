# Switch 开关

风格类 iOS 的开关组件，支持属性定制自定义开关样式。

```kotlin
import com.tencent.kuikly.core.views.Switch
```

**基本用法：**

```kotlin
Switch {
    attr {
        isOn(ctx.switchOn)
        onColor(Color(0xFF34C759))
        unOnColor(Color(0xFF8E8E93))
        thumbColor(Color.WHITE)
    }
    event {
        switchOnChanged { on ->
            ctx.switchOn = on
        }
    }
}
```

**属性 API：**

| 属性方法 | 描述 | 参数类型 |
|---------|------|---------|
| `isOn(on)` | 是否打开 | Boolean |
| `onColor(color)` | 打开时背景色 | Color |
| `unOnColor(color)` | 关闭时背景色 | Color |
| `thumbColor(color)` | 滑块颜色（关闭和开启同一个颜色） | Color |
| `thumbMargin(margin)` | 圆块与开关的贴边边距，默认 2f | Float |
| `enableGlassEffect(enabled)` | iOS 26+ 液态玻璃效果 | Boolean |

> 注：启用液态玻璃效果后，`thumbMargin` 等自定义属性将不再生效。

**事件：**

| 事件 | 描述 | 回调参数 |
|-----|------|---------|
| `switchOnChanged { }` | 开关变化 | Boolean |
