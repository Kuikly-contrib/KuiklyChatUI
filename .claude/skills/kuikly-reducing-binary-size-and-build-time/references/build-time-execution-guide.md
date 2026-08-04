# 编译效率优化执行指南

Kotlin Native 编译耗时主要分布在：**下载依赖项、编译 klib、内联和 Link 阶段**，最主要是 Link 阶段。

## 从使用角度避免构建耗时

### 避免构建前下载依赖项
流水线构建场景下，如发现下载依赖项耗时较大，可将依赖预置到构建机镜像中，或使用专用构建机。

### 避免过多导出
export 的类 / 方法过多影响编译时间和产物大小。符号内部化的方法参见 [binary-size-execution-guide.md](binary-size-execution-guide.md)。

对 OC 平台可使用标志位控制不导出符号，通过显式 `@ObjCName` 注解导出：

```kotlin
// 推荐升级到 2.0.21-mini-060 或更新版本启用
freeCompilerArgs.add("-Xenable-default-objc-export=false")
```

---

## Debug Build 编译优化

### 全量构建场景

**1. 禁用 LTO**

链接阶段耗时占大头，Debug 及不需要关注性能的 Release 构建可禁用 LTO。示例：视频鸿蒙工程通过关闭 LTO 编译耗时由 2.5h 降至 0.5h（下降 80%）。

```kotlin
val isReleaseBuild = project.findProperty("isReleaseBuild")?.toString()?.toBoolean() ?: false

kotlin {
    ohosArm64 {
        compilations.all {
            compilerOptions.options.apply {
                if (!isReleaseBuild) {
                    freeCompilerArgs.add("-opt=false}")
                    freeCompilerArgs.add("-Xadd-light-debug=enable")
                    if (enableLinkOpt().not()) {
                        val CLANG_FLAGS = "clangFlags.ohos_arm64=-cc1 -emit-obj -disable-llvm-optzns -disable-llvm-passes -x ir -fno-emulated-tls;" +
                                "clangOptFlags.ohos_arm64=-O0 -ffunction-sections;" +
                                "clangNooptFlags.ohos_arm64=-O0;" +
                                "clangReleaseFlags.ohos_arm64=-O0"
                        freeCompilerArgs.add("-Xoverride-konan-properties=$CLANG_FLAGS")
                    }
                }
            }
        }
    }
}
```

通过 `-PisReleaseBuild=true` 在命令行控制。

**2. 减少内联**

视频主端工程从 17min 优化至 3.5min（M3 设备），下降 79%。内网 Kotlin 2.0.21 版本 Debug Build 默认减少内联。使用 1.9.x 的建议升级至 2.0.21。

**3. 启用 llvmOptLevel 和 llvmSizeLevel**

解决 Debug 包体过大导致的 lld 耗时激增。腾讯视频 Debug 全量编译从 18min 优化至 11min。**2.0.21-mini-062 版本起支持**，低版本建议升级。

```kotlin
freeCompilerArgs.add("-Xbinary=llvmOptLevel=1")
freeCompilerArgs.add("-Xbinary=llvmSizeLevel=2")
```

### 增量编译优化

**1. 启用增量编译**

Kotlin 2.0.21-mini-031 支持鸿蒙增量编译。腾讯视频全量编译从 4.5min 优化至 1.2min。

**2. 文件级别增量编译优化（2.0.21-mini-070 起支持）**

- `Xenable-cache-transitive-opt-for-cache`：仅重编对外部依赖有影响的修改。广告业务鸿蒙增量从 12min 降至 3min
- `Xenable-retry-single-thread-for-cache`：缓存并发编译崩溃时单线程重试，降低构建失败率

```kotlin
freeCompilerArgs.add("-Xenable-retry-single-thread-for-cache=true")
freeCompilerArgs.add("-Xenable-cache-transitive-opt-for-cache=true")
```

**3. 替换链接器**

替换为 mold 链接器：

```kotlin
kotlin {
    ohosArm64 {
        compilations.all {
            compilerOptions.options.apply {
                val first = gradle.startParameter.taskNames.firstOrNull()
                first?.let {
                    if (it.contains("Debug")) {
                        if (execCommandForStdout(project, projectDir, "which", "mold") == 0) {
                            val linker = "linker.macos_arm64-ohos_arm64=/opt/homebrew/bin/mold"
                            freeCompilerArgs.add("-Xoverride-konan-properties=$linker")
                        } else {
                            execCommandForStdout(project, projectDir, "brew", "install", "mold")
                        }
                    }
                }
            }
        }
    }
}
```

---

## Release Build 编译优化

Release Build 耗时大的主要原因是 LTO 全局优化。以下 3 个 Phase 对性能影响较小，但对编译耗时有明显降低（下降 30%~50%，具体因项目而异，使用时请验证）：

```kotlin
kotlin {
    ohosArm64 {
        compilations.all {
            compilerOptions.options.apply {
                // 禁用虚调用消除
                freeCompilerArgs.add("-Xdisable-phases=DevirtualizationAnalysis")
                // 禁用冗余静态初始化调用移除
                freeCompilerArgs.add("-Xdisable-phases=RemoveRedundantCallsToStaticInitializersPhase")
                // 禁用死代码移除（对产物大小有影响，死代码较多时切勿禁用）
                freeCompilerArgs.add("-Xdisable-phases=DCEPhase")
            }
        }
    }
}
```

**4. LLVM GlobalOpt 优化（2.0.21-mini-112 起支持）**

减少链接阶段对高复杂度函数的调用，编译耗时下降 70%。低版本建议升级。
