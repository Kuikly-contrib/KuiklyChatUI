# APNG 动画

APNG 动画播放组件。

```kotlin
import com.tencent.kuikly.core.views.APNG
```

**基本用法：**

```kotlin
APNG {
    ref { apngRef = it }
    attr {
        size(200f, 200f)
        src("https://example.com/animation.png")
        repeatCount(0) // 0 表示无限循环
        autoPlay(true)
    }
    event {
        animationStart { }
        animationEnd { }
        loadFailure { }
    }
}
```

**属性 API：**

| 属性方法 | 描述 | 参数类型 |
|---------|------|---------|
| `src(src)` | 源文件路径（支持 CDN URL 或本地文件路径） | String |
| `repeatCount(count)` | 重复次数（0 为无限，默认为 0） | Int |
| `autoPlay(play)` | 是否自动播放（默认为 true） | Boolean |

**事件：**

| 事件 | 描述 |
|-----|------|
| `loadFailure { }` | 加载失败 |
| `animationStart { }` | 动画开始 |
| `animationEnd { }` | 动画结束 |

**方法：**

| 方法 | 描述 |
|-----|------|
| `play()` | 播放动画（在 attr.autoPlay 为 true 时不需要手动调用） |
| `stop()` | 停止动画 |
