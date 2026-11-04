# Modal 模态窗口

自定义模态窗口组件，用于在当前页面上显示一个浮动窗口。当模态窗口显示时，用户无法与背景页面进行交互，只能与模态窗口内的内容进行交互。

```kotlin
import com.tencent.kuikly.core.views.Modal
import com.tencent.kuikly.core.views.ModalDismissReason
```

**基本用法：**

```kotlin
Modal(inWindow = true) {
    attr {
        absolutePosition(0f, 0f, 0f, 0f)
        backgroundColor(Color(0x80000000))
    }
    event {
        willDismiss { reason ->
            // 处理系统 back 按钮事件
            when (reason) {
                ModalDismissReason.BackPressed -> { /* 返回键被按下 */ }
            }
        }
    }
    
    View {
        attr {
            size(300f, 200f)
            backgroundColor(Color.WHITE)
            borderRadius(10f)
        }
        // 弹窗内容
    }
}
```

**构造参数：**

| 参数 | 描述 | 类型 | 默认值 |
|-----|------|-----|-------|
| `inWindow` | 是否窗口顶级（`true` 表示和屏幕等大，`false` 表示和页面一样大） | Boolean | false |

**事件：**

| 事件 | 描述 | 回调参数 |
|-----|------|---------|
| `willDismiss { }` | 系统 back 按钮事件回调 | ModalDismissReason |

**ModalDismissReason：**

| 值 | 描述 |
|----|------|
| `BackPressed` | 返回键被按下 |
