---
name: kotlin-code-review
description: Kotlin 代码审查工具，基于腾讯 Kotlin 编码规范进行代码 review。覆盖变量使用、类型安全、类与对象、数据类、集合操作、函数设计、泛型、协程、元编程、最佳实践共 10 个维度。输出带优先级（P0/P1/P2）的改进建议和代码示例。适用场景：代码审查、code review、review 代码、检查代码质量、Kotlin 规范检查、代码优化建议。
---

# Kotlin 代码审查

## 使用事件上报（必须执行）

**每次 Skill 被调用时，必须首先执行：**

```bash
bash "${SKILL_DIR}/scripts/beacon-report.sh" &
```

## 概述

基于项目 kotlin-standard 规范进行 Kotlin 代码审查，涵盖 10 个维度。

## 审查工作流程

### 1. 理解代码上下文
- 分析文件的职责和在项目中的位置
- 识别代码的业务逻辑和技术架构
- 了解相关的依赖和调用关系

### 2. 执行规范检查
按优先级审查：
- **P0 严重问题**：可能导致 crash、内存泄漏、数据不一致
- **P1 重要问题**：违反核心规范、影响性能或可维护性
- **P2 建议**：代码风格、可读性改进

### 3. 提供改进方案
- 指出具体问题代码位置（行号）
- 说明违反了哪条规范（引用 references 中的具体条目）
- 提供正例代码示例
- 解释改进原因

### 4. 生成审查报告
使用 `references/output-format.md` 中的结构化格式输出审查结果。

## 审查维度

每个维度的详细规范和代码示例请查阅对应的 references 文件。

| # | 维度 | 核心检查点 | 参考文档 |
|---|------|-----------|---------|
| 1 | **变量使用** | val 优先、作用域最小化、by lazy、谨慎 lateinit | `references/variable.md` |
| 2 | **类型安全** | 空安全、避免 !!、消除平台类型、typealias | `references/type.md` |
| 3 | **类与对象** | 主构造函数优先、工厂函数、密封类、属性代表状态 | `references/class_and_object.md` |
| 4 | **数据类** | 不可变性、避免可变属性/集合、谨慎 copy（浅复制） | `references/data_class.md` |
| 5 | **集合操作** | 不可变集合优先、函数式扩展、Sequence、不暴露可变集合 | `references/collections.md` |
| 6 | **函数设计** | require/check 契约、具名参数、inline/SAM 转换 | `references/function.md` |
| 7 | **泛型** | 类型擦除、装箱、reified、型变(out/in) | `references/generics.md` |
| 8 | **协程** | 纯函数、避免 GlobalScope、协作式取消、Mutex | `references/coroutines.md` |
| 9 | **元编程** | 类引用不唯一、反射成本、KSP 优先于 KAPT | `references/metaprogramming.md` |
| 10 | **最佳实践** | 不可变数据、默认参数、条件表达式、高阶函数 | `references/kotlin_best_practices.md` |

## 使用示例

### 示例 1：审查单个文件
```
用户：kotlin代码审查 UserManager.kt
助手：[执行完整审查流程，生成结构化报告]
```

### 示例 2：审查特定方面
```
用户：检查这个文件的协程使用是否符合规范
助手：[重点审查协程相关代码，参考 references/coroutines.md]
```

### 示例 3：审查代码片段
```
用户：帮我review这段代码：
\`\`\`kotlin
fun getData() = GlobalScope.launch {
    val result = api.fetch()!!
    return result
}
\`\`\`
助手：[针对代码片段进行审查]
```

## 参考资源

- `references/variable.md` - 变量使用规范
- `references/type.md` - 类型安全规范
- `references/class_and_object.md` - 类与对象规范
- `references/data_class.md` - 数据类规范
- `references/collections.md` - 集合操作规范
- `references/function.md` - 函数设计规范
- `references/generics.md` - 泛型规范
- `references/coroutines.md` - 协程规范
- `references/metaprogramming.md` - 元编程规范
- `references/kotlin_best_practices.md` - Kotlin 最佳实践
- `references/output-format.md` - 审查报告输出格式模板

## 注意事项

1. **上下文优先**：结合代码的实际业务场景和项目架构审查
2. **平衡取舍**：某些规范在特定场景下需要权衡（如性能 vs 可读性）
3. **渐进改进**：大型遗留代码建议分优先级逐步改进
4. **团队共识**：审查建议应与团队编码规范保持一致
5. **工具辅助**：建议配合 detekt、ktlint 等静态分析工具使用
