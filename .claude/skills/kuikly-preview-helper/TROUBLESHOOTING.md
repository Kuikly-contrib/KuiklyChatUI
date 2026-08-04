# 故障排查速查

## 预览环境问题

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| 预览空白 | `created()` 中数据加载依赖原生 Module | 添加 Mock fallback 或使用 `inspectionMode` 判断 |
| 预览崩溃 (NPE) | `lateinit var` 未初始化 / Module 返回 null | 使用 `by observable(默认值)` 替代 / 添加空安全 |
| 预览不显示 | IDE 环境未正确配置 | 参考 [ENV-SETUP.md](ENV-SETUP.md) 检查配置 |
| 图片不显示 | 本地 assets 路径错误或需要鉴权 | 使用 Mock 图片 URL（见 `references/mock-data.md`） |
| 主题不正确 | ThemeManager 读取了 SP 配置 | 使用默认 light 主题，预览时无需配置 |

## 编译问题

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| 编辑后不刷新 | 增量编译未触发 | 多保存几次（Ctrl+S / Command+S） |
| 多模块编译冲突 | 模块间依赖冲突 | clean 对应报错模块后重新编译 |
| 内联函数增量编译失效 | Kotlin Multiplatform 插件问题 | 禁用 Kotlin Multiplatform 插件 |
| 全量编译耗时长 | 首次编译正常现象 | 只需首次全量编译，后续增量即可 |

## 依赖注入问题

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| Module 调用返回 null | 预览环境无原生桥接 | 在 `createExternalModules()` 中注册 Module |
| SharedPreferences 读取失败 | 预览环境无 SP 存储 | 添加默认值保护：`takeUnless { it.isEmpty() } ?: "default"` |
| 夜间模式 NPE | `nightModel!!` 强制解包 | 提供安全的默认值，避免强制解包 |
| 网络请求失败 | 预览环境无网络能力 | 使用 `inspectionMode` 判断，预览时用 Mock 数据 |

## 注解问题

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| @KPreview 不生效 | SDK 版本过低 | 升级到 SDK ≥ 2.16.0-beta |
| 旧版本无法直接预览 | 不支持 @Page 类上加注解 | 使用预览类模式：`@KPreview(kuiklyPageClass = Xxx::class)` |
| Compose DSL 无法预览 | 预览函数有参数 | 创建无参预览函数，传入默认参数 |

## 日志获取

当预览出现问题时，获取日志用于排查：

1. 操作路径：**Help** → **Show Idea Log**
2. 拷贝 `idea.log` 文件副本
3. 在日志中搜索 `KuiklyPreview` 或 `KPreview` 相关错误信息

### 错误日志关键字速查表

| 日志关键字 | 含义 | 常见原因 |
|-----------|------|----------|
| `KuiklyPreview` | 预览框架通用日志前缀 | 预览系统内部异常 |
| `KPreview` | 注解处理相关 | 注解配置或 KSP 处理异常 |
| `NullPointerException` | 空指针 | `lateinit var` 未初始化或 Module 返回 null |
| `ClassNotFoundException` | 类找不到 | `ui-tooling` 依赖未正确添加 |
| `NoSuchMethodError` | 方法不存在 | SDK 版本与 `ui-tooling` 版本不匹配 |
| `CompilationException` | 编译失败 | Kotlin 或 KSP 编译错误 |
| `RenderException` / `RenderError` | 渲染失败 | 预览代码中有运行时异常 |
| `TimeoutException` | 超时 | 预览编译或渲染超时，代码可能有阻塞操作 |
| `inspectionMode` | 预览模式标志 | 预览/真实环境分支逻辑异常 |
| `Module not found` | Module 未注册 | `createExternalModules()` 中缺少注册 |

## 快速诊断

```bash
# 检查 Android Studio 版本
# Help → About Android Studio

# 检查 Runtime 是否为 Chrome 内核
# Help → About Android Studio → 查看 Runtime 信息

# 检查插件是否安装
# Settings → Plugins → 搜索 Kuikly Preview
```

## 错误恢复流程

```
预览失败
    │
    ├─→ 预览空白/崩溃？
    │       │
    │       └─→ 检查 inspectionMode + Mock 数据
    │
    ├─→ 编译错误？
    │       │
    │       └─→ clean 模块 → 重新编译
    │
    ├─→ 依赖注入错误？
    │       │
    │       └─→ 检查 createExternalModules + 空安全
    │
    └─→ 环境问题？
            │
            └─→ 参考 ENV-SETUP.md 重新配置
```

## 参考文档

- **环境配置**：[ENV-SETUP.md](ENV-SETUP.md)
- **全局依赖注入**：[references/global-dependency-injection.md](references/global-dependency-injection.md)
- **Mock 数据**：[references/mock-data.md](references/mock-data.md)
- **重构检查清单**：[references/refactoring-checklist.md](references/refactoring-checklist.md)
