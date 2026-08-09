# KuiklyChatIOS

iOS 原生桥 + Compose Chat UI 资源包（自包含 CocoaPods 模块）。

## 目录结构

```text
KuiklyChatIOS/
├── KuiklyChatIOS.podspec
├── Sources/
│   ├── KRChatAttachmentPickerModule.h/.m
│   ├── KRChatByteCacheModule.h
│   ├── KRChatByteCacheModule.m
│   ├── KRChatImageExpandHandler.h
│   └── KRChatImageExpandHandler.m
├── Resources/
│   └── common/              # kuikly_chat_ic_*.svg → App.app/common/
└── README.md
```

发版前从 `KuiklyChat` 同步图标：`bash scripts/sync-kuikly-chat-ios-assets.sh`

| 能力 | 说明 |
|------|------|
| `KRChatAttachmentPickerModule` | 相册 / 相机 / 文件原生选择器 |
| `KRChatByteCacheModule` | 媒体字节缓存 Native 实现 |
| `KRChatImageExpandHandler` | HTTP / assets / file + SVG 图片加载（`+load` 自动注册） |
| `KuiklyChatIOS/Resources/common/*.svg` | 回形针、文件等内置图标，随 Pod 打入 App |

## 接入（推荐）

与 Kuikly 多模块规范一致：**KMP 依赖 + 平台原生包**，图标资源由 `KuiklyChatIOS` Pod 自动带上，**无需**在宿主 `shared` 里手抄 assets。

### 1. Podfile

```ruby
pod 'OpenKuiklyIOSRender', '~> 2.17.0'   # 与 Kuikly core 版本对齐
pod 'KuiklyChatIOS', :path => '../KuiklyChatIOS'  # 本地开发

# 发布后（podspec 在子目录）：
# pod 'KuiklyChatIOS', :git => 'https://git.woa.com/kuikly/KuiklyChatUI.git', :tag => '1.0.0-2.1.21', :podspec => 'KuiklyChatIOS/KuiklyChatIOS.podspec'
pod 'shared', :path => '../shared'         # 你的 KMP 宿主页模块
```

`KuiklyChatIOS` 使用 `static_framework = true`，确保 SVG 复制到 **主 App bundle** 的 `common/`（供 `assets://common/...` 加载），而不是留在动态 framework 内。

### 2. Kotlin（commonMain）

```kotlin
implementation("com.tencent.kuiklybase:KuiklyChat:<version>")
// 本地：api(project(":KuiklyChat"))
```

在 `createExternalModules()` 合并所需的 Kotlin Module：

```kotlin
import com.tencent.kuiklybase.chat.compose.core.chatByteCacheExternalModules
import com.tencent.kuiklybase.chat.kuiklyai.platform.chatAttachmentPickerExternalModules

override fun createExternalModules(): Map<String, Module>? {
    val modules = (super.createExternalModules() as? HashMap<String, Module>) ?: hashMapOf()
    modules.putAll(chatByteCacheExternalModules())
    modules.putAll(chatAttachmentPickerExternalModules())
    return modules
}
```

使用相机来源时，宿主 `Info.plist` 还需提供 `NSCameraUsageDescription`。相册使用
`PHPickerViewController`，不要求完整照片库权限。

### 3. iOS 图片加载

链接 `KuiklyChatIOS` Pod 后，`KRChatImageExpandHandler` 会在 `+load` 中自动注册（SDWebImage + SVGCoder，支持 `http(s)`、`assets://`、`file://` + `.svg`）。

若宿主需要自定义 `KuiklyRenderComponentExpandProtocol`，请在启动时调用 `[KRChatImageExpandHandler registerDefaultHandler]`，或自行组合其 `hr_setImageWithUrl:` 逻辑。

### 4. shared 模块 iOS 资源

只需打包**你自己**的页面 assets：

```kotlin
// shared/build.gradle.kts
cocoapods {
    extraSpecAttributes["resources"] = "['src/commonMain/assets/**']"
}
```

**不要**再把 `KuiklyChat` 的 assets 拷进 `shared`——图标由 `KuiklyChatIOS/Resources/common` 随 Pod 打入 App。发版前可运行 `bash scripts/sync-kuikly-chat-ios-assets.sh` 从 `KuiklyChat` 同步 SVG。

## 平台对照

| 平台 | KuiklyChat 依赖 | 原生包 | 图标资源 |
|------|-----------------|--------|----------|
| Android | Maven `KuiklyChat` | `KuiklyChatAndroid` | AAR 自动合并 assets |
| iOS | Maven `KuiklyChat` | **`KuiklyChatIOS` Pod** | Pod `resources` 自动打入 App |
| 鸿蒙 | Maven `KuiklyChat` | `@kuikly-open/chat-cache` | 见 `KuiklyChatOhos/README.md` |

## 版本对齐

- `OpenKuiklyIOSRender` 主版本需与 `com.tencent.kuikly:core` 一致（当前 `2.17.x`）。
- `KuiklyChat` 与 `KuiklyChatIOS` 使用同一发布版本号。
