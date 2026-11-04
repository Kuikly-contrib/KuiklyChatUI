---
name: kuikly-component-api
description: Kuikly 组件属性查询助手。快速查询 Kuikly DSL 组件的属性 API、事件、方法和 import 语句。覆盖 View、Text、Image、Button、List、Input、Modal、ActionSheet、AlertDialog、Scroller、Tabs、RichText、Canvas、Video、Slider、Switch、CheckBox、Refresh、WaterfallList、PageList、SliderPage、TransitionView、PAG、APNG、Blur 等 38+ 个组件。适用场景：查属性、查 API、查 import、组件怎么用、有哪些属性、事件回调、方法调用、attr 有什么、event 有什么、Kuikly 组件文档、backgroundColor 怎么用、borderRadius、fontSize、flex 布局属性。
---

# Kuikly 组件属性查询助手

## 使用事件上报（必须执行）

**每次 Skill 被调用时，必须首先执行：**

```bash
bash "${SKILL_DIR}/scripts/beacon-report.sh" &
```

## 概述

本 skill 用于在编写 Kuikly UI 代码时，快速查询组件的**属性 API、事件、方法和 import 语句**。所有信息来自 `references/` 目录下的官方文档，**禁止凭记忆编造 API**。

## 查询流程

当用户询问某个组件的属性、事件、方法或 import 时：

### 1. 确定组件名称

根据用户描述匹配组件，参考下方组件索引表。

### 2. 查阅文档

**必须使用 read_file 读取对应文档后再回答：**

```
# 查询特定组件
read_file("${SKILL_DIR}/references/components/{组件文件名}.md")

# 查询基础属性和事件（所有组件通用）
read_file("${SKILL_DIR}/references/components/base-properties-events.md")

# 查询组件总览
read_file("${SKILL_DIR}/references/all-components.md")
```

### 3. 回复格式

回复时必须包含：
1. **import 语句** — 直接可复制使用
2. **属性/事件/方法** — 根据用户需求提供
3. **代码示例** — 简洁的使用示例

## 组件索引

### 基础组件
| 组件 | 文件 | import |
|------|------|--------|
| View 容器 | `view.md` | `com.tencent.kuikly.core.views.View` |
| Text 文本 | `text.md` | `com.tencent.kuikly.core.views.Text` |
| Image 图片 | `image.md` | `com.tencent.kuikly.core.views.Image` |
| Button 按钮 | `button.md` | `com.tencent.kuikly.core.views.compose.Button` |

### 布局组件
| 组件 | 文件 | import |
|------|------|--------|
| Row 水平布局 | `row.md` | `com.tencent.kuikly.core.views.layout.Row` |
| Column 垂直布局 | `column.md` | `com.tencent.kuikly.core.views.layout.Column` |
| Center 居中布局 | `center.md` | `com.tencent.kuikly.core.views.layout.Center` |
| SafeArea 安全区域 | `safe-area.md` | `com.tencent.kuikly.core.views.SafeArea` |

### 输入组件
| 组件 | 文件 | import |
|------|------|--------|
| Input 单行输入框 | `input.md` | `com.tencent.kuikly.core.views.Input` |
| TextArea 多行输入框 | `textarea.md` | `com.tencent.kuikly.core.views.TextArea` |

### 列表组件
| 组件 | 文件 | import |
|------|------|--------|
| List 列表 | `list.md` | `com.tencent.kuikly.core.views.List` |
| Scroller 滚动容器 | `scroller.md` | `com.tencent.kuikly.core.views.Scroller` |
| PageList 分页列表 | `page-list.md` | `com.tencent.kuikly.core.views.PageList` |
| WaterfallList 瀑布流 | `waterfall-list.md` | `com.tencent.kuikly.core.views.WaterfallList` |

### 刷新组件
| 组件 | 文件 | import |
|------|------|--------|
| Refresh 下拉刷新 | `refresh.md` | `com.tencent.kuikly.core.views.Refresh` |
| FooterRefresh 上拉加载 | `footer-refresh.md` | `com.tencent.kuikly.core.views.FooterRefresh` |

### 弹窗组件
| 组件 | 文件 | import |
|------|------|--------|
| Modal 模态窗口 | `modal.md` | `com.tencent.kuikly.core.views.Modal` |
| AlertDialog 提示对话框 | `alert-dialog.md` | `com.tencent.kuikly.core.views.AlertDialog` |
| ActionSheet 操作表 | `action-sheet.md` | `com.tencent.kuikly.core.views.ActionSheet` |

### 表单组件
| 组件 | 文件 | import |
|------|------|--------|
| Switch 开关 | `switch.md` | `com.tencent.kuikly.core.views.Switch` |
| Slider 滑块 | `slider.md` | `com.tencent.kuikly.core.views.Slider` |
| CheckBox 复选框 | `checkbox.md` | `com.tencent.kuikly.core.views.CheckBox` |
| DatePicker 日期选择器 | `date-picker.md` | `com.tencent.kuikly.core.views.DatePicker` |
| ScrollPicker 滚动选择器 | `scroll-picker.md` | `com.tencent.kuikly.core.views.ScrollPicker` |

### 导航组件
| 组件 | 文件 | import |
|------|------|--------|
| Tabs 标签栏 | `tabs.md` | `com.tencent.kuikly.core.views.Tabs` |
| SliderPage 轮播图 | `slider-page.md` | `com.tencent.kuikly.core.views.compose.SliderPage` |

### 富文本组件
| 组件 | 文件 | import |
|------|------|--------|
| RichText 富文本 | `rich-text.md` | `com.tencent.kuikly.core.views.RichText` |

### 媒体组件
| 组件 | 文件 | import |
|------|------|--------|
| Video 视频 | `video.md` | `com.tencent.kuikly.core.views.Video` |
| APNG 动画 | `apng.md` | `com.tencent.kuikly.core.views.APNG` |
| PAG 动画 | `pag.md` | `com.tencent.kuikly.core.views.PAG` |

### 效果组件
| 组件 | 文件 | import |
|------|------|--------|
| Blur 高斯模糊 | `blur.md` | `com.tencent.kuikly.core.views.Blur` |
| Mask 遮罩 | `mask.md` | `com.tencent.kuikly.core.views.Mask` |
| Canvas 画布 | `canvas.md` | `com.tencent.kuikly.core.views.Canvas` |
| TransitionView 转场动画 | `transition-view.md` | `com.tencent.kuikly.core.views.TransitionView` |

### iOS 原生组件
| 组件 | 文件 | import |
|------|------|--------|
| iOSSwitch / iOSSlider / SegmentedControlIOS / LiquidGlass / GlassEffectContainer | `ios-native.md` | `com.tencent.kuikly.core.views.ios.*` |

### 其他组件
| 组件 | 文件 | import |
|------|------|--------|
| Hover 悬停置顶 | `hover.md` | `com.tencent.kuikly.core.views.Hover` |
| ActivityIndicator 加载指示器 | `activity-indicator.md` | `com.tencent.kuikly.core.views.ActivityIndicator` |

## 常用属性速查

### 基础属性（所有组件通用）

**文件：** `base-properties-events.md`

**样式属性：** `backgroundColor`, `backgroundLinearGradient`, `boxShadow`, `borderRadius`, `border`, `visibility`, `opacity`, `touchEnable`, `transform`, `zIndex`, `overflow`, `clipPath`, `keepAlive`, `animation`, `accessibility`, `autoDarkEnable`

**布局属性：** `width`, `height`, `size`, `maxWidth`, `maxHeight`, `minWidth`, `minHeight`, `margin`, `padding`, `flexDirection`, `flexWrap`, `justifyContent`, `alignItems`, `alignSelf`, `flex`, `positionAbsolute`, `absolutePosition`, `allCenter`

**基础事件：** `click`, `doubleClick`, `longPress`, `pan`, `pinch`, `willAppear`, `didAppear`, `willDisappear`, `didDisappear`, `appearPercentage`, `layoutFrameDidChange`, `animationCompletion`

### 常用 import 汇总

```kotlin
// 基础组件
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.core.base.attr.ImageUri

// 布局组件
import com.tencent.kuikly.core.views.layout.Row
import com.tencent.kuikly.core.views.layout.Column
import com.tencent.kuikly.core.views.layout.Center
import com.tencent.kuikly.core.views.SafeArea
import com.tencent.kuikly.core.layout.FlexAlign

// 列表与滚动
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.PageList
import com.tencent.kuikly.core.views.WaterfallList
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.directives.vforLazy

// 输入
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.views.TextArea

// 弹窗
import com.tencent.kuikly.core.views.Modal
import com.tencent.kuikly.core.views.ModalDismissReason
import com.tencent.kuikly.core.views.AlertDialog
import com.tencent.kuikly.core.views.ActionSheet

// 刷新
import com.tencent.kuikly.core.views.Refresh
import com.tencent.kuikly.core.views.RefreshViewState
import com.tencent.kuikly.core.views.FooterRefresh
import com.tencent.kuikly.core.views.FooterRefreshState

// 表单
import com.tencent.kuikly.core.views.Switch
import com.tencent.kuikly.core.views.Slider
import com.tencent.kuikly.core.views.CheckBox
import com.tencent.kuikly.core.views.DatePicker
import com.tencent.kuikly.core.views.ScrollPicker

// 导航
import com.tencent.kuikly.core.views.Tabs
import com.tencent.kuikly.core.views.TabItem
import com.tencent.kuikly.core.views.compose.SliderPage

// 富文本
import com.tencent.kuikly.core.views.RichText

// 媒体
import com.tencent.kuikly.core.views.Video
import com.tencent.kuikly.core.views.VideoPlayControl
import com.tencent.kuikly.core.views.APNG
import com.tencent.kuikly.core.views.PAG
import com.tencent.kuikly.core.views.PAGScaleMode

// 效果
import com.tencent.kuikly.core.views.Blur
import com.tencent.kuikly.core.views.Mask
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.TransitionView
import com.tencent.kuikly.core.views.TransitionType

// 其他
import com.tencent.kuikly.core.views.Hover
import com.tencent.kuikly.core.views.ActivityIndicator

// iOS 原生
import com.tencent.kuikly.core.views.ios.iOSSwitch
import com.tencent.kuikly.core.views.ios.iOSSlider
import com.tencent.kuikly.core.views.ios.SegmentedControlIOS
import com.tencent.kuikly.core.views.LiquidGlass
import com.tencent.kuikly.core.views.GlassEffectContainer
import com.tencent.kuikly.core.views.GlassEffectStyle
```

## 注意事项

1. **所有 API 必须来自 references 目录下的文档，禁止凭记忆编造**
2. **基础属性和事件**（`base-properties-events.md`）适用于**所有组件**，查询某组件时应同时考虑
3. 如果用户查询的属性在文档中不存在，明确告知并建议查看基础属性或使用自定义扩展
4. 回复时引用文档来源路径
