# 9、协程

**tl;dr**

- **应当** 尽量编写纯函数，避免访问共享状态和产生副作用
- **应当** 避免使用 GlobalScope，合理控制协程的生命周期
- **应当** 正确处理协程的协作式取消，避免捕获取消异常，并合理地设置检查点
- **应当** 通过合理的设计解决并发安全问题，避免在协程中使用线程锁
- **应当** 通过合理的设计避免抛出异常，并利用协程的机制正确处理协程中的异常

## 尽量编写纯函数

纯函数意味着函数执行过程中不依赖外部可变状态，并且不会修改外部状态（**无副作用**）。这极大地简化了并发逻辑，提高了代码的**可测试性**和**可靠性**。

**反例**

在协程中直接修改共享的可变状态，需要额外的同步机制（如锁）来保护数据，容易出错。

```Kotlin
suspend fun doTask(task: Task): Boolean { ... }

suspend fun CoroutineScope.submitTasks(tasks: List<Task>): Int {
    var completedTaskCount = 0
    tasks.map { task ->
        launch(Dispatchers.Default) {
            if (doTask(task)) {
                // 多个协程并发访问时，产生数据竞争
                completedTaskCount++
            }
        }
    }.joinAll()

    return completedTaskCount
}
```

**正例**

使用不可变数据结构和协程安全的工具（如 `StateFlow`、`Channel`），或将副作用限制在明确的范围（如 ViewModel 的 `State`）。尽量编写纯函数，将状态管理交给调用方。

```Kotlin
suspend fun doTask(task: Task): Boolean { ... }

suspend fun CoroutineScope.submitTasks(tasks: List<Task>): Int {
    return tasks.map { task ->
        async(Dispatchers.Default) {
            doTask(task)
        }
    }.count { it }
}
```

**小结**

协程体应当尽量只读取函数输入，返回处理结果，避免直接修改共享状态。


## 避免使用 GlobalScope

`GlobalScope` 的生命周期与整个应用程序一致，使用 `GlobalScope` 创建协程极易导致内存泄漏和资源浪费。应当始终将协程绑定到具有明确生命周期的作用域（如 `viewModelScope`、`rememberCoroutineScope` 或自定义的 `CoroutineScope`）上。

**反例**

使用 `GlobalScope` 启动后台任务，如代码所示：

```Kotlin
class DataManager {
    // 泄漏风险：当 DataManager 被销毁时，这个协程仍在运行！
    fun loadData() {
        GlobalScope.launch {
            // 耗时且无需永久运行的任务
            ...
        }
    }
}
```

**正例**

使用与组件生命周期绑定的 `CoroutineScope`。

```Kotlin
// 在 Android 中：ViewModel 自动拥有 viewModelScope
class MyViewModel : ViewModel() {
    fun loadData() {
        // 结构化并发：当 ViewModel 被清除时，viewModelScope 会自动取消所有子协程
        viewModelScope.launch {
            // 耗时且无需永久运行的任务
            ...
        }
    }
}
```

**小结**

不要直接使用 `GlobalScope` 启动协程，除非你明确需要一个应用程序生命周期级别的、无需被取消的顶层协程（这种情况极为罕见）。任何业务逻辑相关的协程都应在**结构化作用域**内启动。

## 正确处理协程的协作式取消

Kotlin 协程的取消是协作式的。挂起函数（如 `delay()`、`withContext()`）在被调用时会检查协程的 `Job` 状态并响应取消。在非挂起的耗时计算代码中，通常需要显式添加检查点来响应协程的取消状态。

**反例**

**1. 捕获取消异常导致取消失败**

```Kotlin
suspend fun doWork() {
    try {
        // ... 长时间运行的代码 ...
        delay(1000L) // 取消检查点
    } catch (e: Exception) {
        // 捕获了 CancellationException，并“吞掉”了它！
        println("Oops, exception caught: $e")
    }
}
```

**2. `withTimeout` 无法实现超时（计算密集型）**

```Kotlin
suspend fun longRunningTask() = withTimeout(100) { // 100ms 超时
    // 这个循环是计算密集型且不包含任何挂起函数/取消检查点
    // 它不会响应 withTimeout 抛出的 CancellationException，因此无法超时
    var i = 0
    while (true) {
        i++ // 耗时计算
    }
}
```

**正例**

**1. 正确处理取消异常**

`CancellationException` 应该被重新抛出以传播取消状态。

```Kotlin
suspend fun doWorkSafe() {
    try {
        delay(1000L)
    } catch (e: Exception) {
        // 重新抛出 CancellationException 以传播取消信号
        if (e is CancellationException) throw e
        // 处理其他异常
        ...
    }
}
```

**2. 在计算密集型任务中显式检查取消**

对于不调用挂起函数的耗时循环，使用 `ensureActive()` 或 `yield()` 显式检查取消。

```Kotlin
suspend fun longRunningTaskSafe() = withTimeout(100) {
    var i = 0
    while (true) {
        // 显式检查：如果协程已被取消，ensureActive() 会抛出 CancellationException
        currentCoroutineContext().ensureActive()
        
        i++ // 耗时计算
    }
}
```

**3. 使用 `suspendCancellableCoroutine` 转换回调**

正确实现 `invokeOnCancellation` 来处理取消，释放资源或通知底层 API 停止。

```Kotlin
// 假设这是一个老式带回调的 API
interface CancellationHandle { fun cancel() }
interface OldApi {
    fun call(callback: (Result<String>) -> Unit): CancellationHandle
}

suspend fun OldApi.callSuspend(): Result<String> = suspendCancellableCoroutine { continuation ->
    val handle = this.call { result ->
        // 成功或失败时恢复协程
        continuation.resume(result)
    }

    // 关键：当协程被取消时，通知底层 API 停止操作
    continuation.invokeOnCancellation {
        handle.cancel() // 调用底层 API 的取消方法
    }
}
```

**小结**

不要捕获并吞掉 `CancellationException`。对于非挂起代码，使用 `ensureActive()` 或 `yield()` 创建取消检查点。使用 `suspendCancellableCoroutine` 封装回调时，务必实现 `invokeOnCancellation`以确保异步任务得以正确取消。


## 避免在协程中使用线程锁

协程旨在通过协作和挂起来管理并发，在协程中使用线程锁会导致线程阻塞或其他不确定的调度行为。

**反例**

在协程中使用传统的 Java 线程锁。

```Kotlin
val lock = Any()

suspend fun processData() = withContext(Dispatchers.Default) {
    // 协程可以在锁内被挂起并切换线程或执行阻塞性的耗时任务
    synchronized(lock) {
        doHardWork() // 执行耗时任务
        // 另一个协程可能在另一个线程尝试加锁，并被阻塞
    }
}
```

在协程内使用线程锁，如果获取不到锁，线程将会阻塞等待，造成线程资源的浪费。

**正例**

**最佳实践：** 始终优先编写纯函数，避免并发访问共享资源，从设计上杜绝并发安全问题。

如果无法从设计上避免访问共享状态，可以使用 **Kotlin 协程库中的 `Mutex` (互斥锁)**。如代码所示：

```Kotlin
// kotlinx.coroutines.sync.Mutex
val mutex = Mutex()

suspend fun countWithMutex() {
    // 等待锁时当前协程挂起，所在线程可以用于执行其他协程
    mutex.withLock {
        doHardWork()
        ...
    } // 锁自动释放
}
```

`Mutex.withLock` 是一个挂起函数，它在等待锁时会挂起协程而不是阻塞线程，协程挂起之后调度器就可以将所在的线程用于执行其他协程。

需要注意的是，在协程锁范围内调用其他挂起函数导致当前协程被挂起时，协程锁并不会被自动释放，因此应当尽量避免对挂起函数的调用进行加锁。

**小结**

在协程中，可以使用协程锁 `Mutex` 保护共享资源。不过，请尽量编写纯函数，避免并发访问共享资源。

## 正确处理协程中的异常

尽量编写纯函数，也意味着尽量不要在挂起函数中抛异常。已知的异常行为应当谨慎及时地得到处理，并转换为挂起函数的返回值或者协程的结果。想要做到这一点，就必须掌握协程中的异常处理方法。

**示例 1：挂起函数内部的异常处理**

编写挂起函数时，应当妥善处理内部异常，不向外部抛出异常。内部无法恢复的异常需要通过返回值返回，如代码所示：

```kotlin
suspend fun getUserAsync(userId: String): Result<User> {  
    val user = try {  
        queryLocalCache(userId)  
    } catch (e: UserNotFoundException) {  
        null  
    }  
  
    if (user == null) {  
        var retry_left = 3  
        while (true) {  
            return try {  
                Result.success(queryServer(userId))  
            } catch (e: NetworkException) {  
                // 网络异常，执行重试逻辑          
                if (--retry_left > 0) {  
                    continue  
                } else {  
                    Result.failure(e)  
                }  
            } catch (e: AuthException) {  
                Result.failure(e)  
            }  
        }  
    } else {  
        return Result.success(user)  
    }  
}
```

示例代码对`queryLocalCache` 和 `queryServer` 两个函数调用可能抛出的异常做了详细的捕获处理，确保当前函数不会抛出异常；`getUserAsync` 通过 `Result` 类型支持对预期结果和异常结果的返回，可有效明确自身语义，降低使用者的心智负担。

**示例 2：协程异常处理器的合理使用**

`CoroutineExceptionHandler` 只能用于**顶级协程**或**主从作用域（ `SupervisorScope`）的直接子协程**来处理未捕获的异常。例如：

```Kotlin
val scope = CoroutineScope(Dispatchers.Default)  
scope.launch(CoroutineExceptionHandler { c, t ->  
    // 可捕获子协程异常
    println("Parent CoroutineExceptionHandler got $c $t")  
}) {  // 父协程
    launch(CoroutineExceptionHandler { c, t ->  
        // 不能捕获异常
        println("Child CoroutineExceptionHandler got $c $t")  
    }) { // 子协程
        throw IllegalStateException("Child Failed")  
    }  

    delay(500) // 父协程可能在子协程异常抛出时被取消  
    println("A finished") // 很可能不会执行  
}.join()
```

由于子协程并非顶级协程，也不是主从作用域的直接子协程，因此它的异常处理器不能捕获其异常。该异常会向上传播，由父协程的异常处理器捕获并取消父协程。

再例如：

```kotlin
val scope = CoroutineScope(Dispatchers.Default)  
scope.launch(CoroutineExceptionHandler { c, t ->  
    // 不能捕获以下主从作用域中的子协程异常
    println("Parent CoroutineExceptionHandler got $c $t")  
}) {  // 父协程
    supervisorScope {
        launch(CoroutineExceptionHandler { c, t ->  
            // 可捕获异常
            println("Child CoroutineExceptionHandler got $c $t")  
        }) { // 子协程
            throw IllegalStateException("Child Failed")  
        }  
    }
    delay(500) // 父协程在子协程异常抛出时不会被取消  
    println("A finished") // 会执行  
}.join()
```

当子协程处于主从作用域中时，子协程的异常不会向上传递，父协程也不会因此被取消。此外，子协程的异常处理器可以捕获到内部抛出的异常。

**小结**

默认情况下，子协程的异常会向上传播给父协程，导致父协程被取消，进而取消所有兄弟协程。主从作用域可以阻断子协程异常的向上传播。

掌握正确处理异常的方法，是编写稳健的协程代码的关键。

## 扩展阅读

1. **Kotlin 官方文档**
    - [Coroutines guide](https://kotlinlang.org/docs/coroutines-guide.html)
2. **《深入理解 Kotlin 协程》**  
    - 第 5.5 节：协程的取消
    - 第 5.6 节：协程的异常处理
    - 第 5.7 节：协程的作用域
    - 第 6.5 节：并发安全
    
## 总结

在 Kotlin 中，**协程**是处理异步和并发任务的核心。掌握编写协程体的正确方法，理解协程的协作式取消和异常传播机制，合理运用协程作用域实现结构化并发是运用协程解决问题的关键。