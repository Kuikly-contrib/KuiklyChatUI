# Kuikly 内部第三方组件列表

## shrinker
- **描述**：用于 Kuikly Kotlin Multiplatform 工程的 Kotlin 编译器插件，在 iOS Kotlin/Native 编译阶段自动收缩导出符号，减少 Framework 体积，同时保持 Android/JVM/JS 等非 Native 目标的可见性不变
- **Git URL**：https://git.woa.com/tme-kuikly/shrinker.git
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✗, JS ✓
- **支持动态化模式直接使用** true

## kuiklyx-viewmodel
- **描述**：帮助快速关联 Kuikly Pager 生命周期的 ViewModel，降低生命周期管理复杂度
- **Git URL**：https://git.woa.com/tme-kuikly/kuiklyx-viewmodel.git
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓, JS ✓
- **支持动态化模式直接引用** true

## kuiklyx-bridge
- **描述**：Kuikly 统一插件路由组件，通过插件名.方法名(ui.openUrl)进行分发，支持插件解耦，任意位置注册 Plugin 或 PluginMethod，无需关联 KuiklyModule，支持 scheme 路由，同时支持动态化及动态化向下兼容，支持多 Kotlin 版本
- **Git URL**：https://git.woa.com/tme-kuikly/kuiklyx-bridge.git
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓, JS ✓
- **支持动态化模式直接引用** true

## json-mate
- **描述**：JsonMate是一个基于KSP(Kotlin Symbol Processing)的插件，能够为标记了@FromJSONObject的类自动生成从Kuikly的JSONObject反序列化的代码。支持自动代码生成、复用注解、默认值、嵌套对象、泛型、List、Map等特性
- **Git URL**：https://git.woa.com/fishen/kuikly-json-mate.git
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓, JS ✓
- **支持动态化模式直接使用** true

## MMKV-KMP
- **描述**：基于 KMP跨平台MMKV组件，在提供跨端封装的同时，原平台的MMKV可以正常使用且与 KMP 封装互通
- **Git URL**：https://git.woa.com/karaoke/KMP/mmkv-wrap.git
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## markdown-parser
- **描述**：纯Kotlin实现的markdown跨平台解析器
- **Git URL**：https://git.woa.com/QQNews_Android/markdown-parser.git
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## markdown-compose-renderer
- **描述**：基于Kuikly Compose实现的markdown跨平台渲染器
- **Git URL**：https://git.woa.com/QQNews_Android/markdown-compose-render.git
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## tmmlogger
- **描述**：日志组件，可在Android、iOS、鸿蒙平台跨端使用
- **Git URL**：https://git.woa.com/TencentVideoBusinessModules/TMM/TMMLogger.git
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## kvkmm
- **描述**：用于 Key-Value 持久化存储，支持 String、Int、Long、Float、Double、Boolean、Byte、ByteArray、StringArray、Map 等多种数据类型，支持数据变化监听回调
- **Git URL**：https://git.woa.com/tencentVideo_HarmonyOS/VBKVKMM.git
- **平台支持**：Android ✓, iOS ✗, HarmonyOS ✓
- **支持动态化模式直接引用** false

## VBImageUploadServiceKMM
- **描述**：跨端图片上传组件，可用于图片上传
- **Git URL**：https://git.woa.com/tencentVideo_HarmonyOS/VBImageUploadServiceKMM.git
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## kmmkv
- **描述**：封装了微信MMKV，支持Android、iOS、HarmonyOS多平台KV存储读写
- **Git URL**：https://git.woa.com/fortune-app/lct-kmm/kmmkv.git
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## kjson
- **描述**：支持Android、iOS、HarmonyOS多平台的JSON处理组件
- **Git URL**：https://git.woa.com/fortune-app/lct-kmm/kjson.git
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## kloger
- **描述**：支持JVM、Android、iOS、HarmonyOS多平台日志打印，提供visitor接口方便接入XLog等日志上报，定制模块日志tag
- **Git URL**：https://git.woa.com/fortune-app/lct-kmm/kloger.git
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## khttp
- **描述**：支持Android、iOS、HarmonyOS多平台网络请求，支持jsonPost、formPost、get
- **Git URL**：https://git.woa.com/fortune-app/lct-kmm/khttp.git
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## VBSQLite
- **描述**：跨端数据库组件
- **Git URL**：https://git.woa.com/tencentVideo_HarmonyOS/VBSQLiteKMM.git
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## LottieKMM
- **描述**：跨端Lottie动画库
- **Git URL**：https://git.woa.com/tencentVideo_HarmonyOS/LottieKMM.git
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## VBDownloadService
- **描述**：跨端下载库，分别桥接到Android/iOS/鸿蒙下载库，给业务统一调用的接口
- **Git URL**：https://git.woa.com/TencentVideoBusinessModules/TMM/TMMDownloadService.git
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## KotlinStdPlatformExt
- **描述**：kotlin标准扩展库，提供崩溃打印能力
- **Git URL**：https://git.woa.com/VideoBaseClient/TMM-Group/KotlinStdPlatformExt.git
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## Stately
- **描述**：跨端Stately系列库，提供一些通用的并发和多线程操作
- **Git URL**：https://git.woa.com/VideoBaseClient/TMM-Group/Stately.git
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## tmm-platform-utils
- **描述**：KMM 跨端平台基础库，旨在抹平平台接口差异，为业务提供统一平台基础能力实现
- **Git URL**：https://git.woa.com/VideoBaseClient/TMM-Group/TMMPlatformUtils.git
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## kotlinx.collections.immutable
- **描述**：KN跨端不可变集合库
- **Git URL**：https://git.woa.com/VideoBaseClient/TMM-Group/KotlinNative/kotlinx.collections.immutable.git
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓, JS ✓
- **支持动态化模式直接引用** true

## okio
- **描述**：跨端I/O库，提供更高效可靠的文件读写和网络数据处理方式
- **Git URL**：https://git.woa.com/VideoBaseClient/TMM-Group/KotlinNative/okio.git
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓, JS ✓
- **支持动态化模式直接引用** true

## kotlinx-datetime
- **描述**：跨端日期库，提供日期的能力
- **Git URL**：https://git.woa.com/VideoBaseClient/TMM-Group/KotlinNative/kotlinx-datetime.git
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓, JS ✓
- **支持动态化模式直接引用** true

## kotlinx.serialization
- **描述**：跨端序列化库，提供序列化的能力
- **Git URL**：https://git.woa.com/VideoBaseClient/TMM-Group/KotlinNative/kotlinx.serialization.git
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓, JS ✓
- **支持动态化模式直接引用** true

## tmm-concurrency
- **描述**：跨端并发集合库
- **Git URL**：https://git.woa.com/VideoBaseClient/TMM-Group/concurrency.git
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## kotlinx-coroutines
- **描述**：kotlinx-coroutines跨端协程库
- **Git URL**：https://git.woa.com/VideoBaseClient/TMM-Group/KotlinNative/kotlinx.coroutines.git
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## ResHubTencentVideoWrapperKMM
- **描述**：跨端ResHub腾讯视频封装组件，可用于腾讯视频业务ResHub资源管理封装
- **Git URL**：https://git.woa.com/tencentVideo_HarmonyOS/ResHubTencentVideoWrapperKMM.git
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## ResHubKMM
- **描述**：跨端ResHub组件，可用于进行ResHub资源下载
- **Git URL**：https://git.woa.com/tencentVideo_HarmonyOS/ResHubKMM.git
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## pagemodel
- **描述**：用于业务方MVL和非MVL协议请求的组件
- **Git URL**：https://git.woa.com/tencentVideo_HarmonyOS/TMMPageModel.git
- **平台支持**：Android ✗, iOS ✗, HarmonyOS ✓
- **支持动态化模式直接引用** false

## PagKMM
- **描述**：跨端pag组件，可用于kotlin compose中展示pag动画
- **Git URL**：https://git.woa.com/tencentVideo_HarmonyOS/PagKMM.git
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## TMMWire
- **描述**：KMP版Wire组件，可基于PB协议生成跨Android、iOS、Harmony三端的kotlin数据类，提供了相应数据类序列化/反序列化等的运行时
- **Git URL**：https://git.woa.com/TencentVideoBusinessModules/TMM/TMMWire.git
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## atomicfu
- **描述**：拓展鸿蒙平台，添加发布私服配置，适配鸿蒙pthread_t与其他平台数据结构差异
- **Git URL**：https://git.woa.com/VideoBaseClient/TMM-Group/KotlinNative/kotlinx-atomicfu.git
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓, JS ✓
- **支持动态化模式直接引用** true

## QBPermission
- **描述**：基于 Kotlin MultiPlatform 框架对系统权限能力的跨端封装，既适用于基于 KMP 平台的跨端工程，也可以单端单独使用
- **Git URL**：https://git.woa.com/mqqbrowser/NewQB.git (目录: Projects/Framework/Foundation/Permission)
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## QBToggleSDK
- **描述**：公共的特性开关系统，在"主干开发、频繁发布"的研发模式下，有效控制新特性的开启和关闭，也可用于新特性的逐步灰度放量
- **Git URL**：https://git.woa.com/mqqbrowser/NewQB.git (目录: Projects/SDK/Toggle)
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## QBTuringSDK
- **描述**：TaidSDK封装了TuringShield提供的Device FingerPrint能力，提供向图灵顿后台交换OpenId、TAID、AID、TDID的Ticket
- **Git URL**：https://git.woa.com/mqqbrowser/NewQB.git (目录: Projects/SDK/Taid)
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## QBResHubSDK
- **描述**：灯塔大数据套件提供从数据高效上报集成、开发治理、敏捷分析、可视化全链条通用能力
- **Git URL**：https://git.woa.com/mqqbrowser/NewQB.git (目录: Projects/SDK/ResHub)
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## QBQImeiSDK
- **描述**：在IMEI被禁止采集的背景下，QIMEI是为了识别移动端设备，根据设备硬件特性生成唯一设备ID
- **Git URL**：https://git.woa.com/mqqbrowser/NewQB.git (目录: Projects/SDK/QImei)
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## QBKVSDK
- **描述**：基于 Kotlin MultiPlatform 框架对 MMKV 的跨端封装
- **Git URL**：https://git.woa.com/mqqbrowser/NewQB.git (目录: Projects/SDK/KV)
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## QBHttpSDK
- **描述**：基于 Kotlin MultiPlatform 框架对网络请求能力的跨端封装
- **Git URL**：https://git.woa.com/mqqbrowser/NewQB.git (目录: Projects/SDK/Http)
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## QBBuglySDK
- **描述**：对bugly组件的封装，支持错误上报(crash上报，主动上报错误)，性能监控(启动时间、卡顿、内存等)
- **Git URL**：https://git.woa.com/mqqbrowser/NewQB.git (目录: Projects/SDK/Bugly)
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## QBBeaconSDK
- **描述**：灯塔大数据套件提供从数据高效上报集成、开发治理、敏捷分析、可视化全链条通用能力，同时在用户行为分析、画像分析、自动化运营、机器学习、A/B实验决策等领域提供场景化的平台服务
- **Git URL**：https://git.woa.com/mqqbrowser/NewQB.git (目录: Projects/SDK/Beacon)
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## QBUIComponentSDK
- **描述**：基于QQ浏览器组件设计规范实现的Kuikly标准组件
- **Git URL**：https://git.woa.com/mqqbrowser/NewQB.git (目录: Projects/Framework/UI/UIComponent)
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## QBSQLiteSDK
- **描述**：基于 Kotlin MultiPlatform 框架对SQLite的跨端封装
- **Git URL**：https://git.woa.com/mqqbrowser/NewQB.git (目录: Projects/SDK/SQLite)
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## Androidx-Room
- **描述**：基于androidx官方Room组件，实现了对鸿蒙平台（ohosArm64Main）的适配，实现跨端使用
- **Git URL**：https://git.woa.com/mqqbrowser/NewQB.git (目录: Projects/SDK/Room)
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## QBPageSDK
- **描述**：基于 Kotlin MultiPlatform，包含了'前进后退栈'以及'多窗口能力'的page框架
- **Git URL**：https://git.woa.com/mqqbrowser/NewQB.git (目录: Projects/Framework/UI/Page)
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## QBLottieSDK
- **描述**：支持播放 Adobe After Effects 的导出动画
- **Git URL**：https://git.woa.com/mqqbrowser/NewQB.git (目录: Projects/Framework/UI/Lottie)
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## QBKmanifest
- **描述**：适用于 Kotlin MultiPlatform 平台的 DI/IOC 工具，可以实现高内聚、低耦合、可扩展、且无代码侵入的模块组装
- **Git URL**：https://git.woa.com/mqqbrowser/NewQB.git (目录: Plugins/kmanifest)
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## QBDiagnoseLoggerSDK
- **描述**：调试诊断系统，提供本地打日志能力并将日志保存于本地文件，提供日志捞取、日志染色、远程诊断、问题现场快照等端上问题的调试诊断服务
- **Git URL**：https://git.woa.com/mqqbrowser/NewQB.git (目录: Projects/SDK/DiagnoseLogger)
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## QBClipBoard
- **描述**：基于 Kotlin MultiPlatform框架对系统剪切板能力的跨端封装
- **Git URL**：https://git.woa.com/mqqbrowser/NewQB.git (目录: Projects/Framework/Foundation/ClipBoard)
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## QBDBHelperSDK
- **描述**：基于 Kotlin MultiPlatform对SQLite的跨端封装
- **Git URL**：https://git.woa.com/mqqbrowser/NewQB.git (目录: Projects/SDK/DBHelper)
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## webview
- **描述**：支持跨平台 WebView 容器，支持跨平台 JSBridge 及自定义 JSAPI，支持加载耗时统计及性能分析
- **Git URL**：https://git.woa.com/tencentVideo_HarmonyOS/WebViewKMM.git
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## skin
- **描述**：可以跟随系统或者用户自定义肤色使用
- **Git URL**：https://git.woa.com/tencentVideo_HarmonyOS/SkinKMM.git
- **平台支持**：Android ✗, iOS ✗, HarmonyOS ✓
- **支持动态化模式直接引用** false

## toast
- **描述**：提示信息组件
- **Git URL**：https://git.woa.com/tencentVideo_HarmonyOS/ToastKMM.git
- **平台支持**：Android ✓, iOS ✗, HarmonyOS ✓
- **支持动态化模式直接引用** false

## VBRouter
- **描述**：跨平台路由组件，基于 Compose 实现
- **Git URL**：https://git.woa.com/tencentVideo_HarmonyOS/VBRouterKMM.git
- **平台支持**：Android ✗, iOS ✗, HarmonyOS ✓
- **支持动态化模式直接引用** false

## pbservice
- **描述**：支持跨平台TRPC协议请求及TRACE链路分析，支持普通Get/Post请求，支持各阶段链路耗时统计
- **Git URL**：https://git.woa.com/tencentVideo_HarmonyOS/VBPBService.git
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false

## jceTool
- **描述**：Kuikly的跨平台jce组件，支持jce协议转model class，以及解析二进制的jce数据到model
- **Git URL**：https://git.woa.com/Kuikly/KuiklyJceTools.git
- **平台支持**：Android ✓, iOS ✓, HarmonyOS ✓
- **支持动态化模式直接引用** false
