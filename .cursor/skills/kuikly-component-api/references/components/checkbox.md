# CheckBox 复选框

复选框组件，可用作单击选中态/非选中态的切换展示。

```kotlin
import com.tencent.kuikly.core.views.CheckBox
```

**基本用法：**

```kotlin
CheckBox {
    attr {
        size(30f, 30f)
        checked(ctx.isChecked)
        defaultImageSrc("unchecked.png")
        checkedImageSrc("checked.png")
    }
    event {
        checkedDidChanged { checked ->
            ctx.isChecked = checked
        }
    }
}
```

**属性 API：**

| 属性方法 | 描述 | 参数类型 |
|---------|------|---------|
| `checked(value)` | 是否选中 | Boolean |
| `disable(value)` | 是否禁用 | Boolean |
| `checkedImageSrc(src)` | 选中状态的图片资源路径 | String |
| `defaultImageSrc(src)` | 默认状态的图片资源路径 | String |
| `disableImageSrc(src)` | 禁用状态的图片资源路径 | String |
| `checkedViewCreator { }` | 选中状态的视图创建器 | ViewCreator |
| `defaultViewCreator { }` | 默认状态的视图创建器 | ViewCreator |
| `disableViewCreator { }` | 禁用状态的视图创建器 | ViewCreator |

**事件：**

| 事件 | 描述 | 回调参数 |
|-----|------|---------|
| `checkedDidChanged { }` | 选中状态变化 | Boolean |
