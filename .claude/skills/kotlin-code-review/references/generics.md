# 8、泛型

**tl;dr**

- **应当** 正确理解泛型类型擦除，掌握泛型在编译时的作用和运行时的行为
- **应当** 注意基本类型作为泛型参数时的装箱行为
- **建议** 在合适的场景下使用类型实化避免内联函数的泛型类型擦除
- **应当** 使用泛型类型型变确保容器类型的安全性

## 正确理解泛型类型擦除

Kotlin 编译器会将泛型类型参数替换为它的上界（Upper Bound），使得程序在运行时无法直接获取原始的泛型类型信息。默认情况下，泛型参数 `T` 会被擦除为 **`Any?`**。如果指定了上界（如 `T : Number`），则 `T` 擦除为指定的上界（`Number`）。

**示例：泛型类型重载导致符号冲突**

由于存在类型擦除，`List<Int>` 和 `List<String>` 在编译后实际上是同一个类型 `List`。这导致使用泛型类型作为重载函数参数时会发生签名冲突。如代码所示：

```Kotlin
fun foo(list: List<Int>) { ... }

fun foo(list: List<String>) { ... }
```

上述代码在 Kotlin JVM 平台上编译报错如下：

```
Platform declaration clash: The following declarations have the same JVM signature (foo(Ljava/util/List;)V): fun foo(list: List<Int>): Unit defined in root package fun foo(list: List<String>): Unit defined in root package
```

如果必须根据泛型类型重载，则需通过 `@JvmName` 注解明确告知编译器生成不同的 JVM 方法签名，如代码所示：

```Kotlin
@JvmName("fooInt")
fun foo(list: List<Int>) { ... }

@JvmName("fooString")
fun foo(list: List<String>) { ... }
```

**小结**

泛型允许开发者对类型进行抽象，实现更加灵活的代码封装。编译器通常情况下可以在编译时依据泛型类型信息提供足够的类型检查，确保代码的正确性。不过，深入理解泛型擦除对运行时的影响，有助于编写出更加稳健高效的代码。


## 注意基本类型的装箱行为

当 Kotlin 的基本类型（如 `Int`、`Double`）用作泛型类型参数时，它们必须被**装箱**成对应的 Java 包装类，以满足泛型在 JVM 上的实现要求。装箱会带来额外的**内存分配**和**性能开销**。


**示例 1：常见泛型参数的装箱**

Kotlin 从语法上隐藏了基本类型的装箱行为，但这不代表装箱不存在。例如：

```kotlin
class Stack<T> {
    fun push(value: T) { ... }
    fun pop(): T = ...
    ...
}
```

`Stack<T>` 在编译之后等价于 `Stack<Any>`，下面的代码将会发生基本类型装箱：

```kotlin
val stack = Stack<Int>()
stack.push(1) // 常量 1 被装箱

val value: Int = 100
stack.push(value) // value 被装箱
```

如果遇到装箱引起的性能瓶颈，可考虑对基本类型提供专门的实现以减少装箱带来的开销，例如：

```kotlin
class IntStack {
    fun push(value: Int) { ... }
    fun pop(): Int = ...
    ...
}
```

**示例 2：内联函数泛型参数的装箱**

内联函数的泛型参数同样会引起装箱。例如：

```Kotlin
val threshold: Int = ...

val result = calculateValue().let {
    if (it > threshold) it * 2 else it
}
```

理论上，`let` 函数在内联之后会产生以下结果：

```Kotlin
val threshold: Int = ...
val value: Int = calculateValue()

val result = if (value > threshold) value * 2 else value
```

不过，在 Kotlin Native 中，情况并非如此。实际结果等价于：

```kotlin
val threshold: Int = ...
val value: Any? = <Int-box>(calculateValue())

val it: Int = <Int-unbox>(value)
val result = if (it > threshold) it * 2 else it
```

在性能敏感的代码中，应当避免在基本类型上使用 `let`、`also` 等这些看上去免费的内联函数。

**小结**

泛型引起的基本类型装箱非常隐蔽，应当时刻小心基本类型用作泛型参数的场景。

## 使用类型实化避免内联函数的泛型类型擦除

通过在内联函数中使用 **`reified`** 关键字标记泛型类型参数，可以实化（Reification）该类型。这意味着在编译时，编译器会将该泛型参数替换为实际的类型，使得在运行时可以像操作非泛型类型一样获取其 `KClass` 对象。

**反例**

由于类型擦除，在普通函数或非内联函数中，无法获取泛型 `T` 的 `KClass` 对象。

```Kotlin
// 报错：Cannot use 'T' as reified type parameter. Use a class instead.
fun <T> isInstanceOf(value: Any): Boolean {
    // 无法直接使用 T::class 进行类型检查
    // return value is T 也是不被允许的
    return value::class == T::class
}
```

**正例**

在内联函数中使用 `reified` 修饰泛型参数 `T`，使得我们可以在函数体内使用 `T::class` 或 `is T` 进行运行时类型检查。

```Kotlin
// 使用 reified 实化类型，类型信息在内联时被替换，运行时可见
inline fun <reified T> isInstanceOf(value: Any): Boolean {
    // 现在可以在运行时安全地使用 'is' 或 'T::class'
    return value is T 
}

fun main() {
    val result = isInstanceOf<String>("Hello") 
    println("Is String? $result") // 输出: Is String? true
}
```

**扩展示例**

泛型参数类型实化的内联函数不会引起基本类型装箱。

在 8-2 节的示例中，在 Kotlin Native 平台上，`let` 函数会引起 `Int` 类型装箱。如果调用是泛型参数类型实化的内联函数，如代码所示：

```Kotlin
// 注意 T 是实化的类型
inline fun  <reified T, R> T.let2(block: (T) -> R): R {
    return block(this)
}

...
val threshold: Int = ...

val result = calculateValue().let {
    if (it > threshold) it * 2 else it
}
```

调用 `let2` 不会引起 `calculateValue()` 的返回值装箱。

**小结**

对内联函数的泛型参数类型实化，可获得模板化泛型实现的效果。


## 使用泛型类型型变确保容器类型的安全性

**型变（Variance）** 用于描述泛型参数的类型变化对泛型类型自身产生的影响。Kotlin 使用 **`out` （协变）** 和 **`in` （逆变）** 关键字来声明泛型类型型变，缺省时默认为**不可变（Invariant）**。

**协变示例**

使用 `out` 关键字声明泛型参数 `T`，表示 `T` **只能出现在“输出”位置**（例如函数返回类型）。

```Kotlin
// 协变：T 只能作为输出（生产者）
interface Producer<out T> {
    fun produce(): T // 只能作为输出（返回类型）
    // fun consume(item: T) // 编译报错：T 出现在了输入位置
}

fun main() {
    val strProducer = object : Producer<String> {
        override fun produce() = "Hello"
    }
    // 允许：Producer<String> 是 Producer<Any> 的子类型 (安全，因为只能取出)
    val anyProducer: Producer<Any> = strProducer
    println(anyProducer.produce())
}
```

**逆变示例**

使用 `in` 关键字声明泛型参数 `T`，表示 `T` **只能出现在“输入”位置**（例如函数参数类型）。

```Kotlin
// 逆变：T 只能作为输入（消费者）
interface Consumer<in T> {
    fun consume(item: T) // 只能作为输入（参数类型）
    // fun produce(): T // 编译报错：T 出现在了输出位置
}

fun main() {
    val anyConsumer: Consumer<Any> = object : Consumer<Any> {
        override fun consume(item: Any) = println("Consuming $item")
    }
    // Consumer<Any> 是 Consumer<String> 的子类型 (安全，因为 Any 消费者总能消费 String)
    val strConsumer: Consumer<String> = anyConsumer
    strConsumer.consume("World")
}
```

**小结**

掌握泛型型变是灵活运用 Kotlin 泛型的关键。

## 扩展阅读

1. Kotlin 官方文档
   - [Generics: in, out, where](https://kotlinlang.org/docs/generics.html)

## 总结

Kotlin 泛型提供了强大的**类型抽象**能力，开发者必须充分理解其类型擦除机制，并掌握使用内联函数的泛型参数实化在特定场景下获得模板化泛型实现的效果。同时，对于性能敏感的代码，要警惕基本类型作为泛型参数时产生的**装箱**开销。此外，运用泛型型变，可使得泛型类型更加安全。
    
正确使用泛型，能显著提高代码的**类型安全性**、**灵活性**和**复用性**。
