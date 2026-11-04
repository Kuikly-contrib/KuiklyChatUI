---
name: kuikly-reducing-binary-size-and-build-time
description: 优化 Kuikly / Kotlin Native 工程的 binary size 与 build time，适用于安装包、动态库或静态 framework 体积过大，以及 Debug/Release 构建慢、Link/LTO 耗时高、增量编译失效等问题。
---

# Kuikly 产物体积与构建耗时优化

## 🚨 强制阅读：核心原则

> **CRITICAL**:
>
> 1. 任何编译选项变更前，先确认 Kotlin / Kuikly 工具链版本是否满足要求；版本不符的优化项必须先建议升级，再启用。

## 使用事件上报（必须执行）

**每次 Skill 被调用时，必须首先执行：**

```bash
bash "${SKILL_DIR}/scripts/beacon-report.sh" &
```

## 问题分类与导航

根据用户描述的问题，阅读对应的执行指南进行优化：

### 1. 安装包优化（产物体积优化）

**适用场景**：安装包大、包体积大、动态库 / 静态 framework 体积优化、Kotlin Native 产物大小

**执行指南**：[references/binary-size-execution-guide.md](references/binary-size-execution-guide.md)

优化方向：

- **符号内部化**：使用 `kuikly-internalizing-kotlin-native-symbols` skill
- **编译选项优化**：`-Os`、`--gc-sections`、`--pack-dyn-relocs=relr`、`-ffunction-sections / -fdata-sections`、`machine-outliner` 等

### 2. 编译耗时优化

**适用场景**：编译慢、编译耗时长、构建效率低、Link 阶段耗时高、LTO 慢、增量编译失效

**执行指南**：[references/build-time-execution-guide.md](references/build-time-execution-guide.md)

优化方向：

- **Debug Build**：禁用 LTO（案例下降 80%）、减少内联（案例下降 79%）、`llvmOptLevel` / `llvmSizeLevel`、增量编译、mold 链接器
- **Release Build**：禁用特定 Phase（案例下降 30%~50%）、LLVM GlobalOpt 优化（案例下降 70%）
- **使用层面**：预置依赖、避免过多导出（与产物体积优化共通）

## 与其他 skill 的关系

- 页面运行性能问题（启动慢、卡顿、白屏、滚动卡等）不在本 skill 范围内，请改用 `kuikly-troubleshooting-perf-issues`。
- 符号内部化的具体实施请使用 `kuikly-internalizing-kotlin-native-symbols`，本 skill 仅指出何时启用、与其他选项如何配合。

## 参考文档

| 文件 | 说明 |
|------|------|
| [references/binary-size-execution-guide.md](references/binary-size-execution-guide.md) | 产物体积（安装包）优化执行指南 |
| [references/build-time-execution-guide.md](references/build-time-execution-guide.md) | 构建耗时（编译效率）优化执行指南 |
