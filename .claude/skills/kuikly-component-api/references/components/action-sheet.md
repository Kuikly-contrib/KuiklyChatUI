# ActionSheet 操作表

操作表组件，用于提供一组可供用户选择的操作。UI 风格对齐 iOS UIActionSheet 风格，并支持自定义弹窗 UI（底部弹出内容场景）。

```kotlin
import com.tencent.kuikly.core.views.ActionSheet
```

**基本用法：**

```kotlin
ActionSheet {
    attr {
        showActionSheet(ctx.showSheet)
        descriptionOfActions("选择操作")
        actionButtons("取消", "拍照", "从相册选择")
        inWindow(true)
    }
    event {
        clickActionButton { index ->
            ctx.showSheet = false
        }
        clickBackgroundMask {
            ctx.showSheet = false
        }
        actionSheetDidExit {
            // ActionSheet 完全退出后的回调
        }
    }
}
```

**属性 API：**

| 属性方法 | 描述 | 参数类型 |
|---------|------|---------|
| `showActionSheet(show)` | 控制 ActionSheet 是否显示，不显示时不占用布局（必须设置） | Boolean |
| `descriptionOfActions(desc)` | 关于 ActionSheet 的简短描述 | String |
| `actionButtons(cancelTitle, vararg titles)` | ActionSheet 点击的按钮（必须设置），第一个参数为取消按钮标题 | String... |
| `actionButtonsCustomAttr(cancelAttr, vararg attrs)` | 按钮自定义文字样式 | TextAttr... |
| `customContentView { }` | 自定义前景 View UI（代替整个底部白色块区域） | ViewCreator |
| `customBackgroundView { }` | 自定义背景 View UI（代替整个背景黑色蒙层，需设置全屏尺寸） | ViewCreator |
| `inWindow(window)` | 是否全屏显示，默认 `false` | Boolean |

**事件：**

| 事件 | 描述 | 回调参数 |
|-----|------|---------|
| `clickActionButton { }` | 按钮点击，index 值和 actionButtons 传入 button 的下标一致（取消按钮为 0） | Int |
| `clickBackgroundMask { }` | 背景蒙层点击，用于自定义前景 UI 场景下点击背景关闭弹窗 | ClickParams |
| `actionSheetDidExit { }` | ActionSheet 完全退出（不显示&动画结束）回调 | - |
