#!/usr/bin/env bash
# 本地开发：① 拉齐依赖（go mod tidy） ② 启动 HTTP 服务
# 用法：./scripts/dev-run.sh
# 仅同步依赖：SKIP_RUN=1 ./scripts/dev-run.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

export GOPROXY="${GOPROXY:-https://goproxy.cn,direct}"
export GOSUMDB="${GOSUMDB:-sum.golang.google.cn}"

echo "==> [1/2] go mod tidy"
go mod tidy

if [[ "${SKIP_RUN:-}" == "1" ]]; then
  echo "==> SKIP_RUN=1，跳过启动"
  exit 0
fi

echo "==> [2/2] go run ./cmd/server"
exec go run ./cmd/server "$@"
