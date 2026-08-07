# KuiklyChatIOS

iOS 原生桥 + Compose Chat UI 资源包。

| 能力 | 说明 |
|------|------|
| `KRChatByteCacheModule` | 媒体字节缓存 Native 实现 |
| `KuiklyChat/.../assets/common/*.svg` | 回形针、文件等内置图标，随 Pod 打入 App |

## 接入（推荐）

与 Kuikly 多模块规范一致：**KMP 依赖 + 平台原生包**，图标资源由 `KuiklyChatIOS` Pod 自动带上，**无需**在宿主 `shared` 里手抄 assets。

### 1. Podfile

```ruby
pod 'OpenKuiklyIOSRender', '~> 2.17.0'   # 与 Kuikly core 版本对齐
pod 'KuiklyChatIOS', :path => '..'         # 或发布后 pod 'KuiklyChatIOS', '~> x.y.z'
pod 'shared', :path => '../shared'         # 你的 KMP 宿主页模块
```

`KuiklyChatIOS` 已声明 `SDWebImage` / `SDWebImageSVGCoder` 依赖，用于解码 SVG 图标。

### 2. Kotlin（commonMain）

```kotlin
implementation("com.tencent.kuiklybase:KuiklyChat:<version>")
// 本地：api(project(":KuiklyChat"))
```

在 `createExternalModules()` 合并缓存 Module（若使用 `ChatCrossCache`）：

```kotlin
import com.tencent.kuiklybase.chat.compose.core.chatByteCacheExternalModules

override fun createExternalModules(): Map<String, Module>? {
    val modules = (super.createExternalModules() as? HashMap<String, Module>) ?: hashMapOf()
    modules.putAll(chatByteCacheExternalModules())
    return modules
}
```

### 3. iOS 图片加载（一次性配置）

Compose Chat 图标走 `assets://common/kuikly_chat_ic_*.svg`。宿主需在
`KuiklyRenderComponentExpandHandler` 中：

- 注册 `SDImageSVGCoder`
- `http(s)` 走网络
- `assets://` 从 main bundle 的 `common/` 读取

可参考本仓库 `iosApp/iosApp/KuiklyExpand/Handler/KuiklyRenderComponentExpandHandler.m`。

### 4. shared 模块 iOS 资源

只需打包**你自己**的页面 assets：

```kotlin
// shared/build.gradle.kts
cocoapods {
    extraSpecAttributes["resources"] = "['src/commonMain/assets/**']"
}
```

**不要**再把 `KuiklyChat` 的 assets 拷进 `shared`——那是本仓库 Demo 在 `project(:KuiklyChat)` 多模块开发时的临时方案；正式接入由 `KuiklyChatIOS` Pod 提供。

## 平台对照

| 平台 | KuiklyChat 依赖 | 原生包 | 图标资源 |
|------|-----------------|--------|----------|
| Android | Maven `KuiklyChat` | `KuiklyChatAndroid` | AAR 自动合并 assets |
| iOS | Maven `KuiklyChat` | **`KuiklyChatIOS` Pod** | Pod `resources` 自动打入 App |
| 鸿蒙 | Maven `KuiklyChat` | `@kuikly-open/chat-cache` | 见 `KuiklyChatOhos/README.md` |

## 版本对齐

- `OpenKuiklyIOSRender` 主版本需与 `com.tencent.kuikly:core` 一致（当前 `2.17.x`）。
- `KuiklyChat` 与 `KuiklyChatIOS` 使用同一发布版本号。
