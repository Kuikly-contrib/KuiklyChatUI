# 4、数据类

**tl;dr**

- **应当** 优先考虑使用普通类而非数据类，除非确实需要纯数据存储
- **应当** 避免在数据类中使用可变属性，以保持相等性的可靠性
- **应当** 避免在数据类中添加复杂类型的属性，如函数类型或影响可变性的对象
- **应当** 谨慎使用 `copy()` 方法，注意其浅复制的特性
- **不应** 为了便利而滥用数据类，要考虑编译器生成函数对产物体积的影响

## 优先使用普通类

数据类虽然提供了便利的自动生成函数，但在某些场景下使用普通类可能更加合适。需要关注编译器自动生成函数对产物体积的影响、序列化框架的兼容性问题，以及数据类不可继承的限制。

**示例 1：不应包含其他函数**

数据类主要用于存储数据。此规则假定它们除了用于对象间转换的函数外，不应包含任何其他函数，数据类将由编译器自动生成 `equals`、`toString`、`hashCode` 函数。

如果包含对象间转换外的函数，不建议使用 `data class`。
```kotlin
data class DataClassWithFunctions(val i: Int) {
    fun foo() { } // 不建议在数据类中添加业务逻辑方法
}
```

**示例 2：无法继承**
`sealed`、`inner`、`abstract`、`open` 关键字无法修饰 `data class`，因此不适合继承场景。数据类的核心设计目标是作为纯粹的、不可变的数据容器，而这个目标与继承的复杂性是天然冲突的。

会出现报错：`This type is final, so it cannot be inherited from`。

```kotlin
data class BaseEntity(
    val id: String,
    val age: Int
)

class UserEntity(
    id: String,
    age: Int,
    val name: String
) : BaseEntity(id, age) // 编译错误，无法继承
```

**示例 3：无默认构造函数**

数据类没有默认的无参构造器，这会使得一些依赖无参构造器的序列化框架（如 Jackson）产生问题。例如 Jackson 需要无参构造器来创建对象实例，这时就可能产生错误。

```kotlin
data class User(
    val id: String,
    val name: String,
    val email: String
)

// Jackson 反序列化时会失败
fun deserializeWithJackson() {
    val json = """{"id":"1","name":"Alice","email":"alice@example.com"}"""
    val mapper = ObjectMapper()

    // 这行代码会抛出异常：Cannot construct instance of User
    val user = mapper.readValue(json, User::class.java)
}
```

尽管某些序列化框架（如 Gson）可以通过反射机制绕过无参构造器的限制来创建数据类实例，但这种做法存在潜在的安全隐患。这些框架通常使用 `Unsafe` 接口或类似的底层机制来直接分配内存并创建对象，完全绕过了 Kotlin 的类型安全系统。当 JSON 中缺少必需字段时，可能导致非空属性为 `null`，从而破坏 Kotlin 的编译期空安全保证，在运行时抛出 `NullPointerException`。

**小结**

数据类适用于纯数据存储场景，但会带来产物体积增加、序列化兼容性问题和继承限制。当需要业务逻辑、继承关系或特定序列化兼容性时，应优先考虑使用普通类。只有在确实需要纯数据存储且不需要继承时，才使用数据类。

## 避免使用可变属性

数据类中的可变属性会破坏相等性的可靠性。虽然 `equals()` 和 `hashCode()` 方法每次调用时都会重新计算所有属性的值，但可变属性会导致对象在生命周期内相等性发生变化，这在作为 Map 的 key 时会造成严重问题。

**反例**

可变属性导致 `Set` 和 `Map` 的行为异常，特别是作为 Map key 时。
```kotlin
data class User(
    val id: String,
    val name: String,
    var lastLoginTime: Long = 0L // 可变属性
)

fun demonstrateIssue() {
    val userA = User("1", "Alice", 1000L)
    val userB = User("1", "Alice", 1000L)

    println("初始时 userA == userB: ${userA == userB}") // true

    val userSet = mutableSetOf(userA, userB)
    println("Set 初始大小: ${userSet.size}") // 1

    userA.lastLoginTime = 2000L
    // 数据类的 equals 和 hashCode 会包含 var 属性
    println("修改后 userA == userB: ${userA == userB}") // false
    // Set 内部的哈希值已改变，但元素未重新定位，导致查询行为异常
    // 仍然是 1，但 userA 在 Set 中的哈希值已经和其 equals() 行为不一致了
    println("Set 修改后大小: ${userSet.size}")
    // 返回 false
    println("Set 中是否包含 userA: ${userSet.contains(userA)}")
    // 返回 false
    println("Set 中是否包含 userB: ${userSet.contains(userB)}")
}
```

**正例**

```kotlin
data class User(
    val id: String,
    val name: String,
    val lastLoginTime: Long = 0L // 不可变属性
)
```

**小结**

数据类中的可变属性会破坏相等性的可靠性，导致在集合中的行为异常。应优先使用不可变属性，通过 `copy()` 方法创建新实例来更新状态。如果确实需要可变状态，考虑使用普通类并手动实现 `equals()` 和 `hashCode()` 方法，确保这些方法仅依赖于不可变属性。

## 避免添加复杂类型的属性

数据类应保持简单，避免添加函数类型、影响可变性的对象等复杂属性。这些属性会影响 `toString()` 性能、破坏不可变性，并增加代码的复杂性。

**示例 1：包含函数类型**
包含函数类型属性，`toString()` 会使用反射影响性能，应从 `data class` 中删除。
```kotlin
data class User(
    val id: String,
    val name: String,
    val onLogin: (String) -> Unit, // 函数类型
    val permissions: MutableList<String> = mutableListOf() // 可变集合
)
```

**示例 2：包含可变集合**
包含复杂对象，可能影响不可变性，应使用**不可变集合**。
```kotlin
data class Order(
    val id: String,
    val items: List<String>, // 使用不可变集合 List
)
```

**小结**

数据类应保持简单，只包含基本数据类型和不可变对象。避免添加函数类型、可变集合、回调等复杂属性，这些会影响性能、破坏不可变性并增加复杂性。对于需要复杂逻辑的场景，使用普通类并将数据类作为纯数据载体。

## 谨慎使用数据类的 copy

数据类的 `copy()` 方法执行的是浅复制。对于包含可变对象的属性，修改副本可能会影响原始对象。需要特别注意引用类型属性的复制行为。

**反例**

```kotlin
data class User(
    val id: String,
    val name: String,
    val tags: MutableList<String> = mutableListOf() // 可变集合
)

fun demonstrateShallowCopyIssue() {
    val originalUser = User("1", "Alice", mutableListOf("admin", "user"))
    val copiedUser = originalUser.copy(name = "Bob")

    // 修改副本的可变属性
    copiedUser.tags.add("guest")

    // 原始对象也被修改了！
    println("Original User Tags: ${originalUser.tags}") // -> [admin, user, guest]
    println("Updated User Tags: ${updatedUser.tags}")  // -> [admin, user, guest]
}
```

**正例**

```kotlin
data class User(
    val id: String,
    val name: String,
    val tags: List<String> = listOf() // 使用不可变的 List
)

fun demonstrateImmutableWay() {
    val originalUser = User("1", "Alice", listOf("admin", "user"))
    
    // 当你需要“修改”时，你应该创建一个新的 User 实例，并提供一个新的 List
    val updatedUser = originalUser.copy(name = "Bob", tags = originalUser.tags + "guest")
    // 或者 tags = originalUser.tags.toMutableList().apply { add("guest") }.toList()

    println("Original User Tags: ${originalUser.tags}") // -> [admin, user]
    println("Updated User Tags: ${updatedUser.tags}")  // -> [admin, user, guest]
}
```

**小结**

数据类的 `copy()` 方法是浅复制。对于包含可变对象的属性，修改副本会影响原始对象。应优先使用不可变集合和对象，通过创建新实例来实现真正的复制。如果必须使用可变对象，应提供深复制方法或明确文档说明浅复制的行为。

## 正确使用解构

数据类的解构功能虽然便利，但在使用时需要谨慎考虑场景和上下文。解构应该仅在上下文清晰的小作用域内使用，避免在公共API中暴露，并注意属性数量的限制。

**示例 1：公共API**

在公共API中使用解构是绝对禁止的，因为数据类结构（属性顺序或数量）的变更会导致客户端代码崩溃。

```kotlin
// 外部数据类
data class User(
    val name: String,
    val age: Int
)

// 客户端代码
fun clientCode() {
    val user = User("Alice", 25)
    // 危险：依赖解构顺序，如果 User 属性顺序改变，此处代码将出错
    // val (name, age) = user 
    
    // 应该直接访问成员变量，更安全、更清晰
    println("Name: ${user.name}, Age: ${user.age}")
}
```

**示例 2：属性数量**

数据类解构最多适用于3个以内属性的场景。超过4个属性后，解构顺序难以记忆，代码变得脆弱且容易出错。

```kotlin
// 属性过多，解构顺序难以记忆
data class ComplexUser(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val address: String,
    val birthDate: String,
    val department: String
)

fun processUser(user: ComplexUser) {
    // 危险：属性过多，容易搞错解构顺序，且一旦 ComplexUser 属性改变，此处代码就需要修改
    // val (id, firstName, lastName, email, phone, address, birthDate, department) = user
    
    // 推荐做法：直接通过命名属性访问
    println("User: ${user.firstName} ${user.lastName}, Email: ${user.email}")
}
```

**示例 3：Lambda参数**

避免在Lambda参数中直接解构，应在函数体内解构以保持函数签名的清晰性。直接在参数列表解构可能使可读性降低，特别是在嵌套或复杂Lambda中。

```kotlin
data class Point(val x: Int, val y: Int)

fun processPoints() {
    val points = listOf(
        Point(1, 2),
        Point(3, 4),
        Point(5, 6)
    )
    
    // 不推荐：函数签名不清晰，且如果 Point 属性数量变化，这里会受影响
    points.forEach { (x, y) ->
        println("Point: ($x, $y)")
    }

    // 推荐：函数签名清晰，解构发生在函数体内部，更易读
    points.forEach { point ->
        val (x, y) = point
        println("Point: ($x, $y)")
    }
}
```

**小结**

解构功能应该谨慎使用，仅在上下文清晰的小作用域内使用。避免在公共API中暴露解构，限制属性数量在3个以内，并在Lambda参数中避免直接解构。当需要访问更多属性时，考虑将数据类拆分为更小的组合，或者使用命名属性访问而非解构。

## 扩展阅读

- *Effective Kotlin: Best Practices* by Marcin Moskała (Item 37, Item 38)
- *Kotlin in Action, Second Edition* by Sebastian Aigner, Dmitry Jemerov, etc. (Chapter 4.3)
- [Kotlin Docs: Data Classes](https://kotlinlang.org/docs/data-classes.html)
- [Kotlin Docs: Copying](https://kotlinlang.org/docs/data-classes.html#copying)

## 总结

数据类是 Kotlin 提供的一个便利特性，但需要谨慎使用。它们最适合纯数据存储场景，但会带来产物体积增加、序列化兼容性问题和继承限制。关键原则是保持数据类的简单性：使用不可变属性、避免复杂类型、注意浅复制的特性。当需要业务逻辑、继承关系或复杂状态管理时，应优先考虑使用普通类。只有在确实需要纯数据存储且不需要继承时，才使用数据类，并始终遵循不可变性的原则。
