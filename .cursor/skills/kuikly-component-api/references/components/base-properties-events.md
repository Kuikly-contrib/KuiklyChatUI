# 基础属性和事件

所有组件都支持以下基础属性和事件。

## 样式属性

| 属性方法 | 描述 | 参数类型 |
|---------|------|---------|
| `backgroundColor(color)` | 背景颜色 | Color |
| `backgroundLinearGradient(direction, vararg colorStops)` | 线性渐变背景 | Direction, ColorStop... |
| `boxShadow(shadow)` | 阴影效果 | BoxShadow |
| `borderRadius(radius)` | 圆角 | Float / BorderRectRadius |
| `border(border)` | 边框 | Border |
| `visibility(visible)` | 可见性 | Boolean |
| `opacity(alpha)` | 透明度 | Float |
| `touchEnable(enable)` | 是否可触摸 | Boolean |
| `transform(rotate, scale, translate, anchor)` | 变换 | Rotate?, Scale?, Translate?, Anchor? |
| `zIndex(index, useOutline)` | 层级 | Int, Boolean |
| `overflow(clip)` | 溢出裁剪 | Boolean |
| `clipPath(builder)` | 路径裁剪 | ClipPathBuilder? |
| `keepAlive(alive)` | 是否常驻 | Boolean |
| `animation(animation)` | 动画参数 | Animation |
| `accessibility(text)` | 无障碍文本 | String |
| `debugName(name)` | 调试名称 | String |
| `autoDarkEnable(enable)` | 自动暗黑模式 | Boolean |

## 布局属性
| 属性方法 | 描述 | 参数类型 |
|---------|------|---------|
| `width(w)` | 宽度 | Float |
| `height(h)` | 高度 | Float |
| `size(w, h)` | 尺寸 | Float, Float |
| `maxWidth(w)` | 最大宽度 | Float |
| `maxHeight(h)` | 最大高度 | Float |
| `minWidth(w)` | 最小宽度 | Float |
| `minHeight(h)` | 最小高度 | Float |
| `margin(top, left, bottom, right)` | 外边距 | Float... |
| `marginTop(m)` | 上外边距 | Float |
| `marginLeft(m)` | 左外边距 | Float |
| `marginBottom(m)` | 下外边距 | Float |
| `marginRight(m)` | 右外边距 | Float |
| `padding(top, left, bottom, right)` | 内边距 | Float... |
| `paddingTop(p)` | 上内边距 | Float |
| `paddingLeft(p)` | 左内边距 | Float |
| `paddingBottom(p)` | 下内边距 | Float |
| `paddingRight(p)` | 右内边距 | Float |
| `flexDirection(direction)` | 主轴方向 | FlexDirection |
| `flexDirectionRow()` | 主轴方向为横向 | - |
| `flexDirectionColumn()` | 主轴方向为纵向 | - |
| `flexWrap(wrap)` | 换行 | FlexWrap |
| `justifyContent(justify)` | 主轴对齐 | FlexJustifyContent |
| `justifyContentCenter()` | 主轴居中对齐 | - |
| `justifyContentFlexStart()` | 主轴起点对齐 | - |
| `justifyContentFlexEnd()` | 主轴终点对齐 | - |
| `justifyContentSpaceBetween()` | 主轴两端对齐 | - |
| `justifyContentSpaceAround()` | 主轴均匀分布 | - |
| `alignItems(align)` | 交叉轴对齐 | FlexAlign |
| `alignItemsCenter()` | 交叉轴居中对齐 | - |
| `alignSelf(align)` | 自身交叉轴对齐 | FlexAlign |
| `flex(value)` | 弹性系数 | Float |
| `positionType(type)` | 定位类型 | FlexPositionType |
| `positionAbsolute()` | 绝对定位 | - |
| `positionRelative()` | 相对定位 | - |
| `absolutePosition(top, left, bottom, right)` | 绝对定位位置 | Float?... |
| `absolutePositionAllZero()` | 绝对定位四边为 0 | - |
| `left(l)` | 左边距离 | Float |
| `top(t)` | 上边距离 | Float |
| `right(r)` | 右边距离 | Float |
| `bottom(b)` | 下边距离 | Float |
| `allCenter()` | 居中对齐（主轴和交叉轴都居中） | - |
| `alignSelfCenter()` | 自身居中 | - |

## 基础事件

| 事件 | 描述 | 回调参数 |
|-----|------|---------|
| `click { }` | 单击 | ClickParams |
| `doubleClick { }` | 双击 | ClickParams |
| `longPress { }` | 长按 | LongPressParams |
| `pan { }` | 拖拽 | PanGestureParams |
| `pinch { }` | 捏合 | PinchGestureParams |
| `willAppear { }` | 将要可见 | - |
| `didAppear { }` | 完全可见 | - |
| `willDisappear { }` | 将要不可见 | - |
| `didDisappear { }` | 完全不可见 | - |
| `appearPercentage { }` | 可见百分比 | Float |
| `layoutFrameDidChange { }` | 布局变化 | Frame |
| `animationCompletion { }` | 动画完成 | AnimationCompletionParams |

## 事件参数类型

**ClickParams：**

| 参数 | 描述 | 类型 |
|-----|------|-----|
| `x` | 相对于组件的 x 坐标 | Float |
| `y` | 相对于组件的 y 坐标 | Float |

**LongPressParams：**

| 参数 | 描述 | 类型 |
|-----|------|-----|
| `x` | 相对于组件的 x 坐标 | Float |
| `y` | 相对于组件的 y 坐标 | Float |
| `state` | 状态：start/move/end | String |

**PanGestureParams：**

| 参数 | 描述 | 类型 |
|-----|------|-----|
| `x` | 相对于组件的 x 坐标 | Float |
| `y` | 相对于组件的 y 坐标 | Float |
| `state` | 状态：start/move/end | String |
| `pageX` | 相对于页面的 x 坐标 | Float |
| `pageY` | 相对于页面的 y 坐标 | Float |

**PinchGestureParams：**

| 参数 | 描述 | 类型 |
|-----|------|-----|
| `scale` | 缩放比例 | Float |
| `state` | 状态：start/move/end | String |

**Frame：**

| 参数 | 描述 | 类型 |
|-----|------|-----|
| `x` | 在父容器的 x 值 | Float |
| `y` | 在父容器的 y 值 | Float |
| `width` | 宽度 | Float |
| `height` | 高度 | Float |
