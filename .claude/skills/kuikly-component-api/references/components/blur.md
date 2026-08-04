# Blur 高斯模糊

高斯模糊（毛玻璃）组件，盖住其他 View 可进行动态高斯模糊布局位置下方的视图。

```kotlin
import com.tencent.kuikly.core.views.Blur
```

**基本用法：**

```kotlin
// 背景图片
Image {
    attr {
        absolutePosition(0f, 0f, 0f, 0f)
        src("https://example.com/image.jpg")
    }
}

// 模糊效果覆盖在图片上
Blur {
    attr {
        size(pagerData.pageViewWidth, 100f)
        blurRadius(10f)
    }
}
```

**属性 API：**

| 属性方法 | 描述 | 参数类型 |
|---------|------|---------|
| `blurRadius(radius)` | 高斯模糊半径，最大 12.5f，默认 10f | Float |
| `targetBlurViewNativeRefs(refs)` | 想要模糊的 View 的 nativeRef 列表，用于提高 Android 平台上的模糊性能 | List\<Int\> |
| `blurOtherLayer(blur)` | 是否模糊其他单独的 layer（仅 Android，用于开启模糊 TextureView）。注：如果设置了 targetBlurViewNativeRefs 则此属性无效 | Boolean |
