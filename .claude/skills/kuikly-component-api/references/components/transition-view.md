# TransitionView 转场动画

内容视图转场过渡组件，支持淡入淡出、底部过渡入场等动画效果，常用于弹窗出场动画。

```kotlin
import com.tencent.kuikly.core.views.TransitionView
import com.tencent.kuikly.core.views.TransitionType
```

**基本用法：**

```kotlin
// 从底部入场
TransitionView(TransitionType.DIRECTION_FROM_BOTTOM) {
    attr {
        absolutePosition(0f, 0f, 0f, 0f)
    }
    
    View {
        attr {
            size(300f, 200f)
            backgroundColor(Color.WHITE)
        }
        // 弹窗内容
    }
}

// 淡入淡出
TransitionView(TransitionType.FADE_IN_OUT) {
    attr {
        transitionAppear(true) // 入场动画
    }
    event {
        transitionFinish { isEnter ->
            // 动画完成回调
        }
    }
    // 内容
}

// 自定义转场动画
TransitionView(TransitionType.CUSTOM) {
    attr {
        customBeginAnimationAttr {
            opacity(0f)
            transform(Scale(0.5f, 0.5f))
        }
        customEndAnimationAttr {
            opacity(1f)
            transform(Scale(1f, 1f))
        }
        customAnimation(Animation.springEaseInOut(0.35f, 0.9f, 1f))
    }
    // 内容
}
```

**TransitionType 类型：**

| 值 | 描述 |
|----|------|
| `NONE` | 无动画 |
| `DIRECTION_FROM_BOTTOM` | 从底部入场 |
| `DIRECTION_FROM_CENTER` | 从中间缩放入场 |
| `DIRECTION_FROM_RIGHT` | 从右侧入场 |
| `DIRECTION_FROM_LEFT` | 从左侧入场 |
| `FADE_IN_OUT` | 淡入淡出 |
| `CUSTOM` | 自定义转场动画 |

**属性 API：**

| 属性方法 | 描述 | 参数类型 |
|---------|------|---------|
| `transitionAppear(enterOrExit)` | 控制入场或退场动画 | Boolean |
| `customBeginAnimationAttr { }` | 自定义动画起始状态 | Attr.() -> Unit |
| `customEndAnimationAttr { }` | 自定义动画结束状态 | Attr.() -> Unit |
| `customAnimation(animation)` | 自定义动画参数 | Animation |

**事件：**

| 事件 | 描述 | 回调参数 |
|-----|------|---------|
| `transitionFinish { }` | 转场动画结束 | Boolean (是否入场) |
