# 修复前端连接 Java 后端失败计划

## Summary
- 目标：修复前端请求 `http://localhost:8080/api/v1/*` 出现 `ERR_CONNECTION_REFUSED` 的问题，确保默认 Java 后端联调稳定可用。
- 范围：同时处理两类风险
  - 启动链路风险：`start.sh` 中后端进程容易因脚本退出被清理，导致前端仍在运行但后端已不在监听。
  - 前端联调风险：当 `VITE_API_BASE_URL` 缺失或不生效时，当前 Vite 对 `/api` 无代理，前端会打到 5174 本机并失败。
- 成功标准：
  - 使用根目录脚本联调时，后端在 8080 稳定可达，前端不再出现连接拒绝。
  - 未配置 `VITE_API_BASE_URL` 时，前端开发环境也能通过 `/api` 代理访问 Java 后端。

## Current State Analysis
- 后端端口配置正常：`aigc-server/src/main/resources/application.yml` 当前为 `server.port: 8080`。
- 前端环境变量默认正确：`aigc-site-react/.env` 与 `.env.example` 均为 `VITE_API_BASE_URL=http://localhost:8080`。
- 运行时日志显示关键现象：
  - `.logs/backend.log` 中后端已成功启动到 8080，随后进入 `GracefulShutdown` 并退出。
  - 这与 `start.sh` 的 `trap cleanup EXIT INT TERM` 行为一致：脚本退出时会关闭本次启动的后端。
- 前端开发服务器配置缺口：
  - `aigc-site-react/vite.config.ts` 仅对 `/api/comfy` 代理，未对常规 `/api` 提供代理兜底。
- 用户反馈现象：
  - 浏览器请求 `http://localhost:8080/api/v1/style-templates` 报 `net::ERR_CONNECTION_REFUSED`。
  - 用户希望“脚本稳定性 + 前端代理兜底”两者都做。

## Proposed Changes

### 1) 增强 `start.sh` 后端生命周期控制（主修）
- 文件：`start.sh`
- 修改内容：
  - 新增后端清理策略开关（建议变量名：`KEEP_BACKEND_ON_EXIT`，默认 `1`）：
    - `KEEP_BACKEND_ON_EXIT=1`：脚本退出时不主动杀后端（默认，避免前端在跑但后端被连带停掉）。
    - `KEEP_BACKEND_ON_EXIT=0`：保持当前行为，退出时清理本次启动的后端。
  - 调整 `cleanup()`：
    - 仅在 `KEEP_BACKEND_ON_EXIT=0` 且确认为本脚本启动的进程时才执行 kill。
    - `KEEP_BACKEND_ON_EXIT=1` 时仅清理 PID 文件（避免脏状态）并打印提示。
  - 增强失败可观测性：
    - 当前端 `npm run dev` 异常退出时，明确输出“后端保留/清理”的状态和后端日志路径。
    - 在启动成功提示中打印健康检查地址与后端 PID，便于快速确认。
- 为什么这样改：
  - 直接消除“脚本退出导致后端被动停止”的高频联调断连场景。
  - 保留可切换行为，兼容想要“一起停”的使用习惯。

### 2) 为 React 前端增加 `/api` 通用代理兜底
- 文件：`aigc-site-react/vite.config.ts`
- 修改内容：
  - 在 `server.proxy` 中新增 `/api` 代理配置，目标地址优先级：
    - `VITE_API_PROXY_TARGET`（若设置）
    - 否则默认 `http://localhost:8080`
  - 保留现有 `/api/comfy` 的独立代理逻辑，并确保规则不冲突：
    - `/api/comfy` 继续按 `VITE_COMFY_PROXY_TARGET` 决定是否启用与重写。
    - `/api` 代理不重写路径（保持 `/api/v1/**` 原样转发）。
  - 代理启用策略：
    - 开发模式始终启用 `/api` 代理兜底。
    - 若 `VITE_API_BASE_URL` 已显式配置为绝对地址，前端请求将直连后端；代理作为兜底不影响现有行为。
- 为什么这样改：
  - 避免 `.env` 缺失、变量未生效或新同学首次拉起时出现“请求落到 5174 本机而 404/失败”。
  - 与当前 API 路径设计（统一 `/api/v1/*`）天然匹配。

### 3) 对齐启动与联调说明（防回归）
- 文件：`DEVELOPMENT.md`（必要）与 `README.md`（可选，若包含旧口径）
- 修改内容：
  - 明确 `start.sh` 的后端保留策略与开关用法（`KEEP_BACKEND_ON_EXIT`）。
  - 明确 React 开发端口为 `5174`，并说明 `/api` 代理兜底机制与 `VITE_API_PROXY_TARGET` 用法。
  - 增补排障步骤：先访问 `http://localhost:8080/api/v1/health`，再查 `.logs/backend.log`。
- 为什么这样改：
  - 降低后续“端口正确但服务未存活”与“环境变量遗漏”导致的重复故障。

## Assumptions & Decisions
- 决策：默认后端端口保持 `8080`，不调整 Java 配置。
- 决策：采用“双保险”方案（脚本稳定性 + 前端代理兜底），与用户选择一致。
- 假设：当前主要联调路径为根目录 `start.sh` + React 前端 `aigc-site-react`。
- 假设：后端数据库与依赖在本机可正常启动（本次问题核心不是 SQL 配置错误，而是后端生命周期/可达性问题）。

## Verification Steps
1. 启动验证（默认策略）
  - 在根目录执行：`./start.sh --frontend react`
  - 期望：终端输出后端健康检查通过，前端启动成功。
2. 接口可达性验证
  - 浏览器访问：`http://localhost:8080/api/v1/health`
  - 期望：返回健康状态（HTTP 200）。
3. 前端业务接口验证
  - 打开前端页面触发样式模板请求。
  - 期望：`/api/v1/style-templates` 不再出现 `ERR_CONNECTION_REFUSED`。
4. 代理兜底验证
  - 临时移除/注释 `aigc-site-react/.env` 中 `VITE_API_BASE_URL`，重启前端开发服务。
  - 期望：前端仍可通过 Vite `/api` 代理访问 Java 后端。
5. 生命周期开关验证
  - 分别验证 `KEEP_BACKEND_ON_EXIT=1` 与 `KEEP_BACKEND_ON_EXIT=0`。
  - 期望：行为与文档一致（保留/清理后端可预期）。
