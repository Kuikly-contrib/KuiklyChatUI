#!/usr/bin/env bash
# ============================================================
# deco-check.sh — Deco 设计稿规则检查（跨平台通用版）
# 用法:
#   bash deco-check.sh <source-ref> --rule <rule.md> \
#        [--project-dir <path>] [--output <path>] [--remote]
#
# 参数:
#   <source-ref>               设计源引用：Figma URL 或本地 .html / .htm 文件路径
#   --rule <path>              检查规则 markdown 文件路径（必填）
#   --project-dir <path>       用户工程根目录（默认 cwd；相对路径规则文件以此目录为基准解析）
#   --output <path>            输出 JSON 文件路径（可选，默认由 deco 写入 ~/.deco/output/）
#   --remote                   强制使用远程模式（可选；HTML 源默认不传 --remote，因为不依赖 Figma MCP）
#
# 退出码:
#   0  检查执行成功（JSON 已写入；result 为 true/false 均视为成功）
#   1  不可恢复的失败（参数错误 / 规则文件缺失 / 检查失败等）
#   4  登录态过期，需 Agent 介入：先告知用户去浏览器登录，再独立执行 'deco login'，
#      登录成功后用相同参数重新执行本脚本
#
# 注意：本脚本不做环境检查，由 Agent 在调用前视情况执行 deco-env-check.sh
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
SOURCE_REF=""
RULE_PATH=""
PROJECT_DIR="$(pwd)"
ORIGIN_CWD="$(pwd)"
OUTPUT_PATH=""
FORCE_REMOTE=false
REMOTE_FLAG=""
IS_HTML_SOURCE=false
HTML_PATH=""
DESIGN_SOURCE_FLAG=""
DECO_OUTPUT=""
DECO_EXIT_CODE=0

portable_mktemp() {
    if [ "$OS_TYPE" = "macos" ]; then
        mktemp /tmp/deco-check.XXXXXX
    else
        mktemp --tmpdir deco-check.XXXXXX
    fi
}

portable_tcp_check() {
    local host="$1" port="$2" timeout="${3:-3}"
    if command -v nc &>/dev/null; then
        nc -z -w "$timeout" "$host" "$port" 2>/dev/null
    elif command -v bash &>/dev/null && [ -e /dev/tcp ]; then
        (echo >/dev/tcp/"$host"/"$port") 2>/dev/null
    elif command -v timeout &>/dev/null; then
        timeout "$timeout" bash -c "echo >/dev/tcp/$host/$port" 2>/dev/null
    else
        curl -sf --connect-timeout "$timeout" "http://$host:$port" &>/dev/null
    fi
}

resolve_path() {
    local raw="$1"
    local base="$2"
    local resolved="$raw"

    case "$resolved" in
        "~")    resolved="$HOME" ;;
        "~/"*)  resolved="$HOME/${resolved#~/}" ;;
    esac
    if [ "${resolved:0:1}" != "/" ]; then
        resolved="$base/$resolved"
    fi
    if command -v realpath &>/dev/null; then
        resolved="$(realpath "$resolved" 2>/dev/null || echo "$resolved")"
    elif command -v python3 &>/dev/null; then
        resolved="$(python3 -c "import os,sys;print(os.path.abspath(sys.argv[1]))" "$resolved" 2>/dev/null || echo "$resolved")"
    fi
    printf '%s' "$resolved"
}

# ============================================================
# 参数解析
# ============================================================
parse_args() {
    while [ $# -gt 0 ]; do
        case "$1" in
            --rule)
                shift
                RULE_PATH="${1:-}"
                ;;
            --project-dir)
                shift
                PROJECT_DIR="${1:-}"
                ;;
            --output)
                shift
                OUTPUT_PATH="${1:-}"
                ;;
            --remote)
                FORCE_REMOTE=true
                ;;
            *)
                if [ -z "$SOURCE_REF" ]; then
                    SOURCE_REF="$1"
                else
                    fail "未知参数: $1"
                    exit 1
                fi
                ;;
        esac
        shift
    done

    if [ -z "$SOURCE_REF" ]; then
        fail "缺少设计源引用（Figma URL 或本地 HTML 文件路径）"
        exit 1
    fi
    if [ -z "$RULE_PATH" ]; then
        fail "缺少 --rule <rule.md> 参数"
        exit 1
    fi
}

normalize_project_dir() {
    PROJECT_DIR="$(resolve_path "$PROJECT_DIR" "$ORIGIN_CWD")"
    if [ ! -d "$PROJECT_DIR" ]; then
        fail "工程目录不存在: $PROJECT_DIR"
        exit 1
    fi
}

step_detect_source_type() {
    local lc
    lc="$(echo "$SOURCE_REF" | tr '[:upper:]' '[:lower:]')"
    case "$lc" in
        *.html|*.htm) ;;
        *) return ;;
    esac

    IS_HTML_SOURCE=true
    HTML_PATH="$(resolve_path "$SOURCE_REF" "$ORIGIN_CWD")"

    if [ ! -e "$HTML_PATH" ]; then
        fail "HTML 文件不存在: $HTML_PATH"
        exit 1
    fi
    if [ -d "$HTML_PATH" ]; then
        fail "HTML 数据源不接受目录路径: $HTML_PATH"
        exit 1
    fi
    if [ ! -f "$HTML_PATH" ]; then
        fail "HTML 路径不是普通文件: $HTML_PATH"
        exit 1
    fi

    DESIGN_SOURCE_FLAG="--design-source html"
}

step_validate_url() {
    $IS_HTML_SOURCE && return

    if [[ "$SOURCE_REF" != *"figma.com"* ]]; then
        fail "无效的设计源引用：既不是 Figma URL（缺少 figma.com）也不是本地 .html / .htm 文件"
        exit 1
    fi

    if [[ "$SOURCE_REF" != *"node-id="* ]]; then
        fail "Figma URL 缺少 node-id 参数"
        echo "  正确获取方式：在 Figma 选中目标 Frame/Component → 右键 → Copy link to selection"
        exit 1
    fi
}

step_validate_rule() {
    RULE_PATH="$(resolve_path "$RULE_PATH" "$PROJECT_DIR")"

    if [ ! -f "$RULE_PATH" ]; then
        fail "规则文件不存在: $RULE_PATH"
        exit 1
    fi
    if [ ! -s "$RULE_PATH" ]; then
        fail "规则文件内容为空: $RULE_PATH"
        exit 1
    fi

    if [ -n "$OUTPUT_PATH" ]; then
        OUTPUT_PATH="$(resolve_path "$OUTPUT_PATH" "$PROJECT_DIR")"
    fi
}

step_detect_mode() {
    if $FORCE_REMOTE; then
        REMOTE_FLAG="--remote"
        return
    fi

    # HTML 本地源不依赖 Figma Desktop / Figma MCP，跳过 3845 探测
    if $IS_HTML_SOURCE; then
        REMOTE_FLAG=""
        return
    fi

    if portable_tcp_check 127.0.0.1 3845; then
        REMOTE_FLAG=""
    else
        REMOTE_FLAG="--remote"
    fi
}

filter_deco_stream() {
    local mode="pass"
    local line

    while IFS= read -r line || [ -n "$line" ]; do
        case "$line" in
            *"✗"*|*"诊断建议"*|*"相关命令:"*|*"未登录"*|*"请先登录"*|*"检查失败"*)
                printf '%s\n' "$line"
                continue
                ;;
        esac

        case "$line" in
            "🔍 设计稿规则检查"|"检查配置:"|"检查结论:"|"问题统计"*)
                mode="skip"
                continue
                ;;
        esac

        if [ "$mode" = "skip" ]; then
            case "$line" in
                "ℹ ["*|"✓ ["*|"✅ 检查通过"*|*"❌ 检查未通过"*|*"检查结果:"*)
                    mode="pass"
                    ;;
                ""|"  •"*)
                    continue
                    ;;
                *)
                    mode="pass"
                    ;;
            esac
        fi

        if [ "$mode" = "pass" ]; then
            case "$line" in
                "  •"*)
                    printf '%s\n' "$line"
                    ;;
                "✅ 检查通过"*|*"❌ 检查未通过"*|*"检查结果:"*)
                    printf '%s\n' "$line"
                    ;;
            esac
        fi
    done
}

run_deco_cmd() {
    local inner_cmd="$1"
    local tmp_output
    tmp_output=$(portable_mktemp)

    set +e
    ( cd "$PROJECT_DIR" && eval "$inner_cmd" ) 2>&1 | tee "$tmp_output" | filter_deco_stream
    local pipe_status=("${PIPESTATUS[@]}")
    set -e

    DECO_EXIT_CODE=${pipe_status[0]}
    DECO_OUTPUT=$(cat "$tmp_output")
    rm -f "$tmp_output"
}

extract_check_output_path() {
    local path=""
    path=$(echo "$DECO_OUTPUT" | grep -E '检查结果:' | head -1 | sed -E 's/.*检查结果:[[:space:]]*//')
    if [ -z "$path" ] && [ -n "$OUTPUT_PATH" ]; then
        path="$OUTPUT_PATH"
    fi
    printf '%s' "$path"
}

extract_check_passed() {
    if echo "$DECO_OUTPUT" | grep -q "✅ 检查通过"; then
        echo "true"
    elif echo "$DECO_OUTPUT" | grep -q "❌ 检查未通过"; then
        echo "false"
    else
        echo ""
    fi
}

print_check_success_summary() {
    local passed="$1"
    local json_path="$2"
    local summary=""

    ok "检查完成"
    if [ "$passed" = "true" ]; then
        summary="通过"
    elif [ "$passed" = "false" ]; then
        summary="未通过（存在 error 或 warn，见 JSON 报告）"
    else
        summary="已完成"
    fi
    echo "  结果: $summary"

    summary=$(echo "$DECO_OUTPUT" | grep -E '检查结论:' -A1 | tail -1 | sed 's/^[[:space:]]*//')
    [ -n "$summary" ] && echo "  摘要: $summary"

    if [ -n "$json_path" ]; then
        ok "JSON: $json_path"
    fi
}

step_check() {
    local source_arg="$SOURCE_REF"
    $IS_HTML_SOURCE && source_arg="$HTML_PATH"

    local output_flag=""
    [ -n "$OUTPUT_PATH" ] && output_flag="--output \"$OUTPUT_PATH\""

    local inner_cmd="deco check \"$source_arg\" --rule \"$RULE_PATH\" $REMOTE_FLAG $DESIGN_SOURCE_FLAG $output_flag"

    local source_label="Figma"
    $IS_HTML_SOURCE && source_label="HTML"
    local mode_label="本地"
    [ -n "$REMOTE_FLAG" ] && mode_label="远程"
    $IS_HTML_SOURCE && mode_label="—"

    info "审查 | 源=${source_label} | ${mode_label} | 规则=${RULE_PATH} | ${PROJECT_DIR}"
    echo ""

    run_deco_cmd "$inner_cmd"
    echo ""

    local has_result=false
    if echo "$DECO_OUTPUT" | grep -qE "✅ 检查通过|❌ 检查未通过|检查结果:"; then
        has_result=true
    fi

    local needs_login=false
    if echo "$DECO_OUTPUT" | grep -qiE "未登录|Flowly Token|token.*过期|token.*expired|token.*invalid|请先登录"; then
        needs_login=true
    fi

    if $needs_login && ! $has_result; then
        echo ""
        warn "登录态已过期，需重新登录后才能审查"
        echo "  重新登录后用相同参数再次执行本脚本即可。"
        exit 4
    elif echo "$DECO_OUTPUT" | grep -qE "✗|检查失败|规则文件不存在|规则文件内容为空"; then
        fail "审查失败"
        echo "$DECO_OUTPUT" | grep -E "✗|检查失败|规则文件|FAILED" | head -10 | sed 's/^/    /'
        exit 1
    elif ! $has_result && [ "$DECO_EXIT_CODE" -ne 0 ]; then
        fail "审查异常退出（退出码 $DECO_EXIT_CODE）"
        exit 1
    fi

    local passed json_path
    passed=$(extract_check_passed)
    json_path=$(extract_check_output_path)
    print_check_success_summary "$passed" "$json_path"
}

main() {
    parse_args "$@"
    normalize_project_dir
    step_detect_source_type
    step_validate_url
    step_validate_rule
    step_detect_mode
    step_check
    exit 0
}

main "$@"
