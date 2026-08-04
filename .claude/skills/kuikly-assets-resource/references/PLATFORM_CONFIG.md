# 各平台打包配置

首次使用 assets 资源时，需要按平台修改编译配置。

## Android

修改 `shared/build.gradle.kts`，添加 assets 资源路径：

```kotlin
android {
    // ...
    sourceSets {
        named("main") {
            // ...
            assets.srcDirs("src/commonMain/assets")
        }
    }
}
```

## iOS

修改 `shared/build.gradle.kts`，添加 CocoaPods 资源路径：

```kotlin
kotlin {
    cocoapods {
        // ...
        extraSpecAttributes["resources"] = "['src/commonMain/assets/**']"
    }
}
```

> ⚠️ **Kotlin 2.x + Compose 注意**：使用了 compose plugin 的模块，插件会默认修改 `spec.resources`，导致 `build.gradle.kts` 中的设置不生效。需要在 `gradle.properties` 中添加：
> ```properties
> compose.ios.resources.sync=false
> ```

> ⚠️ **多模块配置注意**：iOS 端在多模块项目中，子模块的 assets 资源**不会自动合并**到最终产物中。需要手动将子模块的 assets 资源拷贝到主模块下，由主模块统一打包。例如将子模块 `submodule/src/commonMain/assets/` 下的资源拷贝到主模块 `shared/src/commonMain/assets/` 对应目录中。

### iOS 动态化模式

framework 产物模式默认从 mainBundle 加载图片，无需额外适配。

动态化产物模式需要实现 `KuiklyRenderViewControllerDelegatorDelegate` 的 `assetsPathUrl` 接口，传递资源本地路径：

```objc
@protocol KuiklyRenderViewControllerDelegatorDelegate
// 返回 assets 资源的本地路径，目录下应包含 common/ 和 pageName/ 子目录
- (NSURL *)assetsPathUrl;
@end
```

## 鸿蒙

鸿蒙将业务代码编译为 so 文件，**不支持 assets 资源内置打包**。需要手动将资源拷贝到鸿蒙工程的 `resfile` 目录：

```
shared/src/commonMain/assets/common/* → entry/src/main/resources/resfile/common/*
```

## H5

H5 不需要打包配置。构建后的静态资源产物位于：

```
build/dist/js/productionExecutable/assets
```

将该目录的资源发布到 Web Server 或 CDN 即可。

## 微信小程序

在微信小程序工程目录下执行：

```shell
./gradlew :miniApp:copyAssets
```

> ⚠️ 微信小程序包限制为 2M，一般**不建议**资源内置打包到微信小程序中。

## 动态化打包

动态化资源打包**不需要额外配置**，assets 资源会自动打进产物包：

```
# 产物解压结构示例
├── assets/
│   ├── common/
│   │   └── penguin.png
│   └── image_demo/        # 页面名对应的目录
│       └── panda.png
├── config.json
└── image_demo.js
```

打包时会自动包含 `common/` 目录和对应 `pageName/` 目录下的所有资源。
