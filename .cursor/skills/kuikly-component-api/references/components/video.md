# Video 视频

视频播放器组件。

> 注意：播放器依赖宿主的能力，需要在各端实现适配器。

```kotlin
import com.tencent.kuikly.core.views.Video
import com.tencent.kuikly.core.views.VideoPlayControl
```

**基本用法：**

```kotlin
Video {
    ref { videoRef = it }
    attr {
        size(pagerData.pageViewWidth, 200f)
        src("https://example.com/video.mp4")
        playControl(VideoPlayControl.PLAY)
        resizeModeToCover()
        muted(false)
        rate(1.0f)
    }
    event {
        playStateDidChanged { state, extInfo ->
            // 播放状态变化
        }
        playTimeDidChanged { curTime, totalTime ->
            // 播放时间变化（毫秒）
        }
        firstFrameDidDisplay {
            // 首帧显示（一般用该时机隐藏封面）
        }
    }
}
```

**属性 API：**

| 属性方法 | 描述 | 参数类型 |
|---------|------|---------|
| `src(src)` | 视频源 URL | String |
| `playControl(control)` | 播放控制 | VideoPlayControl |
| `resizeModeToCover()` | 等比例撑满 | - |
| `resizeModeToContain()` | 等比例不裁剪，保留黑边 | - |
| `resizeModeToStretch()` | 缩放撑满（非等比例，会变形） | - |
| `muted(muted)` | 是否静音 | Boolean |
| `rate(rate)` | 倍速（1.0, 1.25, 1.5, 2.0） | Float |

**VideoPlayControl：**

| 值 | 描述 |
|----|------|
| `PREPLAY` | 预播放视频到第一帧，用于预加载优化 |
| `PLAY` | 播放 |
| `PAUSE` | 暂停 |
| `STOP` | 停止 |

**PlayState：**

| 值 | 描述 |
|----|------|
| `NONE` | 无状态 |
| `PLAYING` | 播放中 |
| `BUFFERING` | 缓冲中 |
| `PAUSED` | 已暂停 |
| `PLAY_END` | 播放结束 |
| `ERROR` | 播放错误 |

**事件：**

| 事件 | 描述 | 回调参数 |
|-----|------|---------|
| `playStateDidChanged { }` | 状态变化 | PlayState, JSONObject |
| `playTimeDidChanged { }` | 时间变化（毫秒） | Int (当前时间), Int (总时间) |
| `firstFrameDidDisplay { }` | 首帧显示 | - |
