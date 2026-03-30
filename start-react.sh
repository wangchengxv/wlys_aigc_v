#!/usr/bin/env bash
# 使用 React 前端（aigc-site-react）启动：设置 FRONTEND_DIR 后复用 start.sh
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export FRONTEND_DIR="$ROOT_DIR/aigc-site-react"
exec "$ROOT_DIR/start.sh"
