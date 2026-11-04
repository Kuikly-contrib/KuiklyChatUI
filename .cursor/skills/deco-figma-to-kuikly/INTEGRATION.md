# 转码产物集成指南

> 💡 **快捷方式**：`bash scripts/deco-integrate.sh --project-dir <工程根>` 自动查找产物、推断目录、复制文件、提示路由注册。
> 集成命令**必须**带 `--project-dir`，**直接实跑**（脚本自动推断路径，歧义时 exit 5 输出路径候选 JSON）。

## 产物目录结构

转码成功后，产物位于 Deco CLI 输出目录：

```
<deco-output-base>/
  {TaskName}_{date}_{seq}/
    assets/                   # 图片资源
      icon_xxx.png
      bg_xxx.svg
    assets-manifest.json      # 资源清单（含 CDN/COS URL）
    ComponentName.kt          # 生成的 Kotlin 代码
```

> 产物目录位置因 Node.js 管理工具而异（npm / volta / nvm）。`deco-convert.sh` 和 `deco-integrate.sh` 会自动探测。

手动查找最新输出目录：

```bash
# 方式 1：从 deco-convert.sh 的缓存读取
cat /tmp/deco-latest-output

# 方式 2：从转码输出日志中查找路径（看 "Kotlin 代码:" 或 "输出目录:" 行）

# 方式 3：手动查找
ls -td "$(npm root -g)/@tencent/deco/output"/*/ | head -1
```

---

## 集成步骤

### 1. 复制 .kt 文件

将生成的 `.kt` 文件复制到项目中存放页面代码的目录：

```bash
# 默认：直接实跑，脚本自动推断 pages-dir / assets-dir
bash scripts/deco-integrate.sh --project-dir /path/to/project

# 或指定目标目录（用户/exit 5 指定时）
bash scripts/deco-integrate.sh --project-dir /path/to/project --pages-dir src/commonMain/kotlin/<pkg>/pages/
```

脚本自动处理：
- **文件重命名**：去掉日期后缀（`Foo_2026-03-16_12-43.kt` → `Foo.kt`）
- **package 修复**：将 Deco 默认 package 替换为目标目录对应的 package（从 `commonMain/kotlin/` 后的路径自动推断）

### 2. 复制图片资源（脚本已自动完成）

`deco-integrate.sh` 会把产物 `assets/` 下的文件按**相对路径**复制到 **`<commonMain>/assets/`**（KMP 项目共享资源目录约定）。例如 `assets/common/foo.png` → `commonMain/assets/common/foo.png`（与 `ImageUri.commonAssets("foo.png")` 一致）。脚本从 `--pages-dir` 中提取 `commonMain` 段并把 `kotlin/...` 替换为 `assets` 得到默认目标，可用 `--assets-dir` 覆盖。

```bash
# 默认：图片复制到 <commonMain>/assets/
# 例如 pages-dir=shared/src/commonMain/kotlin/com/example/pages
#       → assets-dir=shared/src/commonMain/assets
bash scripts/deco-integrate.sh --project-dir /path/to/project

# 显式覆盖资源目录（项目使用其它资源约定时）
bash scripts/deco-integrate.sh --project-dir /path/to/project --assets-dir shared/src/commonMain/composeResources/files
```

行为细节：
- **写入前预检，一次实跑**：脚本在复制 `.kt` / 图片**之前**按相对路径（如 `common/foo.png`）检查 `assets/` 同名冲突。无冲突 → 直接全部落盘；有冲突 → **exit 6（此时未写入任何文件）**。
- **冲突后只问一次、实跑重试**：Agent 展示冲突清单让用户选 skip / overwrite，然后用**相同参数** + `--on-conflict skip|overwrite` **实跑**，脚本按策略直接落盘。
  - `skip` = 跳过同名（保留目标已有资源，**推荐**）
  - `overwrite` = 覆盖（用产物替换目标已有资源）
- **assets-manifest.json**：存在时仅作为信息提示（部分图片可能已上传 CDN/COS，代码可能引用网络 URL），本地副本仍会一并拷贝作为离线/兜底资源
- **pages-dir 不含 `/kotlin/`**：脚本无法推导 commonMain/assets → **exit 5**（`assets_dir_unknown`），Agent 询问后用 `--assets-dir` 指定

> **路径歧义（exit 5）**：脚本无法唯一推断 pages-dir / assets-dir 时 exit 5（未写入），输出 `---DECO_INTEGRATE_PROBE_JSON---` 块，含 `reason` / `pages_candidates` / `pages_status` / `assets_status`。Agent 按 `reason` 询问用户后带 `--pages-dir` 和/或 `--assets-dir` 重跑。无需单独的预检步骤——直接实跑遇到 exit 5 即可拿到这份 JSON。

### 3. 注册路由

页面路由注册方式取决于项目配置：

- **Compose DSL + `@Page` 注解**：自动注册，无需手动操作
- **传统 DSL / 无 `@Page` 注解**：在项目的路由配置文件中手动添加导航入口

> 集成脚本会自动检测 `@Page` 注解并提示路由信息。
