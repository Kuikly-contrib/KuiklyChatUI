# ActivityIndicator 加载指示器

旋转菊花样式的加载指示器。iOS 系统限制默认 `size(20f, 20f)`，多端统一固定尺寸为 20f。

```kotlin
import com.tencent.kuikly.core.views.ActivityIndicator
```

**基本用法：**

```kotlin
// 默认白色菊花
ActivityIndicator { }

// 灰色菊花，放大 1.5 倍
ActivityIndicator {
    attr {
        isGrayStyle(true)
        transform(Scale(1.5f, 1.5f))
    }
}
```

**属性 API：**

| 属性方法 | 描述 | 参数类型 |
|---------|------|---------|
| `isGrayStyle(isGray)` | 是否灰色样式（默认白色），该属性无法动态修改 | Boolean |

> 注：固定尺寸为 20x20，可通过 `transform(Scale())` 调整大小。
