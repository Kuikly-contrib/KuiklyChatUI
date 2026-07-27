---
last_updated: 2026-07-27
status: active
owner: @kuikly-chat-team
---

# 测试规范

## 位置与框架
- 单测位于 `KuiklyChat/src/commonTest/kotlin/com/tencent/kuiklybase/chat/`。
- 框架：`kotlin("test")`，JVM host 运行，无需模拟器/设备。
- ⚠️ `commonTest` 是**多平台共享源集**，只能用 `kotlin("test")`。引入 JVM-only 依赖
  （JUnit、Konsist、Mockito 等）会让 js / iosX64 / iosArm64 / iosSimulatorArm64 / macosX64 / macosArm64
  各 target 的测试编译全部失败——即使 `testDebugUnitTest` 在 android 上"看起来"该能过。
  确需 JVM-only 的测试，请新建 `androidUnitTest` 源集并在 `build.gradle.kts` 单独声明依赖。

## 协议测试（AG-UI）
| 文件 | 用例 | 覆盖 |
|------|------|------|
| `agui/AgUiEventParserTest.kt` | 38 | 16 种事件 JSON 反序列化、SSE 行解析、容错 |
| `agui/AgUiSessionTest.kt` | 31 | 状态机全路径：流文本/工具调用/Activity/错误/reset |
| `agui/AgUiProtocolConformanceTest.kt` | 12 | 端到端 Run 序列 |

## 测试模式
- 生产用 `AgUiSession(threadId=...)`（Kuikly ObservableList）；测试用 `AgUiSession.forTesting(threadId=...)`（普通 MutableList，无 Pager 依赖）。

## 运行
```bash
./gradlew :KuiklyChat:testDebugUnitTest      # 当前 81 个用例，应全绿
# 报告：KuiklyChat/build/reports/tests/testDebugUnitTest/index.html
```

本模块无 `jvm()` target，不存在 `compileKotlinJvm` / `compileTestKotlinJvm`；
只编译不跑测试用 `./gradlew :KuiklyChat:compileDebugUnitTestKotlinAndroid`。

## 要求
- 新增事件类型 / 卡片类型 / 映射逻辑必须配套单测。
- 跨 SDK 一致性：可用官方 `ag-ui-protocol` 服务器的 SSE 数据流作对照。
