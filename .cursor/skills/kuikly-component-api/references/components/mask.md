# Mask 遮罩

遮罩视图组件。

```kotlin
import com.tencent.kuikly.core.views.Mask
import com.tencent.kuikly.core.base.attr.ImageUri
```

**基本用法：**

```kotlin
Mask(
    maskFromView = {
        Image {
            attr {
                size(300f, 300f)
                src(ImageUri.pageAssets("mask.png"))
            }
        }
    },
    maskToView = {
        Image {
            attr {
                size(300f, 300f)
                src("https://example.com/image.jpg")
            }
        }
    }
)
```

**构造参数：**

| 参数 | 描述 | 类型 |
|-----|------|-----|
| `maskFromView` | 遮罩源视图 | ViewCreator |
| `maskToView` | 目标视图 | ViewCreator |
