# Center 居中布局

使子元素居中的容器组件。

```kotlin
import com.tencent.kuikly.core.views.layout.Center
```

**基本用法：**

```kotlin
Center {
    attr {
        size(200f, 200f)
        backgroundColor(Color.GRAY)
    }
    
    Text { attr { text("居中内容") } }
}
```
