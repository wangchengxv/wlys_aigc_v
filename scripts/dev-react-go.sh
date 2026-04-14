#!/usr/bin/env bash
# 一键启动：Vite 前端 + Go 后端（端口与 Java 栈不重复）
#   后端 API: http://127.0.0.1:9080   前端: http://127.0.0.1:5173
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GO_DIR="$ROOT/aigc-server-go"
WEB_DIR="$ROOT/aigc-site-react"

API_PORT="${AIGC_DEV_GO_API_PORT:-9080}"
VITE_PORT="${AIGC_DEV_GO_VITE_PORT:-5173}"

export GOPROXY="${GOPROXY:-https://goproxy.cn,direct}"
export GOSUMDB="${GOSUMDB:-sum.golang.google.cn}"

cleanup() {
  if [[ -n "${GO_PID:-}" ]] && kill -0 "$GO_PID" 2>/dev/null; then
    kill "$GO_PID" 2>/dev/null || true
    wait "$GO_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT INT TERM

if [[ ! -d "$GO_DIR" ]] || [[ ! -d "$WEB_DIR" ]]; then
  echo "错误：未找到 aigc-server-go 或 aigc-site-react（请从仓库根目录运行）" >&2
  exit 1
fi

if [[ ! -d "$WEB_DIR/node_modules" ]]; then
  echo "==> 安装前端依赖 (npm install)"
  (cd "$WEB_DIR" && npm install)
fi

echo "==> 启动 Go 后端 :$API_PORT"
(
  cd "$GO_DIR"
  export SERVER_PORT="$API_PORT"
  exec go run ./cmd/server
) &
GO_PID=$!

export VITE_API_BASE_URL="http://127.0.0.1:${API_PORT}"
echo "==> 启动 React 前端 :$VITE_PORT  (VITE_API_BASE_URL=$VITE_API_BASE_URL)"
cd "$WEB_DIR"
npm run dev -- --port "$VITE_PORT" --host 127.0.0.1
