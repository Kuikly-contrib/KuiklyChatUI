# 环境检查详细步骤

> 💡 **快捷方式**：`bash scripts/deco-env-check.sh` 可自动完成以下所有检查。以下为手动操作参考。

## Node.js（≥ 18.0.0）

若 `node -v` 已满足版本要求，直接通过。否则按以下优先级尝试安装（**仅尝试匹配到的第一个工具，失败后不再尝试下一个**）：

### 方式 1：nvm（优先）

```bash
# 检查 nvm 是否可用
command -v nvm

# 若未加载（脚本会自动尝试以下路径）
source ~/.nvm/nvm.sh 2>/dev/null       # 默认安装路径
source $NVM_DIR/nvm.sh 2>/dev/null     # 自定义安装路径

# 安装并切换
nvm install 18 && nvm use 18
```

### 方式 2：volta（nvm 不可用时）

```bash
volta install node@18
```

### 方式 3：fnm

```bash
fnm install 18 && fnm use 18
```

### 均不可用

❌ 停止，告知用户手动安装。推荐方式：
- nvm: https://github.com/nvm-sh/nvm#installing-and-updating
- volta: https://volta.sh
- fnm: https://github.com/Schniz/fnm
- brew: `brew install node@18`（macOS）
- 官网: https://nodejs.org

---

## npm

`npm -v` 存在即通过。不存在 → ❌ 停止（npm 随 Node.js 一起安装）。

---

## Deco CLI（`@tencent/deco`）

**最低版本**：`>= 0.6.51`（`deco-env-check.sh` 会在安装/每日更新后校验；版本过低会尝试强制升级，仍不满足则失败退出）。

### 未安装

```bash
# 使用腾讯内网镜像源（默认）
npm install -g @tencent/deco --registry=https://mirrors.tencent.com/npm/

# 如果需要指定其他 registry
npm install -g @tencent/deco --registry=<your-registry-url>
```

> 脚本默认使用 `https://mirrors.tencent.com/npm/` 作为 npm 源。可通过 `--registry` 参数覆盖：`bash scripts/deco-env-check.sh --registry <url>`

失败 → ❌ 停止。

### 已安装 — 每日更新控制

检查 `~/.deco/.last_update` 标记文件：

```bash
# 读取上次更新日期
LAST_UPDATE=$(cat ~/.deco/.last_update 2>/dev/null)
TODAY=$(date +%Y-%m-%d)

if [ "$LAST_UPDATE" = "$TODAY" ]; then
    echo "今日已更新，跳过"
else
    npm install -g @tencent/deco --registry=https://mirrors.tencent.com/npm/
    mkdir -p ~/.deco && echo "$TODAY" > ~/.deco/.last_update
fi
```

更新失败 → ⚠️ 警告（当前版本仍可使用），不阻塞流程；但若当前版本 **< 0.6.51**，仍会 ❌ 失败退出。

### 版本过低（< 0.6.51）

脚本会输出 `Deco CLI 版本过低` 并尝试 `npm install -g @tencent/deco` 升级。仍不满足时手动执行：

```bash
npm install -g @tencent/deco --registry=https://mirrors.tencent.com/npm/
deco --version   # 确认 >= 0.6.51
```
