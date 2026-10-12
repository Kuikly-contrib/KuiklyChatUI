---
last_updated: 2026-09-21
status: active
owner: @kuikly-chat-team
---

# 测试规范

## 位置与框架
- 单测位于各 `kuiklychat-*` 模块的 `src/commonTest/kotlin/`。
- 框架：`kotlin("test")`，Android host 运行，无需模拟器/设备。
- ⚠️ `commonTest` 是**多平台共享源集**，只能用 `kotlin("test")`。引入 JVM-only 依赖
  （JUnit、Konsist、Mockito 等）会让 js / ios / macos 各 target 的测试编译全部失败。
  确需 JVM-only 的测试，请新建 `androidUnitTest` 源集并在 `build.gradle.kts` 单独声明依赖。
- Preview 数据在 `:kuiklychat-testing`，不随伞形传递。

## 协议测试（AG-UI）
用例在 `kuiklychat-agui/src/commonTest/.../agui/`。

## 测试模式
- 生产用 `AgUiSession(threadId=...)`（Kuikly ObservableList）；测试用 `AgUiSession.forTesting(threadId=...)`（普通 MutableList，无 Pager 依赖）。

## 运行
```bash
bash scripts/ci-gates.sh android
# 或：
./gradlew :kuiklychat-core:testDebugUnitTest \
          :kuiklychat-compose:testDebugUnitTest \
          :kuiklychat-compat:testDebugUnitTest \
          :kuiklychat-agui:testDebugUnitTest \
          :kuiklychat-ai:testDebugUnitTest \
          :kuiklychat-testing:testDebugUnitTest
```

`:KuiklyChat:testDebugUnitTest` 会 dependsOn 各子模块测试，但 umbrella 自身不再带 testing 源码。

## 要求
- 新增事件类型 / 卡片类型 / 映射逻辑必须配套单测。
- 跨 SDK 一致性：可用官方 `ag-ui-protocol` 服务器的 SSE 数据流作对照。
