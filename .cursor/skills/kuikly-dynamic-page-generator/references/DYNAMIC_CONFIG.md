# 动态化配置模板

## 目录

- [JS Target 配置模板](#js-target-配置模板)
- [Kuikly 插件配置](#kuikly-插件配置)
- [KSP 分页参数](#ksp-分页参数)
- [完整配置示例](#完整配置示例)
- [常见编译问题](#常见编译问题)

---

## JS Target 配置模板

如果模块缺少 JS target，需要在 `build.gradle.kts` 的 `kotlin {}` 块中添加：

```kotlin
kotlin {
    // ... 其他 target（android、ios 等）

    js(IR) {
        moduleName = "nativevue2"  // 产物名称，与 kuikly { js { outputName() } } 一致
        browser {
            webpackTask {
                outputFileName = "${moduleName}.js"
            }
            commonWebpackConfig {
                output?.library = null
                devtool = "source-map"
            }
        }
        binaries.executable()
    }

    // ... sourceSets
}
```

### 注意事项

1. `moduleName` 必须与 `kuikly { js { outputName("...") } }` 一致
2. `output?.library = null` 确保不导出全局对象
3. `binaries.executable()` 将 Kotlin/JS 打包为可直接运行的 JS 文件
4. 添加 JS target 后，所有 `commonMain` 中的依赖都必须支持 JS 平台

---

## Kuikly 插件配置

### 根目录 build.gradle.kts

```kotlin
buildscript {
    dependencies {
        classpath("com.tencent.kuikly:core-gradle-plugin:$KuiklyVersion")
    }
}
```

### 模块 build.gradle.kts

```kotlin
plugins {
    id("kuikly")
}

kuikly {
    // JS 动态化（iOS / 鸿蒙 / H5）
    js {
        outputName("nativevue2")
        addSplitPages(listOf("PageA", "PageB"))
        // enableHermesCompile = true  // 可选：启用 Hermes 编译
    }

    // Android DEX 动态化
    dynamicApk {
        addSplitPages(listOf("PageA", "PageB"))
    }
}
```

---

## KSP 分页参数

分包构建时必须配置 KSP 的 `pageName` 参数：

```kotlin
ksp {
    arg("pageName", (project.properties["pageName"] as? String) ?: "")
}
```

---

## 完整配置示例

以下是一个支持动态化的模块的完整 `build.gradle.kts` 关键配置：

```kotlin
plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    id("com.android.library")
    id("com.google.devtools.ksp")
    id("kuikly")
}

kotlin {
    androidTarget()

    js(IR) {
        moduleName = "nativevue2"
        browser {
            webpackTask {
                outputFileName = "${moduleName}.js"
            }
            commonWebpackConfig {
                output?.library = null
                devtool = "source-map"
            }
        }
        binaries.executable()
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    val commonMain by sourceSets.getting {
        dependencies {
            implementation(project(":core"))
            implementation(project(":compose"))
            implementation(project(":core-annotations"))
        }
    }

    val jsMain by sourceSets.getting {
        dependsOn(commonMain)
    }
}

ksp {
    arg("pageName", (project.properties["pageName"] as? String) ?: "")
}

kuikly {
    js {
        outputName("nativevue2")
        addSplitPages(listOf("YourPageName"))
    }
    dynamicApk {
        addSplitPages(listOf("YourPageName"))
    }
}
```

---

## 常见编译问题

### 添加 JS target 后编译失败

**报错特征**：
```
Could not resolve org.jetbrains.kotlinx:xxx
```

**原因**：`commonMain` 中引入了不支持 JS 平台的 KMP 库。

**解决方案**：
1. 检查所有 `commonMain` 依赖是否有 JS 产物
2. 将不支持 JS 的依赖移到特定平台的 sourceSet（如 `androidMain`、`iosMain`）
3. 使用 `expect/actual` 隔离平台特定依赖（但注意动态化代码本身不能使用 expect/actual）

### 找不到 packSplitXXX 任务

**原因**：未启用 kuikly 插件。

**检查**：
1. 根目录 `build.gradle.kts` 是否有 `classpath("com.tencent.kuikly:core-gradle-plugin:...")`
2. 模块 `build.gradle.kts` 是否有 `id("kuikly")`

### JS 产物为空或页面不全

**原因**：KSP 增量编译导致页面注册不全。

**解决方案**：
```properties
# gradle.properties（Kuikly 2.11.0 之前版本需要）
ksp.incremental=false
```

### DEX 产物在低版本 Android 崩溃

**原因**：Android 7.0 及以下系统的 DEX 兼容性问题。

**解决方案**：上线前在低版本设备上充分测试。

### Hermes 编译失败

**检查**：
1. 确认 Hermes 工具链已正确安装
2. 确认 `enableHermesCompile = true` 配置正确
3. 检查 JS 代码是否使用了 Hermes 不支持的语法
