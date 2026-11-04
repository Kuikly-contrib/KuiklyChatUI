# IDE 环境配置指南

> 💡 预览功能需要以下 IDE 配置（缺一不可），配置完成后即可在 Android Studio 中预览 Kuikly 页面。

## 1. Android Studio 版本要求

**必须使用 Android Studio Panda 2 | 2025.3.2 或更高版本**

预览功能依赖新版 IDE 的渲染能力，低于此版本可能无法正常显示预览。

---

## 2. 切换 Chrome 内核的 JetBrain Runtime

预览基于 Chrome 渲染，需要切换到带 Chrome 内核的 Runtime：

### 操作步骤

1. 打开 Android Studio
2. 菜单路径：**Help** → **Find Action**
3. 输入：`Choose boot java runtime for the IDE`
4. 选择带 **Chrome 内核** 的 Runtime
5. 重启 IDE

### 确认方法

菜单：**Help** → **About Android Studio**，查看 Runtime 信息是否包含 Chrome。

---

## 3. 安装 Kuikly Preview Plugin

### 下载插件

> ⚠️ **注意：以下为内网文档链接，需在公司内网环境下访问。** 如链接失效或无法访问，请联系 Kuikly 团队获取最新插件包，或访问 [Kuikly 预览文档](https://kuikly.woa.com/DevGuide/preview-ui.html) 查看最新下载地址。

从文档链接下载 IDE Plugin：https://doc.weixin.qq.com/doc/w3_ACIA0AbdAFwCNFZVaUtmnS3SvKvhd?scode=AJEAIQdfAAoq6HpHXVACIA0AbdAFw

### 安装步骤

1. 打开 Android Studio → **Settings**（Windows/Linux）或 **Preferences**（macOS）
2. 进入 **Plugins** 页面
3. 点击右上角 **⚙️ 齿轮图标**
4. 选择 **Install Plugin from Disk...**
5. 选择下载的 `.zip` 文件
6. 重启 IDE

---

## 4. 升级 Kotlin Multiplatform 插件

Kotlin Multiplatform 插件版本要求：**0.9-253.30387-AS-79** 或更高

此版本修复了以下问题：
- Composable 函数 Editor 报错
- 内联增量编译失效

### 升级方法

1. 打开 **Settings** → **Plugins**
2. 搜索 **Kotlin Multiplatform**
3. 更新到最新版本

---

## 5. KSP 配置确认（仅 Compose DSL 需要）

> ⚠️ **注意**：此配置仅在使用 **Compose DSL** 时需要，其他 DSL（如传统 DSL）无需配置。

确保 `build.gradle.kts` 中已开启预览支持：

```kotlin
ksp {
    arg("enablePreview", "true")
    // ... 其他 KSP 配置
}
```

---

## 6. 依赖确认

### Kotlin 版本要求

**预览功能要求 Kotlin 1.7.20 或更高版本**

低于此版本的项目无法使用预览功能，请先升级 Kotlin 版本。

### ui-tooling 依赖

确保 `commonMain` 依赖中包含 `ui-tooling`：

```kotlin
// 版本格式：{kuiklyVersion}-{kotlinVersion}
// 示例：2.16.0-beta-1.7.20（其中 1.7.20 为项目 Kotlin 版本）
implementation("com.tencent.kuikly:ui-tooling:${Version.getKuiklyVersion()}")
```

> ⚠️ **注意**：`ui-tooling` 的版本号需要匹配项目的 Kotlin 版本后缀，目前支持 Kotlin 1.7.20 及以上。

---

## 快速检查清单

使用以下清单确认环境是否配置完成：

```
IDE 环境检查：
- [ ] Android Studio 版本 ≥ Panda 2 | 2025.3.2
- [ ] Kotlin 版本 ≥ 1.7.20（预览功能最低要求）
- [ ] Runtime 已切换到 Chrome 内核
- [ ] Kuikly Preview Plugin 已安装
- [ ] Kotlin Multiplatform 插件已升级
- [ ] (Compose DSL) build.gradle.kts 中 enablePreview = true
- [ ] ui-tooling 依赖已添加
```

---

## 常见问题

| 问题 | 解决方案 |
|------|---------|
| 找不到 Runtime 切换选项 | 确认 Android Studio 版本是否满足要求 |
| 插件安装后预览不显示 | 重启 IDE，确认 KSP 配置已开启 |
| Composable 函数报错 | 升级 Kotlin Multiplatform 插件 |
| 版本号匹配问题 | 检查 ui-tooling 版本后缀是否与项目 Kotlin 版本一致 |
