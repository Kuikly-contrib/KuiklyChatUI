# Tabs 标签栏

选项卡切换组件，与 PageList 配套使用。继承自 `List`。

```kotlin
import com.tencent.kuikly.core.views.Tabs
import com.tencent.kuikly.core.views.TabItem
```

**基本用法：**

```kotlin
Tabs {
    attr {
        height(44f)
        scrollParams(ctx.scrollParams) // 必须设置，来自 PageList 的 scroll 事件
        defaultInitIndex(0)
        indicatorAlignCenter()
        
        // 指示条
        indicatorInTabItem {
            View {
                attr {
                    height(3f)
                    width(20f)
                    backgroundColor(Color.BLUE)
                }
            }
        }
    }
    
    // Tab 项
    vfor({ tabs }) { tab, index ->
        TabItem { state ->
            Text {
                attr {
                    text(tab.title)
                    padding(horizontal = 16f)
                    // 根据 state.selected 设置选中样式
                    color(if (state.selected) Color.BLUE else Color.BLACK)
                }
            }
        }
    }
}
```

**属性 API：**

| 属性方法 | 描述 | 参数类型 |
|---------|------|---------|
| `scrollParams(params)` | 滚动信息（必须设置），来自 PageList 等 Scroller 容器组件的 scroll 事件 | ScrollParams |
| `defaultInitIndex(index)` | 首次默认初始化的 tabs 对应 index | Int |
| `indicatorInTabItem { }` | 生成可滚动的指示条，配合 scrollParams 同步滚动 | ViewCreator |
| `indicatorAlignCenter()` | 指示条居中滚动 | - |
| `indicatorAlignAspectRatio()` | 指示条按比例滚动（默认行为） | - |

**TabItem 组件：**

TabItem 是与 Tabs 配套使用的子组件，用于定义每个标签项。

```kotlin
TabItem { state ->
    // state.selected 表示当前是否选中
    Text {
        attr {
            text("标签")
            color(if (state.selected) Color.BLUE else Color.BLACK)
        }
    }
}
```

**TabItemView.ItemState：**

| 属性 | 描述 | 类型 |
|-----|------|-----|
| `selected` | 是否选中（响应式变量，用于更新选中高亮 UI） | Boolean |
