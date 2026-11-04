# 10、元编程

**tl;dr**

- **应当** 注意 Kotlin 的类引用的实例不是唯一的
- **应当** 充分理解反射的使用成本，避免在性能敏感的场景下使用反射
- **应当** 在规模可控的情况下，优先考虑使用代码生成技术
- **应当** 优先使用 KSP 而不是 KAPT，以获得更好的编译性能和开发体验

## 类引用的实例不是唯一的

Kotlin 语言规范并未规定运行时类引用（例如`Foo::class`）或成员引用（`Foo::prop`、`Foo::func`）对象的实例是唯一的。

**反例**

对 `Foo::class` 进行加锁，导致加锁失败。

```Kotlin
object Worker {
    fun submitTask(task: Task) {
        // Worker::class 每次返回的实例可能不同
        synchronized(Worker::class) {
            ...
        }
    }
}
```

我们虽然总是可以通过`Worker::class` 表达式获取到 `Worker` 类的类引用，但是表达式的结果可能是不同的实例。这会导致每次调用 `submitTask` 时加锁的对象可能是不同的，进而导致加锁行为不符合预期。

**正例**

 `Foo::class` 的结果可能不是唯一的，应避免对 `Foo::class` 进行加锁。

```Kotlin
object Worker {
    fun submitTask(task: Task) {
        // 改为对 this 对象加锁
        synchronized(this) {
            ...
        }
    }
}
```

本例中，可以直接对 Worker 实例加锁。在 JVM 平台上，也可以对它的 Java 类引用（即 `Worker::class.java` ）进行加锁，因为 Java 类引用实例在类的生命周期内是唯一的。

**小结**

不应假设 `Foo::class`、`Foo::prop`、`Foo::func` 等引用对象实例的唯一性。


## 理解反射的使用成本

Kotlin 反射存在显著的性能开销和维护成本，在使用 Kotlin 反射之前应当充分理解这些问题。

**反例**

在应用程序启动或高频调用场景中使用 Kotlin 反射来初始化或访问成员。

```Kotlin
// 本例省略了对反射的异常处理
// 假设这个函数在应用启动时被调用
fun initializeComponent(className: String) {
    // 首次加载 KClass，消耗大量启动时间
    val kClass = Class.forName(className).kotlin 
    val component = kClass.createInstance()
    val kFunc = kClass.declaredFunctions.firstOrNull { it.name == "initialize" } 
    kFunc?.call(instance, ...)
}
```

Kotlin 反射的信息在首次被访问时需要从 `@Metadata` 注解中反序列化，这个过程非常耗时。
**正例**

通过良好的设计和抽象，将需要在启动时初始化的模块规范化，避免使用反射来处理加载逻辑。如代码所示：

```kotlin
interface InitializationComponent {
    fun initialize(args: Args)
}

fun initializeComponent(component: InitializationComponent) {
    ... // 处理加载前的逻辑
    component.initialize(...)
    ... // 处理加载后的逻辑
}
```


如果必须使用反射实现动态加载执行的逻辑，使用 Java 反射是更好的选择。

```kotlin
// 本例省略了对反射的异常处理
// 假设这个函数在应用启动时被调用
fun initializeComponent(className: String) {
    val jClass = Class.forName(className)
    val instance = jClass.constructors.firstOrNull()?.newInstance()
    val method = jClass.declaredMethods.firstOrNull { it.name == "initialize" }
    method?.invoke(instance, ...)    
}
```

得益于 Java 虚拟机的直接支持，Java 反射相比 Kotlin 反射有更好的性能表现。
**小结**

Kotlin 反射不适合用于性能敏感的业务逻辑。应当避免使用反射，必要时也应优先考虑使用 Java 反射而不是 Kotlin 反射。

## 优先考虑使用代码生成技术

对于序列化、依赖注入、路由等需要元编程的场景，应优先使用代码生成技术而非运行时反射。代码生成将元编程的开销从运行时转移到编译时。

**反例**

使用 Kotlin 反射在运行时实现一个对象工厂，该函数不适用于性能敏感的场景。

```Kotlin
fun <T: Any> createInstance(kClass: KClass<T>): T {
    return kClass.constructors.first().call() 
}
```

**正例**

使用代码生成技术可在编译期生成高效、可追溯的实现代码。

```Kotlin
// 开发者编写
@Factory
class MyService

// 编译时生成
class MyServiceFactory {
    fun createMyService(): MyService {
        // 生成的代码，直接调用构造函数，无反射开销
        return MyService() 
    }
}

// 运行时直接调用生成的代码
val service = MyServiceFactory().createMyService() 
```

编译时生成的代码具有运行性能好、方便追溯和维护等优势。当然，相比使用反射，通常会有产物体积偏大的问题，适用于性能要求高，生成的代码规模可控的场景。

**小结**

代码生成技术可用编译时开销换取运行时性能和维护性。在规模可控的情况下，应当优先考虑代码生成技术，以获得更好的运行时性能表现和用户使用体验。


## 优先使用 KSP 而不是 KAPT

在 Kotlin 项目中，应优先使用 Kotlin 符号处理器 (Kotlin Symbol Processing，KSP) 来编写和执行代码生成器，而不是使用基于 Java 编译器实现的 Kotlin 注解处理器 (Kotlin Annotation Processing Tool，KAPT)。

KAPT 存在以下显著的问题：

1. **难以理解 Kotlin 语法**：KAPT 本质上是 Java 的注解处理器，它处理的是 Kotlin 编译器生成的 Java 存根（Stubs）。这些 Java 存根丢失了大量的 Kotlin 特有的语法信息（如 `internal` 可见性、默认参数值、泛型型变等），使得处理器无法正确理解 Kotlin 代码。
2. **编译流程慢**：KAPT 需要先将 Kotlin 源码编译成 class 文件，然后再将这些 class 文件反编译成 Java 存根，最后才能用于 Java 编译器执行注解处理器。
3. **不支持多平台**：KAPT 依赖 Java 编译器，不支持 Kotlin JVM 以外的其他平台。

而 KSP 是基于 Kotlin 编译器插件 API 实现的注解处理器，无需额外生成 Java 存根，可以直接处理 Kotlin 符号，也支持 Kotlin 多平台，相比之下存在显著的优势。

**小结**

KSP 是 Kotlin 项目中实现代码生成的最佳方案。

## 扩展阅读

1. **Kotlin 官方文档**
   - [Kotlin Symbol Processing API](https://kotlinlang.org/docs/ksp-overview.html)
   - [Reflection](https://kotlinlang.org/docs/reflection.html)
2. **《深入实践 Kotlin 元编程》**
   - 第 3 章：运行时的反射
   - 第 5 章：编译时的符号处理

## 总结

Kotlin 的元编程主要涉及反射和代码生成。运行时反射（尤其是 Kotlin 反射）存在显著的性能开销，不应用于性能敏感的业务场景中。应优先选择代码生成技术（如 KSP） 而非运行时反射，以保证运行时性能和可维护性。