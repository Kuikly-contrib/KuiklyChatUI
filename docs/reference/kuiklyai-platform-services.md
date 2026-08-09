---
last_updated: 2026-08-03
status: active
owner: @yukiykchen
---

# KuiklyAI 平台能力接入指南

> 接入语音识别、附件选择、剪贴板等平台能力，不需要修改 KuiklyChat 组件代码。

## 架构概述

KuiklyAI 通过三个 `commonMain` 接口定义平台能力契约，宿主 App 只需在 Android/iOS 端实现接口并注入即可：

| 能力 | 接口 | 不注入行为 |
|------|------|-----------|
| 语音输入 | `LlmSpeechRecognizer` | 麦克风按钮不显示 |
| 附件选择 | `LlmAttachmentPicker` | 附件按钮不显示 |
| 剪贴板 | `LlmClipboard` | 消息复制功能不可用 |

注入入口：`KuiklyAiChatView(speechRecognizer = ..., attachmentPicker = ..., clipboard = ...)`。

---

## 一、语音输入接入（`LlmSpeechRecognizer`）

### 接口定义

```kotlin
// commonMain — 不需要改，宿主只需实现以下接口

interface LlmSpeechRecognizer {
    /** 当前设备是否具备语音识别能力（含权限已授予） */
    suspend fun isAvailable(): Boolean

    /** 开始录音识别，通过 listener 持续回调结果 */
    fun start(listener: LlmSpeechListener): LlmSpeechSession
}

interface LlmSpeechListener {
    /** 识别出中间/最终文本 — 输入框会实时回显 */
    fun onResult(result: LlmSpeechResult)

    /** 识别失败 */
    fun onError(message: String)
}

interface LlmSpeechSession {
    fun stop()   // 正常结束，等待最终结果
    fun cancel() // 放弃本次录音
}

data class LlmSpeechResult(
    val text: String,
    val isFinal: Boolean
)
```

### 注入方式

```kotlin
// 宿主 App 的 Composable 页面中
KuiklyAiChatView(
    provider = myProvider,
    speechRecognizer = MyThirdPartySpeechRecognizer(context),
    // ...
)
```

### Android 示例：以科大讯飞为例

核心思路：在 `start()` 里把讯飞 SDK 的 `SpeechRecognizer` 回调桥接到 `LlmSpeechListener`。

```kotlin
package com.example.mychat.platform

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmSpeechListener
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmSpeechRecognizer
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmSpeechResult
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmSpeechSession

// 以讯飞 SDK 为例（pseudo-code，具体 API 以 SDK 文档为准）
class IflytekSpeechRecognizer(
    private val activity: Activity
) : LlmSpeechRecognizer {

    override suspend fun isAvailable(): Boolean {
        return activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }

    override fun start(listener: LlmSpeechListener): LlmSpeechSession {
        // 1. 创建讯飞识别器
        val recognizer = com.iflytek.cloud.SpeechRecognizer.createRecognizer(activity, null)

        // 2. 设置回调：桥接到 KuiklyAI 的 LlmSpeechListener
        recognizer.setParameter(com.iflytek.cloud.SpeechConstant.DOMAIN, "iat")
        recognizer.setParameter(com.iflytek.cloud.SpeechConstant.LANGUAGE, "zh_cn")

        recognizer.startListening(object : com.iflytek.cloud.RecognizerListener {
            override fun onResult(results: com.iflytek.cloud.RecognizerResult, isLast: Boolean) {
                val text = parseIflytekResult(results.resultString)
                listener.onResult(LlmSpeechResult(text, isLast))
                if (isLast) session.destroy()
            }
            override fun onError(errorCode: Int) {
                listener.onError("语音识别失败 (code=$errorCode)")
                session.destroy()
            }
            // ... 其他回调空实现
        })

        return IflytekSession(recognizer)
    }

    private fun parseIflytekResult(json: String): String {
        // 从讯飞返回的 JSON 中提取识别文本
        // 具体解析参见讯飞 SDK 文档
        return extractTextFromJson(json)
    }
}

private class IflytekSession(
    private val recognizer: com.iflytek.cloud.SpeechRecognizer
) : LlmSpeechSession {
    private var destroyed = false

    override fun stop() {
        if (destroyed) return
        recognizer.stopListening()
    }

    override fun cancel() {
        if (destroyed) return
        recognizer.cancel()
        destroy()
    }

    fun destroy() {
        if (destroyed) return
        destroyed = true
        recognizer.destroy()
    }
}
```

### Android 示例：以百度语音为例

```kotlin
class BaiduSpeechRecognizer(
    private val context: Context
) : LlmSpeechRecognizer {

    override suspend fun isAvailable(): Boolean {
        return context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }

    override fun start(listener: LlmSpeechListener): LlmSpeechSession {
        // 以百度 3.x SDK 为例
        val asrManager = com.baidu.speech.EventManager.create(context, "asr")
        // 注册回调，把 Baidu 的 onEvent 映射到 LlmSpeechListener.onResult/onError
        // ...
        asrManager.send("start", jsonParams, null, 0, 0)
        return BaiduSession(asrManager)
    }
}
```

无论换哪个厂商 SDK，只需要改实现类内部的 SDK 调用，`LlmSpeechRecognizer` 接口和 `KuiklyAiChatView` 入口都不变。

---

## 二、附件选择接入（`LlmAttachmentPicker`）

### 接口定义

```kotlin
interface LlmAttachmentPicker {
    val supportedSources: Set<LlmAttachmentSource>   // PHOTO_LIBRARY / CAMERA / FILE

    suspend fun pick(source: LlmAttachmentSource): List<LlmAttachment>

    fun kindOf(source: LlmAttachmentSource): LlmAttachmentKind  // 默认实现即可
}
```

### 注入方式

```kotlin
KuiklyAiChatView(
    provider = myProvider,
    attachmentPicker = MyAttachmentPicker(activity),
)
```

### Android 参考实现

库内已有完整示例：`KuiklyChat/src/androidMain/.../platform/android/AndroidLlmPlatform.kt` 中的 `AndroidLlmAttachmentPicker`。它基于 Android 原生 `ACTION_OPEN_DOCUMENT` / `MediaStore` 实现，宿主可以直接复用或替换为自己的选择器（如微信图片选择库、相册三方库）。

### iOS 默认实现

`KuiklyChatIOS` 提供 `KRChatAttachmentPickerModule`，使用
`PHPickerViewController` / `UIImagePickerController` / `UIDocumentPickerViewController`
实现相册、相机和文件选择。Kotlin 页面需在 `createExternalModules()` 中合并
`chatAttachmentPickerExternalModules()`；使用相机时宿主还需配置
`NSCameraUsageDescription`。

---

## 三、剪贴板接入（`LlmClipboard`）

### 接口定义

```kotlin
fun interface LlmClipboard {
    suspend fun copyText(label: String, text: String)
}
```

### 注入方式

```kotlin
KuiklyAiChatView(
    provider = myProvider,
    clipboard = MyClipboard(context),
)
```

实现非常简单，Android 一行代码：

```kotlin
class MyClipboard(context: Context) : LlmClipboard {
    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    override suspend fun copyText(label: String, text: String) {
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }
}
```

---

## 四、批量注入：`LlmChatPlatformServices`

三项能力可以通过聚合参数一次性注入：

```kotlin
KuiklyAiChatView(
    provider = myProvider,
    platformServices = LlmChatPlatformServices(
        clipboard = MyClipboard(context),
        attachmentPicker = MyAttachmentPicker(activity),
        speechRecognizer = IflytekSpeechRecognizer(activity),
    ),
)
```

`platformServices` 与单项参数同时传入时，聚合参数优先。

---

## 五、接入检查清单

- [ ] 在你的宿主 App 中创建实现类（如 `IflytekSpeechRecognizer`），实现 `LlmSpeechRecognizer` 接口
- [ ] 将实现类实例通过 `KuiklyAiChatView(speechRecognizer = ...)` 注入
- [ ] 真机验证：麦克风按钮显示 + 点击后有提示/正常录音
- [ ] 如需附件选择，同样实现 `LlmAttachmentPicker` 并注入
