# Kotlin 最佳实践集合

最佳实践收录主围绕 **可读性/可维护性** / **性能** 及少量**逻辑正确性** 话题讨论。本条目下原则上不讨论 detekt 静态检查能发现的问题，尽可能减少讨论 IDE 就会提示的问题。

> 欢迎 PR

## 利用 Kotlin 语言特性

- 优先使用不可变（而不是可变）数据。初始化后未修改的局部变量与属性，总是将其声明为 `val` 而不是 `var`

- 总是使用不可变集合接口（`Collection`, `List`, `Set`, `Map`）来声明无需改变的集合。使用工厂函数创建集合实例时，尽可能选用返回不可变集合类型的函数

  正例

  ```kotlin
  // 使用不可变集合类型
  fun validateValue(actualValue: String, allowedValues: Set<String>) { …… }
  
  // listOf() 返回 List<T>
  val allowedValues = listOf("a", "b", "c")
  ```

  反例

  ```kotlin
  // 使用可变集合类型作为无需改变的值
  fun validateValue(actualValue: String, allowedValues: HashSet<String>) { …… }
  
  // arrayListOf() 返回 ArrayList<T>，这是一个可变集合类型
  val allowedValues = arrayListOf("a", "b", "c")
  ```

- 使用默认参数：优先声明带有默认参数的函数而不是声明重载函数

  正例

  ```kotlin
  fun foo(a: String = "a") { /*……*/ }
  ```

  反例

  ```kotlin
  fun foo() = foo("a")
  fun foo(a: String) { /*……*/ }
  ```

- 使用类型别名：如果有一个在代码库中多次用到的函数类型或者带有类型参数的类型，那么最好为它定义一个类型别名

  ```kotlin
  typealias MouseClickHandler = (Any, MouseEvent) -> Unit
  typealias PersonIndex = Map<String, Person>
  ```

- 使用具体参数：当一个方法参数过长，或接受多个相同的原生类型参数或者多个 `Boolean` 类型参数时，请使用具体参数语法， 除非在上下文中的所有参数的含义都已绝对清楚。

  ```kotlin
  drawSquare(x = 10, y = 10, width = 100, height = 100, fill = true)
  ```

- 优先使用条件语句

  正例

  ```kotlin
  return if (x) foo() else bar()
  
  return when(x) {
      0 -> "zero"
      else -> "nonzero"
  }
  ```

  反例

  ```kotlin
  if (x)
      return foo()
  else
      return bar()
  
  when(x) {
      0 -> return "zero"
      else -> return "nonzero"
  }
  ```

- 优先使用高阶函数（`filter`、`map` 等）而不是循环

- 使用扩展函数：每当有一个主要用于某个对象的函数时，可以考虑使其成为一个以该对象为接收者的扩展函数。为了尽量减少 API 污染，尽可能地限制扩展函数的可见性。根据需要，使用局部扩展函数、成员扩展函数或者具有私有可视性的顶层扩展函数。

- 尽可能使用中缀函数：一个函数只有用于两个角色类似的对象时才将其声明为中缀函数。良好示例如：`and`、 `to`、`zip`。如：
  正例

  ```kotlin
  val pair = 1 to 2
  ```

  反例

  ```kotlin
  val pair = Pair(1, 2)
  ```

- 灵活使用作用域函数 apply/with/run/also/let 以降低代码的重复

  !!! Warning 关于 to 运算符

      在 map 初始化场景中：
      
      ```kotlin
      val map = mapOf(
          "John" to 12,
          "Sam" to 23,
      )
      ```
      
      每调用一次`to`都会创建一个`Pair`对象，放到`Pair数组`中用于初始化`Map`
      当初始化所需的元素比较多时，建议使用`[]`形式初始化
      
      ```kotlin
        val map = arrayMapOf<String, Int>()
        map["John"] = 12
        map["Sam"] = 23
      ```

## 避免滥用 lateinit/_Delegate_._notNull_()

从禁止使用下调成避免滥用，理由如下:

1. [kotlin 规范](https://kotlinlang.org/docs/coding-conventions.html)中并无对 lateinit 的限制
2. 使用 lateinit 虽然需要开发者明确各处调用避免非空，错误的使用还有可能导致 crash 增高，但是带来的便利也是显而易见的
3. 虽然官方没有明确表示使用场景，但是在 Android 官方 demo 中有对该属性的使用，主要场景
   1. 使用 dagger/Hilt 或者其他依赖注入的框架，需要使用 lateinit，[官方 Demo](https://github.com/android/architecture-components-samples.git)
   2. 使用 ViewBinding 对 View 做判空，[官方 Demo](https://github.com/android/sunflower.git)

## 对于创建保存数据的类，kotlin 中被称为数据类，应当使用 _data_ 关键字

```kotlin
data class User(val name: String, val age: Int)

```

- 在 JVM 中，如果生成的类需要含有一个无参的构造函数，则所有的属性必须指定默认值。

  ```kotlin
  data class User(val name: String = "", val age: Int = 0)
  ```

- 数据类中的成员应该是不可变的

  正例

  ```kotlin
  data class ImmutableDataClass(
    val i: Int,
    val s: String?
  )
  ```

  反例

  ```kotlin
    data class MutableDataClass(var i: Int) {
      var s: String? = null
    }
  ```

## 对于转换操作符，我们应该使用安全的转换操作符

正例

```kotlin
val x: String? = y as? String
// 请注意，尽管事实上 as? 的右边是一个非空类型的 String，但是其转换的结果是可空的。
```

反例

```kotlin
val x: String = y as String
// null 不能转换为 String 因该类型不是可空的， 即如果 y 为空，上面的代码会抛出一个异常。
```

## 字符串使用规范

- 优先使用字符串模板而不是字符串拼接。

- 优先使用多行字符串而不是将 `\n` 转义序列嵌入到常规字符串字面值中。

- 如需在多行字符串中维护缩进，当生成的字符串不需要任何内部缩进时使用 `trimIndent()`，而需要内部缩进时使用 `trimMargin()`。

  ```kotlin
  assertEquals(
   """
   Foo
   Bar
   """.trimIndent(),
   value
   )
  
   val a = """if(a > 1) {
         |    return a
         |}""".trimMargin()
  ```

## 返回 `kotlinx.coroutines.flow` 中的 `Flow`，不要声明为 `suspend` 方法

理由：kotlin 中的 flow 可以理解为 cold flow, 只有当对 flow 返回的结果执行 collect 操作
的时候，才会去执行 flow 中的代码，我们不希望 suspend 多 flow 函数产生副作用。

正例

```kotlin
fun observeSignals(): Flow<Unit> {
  return flow {
      val pollingInterval = getPollingInterval() // Moved into the flow builder block.
      while (true) {
          delay(pollingInterval)
          emit(Unit)
        }
    }
}

private suspend fun getPollingInterval(): Long {
    // Return the polling interval from some repository
    // in a suspending manner.
}
```

反例

```kotlin
suspend fun observeSignals(): Flow<Unit> {
  val pollingInterval = getPollingInterval()
  // Done outside of the flow builder block.
  return flow {
      while (true) {
          delay(pollingInterval)
          emit(Unit)
        }
    }
}

private suspend fun getPollingInterval(): Long {
    // Return the polling interval from some repository
    // in a suspending manner.
}
```

## 减少使用带有标签的表达式

正例

```kotlin
val range = listOf<String>("foo", "bar")
for (r in range) {
  if (r == "bar") break
  println(r)
}

class Outer {
  inner class Inner {
      fun f() {
          val outer = this@Outer
      }
      fun Int.extend() {
          val inner = this@Inner // this would reference Int and not Inner
      }
   }
}

```

反例

```kotlin
val range = listOf<String>("foo", "bar")
loop@ for (r in range) {
  if (r == "bar") break@loop
  println(r)
}

class Outer {
  inner class Inner {
      fun f() {
          val i = this@Inner // referencing itself, use `this instead
        }
    }
}
```

## 对于非空的调用链，过多的空检查是冗余的，应该用 `run{}` 代替

理由：非空的安全调用链是冗余的，可以通过将冗余安全调用放在 run {}块中来删除
。由于消除了多余的空检查，因此可以提高代码覆盖率并降低循环复杂性。

正例

```kotlin
val x = getenv()?.run {
  getValue("HOME")
      .toLowerCase()
      .split("/")
} ?: emptyList()
```

反例

```kotlin
val x = System.getenv()
  ?.getValue("HOME")
  ?.toLowerCase()
  ?.split("/") ?: emptyList()

```

## 对于多次重复使用的字符串变量，应该提前声明

正例

```kotlin
class Foo {
  val lorem = "lorem"
  val s1 = lorem
  fun bar(s: String = lorem) {
      s1.equals(lorem)
  }
}
```

反例

```kotlin
class Foo {
  val s1 = "lorem"
  fun bar(s: String = "lorem") {
      s1.equals("lorem")
  }
}

```

## 优先使用 kotlin 定义的数组

理由：Kotlin 具有专门的数组来表示原始类型，而没有装箱开销，例如 IntArray，ByteArray 等。

正例

```kotlin
fun function(array: IntArray) { }
fun returningFunction(): DoubleArray { }
val aArray = intArrayOf(1, 2, 3)
```

反例

```kotlin
fun function(array: Array<Int>) { }
fun returningFunction(): Array<Double> { }
val bArray = arrayOf(4, 5, 6)
```

## 减少 ForEachOnRange 的使用，会造成较高性能消耗

理由；它不是普通的 for 循环的语法糖，还有诸多参数和上下文需要在执行的时候考虑进来，
这里可能拖慢性能

正例

```kotlin
for (i in 1..10) {
   println(i)
}

```

反例

```kotlin
(1..10).forEach {
   println(it)
}
(1 until 10).forEach {
   println(it)
}
(10 downTo 1).forEach {
   println(it)
}

```

## 减少 SpreadOperator 的使用，因为它会将数组完全复制

正例

```kotlin
// array copy skipped in this case since Kotlin 1.1.60
val foo = bar(*arrayOf("value one", "value two"))

// array not passed so no array copy is required
val foo2 = bar("value one", "value two")

fun bar(vararg strs: String) {
    strs.forEach { println(it) }
}
```

反例

```kotlin
val strs = arrayOf("value one", "value two")
val foo = bar(*strs)

fun bar(vararg strs: String) {
    strs.forEach { println(it) }
}
```

## 使用下标操作替代 get 和 set 方法

正例

```kotlin
val map = Map<String, String>()
map["key"] = "value"
val value = map["key"]
```

反例

```kotlin
val map = Map<String, String>()
map.put("key", "value")
val value = map.get("key")
```

## 不要显式地调用任何 `gc` 方法

反例

```kotlin
System.gc()
Runtime.getRuntime().gc()
System.runFinalization()
```

## 当 lambda 作为方法的参数时可根据以下两种情况考虑使用 inline 关键字进行优化

- 当方法体中使用循环对 lambda 进行调用时，使用 inline 可减少频繁调用方法带来的隐性性能开销。

  正例：

  ```kotlin
  public inline fun <T> Iterable<T>.forEach(action: (T) -> Unit): Unit {
      for (element in this) {
          action(element)
      }
  }
  ```

  反例：

  ```kotlin
  public fun <T> Iterable<T>.forEach(action: (T) -> Unit): Unit {
      for (element in this) { 
          action(element)
      }
  }
  ```

- 当外部循环调用该方法时，使用 inline 可以避免频繁创建匿名对象

  ```kotlin
  fun getName(uid: Int, callback: (String) -> Unit) {
      callback.invoke("xxx")
  }
  
  fun foo() {
      for (id in 1..1000) {
          getName(id) {
              print("id=$id, name=$it")
          }
      }
  }
  ```

  此时`getName`方法为非 inline ，查看`foo`方法 deCompile 成 Java 后的代码:

  ```java
  public final void foo() {
      final int id = 1;
      for(short var2 = 1000; id <= var2; ++id) {
          this.getName(id, (Function1)(new Function1() {
          public Object invoke(Object var1) {
              this.invoke((String)var1);
              return Unit.INSTANCE;
          }
  
          public final void invoke(@NotNull String it) {
              Intrinsics.checkNotNullParameter(it, "it");
              String var2 = "id=" + id + ", name=" + it;
              boolean var3 = false;
              System.out.print(var2);
          }
          }));
      }
  }
  ```

  将`getName`方法修改为 inline ，查看`foo`方法 deCompile 成 Java 后的代码:

  ```java
  public final void foo() {
      int id = 1;
      for(short var2 = 1000; id <= var2; ++id) {
          int $i$f$getName = false;
          String it = "xxx";
          int var6 = false;
          String var7 = "id=" + id + ", name=" + it;
          boolean var8 = false;
          System.out.print(var7);
      }
  }
  ```

  另外，如果 lambda 内部没有调用外部的变量，即使`getName`为非 inline 也不会频繁创建对象，而是使用单例，如下方例子中的`foo`方法，

  ```kotlin
  fun foo() {
      for (id in 1..1000) {
          getName(id) {
              print("name=$it")
          }
      }
  }
  ```

  查看`foo`方法 deCompile 成 Java 后的代码:

  ```java
  public final void foo() {
      int id = 1;
      for(short var2 = 1000; id <= var2; ++id) {
          this.getName(id, (Function1)xxx$foo$1.INSTANCE); // 其中 xxx 为当前所在类的类名
      }
  }
  ```

  考虑到后续代码维护的过程中无法保证在 lambda 中不访问外部的变量，建议这种场景也给`getName`加上  inline 关键字。需要注意的是，inline 方法会导致编译生成的字节码膨胀，如果 inline 方法的方法体比较大并且会被多处大量调用，需要根据实际项目权衡是否使用 inline ，避免造成负优化。

## 对集合使用多个操作符时，可以使用asSequence适当降低时间复杂度

以如下代码为例：

```kotlin
fun test(): Int {
    val list = listOf(0, 1, 2)
    val list2 = list.map {
        it * 2
    }.filter {
        it > 3
    }
    return list2.size
}
```

反编译的java代码为：

```java
public static final int test() {
   List list = CollectionsKt.listOf(new Integer[]{0, 1, 2});
   Iterable $this$filter$iv = (Iterable)list;
   int $i$f$filter = false;
   Collection destination$iv$iv = (Collection)(new ArrayList(CollectionsKt.collectionSizeOrDefault($this$filter$iv, 10)));
   int $i$f$filterTo = false;
   Iterator var7 = $this$filter$iv.iterator();
   Object element$iv$iv;
   int it;
   boolean var10;
   while(var7.hasNext()) {
      element$iv$iv = var7.next();
      it = ((Number)element$iv$iv).intValue();
      var10 = false;
      Integer var12 = it * 2;
      destination$iv$iv.add(var12);
   }
   $this$filter$iv = (Iterable)((List)destination$iv$iv);
   $i$f$filter = false;
   destination$iv$iv = (Collection)(new ArrayList());
   $i$f$filterTo = false;
   var7 = $this$filter$iv.iterator();
   while(var7.hasNext()) {
      element$iv$iv = var7.next();
      it = ((Number)element$iv$iv).intValue();
      var10 = false;
      if (it > 3) {
         destination$iv$iv.add(element$iv$iv);
      }
   }
   List list2 = (List)destination$iv$iv;
   return list2.size();
}
```

有两个可以注意的点：

1. 每个运算符都会创建一个集合
1. 每个运算符都会将上一层的集合进行一次遍历

由此假设初始集合的长度为n，使用了m个操作符，则时间复杂度为`mO(n)`。当n比较大时，对性能有一定的影响

改进措施：可以使用`asSequence`操作符：

```kotlin
fun test(): Int {
    val list = listOf(0, 1, 2)
    val list2 = list.asSequence()
    .map {
        it * 2
    }.filter {
        it > 3
    }.toList() // 注意这里要调用asList
    return list2.size
}
```

对应的java代码为：

```java
public static final int test() {
   List list = CollectionsKt.listOf(new Integer[]{0, 1, 2});
   List list2 = SequencesKt.toList(SequencesKt.filter(SequencesKt.map(CollectionsKt.asSequence((Iterable)list), (Function1)null.INSTANCE), (Function1)null.INSTANCE));
   return list2.size();
}
```

这里的两个`Function1`对应上面的两个lambda表达式。应用sequence后，每调用一次操作符，会创建一个Sequence对象：

```kotlin
public fun <T, R> Sequence<T>.map(transform: (T) -> R): Sequence<R> {
    return TransformingSequence(this, transform)
}

internal class TransformingSequence<T, R>
constructor(private val sequence: Sequence<T>, private val transformer: (T) -> R) : Sequence<R> {
    override fun iterator(): Iterator<R> = object : Iterator<R> {
        val iterator = sequence.iterator()
        override fun next(): R {
            return transformer(iterator.next())
        }

        override fun hasNext(): Boolean {
            return iterator.hasNext()
        }
    }

    internal fun <E> flatten(iterator: (R) -> Iterator<E>): Sequence<E> {
        return FlatteningSequence<T, R, E>(sequence, transformer, iterator)
    }
}
```

对操作符的调用转换成了对迭代器的操作，这样最终一次遍历就完成了。
需要注意的是sequence是延迟处理，需要调用`toList`这样的操作符才会真正执行。

> 通常来说使用asSequence对性能影响不大，因为终端处理的集合的量级一般都很小。仅作为知识点供参考