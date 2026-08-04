# Image 图片

用于展示图片的组件。

```kotlin
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.base.attr.ImageUri
```

**基本用法：**

```kotlin
Image {
    attr {
        size(240f, 180f)
        src("https://example.com/image.jpg")
        resizeCover()
    }
}
```

**属性 API：**

| 属性方法 | 描述 | 参数类型 |
|---------|------|---------|
| `src(src, isDotNineImage)` | 图片源 | String / ImageUri, Boolean |
| `placeholderSrc(placeholder)` | 占位图 | String |
| `resizeCover()` | 等比缩放覆盖 | - |
| `resizeContain()` | 等比缩放包含 | - |
| `resizeStretch()` | 拉伸填充 | - |
| `blurRadius(radius)` | 高斯模糊半径 | Float |
| `tintColor(color)` | 染色 | Color |
| `maskLinearGradient(direction, colorStops)` | 渐变遮罩 | Direction, ColorStop... |
| `capInsets(top, left, bottom, right)` | 拉伸区域 | Float... |

**ImageUri 工具类：**

```kotlin
ImageUri.commonAssets("path")  // 公共资源
ImageUri.pageAssets("path")    // 页面资源
ImageUri.file("path")          // 本地文件
```

**事件：**

| 事件 | 描述 | 回调参数 |
|-----|------|---------|
| `loadSuccess { }` | 加载成功 | LoadSuccessParams |
| `loadFailure { }` | 加载失败 | LoadFailureParams |
| `loadResolution { }` | 获取分辨率成功 | LoadResolutionParams |
