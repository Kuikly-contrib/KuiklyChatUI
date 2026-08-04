#!/usr/bin/env bash
# ============================================================
# deco-scan-components.sh — Deco Kuikly 组件扫描（跨平台通用版）
# 用法:
#   bash deco-scan-components.sh <scan-path> \
#        [--git <repo-url>] \
#        [--dsl compose|traditional] \
#        [--include-internal] \
#        [--namespace <ns>]
#
# 参数:
#   <scan-path>                本地 Kotlin 源码目录（必填，支持 ~ 展开）
#   --git <repo-url>           Git 仓库地址（可选，未指定时从 scan-path 向上找 Git 根并读 remote.origin.url）
#   --dsl compose|traditional  指定 DSL 类型（可选，默认按工程结构自动推断）
#   --include-internal         包含 internal 可见性组件入口（默认仅扫描 public）
#   --namespace <ns>            业务 namespace（可选）
#
# 退出码:
#   0  扫描成功（草稿已保存，Portal URL 已输出）
#   1  不可恢复的失败（参数 / 扫描 / 保存草稿等）
#   2  需用户决策：扫描到 0 个组件且未启用 --include-internal，
#      建议 Agent 询问用户后用 --include-internal 重新执行本脚本
#   3  需用户输入：未提供 --git 且本地 Git 仓库无 remote.origin.url，
#      建议 Agent 询问用户后用 --git <repo-url> 重新执行本脚本
#   4  登录态过期，需 Agent 介入：先告知用户去浏览器登录，再独立执行 'deco login'，
#      登录成功后用相同参数重新执行本脚本（脚本内不再内嵌 deco login，
#      避免 Agent 等待 Shell tool 同步返回时无法提醒用户去浏览器操作）
#
# 注意：本脚本不做环境检查（Node.js / npm / Deco CLI），由 Agent 在调用前
#       视情况自行执行 deco-env-check.sh。
#
# 支持平台: macOS / Linux / Windows (WSL/Git Bash)
# ============================================================
set -euo pipefail

# ---- 平台检测 ----
case "$(uname -s)" in
    Darwin*)              OS_TYPE="macos" ;;
    Linux*)               OS_TYPE="linux" ;;
    MINGW*|MSYS*|CYGWIN*) OS_TYPE="windows" ;;
    *)                    OS_TYPE="unknown" ;;
esac

# ---- 颜色 ----
if [ -t 1 ] && command -v tput &>/dev/null && [ "$(tput colors 2>/dev/null || echo 0)" -ge 8 ]; then
    RED='\033[0;31m'
    GREEN='\033[0;32m'
    YELLOW='\033[1;33m'
    CYAN='\033[0;36m'
    NC='\033[0m'
else
    RED='' GREEN='' YELLOW='' CYAN='' NC=''
fi

ok()   { echo -e "${GREEN}✔${NC} $*"; }
warn() { echo -e "${YELLOW}⚠${NC} $*"; }
fail() { echo -e "${RED}✗${NC} $*"; }
info() { echo -e "${CYAN}ℹ${NC} $*"; }

# ---- 默认值 ----
SCAN_PATH=""
GIT_URL=""
DSL_TYPE=""             # compose / traditional / 空=自动推断
INCLUDE_INTERNAL=false
NAMESPACE=""
DECO_OUTPUT=""
DECO_EXIT_CODE=0

portable_mktemp() {
    if [ "$OS_TYPE" = "macos" ]; then
        mktemp /tmp/deco-scan.XXXXXX
    else
        mktemp --tmpdir deco-scan.XXXXXX
    fi
}

# ============================================================
# 参数解析
# ============================================================
parse_args() {
    while [ $# -gt 0 ]; do
        case "$1" in
            --git)
                shift
                GIT_URL="${1:-}"
                ;;
            --dsl)
                shift
                DSL_TYPE="${1:-}"
                if [[ "$DSL_TYPE" != "compose" && "$DSL_TYPE" != "traditional" ]]; then
                    fail "--dsl 参数必须为 compose 或 traditional"
                    exit 1
                fi
                ;;
            --include-internal)
                INCLUDE_INTERNAL=true
                ;;
            --namespace)
                shift
                NAMESPACE="${1:-}"
                ;;
            *)
                if [ -z "$SCAN_PATH" ]; then
                    SCAN_PATH="$1"
                else
                    fail "未知参数: $1"
                    exit 1
                fi
                ;;
        esac
        shift
    done

    if [ -z "$SCAN_PATH" ]; then
        fail "缺少扫描路径"
        exit 1
    fi

    # 路径展开 ~ 与相对路径
    case "$SCAN_PATH" in
        "~")     SCAN_PATH="$HOME" ;;
        "~/"*)   SCAN_PATH="$HOME/${SCAN_PATH#~/}" ;;
    esac
    if [ "${SCAN_PATH:0:1}" != "/" ]; then
        SCAN_PATH="$(cd "$(pwd)" && pwd)/$SCAN_PATH"
    fi
}

# ============================================================
# 参数校验（输入收集是 Agent 在调用脚本前完成的事，本函数只校验/兜底）
# ============================================================
step_validate_input() {
    if [ ! -d "$SCAN_PATH" ] && [ ! -f "$SCAN_PATH" ]; then
        fail "扫描路径不存在: $SCAN_PATH"
        exit 1
    fi

    # DSL 类型默认值兜底：若 Agent 未传 --dsl，按工程结构推断
    if [ -z "$DSL_TYPE" ]; then
        local has_compose=false has_traditional=false
        local search_dir="$SCAN_PATH"
        [ -f "$search_dir" ] && search_dir="$(dirname "$search_dir")"

        if grep -rq "@Composable" "$search_dir" --include="*.kt" 2>/dev/null; then
            has_compose=true
        fi
        if grep -rq "attr {" "$search_dir" --include="*.kt" 2>/dev/null; then
            has_traditional=true
        fi

        if $has_compose && ! $has_traditional; then
            DSL_TYPE="compose"
        elif $has_traditional && ! $has_compose; then
            DSL_TYPE="traditional"
        elif $has_compose && $has_traditional; then
            DSL_TYPE="compose"
        else
            DSL_TYPE="traditional"
        fi
    fi

    if [[ "$SCAN_PATH" == *"/.codebuddy/skills"* ]]; then
        warn "scan-path 落在 .codebuddy/skills 内 — Git 可能指向 skill 仓库而非用户业务工程"
        echo "  请改为用户 Kuikly 工程的 Kotlin 源码目录（如 shared/src/commonMain/kotlin/...）"
    fi
}

# 输出建议重试命令（保留原参数）
print_retry_hint() {
    local extra_arg="$1"
    echo "    bash $(basename "$0") \"$SCAN_PATH\" $extra_arg \\"
    local extra=""
    [ -n "$NAMESPACE" ] && extra+=" --namespace \"$NAMESPACE\""
    [ -n "$DSL_TYPE" ]  && extra+=" --dsl $DSL_TYPE"
    $INCLUDE_INTERNAL   && extra+=" --include-internal"
    [ -n "$GIT_URL" ]   && extra+=" --git \"$GIT_URL\""
    [ -n "$extra" ] && echo "        $extra"
}

# ============================================================
# 预检 Git 仓库地址（避免无效的远程调用）
# ============================================================
step_preflight_git() {
    if [ -n "$GIT_URL" ]; then
        return 0
    fi

    if ! command -v git &>/dev/null; then
        # git 未装就不预检了，让 deco 自己推断/报错
        return 0
    fi

    local search_dir="$SCAN_PATH"
    [ -f "$search_dir" ] && search_dir="$(dirname "$search_dir")"

    local remote_url=""
    remote_url=$(git -C "$search_dir" config --get remote.origin.url 2>/dev/null || echo "")

    if [ -z "$remote_url" ]; then
        echo ""
        warn "未提供 --git 且 scan-path 所在 Git 仓库未配置 remote.origin.url"
        info "deco 会因此报错（Git 仓库地址解析失败），已提前终止以节省一次远程调用。"
        echo ""
        echo "  请向用户确认 Git 仓库地址后用 --git 重新执行："
        print_retry_hint "--git <repo-url>"
        exit 3
    fi

    # 显式传给 deco --git，与 preflight 判定保持一致（Deco 虽也从 scan-path 推断，但显式传递更可靠）
    GIT_URL="$remote_url"
}

# ============================================================
# Step 3: 执行扫描
# ============================================================
build_deco_cmd() {
    local cmd
    if [ "$DSL_TYPE" = "compose" ]; then
        cmd="deco scan"
    else
        cmd="deco scan-dsl"
    fi
    cmd+=" \"$SCAN_PATH\""
    [ -n "$GIT_URL" ]   && cmd+=" --git \"$GIT_URL\""
    [ -n "$NAMESPACE" ] && cmd+=" --namespace \"$NAMESPACE\""
    $INCLUDE_INTERNAL   && cmd+=" --include-internal"
    echo "$cmd"
}

# 精简 deco 扫描实时输出
filter_deco_scan_stream() {
    local mode="pass"
    local line

    while IFS= read -r line || [ -n "$line" ]; do
        case "$line" in
            "⚠ 未扫描到 public"*| "ℹ 如需包含 internal"*)
                continue
                ;;
            *"✗"*|*"诊断建议"*|*"相关命令:"*|*"未登录"*|*"请先登录"*|*"扫描失败"*|*"保存草稿失败"*)
                printf '%s\n' "$line"
                continue
                ;;
        esac

        case "$line" in
            "🔍 组件扫描"|"🔍 组件扫描 (DSL)"|"🔍 组件扫描 (Compose)"|"📊 扫描统计")
                mode="skip"
                continue
                ;;
            "ℹ 扫描目标:"*| "ℹ 框架类型:"*| "ℹ Git 仓库:"*| "ℹ 业务 namespace:"*| "ℹ 包含 internal 组件入口:"*)
                continue
                ;;
        esac

        if [ "$mode" = "skip" ]; then
            case "$line" in
                "ℹ 正在解析"*|"✓ 扫描完成"*|"✓ 扫描完成！"*)
                    mode="pass"
                    ;;
                ""|"  ├── "*|"  └── "*|"  │"*)
                    continue
                    ;;
                *)
                    mode="pass"
                    ;;
            esac
        fi

        if [ "$mode" = "pass" ]; then
            case "$line" in
                "  ├── "*|"  └── "*)
                    continue
                    ;;
            esac
            printf '%s\n' "$line"
        fi
    done
}

extract_scan_stat() {
    local key="$1"
    echo "$DECO_OUTPUT" | grep -E "$key" | head -1 | sed -E 's/^[[:space:]]*[├└│─]*[[:space:]]*//; s/^[^:]*:[[:space:]]*//; s/[[:space:]]*$//'
}

run_deco_cmd() {
    local full_cmd="$1"
    local tmp_output
    tmp_output=$(portable_mktemp)

    set +e
    eval "$full_cmd" 2>&1 | tee "$tmp_output" | filter_deco_scan_stream
    local pipe_status=("${PIPESTATUS[@]}")
    set -e

    DECO_EXIT_CODE=${pipe_status[0]}
    DECO_OUTPUT=$(cat "$tmp_output")
    rm -f "$tmp_output"
}

# 从 deco 输出中提取 Portal URL（"编辑页面: <URL>" 行）
extract_portal_url() {
    echo "$DECO_OUTPUT" | grep -oE 'https?://[^[:space:]]+' | grep -E '\?env=|/resolve\?' | head -1
}

step_run_scan() {
    local full_cmd
    full_cmd=$(build_deco_cmd)

    DECO_OUTPUT=""
    DECO_EXIT_CODE=0

    run_deco_cmd "$full_cmd"

    # ---- 登录态过期 → exit 4 ----
    if echo "$DECO_OUTPUT" | grep -qiE "未登录|Flowly Token|token.*过期|token.*expired|token.*invalid|请先登录|请确认.*登录"; then
        echo ""
        warn "登录态已过期，需重新登录后才能扫描"
        echo "  重新登录后用相同参数再次执行本脚本即可。"
        exit 4
    fi

    # ---- Git 仓库地址解析失败 → exit 3 ----
    if echo "$DECO_OUTPUT" | grep -qE "Git 仓库地址解析失败"; then
        echo ""
        warn "Git 仓库地址解析失败"
        echo "  请向用户确认后用 --git 重新执行："
        print_retry_hint "--git <repo-url>"
        exit 3
    fi

    # ---- 其它显式失败 ----
    if echo "$DECO_OUTPUT" | grep -qE "扫描失败|保存草稿失败|路径不存在"; then
        fail "扫描失败"
        echo "$DECO_OUTPUT" | grep -E "扫描失败|保存草稿失败|路径不存在|✗|FAILED" | head -5 | sed 's/^/  /'
        exit 1
    fi

    # ---- 0 组件且未含 internal → exit 2 ----
    if echo "$DECO_OUTPUT" | grep -qE "组件数量[:：][[:space:]]*0"; then
        if ! $INCLUDE_INTERNAL; then
            local internal_n
            internal_n=$(extract_scan_stat "已过滤 internal 入口")
            echo ""
            if [ -n "$internal_n" ]; then
                warn "扫描到 0 个 public 组件（已过滤 ${internal_n} 个 internal）"
            else
                warn "扫描到 0 个 public 组件"
            fi
            echo "  是否包含 internal 重新扫描："
            print_retry_hint "--include-internal"
            exit 2
        fi
        warn "扫描到 0 个组件（已包含 internal），请确认目录含 Kuikly 组件代码"
        exit 1
    fi

    # ---- 成功：提取 Portal URL ----
    local portal_url component_n variant_n
    portal_url=$(extract_portal_url)
    component_n=$(extract_scan_stat "组件数量")
    variant_n=$(extract_scan_stat "变体数量")

    if [ -z "$portal_url" ]; then
        if [ "$DECO_EXIT_CODE" -ne 0 ]; then
            fail "扫描异常退出（退出码: $DECO_EXIT_CODE）"
            exit 1
        fi
        warn "未在 deco 输出中找到 Portal URL（扫描可能未正常完成）"
        exit 1
    fi

    ok "扫描完成，草稿已保存"
    echo "  DSL: $DSL_TYPE | 组件: ${component_n:-?} | 变体: ${variant_n:-?}"
    echo "  Portal: $portal_url"
}

# ============================================================
# 主流程
# ============================================================
main() {
    parse_args "$@"

    step_validate_input
    step_preflight_git

    local scan_rel="$SCAN_PATH"
    case "$scan_rel" in
        "$HOME"/*) scan_rel="~/${scan_rel#$HOME/}" ;;
    esac
    info "扫描 | DSL=$DSL_TYPE | ${scan_rel}"

    step_run_scan
    exit 0
}

main "$@"
