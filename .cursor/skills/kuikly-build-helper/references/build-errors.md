# Kuikly 编译问题详细参考

## 1. Gradle 配置问题

### 1.1 Task 'assembleDebug' not found in project ':kuikly-dynamic-apk-builder'

**原因：** kuikly 插件依赖 `assembleXXX` 任务，找不到时报错。

**解决方案：**
1. 检查 `gradle.properties` 删除 `org.gradle.configureondemand=true`
2. 升级 Android Studio 版本
3. 修改模块名确保解析顺序，或配置 `shellProjectName`

### 1.2 找不到打包命令 packSplitApkXXX

**解决方案：** 确保启用了 kuikly 插件：
```kotlin
plugins { id("kuikly") }
kuikly { js { } dynamicApk { } }
```

---

## 2. Kotlin 版本问题

### 2.1 K2 模式不兼容
**错误：** `Plugin is incompatible with the Kotlin plugin in "K2" mode`

**解决：** 关闭 K2 Kotlin Mode 或使用新版插件

### 2.2 版本适配
| Kotlin | AGP | Gradle |
|--------|-----|--------|
| 2.0.21 | 7.4.2 | 7.5.1 |
| 1.9.22 | 7.4.2 | 7.5.1 |
| 1.7.20 | 7.1.3 | 7.3.3 |

Kotlin 1.4.20/1.3.10 需用 KAPT 替代 KSP。

### 2.3 KMP cocoapods sync 报错
**原因：** Kotlin 1.9 与 Xcode 16 兼容性问题
**解决：** 修改 Kotlin 版本或降低 Xcode 至 14

---

## 3. KSP/KAPT 问题

### 3.1 kuiklyCoreEntry 类找不到
1. 清理缓存：`./gradlew clean && ./gradlew --stop`
2. iOS 端重新 `pod install`
3. 检查 core-ksp 版本依赖配置

### 3.2 KSP 参数控制
```kotlin
ksp {
    arg("pageName", getPageName())
    arg("packLocalAarBundle", "1")  // 只打包 supportInLocal=true 的页面
    arg("packBundleByModuleId", "moduleA&moduleB")
}
```

---

## 4. 动态化打包问题

### 4.1 Compose dex 模式 pager not found
**错误：** `pager not found: , 找不到id为""的页面`

**原因：** `core-render-android` 使用了 api/implementation 而非 compileOnly

**解决：** 跨端模块必须用 `compileOnly` 引入 core-render-android

### 4.2 dex 模式页面打开失败
**错误：** `NoSuchMethodError: No static method xxx`

**原因：** 混淆规则冲突

**解决：** 使用默认配置和最新 SDK，二分查找定位问题

### 4.3 分页设置不生效
检查 KSP 配置了 pageName 参数控制：
```kotlin
ksp { arg("pageName", getPageName()) }
fun getPageName() = (project.properties["pageName"] as? String) ?: ""
```

---

## 5. 热重载问题

### 5.1 无法访问排查

**启动前：**
- `npm run serve` 失败 → 先执行 `npm install`
- 检查 `static_server` 文件夹是否存在

**启动后验证：**
- dex: `http://{ip}:8899/kuikly_dynamic.zip`
- js: `http://{ip}:8899/nativevue2.js`

**真机无法访问：**
- 检查是否同一网络
- Android 端口映射：`adb reverse tcp:8899 tcp:8899`
- 鸿蒙端口映射：`hdc rport tcp:8899 tcp:8899`
- 映射后使用 `127.0.0.1`

### 5.2 Windows 热重载注意事项
确保防火墙允许 8899 端口访问

---

## 6. iOS 平台问题

### 6.1 iOS 工程没有读取到 kuikly 产物
1. 检查 Podfile 是否引入 shared 产物
2. 检查 `fetchContextCodeWithPageName` 返回正确的 framework 名
3. 检查 pageName 与 Kotlin 侧页面注解名相同
4. 查看日志报错信息

---

## 7. 鸿蒙平台问题

### 7.1 编译 JS 报错 kotlinStoreYarnLock
**解决：** 删除 `kotlin-js-store` 目录重新编译

### 7.2 编译 Kuikly Compose 报 IR lowering failed
**原因：** KMP 依赖冲突（如 org.jetbrains.compose.runtime:runtime 冲突）

**解决：** `./gradlew demo:dependencies` 检查依赖，修正版本

### 7.3 Compose 函数需要 internal 无法跨模块
**方案1：** Compose 模块不导出，由主模块负责导出
**方案2：** 升级 Kotlin ≥ 2.0.21-mini-043

### 7.4 so 中 InitKuikly 出现 Not mapped
**情况1：** so 和头文件不匹配 → 确保都是最新编译产物
**情况2：** 其他函数先加载了 so → 主入口必须先 import libentry

### 7.5 鸿蒙真机 JS 模式 IP 映射
```bash
hdc rport tcp:8899 tcp:8899
hdc fport ls  # 验证映射
# 使用 127.0.0.1 作为 IP
```

### 7.6 KN 编译优化
1. 升级 Kotlin 2.0（debug 包关闭内联）
2. 关闭 lto（自测包可减少 80% 耗时）
3. 调大 gradle 内存：`org.gradle.jvmargs=-Xmx64G -Xms12G`
4. 减少不必要 export 导出
5. 使用 Mac M4 架构构建机
