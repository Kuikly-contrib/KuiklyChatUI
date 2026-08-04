# TextArea 多行输入框

多行输入框组件。

```kotlin
import com.tencent.kuikly.core.views.TextArea
import com.tencent.kuikly.core.views.InputSpans
import com.tencent.kuikly.core.views.InputSpan
```

**基本用法：**

```kotlin
TextArea {
    ref { textAreaRef = it }
    attr {
        size(300f, 150f)
        placeholder("请输入内容...")
        fontSize(16f)
        lineHeight(20f)
        color(Color.BLACK)
    }
    event {
        textDidChange { params ->
            val text = params.text
        }
    }
}
```

**属性 API：**

| 属性方法 | 描述 | 参数类型 |
|---------|------|---------|
| `text(text)` | 设置文本 | String |
| `fontSize(size)` | 字体大小 | Float |
| `fontSize(size, scaleFontSizeEnable)` | 字体大小（可控制是否跟随系统缩放） | Float, Boolean? |
| `lines(lines)` | 最大行数 | Int |
| `lineHeight(lineHeight)` | 行高 | Float |
| `color(color)` | 文本颜色 | Color |
| `tintColor(color)` | 光标颜色 | Color |
| `placeholder(placeholder)` | 提示文本 | String |
| `placeholderColor(color)` | 提示文本颜色 | Color |
| `maxTextLength(length)` | 最大字符长度 | Int |
| `autofocus(focus)` | 是否自动获取焦点 | Boolean |
| `editable(editable)` | 是否可编辑 | Boolean |
| `fontWeightNormal()` | 正常字重 (400) | - |
| `fontWeightMedium()` | 中等字重 (500) | - |
| `fontWeightBold()` | 粗体字重 (700) | - |
| `textAlignLeft()` | 左对齐 | - |
| `textAlignCenter()` | 居中对齐 | - |
| `textAlignRight()` | 右对齐 | - |
| `keyboardTypePassword()` | 密码键盘 | - |
| `keyboardTypeNumber()` | 数字键盘 | - |
| `keyboardTypeEmail()` | 邮件键盘 | - |
| `returnKeyTypeNone()` | 无按钮 | - |
| `returnKeyTypeSearch()` | 搜索按钮 | - |
| `returnKeyTypeSend()` | 发送按钮 | - |
| `returnKeyTypeDone()` | 完成按钮 | - |
| `returnKeyTypeNext()` | 下一步按钮 | - |
| `returnKeyTypeContinue()` | 继续按钮 | - |
| `returnKeyTypeGo()` | 前往按钮 | - |
| `returnKeyTypeGoogle()` | Google 按钮 | - |
| `returnKeyTypePrevious()` | 上一步按钮 | - |
| `useDpFontSizeDim(useDp)` | 是否使用 dp 作为字体单位（Android） | Boolean |
| `enablesReturnKeyAutomatically(flag)` | 输入框为空时禁用 Return 键（仅 iOS） | Boolean |
| `enablePinyinCallback(enable)` | 是否启用拼音输入回调 | Boolean |
| `inputSpans(spans)` | 富文本样式，配合 textDidChange 实现输入框富文本化 | InputSpans |

**InputSpans 用法：**

```kotlin
val spans = InputSpans()
    .addSpan(InputSpan().text("红色").color(Color.RED).fontSize(16f))
    .addSpan(InputSpan().text("蓝色").color(Color.BLUE).fontWeightBold())

attr {
    inputSpans(spans)
}
```

**InputSpan 方法：**

| 方法 | 描述 | 参数类型 |
|-----|------|---------|
| `text(text)` | 文本内容 | String |
| `fontSize(size)` | 字体大小 | Float |
| `color(color)` | 文本颜色 | Color |
| `fontWeightNormal()` | 正常字重 | - |
| `fontWeightMedium()` | 中等字重 | - |
| `fontWeightBold()` | 粗体字重 | - |
| `lineHeight(lineHeight)` | 行高 | Float |

**事件：**

| 事件 | 描述 | 回调参数 |
|-----|------|---------|
| `textDidChange { }` | 文本变化 | InputParams (text: String) |
| `inputFocus { }` | 获取焦点 | InputParams |
| `inputBlur { }` | 失去焦点 | InputParams |
| `keyboardHeightChange { }` | 键盘高度变化 | KeyboardParams (height, duration) |
| `inputReturn { }` | Return 键 | InputParams (text, imeAction) |
| `textLengthBeyondLimit { }` | 超出最大长度 | - |

**方法：**

| 方法 | 描述 | 参数 |
|-----|------|-----|
| `focus()` | 获取焦点，软键盘会自动弹起 | - |
| `blur()` | 失去焦点，软键盘会自动收起 | - |
| `cursorIndex { }` | 获取光标位置 | (Int) -> Unit |
| `setCursorIndex(index)` | 设置光标位置 | Int |
