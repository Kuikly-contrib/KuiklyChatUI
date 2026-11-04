#!/usr/bin/env bash
# ============================================================
# deco-convert.sh — Deco D2C 一键转码（跨平台通用版）
# 用法: bash deco-convert.sh <source-ref> [--project-dir <path>] [--git <repo-url>] [--dsl compose|traditional] ...
#
# 参数:
#   <source-ref>               设计源引用，支持两种形态（脚本根据后缀自动识别）：
#                                1) Figma 链接（必须包含 figma.com 与 node-id）
#                                2) 本地 HTML 文件路径（.html / .htm，支持 ~ / 相对 / 绝对路径）
#                              识别为 HTML 时，脚本自动追加 --design-source html，
#                              并跳过 Figma URL 校验与本地 Figma MCP（3845）TCP 探测
#   --project-dir <path>       用户 Kuikly 业务工程根目录（默认当前目录）
#                              Deco CLI 用 cwd 推断 Git 仓库；必须指向业务工程而非 skill 目录
#   --git <repo-url>           Git 仓库地址（可选，未指定时从 --project-dir 的 remote.origin.url 读取）
#   --dsl compose|traditional  指定 DSL 类型（可选，默认在 --project-dir 下自动推断）
#   --remote                   强制使用远程模式（可选；HTML 源默认不传 --remote，因为不依赖 Figma MCP）
#   -i, --interactive          交互模式（默认开启，无浏览器环境会自动回退）
#   --no-interactive           关闭交互模式，纯命令行直出
#   --image-scale N            图片导出倍率，正整数如 1/2/3（可选，默认 1）
#   -G, --component-groups <groups>
#                              启用非 common 的项目组件分组（逗号分隔，如 business,icon）
#
# 支持平台: macOS / Linux / Windows (WSL/Git Bash)
#
# 退出码:
#   0  转码成功
#   1  不可恢复的失败（参数错误 / 转码失败 / 异常退出等）
#   4  登录态过期，需 Agent 介入：先告知用户去浏览器登录，再独立执行 'deco login'，
#      登录成功后用相同参数重新执行本脚本。脚本本身不会内嵌 deco login，
#      因为 Agent 在等待 Shell tool 同步返回时无法向用户发出"去浏览器操作"的提醒。
#
# 注意：本脚本不做环境检查（Node.js / npm / Deco CLI），由 Agent 在调用前
#       视情况自行执行 deco-env-check.sh（与 deco-scan-components.sh 一致）。
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

# 跨平台 TCP 端口探测
portable_tcp_check() {
    local host="$1" port="$2" timeout="${3:-3}"
    if command -v nc &>/dev/null; then
        nc -z -w "$timeout" "$host" "$port" 2>/dev/null
    elif command -v bash &>/dev/null && [ -e /dev/tcp ]; then
        (echo >/dev/tcp/"$host"/"$port") 2>/dev/null
    elif command -v timeout &>/dev/null; then
        timeout "$timeout" bash -c "echo >/dev/tcp/$host/$port" 2>/dev/null
    else
        # 回退到 curl
        curl -sf --connect-timeout "$timeout" "http://$host:$port" &>/dev/null
    fi
}

# ---- 默认值 ----
SOURCE_REF=""
ORIGIN_CWD="$(pwd)"
PROJECT_DIR="$(pwd)"
GIT_URL=""
DSL_TYPE=""         # compose / traditional / 空=自动推断
FORCE_REMOTE=false
REMOTE_FLAG=""      # 初始化，避免 set -u 下 unbound variable
INTERACTIVE=true    # 交互模式（转码后打开浏览器配置页面），默认开启，可用 --no-interactive 关闭
INTERACTIVE_FLAG="" # -i / --interactive
IMAGE_SCALE=""      # 图片导出倍率（1/2/3...）
IMAGE_SCALE_FLAG="" # --image-scale N
COMPONENT_GROUPS="" # 非 common 组件分组（逗号分隔）
COMPONENT_GROUPS_FLAG="" # --component-groups ...
IS_HTML_SOURCE=false # 是否本地 HTML 源（由 .html/.htm 后缀自动识别）
HTML_PATH=""         # 规范化后的 HTML 绝对路径
DESIGN_SOURCE_FLAG="" # --design-source <type>，HTML 源时为 "--design-source html"

# ============================================================
# 解析参数
# ============================================================
parse_args() {
    while [ $# -gt 0 ]; do
        case "$1" in
            --project-dir)
                shift
                PROJECT_DIR="${1:-}"
                ;;
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
            --remote)
                FORCE_REMOTE=true
                ;;
            -i|--interactive)
                INTERACTIVE=true
                ;;
            --no-interactive)
                INTERACTIVE=false
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
# 解析 Git 仓库（Deco 默认用 process.cwd() 推断，易误用 skill 目录）
# ============================================================
step_resolve_git() {
    if [ -z "$GIT_URL" ]; then
        GIT_URL=$(git -C "$PROJECT_DIR" config --get remote.origin.url 2>/dev/null || true)
    fi

    if [[ "$PROJECT_DIR" == *"/.codebuddy/skills"* ]]; then
        warn "project-dir 落在 .codebuddy/skills 内 — Git 地址可能不是用户业务工程"
        echo "  当前: $PROJECT_DIR"
        echo "  请改用用户 Kuikly 工程根目录，或显式传 --git <repo-url>"
    fi

    if [ -n "$GIT_URL" ]; then
        : # 成功时静默，摘要见 step_convert 单行 prelude
    else
        warn "未能从 project-dir 解析 Git 仓库（Portal 可能无法关联项目）"
        echo "  project-dir: $PROJECT_DIR"
        echo "  建议显式传 --git <repo-url>"
    fi
}

# ============================================================
# 识别设计源类型（HTML 本地文件 vs Figma URL）
# 仅按后缀（.html / .htm，大小写不敏感）+ 本地存在性识别为 HTML；
# 其它情况一律走 Figma URL 校验。
# ============================================================
step_detect_source_type() {
    local lc
    lc="$(echo "$SOURCE_REF" | tr '[:upper:]' '[:lower:]')"
    case "$lc" in
        *.html|*.htm) ;;
        *) return ;;
    esac

    IS_HTML_SOURCE=true

    HTML_PATH="$SOURCE_REF"
    case "$HTML_PATH" in
        "~")    HTML_PATH="$HOME" ;;
        "~/"*)  HTML_PATH="$HOME/${HTML_PATH#~/}" ;;
    esac
    if [ "${HTML_PATH:0:1}" != "/" ]; then
        HTML_PATH="$ORIGIN_CWD/$HTML_PATH"
    fi
    if command -v realpath &>/dev/null; then
        HTML_PATH="$(realpath "$HTML_PATH" 2>/dev/null || echo "$HTML_PATH")"
    elif command -v python3 &>/dev/null; then
        HTML_PATH="$(python3 -c "import os,sys;print(os.path.abspath(sys.argv[1]))" "$HTML_PATH" 2>/dev/null || echo "$HTML_PATH")"
    fi

    if [ ! -e "$HTML_PATH" ]; then
        fail "HTML 文件不存在: $HTML_PATH"
        exit 1
    fi
    if [ -d "$HTML_PATH" ]; then
        fail "HTML 数据源不接受目录路径: $HTML_PATH"
        echo "  请直接传入 .html 或 .htm 文件路径"
        exit 1
    fi
    if [ ! -f "$HTML_PATH" ]; then
        fail "HTML 路径不是普通文件: $HTML_PATH"
        exit 1
    fi

    DESIGN_SOURCE_FLAG="--design-source html"
}

# ============================================================
# 校验 Figma URL（成功静默，仅失败时输出；HTML 源跳过）
# ============================================================
step_validate_url() {
    $IS_HTML_SOURCE && return

    if [[ "$SOURCE_REF" != *"figma.com"* ]]; then
        fail "无效的设计源引用：既不是 Figma URL（缺少 figma.com）也不是本地 .html / .htm 文件"
        echo "  - Figma URL 示例：https://www.figma.com/design/<file-key>?node-id=...&m=dev"
        echo "  - HTML 本地文件示例：./design.html、~/work/page.htm"
        exit 1
    fi

    if [[ "$SOURCE_REF" != *"node-id="* ]]; then
        fail "Figma URL 缺少 node-id 参数"
        echo "  正确获取方式：在 Figma 选中目标 Frame/Component → 右键 → Copy link to selection"
        exit 1
    fi
}

# ============================================================
# 判断目标 DSL 类型（成功静默，结果统一在执行前摘要）
# ============================================================
step_detect_dsl() {
    [ -n "$DSL_TYPE" ] && return

    local search_dir="$PROJECT_DIR"
    DSL_TYPE="traditional"
    # 用 if 包裹避免 grep 不匹配（exit 1）作为函数尾语句被 set -e 误判为失败
    if grep -rq "@Composable" "$search_dir" --include="*.kt" 2>/dev/null; then
        DSL_TYPE="compose"
    fi
}

# ============================================================
# 判断转码模式（本地/远程，成功静默）
# ============================================================
step_detect_mode() {
    if $FORCE_REMOTE; then
        REMOTE_FLAG="--remote"
        return
    fi

    # HTML 本地源不依赖 Figma Desktop / Figma MCP，跳过 3845 探测
    # 也不强制 --remote（Deco 对 html 源默认即可）
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

# ============================================================
# 执行转码
# ============================================================

# 精简 deco 实时输出：保留进度 / 成败 / 错误，隐藏配置块与 open 提示
filter_deco_stream() {
    local mode="pass"  # pass | skip
    local line

    while IFS= read -r line || [ -n "$line" ]; do
        # 错误 / 警告 / 诊断始终保留
        case "$line" in
            *"✗"*|*"⚠"*|*"诊断建议"*|*"相关命令:"*)
                printf '%s\n' "$line"
                continue
                ;;
        esac

        case "$line" in
            "🎨 Figma 转 Kuikly DSL"|"🎨 Figma 转 Kuikly Compose"|"转换配置:"|"任务信息:"|"资源统计:"|"输出文件:"|"后续操作:"|"ℹ 后续操作:")
                mode="skip"
                continue
                ;;
        esac

        if [ "$mode" = "skip" ]; then
            # 跳过配置/摘要 bullet，直到进度或结果行
            case "$line" in
                "- 正在处理"*|"ℹ ["*|"✓ ["*|"✗ "*|"✗处理失败"*|"处理失败:"*)
                    mode="pass"
                    ;;
                "✓ 生成成功"*|"✓ 生成成功！"*)
                    mode="pass"
                    ;;
                "")
                    continue
                    ;;
                •*|"  •"*)
                    continue
                    ;;
                *)
                    # 非 bullet 的非空行 → 结束 skip
                    mode="pass"
                    ;;
            esac
        fi

        if [ "$mode" = "pass" ]; then
            case "$line" in
                •*|"  •"*)
                    # 过滤 stray config bullets
                    continue
                    ;;
            esac
            printf '%s\n' "$line"
        fi
    done
}

# 从完整 deco 输出提取摘要字段（供成功收尾）
extract_deco_field() {
    local pattern="$1"
    echo "$DECO_OUTPUT" | grep -E "$pattern" | head -1 | sed -E "s/^[^:]*:[[:space:]]*//"
}

extract_kt_path_from_output() {
    local path=""
    path=$(echo "$DECO_OUTPUT" | grep -E 'Kotlin 代码:' | head -1 | sed -E 's/.*Kotlin 代码:[[:space:]]*//')
    if [ -z "$path" ]; then
        path=$(echo "$DECO_OUTPUT" | grep -oE '/[^ ]+\.kt$' | head -1)
    fi
    printf '%s' "$path"
}

print_convert_success_summary() {
    local component duration kt_path latest_dir
    component=$(extract_deco_field '组件名称')
    duration=$(extract_deco_field '处理耗时')
    kt_path=$(extract_kt_path_from_output)
    latest_dir=""
    [ -n "$kt_path" ] && latest_dir=$(dirname "$kt_path")

    ok "转码完成"

    local summary=""
    [ -n "$component" ] && summary="$component"
    summary="${summary}${summary:+, }${DSL_TYPE}"
    if [ -n "$duration" ]; then
        local dur_short
        dur_short=$(echo "$duration" | sed -nE 's/.*\(([0-9.]+s)\).*/\1/p')
        [ -z "$dur_short" ] && dur_short="$duration"
        summary="${summary:+, ${dur_short}}"
    fi
    if [ -n "$summary" ]; then
        echo "  $summary"
    fi

    if [ -n "$kt_path" ]; then
        ok "Kotlin: $kt_path"
    elif [ -n "$latest_dir" ]; then
        ok "产物目录: $latest_dir"
    fi

    # 输出 assets 路径（如果产物包含图片资源）
    if [ -n "$latest_dir" ] && [ -d "$latest_dir/assets" ]; then
        local asset_count
        asset_count=$(find "$latest_dir/assets" -type f 2>/dev/null | wc -l | tr -d ' ')
        if [ "$asset_count" -gt 0 ]; then
            ok "Assets: $latest_dir/assets ($asset_count 个文件)"
        fi
    fi
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

step_convert() {
    local cmd
    if [ "$DSL_TYPE" = "compose" ]; then
        cmd="deco to-kuikly"
    else
        cmd="deco to-kuikly-dsl"
    fi

    # 组装可选参数
    if $INTERACTIVE; then
        local browser_available=false
        case "$OS_TYPE" in
            macos)
                command -v open &>/dev/null && browser_available=true ;;
            linux)
                if [ -n "${DISPLAY:-}" ] || [ -n "${WAYLAND_DISPLAY:-}" ]; then
                    (command -v xdg-open &>/dev/null || command -v sensible-browser &>/dev/null) && browser_available=true
                fi
                ;;
            windows)
                browser_available=true ;;
        esac

        if $browser_available; then
            INTERACTIVE_FLAG="--interactive"
        else
            warn "当前环境无图形界面，已自动跳过交互模式"
            INTERACTIVE=false
        fi
    fi
    [ -n "$IMAGE_SCALE" ] && IMAGE_SCALE_FLAG="--image-scale $IMAGE_SCALE"
    if [ -n "$COMPONENT_GROUPS" ]; then
        # common 分组默认启用，CLI 会自动过滤显式传入的 common
        COMPONENT_GROUPS_FLAG="--component-groups \"$COMPONENT_GROUPS\""
    fi

    local git_flag=""
    [ -n "$GIT_URL" ] && git_flag="--git \"$GIT_URL\""

    # 选择实际传给 deco 的源引用：HTML 用规范化后的绝对路径，否则原 Figma URL
    local source_arg="$SOURCE_REF"
    $IS_HTML_SOURCE && source_arg="$HTML_PATH"

    local inner_cmd="$cmd \"$source_arg\" $REMOTE_FLAG $INTERACTIVE_FLAG $IMAGE_SCALE_FLAG $COMPONENT_GROUPS_FLAG $DESIGN_SOURCE_FLAG $git_flag"

    # 单行准备摘要
    local source_label="Figma"
    $IS_HTML_SOURCE && source_label="HTML"
    local mode_label="本地"
    [ -n "$REMOTE_FLAG" ] && mode_label="远程"
    $IS_HTML_SOURCE && mode_label="—" # HTML 源与 Figma MCP 模式无关
    local interactive_label="开启"
    $INTERACTIVE || interactive_label="关闭"
    info "转码 | 源=${source_label} | DSL=${DSL_TYPE} | ${mode_label} | 交互=${interactive_label}${IMAGE_SCALE:+ | ${IMAGE_SCALE}x}${COMPONENT_GROUPS:+ | 分组=${COMPONENT_GROUPS}} | ${PROJECT_DIR}"
    echo ""

    # 初始化全局变量
    DECO_OUTPUT=""
    DECO_EXIT_CODE=0

    # 执行 deco（在 project-dir 下，并显式传 --git）
    run_deco_cmd "$inner_cmd"

    echo ""

    # 判断转码结果
    # 策略：优先检查成功标志（✓ 生成成功），有成功标志则忽略退出码
    local has_success=false
    if echo "$DECO_OUTPUT" | grep -q "生成成功"; then
        has_success=true
    fi

    # ---- 认证/登录态过期检测 ----
    # 覆盖以下场景：
    #   1. 明确输出 "未登录" / "未登录 Flowly"
    #   2. 登录态过期导致的图片上传失败（"Flowly Token" 出现在错误提示中）
    #   3. Token 过期相关关键词
    local needs_login=false
    if echo "$DECO_OUTPUT" | grep -qiE "未登录|Flowly Token|token.*过期|token.*expired|token.*invalid|请先登录"; then
        needs_login=true
    fi

    if $needs_login && ! $has_success; then
        echo ""
        warn "登录态已过期，需重新登录后才能转码"
        echo "  重新登录后用相同参数再次执行本脚本即可。"
        exit 4
    elif ! $has_success && echo "$DECO_OUTPUT" | grep -qE "✗|FAILED|处理失败"; then
        fail "转码失败"
        echo "  错误摘要："
        echo "$DECO_OUTPUT" | grep -E "✗|FAILED|失败|⚠" | head -10 | sed 's/^/    /'
        echo "  详见上方 deco 输出，故障排查参考: TROUBLESHOOTING.md"
        exit 1
    elif ! $has_success && [ "$DECO_EXIT_CODE" -ne 0 ]; then
        fail "转码异常退出（退出码 $DECO_EXIT_CODE，未匹配到已知错误模式，参见上方输出）"
        exit 1
    fi

    print_convert_success_summary
}

# ============================================================
# 主流程
# ============================================================
main() {
    parse_args "$@"
    normalize_project_dir

    step_detect_source_type
    step_validate_url
    step_resolve_git
    step_detect_dsl
    step_detect_mode
    step_convert

    exit 0
}

main "$@"
