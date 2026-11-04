# 1、变量

**tl;dr**

- **应当**在定义属性时，应优先使用 `val` 而不是 `var` 定义属性，避免不必要的可变性
- **应当**最小化变量的作用域，提高代码的清晰度
- **应当**针对高开销的初始化场景，使用 `by lazy` 实现属性的延迟初始化
- **应当**谨慎使用 `lateinit`，禁用 `isInitialized`

## 优先使用 val 而不是 var 定义属性

`val` 定义的变量是不可变的，而 `var` 定义的变量是可变的。优先使用 `val` 是减少可变性的基础。

```kotlin
val a = 10 // 不可变属性
a = 20 // 编译错误 "Val cannot be reassigned"
```

请注意，`val` 表示变量引用不可变，但引用指向的对象可能是可变的：

```kotlin
val list = mutableListOf(1, 2, 3)
list.add(4) // 合法，list 的引用不可变，但内容可变
```

**小结**

尽量使用 val 定义变量，减少可变性，可以显著提高代码的安全性与可维护性。

## 最小化变量作用域

最小化变量的作用域是编程中的一个重要原则。变量的作用域越小，代码越容易理解，出错的概率也越低。

**反例**

**1：变量作用域过大（在整个函数中声明变量）**

```kotlin
fun calculateSum(numbers: List<Int>): Int {
   var sum = 0  // 提前声明变量，作用域覆盖整个函数
   if (numbers.isNotEmpty()) {
      for (number in numbers) {
         sum += number
      }
   }
   return sum
}
```

`sum` 的作用域覆盖了整个函数，但实际上我们只在 `if` 块和 `for` 循环中使用它。这种设计容易让人错误地认为`sum` 可能在其他地方被使用，增加了代码复杂度。

**2：在类范围内声明全局变量**

```kotlin
class Calculator {
   var result = 0  // 类级别变量，作用域过大

   fun add(a: Int, b: Int): Int {
      result = a + b
      return result
   }

   fun subtract(a: Int, b: Int): Int {
      result = a - b
      return result
   }
}
```

在此示例中，`result` 的作用域覆盖了整个类，而实际上它仅在方法中使用。由于多个方法共享同一个变量，可能导致意外的状态污染，尤其是在并发场景下，这种设计可能引发线程安全问题。

**正例**

**1：将变量作用域限制在使用它的最小范围内**

```kotlin
fun calculateSum(numbers: List<Int>): Int {
   if (numbers.isNotEmpty()) {
      var sum = 0  // 将变量声明移到最小使用范围内
      for (number in numbers) {
         sum += number
      }
      return sum
   }
   return 0
}
```

**2：将变量限制在方法内部**

```kotlin
class Calculator {
   fun add(a: Int, b: Int): Int {
      val result = a + b  // 方法内局部变量
      return result
   }

   fun subtract(a: Int, b: Int): Int {
      val result = a - b  // 方法内局部变量
      return result
   }
}
```

**小结**

始终在尽可能小的作用域内定义变量，优先使用局部变量而非类属性。

## 使用 by lazy 延迟初始化，优化变量定义

在以下场景考虑使用 `by lazy` 延迟初始化变量。

- 高开销初始化：如数据库连接、文件读取。
- 单例模式：确保全局唯一实例。
- 条件性初始化：仅在需要时才创建对象。

**反例**

以下代码在对象创建时立即初始化变量，即使该变量可能并未被使用：

```kotlin
class Service {
   val config = loadConfiguration() // 对象初始化时就初始化变量
}
```

**正例**

通过 `by lazy` 实现延迟初始化，变量在首次访问时才会被实际初始化。

```kotlin
val config by lazy { loadConfiguration() }  // 惰性初始化，访问时才进行初始化
```

**小结**

在需要高开销或条件性初始化的场景下，可以使用 `by lazy` 优化变量定义，避免浪费资源。

## 谨慎使用 lateinit

`lateinit` 成员在被访问之前，必须确保该成员已经被赋值，否则会引发 crash。能够使用 `lateinit` 的前提是在使用到 `lateinit` 对象之前必须有明确的生命周期已经给 `lateinit` 赋值，如 `Activity` 的 `onCreate` 方法。禁用`isInitialized`，如果需要用到 `isInitialized`，说明说明当前场景已经不适合用 `lateinit`。

**反例**

```kotlin
class LateInitExample {
   // 使用 lateinit 声明 name 属性，但没有明确的初始化时机
   private lateinit var name: String

   // 提供一个 setter 方法，用于外部调用初始化 name
   fun setName(name: String) {
      this.name = name
   }

   // 因为无法保证 name 属性在使用之前一定被初始化，引入了 isInitialized 检查
   fun printName() {
      if (this::name.isInitialized) {
         println("Name: $name")
      } else {
         println("Name is not initialized")
      }
   }
}

fun main() {
   val example = LateInitExample()
   // 此时没有调用 setName，name 属性未被初始化
   example.printName() // 输出：Name is not initialized
   // 调用 setName 初始化 name 属性
   example.setName("Kotlin")
   // 再次调用 printName，此时 name 已被初始化
   example.printName() // 输出：Name: Kotlin
}
```

**正例**

```kotlin
// 定义一个 Activity 子类，表示 Android 应用中的一个界面
class MyActivity : Activity() {
   // 使用 lateinit 声明 btn 属性，表示延迟初始化 Button 对象
   // 由于 btn 的生命周期与 Activity 的生命周期绑定，因此可以安全地使用 lateinit
   private lateinit var btn: Button
   // 重写 Activity 的 onCreate 方法，这是生命周期的入口点之一
   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContentView(R.layout.activity_main)
      // 在布局加载完成之后，初始化 btn 属性，findViewById 通过布局文件中的 ID 找到对应的 Button 对象
      btn = findViewById(R.id.btn)
      // 此时 btn 属性已经完成初始化，可以安全使用
      btn.setOnClickListener {
         println("Button clicked!")
      }
   }
}
```

**小结**

`lateinit` 是一种便利的工具，但应谨慎使用，确保生命周期内的初始化顺序是明确的。 禁用 `isInitialized`，对于不能确保明确初始化的属性，建议使用 `nullable` 类型或延迟初始化 (`lazy`) 替代 `lateinit`，以提高代码的安全性和可维护性。

## 扩展阅读

1. **Kotlin 官方文档**
    - [Variables](https://kotlinlang.org/docs/basic-syntax.html#variables)
    - [Properties and Fields](https://kotlinlang.org/docs/properties.html)
    - [Late-initialized properties and variables](https://kotlinlang.org/docs/properties.html#late-initialized-properties-and-variables)
    - [Scope Functions](https://kotlinlang.org/docs/scope-functions.html)
2. **Effective Kotlin: Best Practices by Marcin Moskała**
    - Item 1: "Limit mutability"
    - Item 4: "Minimize the scope of variables"

## 总结

优先使用 `val` 定义变量，限制变量的作用域，避免不必要的状态暴露。此外，通过 `by lazy` 等工具，可以进一步优化变量的定义和资源使用。谨慎使用 `lateinit`，以及禁止使用 `isInitialized`。