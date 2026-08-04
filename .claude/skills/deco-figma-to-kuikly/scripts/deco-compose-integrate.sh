#!/usr/bin/env bash
# ============================================================
# deco-compose-integrate.sh — 标准 Compose Multiplatform 转码产物集成（跨平台通用版）
# 用法: bash deco-compose-integrate.sh <产物目录> --project-dir <path> [--pages-dir <path>] [--assets-dir <path>] [--on-conflict skip|overwrite]
#
# 与 deco-integrate.sh（Kuikly 专用）的区别：
#   deco-integrate.sh  集成 Kuikly DSL / Kuikly Compose 产物（包名 com.tencent.kuikly.*），
#                      落盘到 Kuikly 工程的 shared/src/commonMain/kotlin/.../pages/...
#   deco-compose-integrate.sh 集成 **标准 Compose Multiplatform** 产物（包名固定 com.example.generated，
#                      androidx.compose.*），落盘到 CMP 工程的 <commonMain>/kotlin/com/example/generated/
#                      与 <commonMain>/assets/
#
# CMP 工程 commonMain 根约定（两种都支持，按探测优先级）：
#   1) src/commonMain        （Android Studio / KMP 向导标准布局）
#   2) shared/commonMain     （部分多模块布局）
#
# 资源目录约定（来自 KuiklyUI quick-start）：
#   <commonMain>/assets      （build.gradle.kts: assets.srcDirs("src/commonMain/assets")）
#
# 行为：直接实跑（不预演），写入前预检文件/资源冲突。
#   exit 0  集成成功
#   exit 5  检测到多个 commonMain 候选目录，需用户决策（输出候选 JSON 到 stdout）
#   exit 6  文件/资源冲突（未写入任何文件）；指定 --on-conflict 后实跑覆盖/跳过
#
# 注意：本脚本不调用 deco，也不做环境检查，由 Agent 在调用前视情况执行 deco-env-check.sh。
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
    RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
else
    RED='' GREEN='' YELLOW='' CYAN='' NC=''
fi
ok()   { echo -e "${GREEN}✔${NC} $*"; }
warn() { echo -e "${YELLOW}⚠${NC} $*"; }
fail() { echo -e "${RED}✗${NC} $*"; }
info() { echo -e "${CYAN}ℹ${NC} $*"; }

# ---- 跨平台工具 ----
portable_mktemp() {
    if [ "$OS_TYPE" = "macos" ]; then mktemp /tmp/deco-cmp-int.XXXXXX; else mktemp --tmpdir deco-cmp-int.XXXXXX; fi
}

# ---- 默认值 ----
PRODUCT_DIR=""
PROJECT_DIR=""
PAGES_DIR_OVERRIDE=""
ASSETS_DIR_OVERRIDE=""
ON_CONFLICT=""

# ============================================================
# 参数解析
# ============================================================
parse_args() {
    while [ $# -gt 0 ]; do
        case "$1" in
            --project-dir) shift; PROJECT_DIR="${1:-}" ;;
            --pages-dir)   shift; PAGES_DIR_OVERRIDE="${1:-}" ;;
            --assets-dir)  shift; ASSETS_DIR_OVERRIDE="${1:-}" ;;
            --on-conflict) shift; ON_CONFLICT="${1:-}" ;;
            *)
                if [ -z "$PRODUCT_DIR" ]; then PRODUCT_DIR="$1"; else fail "未知参数: $1"; exit 1; fi
                ;;
        esac
        shift
    done
    if [ -z "$PRODUCT_DIR" ]; then fail "缺少产物目录参数"; exit 1; fi
    if [ -z "$PROJECT_DIR" ]; then fail "缺少 --project-dir 参数"; exit 1; fi
    case "$ON_CONFLICT" in
        ""|skip|overwrite) ;;
        *) fail "--on-conflict 仅支持 skip | overwrite"; exit 1 ;;
    esac
}

# ============================================================
# 规范化路径
# ============================================================
normalize() {
    case "$PROJECT_DIR" in
        "~") PROJECT_DIR="$HOME" ;;
        "~/"*) PROJECT_DIR="$HOME/${PROJECT_DIR#~/}" ;;
    esac
    if [ "${PROJECT_DIR:0:1}" != "/" ]; then PROJECT_DIR="$(cd "$(pwd)" && pwd)/$PROJECT_DIR"; fi
    if [ ! -d "$PROJECT_DIR" ]; then fail "工程目录不存在: $PROJECT_DIR"; exit 1; fi

    case "$PRODUCT_DIR" in
        "~") PRODUCT_DIR="$HOME" ;;
        "~/"*) PRODUCT_DIR="$HOME/${PRODUCT_DIR#~/}" ;;
    esac
    if [ "${PRODUCT_DIR:0:1}" != "/" ]; then PRODUCT_DIR="$(cd "$(pwd)" && pwd)/$PRODUCT_DIR"; fi
    if [ ! -d "$PRODUCT_DIR" ]; then fail "产物目录不存在: $PRODUCT_DIR"; exit 1; fi
}

# ============================================================
# 探测 CMP 工程的 commonMain 根
# 优先级：src/commonMain > shared/commonMain
# ============================================================
detect_common_main() {
    local candidates=()
    # 用 find 找所有 commonMain/kotlin 目录（避免依赖特定前缀）
    local found
    found=$(find "$PROJECT_DIR" -type d -path '*/commonMain/kotlin' 2>/dev/null | grep -vE '/build/|/\.gradle/|/node_modules/' || true)
    if [ -n "$found" ]; then
        while IFS= read -r d; do
            # 取 commonMain 根（去掉末尾的 /kotlin）
            candidates+=("${d%/kotlin}")
        done <<< "$found"
    fi

    # 去重
    local uniq=()
    local seen=""
    for c in "${candidates[@]:-}"; do
        [ -z "$c" ] && continue
        if [[ "$seen" != *"|$c|"* ]]; then
            uniq+=("$c"); seen="$seen|$c|"
        fi
    done

    if [ ${#uniq[@]} -eq 0 ]; then
        fail "未在工程中找到 CMP commonMain 目录（期望 */commonMain/kotlin）。请确认这是标准 Compose Multiplatform 工程，或显式传 --pages-dir"
        exit 1
    fi

    if [ ${#uniq[@]} -gt 1 ]; then
        # 多候选 → exit 5 让用户决策
        local json="{\"candidates\":["
        local first=true
        for c in "${uniq[@]}"; do
            $first || json="$json,"
            json="$json{\"path\":\"$c\"}"
            first=false
        done
        json="$json],\"message\":\"检测到多个 commonMain 候选目录，请指定 --pages-dir 选择其一\"}"
        echo "$json"
        exit 5
    fi

    COMMON_MAIN="${uniq[0]}"
}

# ============================================================
# 主流程
# ============================================================
main() {
    parse_args "$@"
    normalize

    # 收集产物 .kt 与 assets
    local kt_files=()
    while IFS= read -r f; do
        [ -n "$f" ] && kt_files+=("$f")
    done < <(find "$PRODUCT_DIR" -maxdepth 1 -type f -name '*.kt' 2>/dev/null)

    if [ ${#kt_files[@]} -eq 0 ]; then
        fail "产物目录中未发现 .kt 文件: $PRODUCT_DIR"
        exit 1
    fi

    # 确定目标目录
    local pages_dir
    if [ -n "$PAGES_DIR_OVERRIDE" ]; then
        pages_dir="$PAGES_DIR_OVERRIDE"
        COMMON_MAIN="$(dirname "$pages_dir")"
    else
        detect_common_main
        pages_dir="$COMMON_MAIN/kotlin/com/example/generated"
    fi

    local assets_dir
    if [ -n "$ASSETS_DIR_OVERRIDE" ]; then
        assets_dir="$ASSETS_DIR_OVERRIDE"
    else
        # 标准 CMP 资源目录：composeResources/drawable（Res.drawable.* 约定）优先，
        # 回退 Kuikly Compose 的 assets/ 约定
        if [ -d "$COMMON_MAIN/composeResources/drawable" ]; then
            assets_dir="$COMMON_MAIN/composeResources/drawable"
        elif [ -d "$COMMON_MAIN/assets" ]; then
            assets_dir="$COMMON_MAIN/assets"
        else
            # 都不存在则按标准 CMP 约定创建
            assets_dir="$COMMON_MAIN/composeResources/drawable"
        fi
    fi

    mkdir -p "$pages_dir"

    # 冲突预检
    local conflict=0
    local conflict_list=""
    for f in "${kt_files[@]}"; do
        local base
        base="$(basename "$f")"
        if [ -e "$pages_dir/$base" ]; then
            conflict=1
            conflict_list="$conflict_list $base"
        fi
    done

    # 资源冲突预检
    local product_assets="$PRODUCT_DIR/assets"
    if [ -d "$product_assets" ]; then
        while IFS= read -r af; do
            [ -z "$af" ] && continue
            local rel="${af#$product_assets/}"
            if [ -e "$assets_dir/$rel" ]; then
                conflict=1
                conflict_list="$conflict_list assets/$rel"
            fi
        done < <(find "$product_assets" -type f 2>/dev/null)
    fi

    if [ "$conflict" -eq 1 ]; then
        if [ "$ON_CONFLICT" = "skip" ]; then
            warn "存在冲突文件，按 --on-conflict skip 跳过已存在的: $conflict_list"
        elif [ "$ON_CONFLICT" = "overwrite" ]; then
            warn "存在冲突文件，按 --on-conflict overwrite 覆盖: $conflict_list"
        else
            fail "检测到文件/资源冲突（未写入）: $conflict_list"
            echo "  用 --on-conflict skip 跳过冲突文件，或 --on-conflict overwrite 覆盖"
            exit 6
        fi
    fi

    # 实跑写入 .kt
    local written=0
    for f in "${kt_files[@]}"; do
        local base
        base="$(basename "$f")"
        if [ -e "$pages_dir/$base" ] && [ "$ON_CONFLICT" = "skip" ]; then
            info "跳过已存在: $pages_dir/$base"
            continue
        fi
        cp "$f" "$pages_dir/$base"
        ok "写入: $pages_dir/$base"
        written=$((written+1))
    done

    # 实跑写入 assets
    if [ -d "$product_assets" ]; then
        mkdir -p "$assets_dir"
        while IFS= read -r af; do
            [ -z "$af" ] && continue
            local rel="${af#$product_assets/}"
            local dest="$assets_dir/$rel"
            mkdir -p "$(dirname "$dest")"
            if [ -e "$dest" ] && [ "$ON_CONFLICT" = "skip" ]; then
                info "跳过已存在资源: $dest"
                continue
            fi
            cp "$af" "$dest"
            ok "写入资源: $dest"
        done < <(find "$product_assets" -type f 2>/dev/null)
    fi

    echo ""
    ok "CMP 集成完成：$written 个 .kt 已写入 $pages_dir"
    info "资源目录: $assets_dir"
    echo "  下一步：在 CMP 入口处调用 com.example.generated 中的 @Composable 函数以预览/使用页面"
    exit 0
}

main "$@"
