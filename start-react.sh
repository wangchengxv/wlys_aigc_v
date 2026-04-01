#!/usr/bin/env bash
# React 专用启动入口：强制使用 React 前端目录并复用统一启动脚本
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export FRONTEND_DIR="$ROOT_DIR/aigc-site-react"
exec "$ROOT_DIR/start.sh" --frontend react
