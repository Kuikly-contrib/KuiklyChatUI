#!/usr/bin/env bash

set -euo pipefail

SHELL_DIR=$(
  cd "$(dirname "$0")" || exit 1
  pwd
)

# 脚本本身在项目根目录
PROJECT_DIR="${SHELL_DIR}"

GRADLEW="./gradlew"

# ====== 默认配置 ======

# 基础版本号（从 gradle.properties 读取，可通过 -v 参数覆盖）
BASE_VERSION=""

# 是否 SNAPSHOT
PUB_ENABLE_SNAPSHOT=""

# 是否本地 repo
PUB_IS_LOCAL_REPO=""

# 是否需要 clean
NEED_CLEAN=""

# 是否 debug
DEBUG=""

# Group ID（从 gradle.properties 读取，可通过 -g 参数覆盖）
GROUP_ID=""

# Maven 仓库地址（从 gradle.properties 读取，可通过 -m 参数或环境变量 mavenUrl 覆盖）
MAVEN_URL="${mavenUrl:-}"

# Maven 认证（从 gradle.properties 读取，也可通过环境变量传入）
MAVEN_USERNAME="${mavenUserName:-}"
MAVEN_PASSWORD="${mavenPassword:-}"

# Kotlin 版本列表（从 gradle.properties 读取，可通过 -k 参数覆盖）
KOTLIN_VERSION_LIST=""
# 支持 ohos 的 Kotlin 版本列表
OHOS_KOTLIN_VERSION_LIST=""

# 默认 Kuikly Core 版本
DEFAULT_KUIKLY_CORE_VERSION="2.7.0"

# publish 任务
PUBLISH_TASK=publishAllPublicationsToMavenRepository

# ====== 组件模块配置（从 gradle.properties 的 MODULE_NAME 读取） ======
MODULE_KMP=""

# 构建开始时间
BUILD_START_TIME=0

# ====== 从 gradle.properties 读取默认配置 ======

GRADLE_PROPS="${PROJECT_DIR}/gradle.properties"
if [ -f "${GRADLE_PROPS}" ]; then
  PROP_VERSION=$(grep '^MAVEN_VERSION=' "${GRADLE_PROPS}" 2>/dev/null | cut -d'=' -f2 || true)
  if [ -n "${PROP_VERSION}" ]; then
    BASE_VERSION="${PROP_VERSION}"
  fi

  PROP_KOTLIN=$(grep '^KOTLIN_VERSION_LIST=' "${GRADLE_PROPS}" 2>/dev/null | cut -d'=' -f2 || true)
  if [ -n "${PROP_KOTLIN}" ]; then
    KOTLIN_VERSION_LIST="${PROP_KOTLIN}"
  fi

  PROP_OHOS_KOTLIN=$(grep '^OHOS_KOTLIN_VERSION_LIST=' "${GRADLE_PROPS}" 2>/dev/null | cut -d'=' -f2 || true)
  if [ -n "${PROP_OHOS_KOTLIN}" ]; then
    OHOS_KOTLIN_VERSION_LIST="${PROP_OHOS_KOTLIN}"
  fi

  PROP_GROUP_ID=$(grep '^GROUP_ID=' "${GRADLE_PROPS}" 2>/dev/null | cut -d'=' -f2 || true)
  if [ -n "${PROP_GROUP_ID}" ] && [ -z "${GROUP_ID}" ]; then
    GROUP_ID="${PROP_GROUP_ID}"
  fi

  if [ -z "${MAVEN_URL}" ]; then
    PROP_MAVEN_URL=$(grep '^MAVEN_REPO_URL=' "${GRADLE_PROPS}" 2>/dev/null | cut -d'=' -f2 || true)
    if [ -n "${PROP_MAVEN_URL}" ]; then
      MAVEN_URL="${PROP_MAVEN_URL}"
    fi
  fi

  if [ -z "${MAVEN_USERNAME}" ]; then
    PROP_MAVEN_USER=$(grep '^MAVEN_USERNAME=' "${GRADLE_PROPS}" 2>/dev/null | cut -d'=' -f2 || true)
    if [ -n "${PROP_MAVEN_USER}" ]; then
      MAVEN_USERNAME="${PROP_MAVEN_USER}"
    fi
  fi

  if [ -z "${MAVEN_PASSWORD}" ]; then
    PROP_MAVEN_PASS=$(grep '^MAVEN_PASSWORD=' "${GRADLE_PROPS}" 2>/dev/null | cut -d'=' -f2 || true)
    if [ -n "${PROP_MAVEN_PASS}" ]; then
      MAVEN_PASSWORD="${PROP_MAVEN_PASS}"
    fi
  fi

  if [ -z "${MODULE_KMP}" ]; then
    PROP_MODULE_NAME=$(grep '^MODULE_NAME=' "${GRADLE_PROPS}" 2>/dev/null | cut -d'=' -f2 || true)
    if [ -n "${PROP_MODULE_NAME}" ]; then
      MODULE_KMP="${PROP_MODULE_NAME}"
    fi
  fi
fi

# ====== 函数定义 ======

function usage() {
  echo ""
  echo " 发布 ${MODULE_KMP:-组件} 产物到 Maven 仓库"
  echo ""
  echo " Usage: publish-maven.sh [option] <value>"
  echo ""
  echo " Options:"
  echo "  -n, --module-name   指定组件模块名, 默认读取 gradle.properties 中 MODULE_NAME"
  echo "  -v, --version       指定发布版本号, 默认读取 gradle.properties 中 MAVEN_VERSION"
  echo "  -g, --group-id      指定 Group ID, 默认读取 gradle.properties 中 GROUP_ID"
  echo "  -k, --kotlin        指定编译的 Kotlin 版本列表, 使用 , 分割"
  echo "  -ok, --ohos-kotlin  指定鸿蒙 Kotlin 版本列表, 使用 , 分割"
  echo "  -s, --snapshot      指定是否 SNAPSHOT (true/false)"
  echo "  -l, --local-repo    指定是否发布到本地 repo (true/false)"
  echo "  -m, --maven-url     指定 Maven 仓库地址"
  echo "  -c, --clean         指定是否需要 clean 后编译 (true/false)"
  echo "  -d, --debug         是否开启 Gradle debug 模式 (true/false)"
  echo "  -h, --help          查看帮助"
  echo ""
}

function log_info() {
  echo "> [INFO ] $*"
}

function log_warn() {
  echo "> [WARN ] $*"
}

function log_error() {
  echo "> [ERROR] $*"
}

function get_kuikly_core_version() {
  local kotlin_version="$1"
  echo "${DEFAULT_KUIKLY_CORE_VERSION}-${kotlin_version}"
}

function start_timer() {
  BUILD_START_TIME=$(date +%s)
}

function print_duration() {
  local end_time
  end_time=$(date +%s)
  local duration=$((end_time - BUILD_START_TIME))
  local minutes=$((duration / 60))
  local seconds=$((duration % 60))
  log_info "总耗时: ${minutes}分${seconds}秒"
}

function check() {
  cd "${PROJECT_DIR}" || return 1

  if [ ! -f "${GRADLEW}" ]; then
    log_error "gradlew not found at ${PROJECT_DIR}/${GRADLEW}"
    return 1
  fi

  chmod +x "${GRADLEW}"

  if [ "${NEED_CLEAN}" = 'true' ]; then
    log_info "执行 gradle clean..."
    if ! $GRADLEW clean; then
      log_error "gradle clean 失败!"
      return 1
    fi
  else
    log_warn "跳过 gradle clean"
  fi
}

function build_gradle_args() {
  local args=""
  if [[ -n "${PUB_ENABLE_SNAPSHOT}" ]]; then
    args="${args} -PPUB_ENABLE_SNAPSHOT=${PUB_ENABLE_SNAPSHOT}"
  fi
  if [[ -n "${PUB_IS_LOCAL_REPO}" ]]; then
    args="${args} -PPUB_IS_LOCAL_REPO=${PUB_IS_LOCAL_REPO}"
  fi
  if [[ -n "${GROUP_ID}" ]]; then
    args="${args} -PgroupId=${GROUP_ID}"
  fi
  if [[ -n "${MAVEN_URL}" ]]; then
    args="${args} -PmavenRepoUrl=${MAVEN_URL}"
  fi
  if [[ -n "${MAVEN_USERNAME}" ]]; then
    args="${args} -PmavenUsername=${MAVEN_USERNAME}"
  fi
  if [[ -n "${MAVEN_PASSWORD}" ]]; then
    args="${args} -PmavenPassword=${MAVEN_PASSWORD}"
  fi
  if [[ "${DEBUG}" == 'true' ]]; then
    args="${args} --debug"
  fi
  echo "${args}"
}

function publishModule() {
  local project=$1
  local task=$2
  local kt_version=$3
  local build_version=$4
  local enable_ohos="${5:-false}"

  local core_version
  core_version=$(get_kuikly_core_version "${kt_version}")

  local common_args
  common_args=$(build_gradle_args)

  local harmony_flag="false"
  local settings_flag=""
  if [ "${enable_ohos}" = "true" ]; then
    harmony_flag="true"
    settings_flag="-c settings.ohos.gradle.kts"
  fi

  export kuiklyBizVersion="${build_version}"

  log_info "发布 ${project} (kotlin=${kt_version}, version=${build_version}, ohos=${enable_ohos})"

  if ! ${GRADLEW} :${project}:${task} ${settings_flag} \
    -PkotlinVersion="${kt_version}" \
    -PkuiklyCoreVersion="${core_version}" \
    -PHARMONY_ENABLED="${harmony_flag}" \
    -PmavenVersion="${build_version}" \
    ${common_args}; then
    log_error "${project} 构建失败! (kotlin=${kt_version})"
    return 1
  fi

  log_info "${project} 发布成功: ${build_version}"
}

function publishAll() {
  local final_versions=""
  local build_version

  # ====== 标准平台 (Android + iOS + JS) ======
  log_info "========== 开始发布标准平台产物 =========="
  IFS=',' read -r -a VERSIONS <<<"${KOTLIN_VERSION_LIST}"
  for kt_version in "${VERSIONS[@]}"; do
    if [[ "${PUB_ENABLE_SNAPSHOT}" == 'true' ]]; then
      build_version="${BASE_VERSION}-${kt_version}-SNAPSHOT"
    else
      build_version="${BASE_VERSION}-${kt_version}"
    fi

    # 发布跨端模块
    if ! publishModule "${MODULE_KMP}" "${PUBLISH_TASK}" "${kt_version}" "${build_version}" "false"; then
      return 1
    fi

    if [[ -z "${final_versions}" ]]; then
      final_versions="${build_version}"
    else
      final_versions="${final_versions} | ${build_version}"
    fi
  done

  # ====== 鸿蒙平台 ======
  if [[ -n "${OHOS_KOTLIN_VERSION_LIST}" ]]; then
    log_info "========== 开始发布鸿蒙平台产物 =========="
    IFS=',' read -r -a OHOS_VERSIONS <<<"${OHOS_KOTLIN_VERSION_LIST}"
    for kt_version in "${OHOS_VERSIONS[@]}"; do
      if [[ "${PUB_ENABLE_SNAPSHOT}" == 'true' ]]; then
        build_version="${BASE_VERSION}-${kt_version}-SNAPSHOT"
      else
        build_version="${BASE_VERSION}-${kt_version}"
      fi

      if ! publishModule "${MODULE_KMP}" "${PUBLISH_TASK}" "${kt_version}" "${build_version}" "true"; then
        return 1
      fi

      if [[ -z "${final_versions}" ]]; then
        final_versions="${build_version}"
      else
        final_versions="${final_versions} | ${build_version}"
      fi
    done
  fi

  echo ""
  log_info "=========================================="
  log_info "全部发布完成!"
  log_info "发布版本: ${final_versions}"
  log_info "=========================================="
}

function check_required_config() {
  if [ -z "${MODULE_KMP}" ]; then
    log_error "未指定组件模块名! 请在 gradle.properties 中设置 MODULE_NAME 或使用 -n 参数"
    exit 1
  fi
  if [ -z "${BASE_VERSION}" ]; then
    log_error "未指定版本号! 请在 gradle.properties 中设置 MAVEN_VERSION 或使用 -v 参数"
    exit 1
  fi
  if [ -z "${KOTLIN_VERSION_LIST}" ]; then
    log_error "未指定 Kotlin 版本! 请在 gradle.properties 中设置 KOTLIN_VERSION_LIST 或使用 -k 参数"
    exit 1
  fi
}

# ====== 参数解析 ======

while [[ "$#" -gt 0 ]]; do
  case $1 in
  -n | --module-name) shift; MODULE_KMP="${1}" ;;
  -v | --version) shift; BASE_VERSION="${1}" ;;
  -g | --group-id) shift; GROUP_ID="${1}" ;;
  -k | --kotlin) shift; KOTLIN_VERSION_LIST="${1}" ;;
  -ok | --ohos-kotlin) shift; OHOS_KOTLIN_VERSION_LIST="${1}" ;;
  -s | --snapshot) shift; PUB_ENABLE_SNAPSHOT="$1" ;;
  -l | --local-repo) shift; PUB_IS_LOCAL_REPO="$1" ;;
  -m | --maven-url) shift; MAVEN_URL="$1" ;;
  -c | --clean) shift; NEED_CLEAN="$1" ;;
  -d | --debug) shift; DEBUG="$1" ;;
  -h | --help) usage; exit 0 ;;
  *) log_error "未知参数: $1"; usage; exit 1 ;;
  esac
  shift
done

# ====== 打印配置信息 ======
echo ""
log_info "========== ${MODULE_KMP} 发布配置 =========="
log_info "组件模块:     ${MODULE_KMP}"
log_info "版本号:       ${BASE_VERSION}"
log_info "Group ID:     ${GROUP_ID:-未设置(使用 gradle.properties 配置)}"
log_info "Kotlin 版本:  ${KOTLIN_VERSION_LIST}"
log_info "OHOS Kotlin:  ${OHOS_KOTLIN_VERSION_LIST}"
log_info "SNAPSHOT:     ${PUB_ENABLE_SNAPSHOT:-未设置}"
log_info "本地仓库:     ${PUB_IS_LOCAL_REPO:-未设置}"
log_info "Maven URL:    ${MAVEN_URL:-未设置(使用 gradle.properties 配置)}"
log_info "Maven User:   ${MAVEN_USERNAME:+***已配置***}"
log_info "Maven Pass:   ${MAVEN_PASSWORD:+***已配置***}"
log_info "Clean:        ${NEED_CLEAN:-false}"
log_info "Debug:        ${DEBUG:-false}"
log_info "=================================================="
echo ""

# ====== 执行 ======

check_required_config

start_timer

if ! check; then
  log_error "环境检查失败!"
  usage
  exit 1
fi

if ! publishAll; then
  log_error "发布失败!"
  print_duration
  exit 1
fi

print_duration
