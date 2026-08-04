# Kuikly DSL 组件 API 文档

本文档详细列出 Kuikly 声明式 DSL 中所有组件的 API 和 import 语句。

## [基础属性和事件](base-properties-events.md)
所有组件都支持的样式属性、布局属性和基础事件。

---

## 基础组件
- [View 容器](view.md) - 最基础的 UI 组件，可嵌套使用
- [Text 文本](text.md) - 文本显示组件
- [Image 图片](image.md) - 图片展示组件
- [Button 按钮](button.md) - 按钮组件

## 布局组件
- [Row 水平布局](row.md) - 水平排列子元素的容器
- [Column 垂直布局](column.md) - 垂直排列子元素的容器
- [Center 居中布局](center.md) - 使子元素居中的容器
- [SafeArea 安全区域](safe-area.md) - 自动应用安全区域内边距的容器

## 输入组件
- [Input 单行输入框](input.md) - 单行文本输入
- [TextArea 多行输入框](textarea.md) - 多行文本输入

## 列表组件
- [List 列表](list.md) - 列表视图，支持垂直和水平方向
- [Scroller 滚动容器](scroller.md) - 滚动容器
- [PageList 分页列表](page-list.md) - 分页滑动列表
- [WaterfallList 瀑布流](waterfall-list.md) - 瀑布流列表

## 刷新组件
- [Refresh 下拉刷新](refresh.md) - 下拉刷新
- [FooterRefresh 上拉加载](footer-refresh.md) - 上拉加载更多

## 弹窗组件
- [Modal 模态窗口](modal.md) - 自定义模态窗口
- [AlertDialog 提示对话框](alert-dialog.md) - 提示对话框
- [ActionSheet 操作表](action-sheet.md) - 操作表

## 表单组件
- [Switch 开关](switch.md) - 开关组件
- [Slider 滑块](slider.md) - 滑动选择器
- [CheckBox 复选框](checkbox.md) - 复选框
- [DatePicker 日期选择器](date-picker.md) - 日期选择器
- [ScrollPicker 滚动选择器](scroll-picker.md) - 滚动选择器

## 导航组件
- [Tabs 标签栏](tabs.md) - 选项卡切换
- [SliderPage 轮播图](slider-page.md) - 轮播图

## 富文本组件
- [RichText 富文本](rich-text.md) - 富文本，支持 Span、ImageSpan、PlaceholderSpan

## 媒体组件
- [Video 视频](video.md) - 视频播放器
- [APNG 动画](apng.md) - APNG 动画播放
- [PAG 动画](pag.md) - PAG 动画播放（类 Lottie）

## 效果组件
- [Blur 高斯模糊](blur.md) - 高斯模糊（毛玻璃）
- [Mask 遮罩](mask.md) - 遮罩视图
- [Canvas 画布](canvas.md) - 自绘画布
- [TransitionView 转场动画](transition-view.md) - 转场过渡动画

## iOS 原生组件
- [iOS 原生组件](ios-native.md) - iOSSwitch、iOSSlider、SegmentedControlIOS、LiquidGlass、GlassEffectContainer

## 其他组件
- [Hover 悬停置顶](hover.md) - 列表自动悬停组件
- [ActivityIndicator 加载指示器](activity-indicator.md) - 旋转菊花加载指示器
