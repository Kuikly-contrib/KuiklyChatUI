# 3、类与对象

**tl;dr**

- 优先使用主构造函数和默认参数优先创建简单对象
- 工厂函数适用于复杂构造逻辑、返回子类型或隐藏实现
- 顶层函数和伪构造函数是灵活的选择，但需注意避免命名空间污染
- `DSL` 构建器适用于创建复杂的分层对象结构
- 依赖注入解耦对象与外部依赖，但可能增加复杂性
- **建议**在需要携带额外数据时等复杂场景，使用 密封类 `sealed class` 替代枚举类
- **应当**理解属性应仅表示状态，而非复杂计算结果或外部依赖的行为

## 对象创建

- 优先使用带默认参数的主构造函数创建简单对象。
- 当创建逻辑变复杂、需要隐藏实现或返回子类型时，再考虑使用工厂函数。

选择哪种对象创建模式取决于对象的复杂性、API 的设计目标以及与现有代码（特别是 Java）的互操作性需求。

| 创建模式 | 核心用例 | 优势 | 劣势 |
| :--- | :--- | :--- | :--- |
| **主构造函数** | 简单对象，数据模型类 | 极其简洁，惯用，完美替代 Java 的伸缩构造函数和简单构建器 | 构造逻辑受限，无法返回子类型 |
| **伴生对象工厂** | 需要访问私有成员的工厂，与 Java 的互操作性 | 模式通用，可访问私有成员，Java 互操作性好 | 语法比顶层函数冗长 |
| **顶层/伪构造函数** | 通用工具类，隐藏接口实现 | 语法最简洁，可读性高，灵活性强 | 可能污染全局命名空间 |
| **DSL 构建器** | 复杂、分层的对象结构 | 表达力极强，类型安全，可重用性高 | 定义复杂，可能过度设计 |
| **依赖注入** | 需要外部服务或组件的对象 | 解耦，易于测试，灵活性高 | 增加了项目的复杂性 |

熟练掌握这些模式并根据具体场景做出明智的选择，是编写高质量、可维护的 Kotlin 代码的关键。在大多数情况下，从最简单的主构造函数开始，当需求变得更复杂时，再逐步考虑使用更强大的工厂函数或 DSL 模式。

### 基础模式：主构造函数与具名可选参数

对于绝大多数对象，尤其是那些主要用于承载数据的模型类，使用带有具名和默认参数的主构造函数是 Kotlin 中最惯用、最推荐的方式。

**正例**

使用主构造函数和具名参数，代码清晰、简洁。

```kotlin
data class Pizza(val size: String, val cheese: Int = 1, val olives: Int = 0)
val myPizza = Pizza(size = "L", olives = 3) // 清晰、惯用
```

**反例**

为简单对象过度设计一个复杂的 Java 式构建器 (Builder)。在 Kotlin 中，这通常是不必要的，因为主构造函数已经解决了其旨在解决的问题。

```kotlin
// 反例：在 Kotlin 中 Builder 通常是冗余的
val myPizza = Pizza.Builder("L")
        .setOlives(3)
        .build()
```

**优势 (对比 Java 模式)**

- **完全取代“伸缩构造函数模式” (Telescoping Constructor)**：在 Java 中，为了支持不同的参数组合，开发者需要编写多个重载的构造函数，代码冗长且难以维护。Kotlin 的默认参数完美地解决了这个问题，只需一个构造函数即可。
- **在简单场景下优于“构建器模式” (Builder Pattern)**：Java 中经典的构建器模式主要是为了解决缺少命名参数和默认值的问题。Kotlin 的主构造函数原生支持了这些特性，因此在大多数情况下，其代码更短、更清晰，且无需编写额外的 Builder 类和调用 `build()` 方法。

**劣势与局限**

- **构造逻辑受限**：主构造函数的语法非常简洁，但也因此无法包含复杂的初始化逻辑。它必须立即调用超类的构造函数。
- **无法返回子类型**：构造函数总是返回当前类的实例，无法返回其子类型或接口类型，这限制了实现的灵活性和封装性。

### 进阶模式：工厂函数 (Factory Functions)

当对象的创建逻辑比简单的属性赋值更复杂时，工厂函数是比构造函数更强大、更灵活的选择。它们是用于创建对象的普通函数，但拥有构造函数所不具备的多项优势。

**工厂函数的通用优势**

- **拥有描述性名称**：函数名可以清晰地表达对象的创建方式和参数含义，例如 `User.createGuest()` 或 `ArrayList.withCapacity(3)`，避免了构造函数参数的歧义。
- **可返回任何子类型**：工厂函数可以返回其声明返回类型的任何子类型，从而隐藏具体实现。例如，`listOf()` 返回的是 `List` 接口，其具体实现对调用者透明，为库的作者提供了极大的优化空间。
- **不必每次都创建新对象**：工厂函数可以实现缓存机制或返回单例，从而优化性能或确保对象的复用。
- **更灵活的可见性控制**：顶层工厂函数可以通过 `private` 或 `internal` 修饰符来限制其可见范围。
- **可内联 (inline)**：工厂函数可以是内联的，这使其类型参数可以被具体化 (reified)，从而提供更便捷的 API。

Kotlin 中的工厂函数主要有以下几种形式：

**伴生对象工厂函数 (Companion Object Factory Functions)**

这种模式最接近 Java 的静态工厂方法，函数被定义在类的 `companion object` 中。

```kotlin
class User private constructor(val name: String) {
   companion object {
      @JvmStatic // 确保 Java 调用者可以像静态方法一样使用
      fun fromJson(json: String): User {
         /*...*/
         return User(name = "parsed_name")
      }
   }
}

// 使用方式
val user = User.fromJson("{...}")
```

- **优势**：模式被广泛认知，符合从 Java 过来的开发者的直觉；可以访问类的私有成员；通过 `@JvmStatic` 注解可以实现与 Java 的完美互操作；伴生对象可以继承类或实现接口，从而实现更高级的抽象工厂模式。
- **劣势**：语法比顶层函数更冗长 (`User.fromJson` vs `fromJson`)；需要额外定义一个 `companion object` 结构。

**顶层工厂函数 (Top-Level Factory Functions)**

函数直接定义在文件的顶层，不隶属于任何类。这是 Kotlin 标准库中非常常见的模式。

```kotlin
// 类似标准库中的 listOf, setOf
fun <T> linkedListOf(vararg elements: T): LinkedList<T> {
   return LinkedList(elements.toList())
}

// 使用方式
val list = linkedListOf(1, 2, 3)
```

- **优势**：语法最简洁、可读性高，尤其适用于创建通用的小对象；定义简单，无需嵌套在类中。
- **劣势**：公共的顶层函数会污染全局命名空间，可能导致 IDE 自动补全提示混乱。因此，需要谨慎命名以避免与类的方法冲突。

**伪构造函数 (Fake Constructors)**

这是一种特殊的顶层工厂函数，其名称与类名相同。它看起来和用起来都像构造函数，但具备工厂函数的所有优点。

```kotlin
// 标准库中的 List 和 MutableList 就是这种模式
// public inline fun <T> List(size: Int, init: (index: Int) -> T): List<T>

// 使用方式，看起来像在调用构造函数
val userList = List(5) { index -> "User$index" }
```

- **优势**：完美地隐藏了实现细节，API 只暴露接口，而由伪构造函数返回具体的实现类；可以根据参数返回不同的、经过优化的实现；允许在对象创建前执行复杂的算法逻辑。
- **劣势**：因为它不是真正的构造函数，可能会让不熟悉的开发者感到困惑。应谨慎使用，主要用于那些无法在类本身定义构造函数（如接口）或需要构造函数不具备的能力（如 reified 类型参数）的场景。

**转换/拷贝函数**

这些是用于从一个对象创建另一个对象的特殊工厂函数。
- **转换函数**：通常命名为 `to{Type}`（创建新对象）或 `as{Type}`（创建视图或包装器），例如 `list.toSet()`。
- **拷贝函数**：`data class` 自动生成的 `copy()` 方法是此模式的最佳体现，它允许以不可变的方式创建对象的新状态。

### 高级模式：复杂对象的构建

当对象的创建过程涉及多个步骤、复杂的配置或层级结构时，需要更高级的构建模式。

**DSL 构建器 (Domain-Specific Language Builders)**

对于需要构建复杂、分层数据结构（如 UI 布局、HTML、复杂配置）的场景，DSL 是 Kotlin 中最惯用、最强大的模式。

```kotlin
// Ktor 路由配置 DSL 示例
routing {
   get("/news") {
      call.respond(newsData)
   }
}
```

- **优势**：极高的表达力和可读性；代码结构清晰，能够反映被构建对象的层级关系；类型安全；灵活性与可重用性。
- **劣势**：定义复杂，需要对 Kotlin 的高级特性（如带接收者的 Lambda）有深入的理解；对于简单的对象创建，DSL 是一种过度设计。

**依赖注入 (Dependency Injection)**

当一个对象依赖于其他服务或组件时，应使用依赖注入模式，通过构造函数接收其依赖项，而不是在内部自己创建。

```kotlin
class UserService(
   private val userRepository: UserRepository,
   private val emailService: EmailService
) { /*...*/ }
```

- **优势**：解耦，使组件之间的依赖关系变得明确和松散；易于测试，可以轻松地在测试中传入模拟 (mock) 的依赖项；灵活性和可重用性。
- **劣势**：对于大型项目，通常需要引入依赖注入框架（如 `Koin`, `Dagger`/`Hilt`），这会增加项目的复杂性。

## 建议复杂场景下，使用 sealed class 替代枚举类

携带额外数据等复杂场景下，尽量使用密封类 `sealed class` 替代枚举类。

**示例**

```kotlin
// 不建议：普通枚举，无法携带附加数据，状态扩展受限
enum class NetworkState { LOADING, SUCCESS, ERROR }

// 建议：密封类，可额外携带数据
sealed class NetworkState {
   object Loading : NetworkState()
   data class Success(val data: String) : NetworkState()
   data class Error(val error: Throwable) : NetworkState()
}

fun handle(state: NetworkState) {
   // when 表达式更安全，无需 else 分支
   when (state) {
      NetworkState.Loading -> println("Loading...")
      NetworkState.Success -> println("Data: ${state.data}")
      NetworkState.Error -> println("Error: ${state.error.message}")
   }
}
```

**小结**

密封类 `sealed class` 用于定义固定状态集合，需要注意的是子类需在同一文件中声明，支持携带数据，`when` 表达式更安全，无需 `else` 分支，适合表示复杂状态。

## 属性应该代表状态而非行为

“属性应该代表状态而非行为” 是编程中的一个重要设计原则，尤其是在面向对象编程（OOP）中。这个原则的核心思想是：类的属性应该用来描述对象的状态，而行为应该通过方法来实现，而不是通过属性（或字段）来表现行为。由于 Kotlin 为属性提供了 `get()` ，因此很容易在属性的实践中违背这个原则。

**反例**

**1. 将外部依赖，非幂等的结果作为属性**

```kotlin
class WeatherService {
   val temperature: Double
      get() {
         return fetchTemperatureFromAPI() // 从外部 API 获取温度
      }
}
```

**2. 属性值的访问存在计算密集型运算**

```kotlin
class HeavyComputation {
   // 反例：属性值的获取涉及计算密集型运算（阶乘）
   val factorialOfLargeNumber: Long
      get() {
         // 计算一个大数的阶乘，计算密集型操作
         return (1..20).fold(1L) { acc, i -> acc * i }
      }
}
```

每次访问 `factorialOfLargeNumber` 属性都会执行计算密集型操作（阶乘）。 这种设计不符合属性的直觉，因为属性通常被认为是快速访问的。

**3. 属性值的访问可能抛出异常**

```kotlin
class User(private val isLoggedIn: Boolean) {
   // 属性的访问可能抛出异常
   val profilePictureUrl: String
      get() {
         if (!isLoggedIn) {
            throw IllegalStateException("User is not logged in!")
         }
         // 模拟从远程服务器获取头像 URL
         return fetchProfilePictureUrl()
      }
}
```

**正例**

**1. 将外部依赖的结果改为方法**

将涉及外部 API 调用的逻辑设计为方法，而非属性，这是更符合直觉的设计。

```kotlin
class WeatherService {
   // 改为方法以表明它是行为
   fun fetchTemperature(): Double {
      return fetchTemperatureFromAPI() // 从外部 API 获取温度
   }
}
```

**2. 将计算密集型操作改为方法**

计算密集型操作的结果不应定义为属性，而是通过方法显式计算。

```kotlin
class HeavyComputation {
   // 改为方法以避免每次调用都重复计算
   fun computeFactorialOfLargeNumber(): Long {
      return (1..20).fold(1L) { acc, i -> acc * i }
   }
}
```

**3. 将可能抛出异常的逻辑改为方法**

抛出异常的逻辑不应隐藏在属性中，应该通过方法明确表达。

```kotlin
class User(private val isLoggedIn: Boolean) {
   // 改为方法以表明可能会失败
   fun fetchProfilePictureUrl(): String {
      if (!isLoggedIn) {
         throw IllegalStateException("User is not logged in!")
      }
      return fetchFromServer() // 模拟从服务器获取头像 URL
   }
}
```

**小结**

属性应该表示状态，方法应该表示行为。若属性中存在外部依赖、计算密集型操作和可能失败的逻辑，都应修改为使用方法实现，属性设计应符合调用者的直觉，方法和属性的职责应界限清晰。

## 扩展阅读

1. **Kotlin 官方文档**
    - [Kotlin Docs: Classes and Constructors](https://kotlinlang.org/docs/classes.html)
    - [Kotlin Docs: Coding Conventions (Factory Functions)](https://kotlinlang.org/docs/coding-conventions.html#factory-functions)
2.**Effective Kotlin: Best Practices by Marcin Moskała**
    - Item 32: Consider factory functions instead of secondary constructors
    - Item 33: Consider a primary constructor with named optional arguments
    - Item 34: Consider defining a DSL for complex object creation
    - Item 35: Consider using dependency injection

## 总结

Kotlin 提供了多种灵活的对象创建模式，从简单的主构造函数到复杂的工厂函数和 DSL 构建器。优先选择简单模式，如主构造函数和默认参数，能够满足大多数简单场景的需求。当创建逻辑变复杂时，可以逐步引入更高级的模式，如工厂函数或 DSL，来提高代码的可扩展性和维护性。合理选择模式并掌握其使用场景，是编写高质量 Kotlin 代码的关键。