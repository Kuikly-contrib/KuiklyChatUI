# 页面性能问题诊断执行指南

## 工作流程

### Step 1：确认基本信息以及问题信息

明确以下关键信息，如缺失需主动询问用户补齐后再进入下一步：
- 出现问题的页面是哪个、对应的场景/入口路径
- 是否为 JS 动态化模式
- 运行在什么平台（iOS / Android / 鸿蒙 等）
- 问题的具体表现（卡顿、白屏、滚动掉帧、启动慢等）、复现路径与复现概率

### Step 2：推断问题相关代码并埋设日志

根据 Step 1 收集到的信息，通过页面名称定位页面文件。Kuikly 页面名称通过 Kotlin 注解标注：
```kotlin
@Page("ButtonExamplePage")
internal class ButtonExamplePage: BasePager() { }
```

也可以通过用户提到的关键字定位代码文件。

分析页面文件及其关联文件代码，按模式匹配可能的问题点：
- **内置模式**：优先对照「通用场景问题」
- **JS动态化模式**：优先对照「JS动态化场景问题」+ 「通用场景问题」

在推断出的可疑逻辑位置增加日志打点，用于后续运行时验证。要求：
- 统一使用具有标识性的 TAG，例如 `KuiklyPerf_<页面名>_<模块>`，便于按 TAG 过滤抓取
- 在关键入口/出口、循环、回调、耗时调用前后打点，必要时记录起止时间戳计算耗时
- 使用 `KLog.i` 或 `KLog.d`，日志内容简洁，避免再次引入新的性能问题（如全量打印大对象）
- 注意避免出现高频日志，同时如果在低耗时函数加了日志，通过输出评估耗时的时候应该注意其影响

### Step 3：明确平台后自动编译运行

若 Step 1 中已经明确目标平台（iOS / Android / 鸿蒙等），则直接按该平台的标准流程自动执行编译与运行，无需再次询问用户；仅当平台仍不明确时才向用户确认。若编译或启动失败，先修复构建问题再继续。

### Step 4：提醒用户复现问题

运行起来后，主动提醒用户按 Step 1 中描述的复现路径触发问题场景，并等待用户确认复现完成。

同时提示用户在复现过程中保持日志采集开启。

### Step 5：自动根据TAG抓取日志并分析问题

用户确认复现完成后，按 Step 2 埋设的 TAG 自动过滤并抓取日志，通过日志分析定位问题，如果信息不足，考虑补充更多日志再继续。

### Step 6：给出修复方案并在用户确认后修复

定位到问题根因后，对照下方「通用场景问题」「JS动态化场景问题」中的解决方案模版，系统化输出修复方案，包含：
- 问题定位（定位到的具体代码与现象的对应关系）
- 修复思路与潜在风险
- 涉及修改的文件及改动概要

等待用户确认后再执行代码修改。

### Step 7：继续编译运行并提醒用户复现验证

完成修复后，再次自动执行编译运行，并提醒用户按相同路径复现，以确认问题是否已解决。

### Step 8：收尾

- 若用户反馈问题未修复：回到 Step 5，继续抓取日志、分析、调整修复方案；必要时补充新的 TAG 日志后回到 Step 3。
- 若用户确认问题已修复：总结本次优化，内容包含问题现象、根因分析、修复方案、涉及改动、性能收益（如有数据），输出后结束本次诊断流程。

---

## 通用场景问题

Kuikly内置模式性能接近原生，使用不当时容易出现性能问题，动态化模式下更为明显。

### 1. 生命周期函数调用被阻塞

Pager生命周期包括 `created`、`pageDidAppear`、`viewWillLoad`、`viewDidLoad` 等（详见 [Pager生命周期](pager-lifecycle.md) 和 [ComposeView生命周期](compose-view-lifecycle.md)），以及 `willInit`、`initModule`、`didInit`、`body` 等。确保override实现轻量快速。

**案例**：`created` 中 `loadInitialData` 使用 `awaitAll` 等待所有请求完成，阻塞Pager创建。

```kotlin
import com.tencent.kuikly.core.coroutines.GlobalScope

override fun created() {
    super.created()
    loadInitialData()
}

private fun loadInitialData() {
    GlobalScope.launch {
        try {
            val adsJob = async { requestADSSwitch() }
            val recentListJob = async { requestRecentList() }
            val activityJob = async { requestActivityList() }
            val results = listOf(adsJob, recentListJob, activityJob).awaitAll()
        }
    }
}
```

**解决方案**：改造为异步回调方式，避免阻塞生命周期。

```kotlin
override fun created() {
    super.created()
    loadInitialData()
}

private fun loadInitialData() {
    requestADSSwitch { /* completion callback */ }
    requestRecentList { /* completion callback */ }
    requestActivityList { /* completion callback */ }
}
```

### 2. 首屏异步拉取过多数据

数据量过大导致下载、传输、解析各环节耗时增加。

**案例**：首屏加载300K左右JSON数据（含2、3屏内容），无本地缓存。

**解决方案**：
1. 首屏仅拉取本屏数据，首屏成功后再预加载后续数据
2. 实现本地缓存，先展示缓存数据，拉到远端数据后更新

### 3. 过度的日志输出

避免在生命周期回调或首屏数据处理中输出过大的日志。

**案例**：对网络返回数据整体打印。

```kotlin
acquireModule<NetworkModule>(NetworkModule.MODULE_NAME).requestPost(
    "https://example.com/example_service",
    JSONObject().apply { put("key", "value") }
) { data, success, errorMsg, response ->
    KLog.i("ExampleTag", data.toString()) // 问题：全量打印
}
```

**解决方案**：限制打印长度，使用 debug log。

```kotlin
) { data, success, errorMsg, response ->
    val str = data.toString()
    KLog.d("ExampleTag", if(str.length > 100) str.substring(0, 100) else str)
}
```

### 4. 同步Module调用过于耗时

同步方法会等待返回，耗时过长导致Kuikly线程卡住。

**案例A**：`created` 中调用耗时Module接口。

```kotlin
val data = acquireModule<BizDataModule>(BizDataModule.MODULE_NAME).getData()
```

**解决方案**：降低耗时或改为异步回调。

```kotlin
acquireModule<BizDataModule>(BizDataModule.MODULE_NAME).getData {
    processData(it)
}
```

**案例B**：循环中频繁调用同步Native方法（如 `dateFormatter` 通过 `callNativeSync` 调用宿主侧ArkTS）。

```kotlin
internal class MyDateModule : BaseModule() {
    override fun moduleName(): String = MODULE_NAME
    fun dateFormatter(timeStamp: Long, format: String): JSONObject {
        val timeObject = JSONObject()
        timeObject.put("timeStamp", timeStamp)
        timeObject.put("format", format)
        return JSONObject(callNativeSync(DATE_FORMATTER, timeObject))
    }
    fun callNativeSync(methodName: String, data: JSONObject?): String {
        return toNative(false, methodName, data?.toString(), null, true).toString()
    }
}
```

在循环中逐条调用，严重卡顿：

```kotlin
fun parseJsonData(data: JSONObject?) {
    val list = data?.optJSONArray("filterList")
    if (list != null) {
        for (i in 0 until list.length()) {
            val item = list.optJSONObject(i)
            val startTime = item.opt(0)
            val formatStart = dateModule?.dateFormatter(startTime as Long, "yyyy/MM/dd")
        }
    }
}
```

**解决方案**：
1. 端侧以C实现 `MyDateModule`，参考 [registerExampleCModule](https://github.com/Tencent-TDS/KuiklyUI/blob/main/ohosApp/entry/src/main/cpp/napi_init.cpp)
2. 按需调用，仅在显示前执行
3. 使用Kuikly自带 `CalendarModule` 或改为异步调用

### 5. attr block中放置过多业务逻辑

attr block仅用于 observable 及属性更新/绑定，observable更新时会被重复执行，不应包含计算逻辑。

**案例**：attr中包含 `calculateMargin()`、`calulateBorderRadius()`、`calulateHeight()` 等计算。

**解决方案**：将计算逻辑移出attr，不变的用普通变量，会变的用observable。

```kotlin
val myMargin = calculateMargin()
var myBorderRadius by observable(0f)
var myHeight by observable(0f)

fun updateBorderRadius() { myBorderRadius = calulateBorderRadius() }
fun updateHeight() { myHeight = calulateHeight() }

View {
    attr {
        padding(myPadding)
        margin(myMargin)
        borderRadius(myBorderRadius)
        border(Border(lineWidth = 0.5f, lineStyle = BorderStyle.SOLID, color = Color(0xFFFB8C00)))
        allCenter()
        height(myHeight)
    }
}
```

### 6. 数据更新逻辑不合理

**案例**：`mergeItemsOrdered` 复杂度 O(n²)，数据量增长后造成严重卡顿。

```kotlin
private fun refreshViewModel() {
    bindValueChange({ attr.itemsUpdated }) {
        viewModel?.refreshAll(items = attr.items)
    }
}

override fun refreshAll(items: List<ShopItemBean>) {
    val targetDataList = mutableListOf<BaseDataItem<ItemBean>>().also {
        it.addAll(itemProcessor.initDataList(items))
    }
    dataList.mergeItemsOrdered(targetDataList, { item1, item2 -> item1.data == item2.data })
    refreshRenderList()
}
```

**解决方案**：数据不重复且有序时改为append；否则用 [diffUpdate](https://kuikly.tds.qq.com/DevGuide/reactive-update.html#高效更新列表-diffupdate) 差量更新。

### 7. Observable泄漏

**案例**：大量使用 `ReactiveObserver.bindValueChange` 无对应 `unbindValueChange`，observerOwnerMap持续增长。

**解决方案**：
1. 确保 `bindValueChange` 不在attr block等重复执行位置调用
2. 子view移除时执行unbind：

```kotlin
override fun didRemoveFromParentView() {
    super.didRemoveFromParentView()
    ReactiveObserver.unbindValueChange(model.showShowSelector)
}
```

---

## JS动态化场景问题

动态化模式以JS方式运行，在复杂信息流场景中动态化与Native差距约20%。偏差较大时应检查实现细节。

### 1. JSON数据解析慢

内置模式无问题，JS动态化模式易有性能问题。

**案例**：
```kotlin
val jsonObj = JSONObject(jsonStr)
val value = jsonObj.optString("key", "")
```

**解决方案**：使用JS引擎内置的解析能力。**2.7.0+版本默认启用，无需手动设置**。低版本需手动设置：

```kotlin
JSON.useNativeMethod = true
```

### 2. Range比较慢

**案例**：scroll中使用 `IntRange.intersect` 判断重叠，滚动缓慢。

**解决方案**：改为数值比较代替集合重叠判断。

```kotlin
scroll {
    var index = 0
    val offsetX = it.offsetX
    val pageListWidth = it.viewWidth
    ctx.galleryList.forEach { item ->
        val itemLeft = index * pageListWidth
        var lower = max((itemLeft).toInt(), (offsetX).toInt())
        var upper = min((itemLeft + pageListWidth).toInt(), (offsetX + pageListWidth).toInt())
        var count = if (upper - lower >= 0) upper - lower + 1 else 0
        val visiblePercentage = count * 1f / pageListWidth.toInt()
        item.transformScale1 = 0.85f + (1 - 0.85f) * visiblePercentage
        index++
    }
}
```

### 3. 集合操作慢

Kotlin集合类在JS引擎中效率低。Kuikly 1.9.0+ 内置JS集合能力支持，建议升级至2.x或至少1.9.0以上。
