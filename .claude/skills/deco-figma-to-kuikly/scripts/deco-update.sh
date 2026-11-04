#!/usr/bin/env bash
# ============================================================
# deco-update.sh — Deco D2C 增量更新（跨平台通用版）
# 用法: bash deco-update.sh <file...> --source-ref <ref> [--project-dir <path>] [--design-source figma|html] ...
#
# 功能：把已有 Kuikly / Compose 代码对照最新设计稿做增量更新，
#       产出 git patch（用户自行 git apply），不覆盖原文件。
#
# 参数:
#   <file...>                   待更新的源文件（一个或多个 .kt 路径，必填）
#   --source-ref <ref>          新设计稿引用（Figma URL 或本地 HTML 路径，必填）
#   --project-dir <path>        用户 Kuikly / KMP 业务工程根目录（默认当前目录）
#                              Deco CLI 用 cwd 推断 Git 仓库；必须指向业务工程而非 skill 目录
#   --design-source <type>      设计源类型：figma（默认）| html
#   -o, --output <path>         增量 patch 输出路径（可选，默认写入 deco 输出目录）
#   --churn-threshold <n>       改动比例阈值（0-100，默认 95；超过该比例视为大改而非增量）
#   --remote                   强制使用远程模式（可选；HTML 源默认不传 --remote）
#   --download-assets           资源本地化（默认开启）
#   --no-download-assets        保留远程资源 URL
#   --image-scale N             图片导出倍率，正整数如 1/2/3（可选，默认 1）
#   -G, --component-groups <groups>
#                              启用非 common 的项目组件分组（逗号分隔，如 business,icon）
#   --prompt-dir <path>         自定义 Prompt Profile 目录路径（可选，优先于全局配置）
#
# 支持平台: macOS / Linux / Windows (WSL/Git Bash)
#
# 退出码:
#   0  增量更新成功（产出 patch）
#   1  不可恢复的失败（参数错误 / 更新失败 / 异常退出 / 登录态过期未登录 等）
#   4  登录态过期，需 Agent 介入：先告知用户去浏览器登录，再独立执行 'deco login'，
#      登录成功后用相同参数重新执行本脚本。
#
# 注意：本脚本不做环境检查（Node.js / npm / Deco CLI），由 Agent 在调用前
#       视情况自行执行 deco-env-check.sh（与其它 deco 脚本一致）。
# ============================================================
set -euo pipefail

# ---- 平台检测 ----
detect_os() {
    case "$(uname -s)" in
        Darwin*)  OS_TYPE="macos" ;;
        Linux*)   OS_TYPE="linux" ;;
        MINGW*|MSYS*|CYGWIN*) OS_TYPE="windows" ;;
        *)        OS_TYPE="unknown" ;;
    esac
}
detect_os

# ---- 颜色（检测终端是否支持） ----
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

# ---- 跨平台工具函数 ----

# 跨平台 mktemp
portable_mktemp() {
    if [ "$OS_TYPE" = "macos" ]; then
        mktemp /tmp/deco-output.XXXXXX
    else
        mktemp --tmpdir deco-output.XXXXXX
    fi
}

# 跨平台真实路径解析（fallback 到 python3）
portable_resolve() {
    local p="$1"
    if command -v realpath &>/dev/null; then
        realpath "$p" 2>/dev/null || echo "$p"
    elif command -v python3 &>/dev/null; then
        python3 -c "import os,sys;print(os.path.abspath(os.path.expanduser(sys.argv[1])))" "$p" 2>/dev/null || echo "$p"
    else
        echo "$p"
    fi
}

# ---- 默认值 ----
SOURCE_FILES=()
SOURCE_REF=""
PROJECT_DIR="$(pwd)"
DESIGN_SOURCE="figma"
OUTPUT_PATH=""
CHURN_THRESHOLD=95
FORCE_REMOTE=false
REMOTE_FLAG=""
DOWNLOAD_ASSETS=true
DOWNLOAD_ASSETS_FLAG=""
IMAGE_SCALE=""
IMAGE_SCALE_FLAG=""
COMPONENT_GROUPS=""
COMPONENT_GROUPS_FLAG=""
PROMPT_DIR=""
PROMPT_DIR_FLAG=""
ORIGIN_CWD="$(pwd)"

# ============================================================
# 解析参数
# ============================================================
parse_args() {
    while [ $# -gt 0 ]; do
        case "$1" in
            --source-ref)
                shift
                SOURCE_REF="${1:-}"
                ;;
            --project-dir)
                shift
                PROJECT_DIR="${1:-}"
                ;;
            --design-source)
                shift
                DESIGN_SOURCE="${1:-figma}"
                if [ "$DESIGN_SOURCE" != "figma" ] && [ "$DESIGN_SOURCE" != "html" ]; then
                    fail "--design-source 参数必须为 figma 或 html"
                    exit 1
                fi
                ;;
            -o|--output)
                shift
                OUTPUT_PATH="${1:-}"
                ;;
            --churn-threshold)
                shift
                CHURN_THRESHOLD="${1:-95}"
                if ! [[ "$CHURN_THRESHOLD" =~ ^[0-9]+$ ]] || [ "$CHURN_THRESHOLD" -gt 100 ]; then
                    fail "--churn-threshold 参数必须为 0-100 的整数"
                    exit 1
                fi
                ;;
            --remote)
                FORCE_REMOTE=true
                ;;
            --download-assets)
                DOWNLOAD_ASSETS=true
                ;;
            --no-download-assets)
                DOWNLOAD_ASSETS=false
                ;;
            --image-scale)
                shift
                IMAGE_SCALE="${1:-}"
                if ! [[ "$IMAGE_SCALE" =~ ^[1-9][0-9]*$ ]]; then
                    fail "--image-scale 参数必须为正整数（如 1、2、3）"
                    exit 1
                fi
                ;;
            -G|--component-groups)
                shift
                COMPONENT_GROUPS="${1:-}"
                if [ -z "$COMPONENT_GROUPS" ]; then
                    fail "--component-groups 参数不能为空"
                    exit 1
                fi
                ;;
            --prompt-dir)
                shift
                PROMPT_DIR="${1:-}"
                if [ -z "$PROMPT_DIR" ]; then
                    fail "--prompt-dir 参数不能为空"
                    exit 1
                fi
                ;;
            *)
                if [[ "$1" != -* ]]; then
                    SOURCE_FILES+=("$1")
                else
                    fail "未知参数: $1"
                    exit 1
                fi
                ;;
        esac
        shift
    done

    if [ ${#SOURCE_FILES[@]} -eq 0 ]; then
        fail "缺少待更新的源文件（<file...>）"
        exit 1
    fi
    if [ -z "$SOURCE_REF" ]; then
        fail "缺少 --source-ref 参数（新设计稿引用）"
        exit 1
    fi
}

# ============================================================
# 规范化 --project-dir（~ 展开、相对路径转绝对路径）
# ============================================================
normalize_project_dir() {
    case "$PROJECT_DIR" in
        "~")    PROJECT_DIR="$HOME" ;;
        "~/"*)  PROJECT_DIR="$HOME/${PROJECT_DIR#~/}" ;;
    esac
    if [ "${PROJECT_DIR:0:1}" != "/" ]; then
        PROJECT_DIR="$(cd "$(pwd)" && pwd)/$PROJECT_DIR"
    fi
    if [ ! -d "$PROJECT_DIR" ]; then
        fail "工程目录不存在: $PROJECT_DIR"
        exit 1
    fi
}

# ============================================================
# 校验源文件存在 + 设计源引用
# ============================================================
step_validate() {
    local missing=0
    for f in "${SOURCE_FILES[@]}"; do
        local resolved
        resolved=$(portable_resolve "$f")
        if [ ! -f "$resolved" ]; then
            fail "源文件不存在: $resolved"
            missing=1
        fi
    done
    [ "$missing" -ne 0 ] && exit 1

    if [ "$DESIGN_SOURCE" = "html" ]; then
        local ref_resolved
        ref_resolved=$(portable_resolve "$SOURCE_REF")
        if [ ! -f "$ref_resolved" ]; then
            fail "HTML 设计源文件不存在: $ref_resolved"
            exit 1
        fi
    else
        if [[ "$SOURCE_REF" != *"figma.com"* ]]; then
            fail "无效的 Figma URL（缺少 figma.com）"
            exit 1
        fi
        if [[ "$SOURCE_REF" != *"node-id="* ]]; then
            fail "Figma URL 缺少 node-id 参数"
            echo "  正确获取方式：在 Figma 选中目标 Frame/Component → 右键 → Copy link to selection"
            exit 1
        fi
    fi
}

# ============================================================
# 执行增量更新
# ============================================================

# 精简 deco 实时输出：保留进度 / 成败 / 错误
filter_deco_stream() {
    local line
    while IFS= read -r line || [ -n "$line" ]; do
        case "$line" in
            *"✗"*|*"⚠"*|*"诊断建议"*|*"相关命令:"*)
                printf '%s\n' "$line"
                continue
                ;;
            ""|•*|"  •"*)
                continue
                ;;
        esac
        printf '%s\n' "$line"
    done
}

extract_patch_path_from_output() {
    echo "$DECO_OUTPUT" | grep -E '增量 patch:' | head -1 | sed -E 's/.*增量 patch:[[:space:]]*//'
}

# 执行 deco 命令：实时流式输出 + 捕获输出供后续检查
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

step_update() {
    # 组装可选参数
    if $FORCE_REMOTE; then
        REMOTE_FLAG="--remote"
    else
        REMOTE_FLAG=""
    fi
    if $DOWNLOAD_ASSETS; then
        DOWNLOAD_ASSETS_FLAG="--download-assets"
    else
        DOWNLOAD_ASSETS_FLAG="--no-download-assets"
    fi
    [ -n "$IMAGE_SCALE" ] && IMAGE_SCALE_FLAG="--image-scale $IMAGE_SCALE"
    if [ -n "$COMPONENT_GROUPS" ]; then
        COMPONENT_GROUPS_FLAG="--component-groups \"$COMPONENT_GROUPS\""
    fi
    if [ -n "$PROMPT_DIR" ]; then
        PROMPT_DIR_FLAG="--prompt-dir \"$PROMPT_DIR\""
    fi
    local output_flag=""
    [ -n "$OUTPUT_PATH" ] && output_flag="-o \"$OUTPUT_PATH\""

    # 源文件列表：用 resolve 后的绝对路径，空格安全
    local files_args=""
    for f in "${SOURCE_FILES[@]}"; do
        local rf
        rf=$(portable_resolve "$f")
        files_args="$files_args \"$rf\""
    done

    # 设计源引用：html 用绝对路径，figma 用原 URL
    local source_ref_arg="$SOURCE_REF"
    [ "$DESIGN_SOURCE" = "html" ] && source_ref_arg=$(portable_resolve "$SOURCE_REF")

    inner_cmd="deco update$files_args --source-ref \"$source_ref_arg\" --design-source $DESIGN_SOURCE $output_flag --churn-threshold $CHURN_THRESHOLD $REMOTE_FLAG $DOWNLOAD_ASSETS_FLAG $IMAGE_SCALE_FLAG $COMPONENT_GROUPS_FLAG $PROMPT_DIR_FLAG"

    local mode_label="本地"
    [ -n "$REMOTE_FLAG" ] && mode_label="远程"
    [ "$DESIGN_SOURCE" = "html" ] && mode_label="—"
    info "增量更新 | 源文件数=${#SOURCE_FILES[@]} | 设计源=${DESIGN_SOURCE} | ${mode_label} | churn≥${CHURN_THRESHOLD}% | 工程=${PROJECT_DIR}"
    echo ""
}

# ============================================================
# 主流程
# ============================================================
main() {
    parse_args "$@"
    normalize_project_dir
    step_validate
    step_update

    DECO_OUTPUT=""
    DECO_EXIT_CODE=0

    run_deco_cmd "$inner_cmd"

    echo ""

    local has_success=false
    if echo "$DECO_OUTPUT" | grep -q "增量更新成功"; then
        has_success=true
    fi

    # 登录态过期检测
    local needs_login=false
    if echo "$DECO_OUTPUT" | grep -qiE "未登录|Flowly Token|token.*过期|token.*expired|token.*invalid|请先登录"; then
        needs_login=true
    fi

    if $needs_login && ! $has_success; then
        echo ""
        warn "登录态已过期，需重新登录后才能更新"
        echo "  重新登录后用相同参数再次执行本脚本即可。"
        exit 4
    elif ! $has_success && echo "$DECO_OUTPUT" | grep -qE "✗|FAILED|处理失败|update 失败"; then
        fail "增量更新失败"
        echo "  错误摘要："
        echo "$DECO_OUTPUT" | grep -E "✗|FAILED|失败|⚠" | head -10 | sed 's/^/    /'
        echo "  详见上方 deco 输出，故障排查参考: TROUBLESHOOTING.md"
        exit 1
    elif ! $has_success && [ "$DECO_EXIT_CODE" -ne 0 ]; then
        fail "增量更新异常退出（退出码 $DECO_EXIT_CODE，未匹配到已知错误模式，参见上方输出）"
        exit 1
    fi

    # 成功：提取 patch 路径并自动 git apply（先预检，失败则提示手动）
    local patch_path
    patch_path=$(extract_patch_path_from_output)
    ok "增量更新成功"
    if [ -n "$patch_path" ]; then
        ok "增量 patch: $patch_path"
        info "自动应用 patch 到工程: $PROJECT_DIR"
        set +e
        if ( cd "$PROJECT_DIR" && git apply --check "$patch_path" ) 2>/dev/null; then
            ( cd "$PROJECT_DIR" && git apply "$patch_path" )
            local apply_exit=$?
            if [ "$apply_exit" -eq 0 ]; then
                ok "已自动 git apply（patch 已落盘到 $PROJECT_DIR）"
            else
                warn "git apply 执行失败（exit $apply_exit），请手动应用："
                echo "  cd \"$PROJECT_DIR\" && git apply \"$patch_path\""
            fi
        else
            warn "git apply --check 预检未通过（可能存在冲突或上下文不匹配），未自动应用。请手动检查："
            echo "  cd \"$PROJECT_DIR\" && git apply --check \"$patch_path\""
            echo "  cd \"$PROJECT_DIR\" && git apply --3way \"$patch_path\"   # 如需三方合并"
        fi
        set -e
    else
        warn "未能从输出提取 patch 路径，请检查上方 deco 输出确认产物位置"
    fi
    exit 0
}

main "$@"
