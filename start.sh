#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_DIR="$ROOT_DIR/aigc-site"
BACKEND_DIR="$ROOT_DIR/aigc-server"
LOG_DIR="$ROOT_DIR/.logs"
PID_FILE="$LOG_DIR/backend.pid"
BACKEND_LOG="$LOG_DIR/backend.log"
STARTED_BACKEND=0

mkdir -p "$LOG_DIR"

command_exists() {
  command -v "$1" >/dev/null 2>&1
}

assert_dirs() {
  if [ ! -d "$FRONTEND_DIR" ]; then
    echo "[错误] 未找到前端目录: $FRONTEND_DIR"
    exit 1
  fi
  if [ ! -d "$BACKEND_DIR" ]; then
    echo "[错误] 未找到后端目录: $BACKEND_DIR"
    exit 1
  fi
}

is_running() {
  local pid="$1"
  kill -0 "$pid" >/dev/null 2>&1
}

wait_backend_ready() {
  local retries=40
  local i=1
  while [ "$i" -le "$retries" ]; do
    if curl -s "http://localhost:8080/api/v1/health" >/dev/null 2>&1; then
      echo "[后端] 健康检查通过: http://localhost:8080/api/v1/health"
      return 0
    fi
    sleep 1
    i=$((i + 1))
  done
  echo "[警告] 后端启动超时，请查看日志: $BACKEND_LOG"
  return 1
}

start_backend() {
  if [ -f "$PID_FILE" ]; then
    old_pid="$(cat "$PID_FILE")"
    if is_running "$old_pid"; then
      echo "[后端] 已在运行 (PID: $old_pid)"
      return 0
    fi
    rm -f "$PID_FILE"
  fi

  echo "[后端] 启动中..."
  cd "$BACKEND_DIR"
  nohup mvn spring-boot:run >"$BACKEND_LOG" 2>&1 &
  backend_pid=$!
  echo "$backend_pid" >"$PID_FILE"
  STARTED_BACKEND=1
  echo "[后端] 已启动 (PID: $backend_pid)"
  wait_backend_ready || true
}

prepare_frontend_env() {
  if [ ! -f "$FRONTEND_DIR/.env" ]; then
    if [ -f "$FRONTEND_DIR/.env.example" ]; then
      cp "$FRONTEND_DIR/.env.example" "$FRONTEND_DIR/.env"
      echo "[前端] 已创建 .env 文件"
    else
      echo "VITE_API_BASE_URL=http://localhost:8080" >"$FRONTEND_DIR/.env"
      echo "[前端] 已创建默认 .env 文件"
    fi
  fi
}

install_frontend_deps() {
  if [ ! -d "$FRONTEND_DIR/node_modules" ]; then
    echo "[前端] 未检测到 node_modules，正在安装依赖..."
    cd "$FRONTEND_DIR"
    npm install
  fi
}

cleanup() {
  if [ "$STARTED_BACKEND" -eq 1 ] && [ -f "$PID_FILE" ]; then
    pid="$(cat "$PID_FILE")"
    if is_running "$pid"; then
      echo
      echo "[清理] 正在关闭本次启动的后端服务..."
      kill "$pid" >/dev/null 2>&1 || true
    fi
    rm -f "$PID_FILE"
  fi
}

main() {
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

  start_backend
  prepare_frontend_env
  install_frontend_deps

  trap cleanup EXIT INT TERM

  echo "[前端] 启动中..."
  echo "[访问地址] 默认 http://localhost:5173（若端口被占用，请看下方 Vite 输出的 Local 地址）"
  cd "$FRONTEND_DIR"
  npm run dev
}

main
