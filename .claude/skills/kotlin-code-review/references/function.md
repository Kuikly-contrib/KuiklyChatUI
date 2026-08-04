# 7、函数

**tl;dr**

- 在 Kotlin 中，函数是一等公民
- **应当**使用 require 和 check 强制执行函数契约，确保输入和内部逻辑符合预期
- **应当**利用具名参数提升代码可读性，尤其是在参数较多、类型相同或包含布尔标志时
- **应当**始终保持操作符行为与命名一致，避免误导和歧义
- **应当**正确理解高阶函数的内联行为，优化性能和设计
- **应当**正确理解 SAM 转换简化与 Java 函数式接口的交互

## 使用 require 和 check 来强制执行函数契约

Kotlin的一个核心原则：尽早失败 (Fail-Fast)，主动检查并明确声明你的期望，这是一种“攻击性编程”策略。目的是让代码中的缺陷（如无效参数或错误状态）立即暴露，而不是被隐藏起来，导致后期难以调试的 bug。 Kotlin 提供了 `require` 和 `check` 方法用于强制执行这些契约。

**示例 1：使用 `require` 用于检查函数输入的前置条件。**

如果前置条件不满足，则抛出 `IllegalArgumentException`

```kotlin
fun setAge(age: Int) {
    require(age > 0) { "Age must be positive" }
}

// 调用时
setAge(-5) // 抛出 IllegalArgumentException: Age must be positive
```

**示例 2：使用 `check` 用于检查函数内部的逻辑约束。**

如果内部逻辑约束不满足，则抛出 `IllegalStateException`

```kotlin
fun process(data: List<Int>) {
    check(data.isNotEmpty()) { "Data cannot be empty" }
}

// 调用时
process(emptyList()) // 抛出 IllegalStateException: Data cannot be empty
```

## 考虑使用具名参数

具名参数是 Kotlin 的重要特性之一，为函数调用提供了更高的可读性和安全性。以下场景中建议使用具名参数：

**示例 1：多个同类型参数**

当函数包含多个同类型参数时，具名参数可以避免顺序混淆。

```kotlin
// 不建议：容易混淆参数含义
resizeImage(1024, 768, 1.5)

// 建议：使用具名参数明确参数含义
resizeImage(width = 1024, height = 768, scaleFactor = 1.5)
```

**示例 2：布尔标志参数**

布尔值本身不携带语义信息，具名参数可以明确标志的作用。

```kotlin
// 不建议：true 的含义不明确
saveDocument(document, true)

// 建议：使用具名参数明确意图
saveDocument(document = document, overwrite = true)
```

**示例 3：带有默认值的参数**

具名参数允许覆盖特定的默认值，而无需按照位置传递所有参数。

```kotlin
fun formatText(text: String, bold: Boolean = false, italic: Boolean = false, color: String = "black") { ... }

// 不建议：必须按顺序传递前面所有默认参数才能修改后面的（此处想改 color, 但必须指定 bold 和 italic）
formatText("Hello", false, false, "blue") // 冗长且意图不直接

// 建议：只覆盖需要修改的参数
formatText("Hello", color = "blue")
```

**示例 4：参数列表较长时**

当参数数量较多时，具名参数可显著提升代码的可读性。

```kotlin
// 不建议：长参数列表，不使用具名参数，可读性差
val user = User(userId, "Alice", "Smith", "alice.smith@example.com", LocalDate.now(), true)

// 建议：长参数列表，使用具名参数提升可读性 (IDE 会自动格式化)
val user = User(
    id = userId,
    firstName = "Alice",
    lastName = "Smith",
    email = "alice.smith@example.com",
    registrationDate = LocalDate.now(),
    isVerified = true
)
```

**示例 5：函数类型的参数**

当函数有多个函数类型的参数（尤其是它们都有默认值或者意义不同）时，必须使用具名参数来明确每个 lambda 表达式的职责，避免因位置混淆而导致的逻辑错误。

```kotlin
// 函数类型的多个可选参数特别容易让人混淆：
fun call(before: () -> Unit = {}, after: () -> Unit = {}) {
   before()
   print("Middle")
   after()
}

// 反例：传递单个函数类型参数时位置歧义
call({ print("CALL") }) // 作为第一个参数 before 还是第二个参数 after？结果可能是 CALLMiddle 或 MiddleCALL？
call { print("CALL") } // 这种写法是传递第二个参数 after（结果为 MiddleCALL），但意图可能被误解

// 正例：明确指定要传递哪个函数参数
call(before = { print("CALL") }) // 输出 CALLMiddle
call(after = { print("CALL") }) // 输出 MiddleCALL
```

注意：当函数最后一个参数是函数类型，且该 `lambda` 是调用主要目的时（如 `repeat`, `thread`, `with`），可以使用尾随 `lambda` 语法省略参数名和括号，这是 Kotlin 的惯用写法。

**小结**
利用 Kotlin 的具名参数 `Named Arguments` 特性，在调用函数时显式指定参数名，能显著提升代码可读性、降低认知负荷和防止错误。

在以下场景中强烈推荐使用具名参数：
- 多个同类型参数：消除因顺序混淆而导致的 bug。
- 布尔标志参数：`createWindow(isVisible = true)` 远比 `createWindow(true)` 清晰。
- 带有默认值的参数：清晰地表明你正在覆盖哪个默认值。
- 参数列表较长时：提高可读性和可维护性。
- 函数类型的参数

## 操作符的行为应该与其名称一致

操作符的行为必须与其名称保持一致，这是 Kotlin 设计的核心原则之一。错误的操作符重载会使代码产生歧义，降低可读性和可维护性。

**反例**

意义不明确的操作符重载，以下是一个关于 `times()` 操作符的模糊用法：

```kotlin
// 反例：意义不明确的操作符重载
operator fun Int.times(operation: () -> Unit): () -> Unit = {
      repeat(this) { operation() }
   }

val tripledHello = 3 * { print("Hello") }
tripledHello() // 输出: HelloHelloHello
```

在上例中，`times()` 操作符被定义为生成一个新的函数，该函数可以被调用三次。然而，对一些开发者来说，这种用法可能不符合直觉，容易误认为 `3 * {}` 是立即调用该函数三次。

**正例**

使用更具描述性的函数名，并避免滥用操作符。

```kotlin
// 使用带描述性的中缀函数
infix fun Int.timesRepeated(operation: () -> Unit): () -> Unit = {
      repeat(this) { operation() }
   }

val tripledHello = 3 timesRepeated { print("Hello") }
tripledHello() // 输出: HelloHelloHello
```

或者直接使用 Kotlin 标准库提供的 `repeat` 函数：

```kotlin
repeat(3) { print("Hello") } // 输出: HelloHelloHello
```

**小结**

操作符的行为应该与其名称保持一致，避免引发误解或歧义。对于复杂的逻辑，推荐使用更具描述性的方法名而非重载操作符。

## 正确理解高阶函数的内联行为

高阶函数允许将函数作为参数或返回值，但会引入一定的性能开销。Kotlin 提供了 `inline` 关键字优化高阶函数，消除多余的对象分配和函数调用开销。

### 高阶函数的性能问题

高阶函数会在运行时引入额外的性能开销，包括：
- Lambda 对象的分配：每次调用高阶函数时，`lambda` 表达式会被编译为 `FunctionX` 的匿名类或实例。
- 间接方法调用：`lambda` 的调用需要通过匿名类的 `invoke()` 方法，性能不如直接调用函数。
- 函数调用栈的开销：高阶函数本质上是函数调用，频繁调用时会增加调用栈的深度。

例如，以下代码中 `performOperation` 是一个高阶函数，接收一个 `(Int) -> Int` 类型的函数作为参数：

```kotlin
fun performOperation(x: Int, operation: (Int) -> Int): Int {
   return operation(x)
}

val result = performOperation(5) { it * it }
println(result) // 输出 25
```

在编译后，`lambda` 会被编译为匿名类，类似以下代码：

```kotlin
val operation = object : Function1<Int, Int> {
   override fun invoke(x: Int): Int = x * x
}
val result = performOperation(5, operation)
```

**示例 1：内联函数的优化**

通过将高阶函数声明为 `inline`，可以在编译时将函数的代码直接嵌入到调用处，消除上述性能开销：

```kotlin
inline fun performOperation(x: Int, operation: (Int) -> Int): Int {
   return operation(x)
}

val result = performOperation(5) { it * it }
```

编译后，代码会被直接替换到调用处，类似：

```kotlin
val result = run {
   val x = 5
   x * x
}
```

**示例 2：使用 `noinline` 控制内联行为**

默认情况下，内联函数的所有 `lambda` 参数都会被内联，但某些 `lambda` 参数可能不适合内联，使用 `noinline` 可避免该参数被内联：

```kotlin
inline fun performOperation(
   x: Int,
   operation: (Int) -> Int,
   noinline logOperation: (Int) -> Unit
): Int {
   logOperation(x) // 这个 lambda 不会被内联
   return operation(x) // 这个 lambda 会被内联
}
```

**示例 3：使用 `crossinline` 防止非局部返回**

`crossinline` 用于防止 `lambda` 参数的非局部返回。例如，在多线程场景中，`lambda` 中的 `return` 可能会导致不可预期的行为，`crossinline` 可以强制 `lambda` 只能返回到自身内部：

```kotlin
inline fun performOperation(x: Int, crossinline operation: (Int) -> Unit) {
   Thread {
      operation(x) // 此处运行 lambda，不能非局部返回
   }.start()
}
```

## 正确理解 SAM 转换的类型

在 Kotlin 中，SAM（Single Abstract Method） 是一种简化使用 Java 函数式接口的机制。Kotlin 提供了对 SAM 转换的支持，使得我们可以通过 `lambda` 表达式直接实现单抽象方法接口，而无需显式创建匿名类。正确理解 SAM 转换的类型可以帮助我们更高效地与 Java 代码交互。

### 函数类型的 Lambda Listener 的注册与反注册

在监听器的注册和反注册场景中，通常需要保证注册和反注册操作使用的是同一个对象。如果 Lambda 被包装成一个新的对象，那么反注册时传入的 Lambda 就会和之前注册时的包装对象不一致，从而导致反注册失败，最终可能引发 内存泄漏。

**反例：注册与反注册对象不一致**

```kotlin
// Java 接口
interface Listener {
   void onEvent();
}

// Java 提供的注册和反注册方法
class EventSource {
   private final List<Listener> listeners = new ArrayList<>();

   public void addListener(Listener listener) {
      listeners.add(listener);
   }

   public void removeListener(Listener listener) {
      listeners.remove(listener);
   }
}

// Kotlin 中的代码
val eventSource = EventSource()

// 注册监听器
eventSource.addListener { println("Event occurred!") }

// 试图反注册
eventSource.removeListener { println("Event occurred!") }  // 移除的是另外的包装对象，无法成功反注册
```

**正例：显式将 Listener 对象保存为变量**

避免直接使用 Lambda，而是显式保留包装后的 Listener 对象。这样可以确保注册和反注册时使用的是同一个对象。

```kotlin
val eventSource = EventSource()

// 显式保存 Listener 对象
val listener = Listener { println("Event occurred!") }

// 注册监听器
eventSource.addListener(listener)

// 反注册监听器
eventSource.removeListener(listener)  // 成功反注册
```

**小结**

正确处理 SAM 转换，确保注册与反注册对象的一致性，避免潜在内存泄漏。

## 扩展阅读
1. **Kotlin 官方文档**
   - [Kotlin Docs: Functions](https://kotlinlang.org/docs/functions.html)
   - [Kotlin Docs: Named Arguments](https://kotlinlang.org/docs/functions.html#named-arguments)
   - [Kotlin Docs: Inline Functions](https://kotlinlang.org/docs/inline-functions.html)
   - [Kotlin Docs: SAM Conversions](https://kotlinlang.org/docs/java-interop.html#sam-conversions)
2. **Effective Kotlin: Best Practices by Marcin Moskała**
   - Item 5: Specify your expectations for arguments and state
   - Item 11: An operator’s meaning should be consistent with its function name
   - Item 13: Consider making types explicit
   - Item 14: Consider referencing receivers explicitly
   - Item 17: Consider naming arguments
   - Item 51: Use the inline modifier for functions with parameters of functional types


## 总结
在 Kotlin 中，函数设计的最佳实践包括强制执行函数契约、使用具名参数提升可读性、保持操作符行为一致、使用内联关键字优化高阶函数性能以及正确处理 SAM 转换的类型。
