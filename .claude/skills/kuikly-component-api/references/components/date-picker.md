# DatePicker 日期选择器

基于 `Scroller` 实现的日期选择器组件。

```kotlin
import com.tencent.kuikly.core.views.DatePicker
```

**基本用法：**

```kotlin
DatePicker {
    attr {
        width(300f)
        backgroundColor(Color.WHITE)
    }
    event {
        chooseEvent { dateInfo ->
            val year = dateInfo.date?.year
            val month = dateInfo.date?.month
            val day = dateInfo.date?.day
            val timestamp = dateInfo.timeInMillis
        }
    }
}
```

**事件：**

| 事件 | 描述 | 回调参数 |
|-----|------|---------|
| `chooseEvent { }` | 选择日期 | DatePickerDate |

**DatePickerDate：**

| 参数 | 描述 | 类型 |
|-----|------|-----|
| `timeInMillis` | 当前选择日期的时间戳（毫秒） | Long |
| `date` | 日期对象 | Date? |

**Date：**

| 参数 | 描述 | 类型 |
|-----|------|-----|
| `year` | 当前选择日期的年 | Int |
| `month` | 当前选择日期的月 | Int |
| `day` | 当前选择日期的日 | Int |
