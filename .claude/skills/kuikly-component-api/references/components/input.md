# Input 单行输入框

单行输入框组件。

```kotlin
import com.tencent.kuikly.core.views.Input
```

**基本用法：**

```kotlin
Input {
    ref { inputRef = it }
    attr {
        size(200f, 40f)
        placeholder("请输入")
        placeholderColor(Color.GRAY)
        fontSize(16f)
        color(Color.BLACK)
        tintColor(Color.BLUE)
    }
    event {
        textDidChange { params ->
            val text = params.text
        }
        inputFocus { }
        inputBlur { }
    }
}
```

**属性 API：**

| 属性方法 | 描述 | 参数类型 |
|---------|------|---------|
| `text(text)` | 设置文本 | String |
| `fontSize(size)` | 字体大小 | Float |
| `color(color)` | 文本颜色 | Color / Long |
| `placeholder(placeholder)` | 提示文本 | String |
| `placeholderColor(color)` | 提示文本颜色 | Color / Long |
| `tintColor(color)` | 光标颜色 | Color / Long |
| `maxTextLength(length)` | 最大字符长度 | Int |
| `autofocus(focus)` | 是否自动获取焦点，获取焦点后会触发软键盘弹起 | Boolean |
| `editable(editable)` | 是否可编辑 | Boolean |
| `keyboardTypePassword()` | 密码键盘 | - |
| `keyboardTypeNumber()` | 数字键盘 | - |
| `keyboardTypeEmail()` | 邮件键盘 | - |
| `returnKeyTypeSearch()` | 搜索按钮 | - |
| `returnKeyTypeSend()` | 发送按钮 | - |
| `returnKeyTypeDone()` | 完成按钮 | - |
| `returnKeyTypeNext()` | 下一步按钮 | - |
| `returnKeyTypeGo()` | 前往按钮 | - |
| `textAlignLeft()` | 左对齐 | - |
| `textAlignCenter()` | 居中对齐 | - |
| `textAlignRight()` | 右对齐 | - |
| `inputSpans(spans)` | 富文本样式，配合 textDidChange 实现输入框富文本化 | InputSpans |

**事件：**

| 事件 | 描述 | 回调参数 |
|-----|------|---------|
| `textDidChange { }` | 文本变化 | InputParams (text: String) |
| `inputFocus { }` | 获取焦点 | InputParams |
| `inputBlur { }` | 失去焦点 | InputParams |
| `keyboardHeightChange { }` | 键盘高度变化 | KeyboardParams (height, duration) |
| `onTextReturn { }` | Return 键 | InputParams |
| `textLengthBeyondLimit { }` | 超出最大长度 | InputParams |

**方法：**

| 方法 | 描述 | 参数 |
|-----|------|-----|
| `setText(text)` | 设置文本 | String |
| `focus()` | 获取焦点，软键盘会自动弹起 | - |
| `blur()` | 失去焦点，软键盘会自动收起 | - |
| `cursorIndex { }` | 获取光标位置 | (Int) -> Unit |
| `setCursorIndex(index)` | 设置光标位置 | Int |
