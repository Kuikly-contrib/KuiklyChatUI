# 2、类型

**tl;dr**

- **应当**使用 `T?`、`?.`、`?:`、`智能转换` 等处理空值
- **不应当**使用 `!!` 非空断言
- **应当**消除平台类型的风险，明确跨语言交互的可空性
- **应当**使用类型别名 `typealias` 简化复杂类型声明
- **应当**在类型不明确时显式声明变量类型
- **应当**公共 API 避免暴露需要推断的类型

## 妥善处理空值

Kotlin 的空安全机制通过编译期检查，将运行时错误转化为类型错误，从根本上减少了空指针异常的可能性。

### 类型系统的区分：可空类型 T? 与非空类型 T

Kotlin 空安全机制的核心在于其类型系统对可空性的明确区分。与 Java 不同，Kotlin 中的任何类型**默认都是非空的** (`Non-Nullable`)。

```kotlin
var name: String = "Alice" // 必须持有 String 实例
name = null // 编译错误！
```

如果一个变量确实需要能够持有 `null` 值，开发者必须显式地在其类型声明后加上问号 (`?`)，将其标记为**可空类型** (`Nullable`)。

```kotlin
var address: String? = "123 Main St"
address = null // 合法
```

这种在编译期就强制区分可空性的设计，是 Kotlin 相比于 Java 基于注解的空安全方案（如 `@Nullable`）的根本优势。它将空指针检查的责任从开发者的运行时纪律转变为编译器的静态保证。

### 安全调用操作符 ?.

安全调用操作符 (`?.`) 是处理可空类型时最常用、最核心的工具。它将一次空值检查和一次方法调用合并为一个简洁的操作。如果接收者为 `null`，则整个表达式立即停止执行并返回 `null`。

它支持链式调用，极大地提升了代码的简洁性和可读性，有效地替代了 Java 中常见的冗长、嵌套的 `if-else` 检查。

**反例**

```kotlin
var country: String? = null
// java 风格的赋值代码
if (person != null) {
   val company = person.company
   if (company != null) {
      val address = company.address
      if (address != null) {
         country = address.country
      }
   }
}
```

**正例**

```kotlin
// 只有当 person、company 和 address 都不为 null 时，才会返回 country
val country = person?.company?.address?.country
```

### Elvis 操作符 ?:

Elvis 操作符 (`?:`) 为处理 `null` 值提供了优雅的备选方案。当其左侧表达式的结果为 `null` 时，它会返回右侧的备选值。

```kotlin
// 如果 name 不为 null，则返回 name；否则返回 "Unknown"
val displayName = name ?: "Unknown"
```

更强大的是，`return` 和 `throw` 在 Kotlin 中都是表达式，可以被用在 Elvis 操作符的右侧，成为实现前置条件检查的强大工具。

```kotlin
fun printShippingLabel(person: Person) {
   // 如果 company 或 address 为 null，则抛出异常
   val address = person.company?.address ?: throw IllegalArgumentException("No address")
   // 在这里，address 被智能转换为非空类型
   println(address.streetAddress)
}
```

### 智能转换 Smart Casts 与安全转换 as?

当开发者通过 `if (x != null)` 显式地检查了一个可空变量后，编译器会在该检查的作用域内自动将这个变量视为其对应的**非空类型**，无需手动转换，这就是**智能转换**。

```kotlin
fun printLength(s: String?) {
    if (s != null) {
        // 在这个代码块内，s 被智能转换为 String 类型
        println(s.length)
    }
}
```

与此相辅相成的是**安全转换操作符 `as?`**。标准的 `as` 在转换失败时会抛出异常，而 `as?` 在转换失败时则会返回 `null`。

```kotlin
// 在实现 equals 方法时，这是一个非常常见的模式
override fun equals(other: Any?): Boolean {
    val otherPerson = other as? Person ?: return false
    //... 比较属性
    return true
}
```

### 强烈不建议使用 非空断言 !!

在 Kotlin 的工具箱中，非空断言 (`!!`) 是一个特殊的存在。它是一个危险的“逃生舱口”，其设计本身就带有警示意味。

**反例**

使用非空断言 `!!`，它会破坏编译期的安全保证，将空指针风险重新引入到运行时。

```kotlin
  // 如果 person 或其 address 为 null，将在运行时抛出 NullPointerException
val city: String = person!!.address!!.city
```

**正例**

优先使用安全调用 `?.` 和 Elvis 操作符 `?:` 来流畅地处理潜在的 `null`。

  ```kotlin
  // 如果 person 或其 address 为 null，则返回 "Unknown"
val city: String = person?.address?.city ?: "Unknown"
  ```

### 定义与机制

`!!` 操作符用于将任何可空类型的值强制转换为其对应的非空类型。如果在运行时该值为 `null`，它会立即抛出`KotlinNullPointerException`。这相当于开发者向编译器声明：“我确信此值不为 null，并愿意承担运行时崩溃的风险。”

### 为什么应该避免使用 !!

权威观点普遍将 `!!` 视为一种**代码异味 (code smell)** 和反模式，原因如下：

- **重新引入空指针异常**：它将 Kotlin 在编译期解决的问题——不可预测的运行时 空指针异常——又重新引入了进来。
- **掩盖上下文信息**：它抛出的是一个通用的 `KotlinNullPointerException`，难以追踪错误的根源。
- **调试困难**：在链式调用中（如 `a!!.b!!.c`），一旦发生异常，堆栈跟踪无法明确指出是 `a`、`b` 还是 `c` 为`null`。
- **语法警示**：其语法（两个感叹号）被刻意设计得“有些粗鲁”，目的是“促使你寻找更好的解决方案”。

**小结**

- 使用可空类型 `T?` 明确变量的可空性。
- 使用安全调用操作符 `?.` 和 Elvis 操作符 `?:` 简化空值检查。
- 避免使用非空断言 `!!` ，优先选择更安全的替代方案。

## 使用 Type Aliases 来简化复杂类型定义

在 Kotlin 中，类型别名 (Type Aliases) 提供了一种为现有类型引入替代名称的机制。它们并不会创建新的类型，但可以为复杂或冗长的类型提供更简洁的表达方式，从而提高代码可读性和可维护性。

**示例 1：简化长泛型类型**

当泛型类型过长或复杂时，使用类型别名可以显著提高代码的可读性。

```kotlin
// 定义类型别名
typealias NodeSet = Set<Network.Node>
typealias FileTable<K> = MutableMap<K, MutableList<File>>

// 使用类型别名
val nodes: NodeSet = setOf(Network.Node())
val fileMappings: FileTable<String> = mutableMapOf("key" to mutableListOf(File("example.txt")))
```

**示范 2：函数类型的别名**

函数类型的声明通常较为复杂，类型别名可以为其提供更直观的表示方式。

```kotlin
// 定义类型别名
typealias MyHandler = (Int, String, Any) -> Unit
typealias Predicate<T> = (T) -> Boolean

// 使用类型别名
val handler: MyHandler = { code, message, data ->
   println("Code: $code, Message: $message, Data: $data")
}

val isPositive: Predicate<Int> = { it > 0 }
println(isPositive(42)) // 输出：true
```

**示例 3：嵌套类的别名**

在访问深层次的嵌套类时，类型别名可以减少冗长的路径声明。

```kotlin
// 定义嵌套类
class A {
   inner class Inner
}
class B {
   inner class Inner
}

// 定义类型别名
typealias AInner = A.Inner
typealias BInner = B.Inner

// 使用类型别名
val aInner: AInner = A().Inner()
val bInner: BInner = B().Inner()
```

### 局限性与注意事项

尽管类型别名能够提升代码的可读性，但它们不会创建新的类型，仅仅是现有类型的替代名称。这意味着类型别名无法用于区分那些语义不同但结构相同的类型。例如：

```kotlin
typealias UserId = String
typealias SessionId = String

fun process(id: UserId) { /* ... */ }

val sessionId: SessionId = "session_123"
process(sessionId) // 语义上不对，但编译器不会报错
```

此外，类型别名的过度使用可能导致类型定义混乱，尤其是顶层类型别名的滥用。建议将类型别名的使用限制在模块内部，避免影响代码的全局理解。

**小结**

`typealias` 是 Kotlin 用于为现有类型创建别名的功能，主要用来简化复杂类型的声明，增强代码的可读性和表达力。合理使用 typealias 能让代码更清晰，但过度使用可能导致混淆，因此需适度应用，以在简洁与可维护性之间找到平衡。

## 尽可能消除平台类型

平台类型是来自另一种语言并具有未知可空性的类型。 Kotlin 的空安全机制的目标就是消除空指针异常，但是 Kotlin 又同时提供了平台类型，它在一定程度上破坏了 Kotlin 的空安全机制，让开发者们无法直接利用空安全机制，来编写安全的代码。 事实上，平台类型的危害不仅于此：它们还破坏了 Kotlin 与 Java、原生代码以及 JavaScript 之间的交互，这些交互中都有可空类型的信息，而平台类型没有可空类型的信息。 因此，在与另一种语言交互时，如果目前语言代码没有明确声明可空/非空，我们尽量补充上，例如在 Java 的方法声明里通过`@Nullable` 和 `@NonNull `注解指定可空/非空。 这样可以充分利用空安全机制，编写安全的代码。如果另一种语言的代码我们没有权限修改，此时我们应该分析我们调用的代码是否一定会返回非空值，如果不可能返回空值，则使用非空类型接收返回值，否则使用可空类型接收返回值。这样如果我们声明了非空类型，即使后来实现改了，返回了空，也可以快速定位是因为调用了另一种语言的方法返回了空，不会将空指针问题扩散到其他地方，降低了分析和修改难度。

**反例**

**与 Java 交互，没有显式添加 `@Nullable` 和 `@NonNull` 注解**

```kotlin
// Java 代码（无注解）
public String processInput() { /* 可能返回null */ }

// Kotlin 危险用法
fun handleInput() {
   val input = javaService.processInput()  // String! 平台类型

   // 危险：直接当作非空类型使用
   println(input.length)  // 运行时可能有空指针异常

   // 平台类型传播
   saveToCache(input)  // 污染其他代码区域
}

fun saveToCache(data: String) {  // 错误假设非空
   cache.save(data)  // 此处仍可能有空指针异常
}
```

**正例**

**Java 代码里添加空安全注解**

```kotlin
// Java 代码（带注解）
@NonNull
public String getCustomerName() { /*...*/ }

// Kotlin 安全调用
val name: String = javaService.customerName  // 明确声明非空类型
showName(name)  // 安全调用无需空检查

// 当 Java 可能返回 null 时
@Nullable
public String getDisplayName() { /*...*/ }

// Kotlin 处理可空类型
val displayName: String? = javaService.displayName
showName(displayName ?: "Unknown")  // Elvis 操作符处理空场景
```

**2. Java 代码里没有空安全注解，在 Kotlin 里明确指定类型**

当没有权限修改 Java 代码时，根据对 Java 代码逻辑的分析，确定 Java 方法不会返回空值，我们也可以使用非空类型来声明，例如下面的
Java 方法，调用者传入的 `customerName` 不可能为 `null`。所以 Kotlin 声明 name 变量为非空 String

```kotlin
public String getCustomerName() { /*...*/ }

// Kotlin安全调用
val name: String =
    javaService.customerName  // 明确声明非空类型，即使以后Java代码有修改，导致了空指针异常，也可以快速定位，将这里声明改为可空，其他地方修改后可以继续安全使用
showName(name)  // 安全调用无需空检查
```

## 在变量不清晰时指定其类型

Kotlin的类型系统在大多数场景足够智能，但在许多场景里需要我们去显式声明变量的类型，例如以下的场景。

**示例 1：集合多重嵌套**

当集合嵌套层级较深时，无法直观看出每一层集合的泛型类型

```kotlin
private val ipStrategyMap = hashMapOf<String, DownloadIpStrategy>()
// 反例：类型推断困难，建议显式声明
val entries = listOf(ipStrategyMap.entries)
// 正例：显式声明完整嵌套类型
val entries: List<Set<Map.Entry<Int, DownloadIpStrategy>>> = listOf(ipStrategyMap.entries)
```

**示例 2：复杂表达式或链式调用**

当表达式包含类型转换、作用域函数或复杂计算时，显式类型可避免歧义。

```kotlin
// 反例：复杂表达式无法直观看出计算结果的类型
val result = data?.let { it.transform() } ?: fallback()

// 正例：显式声明返回值类型
val result: String = data?.let { it.transform() } ?: fallback()
```

**示例 3：Lambda参数的类型声明**

当Lambda参数类型无法推断时需明确声明，尤其在多参数场景。

```kotlin
// 反例：多参数Lambda类型模糊
list.zip(otherList) { a, b -> ... }  // a,b类型可能不明确

// 正例：显式声明参数类型
list.zip(otherList) { a: TypeA, b: TypeB -> ... }
```

**小结**

在类型不明确时，应显式声明变量的类型。

## 不要暴露需要推断的类型

在公共 API（如类的属性或函数返回值）中，避免使用不明确的类型推断（如 `Any`、`Unit?` 或隐式返回值）来让使用者猜测或推断实际类型。与前面的章节《在变量不清晰时指定其类型》相似的是，这里探讨的是属性以及函数的类型声明问题。

**反例**

```kotlin
class DataProcessor {
   // 类型推断为 String，但暴露了推断过程
   val name = "processor_1"

   // 返回值类型推断为 Int，但不明确
   fun getIndex() = 2

   // 返回值写成 Any，类型信息丢失
   fun processData(input: String): Any {
      return input.split(",")
   }

   // 返回值写成 Unit? ，意义不明
   fun sendData(): Unit? {
      return if (shouldSend) {
         doWork()
         null // 成功返回 null
      } else {
         Unit // 未执行返回 Unit
      }
   }
}
```

虽然 Kotlin 可以推断类型，但在公共 API 中，这种写法隐藏了实际类型，可能导致阅读代码时不直观，或者在类型发生变更时出现问题。

**正例**

```kotlin
class Person {
   // 类型明确为 String
   val name: String = "processor_1"

   // 返回值类型明确为 Int
   fun getIndex(): Int {
      return 25
   }

   // 返回值明确为 List<String>
   fun processData(input: String): List<String> {
      return input.split(",")
   }

   // 使用密封类表示状态
   sealed class SendResult {
      object Success : SendResult()
      object Skipped : SendResult()
   }

   // 返回值明确为 密封类
   fun sendData(): ProcessResult {
      return if (shouldSend) {
         doWork()
         SendResult.Success // 明确表示成功状态
      } else {
         SendResult.Skipped // 明确表示未执行状态
      }
   }
}
```

**小结**

避免暴露需要推断的类型尤其重要在以下情况：

- 返回值类型复杂或可能改变。
- 返回值意义不明，如 `Unit?`
- 返回值涉及泛型（如 `List`、`Map`）。
- 返回值为 `null` 或 `Any`，容易丢失类型信息。

通过显式声明返回类型，可以让代码更安全、更清晰，减少意外错误。

## 扩展阅读

1. **Kotlin 官方文档**
   - [Null safety](https://kotlinlang.org/docs/null-safety.html)
   - [Nullable values and null checks](https://kotlinlang.org/docs/basic-syntax.html#nullable-values-and-null-checks)
   - [Smart casts](https://kotlinlang.org/docs/typecasts.html#smart-casts)
   - [Null-safety and platform types](https://kotlinlang.org/docs/java-interop.html#null-safety-and-platform-types)
2. **Effective Kotlin: Best Practices by Marcin Moskała**
  - Item 13: "Consider making types explicit"
  - Item 14: "Consider referencing receivers explicitly"

## 总结

Kotlin 的类型系统不仅是一种工具，更是一种设计理念。通过显式类型定义和安全的空值处理，开发者能够专注于编写安全、简洁的代码。同时，通过合理使用显式类型、类型别名和空安全注解，开发者可以进一步增强代码的可维护性和跨语言交互的安全性。
