#!/usr/bin/env bash
# ============================================================
# deco-integrate.sh — Deco D2C 产物集成到 Kuikly 项目（跨平台通用版）
# 用法: bash deco-integrate.sh <产物目录> --project-dir <项目根目录> [--pages-dir <目标目录>] [--assets-dir <资源目录>]
#
# 参数:
#   <产物目录>                   转码产物路径（必填，从 deco-convert.sh 输出中获取）
#   --project-dir <path>        Kuikly 项目根目录（默认当前目录）
#   --pages-dir <path>          页面目标目录（绝对路径或相对项目根目录的相对路径）
#                                 1) 优先使用用户指定值
#                                 2) 未指定时在 commonMain/kotlin 下自动推断
#                                 3) 推断不出唯一结果时 exit 5 让 Agent 询问用户
#   --assets-dir <path>         图片资源目标目录（绝对路径或相对项目根目录的相对路径）
#                                 默认 <commonMain>/assets，即同 KMP 项目共享资源目录
#                                 （从 pages-dir 中提取 commonMain 段，替换 kotlin/... 为 assets）
#   --on-conflict skip|overwrite  图片资源同名冲突处理策略（仅在有冲突时需要）
#                                 skip      = 跳过同名文件（保留目标已有资源）
#                                 overwrite = 用产物覆盖（替换目标已有资源）
#
# 支持平台: macOS / Linux / Windows (WSL/Git Bash)
#
# 集成流程（脚本内一次性完成冲突预检）：
#   1) 解析 pages-dir / assets-dir
#   2) 预检产物 assets/ 与目标 commonMain/assets/ 是否同名冲突
#   3) 无冲突 → 直接写入 .kt + 图片
#   4) 有冲突且未传 --on-conflict → exit 6（此时尚未写入任何文件）
#   5) Agent 询问用户后，用相同参数 + --on-conflict skip|overwrite 再跑一次
#
# 退出码:
#   0  集成成功
#   1  不可恢复的失败（产物目录缺失 / .kt 复制失败 等）
#   5  需用户输入路径（尚未写入任何文件）：
#      reason=no_commonmain | multiple_commonmain | pages_ambiguous | no_pages_candidates
#      | assets_dir_unknown
#      输出含 ---DECO_INTEGRATE_PROBE_JSON--- 结构化块，Agent 优先解析 JSON
#   6  需用户决策图片资源同名冲突（预检阶段发现，尚未写入）
#      Agent 展示冲突清单询问后，用 --on-conflict skip|overwrite 重试
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

# 跨平台 sed -i
portable_sed_i() {
    local expression="$1"
    local file="$2"
    if [ "$OS_TYPE" = "macos" ]; then
        sed -i '' "$expression" "$file"
    else
        sed -i "$expression" "$file"
    fi
}

# ---- 默认值 ----
ARTIFACT_DIR=""
PROJECT_DIR="$(pwd)"
PAGES_DIR=""         # 空=自动探测
ASSETS_DIR=""        # 空=默认 <commonMain>/assets
RESOLVED_REL_ASSETS_DIR=""  # 解析后的资源目录（相对 PROJECT_DIR）
ON_CONFLICT=""       # 空 / skip / overwrite
# 集成摘要（收尾时输出）
INTEGRATE_ASSET_COPIED=0
INTEGRATE_ASSET_SKIPPED=0
INTEGRATE_ASSET_OVERWRITTEN=0

# 路径探测 / exit 5 结构化输出状态
PROBE_REASON=""
PROBE_PAGES_STATUS=""       # resolved | ambiguous | no_commonmain | multiple_commonmain | no_candidates
PROBE_PAGES_CANDIDATES=()
PROBE_COMMONMAIN_REL=()
PROBE_ASSETS_STATUS=""      # derived | specified | unknown
PROBE_ARTIFACT_DSL=""
ARTIFACT_DSL=""

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
            --pages-dir)
                shift
                PAGES_DIR="${1:-}"
                ;;
            --assets-dir)
                shift
                ASSETS_DIR="${1:-}"
                ;;
            --on-conflict)
                shift
                ON_CONFLICT="${1:-}"
                if [ "$ON_CONFLICT" != "skip" ] && [ "$ON_CONFLICT" != "overwrite" ]; then
                    fail "--on-conflict 必须为 skip 或 overwrite，收到: $ON_CONFLICT"
                    exit 1
                fi
                ;;
            *)
                if [ -z "$ARTIFACT_DIR" ]; then
                    ARTIFACT_DIR="$1"
                else
                    fail "未知参数: $1"
                    exit 1
                fi
                ;;
        esac
        shift
    done
}

# ============================================================
# 项目根目录规范化 + 路径探测 JSON 输出
# ============================================================
normalize_project_dir() {
    case "$PROJECT_DIR" in
        "~")    PROJECT_DIR="$HOME" ;;
        "~/"*)  PROJECT_DIR="$HOME/${PROJECT_DIR#~/}" ;;
    esac
    if [[ "$PROJECT_DIR" != /* ]]; then
        PROJECT_DIR="$(cd "$PROJECT_DIR" && pwd)"
    fi
    if [[ "$PROJECT_DIR" == *"/.codebuddy/skills"* ]]; then
        warn "project-dir 落在 .codebuddy/skills 内 — 请传用户 Kuikly 业务工程根目录"
    fi
}

json_escape() {
    local s="$1"
    s="${s//\\/\\\\}"
    s="${s//\"/\\\"}"
    s="${s//$'\n'/\\n}"
    s="${s//$'\r'/\\r}"
    s="${s//$'\t'/\\t}"
    printf '%s' "$s"
}

json_string_array() {
    local out="["
    local first=true item
    for item in "$@"; do
        if $first; then first=false; else out+=","; fi
        out+="\"$(json_escape "$item")\""
    done
    out+="]"
    printf '%s' "$out"
}

emit_integrate_probe_json() {
    local pages_dir_json="null"
    local assets_dir_json="null"
    local artifact_dir_json="null"
    [ -n "$PAGES_DIR" ] && pages_dir_json="\"$(json_escape "$PAGES_DIR")\""
    [ -n "$RESOLVED_REL_ASSETS_DIR" ] && assets_dir_json="\"$(json_escape "$RESOLVED_REL_ASSETS_DIR")\""
    [ -n "$ASSETS_DIR" ] && assets_dir_json="\"$(json_escape "$ASSETS_DIR")\""
    [ -n "$ARTIFACT_DIR" ] && [ -d "$ARTIFACT_DIR" ] && artifact_dir_json="\"$(json_escape "$ARTIFACT_DIR")\""

    echo "---DECO_INTEGRATE_PROBE_JSON---"
    printf '{'
    printf '"project_dir":"%s",' "$(json_escape "$PROJECT_DIR")"
    printf '"artifact_dir":%s,' "$artifact_dir_json"
    printf '"artifact_dsl":"%s",' "$(json_escape "${ARTIFACT_DSL:-unknown}")"
    printf '"common_main_kotlin_dirs":%s,' "$(json_string_array ${PROBE_COMMONMAIN_REL[@]+"${PROBE_COMMONMAIN_REL[@]}"})"
    printf '"pages_dir":%s,' "$pages_dir_json"
    printf '"pages_status":"%s",' "$(json_escape "${PROBE_PAGES_STATUS:-unknown}")"
    printf '"pages_candidates":%s,' "$(json_string_array ${PROBE_PAGES_CANDIDATES[@]+"${PROBE_PAGES_CANDIDATES[@]}"})"
    printf '"reason":"%s",' "$(json_escape "${PROBE_REASON:-}")"
    printf '"assets_dir":%s,' "$assets_dir_json"
    printf '"assets_status":"%s"' "$(json_escape "${PROBE_ASSETS_STATUS:-unknown}")"
    printf '}\n'
    echo "---END---"
}

emit_integrate_conflict_json() {
  local -a samples=()
  local i=0 c
  for c in "${ASSET_CONFLICT_FILES[@]}"; do
    samples+=("$c")
    i=$((i + 1))
    [ "$i" -ge 3 ] && break
  done

  echo "---DECO_INTEGRATE_CONFLICT_JSON---"
  printf '{'
  printf '"conflict_count":%d,' "${#ASSET_CONFLICT_FILES[@]}"
  printf '"target_dir":"%s",' "$(json_escape "${RESOLVED_REL_ASSETS_DIR:-}")"
  printf '"artifact_dir":"%s",' "$(json_escape "${ARTIFACT_DIR:-}")"
  printf '"samples":%s' "$(json_string_array ${samples[@]+"${samples[@]}"})"
  printf '}\n'
  echo "---END---"
}

# 页面目录候选 denylist：目录名段精确匹配（终端为 pages/page/demo/screen/screens 时豁免）
is_denied_pages_rel() {
    local rel="$1"
    [ -z "$rel" ] || [ "$rel" = "." ] && return 1

    local basename="${rel##*/}"
    case "$basename" in
        pages|page|demo|screen|screens) return 1 ;;
    esac
    case "$basename" in
        components|component|widgets|widget|utils|util|base|internal|api|model|models|viewmodel|viewmodels|theme|di|ext|extension|extensions|foundation|core|shared) return 0 ;;
    esac

    local seg
    local IFS='/'
    local -a parts=($rel)
    for seg in "${parts[@]}"; do
        case "$seg" in
            components|component|widgets|widget|utils|util|base|internal|api|model|models|viewmodel|viewmodels|theme|di|ext|extension|extensions|foundation|core|shared) return 0 ;;
        esac
    done
    return 1
}

# ============================================================
# 推断产物 DSL 类型（用于在 commonMain 内寻找同类型现有代码）
# ============================================================
detect_artifact_dsl() {
    if [ -z "$ARTIFACT_DIR" ] || [ ! -d "$ARTIFACT_DIR" ]; then
        echo "unknown"
        return 0
    fi
    if grep -lE "@Composable" "$ARTIFACT_DIR"/*.kt 2>/dev/null | head -1 | grep -q .; then
        echo "compose"
    elif grep -lE "attr \{|event \{" "$ARTIFACT_DIR"/*.kt 2>/dev/null | head -1 | grep -q .; then
        echo "traditional"
    else
        echo "unknown"
    fi
}

# 把 commonMain/kotlin 内的若干 .kt 文件路径归约成"它们的最近公共父目录（相对项目根）"
# stdin: 一行一个文件绝对路径
common_parent_relto_project() {
    local cm_kotlin="$1"
    local first=""
    local common=""

    while IFS= read -r f; do
        local rel="${f#$cm_kotlin/}"           # 例如 <pkg>/pages/Foo.kt
        rel=$(dirname "$rel")                   # 例如 <pkg>/pages
        if [ -z "$first" ]; then
            first="$rel"
            common="$rel"
        else
            # 计算 common 与 rel 的公共前缀（按 / 分段）
            local out=""
            local IFS='/'
            local -a a=($common)
            local -a b=($rel)
            local i=0
            while [ $i -lt ${#a[@]} ] && [ $i -lt ${#b[@]} ] && [ "${a[$i]}" = "${b[$i]}" ]; do
                if [ -z "$out" ]; then
                    out="${a[$i]}"
                else
                    out="$out/${a[$i]}"
                fi
                i=$((i+1))
            done
            common="$out"
        fi
    done

    if [ -z "$common" ]; then
        return 1
    fi
    echo "${cm_kotlin#$PROJECT_DIR/}/$common"
}

find_dsl_kt_files_in_commonmain() {
    local cm_kotlin="$1"
    local dsl="$ARTIFACT_DSL"
    if [ "$dsl" = "compose" ]; then
        grep -rlE "@Composable" "$cm_kotlin" --include="*.kt" 2>/dev/null || true
    elif [ "$dsl" = "traditional" ]; then
        grep -rlE "attr \{|event \{" "$cm_kotlin" --include="*.kt" 2>/dev/null || true
    else
        grep -rlE "@Composable|attr \{|event \{" "$cm_kotlin" --include="*.kt" 2>/dev/null || true
    fi
}

filter_kt_files_not_denied() {
    local cm_kotlin="$1"
    while IFS= read -r f; do
        [ -z "$f" ] && continue
        local rel dir
        rel="${f#$cm_kotlin/}"
        dir=$(dirname "$rel")
        if ! is_denied_pages_rel "$dir"; then
            echo "$f"
        fi
    done
}

find_named_pages_dirs_in_commonmain() {
    local cm_kotlin="$1"
    find "$cm_kotlin" -type d \( -name pages -o -name page -o -name demo -o -name screen -o -name screens \) 2>/dev/null || true
}

# 候选目录去重（绝对路径）
dedupe_abs_dirs() {
    printf '%s\n' "$@" | sort -u
}

# 多候选均为嵌套关系时，取最浅目录
pick_shortest_nested_candidate() {
    local -a candidates=("$@")
    local shortest="${candidates[0]}"
    local d all_nested=true
    for d in "${candidates[@]}"; do
        if [ ${#d} -lt ${#shortest} ]; then
            shortest="$d"
        fi
    done
    for d in "${candidates[@]}"; do
        case "$d" in
            "$shortest"|"$shortest"/*) ;;
            *) all_nested=false; break ;;
        esac
    done
    if $all_nested; then
        echo "$shortest"
        return 0
    fi
    return 1
}

update_probe_pages_candidates() {
    local -a abs_candidates=("$@")
    PROBE_PAGES_CANDIDATES=()
    local d
    for d in "${abs_candidates[@]}"; do
        PROBE_PAGES_CANDIDATES+=("${d#$PROJECT_DIR/}")
    done
}

# 自动探测页面目录（commonMain 优先，无法唯一推断时 exit 5）
detect_pages_dir_internal() {
    PROBE_PAGES_CANDIDATES=()
    PROBE_COMMONMAIN_REL=()
    PROBE_REASON=""
    PROBE_PAGES_STATUS=""

    local commonmains=()
    while IFS= read -r d; do
        [ -n "$d" ] && commonmains+=("$d")
    done < <(find "$PROJECT_DIR" -type d -path "*/commonMain/kotlin" 2>/dev/null)

    local d
    for d in "${commonmains[@]+"${commonmains[@]}"}"; do
        PROBE_COMMONMAIN_REL+=("${d#$PROJECT_DIR/}")
    done

    if [ ${#commonmains[@]} -eq 0 ]; then
        echo ""
        warn "未在项目下找到 commonMain/kotlin 目录"
        PROBE_REASON="no_commonmain"
        PROBE_PAGES_STATUS="no_commonmain"
        emit_integrate_probe_json
        return 5
    fi

    if [ ${#commonmains[@]} -gt 1 ]; then
        echo ""
        warn "项目下有多个 commonMain/kotlin 目录，无法自动选择："
        for d in "${commonmains[@]}"; do
            echo "    - ${d#$PROJECT_DIR/}"
        done
        PROBE_REASON="multiple_commonmain"
        PROBE_PAGES_STATUS="multiple_commonmain"
        emit_integrate_probe_json
        return 5
    fi

    local cm_kotlin="${commonmains[0]}"
    ARTIFACT_DSL=$(detect_artifact_dsl)

    local -a candidates=()
    local -a dsl_files=()
    while IFS= read -r f; do
        [ -n "$f" ] && dsl_files+=("$f")
    done < <(find_dsl_kt_files_in_commonmain "$cm_kotlin" | filter_kt_files_not_denied "$cm_kotlin")

    if [ ${#dsl_files[@]} -gt 0 ]; then
        local parent_rel
        parent_rel=$(printf '%s\n' "${dsl_files[@]}" | common_parent_relto_project "$cm_kotlin" || true)
        if [ -n "$parent_rel" ]; then
            local rel_from_cm="${parent_rel#${cm_kotlin#$PROJECT_DIR/}/}"
            if [ "$parent_rel" != "${cm_kotlin#$PROJECT_DIR/}" ] && ! is_denied_pages_rel "$rel_from_cm"; then
                candidates+=("$PROJECT_DIR/$parent_rel")
            fi
        fi
    fi

    while IFS= read -r d; do
        [ -z "$d" ] && continue
        if [ "$d" = "$cm_kotlin" ]; then
            continue
        fi
        local rel_from_cm="${d#$cm_kotlin/}"
        if ! is_denied_pages_rel "$rel_from_cm"; then
            candidates+=("$d")
        fi
    done < <(find_named_pages_dirs_in_commonmain "$cm_kotlin")

    if [ ${#candidates[@]} -gt 0 ]; then
        local -a deduped=()
        while IFS= read -r line; do
            [ -n "$line" ] && deduped+=("$line")
        done < <(dedupe_abs_dirs "${candidates[@]}")
        candidates=("${deduped[@]+"${deduped[@]}"}")
    fi

    update_probe_pages_candidates "${candidates[@]+"${candidates[@]}"}"

    if [ ${#candidates[@]} -eq 0 ]; then
        echo ""
        warn "commonMain 内未找到任何候选页面目录"
        echo "  规则匹配项："
        echo "    - 同 DSL 类型(${ARTIFACT_DSL})的现有 .kt 公共父目录（已排除 components/utils 等）: 0"
        echo "    - 命名为 pages/page/demo/screen/screens 的子目录: 0"
        PROBE_REASON="no_pages_candidates"
        PROBE_PAGES_STATUS="no_candidates"
        emit_integrate_probe_json
        return 5
    fi

    if [ ${#candidates[@]} -gt 1 ]; then
        local picked
        if picked=$(pick_shortest_nested_candidate "${candidates[@]}"); then
            PAGES_DIR="${picked#$PROJECT_DIR/}"
            PROBE_PAGES_STATUS="resolved"
            return 0
        fi

        echo ""
        warn "commonMain 内有多个候选页面目录，无法自动唯一确定："
        for d in "${candidates[@]}"; do
            echo "    - ${d#$PROJECT_DIR/}"
        done
        PROBE_REASON="pages_ambiguous"
        PROBE_PAGES_STATUS="ambiguous"
        emit_integrate_probe_json
        return 5
    fi

    PAGES_DIR="${candidates[0]#$PROJECT_DIR/}"
    PROBE_PAGES_STATUS="resolved"
    return 0
}

# ============================================================
# 校验产物目录（必须由调用方显式传入）
# ============================================================
find_artifact_dir() {
    if [ -n "$ARTIFACT_DIR" ] && [ -d "$ARTIFACT_DIR" ]; then
        return 0
    fi

    fail "未指定产物目录"
    echo "  用法: bash $(basename "$0") <产物目录> --project-dir <项目根目录>"
    return 1
}

# ============================================================
# 校验产物含 .kt 文件
# ============================================================
verify_kt_present() {
    local kt_count
    kt_count=$(find "$ARTIFACT_DIR" -maxdepth 1 -name "*.kt" -type f 2>/dev/null | wc -l | tr -d ' ')
    if [ "$kt_count" -eq 0 ]; then
        fail "未找到 .kt 文件"
        return 1
    fi
    return 0
}

# ============================================================
# 复制 .kt 文件（自动重命名 + 修复 package）
# ============================================================
copy_kt_files() {
    local target_dir="$PROJECT_DIR/$PAGES_DIR"

    if [ ! -d "$target_dir" ]; then
        mkdir -p "$target_dir"
        ok "创建目录: $target_dir"
    fi

    # 推断目标 package（从 commonMain/kotlin/ 之后的路径提取）
    local target_package=""
    if [[ "$target_dir" == *"/kotlin/"* ]]; then
        target_package=$(echo "$target_dir" | sed 's|.*/kotlin/||' | tr '/' '.')
    fi

    while IFS= read -r f; do
        # 去掉日期后缀：FileName_2026-03-16_12-43.kt → FileName.kt
        local clean_name
        clean_name=$(basename "$f" | sed -E 's/_[0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{2}-[0-9]{2}(\.kt)$/\1/')

        cp "$f" "$target_dir/$clean_name"
        local target_file="$target_dir/$clean_name"

        # 修复 package 声明（跨平台 sed -i）
        if [ -n "$target_package" ]; then
            local orig_package
            orig_package=$(grep -m1 "^package " "$target_file" 2>/dev/null | awk '{print $2}' || true)
            if [ -n "$orig_package" ] && [ "$orig_package" != "$target_package" ]; then
                portable_sed_i "s|^package ${orig_package}|package ${target_package}|" "$target_file" 2>/dev/null || true
            fi
        fi
    done < <(find "$ARTIFACT_DIR" -maxdepth 1 -name "*.kt" -type f 2>/dev/null)
}

# 从 PAGES_DIR 推导 commonMain/assets 目录（KMP 项目资源约定）
# 输入示例: <commonMain>/kotlin/<pkg>/pages
# 输出示例: <commonMain>/assets
# 失败时返回非 0
derive_commonmain_assets_dir() {
    local pd="$1"
    case "$pd" in
        */kotlin/*|*/kotlin)
            # 取 kotlin/ 之前的部分（即 commonMain 目录路径）作为 assets 父目录
            local before_kotlin="${pd%%/kotlin*}"
            echo "$before_kotlin/assets"
            return 0
            ;;
    esac
    return 1
}

# 解析资源目标目录 → RESOLVED_REL_ASSETS_DIR；失败 return 1（并设置 PROBE_ASSETS_STATUS=unknown）
resolve_assets_target_dir() {
    if [ -n "$ASSETS_DIR" ]; then
        RESOLVED_REL_ASSETS_DIR="$ASSETS_DIR"
        PROBE_ASSETS_STATUS="specified"
        return 0
    fi
    RESOLVED_REL_ASSETS_DIR=$(derive_commonmain_assets_dir "$PAGES_DIR" || true)
    if [ -z "$RESOLVED_REL_ASSETS_DIR" ]; then
        PROBE_ASSETS_STATUS="unknown"
        return 1
    fi
    PROBE_ASSETS_STATUS="derived"
    return 0
}

report_exit5_assets() {
    echo ""
    warn "无法从 pages-dir 推导 commonMain/assets 目录（pages-dir 须位于 commonMain/kotlin/ 下）"
    echo "    pages-dir = $PAGES_DIR"
    PROBE_REASON="assets_dir_unknown"
    emit_integrate_probe_json
}

# 产物 assets/ 下的相对路径（保留 common/ 等子目录结构）
# 例: $ARTIFACT_DIR/assets/common/foo.png → common/foo.png
asset_rel_path() {
    local f="$1"
    echo "${f#"$ARTIFACT_DIR/assets/"}"
}

# 扫描产物 assets/ 与目标目录的同名冲突（按相对路径匹配，保留 common/ 子目录）
ASSET_CONFLICT_FILES=()
scan_asset_conflicts() {
    ASSET_CONFLICT_FILES=()
    [ -d "$ARTIFACT_DIR/assets" ] || return 0

    local target="$PROJECT_DIR/$RESOLVED_REL_ASSETS_DIR"
    local f rel
    while IFS= read -r f; do
        rel=$(asset_rel_path "$f")
        if [ -e "$target/$rel" ]; then
            ASSET_CONFLICT_FILES+=("$rel")
        fi
    done < <(find "$ARTIFACT_DIR/assets" -type f 2>/dev/null)
}

# 写入前预检：有冲突且未指定策略 → exit 6（不复制 .kt / 图片）
pre_check_asset_conflicts() {
    # 无 assets 或空目录 → 无需预检
    if [ ! -d "$ARTIFACT_DIR/assets" ]; then
        return 0
    fi
    local asset_count
    asset_count=$(find "$ARTIFACT_DIR/assets" -type f 2>/dev/null | wc -l | tr -d ' ')
    [ "$asset_count" -eq 0 ] && return 0

    resolve_assets_target_dir || { report_exit5_assets; return 5; }

    scan_asset_conflicts

    if [ ${#ASSET_CONFLICT_FILES[@]} -eq 0 ]; then
        return 0
    fi

    # 已指定策略 → 预检通过，后续 copy_assets 按策略处理
    if [ -n "$ON_CONFLICT" ]; then
        return 0
    fi

    # 有冲突、未指定策略 → 停下询问（尚未写入任何文件）
    echo ""
    warn "图片资源同名冲突 ${#ASSET_CONFLICT_FILES[@]} 个 — 已暂停集成，尚未写入文件"
    echo "  目标: $RESOLVED_REL_ASSETS_DIR"
    local c shown=0
    for c in "${ASSET_CONFLICT_FILES[@]}"; do
        echo "    - $c"
        shown=$((shown + 1))
        [ "$shown" -ge 3 ] && break
    done
    [ "${#ASSET_CONFLICT_FILES[@]}" -gt 3 ] && echo "    … 等 ${#ASSET_CONFLICT_FILES[@]} 个"
    emit_integrate_conflict_json
    return 6
}

# ============================================================
# 复制图片资源（调用前须已通过 pre_check_asset_conflicts）
# ============================================================
copy_assets() {
    if [ ! -d "$ARTIFACT_DIR/assets" ]; then
        return 0
    fi

    local asset_count
    asset_count=$(find "$ARTIFACT_DIR/assets" -type f 2>/dev/null | wc -l | tr -d ' ')

    if [ "$asset_count" -eq 0 ]; then
        return 0
    fi

    if [ -z "$RESOLVED_REL_ASSETS_DIR" ]; then
        resolve_assets_target_dir || return 1
    fi
    local rel_assets_dir="$RESOLVED_REL_ASSETS_DIR"
    local target_assets_dir="$PROJECT_DIR/$rel_assets_dir"

    # 创建目标目录
    if [ ! -d "$target_assets_dir" ]; then
        mkdir -p "$target_assets_dir"
        ok "创建目录: $rel_assets_dir"
    fi

    # 实际拷贝（保留 assets/ 下相对路径，如 assets/common/foo.png → commonMain/assets/common/foo.png）
    local copied=0
    local skipped=0
    local overwritten=0
    while IFS= read -r f; do
        local rel target_file
        rel=$(asset_rel_path "$f")
        target_file="$target_assets_dir/$rel"

        if [ -e "$target_file" ]; then
            case "$ON_CONFLICT" in
                skip)
                    skipped=$((skipped + 1))
                    ;;
                overwrite)
                    mkdir -p "$(dirname "$target_file")"
                    cp -fp "$f" "$target_file"
                    overwritten=$((overwritten + 1))
                    ;;
            esac
        else
            mkdir -p "$(dirname "$target_file")"
            cp -p "$f" "$target_file"
            copied=$((copied + 1))
        fi
    done < <(find "$ARTIFACT_DIR/assets" -type f 2>/dev/null)

    INTEGRATE_ASSET_COPIED=$copied
    INTEGRATE_ASSET_SKIPPED=$skipped
    INTEGRATE_ASSET_OVERWRITTEN=$overwritten
}

# ============================================================
# 提示路由注册
# ============================================================
# 精简模式收尾摘要
collect_route_lines() {
    local lines=""
    while IFS= read -r f; do
        local basename_f display_name page_name classname line
        basename_f=$(basename "$f")
        display_name=$(echo "$basename_f" | sed -E 's/_[0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{2}-[0-9]{2}(\.kt)$/\1/')
        page_name=$(grep -o '@Page("[^"]*")' "$f" 2>/dev/null | sed 's/@Page("//;s/")//' | head -1 || true)
        if [ -n "$page_name" ]; then
            line="  $display_name → 路由 \"$page_name\" (@Page 自动注册)"
        else
            classname=$(grep -o 'class [A-Za-z0-9_]*' "$f" 2>/dev/null | head -1 | awk '{print $2}' || true)
            if [ -n "$classname" ]; then
                line="  $display_name → 类 $classname (需手动注册路由)"
            else
                line="  $display_name"
            fi
        fi
        lines+="${line}"$'\n'
    done < <(find "$ARTIFACT_DIR" -maxdepth 1 -name "*.kt" -type f 2>/dev/null)
    printf '%s' "$lines"
}

print_integrate_summary() {
    local kt_count asset_total route_lines
    kt_count=$(find "$ARTIFACT_DIR" -maxdepth 1 -name "*.kt" -type f 2>/dev/null | wc -l | tr -d ' ')
    asset_total=$((INTEGRATE_ASSET_COPIED + INTEGRATE_ASSET_SKIPPED + INTEGRATE_ASSET_OVERWRITTEN))
    [ "$asset_total" -eq 0 ] && asset_total=$(find "$ARTIFACT_DIR/assets" -type f 2>/dev/null | wc -l | tr -d ' ')

    ok "集成完成"
    echo "  页面: $PAGES_DIR"
    if [ -n "$RESOLVED_REL_ASSETS_DIR" ] && [ "$asset_total" -gt 0 ]; then
        local asset_note="${INTEGRATE_ASSET_COPIED} 新文件"
        [ "$INTEGRATE_ASSET_SKIPPED" -gt 0 ] && asset_note+=", ${INTEGRATE_ASSET_SKIPPED} 跳过"
        [ "$INTEGRATE_ASSET_OVERWRITTEN" -gt 0 ] && asset_note+=", ${INTEGRATE_ASSET_OVERWRITTEN} 覆盖"
        echo "  资源: $RESOLVED_REL_ASSETS_DIR (${asset_note})"
    fi
    echo "  Kotlin: ${kt_count} 个"
    route_lines=$(collect_route_lines)
    if [ -n "$route_lines" ]; then
        printf '%s' "$route_lines"
    fi
}

# ============================================================
# 路径参数规范化（pages-dir / assets-dir）
# ============================================================
normalize_pages_dir_arg() {
    case "$PAGES_DIR" in
        "~")    PAGES_DIR="$HOME" ;;
        "~/"*)  PAGES_DIR="$HOME/${PAGES_DIR#~/}" ;;
    esac
    if [[ "$PAGES_DIR" = /* ]]; then
        if [[ "$PAGES_DIR" != "$PROJECT_DIR"/* ]] && [ "$PAGES_DIR" != "$PROJECT_DIR" ]; then
            fail "--pages-dir 绝对路径必须在 --project-dir 之下：$PAGES_DIR"
            echo "  --project-dir = $PROJECT_DIR"
            exit 1
        fi
        PAGES_DIR="${PAGES_DIR#$PROJECT_DIR/}"
    fi
}

normalize_assets_dir_arg() {
    case "$ASSETS_DIR" in
        "~")    ASSETS_DIR="$HOME" ;;
        "~/"*)  ASSETS_DIR="$HOME/${ASSETS_DIR#~/}" ;;
    esac
    if [[ "$ASSETS_DIR" = /* ]]; then
        if [[ "$ASSETS_DIR" != "$PROJECT_DIR"/* ]] && [ "$ASSETS_DIR" != "$PROJECT_DIR" ]; then
            fail "--assets-dir 绝对路径必须在 --project-dir 之下：$ASSETS_DIR"
            echo "  --project-dir = $PROJECT_DIR"
            exit 1
        fi
        ASSETS_DIR="${ASSETS_DIR#$PROJECT_DIR/}"
    fi
}

# ============================================================
# 主流程
# ============================================================
main() {
    parse_args "$@"

    normalize_project_dir

    info "集成 | ${PROJECT_DIR}"

    find_artifact_dir || exit 1

    if [ -n "$PAGES_DIR" ]; then
        normalize_pages_dir_arg
        PROBE_PAGES_STATUS="resolved"
    else
        set +e
        detect_pages_dir_internal
        local detect_rc=$?
        set -e
        case $detect_rc in
            0) ;;
            5) exit 5 ;;
            *) exit 1 ;;
        esac
    fi

    if [ -n "$ASSETS_DIR" ]; then
        normalize_assets_dir_arg
    fi

    verify_kt_present || exit 1

    set +e
    pre_check_asset_conflicts
    local pre_rc=$?
    set -e
    case $pre_rc in
        0) ;;
        5) exit 5 ;;
        6) exit 6 ;;
        *) exit 1 ;;
    esac

    copy_kt_files
    copy_assets || exit 1

    echo ""
    print_integrate_summary
}

main "$@"
