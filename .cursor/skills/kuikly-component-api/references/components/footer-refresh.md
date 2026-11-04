# FooterRefresh 上拉加载

用于在 List 中实现上拉加载更多。

```kotlin
import com.tencent.kuikly.core.views.FooterRefresh
import com.tencent.kuikly.core.views.FooterRefreshState
import com.tencent.kuikly.core.views.FooterRefreshEndState
```

**基本用法：**

```kotlin
List {
    // 列表内容
    
    FooterRefresh {
        ref { footerRef = it }
        attr {
            height(50f)
            preloadDistance(100f)
        }
        event {
            refreshStateDidChange { state ->
                when (state) {
                    FooterRefreshState.REFRESHING -> loadMore()
                    else -> {}
                }
            }
        }
        
        Text { attr { text("加载中...") } }
    }
}
```

**属性 API：**

| 属性方法 | 描述 | 参数类型 |
|---------|------|---------|
| `preloadDistance(distance)` | 触发加载时距离底部的距离 | Float |
| `minContentSize(width, height)` | 最小内容尺寸 | Float, Float |
| `refreshState(state)` | 刷新状态 | FooterRefreshState |

**FooterRefreshState：**

| 值 | 描述 |
|----|------|
| `IDLE` | 普通闲置状态 |
| `REFRESHING` | 正在刷新中的状态 |
| `NONE_MORE_DATA` | 无更多数据状态（不会再触发刷新） |
| `FAILURE` | 失败状态（一般展示点击重试 UI） |

**事件：**

| 事件 | 描述 | 回调参数 |
|-----|------|---------|
| `refreshStateDidChange { }` | 状态变化 | FooterRefreshState |

**方法：**

| 方法 | 描述 | 参数 |
|-----|------|-----|
| `beginRefresh()` | 手动开始刷新 | - |
| `endRefresh(endState)` | 结束刷新并设置结束状态 | FooterRefreshEndState |
| `resetRefreshState(state)` | 重置刷新状态 | FooterRefreshEndState |
