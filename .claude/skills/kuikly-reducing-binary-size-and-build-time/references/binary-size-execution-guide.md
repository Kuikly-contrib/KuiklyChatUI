# 安装包优化（包体积优化）执行指南

> **注意**：多数业务在鸿蒙上将 Kotlin Native 产物编译为动态库，iOS 上编译为静态 framework，因此 iOS 上宿主编译选项也影响最终链接产物大小。可结合 iOS 苹果官方及业界优化措施进行整体优化，本指引重点关注 Kotlin Native 产物大小。

## 1. 编译选项优化

**具体优化项**：

| 优化项 | 说明 | 注意事项 |
|-------|------|---------|
| `--pack-dyn-relocs=relr` | 动态重定位打包 | - |
| `--gc-sections` + `-ffunction-sections` + `-fdata-sections` | 垃圾代码清除与函数 / 数据级别分段 | - |
| `-Os` | 优化代码大小 | `-Oz` 效果更佳但对性能影响偏大 |
| `-mllvm -enable-machine-outliner=always` | 提取重复指令 | 对性能影响偏大，使用时多加关注 |

**配置示例**（build.gradle.kts）：

以下以鸿蒙target配置为例：

```kotlin
val isReleaseBuild = project.findProperty("isReleaseBuild")?.toString()?.toBoolean() ?: false

kotlin {
    targets.all {
        compilations.all {
            kotlinOptions {
                if (isReleaseBuild) {
                    // 注意: -enable-machine-outliner=always 对性能影响偏大，后续将提供和PGO结合的优化
                    val CLANG_OPT_FLAGS = "-Os -mllvm -enable-machine-outliner=always -ffunction-sections"
                    val CLANG_FLAGS = "clangOptFlags.ios_arm64=$CLANG_OPT_FLAGS;clangDebugFlags.ios_arm64=$CLANG_OPT_FLAGS;clangOptFlags.ohos_arm64=$CLANG_OPT_FLAGS;clangDebugFlags.ohos_arm64=$CLANG_OPT_FLAGS"
                    freeCompilerArgs += "-Xoverride-konan-properties=$CLANG_FLAGS"
                }
            }
        }
    }
    ohosArm64 {
        binaries.sharedLib("shared") {
            // ... 省略其他选项 ...
            freeCompilerArgs += "-Xadd-light-debug=enable"
            linkerOpts += "--pack-dyn-relocs=relr"
            linkerOpts += "--gc-sections"
        }
    }
}
```

## 2. Kotlin Native 符号内部化

对工程执行编译后，在工程中查找libxxx_api.h（例如shared工程导出的c头文件名为libshared_api.h），通常位于build/bin/ohosArm64/xxx{Debug|Release}Shared/目录。
如果头文件中存在大量导出符号，建议用户使用 `kuikly-internalizing-kotlin-native-symbols` skill 进行符号内部化。
