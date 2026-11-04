# PAG 动画

PAG 动画播放组件（类 Lottie）。

```kotlin
import com.tencent.kuikly.core.views.PAG
import com.tencent.kuikly.core.views.PAGScaleMode
import com.tencent.kuikly.core.base.attr.ImageUri
```

**基本用法：**

```kotlin
PAG {
    ref { pagRef = it }
    attr {
        size(pagerData.pageViewWidth, 200f)
        src(ImageUri.pageAssets("animation.pag"))
        repeatCount(0)
        scaleMode(PAGScaleMode.LETTER_BOX)
        autoPlay(true)
    }
    event {
        animationStart { }
        animationEnd { }
        animationRepeat { }
    }
}
```

**属性 API：**

| 属性方法 | 描述 | 参数类型 |
|---------|------|---------|
| `src(src)` | 源文件路径（支持 CDN URL 或本地文件路径） | String |
| `src(uri)` | Assets 资源文件路径 | ImageUri |
| `repeatCount(count)` | 重复次数（0 为无限，默认为 1） | Int |
| `scaleMode(mode)` | 缩放模式 | PAGScaleMode |
| `scaleModeNone()` | 不缩放，使用原始大小 | - |
| `scaleModeStretch()` | 拉伸填充，不保持宽高比 | - |
| `scaleModeLetterBox()` | 等比完整显示（默认） | - |
| `scaleModeZoom()` | 等比填满，可能裁剪 | - |
| `autoPlay(play)` | 是否自动播放（默认为 true） | Boolean |
| `replaceTextLayerContent(layerName, textContent)` | 替换文字图层内容 | String, String |
| `replaceImageLayerContent(layerName, imageFilePath)` | 替换图像图层内容 | String, String |
| `replaceImageLayerContent(layerName, uri)` | 替换图像图层内容（Assets 资源） | String, ImageUri |

**PAGScaleMode：**

| 值 | 描述 |
|----|------|
| `NONE` | 不缩放 |
| `STRETCH` | 拉伸填充 |
| `LETTER_BOX` | 等比完整显示（默认） |
| `ZOOM` | 等比填满 |

**事件：**

| 事件 | 描述 |
|-----|------|
| `loadFailure { }` | 加载失败 |
| `animationStart { }` | 动画开始 |
| `animationEnd { }` | 动画结束 |
| `animationCancel { }` | 动画取消 |
| `animationRepeat { }` | 动画重复 |

**方法：**

| 方法 | 描述 | 参数 |
|-----|------|-----|
| `play()` | 播放动画 | - |
| `stop()` | 停止动画 | - |
| `setProgress(value)` | 设置播放进度 [0.0, 1.0] | Float |
