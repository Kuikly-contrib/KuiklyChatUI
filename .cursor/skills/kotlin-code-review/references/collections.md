# 5、集合

**tl;dr**

- **应当** 优先使用不可变集合
- **应当** 优先使用函数式扩展处理数据流
- **应当** 避免在 `forEach` 中使用 `return`
- **应当** 在需要延迟计算或处理大数据集时考虑使用 `Sequence`
- **不应** 在外部 API 中暴露可变集合

## 优先使用不可变集合

不可变集合提供了更好的安全性和可预测性。它们防止意外的修改，保证并发安全性，并让代码更容易理解和维护。

**示例 1：避免集合被意外修改**

在面向对象设计中，封装原则要求类的内部状态不应被外部直接访问和修改。当 API 返回可变集合时，调用者可能会意外修改集合内容，导致内部状态不一致。使用不可变集合可以防止这种意外修改，提高代码的可靠性和可维护性。

```kotlin
// 不建议：暴露可变集合
class UserManager {
    private val users = mutableListOf<User>()
    
    // 危险：外部可以修改内部状态
    fun getUsers(): MutableList<User> = users
}

// 建议：返回不可变集合
class UserManager {
    private val users = mutableListOf<User>()
    
    // 安全：返回不可变副本
    fun getUsers(): List<User> = users.toList()
}
```

**示例 2：保证并发安全性**

在多线程环境中，可变集合的并发访问可能导致数据竞争和不可预测的行为。例如，当一个线程正在遍历集合时，另一个线程可能同时修改集合，导致 `ConcurrentModificationException` 或数据不一致。不可变集合由于其不可变性，天然支持并发访问而无需额外的同步机制，如锁或原子操作。这简化了并发编程的复杂性，并提高了程序的性能和稳定性。

```kotlin
// 不建议：可变集合在多线程环境下不安全
class UserRepository {

    private val users = mutableListOf<User>()
    private val lock = Any()

    fun getAllUsers(): List<User> {
        synchronized(lock) {
            return users
        }
    }

    fun addUser(user: User) {
        synchronized(lock) {
            users.add(user)
        }
    }

    fun removeUser(userId: String) {
        synchronized(lock) {
            users.removeAll { it.id == userId }
        }
    }
}

// 建议：使用不可变集合
class UserRepository {
    // ……

    fun getAllUsers(): List<User> {
        synchronized(lock) {
            // 返回一个不可变的副本
            return users.toList()
        }
    }

    // ……
}
```

在实际应用中，某些场景确实需要修改集合内容。最佳实践是在内部使用可变集合进行状态管理，但对外暴露不可变接口。

当需要向外部提供集合数据时，应创建不可变副本以防止意外修改，确保数据的一致性。

**小结**

优先使用不可变集合可以避免意外的修改，保证并发安全性，并提高代码的可维护性。在需要修改集合的场景下，在内部使用可变集合，但对外暴露不可变接口。向外部提供数据时，应创建不可变副本。

## 优先使用函数式扩展处理数据流

函数式编程风格使代码更简洁、可读性更强，并且更容易进行测试和维护。使用 `map`、`filter`、`reduce` 等函数式扩展可以清晰地表达数据处理逻辑。

**示例 1：使用 map 进行数据转换**

`map` 函数是函数式编程中的核心操作之一，它接受一个转换函数并将其应用到集合中的每个元素上，返回一个包含转换结果的新集合。这种声明式的编程风格比命令式的循环更加简洁、可读性更强，并且更容易进行单元测试和调试。

```kotlin
// 不建议：使用循环进行转换
fun convertToStrings(numbers: List<Int>): List<String> {
    val result = mutableListOf<String>()
    for (number in numbers) {
        result.add(number.toString())
    }
    return result
}

// 建议：使用 map 函数
fun convertToStrings(numbers: List<Int>): List<String> {
    return numbers.map { it.toString() }
}
```

**示例 2：使用 filter 进行数据筛选**

`filter` 函数根据提供的谓词（predicate）函数筛选集合中的元素，只保留满足条件的元素。这种操作在数据处理中非常常见，如筛选有效数据、过滤异常值等。函数式风格的筛选操作比传统的循环方式更加直观和高效。

```kotlin
// 不建议：使用循环进行筛选
fun getAdults(people: List<Person>): List<Person> {
    val result = mutableListOf<Person>()
    for (person in people) {
        if (person.age >= 18) {
            result.add(person)
        }
    }
    return result
}

// 建议：使用 filter 函数
fun getAdults(people: List<Person>): List<Person> {
    return people.filter { it.age >= 18 }
}
```

**示例 3：使用 reduce 进行数据聚合**

`reduce` 函数将集合中的所有元素通过累积操作聚合成单个值。它接受一个二元操作函数，从集合的第一个元素开始，依次将操作应用到累积结果和下一个元素上。这种操作常用于计算总和、乘积、最大值、最小值等聚合统计。

```kotlin
// 不建议：使用循环进行聚合
fun calculateTotal(prices: List<Double>): Double {
    var total = 0.0
    for (price in prices) {
        total += price
    }
    return total
}

// 建议：使用 reduce 函数
fun calculateTotal(prices: List<Double>): Double {
    return prices.reduce { acc, price -> acc + price }
}
```

**示例 4：链式调用多个函数式扩展**

函数式扩展的一个重要优势是可以进行链式调用，将多个操作组合成一个数据流水线。这种组合方式使得复杂的数据处理逻辑变得清晰易懂，每个操作都有明确的职责，便于理解和维护。链式调用还支持惰性求值，可以提高性能。

```kotlin
// 处理用户数据：筛选、转换、排序
fun processUsers(users: List<User>): List<String> {
    return users
        // 筛选活跃用户
        .filter { it.isActive }
        // 转换为大写
        .map { it.name.uppercase() }
        // 排序
        .sorted()
        // 去重
        .distinct()
}
```

**小结**

函数式扩展使代码更简洁、可读性更强，并且更容易进行测试和维护。优先使用 `map`、`filter`、`reduce`、`fold` 等函数式扩展来处理数据流，避免使用传统的循环结构。

## 避免在 forEach 中使用 return

在 `forEach` 中使用 `return` 不会实现预期的 `continue` 或 `break` 功能，这是初学者常见的错误。`forEach` 中的无标签 `return` 会从**定义该 lambda 的外层函数**中返回，而不是仅从 lambda 返回或跳过当前迭代。

**反例**

在 `forEach` 中使用无标签 `return` 语句是初学者常见的错误。`forEach` 是一个高阶函数，它接受一个 lambda 表达式作为参数。当在 lambda 中使用无标签 `return` 时，它会尝试从包含 `forEach` 调用的函数（即外层函数）中返回。这种行为与传统循环中的 `continue` 或 `break` 行为完全不同，常常导致意外的提前退出整个函数。

```kotlin
fun findFirstEven(numbers: List<Int>): Int? {
    var result: Int? = null
    numbers.forEach { number ->
        if (number % 2 == 0) {
            result = number
            // 错误：这会从 findFirstEven 函数中返回，导致 forEach 提前终止
            return result
        }
    }
    return result
}

fun processNumbers(numbers: List<Int>) {
    println("开始处理数字...")
    numbers.forEach { number ->
        if (number < 0) {
            // 错误：这会直接退出整个 processNumbers 函数，而不是跳过当前负数
            return
            // 虽然可以使用 return@forEach 跳过当前元素（相当于 continue），但对于复杂的控制流，不推荐在 forEach 中使用。
            // return@forEach
        }
        println("处理: $number")
    }
    println("处理完毕。")
}
```

**正例**

要实现预期的控制流，应该使用适当的函数式扩展或传统的循环结构。对于查找第一个满足条件的元素，可以使用 `firstOrNull` 等函数；对于筛选元素，可以使用 `filter` 函数；对于需要提前终止或跳过当前迭代的场景，应该使用传统的 `for` 循环配合 `break` 或 `continue` 语句。

```kotlin
// 使用 firstOrNull 查找第一个满足条件的元素
fun findFirstEven(numbers: List<Int>): Int? {
    return numbers.firstOrNull { it % 2 == 0 }
}

// 使用 filter 筛选元素，然后对筛选后的元素进行处理
fun processPositiveNumbers(numbers: List<Int>) {
    numbers.filter { it >= 0 }.forEach { number ->
        println("处理: $number")
    }
}

// 使用传统 for 循环实现 break 功能
fun findFirstEvenWithLoop(numbers: List<Int>): Int? {
    for (number in numbers) {
        if (number % 2 == 0) {
            return number
        }
    }
    return null
}

// 使用传统 for 循环实现 continue 功能
fun processNumbersWithLoop(numbers: List<Int>) {
    println("开始处理数字...")
    for (number in numbers) {
        if (number < 0) {
            continue
        }
        println("处理: $number")
    }
    println("处理完毕。")
}
```

**小结**

避免在 `forEach` 中使用无标签 `return`，因为它会导致非局部返回，意外地跳出整个包含 `forEach` 的函数。要实现 `continue` 行为，请使用**标签返回 `return@forEach`**，但对于复杂的控制流，**优先使用传统的 `for` 循环**或更适合场景的函数式集合操作（如 `firstOrNull`、`filter` 等）。

## 考虑使用 Sequence

`Sequence` 提供延迟计算和内存效率，特别适合处理大数据集或需要链式操作的场景。`Sequence` 是惰性的，只有在需要结果时才会执行计算。

**示例 1：大数据集处理**

在处理大数据集时，传统的集合操作会创建多个中间集合，导致内存使用量急剧增加。`Sequence` 采用惰性求值（lazy evaluation）策略，只有在需要结果时才执行计算，并且不会创建中间集合。这种特性使得 `Sequence` 在处理大数据集时具有显著的内存优势。

```kotlin
// 使用 List：会创建多个中间集合
fun processLargeDataset(numbers: List<Int>): List<String> {
    return numbers
        // 创建新的 List
        .filter { it > 0 }
        // 创建新的 List
        .map { it * 2 }
        // 创建新的 List
        .filter { it < 100 }
        // 创建新的 List
        .map { it.toString() }
}

// 使用 Sequence：延迟计算，内存效率更高
fun processLargeDataset(numbers: List<Int>): List<String> {
    return numbers.asSequence()
        // 延迟计算
        .filter { it > 0 }
        // 延迟计算
        .map { it * 2 }
        // 延迟计算
        .filter { it < 100 }
        // 延迟计算
        .map { it.toString() }
        // 只在最后创建结果集合
        .toList()
}
```

**示例 2：提前终止操作**

`Sequence` 的一个重要特性是支持提前终止操作。当使用 `firstOrNull`、`find`、`any`、`all` 等终端操作时，`Sequence` 会在找到第一个满足条件的结果后立即停止计算，而不会处理剩余的元素。这种短路求值（short-circuit evaluation）可以显著提高性能，特别是在处理大量数据时。

```kotlin
// 使用 List：会处理所有元素
fun findFirstLargeNumber(numbers: List<Int>): Int? {
    return numbers
        .filter { it > 0 }
        .map { it * 2 }
        .firstOrNull { it > 100 }
}

// 使用 Sequence：找到第一个满足条件的元素就停止
fun findFirstLargeNumber(numbers: List<Int>): Int? {
    return numbers.asSequence()
        .filter { it > 0 }
        .map { it * 2 }
        .firstOrNull { it > 100 }
}
```

**示例 3：无限序列**

`Sequence` 的一个重要应用场景是表示无限序列，如斐波那契数列、素数序列等。通过使用 `sequence` 构建器和 `yield` 函数，可以创建按需生成元素的无限序列。这种能力使得 `Sequence` 在处理数学计算、数据流处理等场景中非常有用。

```kotlin
// 生成无限序列
fun fibonacci(): Sequence<Int> = sequence {
    var a = 0
    var b = 1
    while (true) {
        yield(a)
        val temp = a + b
        a = b
        b = temp
    }
}

// 使用无限序列
fun getFirstTenFibonacci(): List<Int> {
    return fibonacci().take(10).toList()
}
```

**示例 4：何时不使用 Sequence**

虽然 `Sequence` 在处理大数据集时具有优势，但并非所有场景都适合使用 `Sequence`。对于小数据集，`List` 的简单性和直接性可能更合适。此外，如果需要多次访问集合中的元素，`List` 的随机访问特性比 `Sequence` 的顺序访问更高效。在选择数据结构时，应该根据具体的使用场景和性能需求来决定。

```kotlin
// 小数据集：List 更简单直接
fun processSmallDataset(numbers: List<Int>): List<String> {
    return numbers.map { it.toString() }
}

// 需要多次访问：List 更高效
fun processAndReuse(numbers: List<Int>) {
    val processed = numbers.map { it * 2 }
    
    // 多次使用 processed
    println("Sum: ${processed.sum()}")
    println("Average: ${processed.average()}")
    println("Max: ${processed.maxOrNull()}")
}
```

**小结**

`Sequence` 提供延迟计算和内存效率，特别适合处理大数据集或需要链式操作的场景。对于小数据集或需要多次访问的场景，`List` 可能更合适。选择 `Sequence` 还是 `List` 应该根据具体的使用场景来决定。

## 扩展阅读

- *Effective Kotlin: Best Practices* by Marcin Moskała (Item 45, Item 46, Item 47)
- *Kotlin in Action, Second Edition* by Sebastian Aigner, Dmitry Jemerov, etc. (Chapter 8)
- [Kotlin Docs: Collections](https://kotlinlang.org/docs/collections-overview.html)
- [Kotlin Docs: Sequences](https://kotlinlang.org/docs/sequences.html)

## 总结

集合是 Kotlin 编程中的重要组成部分，正确使用集合可以提高代码的安全性、可读性和性能。关键原则是优先使用不可变集合、使用函数式扩展处理数据流、避免在 `forEach` 中使用 `return`，并在适当的时候考虑使用 `Sequence`。这些最佳实践可以帮助开发者编写更安全、更高效的 Kotlin 代码。
