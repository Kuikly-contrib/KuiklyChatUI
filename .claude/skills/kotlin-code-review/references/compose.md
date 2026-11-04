# 12、Compose 编程实践

**tl;dr**
-   应在事件回调或副作用处理器中修改状态，以避免无限重组循环。
-   应通过状态适配器将外部非响应式数据源桥接到 Compose 的 State 系统中。
-   所有直接影响 UI 的 State 读写操作都应在主线程执行，以避免并发冲突。
-   应使用 `derivedStateOf` 来派生状态，以确保仅在计算结果变化时才触发重组。
-   应使用 `rememberUpdatedState` 在长期运行的副作用中安全地访问最新的状态或 lambda，而无需重启副作用。
-   应遵循 `by remember` 管理状态、`val = remember` 缓存对象的模式，并避免 `var = remember`。
-   应将所有命令式行为（如 I/O 操作）作为副作用封装在生命周期感知的 Effect Handlers 中。
-   不应在长生命周期对象中持有任何在组合过程中创建的 Composable Lambda，以防止内存泄漏。
-   应为运行时可能变化的值使用 `compositionLocalOf`，仅为极少变化的值使用 `staticCompositionLocalOf`，以优化重组范围。

## 避免在组合过程中修改 State

组合函数的核心职责是根据输入的状态来描述 UI，此过程应为纯粹的只读操作。在组合函数执行期间直接修改其依赖的状态，是一种被称为“向后写入 (Backwards Write)”的错误实践。

向后写入会立即将当前组合过程标记为无效，并调度一次新的重组。由于状态的修改逻辑依然存在于组合函数体中，这将引发一个无限的重组循环，导致严重的性能问题。因此，所有状态变更都应被隔离在组合过程之外，通常在事件回调或受控的副作用处理器中执行。

**反例 1**

在 Composable 函数体中，读取一个 `State` 对象后，在同一组合作用域内又直接写入该 `State` 对象。

```kotlin
@Composable
fun BadCounter() {
    var count by remember { mutableStateOf(0) }

    // 1. 读取 State，Compose 开始观察 count 的变化
    Text(text = "Count: $count")

    // 2. 在同一组合过程中写入 State，立即触发下一次重组，形成无限循环
    count++
}
```

**正例 1**

将状态修改操作移至事件回调中，例如 `Button` 的 `onClick` lambda。该回调在组合过程之外执行，确保状态的变更只会触发一次合法的、可预测的重组。

```kotlin
@Composable
fun GoodCounter() {
    var count by remember { mutableStateOf(0) }

    Column {
        Text(text = "Count: $count")
        Button(onClick = {
            // 在组合过程之外的事件回调中安全地修改状态
            count++
        }) {
            Text("Increment")
        }
    }
}
```

**反例 2**

向后写入也可能以更隐蔽的方式发生。例如，上层组件读取了状态，而深层的子组件在组合过程中修改了同一个状态，这同样会造成无限重组。

```kotlin
class CounterState {
    var count: Int by mutableStateOf(0)
}

@Composable
fun Grandparent(state: CounterState) {
    // 1. Grandparent 读取 state.count
    Text("Count from Grandparent: ${state.count}")
    Parent(state)
}

@Composable
fun Parent(state: CounterState) {
    Child(state)
}

@Composable
fun Child(state: CounterState) {
    // 2. Child 在组合过程中写入 state.count，
    //    这使得 Grandparent 的读取失效，导致其重组，形成循环。
    state.count++
}
```

**正例 2**

遵循单向数据流原则，通过状态提升（State Hoisting）来解决。状态由高层组件持有，并通过参数向下传递。状态的修改通过事件回调（lambda）从低层组件向上传递，确保修改行为依然发生在受控的回调中。

```kotlin
@Composable
fun GoodGrandparent() {
    var count by remember { mutableStateOf(0) }

    Column {
        Text("Count from Grandparent: $count")

        Parent(
            count = count,
            onIncrement = { count++ }
        )
    }
}

@Composable
fun Parent(count: Int, onIncrement: () -> Unit) {
    Child(onIncrement = onIncrement)
}

@Composable
fun Child(onIncrement: () -> Unit) {
    Button(onClick = onIncrement) {
        Text("Increment from Child")
    }
}
```

**小结**

组合过程应为纯粹的只读操作，其唯一职责是根据状态描述 UI。任何状态的写入都应在组合过程之外执行，例如在用户事件回调或 `LaunchedEffect` 等受控的副作用处理器中，以确保 UI 的更新是可预测且高效的，从而避免无限重组循环。

## 避免读取非 State 的外部属性

Compose 的响应式模型仅能自动追踪对 `State` 对象的读取。当 Composable 依赖的数据源是一个外部的、非 `State` 的普通属性或方法时，Compose 无法感知其底层数据的变化。

直接读取这类外部属性，仅会在 Composable 首次组合或因其他 `State` 变化而重组时获取一次瞬时值。当该外部属性在组合过程之外发生变化时，Compose 不会收到任何通知，因此不会触发重组，最终导致 UI 显示过时的数据。

安全地集成这类外部非响应式数据源的推荐模式是，创建一个“状态适配器”。这个适配器通常是一个自定义的 Composable 函数，它使用 `DisposableEffect` 或 `produceState` 等副作用处理器来监听外部世界的变化，并将这些变化同步到一个内部的 `State` 对象中。

**反例**

下例中，`ConnectionStatusBanner` 直接调用一个传统的 `NetworkManager` 的属性来决定是否显示。Compose 无法追踪其 `isOnline` 字段的变化。

```kotlin
// 一个传统的、非 Compose 的网络管理单例
object NetworkManager {
    var isOnline: Boolean = true
        private set

    fun onNetworkChange(online: Boolean) {
        isOnline = online
    }
}

@Composable
fun ConnectionStatusBanner() {
    // 错误：直接读取一个外部、非响应式的属性。
    if (!NetworkManager.isOnline) {
        // 当网络断开时，此 Composable 不会重组，Banner 永远不会显示。
        Text(
            text = "无网络连接",
            modifier = Modifier.background(Color.Red).fillMaxWidth()
        )
    }
}
```

**正例**

创建一个名为 `rememberIsOnline` 的状态适配器。它封装了注册和注销网络回调的逻辑，并将其最新的状态作为 Compose `State` 暴露出来。

```kotlin
// 改造 NetworkManager 以支持监听器模式
object NetworkManager {
    private val listeners = mutableListOf<(Boolean) -> Unit>()

    fun registerCallback(callback: (Boolean) -> Unit) {
        listeners.add(callback)
        callback(checkCurrentNetworkStatus())
    }

    fun unregisterCallback(callback: (Boolean) -> Unit) {
        listeners.remove(callback)
    }

    private fun checkCurrentNetworkStatus(): Boolean { /* ... */ return true }
}

@Composable
fun rememberIsOnline(): State<Boolean> {
    return produceState(initialValue = true) {
        val callback = { online: Boolean ->
            value = online
        }
        NetworkManager.registerCallback(callback)
        awaitDispose {
            NetworkManager.unregisterCallback(callback)
        }
    }
}

@Composable
fun ConnectionStatusBanner() {
    val isOnline by rememberIsOnline()

    if (!isOnline) {
        Text(
            text = "无网络连接",
            modifier = Modifier.background(Color.Red).fillMaxWidth()
        )
    }
}
```

**小结**

不应在 Composable 中直接读取外部的、非响应式的属性，因为 Compose 无法追踪其变化。正确的做法是创建一个“状态适配器” Composable，使用 `DisposableEffect` 或 `produceState` 等工具监听外部变化，并将其同步到一个 `State` 中，从而将外部数据源安全地桥接到 Compose 的响应式世界。

## 避免在子线程中访问和修改 State

尽管 Compose 的底层快照系统在设计上支持并发，但当前的 `State` 实现并非线程安全。为保证 UI 的稳定性和可预测性，所有可能影响 UI 的 `State` 读写操作都应在主线程上执行。

Compose 的状态管理基于快照 (Snapshot) 系统。可以将其理解为一个版本控制模型：存在一个全局的主快照，而在 Composable 作用域之外的状态读写都在此主快照上进行。每当 Composable 重组时，它会基于主快照创建一个隔离的子快照，组合过程中的所有状态读写都发生在这个子快照内。重组结束后，子快照的变更会被应用回主快照。

在后台线程中直接访问 `State` 会破坏这一模型，并导致两种典型的 `IllegalStateException` 崩溃。

**示例 1: 并发写入冲突**

当一个 `State` 对象同时在组合过程的子快照中被写入，又在组合过程之外（如此处的后台线程）的主快照中被写入时，会在重组结束、应用快照变更时发生冲突。

```kotlin
private var state by mutableIntStateOf(0)

@Composable
fun StateConflictSample() {
    // 在组合过程的子快照中写入
    state++
    Text("$state")

    // 模拟在后台线程（主快照）中并发写入
    runBlocking {
        withContext(Dispatchers.Default) {
            state--
        }
    }
}
// 可能的崩溃日志: IllegalStateException: Unsupported concurrent change during composition.
```

**示例 2: 读取未应用的快照中的状态**

如果在组合过程的子快照中创建了一个新的 `State` 对象，那么在子快照被应用到主快照之前，该 `State` 对象对于主快照是不可见的。此时，任何在主快照上运行的代码（如后台线程）尝试读取该 `State`，都会导致崩溃。反之亦然。

```kotlin
@Composable
fun StateReadErrorSample1() {
    // `state` 在组合过程的子快照中被创建
    val state by remember { mutableStateOf("Initial State") }
    Text(text = state)

    // 错误：在后台线程（主快照）中，试图读取一个仅存在于子快照中的新状态。
    runBlocking {
        withContext(Dispatchers.Default) {
            println(state)
        }
    }
}
// 可能的崩溃日志: IllegalStateException: Reading a state that was created after the snapshot was taken...

@Composable
fun StateReadErrorSample2() {
    // `state` 在后台线程（主快照）中被创建
    val state by runBlocking {
        withContext(Dispatchers.Default) {
            mutableStateOf("state")
        }
    }
    // 错误：在组合过程（子快照）中，试图读取一个仅存在于主快照中的新状态。
    Text(text = state)
}
// 可能的崩溃日志: IllegalStateException: Reading a state that was created after the snapshot was taken...
```

**小结**

为确保线程安全并避免 `IllegalStateException` 崩溃，所有直接影响 UI 的 `State` 读写操作都应在主线程执行。在协程中，推荐的模式是默认在主线程工作，仅将耗时的非 UI 操作通过 `withContext` 切换到后台线程，并在协程自动切回主线程后才与 `State` 交互。

## 使用 derivedStateOf 优化重组

当一个值的计算依赖于一个或多个 `State` 对象时，特别是当源 `State`（例如 `LazyListState`）会频繁变化时，直接在组合函数中进行计算可能导致不必要的重组。

`derivedStateOf` 用于解决此问题。它会创建一个新的派生 `State`，该 `State` 仅在其计算结果真正发生变化时，才会通知其读取者进行重组。这有效地将下游组件与上游状态中不相关的频繁更新解耦，从而显著提升性能。

**反例**

在下例中，`isAtTop` 的值直接由 `listState.firstVisibleItemIndex` 计算得出。`LazyListState` 在滚动过程中，其内部的 `firstVisibleItemIndex` 属性会持续变化。尽管 `isAtTop` 只关心 `firstVisibleItemIndex` 是否为 0，但 `listState.firstVisibleItemIndex` 值的任何变化都会导致读取它的 `ListWithHeader` Composable 频繁重组。

```kotlin
@Composable
fun ListWithHeader() {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // 错误：直接读取频繁变化的 firstVisibleItemIndex
    // 任何 firstVisibleItemIndex 变化都会导致重组
    val isAtTop = listState.firstVisibleItemIndex == 0

    Column {
        if (!isAtTop) {
            Button(onClick = {
                scope.launch {
                    listState.animateScrollToItem(index = 0)
                }
            }) {
                Text("回到顶部")
            }
        }
        LazyColumn(state = listState) {
            items(100) { index ->
                Text("Item #$index", modifier = Modifier.fillMaxWidth().padding(16.dp))
            }
        }
    }
}
```

**正例**

通过 `remember { derivedStateOf { ... } }`，我们创建了一个只订阅 `firstVisibleItemIndex` 变化的派生状态。现在，只有当 `listState.firstVisibleItemIndex == 0` 的布尔结果从 `true` 变为 `false` 或反之时，`isAtTop` 的值才会更新，并精确地只通知读取它的 Composable 进行重组。

```kotlin
@Composable
fun ListWithHeaderOptimized() {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // 正确：使用 derivedStateOf 创建派生状态。
    // 只有 `firstVisibleItemIndex == 0` 的计算结果变化时才会重组
    val isAtTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 }
    }

    Column {
        if (!isAtTop) {
            Button(onClick = {
                scope.launch {
                    listState.animateScrollToItem(index = 0)
                }
            }) {
                Text("回到顶部")
            }
        }
        LazyColumn(state = listState) {
            items(100) { index ->
                Text("Item #$index", modifier = Modifier.fillMaxWidth().padding(16.dp))
            }
        }
    }
}
```

**小结**

当一个值的计算依赖于一个或多个频繁变化的 `State` 时，应使用 `remember { derivedStateOf { ... } }` 来创建派生状态。这能确保只有当计算结果真正改变时，读取该值的 Composable 才会重组，从而显著减少不必要的重组，提升性能。

## 使用 rememberUpdatedState 访问最新值

副作用处理器（如 `LaunchedEffect`）的 lambda 在其 `key` 未发生变化时，只会被创建和执行一次。这导致 lambda 会通过闭包捕获其创建时作用域内的状态值。如果该状态在后续的重组中发生变化，长期运行的副作用将继续持有并操作一个过时的（stale）值。

`rememberUpdatedState` 专为解决此场景而设计。它创建一个特殊的 `State` 引用，该引用本身是稳定的（不会导致副作用重启），但其 `.value` 始终指向在最新一次重组中传入的值。这使得长期运行的副作用可以在不重启的情况下，安全地访问到最新的状态。

**反例 1**

`LaunchedEffect` 的 `key` 为 `Unit`，其协程块只在 Composable 首次进入组合时启动。它捕获了 `onTimeout` 的初始实例。如果父组件因重组而传入了一个新的 `onTimeout` lambda，这个 Effect 不会感知到变化，并将在超时后调用一个过时的 lambda。

```kotlin
@Composable
fun TimerScreen(onTimeout: () -> Unit) {
    val timeoutMillis = 5000L

    // 错误：onTimeout lambda 捕获的是初始的 onTimeout 实例。
    LaunchedEffect(Unit) {
        delay(timeoutMillis)
        onTimeout() // 调用的是过时的 lambda
    }
}
```

**反例 2**

将 `onTimeout` 作为 `key` 传递给 `LaunchedEffect`。虽然这能确保 `onTimeout` 始终是最新版本，但它破坏了副作用的连续性。如果父组件因重组而传入一个新的 `onTimeout` 实例，`LaunchedEffect` 也会被不必要地取消和重启，导致计时器重置。

```kotlin
@Composable
fun TimerScreen(onTimeout: () -> Unit) {
    val timeoutMillis = 5000L

    // 错误：将不稳定的 lambda 作为 key。
    LaunchedEffect(onTimeout) {
        delay(timeoutMillis)
        onTimeout()
    }
}
```

**正例**

使用 `rememberUpdatedState` 为 `onTimeout` 创建一个稳定的引用。`LaunchedEffect` 的生命周期与 Composable 绑定，不会因 `onTimeout` 的变化而重启。同时，在协程内部通过读取 `latestOnTimeout` 的 `.value`，总能确保执行的是最新传入的 lambda。

```kotlin
@Composable
fun TimerScreen(onTimeout: () -> Unit) {
    val timeoutMillis = 5000L

    val latestOnTimeout by rememberUpdatedState(onTimeout)

    LaunchedEffect(Unit) {
        delay(timeoutMillis)
        latestOnTimeout()
    }
}
```

**小结**

当一个长期运行的副作用（如 `LaunchedEffect(Unit)`）需要访问一个在重组中会变化的值或 lambda 时，应使用 `rememberUpdatedState` 来包装该值。这可以确保副作用在不重启的情况下，始终能安全地读取到最新的版本，从而避免了捕获过时状态或因 `key` 变化而导致不必要重启的问题。

## 正确使用 remember 的声明模式

在 Composable 函数中，`remember` 的核心作用是在多次重组之间缓存一个值。然而，变量的声明方式是使用属性委托 (`by`) 还是直接赋值 (`=`)决定了其行为的根本差异。

-   **直接赋值 (`=`)**: `var a = value` 仅是将一个值赋给局部变量 `a`。对 `a` 的后续赋值只会改变这个局部变量的指向，Compose 的状态系统对此完全无感，因此不会触发重组。
-   **属性委托 (`by`)**: `var a by stateObject` 是 Kotlin 的语法糖，它将变量 `a` 的 `get()` 和 `set()` 方法委托给 `stateObject` 的 `getValue()` 和 `setValue()` 操作符。如果 `stateObject` 是一个 Compose `State`，那么读写 `a` 实际上就是在与 Compose 的响应式系统交互，从而能自动触发重组。

**示例 1：使用 var ... by remember 声明可变状态**

这是在 Composable 内部声明可变状态的标准范式。当需要一个值，其变化需要驱动 UI 更新时，应使用此模式。

```kotlin
@Composable
fun ClickCounter() {
    var count by remember { mutableStateOf(0) }

    Button(onClick = { count++ }) {
        Text("Clicked ${count} times")
    }
}
```

**示例 2：使用 val ... by remember 订阅只读状态**

当需要订阅一个外部数据源（如 ViewModel 的 `Flow`），且该状态在当前 Composable 中是只读的时，使用此模式。

```kotlin
@Composable
fun UserProfile(viewModel: UserViewModel) {
    val user by viewModel.userFlow.collectAsState()

    Text("Hello, ${user.name}")
}
```

**示例 3：使用 val ... = remember 缓存稳定对象**

这是 `remember` 最基础的用法，用于性能优化。当需要一个对象在多次重组之间保持实例稳定，但其本身的变化不应触发 UI 更新时，使用此模式。

```kotlin
@Composable
fun UserList(viewModel: UserViewModel) {
    // 缓存一个重量级且不可变的对象
    val jsonParser = remember { Gson() }

    // 记住一个 Compose 工具对象
    val focusRequester = remember { FocusRequester() }
}
```

**示例 4 ：避免使用 var ... = remember**

这种写法创建的只是一个普通的局部变量，其初始值被 `remember` 缓存。

```kotlin
@Composable
fun BadClickCounter() {
    // 错误：这是一个局部变量
    var count = remember { 0 }

    Button(onClick = { count++ }) {
        Text("Clicked ${count} times") // 永远显示 "Clicked 0 times"
    }
}
```

**小结**

`remember` 的使用应遵循明确的模式：
-   **管理状态**：使用 `val/var ... by remember` 配合 `mutableStateOf` 或 `collectAsState` 等 `State` 提供者。
-   **缓存对象**：使用 `val ... = remember` 来维持对象（尤其是 lambda 和重量级对象）的实例稳定性以优化性能。
-   **应当避免** `var ... = remember { ... }` 的用法，因为它既无法触发重组，也无法在重组间持久化状态。


## 正确理解和处理副作用

Compose 的核心范式是声明式的：`UI = f(State)`。一个 Composable 函数的唯一职责，是作为一个纯粹的转换器，将其**显式的、可被观察的状态**映射为一段 UI 描述。

这个转换过程应是无副作用的，因为它的目的仅仅是**描述** UI 在某个特定时刻的**样貌**，而不是**执行**任何行为。

**副作用 (Side Effect)** 则是任何逃离这个纯粹“描述”范畴的操作。它代表了一种命令式的行为，即“做某件事”，而不是“是某种样貌”。

Compose 运行时为了高效地更新 UI，可以随时、以任何频率执行或跳过重组。如果副作用混杂其中，它们的执行时机将变得混乱且不可控。

Compose 的设计策略并非消除副作用，而是将其纳入一个受控的管理模型中。为此，Compose 提供了一套具有生命周期感知的 **Effect Handlers**。这些 API 提供了一个安全的上下文，能够将命令式行为的执行，与其来源的 Composable 的生命周期精确地绑定起来。

**反例 1**

在组合过程中直接打印日志。由于重组的频率和时机不可预测，这里的日志可能会被重复打印多次，或在预期之外的时机打印，使其失去诊断意义。

```kotlin
@Composable
fun UserDetailScreen(user: User) {
    // 错误：在组合过程中直接执行日志记录。
    Log.d("Analytics", "UserDetailScreen composed for ${user.name}")

    Text("User: ${user.name}")
}
```

**正例 1**

对于“进入屏幕时执行一次”这类副作用，应使用 `LaunchedEffect`。将 `key` 设置为 `Unit` 可以确保其代码块在 Composable 的生命周期内只执行一次。

```kotlin
@Composable
fun UserDetailScreen(user: User) {
    // 正确：将一次性的副作用与 Composable 的出现绑定。
    LaunchedEffect(Unit) {
        Log.d("Analytics", "UserDetailScreen shown for ${user.name}")
    }

    Text("User: ${user.name}")
}
```

**反例 2**

在组合过程中直接修改外部状态，例如调用 ViewModel 的方法来记录一次屏幕浏览事件。`logScreenView` 方法可能会因为不相关的重组而被意外地调用多次，导致数据严重失真。

```kotlin
@Composable
fun ArticleScreen(articleId: String, viewModel: ArticleViewModel) {
    // 错误：直接在组合过程中修改外部状态。
    viewModel.logScreenView(articleId)

    Text("Article ID: $articleId")
}
```

**正例 2**

将一次性的状态修改操作同样放入 `LaunchedEffect(Unit)` 中，确保该行为只在 Composable 首次进入组合时执行。

```kotlin
@Composable
fun ArticleScreen(articleId: String, viewModel: ArticleViewModel) {
    // 正确：将一次性的状态修改与 Composable 的出现绑定。
    LaunchedEffect(Unit) {
        viewModel.logScreenView(articleId)
    }

    Text("Article ID: $articleId")
}
```

**反例 3**

在组合函数体中直接启动协程是一个典型的、危险的副作用。下例中，`scope.launch` 会在**每一次** `UserProfile` 重组时都被调用。

```kotlin
@Composable
fun UserProfile(userId: String, viewModel: UserViewModel) {
    val scope = rememberCoroutineScope()

    // 错误：在组合函数体中直接执行副作用
    scope.launch {
        val profile = viewModel.fetchProfile(userId)
    }
}
```

**正例 3**

使用 `LaunchedEffect` 将异步副作用与一个或多个 `key` 的生命周期绑定。`LaunchedEffect` 的代码块只在其 `key` 发生变化时（或首次组合时）执行。

```kotlin
@Composable
fun UserProfile(userId: String, viewModel: UserViewModel) {
    // 正确：将副作用封装在生命周期感知的 Effect Handler 中
    LaunchedEffect(userId) {
        val profile = viewModel.fetchProfile(userId)
    }
}
```

**小结**

务必保持 Composable 函数的纯粹性，其唯一职责是根据显式状态输入来描述 UI。所有命令式的行为（如 I/O 操作、修改外部状态）都必须作为副作用，被封装在合适的 Effect Handler 中，并将其生命周期与一个或多个状态 `key` 绑定。

## 避免泄露 Composable Lambda

不应在数据模型、ViewModel 或任何生命周期长于 UI 的对象中，持有**在 Composable 函数执行期间创建的** `@Composable () -> Unit` 类型的 lambda。这种做法会捕获其创建时的组合上下文 (Composition Context)，导致严重的内存泄漏。

**示例 1：存储在组合过程外创建的 Lambda**

`compose1` 在任何 Composable 函数之外被定义。因为它在组合过程之外创建，所以不存在可供其捕获的 `Composer` 上下文。它是一个无状态、无上下文的纯粹定义。因此，将它赋值给一个长生命周期的变量是完全安全的。

```kotlin
// 安全：在顶层定义，不捕获任何组合上下文
val compose1: @Composable () -> Unit = { Box() }

@Composable
fun SafeScreen() {
    compose1()
}
```

**示例 2：存储在组合过程中创建的 Lambda**

`compose2` 是一个长生命周期的全局变量。在 `MainPage` 函数执行时，`{ Box() }` 这个 lambda 被创建，它捕获了 `MainPage` 本次执行的 `Composer` 上下文。将这个“有上下文”的 lambda 赋值给 `compose2`，就造成了内存泄漏。

```kotlin
// 一个长生命周期的全局变量
lateinit var compose2: @Composable () -> Unit

@Composable
fun MainPage() {
    // 危险：这个在 Composable 函数体内创建的 lambda，
    // 捕获了 MainPage 的 Composer 上下文。
    compose2 = { Box() }
}
```

**小结**

在 Composable 函数执行期间创建的 lambda 会隐式捕获其短暂的 `Composer` 上下文。将其赋值给长生命周期的对象（如数据模型、ViewModel 或全局变量）会阻止该上下文被回收，从而导致内存泄漏。

## 准确选择 CompositionLocal 以优化性能

`CompositionLocal` 提供了一种在 Composable 组件树中隐式向下传递数据的机制。Compose 为其创建提供了两种 API：`compositionLocalOf` 和 `staticCompositionLocalOf`。它们的核心区别在于当 `CompositionLocal` 的值发生变化时，Compose 如何确定需要重组的范围，错误的选择可能导致显著的性能问题。

-   `staticCompositionLocalOf`：当其值变化时，会使提供该值的 `CompositionLocalProvider` 的**整个内容作用域**无效。这意味着作用域内的**所有** Composable 都会被重组，无论它们是否实际读取了该 `CompositionLocal` 的值。
-   `compositionLocalOf`：当其值变化时，Compose 会精确地只重组那些在组合过程中**实际读取**了 `.current` 值的 Composable。

**反例**

下例中，`LocalKeyboardHeight` 是一个可能频繁变化的值，但错误地使用了 `staticCompositionLocalOf`。

```kotlin
// 错误：对一个动态变化的值使用了 staticCompositionLocalOf
val LocalKeyboardHeight = staticCompositionLocalOf { 0.dp }

@Composable
fun KeyboardAwareScreen() {
    val keyboardHeight = observeKeyboardHeight()

    CompositionLocalProvider(LocalKeyboardHeight provides keyboardHeight) {
        // 当 keyboardHeight 变化时，即使 HeavyComponent 和 OtherComponent
        // 根本不关心键盘高度，它们也会被无效化并可能重组，造成性能浪费。
        HeavyComponent()
        OtherComponent()
        KeyboardDependentComponent()
    }
}

@Composable
fun KeyboardDependentComponent() {
    val keyboardOverlap = LocalKeyboardHeight.current
    Spacer(modifier = Modifier.height(keyboardOverlap))
}
```

**正例**

将 `staticCompositionLocalOf` 更改为 `compositionLocalOf`。现在，Compose 会精确地追踪 `LocalKeyboardHeight` 的读取者。

```kotlin
// 正确：对动态变化的值使用 compositionLocalOf
val LocalKeyboardHeight = compositionLocalOf { 0.dp }

@Composable
fun KeyboardAwareScreen() {
    val keyboardHeight = observeKeyboardHeight()

    CompositionLocalProvider(LocalKeyboardHeight provides keyboardHeight) {
        // 当 keyboardHeight 变化时，Compose 知道只有 KeyboardDependentComponent
        // 读取了 LocalKeyboardHeight.current，因此只会重组它。
        HeavyComponent()
        OtherComponent()
        KeyboardDependentComponent()
    }
}

@Composable
fun KeyboardDependentComponent() {
    val keyboardOverlap = LocalKeyboardHeight.current
    Spacer(modifier = Modifier.height(keyboardOverlap))
}
```

**小结**

`CompositionLocal` 类型的选择对性能至关重要。对于应用生命周期内基本不变的值，可使用 `staticCompositionLocalOf`。对于所有可能在运行时变化的值，应使用 `compositionLocalOf`，以确保重组范围最小化。

## 扩展阅读

-   [Jetpack Compose 中的思维模型 (Thinking in Compose)](https://developer.android.com/jetpack/compose/mental-model)
-   [状态和 Jetpack Compose (State and Jetpack Compose)](https://developer.android.com/jetpack/compose/state)
-   [Compose 中的附带效应 (Side-effects in Compose)](https://developer.android.com/jetpack/compose/side-effects)
-   [Compose 中的性能 (Compose performance)](https://developer.android.com/jetpack/compose/performance)
-   [使用 CompositionLocal 将数据的作用域限定在局部 (CompositionLocal)](https://developer.android.com/jetpack/compose/compositionlocal)
-   [Jetpack Compose 的各个阶段 (Phases of Jetpack Compose)](https://developer.android.com/jetpack/compose/phases)

## 总结

本章总结的 Compose 最佳实践共同指向一个核心思想：**构建声明式的、由状态驱动的、可预测的 UI**。

其基石在于严格遵循单向数据流原则，确保组合过程的纯粹性与只读性。所有状态的变更都必须在受控的事件回调或副作用处理器中进行，以保证 UI 更新的可预测性。

对于任何必须与外部世界交互的命令式行为——从异步数据获取到日志记录——都应作为副作用，并被审慎地封装在具有生命周期感知的 Effect Handlers 中。

性能优化是构建高质量应用的关键。通过 `remember` 缓存稳定对象、使用 `derivedStateOf` 避免不必要的重组、以及精确选择 `CompositionLocal` 类型，开发者可以构建出流畅且高效的用户界面。

最后，应用的健壮性依赖于对线程安全和内存管理的深刻理解。确保 UI 状态仅在主线程被访问，并避免因捕获组合上下文而导致的 Composable Lambda 泄漏，是预防运行时崩溃和内存问题的根本保障。

将这些实践内化为日常编码习惯，不仅是技术上的精进，更是向编写可维护、可扩展、高质量 Compose 应用迈出的关键一步。
