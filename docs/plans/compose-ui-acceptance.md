---
last_updated: 2026-08-03
status: active
---

# Compose Chat UI 跨端验收清单

> Phase 1–3 编译门禁已覆盖 Android 单测与 JS 编译；以下为人工 UI 验收项，可在各宿主 Demo 上逐项勾选。

## Android（`ComposeChatSdkDemoPage`）

- [ ] 发送文本后流式输出正常，Tool Call 卡片与文本同一次发送出现
- [ ] 上滑读历史时新消息不抢滚动，出现「回到底部」按钮与未读计数
- [ ] 点击「回到底部」后列表贴底并清空未读
- [ ] 加载更早消息后可视位置不跳动
- [ ] 发送中显示停止按钮，停止后可再次发送
- [ ] 失败时 Composer 与顶部错误条可展示并可清除

## JS / H5（`compileKotlinJs` 已通过）

- [ ] `ComposeChatSdkDemoPage` 可打开且布局正常
- [ ] 列表滚动与贴底行为与 Android 一致
- [ ] Composer 输入与发送/停止可用

## 输入法与键盘

- [ ] 多行输入可换行（平台默认行为）
- [ ] 发送后输入框清空

## 主题

- [ ] `ChatTheme` token 覆盖背景、气泡、Composer、错误色
- [ ] `ChatStrings` 文案可替换

## 备注

- 真实生产 AG-UI 网络联调不在 Phase 1–3 范围；使用 `ScriptedSseLlmTransport` + `AgUiLlmProvider` 完成端到端验收。
- IME `Send` action 依赖 Kuikly Compose 键盘 API，当前以点击发送按钮为主路径。
