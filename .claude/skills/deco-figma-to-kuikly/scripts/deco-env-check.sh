#!/usr/bin/env bash
# ============================================================
# deco-env-check.sh — Deco D2C 转码环境检查（跨平台通用版）
# 用法: bash deco-env-check.sh [--registry <npm-registry-url>]
# 返回: 0 = 环境就绪, 非 0 = 缺少依赖
#
# 支持平台: macOS / Linux / Windows (WSL/Git Bash)
# ============================================================
set -euo pipefail

# ---- 颜色（检测终端是否支持） ----
if [ -t 1 ] && command -v tput &>/dev/null && [ "$(tput colors 2>/dev/null || echo 0)" -ge 8 ]; then
    RED='\033[0;31m'
    GREEN='\033[0;32m'
    YELLOW='\033[1;33m'
    NC='\033[0m'
else
    RED='' GREEN='' YELLOW='' NC=''
fi

ok()   { echo -e "${GREEN}✔${NC} $*"; }
warn() { echo -e "${YELLOW}⚠${NC} $*"; }
fail() { echo -e "${RED}✗${NC} $*"; }

# ---- 全局变量 ----
MIN_NODE_MAJOR=18
MIN_DECO_VERSION="0.6.51"
DECO_PKG="@tencent/deco"
DECO_REGISTRY=""        # 空=使用默认（腾讯内网镜像源）
LAST_UPDATE_FILE="$HOME/.deco/.last_update"
EXIT_CODE=0
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ============================================================
# Beacon 事件上报（后台异步，不影响主流程）
# 依赖 @tencent/beacon-node-new-sdk 通过 npm -g 安装，运行时用 NODE_PATH 让 require 找到
# ============================================================
beacon_report() {
    local node_script="$SCRIPT_DIR/beacon-report.js"
    local skill_name="deco-figma-to-kuikly"
    local project_dir
    project_dir="$(pwd)"
    local project_name
    project_name=$(basename "$project_dir")

    command -v node &>/dev/null || return 0
    command -v npm &>/dev/null || return 0
    [ -f "$node_script" ] || return 0

    local global_root
    global_root=$(npm root -g 2>/dev/null || echo "")

    # 检查并全局安装依赖（仅首次需要，静默执行）
    if [ -z "$global_root" ] || [ ! -d "$global_root/@tencent/beacon-node-new-sdk" ]; then
        npm install -g @tencent/beacon-node-new-sdk --registry=https://mirrors.tencent.com/npm/ --silent &>/dev/null || return 0
        global_root=$(npm root -g 2>/dev/null || echo "")
    fi

    # 后台执行上报，不阻塞主流程；通过 NODE_PATH 让 require 解析到全局 node_modules
    NODE_PATH="$global_root" node "$node_script" \
        "skill_invoked" \
        "skill_name=$skill_name" \
        "project_name=$project_name" \
        "project_dir=$project_dir" &>/dev/null &
    disown 2>/dev/null || true
}

# ---- 默认 registry（腾讯内网镜像源） ----
DEFAULT_REGISTRY="https://mirrors.tencent.com/npm/"

# ---- 解析参数 ----
while [ $# -gt 0 ]; do
    case "$1" in
        --registry)
            shift
            DECO_REGISTRY="${1:-}"
            ;;
    esac
    shift
done

# 构建 npm install 命令（默认使用腾讯内网镜像源，可通过 --registry 覆盖）
npm_install_deco() {
    local registry="${DECO_REGISTRY:-$DEFAULT_REGISTRY}"
    npm install -g "$DECO_PKG" --registry="$registry" 2>&1
}

# 从 deco --version 输出中提取 x.y.z（取最后一处匹配，兼容带包名前缀）
extract_deco_version() {
    local raw="${1:-}"
    [ -z "$raw" ] && raw="$(deco --version 2>/dev/null || true)"
    echo "$raw" | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | tail -1
}

# 语义化版本比较：$1 >= $2 时返回 0
version_ge() {
    local ver="${1#v}" min="${2#v}"
    local IFS=.
    local -a ver_parts min_parts
    read -r -a ver_parts <<< "$ver"
    read -r -a min_parts <<< "$min"
    local i len="${#ver_parts[@]}"
    [ "${#min_parts[@]}" -gt "$len" ] && len="${#min_parts[@]}"
    for ((i = 0; i < len; i++)); do
        local v="${ver_parts[i]:-0}"
        local m="${min_parts[i]:-0}"
        if ((10#$v > 10#$m)); then return 0; fi
        if ((10#$v < 10#$m)); then return 1; fi
    done
    return 0
}

ensure_deco_min_version() {
    local ver
    ver="$(extract_deco_version)"
    if [ -z "$ver" ]; then
        fail "无法解析 Deco CLI 版本（deco --version 输出异常）"
        return 1
    fi
    DECO_VER="$ver"

    if version_ge "$ver" "$MIN_DECO_VERSION"; then
        return 0
    fi

    warn "Deco CLI v${ver} 版本过低（需要 >= ${MIN_DECO_VERSION}），尝试升级..."
    if npm_install_deco >/dev/null 2>&1; then
        ver="$(extract_deco_version)"
        if [ -n "$ver" ]; then
            DECO_VER="$ver"
        fi
        if version_ge "$ver" "$MIN_DECO_VERSION"; then
            DECO_UPDATE_NOTE="升级至 ${ver}（满足 >= ${MIN_DECO_VERSION}）"
            mkdir -p "$(dirname "$LAST_UPDATE_FILE")"
            date +%Y-%m-%d > "$LAST_UPDATE_FILE"
            return 0
        fi
    fi

    fail "Deco CLI 版本过低：当前 ${DECO_VER}，需要 >= ${MIN_DECO_VERSION}"
    local registry="${DECO_REGISTRY:-$DEFAULT_REGISTRY}"
    echo "  手动升级: npm install -g $DECO_PKG --registry=$registry"
    return 1
}

# ============================================================
# 状态收集（成功路径下用于合并到单行总结，失败路径下立即报错退出）
# ============================================================
NODE_VER=""
NPM_VER=""
DECO_VER=""
DECO_UPDATE_NOTE=""   # 例如 "今日已检查" / "已是最新" / "升级自 a.b.c" / "首次安装" / "检查更新失败,沿用当前版本"

# ============================================================
# 1. 检查 Node.js >= 18
# ============================================================
check_node() {
    # 尝试加载 nvm（静默）
    if ! command -v node &>/dev/null; then
        local nvm_candidates=(
            "$HOME/.nvm/nvm.sh"
            "${NVM_DIR:-/dev/null}/nvm.sh"
            "/usr/local/share/nvm/nvm.sh"
            "/usr/share/nvm/nvm.sh"
        )
        for nvm_path in "${nvm_candidates[@]}"; do
            if [ -f "$nvm_path" ]; then
                # shellcheck source=/dev/null
                source "$nvm_path" 2>/dev/null || true
                break
            fi
        done
    fi

    if command -v node &>/dev/null; then
        local ver major
        ver=$(node -v 2>/dev/null | sed 's/^v//')
        major=$(echo "$ver" | cut -d. -f1)
        if [ "$major" -ge "$MIN_NODE_MAJOR" ] 2>/dev/null; then
            NODE_VER="v${ver}"
            return 0
        fi
        warn "Node.js v${ver} 版本过低（需要 >= ${MIN_NODE_MAJOR}），尝试自动安装..."
    else
        warn "Node.js 未安装，尝试自动安装..."
    fi

    if command -v nvm &>/dev/null && nvm install "$MIN_NODE_MAJOR" >/dev/null 2>&1 && nvm use "$MIN_NODE_MAJOR" >/dev/null 2>&1; then
        NODE_VER="$(node -v)"
        return 0
    fi
    if command -v volta &>/dev/null && volta install "node@${MIN_NODE_MAJOR}" >/dev/null 2>&1; then
        NODE_VER="$(node -v)"
        return 0
    fi
    if command -v fnm &>/dev/null && fnm install "$MIN_NODE_MAJOR" >/dev/null 2>&1 && fnm use "$MIN_NODE_MAJOR" >/dev/null 2>&1; then
        NODE_VER="$(node -v)"
        return 0
    fi

    fail "无法自动安装 Node.js >= ${MIN_NODE_MAJOR}（请手动安装：nvm / volta / fnm / https://nodejs.org）"
    return 1
}

# ============================================================
# 2. 检查 npm
# ============================================================
check_npm() {
    if command -v npm &>/dev/null; then
        NPM_VER="$(npm -v)"
        return 0
    fi
    fail "npm 未找到（npm 随 Node.js 一起安装，请重装 Node.js）"
    return 1
}

# ============================================================
# 3. 检查 Deco CLI（安装/每日更新）
# ============================================================
check_deco() {
    if command -v deco &>/dev/null; then
        DECO_VER="$(extract_deco_version || echo unknown)"

        local today last_update
        today=$(date +%Y-%m-%d)
        last_update=$(cat "$LAST_UPDATE_FILE" 2>/dev/null || echo "")

        if [ "$last_update" = "$today" ]; then
            DECO_UPDATE_NOTE="今日已检查"
            ensure_deco_min_version || return 1
            return 0
        fi

        local before_ver="$DECO_VER"
        if npm_install_deco >/dev/null 2>&1; then
            mkdir -p "$(dirname "$LAST_UPDATE_FILE")"
            echo "$today" > "$LAST_UPDATE_FILE"
            DECO_VER="$(extract_deco_version || echo unknown)"
            if [ "$before_ver" = "$DECO_VER" ]; then
                DECO_UPDATE_NOTE="已是最新"
            else
                DECO_UPDATE_NOTE="升级自 ${before_ver}"
            fi
        else
            DECO_UPDATE_NOTE="检查更新失败,沿用当前版本"
        fi
        ensure_deco_min_version || return 1
        return 0
    fi

    echo "→ 首次安装 Deco CLI（约 30s~1min）..."
    if npm_install_deco >/dev/null 2>&1; then
        DECO_VER="$(extract_deco_version || echo unknown)"
        DECO_UPDATE_NOTE="首次安装"
        ensure_deco_min_version || return 1
        return 0
    fi

    fail "Deco CLI 安装失败"
    local registry="${DECO_REGISTRY:-$DEFAULT_REGISTRY}"
    echo "  手动安装: npm install -g $DECO_PKG --registry=$registry"
    return 1
}

# ============================================================
# 主流程
# ============================================================
main() {
    beacon_report

    check_node || { EXIT_CODE=1; fail "环境检查未通过：Node.js"; exit $EXIT_CODE; }
    check_npm  || { EXIT_CODE=1; fail "环境检查未通过：npm";     exit $EXIT_CODE; }
    check_deco || { EXIT_CODE=1; fail "环境检查未通过：Deco CLI"; exit $EXIT_CODE; }

    local note=""
    [ -n "$DECO_UPDATE_NOTE" ] && note=" (${DECO_UPDATE_NOTE})"
    ok "环境就绪：Node ${NODE_VER}  npm ${NPM_VER}  Deco ${DECO_VER}${note}"
    exit 0
}

main "$@"
