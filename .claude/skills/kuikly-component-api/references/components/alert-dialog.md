# AlertDialog 提示对话框

提示对话框组件，UI 风格对齐 iOS UIAlertController 风格，并支持自定义弹窗 UI。

```kotlin
import com.tencent.kuikly.core.views.AlertDialog
```

**基本用法：**

```kotlin
AlertDialog {
    attr {
        showAlert(ctx.showAlert)
        title("提示")
        message("确定要删除吗？")
        actionButtons("取消", "确定")
        inWindow(true)
    }
    event {
        clickActionButton { index ->
            ctx.showAlert = false
        }
        willDismiss {
            ctx.showAlert = false
        }
    }
}
```

**属性 API：**

| 属性方法 | 描述 | 参数类型 |
|---------|------|---------|
| `showAlert(show)` | 控制 Alert 是否显示，不显示时不占用布局（必须设置） | Boolean |
| `title(title)` | Alert 标题（当 message 不为空时可选） | String |
| `message(message)` | Alert 内容（当 title 不为空时可选） | String |
| `actionButtons(vararg titles)` | Alert 点击的按钮（必须设置） | String... |
| `actionButtonsCustomAttr(vararg attrs)` | 按钮自定义文字样式 | TextAttr... |
| `customContentView { }` | 自定义前景 View UI（代替整个白色块区域，居中显示） | ViewCreator |
| `customBackgroundView { }` | 自定义背景 View UI（代替整个背景黑色蒙层，需设置全屏尺寸） | ViewCreator |
| `inWindow(window)` | 是否全屏显示，默认 `false` | Boolean |

**事件：**

| 事件 | 描述 | 回调参数 |
|-----|------|---------|
| `clickActionButton { }` | 按钮点击，index 值和 actionButtons 传入 button 的下标一致 | Int |
| `clickBackgroundMask { }` | 背景蒙层点击 | ClickParams |
| `willDismiss { }` | 系统返回事件（back 按钮或右滑）回调 | - |
| `alertDidExit { }` | Alert 完全退出（不显示&动画结束）回调 | - |
