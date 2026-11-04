# View 容器

最基础的 UI 组件，可嵌套使用，用于组织子 View 布局。

```kotlin
import com.tencent.kuikly.core.views.View
```

**基本用法：**

```kotlin
View {
    attr {
        size(100f, 100f)
        backgroundColor(Color.GREEN)
        borderRadius(10f)
    }
    event {
        click { params ->
            // 处理点击
        }
    }
    // 子组件
    Text { attr { text("子文本") } }
}
```

**特有属性：**

| 属性方法 | 描述 | 参数类型 |
|---------|------|---------|
| `backgroundImage(src, imageAttr)` | 设置背景图片（默认 resize 为 cover） | String, ImageAttr? |
| `glassEffectIOS(style, tintColor, interactive, enable)` | iOS 26+ 液态玻璃效果 | GlassEffectStyle, Color?, Boolean, Boolean |
| `glassEffectContainerIOS(spacing)` | 液态玻璃容器效果，子元素间产生视觉融合 | Float |

**GlassEffectStyle 枚举值：**
- `REGULAR` - 标准液态玻璃效果
- `CLEAR` - 清透液态玻璃效果

**特有事件：**

| 事件 | 描述 | 回调参数 |
|-----|------|---------|
| `touchUp { }` | 触摸抬起 | TouchParams |
| `touchDown { }` | 触摸按下 | TouchParams |
| `touchMove { }` | 触摸移动 | TouchParams |
| `touchCancel { }` | 触摸取消 | TouchParams |

**TouchParams：**

| 参数 | 描述 | 类型 |
|-----|------|-----|
| `x` | 触摸点在自身 view 坐标系下的 X | Float |
| `y` | 触摸点在自身 view 坐标系下的 Y | Float |
| `pageX` | 触摸点在根视图 Page 下的 X | Float |
| `pageY` | 触摸点在根视图 Page 下的 Y | Float |
| `pointerId` | 触摸点对应的 ID | Int |
| `action` | 事件类型 | String |
| `touches` | 多指触摸信息数组 | List\<Touch\> |

**方法：**

| 方法 | 描述 |
|-----|------|
| `bringToFront()` | 将组件置顶到父组件最高层级 |
