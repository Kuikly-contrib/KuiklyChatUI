# Refresh 下拉刷新

用于在 List 中实现下拉刷新。

```kotlin
import com.tencent.kuikly.core.views.Refresh
import com.tencent.kuikly.core.views.RefreshViewState
```

**基本用法：**

```kotlin
List {
    attr { flex(1f) }
    
    Refresh {
        ref { refreshRef = it }
        attr {
            height(60f)
            refreshEnable = true
        }
        event {
            refreshStateDidChange { state ->
                when (state) {
                    RefreshViewState.REFRESHING -> loadData()
                    else -> {}
                }
            }
        }
        
        // 自定义刷新视图
        ActivityIndicator { }
    }
    
    // 列表内容
}
```

**属性 API：**

| 属性方法                           | 描述                 | 参数类型 |
|--------------------------------|--------------------|---------|
| `refreshEnable = enable`       | 是否启用下拉刷新，默认 `true` | Boolean |
| `refreshState(state)`          | 刷新状态               | RefreshViewState |
| `contentInsetWhenEndDrag(top)` | 松手后边距              | Float |

**RefreshViewState：**

| 值 | 描述 |
|----|------|
| `IDLE` | 空闲状态，即未下拉 |
| `PULLING` | 松开就可以进行刷新的状态 |
| `REFRESHING` | 正在刷新中的状态 |

**事件：**

| 事件 | 描述 | 回调参数 |
|-----|------|---------|
| `refreshStateDidChange { }` | 状态变化 | RefreshViewState |
| `pullingPercentageChanged { }` | 下拉百分比变化 [0, 1] | Float |

**方法：**

| 方法 | 描述 | 参数 |
|-----|------|-----|
| `beginRefresh(animated)` | 开始刷新，默认 `true` | Boolean |
| `endRefresh(animated)` | 结束刷新 | Boolean |
