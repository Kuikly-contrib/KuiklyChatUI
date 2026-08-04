# Column 垂直布局

垂直排列子元素的容器组件。

```kotlin
import com.tencent.kuikly.core.views.layout.Column
import com.tencent.kuikly.core.layout.FlexAlign
```

**基本用法：**

```kotlin
Column {
    attr {
        width(100f)
    }
    
    Text { attr { text("上") } }
    Text { attr { text("中") } }
    Text { attr { text("下") } }
}

// 指定对齐方式
Column {
    attr {
        alignItems(FlexAlign.CENTER)
    }
    // 子元素水平居中
}
```

> Column 组件自动设置 `flexDirectionColumn()`，支持所有布局属性，详见[基础属性和事件](base-properties-events.md)。
