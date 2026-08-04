# Row 水平布局

水平排列子元素的容器组件。

```kotlin
import com.tencent.kuikly.core.views.layout.Row
import com.tencent.kuikly.core.layout.FlexAlign
```

**基本用法：**

```kotlin
Row {
    attr {
        width(300f)
        height(50f)
    }
    
    Text { attr { text("左") } }
    Text { attr { text("中") } }
    Text { attr { text("右") } }
}


// 指定对齐方式
Row {
    attr {
        alignItems(FlexAlign.CENTER)
    }
    // 子元素垂直居中
}
```

> Row 组件自动设置 `flexDirectionRow()`，支持所有布局属性，详见[基础属性和事件](base-properties-events.md)。
