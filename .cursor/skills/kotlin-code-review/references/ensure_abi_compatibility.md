# 11、确保 ABI 兼容

## tl;dr

- 明确指定函数返回值类型，避免类型推导变化
- 使用 `@OptIn` 和 `@RequiresOptIn` 注解管理 API 稳定性
- 通过方法重载扩展功能，而非修改现有方法签名
- 保持内联函数稳定，或使用新函数替代
- 保持类型继承结构稳定
- 不要为现有函数添加默认参数
- 不要在单一参数和变长参数间切换
- 不要更改已发布 API 的参数类型、顺序或个数
- 不要更改已发布类型的继承关系

## 不明确的返回值类型

在 Kotlin 中，如果函数没有明确指定返回值类型，编译器会根据函数体推导类型。当函数实现发生变化时，推导出的类型可能会改变，导致 ABI 不兼容，使已编译的客户端代码出现类型不匹配错误。

**反例**

```kotlin
// 不推荐：返回值类型不明确，可能导致类型推导变化
class ApiService {
    // 初始版本：推导为 Int
    fun getValue() = 42
    
    // 后续版本：推导为 Long，破坏 ABI 兼容性
    fun getValue() = 42L
}

// 用户代码可能依赖特定类型
fun userCode(service: ApiService) {
    val value: Int = service.getValue() // 可能发生 ClassCastException
}
```

**正例**
```kotlin
// 推荐：明确指定返回值类型
class ApiService {
    fun getValue(): Int = 42
    
    // 如果需要更改类型，添加新方法而非修改现有方法
    fun getValueAsLong(): Long = 42L
}
```

**小结**

明确指定返回值类型可以避免类型推导变化导致的 ABI 不兼容问题。

## 函数参数变化

函数参数的任何变化都会破坏 ABI 兼容性，包括参数类型、顺序、个数的修改。这些更改会导致已编译的客户端代码无法找到匹配的方法签名，引发编译错误或运行时异常。

**反例**

```kotlin
// 不推荐：更改函数参数类型、顺序或个数
class DataProcessor {
    // 初始版本
    fun process(data: String, count: Int): String = "$data-$count"
    
    // 破坏性更改：参数类型变化
    fun process(data: Int, count: String): String = "$data-$count"
    
    // 破坏性更改：参数顺序变化
    fun process(count: Int, data: String): String = "$data-$count"
    
    // 破坏性更改：参数个数变化
    fun process(data: String, count: Int, flag: Boolean): String = "$data-$count-$flag"
}
```

**正例**

```kotlin
// 推荐：保持函数签名稳定，通过重载扩展功能
class DataProcessor {
    // 保持原有方法不变
    fun process(data: String, count: Int): String = "$data-$count"
    
    // 添加重载方法而非修改现有方法
    fun process(data: Int, count: String): String = "$data-$count"
    fun process(data: String, count: Int, flag: Boolean): String = "$data-$count-$flag"
}
```

**小结**

通过方法重载而非修改现有方法签名来扩展功能，确保 ABI 兼容性。

## 默认参数陷阱

为现有函数添加默认参数是一个常见的陷阱。虽然从 API 层面看是向后兼容的，但在 ABI 层面会破坏兼容性，因为默认参数会改变函数的字节码签名，导致已编译的客户端代码无法正确调用。

**反例**

```kotlin
// 不推荐：为现有函数添加默认参数
class Calculator {
    // 初始版本
    fun calculate(a: Int, b: Int): Int = a + b
    
    // 添加默认参数，API 兼容但 ABI 不兼容
    fun calculate(a: Int, b: Int, operation: String = "add"): Int = 
        when (operation) {
            "add" -> a + b
            "multiply" -> a * b
            else -> a + b
        }
}
```

**正例**

```kotlin
// 推荐：使用重载方法而非默认参数
class Calculator {
    fun calculate(a: Int, b: Int): Int = a + b
    
    // 添加重载方法
    fun calculate(a: Int, b: Int, operation: String): Int = 
        when (operation) {
            "add" -> a + b
            "multiply" -> a * b
            else -> a + b
        }
}
```

**小结**

添加默认参数虽然 API 兼容，但会破坏 ABI 兼容性，应使用重载方法替代。

## 变长参数问题

单一参数函数和变长参数函数在 ABI 层面是不兼容的。即使从 API 角度看调用方式相同，但底层的字节码签名不同，会导致已编译的客户端代码无法正确调用修改后的函数。

**反例**

```kotlin
// 不推荐：在单一参数和变长参数间切换
class Logger {
    // 初始版本：单一参数
    fun log(message: String) = println(message)
    
    // 更改为变长参数，API 兼容但 ABI 不兼容
    fun log(vararg messages: String) = messages.forEach(::println)
}
```

**正例**

```kotlin
// 推荐：保持方法签名稳定，添加重载方法
class Logger {
    fun log(message: String) = println(message)
    
    // 添加变长参数的重载方法
    fun log(vararg messages: String) = messages.forEach(::println)
}
```

**小结**

单一参数和变长参数函数在 ABI 层面不兼容，应使用重载方法处理不同参数需求。

## 内联函数变更

内联函数的任何变更都会直接影响调用方的二进制代码，因为内联函数在编译时会被直接嵌入到调用点。修改内联函数的实现会改变所有调用点的代码，导致 ABI 不兼容。

**反例**

```kotlin
// 不推荐：修改内联函数体
inline fun processData(data: String): String {
    // 初始实现
    return data.uppercase()
    
    // 修改实现，破坏 ABI 兼容性
    // return data.lowercase()
}
```

**正例**

```kotlin
// 推荐：保持内联函数稳定，或使用非内联函数
inline fun processData(data: String): String = data.uppercase()

// 如需修改行为，添加新函数
inline fun processDataLowercase(data: String): String = data.lowercase()
```

**小结**

内联函数的任何变更都会影响调用方的二进制代码，应保持稳定或使用新函数。

## 类型继承结构变化

类型的继承结构变化会破坏多态性和类型检查机制。当类的继承关系发生改变时，已编译的客户端代码中的类型转换、方法分派和类型检查都会受到影响，导致运行时错误。

**反例**

```kotlin
// 不推荐：更改类型的继承结构
// 初始版本
open class BaseService
class ApiService : BaseService()

// 破坏性更改：移除继承关系
class ApiService // 不再继承 BaseService

// 或更改继承层次
class ApiService : AnotherBaseService()
```

**正例**

```kotlin
// 推荐：保持类型继承结构稳定
open class BaseService
class ApiService : BaseService()

// 如需更改继承关系，创建新类型
class NewApiService : AnotherBaseService()
```

**小结**

类型继承结构的变化会破坏多态性和类型检查，应保持稳定或创建新类型。

## 使用 OptIn 注解

Kotlin 提供了 `@OptIn` 和 `@RequiresOptIn` 注解来管理 API 的稳定性。这些注解允许库开发者明确标记实验性 API，要求用户显式选择使用，从而在 API 变更时提供更好的兼容性保证。

**示例：RequiresOptIn**

```kotlin
// 定义实验性 API
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "此 API 是实验性的，可能在未来版本中更改"
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class ExperimentalApi

// 使用实验性 API
@ExperimentalApi
class ExperimentalService {
    fun experimentalMethod(): String = "实验性功能"
}

// 用户代码需要显式 opt-in
@OptIn(ExperimentalApi::class)
fun useExperimentalApi() {
    val service = ExperimentalService()
    println(service.experimentalMethod())
}
```

**小结**

使用 `@OptIn` 和 `@RequiresOptIn` 注解管理 API 稳定性，让用户明确了解 API 状态。

## 扩展阅读

- [Kotlin Opt-in Requirements](https://kotlinlang.org/docs/opt-in-requirements.html)
- [Kotlin Deprecation](https://kotlinlang.org/docs/deprecation.html)
- [Binary Compatibility](https://kotlinlang.org/docs/binary-compatibility.html)

## 总结

确保 ABI 兼容性的关键在于：明确返回值类型、避免破坏性更改、使用 OptIn 注解管理 API 稳定性，以及通过重载方法而非修改现有方法签名来扩展功能。这些实践可以显著减少升级成本并提高库的可靠性。
