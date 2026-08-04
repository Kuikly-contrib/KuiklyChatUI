---
name: deco-figma-to-kuikly
description: 通过 Deco CLI 完成四类 Kuikly / Compose 工作流：(1) 将 Figma 设计稿或本地 HTML 转换为 Kotlin 代码——支持三种并列转码格式（Kuikly DSL / Kuikly Compose / 标准 Compose Multiplatform），(2) 对已有代码做增量更新并产出 git patch（deco update），(3) 扫描本地 Kotlin 组件并发布到 Portal，(4) 对设计稿执行规则检查并输出 JSON 报告。适用场景：D2C 转码、Figma/HTML 转 Kuikly、转标准 Compose、deco to-kuikly、deco to-compose、组件扫描、deco scan、团队组件入库、设计稿增量更新、deco update、设计稿规则检查、deco check、用deco审查、交付前验收。
---

# Deco Figma / HTML → Kuikly 转码 / 组件扫描 / 规则检查

通过 Deco CLI 完成四类工作流：

- **D2C 转码**：将 Figma 设计稿或本地 HTML 文件转换为 Kotlin 代码。支持**三种并列转码格式**，框架目标不同、不可混用：
  - **Kuikly DSL**（传统 `attr {}` 写法，`deco to-kuikly-dsl`）
  - **Kuikly Compose**（`com.tencent.kuikly.compose.*`，`deco to-kuikly`）
  - **标准 Compose Multiplatform**（`androidx.compose.*`，`deco to-compose`）—— 跨平台 CMP 工程使用，不直接走 Kuikly 的 `deco-integrate` 落盘逻辑
- **增量更新**：对已有 Kuikly / Compose 代码对照最新设计稿做增量更新，产出 git patch 并**自动 `git apply`**（`deco update`）
- **组件扫描**：扫描本地 Kotlin 工程的 Kuikly 组件并发布到 Portal 草稿，供后续转码 / 配置使用
- **设计稿规则检查**：对 Figma / HTML 设计稿按 Markdown 规则文件审查，输出结构化 JSON 报告

**核心原则：失败即停止** — 任何步骤失败，立即终止并反馈错误，不自动重试、不跳过。

### ⚠️ Agent 行为约束（必须严格遵守，无例外）

> **这些规则的优先级高于你的默认行为。违反任何一条都是错误。**

1. **判断成败以输出内容为准**（退出码仅作辅助）：
   - ✅ **成功**：输出含 `✔ 转码完成` / `✓ 生成成功` / `✔ 检查完成` → 继续后续步骤（即使退出码非 0）
   - ❌ **硬失败**：输出含 `✗`（全角叉号）/ `FAILED` / `处理失败` → 立即终止，按 Rule 4 反馈
   - ⚠️ **空输出**（同轮完全无 stdout/stderr）：**不要**当失败，按 Rule 5 判为用户取消
   - ⚠️ "Error" 等词可能是正常日志，结合上下文判断，不要见 "Error" 就判失败
   - **退出码三分类**（贯穿全文）：
     - **成功**：exit 0 或含成功输出
     - **硬失败**：exit 1 或含 `✗` → 停止，等用户指示（Rule 2/3/4/8）
     - **决策点**：exit 2/3/4/5/6 → **不是失败**，按对应 Step 拿到用户输入 / 登录后继续，不算「失败后重试」

2. **硬失败后禁止绕过脚本**（仅针对 exit 1 / `✗`，决策点退出码不在此限）：
   - ❌ 手动拆分脚本步骤逐个执行
   - ❌ 跳过失败步骤继续后续步骤
   - ❌ 用 `deco` 命令直接执行转码（绕过脚本封装）
   - ❌ 以"查看更详细输出" / "URL 特殊字符"等理由换方式重跑——重跑同一脚本不会让失败变成功，脚本默认输出已是为排查准备的全部信息
   - ❌ 读脚本源码自行推断失败原因后换姿势重跑（排查交给用户，Agent 只反馈错误并停止）
   - ❌ 执行 `deco --version`、`nc`、`curl`、`ls`、`cat`、`which` 等"诊断命令"自行排查
   - ❌ 对 Skill 脚本加**本 Skill「脚本工具」表未列出**的参数（含 `--help` / `-h`、`bash -x`、`| tail` 等）——用法以 SKILL.md 为准，**禁止**靠命令行查帮助或试探未文档化 flag
   - ❌ **exit 5/6 后**以「解析 stdout」「核对目录」「debug 模式」等名义**无用户明确指令就重跑同一脚本**（含无 `--on-conflict`/`--pages-dir` 的裸跑）——决策信息已在首次 stdout/JSON 中给出，重跑不会得到新信息
   - exit 4（登录态过期）是**唯一**允许 Agent 主动调 `deco login` 的场景：详见 Rule 7 + Step 2

3. **硬失败后禁止自动重试**（零容忍，仅针对 exit 1 / `✗`）：
   - ❌ 加 `2>&1` / 换引号转义 / 拆子命令 / 以"需要 approval"或"命令被拒绝"为由重跑
   - **允许重新执行的情况**：(1) 用户**明确指示**重试（"再跑一次"）；(2) **决策点退出码**（exit 2/3/4/5/6）在拿到用户输入 / 登录后，按对应 Step 带新参数重跑——这是流程推进，**不是**「失败后重试」

4. **失败反馈要简练**（禁止冗长复盘、禁止误判取消）：
   - **判据**：同轮对话里脚本输出已出现 `✗` / `处理失败` / `转码失败` → **就是失败**，立即按本条规定反馈并停止。**禁止**再问「是否取消」「是否自行退出」「要不要重跑看详细输出」
   - **Shell tool 包装信息**：工具层只显示 `Command failed with exit code 1` **不等于**零输出——只要同轮已展示过脚本 stdout（哪怕一行 `ℹ`/`✗`），就必须按脚本内容判成败，**不得**套用 Rule 5 的「取消」逻辑
   - **禁止**重复粘贴整段脚本输出（用户终端里已有）；只提取**一行核心错误** + **1~2 条修复建议**
   - **模板**（直接套用，3~5 行以内）：

     ```
     **Step N 失败**：<一步话错误摘要>

     建议：<最多 2 条，来自 TROUBLESHOOTING.md>

     流程已停止。如需重试请明确告诉我。
     ```

   - 然后**停止一切 tool call**，等用户指示

5. **识别用户取消 / 拒绝 / 跳过 — 立即停止，禁止换参数重试**：

   触发任一信号都应判定为「用户主动叫停」：
   - 工具结果文本明确含 `命令被跳过` / `skipped` / `canceled` / `denied` / `用户取消` / `user rejected` / `tool call denied` 等关键字
   - 退出码非 0 且**同轮对话中完全看不到任何脚本 stdout/stderr**（0 字节）—— 仅此时可判取消；**只要同轮出现过脚本输出**（`✔`/`ℹ`/`⚠`/`✗`/deco 进度行等）就**绝对不是**取消，必须按 Rule 1 / Rule 4 处理

   触发后**强制行为**：

   - ✅ 立即停止整条流程（转码/扫描/集成等），不再调用任何后续 tool call
   - ✅ 用纯文本告知用户：(a) 哪一步被取消 (b) 后续因此停止 (c) 询问是否需要重新执行（等用户明确答复"是/重试"再继续）
   - ❌ **禁止**任何形式的换参数重试，包括但不限于：
     - "让我用更短/更长的超时方式执行" / "调整 `block_until_ms` 再跑"
     - "换 `timeout` 命令包一层重跑"
     - "拆成更小的步骤分别执行"
     - "改成不开浏览器的模式（加 `--no-interactive`）再试"
     - "我先跑一个轻量诊断命令看看"
   - ❌ **禁止**给"换种方式"找任何借口（包括"超时太长"、"参数可能不对"、"脚本可能 hang"）。用户取消就是用户取消，不要替用户决定要不要继续

6. **转码耗时正常 + 长时间静默是常态**：转码通常需要 30s~2min（复杂稿/远程模式可达 3~5min）。deco 的进度输出是**断续的**，可能在某个阶段（尤其是 `正在处理...` / 图片上传 / 远程解析）**连续 1~2 分钟无任何新输出**，这是正常行为，不是卡死：
   - 不得因"等待时间过长"而中断脚本
   - 不得因"长时间没有新输出"而认为脚本卡死
   - 进程是否存活由 tool runtime 自行展示（Cursor / CodeBuddy 都有进度指示），看到进程仍在 running 就继续等
   - 调用 `deco-convert.sh` 时**必须**给足超时时间（如使用 `block_until_ms` 之类参数，至少 300000ms / 5 分钟）
   - 在用户面前主动说明"转码可能静默 1~2 分钟，请耐心等待，不要中途取消"，避免用户误以为卡死手动 kill

7. **浏览器阻塞场景必须高亮提醒用户**：浏览器操作有两类——**确定会发生**（启动前预告即可）与**条件触发**（必须由脚本退出、Agent 介入后再独立执行）。**所有浏览器阻塞提醒必须使用下方的「高亮卡片格式」**，仅一行文字夹在大段执行说明里**用户极易错过**。

   ### 7.1 高亮卡片格式（强制模板）

   提醒消息必须独占成块，**前后用分隔线、标题用 emoji + 大字号粗体、正文用 blockquote 引用框**。Markdown 模板：

   ```markdown
   ---

   ## 🌐 请在浏览器中完成操作

   > **【动作】**：<在浏览器里要做的具体事，例如"完成 Flowly 授权登录" / "完成代码分组配置并关闭页面">
   >
   > **【命令行状态】**：会一直等待，**不要在终端里中断它**
   >
   > **【完成后】**：<操作结束后会发生什么，例如"我会用原参数重新执行转码" / "转码会自动继续">

   ---
   ```

   规范：
   - **必须**带前后水平分隔线（`---`）做视觉隔断
   - **必须**用 `## 🌐` 大标题，emoji 在前抓眼球
   - **必须**用 blockquote (`>`) 包裹正文形成有色侧边栏
   - **必须**有「动作 / 命令行状态 / 完成后」三段，不要省略
   - **不得**把这条提醒和其它解释、命令、TODO 列表挤在同一段；提醒前后**只**留分隔线，不要再叠加无关 chitchat

   ### 7.2 三类触发场景对照

   | 触发条件 | 时机 | 卡片【动作】栏写什么 | 卡片【完成后】栏写什么 |
   |---------|------|---------|---------|
   | 调用 `deco-convert.sh` 默认开启交互模式 | **调脚本前**主动发卡片 | "在浏览器里完成代码分组配置并关闭页面" | "命令行会自动继续转码后续步骤" |
   | `deco-convert.sh` / `deco-scan-components.sh` **退出码 4**（登录态过期） | 收到 exit 4 后**立刻**发卡片，**再**独立调 `deco login` | "在浏览器里完成 Flowly 授权登录" | "我会用原参数重新执行 \<脚本名\>" |
   | `deco-scan-components.sh` 退出码 0 后输出 Portal URL | 脚本返回后用卡片转告 URL | "在打开的浏览器页面里完成扫描结果配置（若未自动打开请手动访问以下 URL：\<URL\>）" | "扫描入库后流程结束" |

   ### 7.3 架构约束（必读）

   Agent 在 Shell tool 同步等待脚本结束之前**无法**向用户插入消息。所以"脚本运行中弹浏览器"必须靠**脚本退出 + Agent 重新介入**实现，不能依赖"流式检测立刻打断"——后者是空头支票。脚本侧已通过 exit 4 把登录这条副流程挤到 Agent 层。

   ### 7.4 强制行为

   - ✅ 看到 exit 4 → **先发高亮卡片** → 再独立调 `deco login`（这次 tool call 也会阻塞等浏览器，但用户已经看到卡片）→ 登录返回后用原参数重跑脚本
   - ✅ 启动交互转码前的预告也必须用高亮卡片，不能只是一句话
   - ❌ **禁止**用单行普通文本提醒（"请在浏览器中完成登录"夹在大段说明里 = 用户看不到 = 等于没提醒）
   - ❌ **禁止**因为"浏览器操作期间 stdout 安静"而判定卡死并 kill 进程
   - ❌ **禁止**自行尝试在 Shell tool 等待期间给用户发消息（架构上做不到）

8. **硬失败后**（仅 exit 1 / `✗`）：按 Rule 4 简练反馈 → 立即停止所有 tool call → 不诊断、不试替代方案 → 等用户指示。（决策点退出码不适用本条，见 Rule 1 分类）

9. **用户决策点：按问题类型选交互方式**（重要）：
   - **闭集合选择题**（是/否、单选、多选，且每个选项都是用户真实可能选的路径）：如果客户端支持结构化询问 UI（Cursor `AskQuestion`、CodeBuddy quick-reply 卡片等），**必须**优先使用结构化 UI；不要只用纯文本来回
     - 典型场景：退出码 2 询问是否启用 `--include-internal`；Step 2 推断不出来时询问 DSL 类型
     - **有倾向性的选项必须显式标注"推荐"**：当某个选项是基于上下文的明显优选路径（例如 exit 2 场景下用户大概率想包含 internal 才能往下走），选项文案末尾追加 `（推荐）` 后缀，让用户一眼看到默认方向；没有倾向性的（如 DSL 类型 compose vs traditional）则不要乱标
     - **问题文案用纯文本**：结构化 UI 通常不渲染 markdown，`**加粗**` / `-` 列表 / `>` 引用 / 多级标题会被原样显示，挤成一坨反而比纯文本更难读。提问内容控制在 2~3 句话以内，关键信息（推荐选项、隐私边界等）用纯文本写明
   - **自由文本输入**（如 Git URL、Namespace、Figma URL 等需要用户敲入具体字符串的场景）：**禁止**套用结构化 UI。即使客户端支持「文本输入 + 取消」组合控件，也不要为了"看起来更专业"而把唯一可点的"暂不提供 / 跳过"塞进选项 —— 用户不回复本来就是跳过，这种 UI 等于把不弹框这件事重新实现一遍，反而打扰用户
     - 典型反例：用单选卡片问 Git 仓库地址，唯一选项是"暂不提供，跳过"
     - 正确做法：直接纯文本问询 + 给出格式示例，等待用户回复 URL 或显式表示放弃
   - **客户端不支持任何结构化交互时**：所有问题统一退化为纯文本问询；有倾向性的选项在文本中标注"（推荐）"
   - **禁止假装提问后自行决定**：不能"询问"后立即默认按某选项处理。提问就必须等待用户答复

## 脚本工具

本 Skill 提供六个可执行脚本，位于 `scripts/` 目录下：

| 脚本 | 功能 | 用法 |
|------|------|------|
| `deco-env-check.sh` | 环境检查（Node.js/npm/Deco CLI >= 0.6.51 检查与自动安装/更新） | `bash scripts/deco-env-check.sh` |
| `deco-convert.sh` | **D2C 转码（Kuikly DSL / Kuikly Compose）**（设计源识别 → URL/HTML 校验、**--project-dir 下** DSL 推断、模式探测、**显式 --git** 转码执行）。第一个参数支持 Figma URL 或本地 `.html` / `.htm` 文件路径，识别为 HTML 时脚本自动追加 `--design-source html` 并跳过 Figma MCP（3845）TCP 探测。检测到登录态过期 → exit 4 让 Agent 介入。**不含**环境检查。注：本脚本**只**产出 Kuikly DSL / Kuikly Compose，不产出标准 CMP | `bash scripts/deco-convert.sh "<source-ref>" [--project-dir <path>] [--git <url>] [--dsl compose\|traditional] [--remote] [--no-interactive] [--image-scale N] [--component-groups business,icon]` |
| `deco-compose.sh` | **D2C 转码为「标准 Compose Multiplatform」**（设计源识别 → URL/HTML 校验 → 模式探测 → 调 `deco to-compose`，产出 `androidx.compose.*` 的 `.kt`）。与 `deco-convert.sh` **并列**的第三种转码格式，框架目标不同、不可混用。检测到登录态过期 → exit 4 让 Agent 介入。**不含**环境检查；**转码成功后自动调用 `deco-compose-integrate.sh` 集成** | `bash scripts/deco-compose.sh "<source-ref>" [--project-dir <path>] [--git <url>] [--remote] [--interactive] [--no-interactive] [--download-assets\|--no-download-assets] [--image-scale N] [--component-groups business,icon] [--prompt-dir <path>]` |
| `deco-compose-integrate.sh` | **标准 CMP 产物集成**（须 `<产物目录>` + `--project-dir`；**直接实跑**）。自动探测 CMP 工程 `commonMain` 根（`src/commonMain` 优先于 `shared/commonMain`），将 `.kt` 写入 `<commonMain>/kotlin/com/example/generated/`，资源写入 `<commonMain>/assets/`。exit 5 = 多个 commonMain 候选需用户决策（输出候选 JSON）；exit 6 = 文件/资源冲突（未写入）→ `--on-conflict` 实跑重试。**仅适用于 `deco-compose.sh` 产出的标准 CMP 产物** | `bash scripts/deco-compose-integrate.sh <产物目录> --project-dir <path> [--pages-dir <path>] [--assets-dir <path>] [--on-conflict skip\|overwrite]` |
| `deco-update.sh` | **增量更新**（对已有 Kuikly / Compose 代码对照最新设计稿做增量更新，产出 git patch）。第一个位置参数为待更新源文件（可多个），必须传 `--source-ref` 指向新设计稿。检测到登录态过期 / 未登录 → exit 4 让 Agent 介入。**不含**环境检查 | `bash scripts/deco-update.sh <file...> --source-ref "<ref>" [--project-dir <path>] [--design-source figma\|html] [-o <output>] [--churn-threshold N] [--remote] [--download-assets\|--no-download-assets] [--image-scale N] [--component-groups business,icon] [--prompt-dir <path>]` |
| `deco-scan-components.sh` | **组件扫描**（输入收集 + 调 deco scan + 0 组件智能提示）。检测到登录态过期 → exit 4 让 Agent 介入。**不含**环境检查，由 Agent 在调用前自行决定是否执行 `deco-env-check.sh` | `bash scripts/deco-scan-components.sh <scan-path> [--git <url>] [--dsl compose\|traditional] [--include-internal] [--namespace <ns>]` |
| `deco-check.sh` | **设计稿规则检查**（设计源 + 规则文件校验 → 调 `deco check` → 输出 JSON 路径与通过/未通过摘要）。检测到登录态过期 → exit 4 让 Agent 介入。**不含**环境检查 | `bash scripts/deco-check.sh "<source-ref>" --rule <rule.md> [--project-dir <path>] [--output <path>] [--remote]` |
| `deco-integrate.sh` | 转码产物集成（**须 `<产物目录>` + `--project-dir`**；**直接实跑**，脚本自动推断页面路径；写入前预检资源冲突）。exit 5 = 路径需用户决策（输出候选 JSON）；exit 6 = 资源冲突（未写入）→ `--on-conflict` 实跑重试。**仅适用于 `deco-convert.sh` 产出的 Kuikly 产物**，不适用于 `deco-compose.sh` 的标准 CMP 产物 | `bash scripts/deco-integrate.sh <产物目录> --project-dir <path> [--pages-dir <path>] [--assets-dir <path>] [--on-conflict skip\|overwrite]` |

> **推荐用法**（三条链路统一规则：环境检查由 Agent 在会话首次调用前执行一次，后续脚本都不重复跑）：
> - 转码场景（Kuikly DSL / Compose）：（首次会话先 `deco-env-check.sh`）→ `deco-convert.sh` → `deco-integrate.sh` → 询问用户是否扫描入团队组件库（D2C 工作流 Step 4）→ 选"是"则 `deco-scan-components.sh`
> - 转码场景（标准 Compose Multiplatform）：（首次会话先 `deco-env-check.sh`）→ `deco-compose.sh`（转码成功后**自动**调 `deco-compose-integrate.sh` 集成到 CMP 工程；若工程有多个 commonMain 或存在冲突，脚本会给出重试命令）
> - 增量更新场景：（首次会话先 `deco-env-check.sh`）→ `deco-update.sh <file...> --source-ref <ref>` → 提示用户 `git apply <patch>`
> - 组件扫描场景：（首次会话先 `deco-env-check.sh`）→ `deco-scan-components.sh`（结果在 Portal 中配置）
> - 规则检查场景：（首次会话先 `deco-env-check.sh`）→ `deco-check.sh` → 解读 JSON 报告 → 建议修复 error 后再转码
>
> ⚠️ **跨平台说明**：脚本自动检测操作系统（macOS / Linux / Windows WSL），适配 `sed`、`mktemp`、`nc` 等命令差异，无需手动区分。

## 自然语言 Prompt 解析（Agent 必读）

用户常以 Portal 用户手册中的紧凑 Prompt 发起任务（如 `用deco转换@...`）。Agent **必须先解析 Prompt，再组装脚本参数**；不要要求用户改说脚本 flag。

### 转码 Prompt → `deco-convert.sh`

| 用户表述（示例） | 脚本参数 |
|-----------------|---------|
| `用deco转换@<figma-url>` / `用deco转换@<path.html>` | 第一个位置参数 `<source-ref>` |
| `转为 Compose` / `Compose 代码` | `--dsl compose` |
| `转为 Kuikly DSL` / `DSL 代码` | `--dsl traditional` |
| 未指定格式 | 省略 `--dsl`，由 `--project-dir` 下工程结构自动推断 |
| `导出 @2x` / `@2x 资源` / `2倍图` / `2 倍图` | `--image-scale 2` |
| `3倍图` / `导出 3x` / `3 倍图` | `--image-scale 3` |
| `非交互式` / `不要弹浏览器` / `跳过交互` | `--no-interactive` |
| `包含组件分组 business,icon` / `启用 business、icon 组件分组` / `包含组件分组 business, icon` | `--component-groups business,icon` |

**组件分组说明**：Portal 按 Git remote 匹配云端项目；`common` 分组**默认启用**，**不要**写进 `--component-groups`。仅非 common 分组（如 `business`、`icon`）需在 Prompt 中显式声明或通过 `--component-groups` 传入。

**`@` 引用解析**：Prompt 中 `@` 后的 URL 或路径即 `<source-ref>`；若同时出现 Figma URL 与 HTML 路径，以用户明确指向转码目标的那个为准。

### 转标准 Compose Prompt → `deco-compose.sh`

| 用户表述（示例） | 脚本参数 |
|-----------------|---------|
| `用deco转标准Compose@<figma-url>` / `转成 Compose Multiplatform` / `标准 CMP` / `androidx.compose` | 走 `deco-compose.sh`（而非 `deco-convert.sh`）；框架目标为 `androidx.compose.*` |
| `转 Compose 但不走 Kuikly` / `纯 CMP` | `deco-compose.sh`（产出标准 `androidx.compose.*`，不依赖 Kuikly 运行时） |
| `下载资源到本地` / `资源本地化` | 默认 `--download-assets`（可不写） |
| `保留远程图片地址` / `不下载资源` | `--no-download-assets` |
| `自定义 prompt 目录 xxx` | `--prompt-dir xxx` |
| 其它参数（`--remote` / `--image-scale` / `--component-groups` / `--git` / `--no-interactive`） | 与 `deco-convert.sh` 同义，照常映射 |

> ⚠️ **compose 与 kuikly-compose 易混淆**：`deco to-compose` = 标准 Compose Multiplatform（`androidx.compose.*`）；`deco to-kuikly`（加 `--dsl compose`）= Kuikly Compose（`com.tencent.kuikly.compose.*`）。两者产物不同、不可互相替代。用户说"转 Compose"时，**必须**先判断他要的是哪一种框架目标，判断不了就问。

### 增量更新 Prompt → `deco-update.sh`

| 用户表述（示例） | 脚本参数 |
|-----------------|---------|
| `用deco更新@./Foo.kt，对照@<figma-url>` / `增量更新 Foo.kt`（单个页面/组件，代码只在一个文件里） | 第一个位置参数 `<file>` = `./Foo.kt`，`--source-ref` = `<figma-url>` |
| `增量更新 @./HomePage.kt 对照 @<figma-url>`（UI 代码可能拆到多个 .kt，用户**只 @ 了入口**） | **Agent 先读入口文件，判断哪些直接关联的 UI 源码也要一起更新**，把入口 + 判断出的关联文件一起作为多个位置参数传给 `deco-update.sh`（见下方「关联 UI 源码的判断与收集」） |
| `更新这几个文件 @a.kt @b.kt 对照 @<figma-url>`（用户**已显式列出**多个文件） | 直接把 `a.kt b.kt` 作为多个位置参数 + `--source-ref <figma-url>`，尊重用户给的集合，不再自行增删 |
| `更新对照本地 html @./design.html` | `--source-ref ./design.html --design-source html` |
| `改动阈值调到 80` / `churn 80` | `--churn-threshold 80` |
| `patch 输出到 ./update.patch` | `-o ./update.patch` |
| 其它参数（`--remote` / `--download-assets` / `--image-scale` / `--component-groups` / `--prompt-dir`） | 与转码脚本同义 |

**关联 UI 源码的判断与收集（只 @ 入口时，由 Agent 判断，不用脚本）**：一个页面的 UI 代码常被拆到多个 `.kt`（子视图组件、页面自己的状态/数据类）。`deco-update.sh` 只把**显式传入的文件**透传给 CLI、**不做依赖展开**，所以用户只 @ 入口时可能漏更新其它文件。这件事**由 Agent 自己读代码判断**，不要用机械脚本代劳——原因见「为什么不用机械收集脚本」。

> ⚠️ **判断标准只有一句话：顺着「这个页面实际渲染出来的视图树」走，收集那些「改这次设计稿会跟着改」的 UI 源码文件。** 别把它拆成层数、命名规则、组件白名单这类机械 proxy——那些反而会误导（真实仓库里文件名常≠类名、真正该收的子视图也常在第二三层）。你会读 Kotlin，就用读代码的本能判断，方法如下：
> 1. **读入口，先认出它是哪种写法**：有 `@Composable` → Compose 系，子视图是入口调用的 `@Composable fun`；有 `override fun body(): ViewBuilder` + `attr {}`（无 `@Composable`）→ Kuikly DSL 系，页面基类是 `: BasePager()`（`@Page` 独立页）或 `: ComposeView<Attr,Event>()`（可复用业务视图，末尾常有 `fun ViewContainer.Xxx(init)` 工厂函数）。
> 2. **看 `body(){}` / `@Composable` 体里实际渲染了哪些子节点**，而不是扫 `import`——自定义子组件常靠通配 `import ...core.views.*`、同包、或工厂函数调用引入，`import` 行里根本看不到。挑出**项目自定义的**子视图（来自本工程包名），排除**框架内置组件**（`com.tencent.kuikly.core.views.*` 里的 `List`/`Text`/`Tabs`/`View`/`Image`/`PageList` 等）。
> 3. **定位每个自定义子视图的 `.kt` 文件：直接在工程里 `search_files "class <组件名>"` 搜类定义，不要靠文件名猜。** 组件调用名、类名、文件名三者经常不一致（真实例：`body()` 里写 `AppFeedListPage {…}`，类是 `AppFeedListPageView`，而文件叫 `AppFeedListPage.kt`）。搜类定义是唯一可靠的定位方式。
> 4. **顺着视图树自然往下追，但只追 UI 视图链、追到不再有自定义子视图就停**：如果一个子视图（如列表页）内部又渲染了自定义卡片、卡片又渲染了作者/正文/操作栏等子区块，这些都是「改这张设计稿会跟着改」的，该收；追到某个视图只剩框架内置组件、纯文字/数据渲染就触底。**这不是机械递归 import 闭包**（那会把工具类、数据层、间接依赖全卷进来指数膨胀）——你追的是「肉眼可见的视图组成关系」，靠判断力在该停的地方停。

- **触发条件**：用户对**一个入口文件**发起增量更新（只 @ 了一个 `.kt`），且没有显式列出其它文件。
- **Agent 动作**：
  1. 读入口 `.kt`，判断它是 Compose 系还是 DSL 系（见上方⚠框第 1 点）。
  2. 看它 `body(){}` / `@Composable` 体里**实际渲染的子节点**，挑出项目自定义子视图（排除框架内置组件），用 `search_files "class <组件名>"` 在工程里定位每个的 `.kt`（见⚠框第 2、3 点）。
  3. **顺着视图组成关系自然往下追**：子视图内部又渲染的自定义子视图/子区块，只要「改这次设计稿会跟着改」就一并收；追到只剩框架内置组件/纯数据渲染就停（见⚠框第 4 点）。判断标准始终是「改设计稿时这个文件会不会跟着改」，不是「在第几层」。
  4. **同时收**这次设计稿改动会波及的页面级状态/数据模型类（Compose 系常是独立 state/data class；DSL 系状态多是页面类内 `by observable` 属性，需收的是其元素/模型类，如 `class ListItem : BaseObject()`）——但只在改动确实触及数据结构时收。
  5. **明确排除**：框架/三方（`androidx.*` / `com.tencent.kuikly.core.*` 内置组件 / `kotlin(x).*` / 各种库）；与视觉无关的纯工具类、网络/持久层、数据请求 manager；**跨页面复用的公共组件/主题/多语言基础设施**（`ThemeManager`/`LangManager` 等——改它会影响别的页面，属于另一次决策，除非用户明确要改；若设计稿新增了主题里还没有的颜色/字体 token，作为存疑项问用户）。
  6. 把**入口 + 判断出的关联文件**一起作为多个位置参数传给 `deco-update.sh`。
- **数量控制**：小页面的关联 UI 文件常是个位数；但一个卡片流/复杂页面顺着视图树追下来到十几个也可能是对的（真实例：`AppHomePageView` 追出 10 个文件，全部合理）。**不要用数字当硬门槛**——数量本身不说明对错，「每个文件是不是这次设计稿改动会跟着改的」才是。把最终清单连同「选了哪些、为什么、排除了什么」一并告诉用户，让用户确认或增删。
- **不触发**：用户**已显式 @ 多个文件**时，尊重用户给的集合，不要再自行增删。
- **拿不准就问**：关联关系判断不清、或候选文件是否该纳入有歧义时，列出你倾向纳入的文件 + 理由问用户，不要闷头替用户决定改哪些文件。

> **为什么不用机械收集脚本**：读 `.kt`、看懂 import、判断「哪些是直接构成这个页面的 UI 源码」本来就是 Agent 的强项。正则脚本做不到——它看不懂 `import a.b.*` 通配与 typealias、分不清 UI 依赖和顺手引的工具类、更没法判断「这次设计稿改动会不会波及某个文件」。而且机械递归 import 会指数级膨胀，把整条依赖闭包全拉进来，既慢又不准。收集阶段该宁多勿漏地**看**，但「选哪几个发给 CLI」是相关性判断，必须由 Agent 做，不能交给脚本按发现顺序机械截断。

**增量更新说明**：`deco update` 产出 git patch（`.patch` 文件），**不直接修改原文件**。Agent 在成功输出后**必须**提示用户执行 `git apply <patch路径>`（或交由用户决定如何应用）。`--churn-threshold` 默认 95，即改动比例超过 95% 会被判定为"大改"而非"增量"，用户可酌情下调以允许更大范围更新。

### 扫描 Prompt → `deco-scan-components.sh`

| 用户表述（示例） | 脚本参数 |
|-----------------|---------|
| `用deco扫描Compose组件@./shared` | `<scan-path>=./shared` + `--dsl compose` |
| `用deco扫描Kuikly DSL组件@./shared` | `<scan-path>=./shared` + `--dsl traditional` |
| `用deco扫描组件@./shared`（未指定格式） | `<scan-path>=./shared`，省略 `--dsl` 由工程推断 |
| `@` 后的路径 | 第一个位置参数 `<scan-path>`（支持 `~/`、相对、绝对路径） |

扫描路径 MUST 指向用户业务工程的 Kotlin 源码目录或 `.kt` 文件，**不要**指向 `.codebuddy/skills/`。

### 审查 Prompt → `deco-check.sh`

| 用户表述（示例） | 脚本参数 |
|-----------------|---------|
| `用deco审查@<figma-url>，规则@docs/rules.md` | `<source-ref>` + `--rule docs/rules.md` |
| `用deco审查@./design.html，规则@./docs/design-check-rules.md` | HTML 路径 + `--rule ./docs/design-check-rules.md`（脚本自动识别 HTML 源） |
| `规则@<path>` | `--rule <path>`（相对路径以 `--project-dir` 解析，默认 cwd） |
| `--output` / `输出到 xxx.json` | `--output <path>`（可选） |

**推荐流程**：交付前 `deco-check.sh` → 修复 JSON 中 `type: error` 的问题 → 再进入 D2C 转码。

## 工作流程（转标准 Compose Multiplatform）

**与「D2C 转码（Kuikly DSL / Compose）」并列**，当用户目标框架是标准 `androidx.compose.*`（跨平台 CMP 工程、不依赖 Kuikly 运行时）时走本流程。产物为独立 `.kt`，**不调用 `deco-integrate.sh`**。

```text
Compose MP 转码进度：
- [ ] Step 1 [Agent]：环境检查（本会话内若未做过，先跑 deco-env-check.sh；做过则跳过）
- [ ] Step 2 [Script]：转标准 Compose — bash scripts/deco-compose.sh "<source-ref>" --project-dir <工程根>
        源识别 → URL/HTML 校验 → 模式探测 → 调 deco to-compose（产物 androidx.compose.* .kt）
        登录态过期则 exit 4 让 Agent 介入
- [ ] Step 3 [Script，自动]：deco-compose.sh 成功后会自动调用 deco-compose-integrate.sh
        探测 CMP 工程 commonMain → 写入 <commonMain>/kotlin/com/example/generated/ 与 <commonMain>/assets/
        多 commonMain 候选(exit 5) 或 文件冲突(exit 6) 时给出重试命令，不阻断整体成功
```

> ⚠️ **与 to-kuikly 的区别**：`deco-compose.sh` 调的是 `deco to-compose`，产出标准 Compose Multiplatform（`androidx.compose.*`）；`deco-convert.sh` 调的是 `deco to-kuikly` / `deco to-kuikly-dsl`，产出 Kuikly Compose / Kuikly DSL。两者**不可混用**，不要因为都叫"Compose"就走错脚本。

### 执行命令

```bash
# 推荐：显式指定用户业务工程根目录（避免 Git / 模式推断落到 skill 目录）
bash scripts/deco-compose.sh "<figma-url>" --project-dir /path/to/kmp-project

# 已在工程根目录执行时，可省略 --project-dir（默认 cwd）
bash scripts/deco-compose.sh "<figma-url>"

# 本地 HTML 文件转标准 Compose（脚本自动加 --design-source html）
bash scripts/deco-compose.sh ./design.html --project-dir /path/to/kmp-project

# 强制远程 / 指定倍图 / 启用资源本地化 / 关闭资源下载
bash scripts/deco-compose.sh "<figma-url>" --project-dir /path/to/kmp-project --remote
bash scripts/deco-compose.sh "<figma-url>" --project-dir /path/to/kmp-project --image-scale 3
bash scripts/deco-compose.sh "<figma-url>" --project-dir /path/to/kmp-project --no-download-assets
```

### 可选参数

| 参数 | 说明 |
|------|------|
| `--project-dir <path>` | 用户 KMP 业务工程根目录（默认 cwd）。Deco 用 cwd 推断 Git，必须指向业务工程而非 skill 目录 |
| `--git <repo-url>` | Git 仓库地址（默认从 `--project-dir` 的 `remote.origin.url` 读取） |
| `--remote` | 强制远程模式（无需 Figma Desktop）；HTML 源默认就不依赖 Figma MCP |
| `-i, --interactive` | 开启交互模式（打开 sloth 配置页面，默认关闭） |
| `--no-interactive` | 关闭交互模式（默认行为） |
| `--download-assets` | 资源本地化（下载至输出目录 assets/ 并改写为本地资源语法，默认开启） |
| `--no-download-assets` | 保留远程资源 URL（不下载） |
| `--image-scale N` | 图片导出倍率（正整数，默认 1，高清屏推荐 2 或 3） |
| `-G, --component-groups <groups>` | 启用非 common 的项目组件分组（逗号分隔，如 `business,icon`；`common` 默认已启用） |
| `--prompt-dir <path>` | 自定义 Prompt Profile 目录路径（优先于全局配置） |

### 自动集成（CMP 工程结构）

`deco-compose.sh` 成功转码后会**自动调用** `deco-compose-integrate.sh`，按标准 Compose Multiplatform 工程结构落盘：

- **Kotlin 源码**：探测工程内的 `commonMain/kotlin` 目录（`src/commonMain` 优先于 `shared/commonMain`），写入 `<commonMain>/kotlin/com/example/generated/`（因为 to-compose 产物包名固定为 `com.example.generated`）
- **资源**：写入 `<commonMain>/assets/`（KuiklyUI quick-start 约定 `assets.srcDirs("src/commonMain/assets")`）

集成脚本的退出码处理：
- **exit 0**：集成成功，已落盘
- **exit 5**：工程存在多个 `commonMain` 候选目录 → 脚本输出候选 JSON，并提示用 `--pages-dir` 指定其一后重跑（转码产物已落盘，不丢失）
- **exit 6**：文件/资源冲突（目标已存在同名文件）→ 提示用 `--on-conflict skip|overwrite` 重跑

这两种情况**不会**让 `deco-compose.sh` 整体失败——转码本身已成功，集成可独立补跑。

### 错误处理

与 `deco-convert.sh` 一致：exit 4 = 登录态过期（Agent 介入高亮卡片 → `deco login` → 原参数重跑）；exit 1（含 `✗` / `FAILED` / `处理失败`）= 不可恢复失败，按 Rule 4 简练反馈并停止；exit 0 + `生成成功` = 成功，从输出提取 `Kotlin 代码: <路径>`。

> ⏱ **耗时与静默**：同 Rule 6。脚本调用给 ≥5min 超时；进程在 running 就继续等。

---

## 工作流程（增量更新）

**独立工作流**，对已有 Kuikly / Compose 代码对照最新设计稿做增量更新，产出 git patch。与转码链路正交：不需要先转码，直接拿现有 `.kt` + 新设计稿即可。

```text
增量更新进度：
- [ ] Step 1 [Agent]：环境检查（本会话内若未做过，先跑 deco-env-check.sh；做过则跳过）
- [ ] Step 1.5 [Agent，仅「只 @ 入口」时]：读入口 .kt，判断哪些直接关联的 UI 源码要一起更新
        顺入口 body(){}/@Composable 实际渲染的视图树往下追，收「改这次设计稿会跟着改」的 UI 文件；
        排除框架三方、工具类、跨页面公共组件/主题；追到只剩框架内置/纯数据就停；拿不准就列出候选问用户
        用户已显式 @ 多个文件时跳过本步，直接用用户给的文件集合
- [ ] Step 2 [Script]：增量更新 — bash scripts/deco-update.sh <file...> --source-ref "<ref>" [--design-source figma|html] [--project-dir <工程根>]
        <file...> = 入口 + Step 1.5 判断出的关联文件（或用户显式 @ 的文件）
        校验源文件/设计稿 → 调 deco update → 产出增量 patch
        登录态过期/未登录则 exit 4 让 Agent 介入
- [ ] Step 3 [Script，自动]：deco-update.sh 成功后会自动 git apply（先 git apply --check 预检）
        预检通过则直接落盘；预检失败（冲突/上下文不匹配）则回退为提示手动 apply，不阻断
```

### 执行命令

```bash
# 更新单个文件，对照 Figma 设计稿
bash scripts/deco-update.sh ./Foo.kt --source-ref "https://www.figma.com/design/xxx?node-id=0-2" --project-dir /path/to/project

# 更新多个文件
bash scripts/deco-update.sh ./Foo.kt ./Bar.kt --source-ref "<figma-url>" --project-dir /path/to/project

# 【只 @ 入口 → Agent 判断关联 UI 源码 → 一并更新】（推荐用于多文件拆分的页面）
#   Step 1.5：Agent 读 HomePage.kt，顺视图树判断出关联的 UI 文件（子 Composable/子视图、页面状态类），
#             排除框架三方/工具类/跨页面公共组件，追到只剩框架内置/纯数据就停；拿不准就问用户。
#   Step 2：把入口 + 判断出的关联文件一起列出来传给 deco update（示例判断出 3 个关联文件）
bash scripts/deco-update.sh ./HomePage.kt ./HomeHeader.kt ./HomeState.kt --source-ref "<figma-url>" --project-dir /path/to/project

# 对照本地 HTML 设计稿
bash scripts/deco-update.sh ./Foo.kt --source-ref ./design.html --design-source html --project-dir /path/to/project

# 指定 patch 输出路径 / 调整 churn 阈值
bash scripts/deco-update.sh ./Foo.kt --source-ref "<figma-url>" -o ./update.patch
bash scripts/deco-update.sh ./Foo.kt --source-ref "<figma-url>" --churn-threshold 80
```

### 可选参数

| 参数 | 说明 |
|------|------|
| `<file...>` | 待更新的源文件（一个或多个 `.kt` 路径，**必填**） |
| `--source-ref <ref>` | 新设计稿引用（Figma URL 或本地 HTML 路径，**必填**） |
| `--project-dir <path>` | 用户业务工程根目录（默认 cwd），用于推断 Git 与定位文件 |
| `--design-source figma\|html` | 设计源类型（默认 figma） |
| `-o, --output <path>` | 增量 patch 输出路径（默认写入 deco 输出目录） |
| `--churn-threshold N` | 改动比例阈值 0-100（默认 95；超过视为大改而非增量） |
| `--remote` | 强制远程模式 |
| `--download-assets` / `--no-download-assets` | 资源本地化（默认开启）/ 保留远程 URL |
| `--image-scale N` | 图片导出倍率（正整数，默认 1） |
| `-G, --component-groups <groups>` | 启用非 common 的项目组件分组 |
| `--prompt-dir <path>` | 自定义 Prompt Profile 目录路径 |

### 退出码与 Agent 行为

| 退出码 | 含义 | Agent 行为 |
|:---:|------|------|
| **0** | 增量更新成功，产出 patch 并**自动 git apply** | 脚本先 `git apply --check` 预检；通过则直接落盘并提示已应用；预检失败（冲突/上下文不匹配）则回退为提示手动 `git apply --3way`，不阻断整体成功 |
| **1** | 不可恢复失败（缺参数 / 源文件不存在 / 更新失败 / 未登录等） | 按顶部「失败即停止」规则反馈错误，**禁止重试** |
| **4** | 登录态过期 / 未登录（**需 Agent 介入**） | (a) 先发顶部 Rule 7.1「高亮卡片」（动作：完成 Flowly 授权登录；完成后：用原参数重跑 `deco-update.sh`）(b) 独立执行 `deco login` (c) 登录成功后用原参数重跑脚本 |

> ⚠️ **patch 自动应用但可回退**：`deco update` 产出 `.patch` 后，脚本会尝试自动 `git apply`。若预检不通过（例如本地代码与 patch 上下文不一致），脚本**不会**强行应用，而是给出手动命令让用户决定。`git apply` 是可逆操作（未提交前 `git checkout` 即可撤销），故自动 apply 风险可控。

---

## 工作流程（D2C 转码）

复制此清单跟踪进度：

```
转码进度：
- [ ] Step 1 [Agent]：环境检查（本会话内若未做过，先跑 deco-env-check.sh；做过则跳过）
- [ ] Step 2 [Script]：转码 — bash scripts/deco-convert.sh "<source-ref>" --project-dir <工程根>
        <source-ref> 支持 Figma URL 或本地 .html / .htm 文件路径
        脚本内部串联：源识别 → URL/HTML 校验 → DSL 推断 → 模式探测 → 调 deco（登录态过期则 exit 4 让 Agent 介入）
- [ ] Step 3 [Script]：集成产物 — bash scripts/deco-integrate.sh <产物目录> --project-dir <工程根>（产物目录从 Step 2 转码输出中提取）
        脚本自动推断页面路径 + 预检资源冲突；无冲突一次写完；exit 6 冲突 → 问用户 → --on-conflict 重跑；exit 5 路径歧义 → 问用户 → 带 --pages-dir/--assets-dir 重跑
- [ ] Step 4 [Agent]：询问是否扫描入团队组件库（仅当有 git remote + 有组件生成才发问）
```

> ⚠️ **环境检查不要重复执行**：`deco-env-check.sh` / `deco-convert.sh` / `deco-scan-components.sh` 三者**都不**在脚本内部互相调用 env-check。Agent 在本会话首次需要时调一次 `deco-env-check.sh` 即可，后续转码 / 扫描都直接跳过 Step 1。重复调会多输出一行 `✔ 环境就绪：...`，纯属浪费。

## 工作流程（组件扫描）

```
扫描进度（前两步由 Agent 在对话中完成，第三步才调脚本）：
- [ ] Step 1 [Agent]：环境检查（本会话内若未做过，先跑 deco-env-check.sh；做过则直接跳过）
- [ ] Step 2 [Agent]：收集输入（扫描路径、Git 地址、DSL 类型、是否含 internal）
- [ ] Step 3 [Script]：调用 deco-scan-components.sh，按退出码 0/1/2/3 处理结果
```

> 💡 **职责划分**：
> - **Step 1（环境检查）** —— Agent 视情况调用 `bash scripts/deco-env-check.sh`，本会话内已通过则跳过
> - **Step 2（输入收集）** —— **Agent 在对话中完成**：与用户确认扫描路径、Git 仓库、DSL 类型、是否含 internal。脚本无法做这一步，只能在 Step 3 中对收到的参数做校验
> - **Step 3（执行）** —— Agent 把收齐的参数透传给 `bash scripts/deco-scan-components.sh`，按退出码语义处理后续

---

## Step 1: 环境检查（由 Agent 编排，不在脚本内）

`deco-convert.sh`、`deco-scan-components.sh`、`deco-check.sh` 都**不**做环境检查，由 Agent 自行决定是否调用 `deco-env-check.sh`。

**Agent 判断规则**：

- 本会话内**未**成功跑过 `deco-env-check.sh` → 调用 convert 之前先执行 `bash scripts/deco-env-check.sh`，环境就绪后再进入 Step 2
- 本会话内**已**成功跑过 → **直接跳过本步骤**，进入 Step 2

不要把环境检查作为「保险措施」无脑加在每次转码前。

```bash
bash scripts/deco-env-check.sh
```

脚本检查 Node.js ≥18 / npm / Deco CLI（**Deco >= 0.6.51**），并按每日策略（`~/.deco/.last_update`）自动安装/更新；版本过低会强制尝试升级，仍不满足则失败退出。手动命令与安装细节见 [ENV-SETUP.md](ENV-SETUP.md)。

---

## Step 2: 转码

调用 `deco-convert.sh`，脚本内部依次：**源识别**（Figma URL / HTML 本地文件） → URL/HTML 校验 → 解析 Git 仓库 → DSL 类型推断 → 转码模式探测 → 调 deco。登录态过期由脚本通过 exit 4 信号让 Agent 介入处理（详见下方「错误处理」），脚本本身不再内嵌 `deco login`。

> ⚠️ **Git 仓库地址**：Deco CLI 默认用**命令执行时的 cwd** 推断 `remote.origin.url`。Agent 若在 `.codebuddy/skills/` 目录下跑脚本，会误用 **skill 仓库**（如 `kuikly-skills.git`）而非用户业务工程。**必须**传 `--project-dir` 指向用户 Kuikly 工程根目录（或在工程根目录下执行脚本）；脚本会从该目录读取 Git 并显式传给 `deco --git`。

### 设计源类型

第一个位置参数 `<source-ref>` 支持两种形态，脚本根据后缀**自动识别**，**Agent 不需要手工指定 `--design-source`**：

| 形态 | 识别规则 | 脚本行为 |
|------|---------|---------|
| **Figma URL** | 包含 `figma.com` 且含 `node-id=` | 走 Figma MCP（本地 3845 探测可达 → 本地；不可达 → 远程） |
| **HTML 本地文件** | 后缀 `.html` 或 `.htm`（大小写不敏感）且本地存在 | 自动追加 `--design-source html`，跳过本地 3845 探测；支持 `~`/相对/绝对路径，解析为绝对路径后传给 deco；目录、不存在、非 `.html`/`.htm` 一律 exit 1 |

> ⚠️ **HTML 源限制**：Deco 的 HTML 数据源**只接受 `.html` / `.htm` 文件路径**，不接受目录、不会自动在目录下找 `index.html`。资源解析以该 HTML 文件所在目录为基准。

### 执行命令

```bash
# 推荐：显式指定用户业务工程根目录（避免 Git / DSL 推断落到 skill 目录）
bash scripts/deco-convert.sh "<figma-url>" --project-dir /path/to/kuikly-project

# 已在用户工程根目录执行时，可省略 --project-dir（默认 cwd）
bash scripts/deco-convert.sh "<figma-url>"

# Git remote 缺失或需覆盖时
bash scripts/deco-convert.sh "<figma-url>" --project-dir /path/to/kuikly-project --git git@git.woa.com:team/your-app.git

# 关闭交互 / 远程 / 指定 DSL / 多倍图 / 非 common 组件分组
bash scripts/deco-convert.sh "<figma-url>" --project-dir /path/to/kuikly-project --no-interactive
bash scripts/deco-convert.sh "<figma-url>" --project-dir /path/to/kuikly-project --remote --image-scale 3
bash scripts/deco-convert.sh "<figma-url>" --project-dir /path/to/kuikly-project --component-groups business,icon

# 本地 HTML 文件转码（脚本自动加 --design-source html，无需手写）
bash scripts/deco-convert.sh ./design.html --project-dir /path/to/kuikly-project
bash scripts/deco-convert.sh ~/work/page.htm --project-dir /path/to/kuikly-project --dsl compose
```

> ⏱ **耗时与静默**：详见顶部 Rule 6。要点：脚本调用给 ≥5min 超时；进程在 running 就继续等，别因长时间无新输出而中断或重试。

### 内部决策规则（仅供 Agent 理解，无需干预）

| 阶段 | 规则 |
|------|------|
| 源识别 | 1) 后缀是 `.html` / `.htm`（大小写不敏感）→ HTML 本地源；规范化为绝对路径，校验「存在 + 是文件 + 不是目录」 2) 否则 → Figma URL |
| URL/HTML 校验 | **Figma 源**：必须含 `figma.com` 与 `node-id`，缺失 → `✗` 退出并提示「Copy link to selection」<br>**HTML 源**：跳过 Figma URL 校验；不存在 / 是目录 / 非普通文件 → `✗` 退出 |
| DSL 类型 | 1) 用户传 `--dsl` 优先 2) **`--project-dir` 下**含 `@Composable` 的 `.kt` → `compose` 3) 否则 `traditional` |
| Git 仓库 | 1) 用户传 `--git` 优先 2) 从 **`--project-dir`** 的 `remote.origin.url` 读取并传给 `deco --git` 3) 落在 `.codebuddy/skills` 内会 warn |
| 转码模式 | 1) 用户传 `--remote` 优先 2) **HTML 源**：跳过 3845 探测，不传 `--remote`（HTML 不依赖 Figma MCP） 3) `nc -z -w3 127.0.0.1 3845` 可达 → 本地 4) 否则远程 |
| `--design-source` | HTML 源 → 自动追加 `--design-source html`；Figma 源 → 不追加（deco 默认 figma） |

### 可选参数

| 参数 | 说明 |
|------|------|
| `--project-dir <path>` | **用户 Kuikly 业务工程根目录**（默认 cwd）。Deco 用 cwd 推断 Git，必须指向业务工程而非 skill 目录 |
| `--git <repo-url>` | Git 仓库地址（默认从 `--project-dir` 的 `remote.origin.url` 读取） |
| `--remote` | 强制远程模式（无需 Figma Desktop）；HTML 源默认就不依赖 Figma MCP，传不传都可以 |
| `--no-interactive` | 关闭脚本默认开启的交互模式（纯命令行直出，不开浏览器） |
| `--image-scale N` | 图片导出倍率（正整数，默认 1，高清屏推荐 2 或 3） |
| `--component-groups <groups>` | 启用非 common 的项目组件分组（逗号分隔，如 `business,icon`；`common` 默认已启用，无需传入） |
| `--dsl compose|traditional` | 显式指定 DSL，跳过自动推断 |

> 注：`--design-source` 由脚本根据第一个位置参数的后缀自动设置，**Agent 不要手工传**。

### 交互模式

默认开启。脚本检测到无图形环境（CI/SSH/容器）会**自动回退**为非交互并打印 `⚠ ...已自动跳过交互模式`，不会卡死。

**关键行为**：交互模式会自动打开浏览器配置页面，**命令行会一直阻塞，直到用户在浏览器里完成代码分组配置并关闭页面**，转码才继续往下走。

**Agent 规则：**

1. **启动转码前必须发「高亮卡片」预告**：按 Rule 7.1 模板，文案见 Rule 7.2 表第 1 行（动作=完成代码分组配置并关闭页面；完成后=命令行自动继续）。**禁止**单行文本敷衍。
2. **保持默认开启**：不要主动加 `--no-interactive`，让脚本默认 + 自动回退兜底
3. **用户明确要求关闭才加**：用户说「不要弹浏览器」「直接出代码」「批量跑、跳过交互」时才加 `--no-interactive`
4. **不要为"保险"预防性关闭**：脚本已自动探测环境，不需要 Agent 替它判断
5. **裸 `deco` 命令保持原语义**：直接调用 `deco to-kuikly` 时交互默认关闭，需用户明确要求才加 `--interactive`
6. **浏览器阻塞期间不算"卡死"**：等待用户操作浏览器是预期行为，与"长时间静默"叠加可能让 stdout 安静很久，**禁止**因此中断脚本

### 错误处理

`deco` 失败时也可能返回 exit 0，脚本通过**输出内容匹配**判断成败，并按以下退出码语义反馈：

| 输出特征 / 退出码 | 处理 |
|---------|------|
| 退出码 **4** | 登录态过期（脚本检测到 `未登录` / `Flowly Token` / `请先登录` 等关键字）→ 脚本不内嵌 `deco login`，由 Agent 介入：先告知用户去浏览器登录，再独立执行 `deco login`，成功后用原参数重跑脚本（详见下方流程） |
| 退出码 **1** + 输出含 `✗` / `FAILED` / `处理失败` | 不可恢复失败 → Rule 4 简练反馈并停止（**禁止**反问是否取消） |
| 退出码 **1** + 其它 | 异常退出 → Rule 4 简练反馈并停止 |
| 退出码 **0** + 输出含 `✔ 转码完成` / `✓ 生成成功` | 成功 → 从输出中提取 `✔ Kotlin: <路径>` 的父目录作为产物目录，进入 Step 3。若产物含图片资源，还会输出 `✔ Assets: <路径> (N 个文件)` |

**为什么 exit 4 不在脚本内自动跑 `deco login`**：脚本内部跑 `deco login` 时会立刻打开浏览器并阻塞命令行，但 Agent 此时正在 Shell tool 同步等待脚本结束，**无法向用户发出"请去浏览器操作"的提醒**——用户只能看到脚本自己的小字 stdout 提示，很容易错过。把登录挤出脚本、让 Agent 重新介入，才能在浏览器打开**之前**给用户一条明确消息。

**Agent 收到 exit 4 后的强制流程**（按顺序执行，不要省略 (a)）：

(a) **先发「高亮卡片」**：按 Rule 7.1 模板，文案见 Rule 7.2 表第 2 行（动作=完成 Flowly 授权登录；完成后=用原参数重跑 `deco-convert.sh`）。**禁止**单行文本敷衍。

(b) **独立执行 `deco login`**：这是新的 tool call，仍会阻塞等用户在浏览器完成授权，但用户已经在 (a) 看到高亮卡片
- 登录成功（exit 0）→ 进入 (c)
- 登录失败 → 反馈错误并停止，提示用户手动 `deco login` 后再来

(c) **用原参数重跑 `deco-convert.sh`**：用首次调用时的完整命令重新执行，保留全部参数（`--remote` / `--no-interactive` / `--image-scale` / `--dsl` 等）。

---

## Step 3: 集成产物到项目

**硬规则**：
1. 集成命令**必须**传 `<产物目录>`（第一个位置参数），从 Step 2 转码脚本的 stdout 中提取（输出行格式：`✔ Kotlin: <产物文件路径>`，取其父目录）
2. 集成命令**必须**带 `--project-dir`，指向与 Step 2 转码相同的用户 Kuikly 业务工程根目录（禁止在 `.codebuddy/skills/` 下用默认 cwd）

集成脚本**自身就会自动推断** pages-dir / assets-dir：路径明确时一次写完，歧义时 exit 5 退出（未写入任何文件，输出含路径候选 JSON）。所以**直接实跑**，再按下方「退出码与 Agent 行为」表推进：

```bash
bash scripts/deco-integrate.sh /path/to/.deco/output/<uuid>_<node-id>_<date> --project-dir /path/to/kuikly-project
```

> ⚠️ **直接实跑，不要"先探测再实跑"**：脚本会自动推断页面路径，路径明确就一次写完，歧义才 exit 5（携带路径候选 JSON 供你问用户）。任何"先跑一次只探测路径、再跑一次实跑"的两段式都是多余的——标准工程白白多执行一次，歧义工程的 exit 5 本身就等于探测结果。

实跑示例（按需带路径参数）：

```bash
# 用户/exit 5 指定页面目录
bash scripts/deco-integrate.sh /path/to/artifact-dir --project-dir /path/to/kuikly-project \
  --pages-dir shared/src/commonMain/kotlin/<pkg>/pages

# 非标准资源布局
bash scripts/deco-integrate.sh /path/to/artifact-dir --project-dir /path/to/kuikly-project \
  --pages-dir ... --assets-dir shared/src/commonMain/composeResources/files

# exit 6 冲突后，用户选择 skip/overwrite，用相同参数重跑
bash scripts/deco-integrate.sh /path/to/artifact-dir --project-dir /path/to/kuikly-project --on-conflict skip
```

### 页面目录推断策略（脚本内置，Agent 可读）

按优先级：

1. **用户传入 `--pages-dir`** → 直接使用（绝对路径须落在 `--project-dir` 下；否则 exit 1）
2. **未传时自动推断**：
   - **2a** 找项目下 `*/commonMain/kotlin`。0 个 / 多个 → exit 5（`reason=no_commonmain|multiple_commonmain`）
   - **2b** 在唯一的 commonMain 内：
     - 与产物 DSL 同类型的现有 `.kt` → 取它们的**公共父目录**（**排除** `components/`、`utils/`、`widget/` 等非页面目录）
     - 命名为 `pages` / `page` / `demo` / `screen` / `screens` 的子目录
   - **2c** 候选去重：
     - 唯一候选 → 用它
     - 多候选但互为嵌套 → 取最浅目录
     - 多候选且不嵌套 → exit 5（`reason=pages_ambiguous`）
3. **`assets-dir`**：默认从 pages-dir 推导 `<commonMain>/assets`（pages-dir 须在 `commonMain/kotlin/` 下：取 `kotlin/` 之前路径 + `/assets`）→ 失败 exit 5（`reason=assets_dir_unknown`），Agent 问 `--assets-dir`

### 退出码与 Agent 行为

| 退出码 | 含义 | Agent 行为 |
|:---:|------|------|
| **0** | 集成成功 | 实跑成功 → 进入 Step 4 |
| **1** | 不可恢复失败（产物缺失 / 路径越界等） | 按顶部「失败即停止」规则反馈错误，**禁止重试** |
| **5** | **需用户输入路径**（尚未写入） | **仅解析** `---DECO_INTEGRATE_PROBE_JSON---` + 首次 stdout。**禁止**无参数重跑、`bash -x`、`2>&1` 解析、`ls`/`find` 核对目录。用户答复后**一次**带 `--pages-dir`/`--assets-dir` 实跑 |
| **6** | **需用户决策图片资源同名冲突**（预检发现，**尚未写入**） | **仅解析** `---DECO_INTEGRATE_CONFLICT_JSON---` + 首次 stdout 摘要。**禁止**无 `--on-conflict` 重跑、任何形式的「再跑一次看详情」、`bash -x`、`ls` 产物目录。用户选 skip/overwrite 后**一次**实跑 |

**询问 UI**（按顶部 Agent 行为约束第 9 条）：
- **exit 5 + `pages_candidates` 非空**：单选卡片列出候选 +「自定义路径」
- **exit 5 + `reason=no_commonmain|no_pages_candidates`**：纯文本问询，示例 `shared/src/commonMain/kotlin/<pkg>/pages`
- **exit 5 + `reason=assets_dir_unknown`**：问资源目录，示例 `shared/src/commonMain/assets` 或项目的 `composeResources` 路径
- **exit 6 资源冲突**：单选卡片 skip（推荐）/ overwrite

### 脚本顺手做的事（agent 无需手动处理）

- **文件重命名**：去掉 Deco 输出的日期后缀（如 `Foo_2026-03-16_12-43.kt` → `Foo.kt`）
- **package 修复**：将 Deco 默认 package 替换为目标目录对应的 package（从路径中自动推断）
- **资源预检 + 拷贝**：写入前预检产物 `assets/` 与目标 **`commonMain/assets/`** 是否同名（按相对路径，如 `common/foo.png`）；无冲突则与 `.kt` 一并直接落盘；有冲突则 **exit 6（此时未写入）** → Agent 问一次 → `--on-conflict skip\|overwrite` 实跑完成。产物 `assets/common/*` 会保留 `common/` 子目录拷贝到 `commonMain/assets/common/*`（与 `ImageUri.commonAssets()` 路径一致）。`assets-manifest.json` 仅作提示，本地副本仍会拷贝
- **路由检测**：检测 `@Page` 注解并提示路由信息

> 手动集成步骤（产物目录、`.kt` 放置、路由注册）见 [INTEGRATION.md](INTEGRATION.md)。常规流程应走脚本，不要 Agent 手动逐步搬运。

---

## Step 4: 询问是否扫描入团队组件库（可选）

转码 + 集成完成后，**先做两项前置检查，全部满足才发起询问**；任意一项不满足，Step 4 静默跳过，不要打扰用户。

**前置条件**（Agent 在发问前自行核对）：

| 检查项 | 怎么判断 | 不满足时的行为 |
|------|------|------|
| 集成目录所在 Git 仓库有 `remote.origin.url` | `git -C <integrated-project-dir> config --get remote.origin.url` 拿到非空值 | 静默跳过 Step 4（扫描必然 exit 3，提问没有意义） |
| 本次转码 + 集成实际产生了组件代码 | Step 2 转码输出含 `✔ 转码完成` 或 `✓ 生成成功`，且 Step 3 集成日志包含被复制 / 重命名的 `.kt` 文件 | 静默跳过 Step 4（没有可扫描的产物） |

两项都满足时，Agent **必须主动**询问；都满足却不问 = 违规。这一步**不要**默认跳过，也**不要**默认执行。

**询问 UI**（按顶部 Agent 行为约束第 9 条）：

- 客户端支持结构化询问 UI 时：用是/否单选卡片
  - `是，扫描并入团队组件库（推荐）` ← 倾向选项
  - `否，结束流程`
- 不支持时：纯文本问询，文本中保留"（推荐）"标注

**问题文案要求**（让用户清楚隐私边界后再选择）：

- **纯文本**，不要 `**加粗**` / `-` 列表 / `>` 引用等 markdown，结构化 UI 不会渲染，全堆成一坨更难读
- 控制在 2~3 句话以内，把"做什么"+"不做什么"+"保存哪些字段"压成短摘要
- 不要省略隐私边界（不编译 / 不运行 / 不上传源码），但允许合并表述
- 推荐文案（可直接用，也可同义改写，但口径不能变）：

```
是否将刚生成的组件扫描入团队组件库？
扫描在本机由 Deco CLI 静态分析 .kt 文件完成，不编译、不运行、不上传源码。
仅保存：组件名/描述、方法签名/import、参数信息、组件变体、Git 仓库地址。
```

**用户选 "是"**：

- 进入「组件扫描」工作流
- Scan Step 1（环境检查）：本会话已通过转码完成，**直接跳过**
- Scan Step 2（输入收集）：扫描路径优先用 Step 3 集成的目标项目目录（即 `--pages-dir` 或自动探测出的页面目录所在的项目根），DSL 类型沿用本次转码的 DSL，**是否含 internal 默认为"是"（自动带 `--include-internal`）**，其它字段按 Scan Step 2 表询问 / 推断
- Scan Step 3：调 `deco-scan-components.sh`（**必须带 `--include-internal`**），按退出码 0/1/2/3 处理

**用户选 "否" / 不回复**：D2C 流程在 Step 4 结束，不再发起扫描。

---

# 组件扫描

将本地 Kotlin 工程中的 Kuikly 组件入口扫描出来，发布为 Flowly 草稿，并打开 Portal 让用户配置组件集（用于后续转码命中已有组件）。

> ⚠️ 失败即停止、禁止绕过脚本、禁止 Agent 自动重试；仍按顶部 `Agent 行为约束` 执行。

## Scan Step 1: 环境检查（由 Agent 编排，不在脚本内）

`deco-scan-components.sh` 本身**不**做环境检查，原因是 Agent 在同一会话内通常已经通过转码或先前扫描跑过 `deco-env-check.sh`，重复执行会产生无意义的 npm 网络调用。

**Agent 判断规则**：

- 本会话内**未**成功跑过 `deco-env-check.sh` / `deco-convert.sh` → 调用前先执行 `bash scripts/deco-env-check.sh`，环境检查通过后再进入 Step 2
- 本会话内**已**成功跑过其中任意一个 → **直接跳过本步骤**，进入 Step 2

不要把环境检查作为「保险措施」无脑加在每次扫描前。

## Scan Step 2: 收集输入（仅 Agent 在对话中完成）

> 这一步**完全由 Agent 在对话中完成**，脚本无法替代——脚本只能在 Step 3 中对收到的参数做合法性校验和默认值兜底，无法发起询问。

Agent 需在调用脚本**之前**向用户确认下列输入。缺失时主动询问，不要静默使用默认值，更不要编造。**询问方式遵循顶部 Agent 行为约束第 9 条**：闭集合选择题用结构化 UI；自由文本输入直接纯文本问询，不要给"跳过"做唯一选项。

| 输入 | 是否必填 | 收集方式 | 询问 UI 建议 |
|------|:---:|------|------|
| 扫描路径 | 必填 | 用户给出，支持 `~/`，必须是本地 Kotlin 源码目录或 `.kt` 文件 | 自由文本输入 |
| Git 仓库地址 | 视情况 | 从 **`scan-path` 向上**找 Git 根读 `remote.origin.url`（**不用 cwd**，比转码安全）；能拿到则脚本显式传 `--git`。`scan-path` 必须指向用户业务工程的 Kotlin 源码，**不要**指向 `.codebuddy/skills/` 内目录 | 自由文本输入 |
| DSL 类型 | 视情况 | 优先用 D2C 转码 Step 2 的工程推断规则（`@Composable` → compose，`attr {` → traditional）；推断不出来再问用户。脚本内同样会兜底推断，但 Agent 应优先在对话中确定 | 推断不出来时用单选卡片：`compose` / `traditional` |
| 是否含 internal | 默认否 | **不要**预防性加 `--include-internal`。仅在用户明确要求或 Step 3 返回退出码 2 后用户同意时才加 | 单选卡片：`是` / `否（推荐）` |
| Namespace | 可选 | 用户/业务方明确给出再传 `--namespace <ns>`，否则不传 | 自由文本输入 |

## Scan Step 3: 调用 deco 扫描命令

**脚本方式**（推荐）：

```bash
# 最小调用：自动推断 DSL，仅扫 public 组件
bash scripts/deco-scan-components.sh <scan-path>

# 指定 Git 与 DSL，业务 namespace
bash scripts/deco-scan-components.sh <scan-path> \
    --git git@example.com:foo/bar.git \
    --dsl compose \
    --namespace my-biz

# 包含 internal（仅在用户确认或第二次重跑时使用）
bash scripts/deco-scan-components.sh <scan-path> --include-internal
```

### 退出码与输出语义（Agent 必读）

| 退出码 | 含义 | Agent 行为 |
|:---:|------|------|
| **0** | 扫描成功，草稿已保存，输出末尾包含 Portal URL | 用顶部 Rule 7.1「高亮卡片」转告 Portal URL（动作：在浏览器里完成扫描结果配置；完成后：扫描入库后流程结束） |
| **1** | 不可恢复失败（路径不存在 / 保存草稿失败 / 其它扫描失败） | 按顶部「失败即停止」规则反馈错误，**禁止重试** |
| **2** | 扫描到 0 个组件且未启用 `--include-internal`（**需用户决策**） | 询问用户是否使用 `--include-internal` 重新扫描，**得到肯定答复后**再执行脚本输出中给出的重试命令 |
| **3** | 未提供 `--git` 且本地 Git 仓库未配置 `remote.origin.url`（**需用户输入**） | 向用户询问 Git 仓库地址，**得到具体 URL 后**用 `--git <url>` 执行脚本输出中给出的重试命令 |
| **4** | 登录态过期（**需 Agent 介入**） | (a) 先发顶部 Rule 7.1「高亮卡片」（动作：完成 Flowly 授权登录；完成后：用原参数重跑 `deco-scan-components.sh`）(b) 独立执行 `deco login` (c) 登录成功后用原参数重跑脚本 |

### 子场景处理

#### 3.1 成功路径

脚本输出末尾会有：

```
✔ 扫描完成，草稿已保存

ℹ deco 已尝试自动打开 Portal 页面，请在浏览器中完成扫描结果配置。
  Portal URL（若浏览器未自动打开，可手动访问）：
    https://...portal.../?env=prod#/resolve?key=...&git_url=...
```

或在无图形环境下：

```
⚠ 当前环境无法打开本地浏览器，请手动访问以下 URL 完成配置：
    https://...
```

> 因为 `deco` 的 `openBrowser()` 失败时仅写日志、不输出 stdout，脚本无法判定浏览器是否弹起，因此**两种场景统一**给出 Portal URL。Agent **必须**用 Rule 7.1 模板转告 URL（文案见 Rule 7.2 表第 3 行；【命令行状态】写"脚本已退出，无需等待"），不要只贴一行链接。

#### 3.2 登录态过期 → 退出码 4（需 Agent 介入）

deco 输出包含 `未登录` / `Flowly Token` / `token.*过期` / `请先登录` / `请确认.*登录` 时，脚本**不**内嵌 `deco login`，而是直接 exit 4。原因：脚本运行期间 Agent 在 Shell tool 同步等待，**无法**向用户发出"去浏览器操作"的提醒；把登录挤到 Agent 层才能保证用户在浏览器打开**之前**收到明确预告。

Agent 看到退出码 4 后：

1. **先发顶部 Rule 7.1「高亮卡片」**（动作：完成 Flowly 授权登录；完成后：用原参数重跑 `deco-scan-components.sh`）。**禁止**用单行文本敷衍。
2. 独立执行 `deco login`（这次 tool call 会阻塞等用户在浏览器完成授权，但用户已经在第 1 步看到高亮卡片）
3. 登录成功 → 用首次调用的原始参数重跑 `deco-scan-components.sh`
4. 登录失败 → 反馈错误并停止，提示用户手动 `deco login` 后再来

#### 3.3 0 组件 + 未含 internal → 退出码 2

脚本检测到 deco 输出中 `组件数量: 0` 且当前未传 `--include-internal` 时：

1. 输出 `⚠ 扫描到 0 个组件，且未启用 --include-internal`
2. 给出建议命令（带上原参数 + `--include-internal`）
3. 以**退出码 2** 退出（这是脚本里**唯一非失败**的非零退出码）

Agent 必须：

1. 看到退出码 2 + 上述输出 → 向用户询问「项目内组件入口可能为 internal 可见性，是否使用 `--include-internal` 重新扫描？」
   - 客户端支持结构化询问 UI 时（参考顶部 Agent 行为约束第 9 条）：**用是/否单选卡片**，选项文案：
     - `是，重新扫描并包含 internal（推荐）` ← 倾向选项，因为已扫描到 0 个 public 但有 N 个 internal 被过滤，不重扫即无产物可发
     - `否，结束扫描`
   - 不支持时：纯文本提问后等待用户答复，文本中保留"（推荐）"标注
2. 用户确认后，执行脚本提示的重试命令
3. 用户拒绝 → 任务结束，告知用户当前无可发布组件

> ⚠️ Agent **不得**绕过用户确认直接重跑 `--include-internal`——这是用户决策点，不是自动恢复点。

#### 3.4 缺失 Git 仓库地址 → 退出码 3

脚本在调 deco 之前预检 `git -C <scan-path> config --get remote.origin.url`：

- **与转码不同**：Deco `scan` / `scan-dsl` 用 **`scan-path`** 推断 Git，**不用 cwd**，因此不会出现「在 skill 目录执行命令就误用 kuikly-skills.git」的问题——**前提是 scan-path 指向用户业务工程的 Kotlin 源码**
- 用户已显式传 `--git <url>` → 跳过预检
- 预检成功 → 脚本**显式**把 remote 传给 `deco --git`
- `scan-path` 落在 `.codebuddy/skills/` 内 → 脚本 warn（可能扫到 skill 仓库的 Git）
- 本机未装 git → 跳过预检（让 deco 自己推断或失败）
- 仓库无 `remote.origin.url` → 输出提示并以**退出码 3** 退出，**不**进行远程调用

兜底：即使预检漏判（例如 deco 自身解析逻辑更复杂），运行后捕到 `Git 仓库地址解析失败` 也走退出码 3，语义一致。

Agent 必须：

1. 看到退出码 3 + `未提供 --git 且本地 Git 仓库未配置 remote.origin.url` 提示
2. **用纯文本**向用户询问「项目的 Git 仓库地址（`git@xxx:org/repo.git` 或 `https://...`）」
   - 这是自由文本输入，**禁止**套用单选/卡片等结构化 UI
   - 不要给"暂不提供，跳过"这种唯一选项 —— 用户不回复 URL 本来就等同于跳过
   - 可在问题中给出 2~3 个示例格式（SSH / HTTPS）方便用户复制改写
3. 用户给出 URL → 按脚本输出的重试命令重新执行
4. 用户显式表示提供不了 / 暂不处理 → 任务结束，告知用户需要先在仓库里 `git remote add origin ...`

> ⚠️ Agent **不得**编造 / 推测 Git 仓库地址，必须由用户明确给出。

---

# 设计稿规则检查

对 Figma 或本地 HTML 设计稿按 Markdown 规则文件进行 AI 审查，输出结构化 JSON 报告。用于转码前交付验收。

> ⚠️ 失败即停止、禁止绕过脚本、禁止 Agent 自动重试。

## Check Step 1: 环境检查（由 Agent 编排）

本会话内未成功跑过 `deco-env-check.sh` 则先执行，已通过则跳过。

## Check Step 2: 收集输入（Agent 在对话中完成）

| 输入 | 是否必填 | 收集方式 |
|------|:---:|------|
| 设计稿 `<source-ref>` | 必填 | 从 Prompt 的 `@` 引用或用户消息提取 Figma URL / `.html` 路径 |
| 规则文件 `--rule` | 必填 | 从 Prompt 的 `规则@...` 或用户消息提取；建议纳入版本管理（如 `docs/design-check-rules.md`） |
| `--project-dir` | 视情况 | 解析相对路径时 MUST 指向用户工程根目录（默认 cwd） |
| `--output` | 可选 | 用户指定 JSON 输出路径时传入；否则 deco 默认写入 `~/.deco/output/` |

**规则文件格式**（Markdown）：

- 每条规则用 `###` 三级标题
- 正文含 **问题级别**（`error` 必须修复 / `warn` 建议优化）与 **描述**（检查逻辑）
- 可选代码块列举允许值等约束

## Check Step 3: 调用 `deco-check.sh`

```bash
# Figma 设计稿
bash scripts/deco-check.sh "https://www.figma.com/design/xxx?node-id=0-2" \
  --rule docs/design-check-rules.md \
  --project-dir /path/to/kuikly-project

# 本地 HTML 设计稿
bash scripts/deco-check.sh ./design/landing/index.html \
  --rule docs/design-check-rules.md \
  --project-dir /path/to/kuikly-project

# 指定 JSON 输出路径
bash scripts/deco-check.sh "<source-ref>" --rule ./rules.md --output ./result.json \
  --project-dir /path/to/kuikly-project
```

> ⏱ 审查通常 30s~2min，调用时给 ≥5min 超时；长时间无新输出正常，进程 running 就继续等。

### 退出码与 Agent 行为

| 退出码 | 含义 | Agent 行为 |
|:---:|------|------|
| **0** | 检查执行成功（JSON 已写入） | 读取 `✔ JSON: <path>` 路径；向用户解读报告（见下） |
| **1** | 不可恢复失败 | Rule 4 简练反馈并停止 |
| **4** | 登录态过期 | 高亮卡片 → `deco login` → 原参数重跑 `deco-check.sh` |

### 解读 JSON 报告

脚本 stdout 含 `✔ 检查完成` 与 `结果: 通过/未通过`。Agent MUST 读取 JSON 并向用户摘要：

| 字段 | 含义 |
|------|------|
| `result` | 存在 `error` 则为 `false`；仅有 `warn` 时可为 `true` |
| `summary` | 一句话结论 |
| `problems[].node` | 违规图层路径 |
| `problems[].detail` | 具体问题描述 |
| `problems[].type` | `error` 必须修 / `warn` 建议优化 |

**后续建议**：

- 存在 `error` → 建议用户修复设计稿或规则允许的范围后再转码
- 仅有 `warn` → 可询问是否继续转码
- 检查通过 → 可主动询问是否进入 D2C 转码流程

---

## 参考文档

- **环境检查详情**：[ENV-SETUP.md](ENV-SETUP.md) — Node.js 安装、Deco CLI 管理
- **产物集成指南**：[INTEGRATION.md](INTEGRATION.md) — 代码放置、路由注册
- **故障排查**：[TROUBLESHOOTING.md](TROUBLESHOOTING.md) — 常见错误与解决方案
- **脚本目录**：[scripts/](scripts/) — 可执行的自动化脚本
