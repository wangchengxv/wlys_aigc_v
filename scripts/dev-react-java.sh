#!/usr/bin/env bash
# 一键启动：Vite 前端 + Spring Boot（Java）后端（端口与 Go 栈不重复）
#   后端 API: http://127.0.0.1:9090   前端: http://127.0.0.1:5175
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAVA_DIR="$ROOT/aigc-server"
WEB_DIR="$ROOT/aigc-site-react"

API_PORT="${AIGC_DEV_JAVA_API_PORT:-9090}"
VITE_PORT="${AIGC_DEV_JAVA_VITE_PORT:-5175}"

cleanup() {
  if [[ -n "${JAVA_PID:-}" ]] && kill -0 "$JAVA_PID" 2>/dev/null; then
    kill "$JAVA_PID" 2>/dev/null || true
    wait "$JAVA_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT INT TERM

if [[ ! -d "$JAVA_DIR" ]] || [[ ! -d "$WEB_DIR" ]]; then
  echo "错误：未找到 aigc-server 或 aigc-site-react（请从仓库根目录运行）" >&2
  exit 1
fi

if ! command -v mvn >/dev/null 2>&1; then
  echo "错误：未找到 mvn，请先安装 Maven 并加入 PATH" >&2
  exit 1
fi

if [[ ! -d "$WEB_DIR/node_modules" ]]; then
  echo "==> 安装前端依赖 (npm install)"
  (cd "$WEB_DIR" && npm install)
fi

echo "==> 启动 Java 后端 :$API_PORT"
(
  cd "$JAVA_DIR"
  export SERVER_PORT="$API_PORT"
  exec mvn -q -DskipTests spring-boot:run
) &
JAVA_PID=$!

export VITE_API_BASE_URL="http://127.0.0.1:${API_PORT}"
echo "==> 启动 React 前端 :$VITE_PORT  (VITE_API_BASE_URL=$VITE_API_BASE_URL)"
cd "$WEB_DIR"
npm run dev -- --port "$VITE_PORT" --host 127.0.0.1
