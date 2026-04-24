#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$ROOT_DIR/aigc-server"
FRONTEND_DIR="${FRONTEND_DIR:-}"
FRONTEND_MODE="auto"
BACKEND_PORT="${BACKEND_PORT:-8080}"
HEALTH_URL="http://localhost:${BACKEND_PORT}/api/v1/health"
KEEP_BACKEND_ON_EXIT="${KEEP_BACKEND_ON_EXIT:-1}"

LOG_DIR="$ROOT_DIR/.logs"
PID_FILE="$LOG_DIR/backend.pid"
BACKEND_LOG="$LOG_DIR/backend.log"
STARTED_BACKEND=0

mkdir -p "$LOG_DIR"

usage() {
  cat <<'EOF'
一键启动前后端脚本

用法:
  ./start.sh
  ./start.sh --frontend react
  ./start.sh --frontend vue
  ./start.sh --frontend-dir ./aigc-site-react

参数:
  --frontend <react|vue|auto>  指定前端类型（默认 auto）
  --frontend-dir <path>         指定前端目录（优先级最高）
  -h, --help                    显示帮助
EOF
}

command_exists() {
  command -v "$1" >/dev/null 2>&1
}

is_running() {
  local pid="$1"
  kill -0 "$pid" >/dev/null 2>&1
}

read_pid_file() {
  if [ ! -f "$PID_FILE" ]; then
    return 1
  fi
  local pid
  pid="$(<"$PID_FILE")"
  if [ -z "$pid" ]; then
    return 1
  fi
  if ! [[ "$pid" =~ ^[0-9]+$ ]]; then
    return 1
  fi
  printf '%s' "$pid"
}

port_pid() {
  if ! command_exists lsof; then
    return 1
  fi
  local pid
  pid="$(lsof -ti tcp:"$BACKEND_PORT" -sTCP:LISTEN 2>/dev/null | sed -n '1p')"
  if [ -z "$pid" ]; then
    return 1
  fi
  printf '%s' "$pid"
}

stop_pid() {
  local pid="$1"
  if [ -z "$pid" ]; then
    return 0
  fi
  if ! is_running "$pid"; then
    return 0
  fi
  kill "$pid" >/dev/null 2>&1 || true
  local i=1
  while [ "$i" -le 10 ]; do
    if ! is_running "$pid"; then
      return 0
    fi
    sleep 1
    i=$((i + 1))
  done
  kill -9 "$pid" >/dev/null 2>&1 || true
}

ensure_backend_port_available() {
  local listen_pid=""
  local tracked_pid=""
  listen_pid="$(port_pid || true)"
  tracked_pid="$(read_pid_file || true)"

  if [ -n "$tracked_pid" ] && ! is_running "$tracked_pid"; then
    rm -f "$PID_FILE"
    tracked_pid=""
  fi

  if [ -z "$listen_pid" ]; then
    return 0
  fi

  if [ -n "$tracked_pid" ] && [ "$listen_pid" = "$tracked_pid" ]; then
    echo "[后端] 检测到旧的脚本后端占用端口 ${BACKEND_PORT} (PID: $tracked_pid)，正在清理..."
    stop_pid "$tracked_pid"
    rm -f "$PID_FILE"
    return 0
  fi

  echo "[错误] 端口 ${BACKEND_PORT} 已被其他进程占用 (PID: $listen_pid)"
  echo "[错误] 为避免误连旧服务，已停止启动。请先关闭该进程后重试。"
  exit 1
}

parse_args() {
  while [ "$#" -gt 0 ]; do
    case "$1" in
      --frontend)
        if [ "$#" -lt 2 ]; then
          echo "[错误] --frontend 缺少参数值"
          exit 1
        fi
        FRONTEND_MODE="$2"
        shift 2
        ;;
      --frontend-dir)
        if [ "$#" -lt 2 ]; then
          echo "[错误] --frontend-dir 缺少参数值"
          exit 1
        fi
        FRONTEND_DIR="$2"
        shift 2
        ;;
      -h|--help)
        usage
        exit 0
        ;;
      *)
        echo "[错误] 不支持的参数: $1"
        usage
        exit 1
        ;;
    esac
  done
}

resolve_frontend_dir() {
  if [ -n "$FRONTEND_DIR" ]; then
    if [ "${FRONTEND_DIR#/}" = "$FRONTEND_DIR" ]; then
      FRONTEND_DIR="$ROOT_DIR/$FRONTEND_DIR"
    fi
    return
  fi

  case "$FRONTEND_MODE" in
    react)
      FRONTEND_DIR="$ROOT_DIR/aigc-site-react"
      ;;
    vue)
      FRONTEND_DIR="$ROOT_DIR/aigc-site"
      ;;
    auto)
      if [ -d "$ROOT_DIR/aigc-site-react" ]; then
        FRONTEND_DIR="$ROOT_DIR/aigc-site-react"
      elif [ -d "$ROOT_DIR/aigc-site" ]; then
        FRONTEND_DIR="$ROOT_DIR/aigc-site"
      else
        FRONTEND_DIR=""
      fi
      ;;
    *)
      echo "[错误] --frontend 仅支持 react / vue / auto"
      exit 1
      ;;
  esac
}

assert_dirs() {
  if [ ! -d "$BACKEND_DIR" ]; then
    echo "[错误] 未找到后端目录: $BACKEND_DIR"
    exit 1
  fi
  if [ -z "$FRONTEND_DIR" ] || [ ! -d "$FRONTEND_DIR" ]; then
    echo "[错误] 未找到前端目录。可尝试："
    echo "  ./start.sh --frontend react"
    echo "  ./start.sh --frontend-dir ./aigc-site-react"
    exit 1
  fi
}

wait_backend_ready() {
  local retries=40
  local i=1
  local pid="$1"
  while [ "$i" -le "$retries" ]; do
    if ! is_running "$pid"; then
      echo "[错误] 后端进程已退出 (PID: $pid)"
      echo "[错误] 请查看日志定位原因: $BACKEND_LOG"
      return 1
    fi
    if curl -fsS "$HEALTH_URL" >/dev/null 2>&1; then
      echo "[后端] 健康检查通过: $HEALTH_URL"
      return 0
    fi
    sleep 1
    i=$((i + 1))
  done
  echo "[错误] 后端启动超时或健康检查未通过: $HEALTH_URL"
  echo "[错误] 请查看日志定位原因: $BACKEND_LOG"
  return 1
}

start_backend() {
  ensure_backend_port_available

  echo "[后端] 启动中..."
  (
    cd "$BACKEND_DIR"
    nohup mvn spring-boot:run >"$BACKEND_LOG" 2>&1 &
    echo "$!" >"$PID_FILE"
  )
  STARTED_BACKEND=1
  local new_pid
  new_pid="$(read_pid_file || true)"
  if [ -z "$new_pid" ]; then
    echo "[错误] 后端 PID 写入失败，启动中止。"
    exit 1
  fi
  echo "[后端] 已启动 (PID: $new_pid)"
  if ! wait_backend_ready "$new_pid"; then
    exit 1
  fi
}

prepare_frontend_env() {
  if [ ! -f "$FRONTEND_DIR/.env" ]; then
    if [ -f "$FRONTEND_DIR/.env.example" ]; then
      cp "$FRONTEND_DIR/.env.example" "$FRONTEND_DIR/.env"
      echo "[前端] 已从 .env.example 生成 .env"
    else
      echo "VITE_API_BASE_URL=http://localhost:8080" >"$FRONTEND_DIR/.env"
      echo "[前端] 已创建默认 .env"
    fi
  fi
}

install_frontend_deps() {
  if [ ! -d "$FRONTEND_DIR/node_modules" ]; then
    echo "[前端] 未检测到 node_modules，正在安装依赖..."
    (
      cd "$FRONTEND_DIR"
      npm install
    )
  fi
}

cleanup() {
  if [ "$STARTED_BACKEND" -eq 1 ] && [ -f "$PID_FILE" ]; then
    pid="$(<"$PID_FILE")"
    if [ "${KEEP_BACKEND_ON_EXIT}" = "0" ]; then
      if is_running "$pid"; then
        echo
        echo "[清理] 正在关闭本次启动的后端服务..."
        kill "$pid" >/dev/null 2>&1 || true
      fi
      rm -f "$PID_FILE"
    else
      if is_running "$pid"; then
        echo
        echo "[清理] 已保留后端服务运行 (PID: $pid, HEALTH: $HEALTH_URL)"
        echo "[清理] 如需退出时自动关闭后端，请设置 KEEP_BACKEND_ON_EXIT=0"
      else
        rm -f "$PID_FILE"
      fi
    fi
  fi
}

main() {
  parse_args "$@"
  resolve_frontend_dir
  assert_dirs

  if ! command_exists mvn; then
    echo "[错误] 未检测到 mvn，请先安装 Maven。"
    exit 1
  fi
  if ! command_exists npm; then
    echo "[错误] 未检测到 npm，请先安装 Node.js/NPM。"
    exit 1
  fi
  if ! command_exists curl; then
    echo "[错误] 未检测到 curl，请先安装 curl。"
    exit 1
  fi
  if ! command_exists lsof; then
    echo "[错误] 未检测到 lsof，无法检测端口占用。请先安装 lsof。"
    exit 1
  fi

  start_backend
  prepare_frontend_env
  install_frontend_deps

  trap cleanup EXIT INT TERM

  echo "[前端] 启动中..."
  echo "[前端] 目录: $FRONTEND_DIR"
  echo "[访问地址] 以终端中 Vite 输出的 Local 地址为准"
  echo "[后端] 健康检查地址: $HEALTH_URL"
  set +e
  (
    cd "$FRONTEND_DIR"
    npm run dev
  )
  frontend_exit_code=$?
  set -e
  if [ "$frontend_exit_code" -ne 0 ]; then
    echo "[前端] dev 进程异常退出 (exit=$frontend_exit_code)"
    echo "[前端] 可查看后端日志: $BACKEND_LOG"
    if [ "${KEEP_BACKEND_ON_EXIT}" = "0" ]; then
      echo "[前端] 当前策略：退出时清理后端 (KEEP_BACKEND_ON_EXIT=0)"
    else
      echo "[前端] 当前策略：保留后端运行 (KEEP_BACKEND_ON_EXIT=1)"
    fi
  fi
  return "$frontend_exit_code"
}

main "$@"
