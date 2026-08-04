# 故障排查速查

## 环境问题

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| `nvm: command not found` | nvm 未加载到当前 shell | `source ~/.nvm/nvm.sh`（或 `$NVM_DIR/nvm.sh`）；或重启终端 |
| `npm: command not found` | Node.js 未安装 | 安装 Node.js >= 18（见 ENV-SETUP.md） |
| Deco CLI 安装失败 | npm 源不可达或权限不足 | 检查网络；默认使用 `--registry=https://mirrors.tencent.com/npm/`；或 `sudo npm install -g`；推荐使用 nvm/volta 避免权限问题 |
| `Deco CLI 版本过低` / 需要 `>= 0.6.51` | 全局 `@tencent/deco` 过旧，或 npm 更新未拉到新版本 | 执行 `npm install -g @tencent/deco --registry=https://mirrors.tencent.com/npm/` 后重跑 `bash scripts/deco-env-check.sh` |
| Permission error | 系统 npm 需要 sudo | 改用 nvm/volta/fnm 管理的 Node.js |

## 认证问题

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| `未登录` / `未登录 Flowly` | Deco 认证过期或首次使用 | `deco-convert.sh` / `deco-scan-components.sh` 检测到后 **exit 4**，由 Agent 介入：先告知用户去浏览器登录 → 独立执行 `deco login` → 用原参数重跑脚本 |
| `图片上传失败` + `请检查 Flowly Token` | 登录态（Flowly Token）过期，导致图片无法上传到 CDN | 同上：脚本检测到 `Flowly Token` 关键词 → exit 4 → Agent 介入处理 |
| `图片上传失败` 但无 Token 相关提示 | 图片上传服务暂时不可用或网络问题 | 检查网络连接；等待几分钟后重试；确认 Deco 服务状态 |
| 登录超时 | 登录未在限定时间内完成 | 重新执行 `deco login` |
| 浏览器未自动打开 | 系统默认浏览器设置问题 | 从终端输出中手动复制 URL 到浏览器 |
| 重置认证 | 需要清除旧 token | 删除 `~/.deco/config.json` 后重新执行 `deco login` |
| IOA 认证弹窗 / 证书错误 | 腾讯内网 IOA 安全认证拦截了 npm 请求 | 确保 IOA 客户端已登录且证书有效；如果 IOA 证书过期，先更新 IOA 再重试 npm install |
| `UNABLE_TO_VERIFY_LEAF_SIGNATURE` | IOA 证书链不被 Node.js 信任 | 临时方案：`export NODE_TLS_REJECT_UNAUTHORIZED=0` 再重试；长期方案：更新 IOA 证书 |

## 设计源（URL / HTML）问题

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| URL 缺少 `node-id` | 复制了浏览器地址栏的链接 | 在 Figma 中选中元素 → 右键 → Copy link to selection |
| 转码失败（Group 元素） | 选择了 Group 而非 Frame/Component | 选择 Frame 或 Component 再复制链接 |
| `无效的设计源引用：既不是 Figma URL...也不是本地 .html / .htm 文件` | 第一个参数既不含 `figma.com`+`node-id`，后缀也不是 `.html` / `.htm` | 给出 Figma URL 或本地 `.html` / `.htm` 文件路径；HTML 源**只识别后缀**，不会做 MIME 探测 |
| `HTML 文件不存在` | 传入路径在解析后不存在（绝对/相对/`~` 已规范化） | 检查路径拼写；脚本相对路径以**调用 cwd** 为基准（不是 `--project-dir`） |
| `HTML 数据源不接受目录路径` | 传入了目录（即使目录名以 `.html` 结尾） | 直接传 `.html` / `.htm` 文件路径；Deco 不会自动在目录下找 `index.html` |

## 转码问题

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| 本地模式连接超时 | Figma Desktop 未启动或 MCP 未开启 | 启动 Figma Desktop 并启用 Dev Mode |
| `ECONNREFUSED` on 3845 | Figma Desktop 未运行或未打开设计文件 | 启动 Figma Desktop 并打开目标设计文件 |
| 输出目录为空 | 转码中断或网络异常 | 重新执行转码命令 |
| 转码配置里 Git 仓库是 `kuikly-skills.git` 等业务无关地址 | Agent 在 `.codebuddy/skills/` 目录下执行脚本，Deco 用 **cwd** 推断 Git | 用 `--project-dir` 指向用户 Kuikly 工程根目录，或在该工程根目录下执行；必要时显式 `--git <repo-url>` |
| 长时间无 deco 进度输出（超过 5min 仍无任何变化） | deco 进程可能异常退出或网络严重阻塞 | 检查 deco 进程：`ps aux \| grep deco`，必要时手动终止后重试。**少于 5min 的静默不算卡死** |
| 远程模式连接超时 / `[Figma MCP] SSE 连接超时：未能获取 sessionId` | Relay Server 不可达、远程 Figma Desktop 未就绪、或设计稿未共享给代理账号 | 检查内网/VPN；确认设计稿已共享给远程代理（至少 can view）；稍后重试 |
| 转码成功但退出码非 0 | 非关键步骤（如产物目录查找）命令返回非 0 | 以输出内容判断成败：有 `✓ 生成成功` 或 `✔ 转码完成` 即为成功，忽略退出码 |
| Agent 将成功执行误判为 disapprove / 将明确失败误判为「用户取消」 | 只看 tool 包装 exit code，忽略同轮已展示的脚本输出 | 同轮有脚本输出就以内容判成败；有 `✗`/`处理失败` = 失败，Rule 4 简练反馈，**禁止**追问「是否取消」 |
| Agent 在脚本失败后重试/绕过脚本 | Agent 违反行为约束 | 脚本失败后 Agent 必须立即停止，输出错误报告，等待用户指示。禁止重试、禁止执行 `deco --version`/`nc` 等诊断命令、禁止绕过脚本直接调用 `deco` |
| `图片上传失败，无法继续打包` | 登录态过期（Flowly Token 失效）或网络/服务问题 | 含 `Flowly Token` → 脚本 exit 4 → Agent 介入：告知用户去浏览器登录 → `deco login` → 重跑；不含 Token 字样 → 报告错误并停止 |

## 集成（deco-integrate.sh）问题

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| `deco-integrate.sh` 退出码 5 | commonMain/kotlin 推断不出唯一页面目录（0 / 多个 commonMain，或 commonMain 内候选数 != 1） | Agent 把脚本输出里列出的候选目录展示给用户，询问后用 `--pages-dir` 重新执行 |
| `deco-integrate.sh` 退出码 6 | 预检发现图片资源在 `commonMain/assets/` 下有同名冲突（尚未写入） | Agent 展示冲突清单 → 用户选 skip/overwrite → 用相同参数 + `--on-conflict` **实跑**重试 |
| `--pages-dir` / `--assets-dir` 绝对路径必须在 `--project-dir` 之下 | 用户传入的绝对路径越界 | 改用相对路径（基于 `--project-dir`）或把绝对路径调整到项目内 |
| 自动探测把产物放到了不期望的目录 | commonMain 内有多个同 DSL 的现有目录，脚本归约到了公共父目录 | 用 `--pages-dir` 显式指定目标目录覆盖自动推断 |
| `无法从 pages-dir 推导 commonMain/assets 目录` | pages-dir 不在 `commonMain/kotlin/` 下 | 用 `--assets-dir <path>` 显式指定资源目录 |

## 设计稿规则检查问题

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| `规则文件不存在` | `--rule` 相对路径基准错误 | 加 `--project-dir` 指向工程根，或使用绝对路径 |
| `规则文件内容为空` | Markdown 规则未编写 | 按 Portal 用户手册格式编写 `###` 规则条目 |
| `审查失败` + 未登录 | Flowly Token 过期 | 脚本 exit 4 → Agent 介入 `deco login` 后重跑 |
| JSON 中 `result: false` | 设计稿违反规则 | **不是脚本失败**；向用户展示 `problems` 并建议修复后再转码 |

## 跨平台问题

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| `sed: invalid option -- ''` (Linux) | macOS BSD sed 和 GNU sed 的 `-i` 语法不同 | 脚本已内置 `portable_sed_i` 自动适配，如手动操作：macOS 用 `sed -i ''`，Linux 用 `sed -i` |
| `nc: command not found` | 系统未安装 netcat | 脚本会回退到 `/dev/tcp` 或 `curl`；或手动安装：`apt install netcat-openbsd`（Linux） |
| 产物目录找不到 | Node.js 管理工具（volta/nvm/系统）的全局包路径各不相同 | 脚本内置多路径探测；也可从转码日志中直接复制路径 |

## 快速诊断

**脚本方式**（推荐）：

```bash
# 一键环境诊断
bash scripts/deco-env-check.sh
```

**手动命令**：

```bash
# 检查 Node.js 版本
node -v

# 检查 Deco CLI 版本
deco --version

# 检查 Figma MCP 是否可用（用 TCP 探测）
nc -z -w 3 127.0.0.1 3845 && echo "MCP 可用" || echo "MCP 不可用"

# 查看 Deco 输出目录（路径因 Node.js 管理工具而异）
cat /tmp/deco-latest-output 2>/dev/null || echo "无缓存，请先执行转码"
```
