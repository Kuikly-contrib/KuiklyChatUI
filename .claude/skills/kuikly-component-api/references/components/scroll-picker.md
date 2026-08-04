# ScrollPicker 滚动选择器

滚动选择器组件，可组合用作日期或地区选择器。

```kotlin
import com.tencent.kuikly.core.views.ScrollPicker
```

**基本用法：**

```kotlin
ScrollPicker(arrayOf("A", "B", "C", "D", "E"), defaultIndex = 0) {
    attr {
        itemWidth = 100f
        itemHeight = 40f
        countPerScreen = 5
        itemBackGroundColor = Color.WHITE
        itemTextColor = Color.BLACK
    }
    event {
        scrollEndEvent { value, index ->
            // 选中 value, index
        }
        scrollEvent { value, index ->
            // 滚动过程中的选中项
        }
    }
}
```

**构造参数：**

| 参数 | 描述 | 类型 |
|-----|------|-----|
| `itemList` | 选项列表 | Array\<String\> |
| `defaultIndex` | 默认选中索引 | Int? |

**属性：**

| 属性 | 描述 | 类型 |
|-----|------|-----|
| `itemWidth` | 选项宽度 | Float |
| `itemHeight` | 选项高度 | Float |
| `countPerScreen` | 每屏选项数 | Int |
| `itemBackGroundColor` | 选项背景色 | Color |
| `itemTextColor` | 选项文字色 | Color |

**事件：**

| 事件 | 描述 | 回调参数 |
|-----|------|---------|
| `scrollEndEvent { }` | 停止滚动后选中 item | String (value), Int (index) |
| `scrollEvent { }` | 滚动过程中选中 item | String (value), Int (index) |
| `dragEndEvent { }` | （已废弃）停止拖拽后选中 item，请使用 scrollEndEvent | String, Int |
