# SafeArea 安全区域

自动应用安全区域内边距的容器组件，避免内容被刘海、圆角等遮挡。

```kotlin
import com.tencent.kuikly.core.views.SafeArea
```

**基本用法：**

```kotlin
SafeArea {
    attr {
        flex(1f)
        backgroundColor(Color.WHITE)
    }
    
    // 页面内容
    Text { attr { text("安全区域内的内容") } }
}
```

> 注：SafeArea 会自动添加 `padding(top = safeAreaInsets.top, left = safeAreaInsets.left, bottom = safeAreaInsets.bottom, right = safeAreaInsets.right)` 内边距。
