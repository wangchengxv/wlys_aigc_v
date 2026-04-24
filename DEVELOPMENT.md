# AIGC 图文生成平台 — 开发文档

本文档面向在本仓库中开发与联调的同学，与根目录 [README.md](./README.md)（接口清单与功能说明）互补。

## 1. 项目概览

| 模块 | 说明 |
|------|------|
| `aigc-server` | Spring Boot 后端：生成任务、历史、模型配置、LLM 路由代理、剧本工程流水线等 |
| `aigc-site-react` | **当前主推** 前端：React 19 + TypeScript + Vite |
| `aigc-site-vue-off` | 历史 Vue 前端（已下线/归档，一般不再使用） |

根目录 README 中若仍写 `aigc-site`（Vue），请以本仓库实际目录 **`aigc-site-react`** 为准。

## 2. 技术栈

**后端**

- Java **17**
- Spring Boot **3.5.x**（Web、Validation、JPA、Actuator）
- MySQL + **Flyway** 迁移
- 其他：Apache POI（文档）、AWS Bedrock / Google Auth 等（按功能模块使用）

**前端**

- React **19**、TypeScript、**Vite 8**
- `react-router-dom`、`axios`、`zustand`
- 开发服务器端口见下文（默认 **5174**，非 5173）

## 3. 环境要求

- **JDK 17**、**Maven**（`mvn`）
- **Node.js** + **npm**
- **MySQL**：与 `application.yml` 中数据源一致，且需能通过 Flyway 建表/升级
- 联调脚本依赖：**curl**、**lsof**（macOS/Linux 通常自带）

## 4. 目录结构（简要）

```
aigc-server/                 # 后端
  src/main/java/com/example/aigc/
  src/main/resources/
    application.yml          # 主配置（端口、数据源、aigc.* 等）
    db/migration/            # Flyway SQL
aigc-site-react/             # React 前端
  src/                       # 页面、组件、路由
  vite.config.ts             # 含 dev server 端口
  .env / .env.example        # VITE_API_BASE_URL
start.sh                     # 一键：后端 + 前端
start-react.sh               # 指定 React 前端后调用 start.sh
```

入口类：`com.example.aigc.AigcServerApplication`。

## 5. 配置说明

### 5.1 后端 `application.yml`

- **`server.port`**：默认 `8080`。
- **数据源**：通过 `DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USER`、`DB_PASSWORD` 等环境变量覆盖；生产勿把真实密码提交仓库。
- **`aigc.storage.data-dir`**：本地 JSON/文件类持久化根目录，默认 `${user.home}/.aigcmanju`（可用 `AIGC_DATA_DIR` 覆盖）。
- **`aigc.cors.allowed-origin-patterns`**：跨域来源，可用 `AIGC_CORS_ALLOWED_ORIGIN_PATTERNS` 覆盖（逗号分隔）。
- **`aigc.ark.*`**：火山方舟相关；**`ARK_API_KEY`** 建议用环境变量注入，勿写进仓库。
- **`aigc.auth.*`**：如 `AIGC_ACCESS_TOKEN`、`AIGC_USER_ID_REQUIRED`。
- **`aigc.pipeline.video.*`**：剧本流水线视频并发与轮询等。

详细键名与行为以 **README 第 5 节「模型与密钥配置」** 为准。

### 5.2 前端环境变量

复制 `aigc-site-react/.env.example` 为 `.env`，至少设置：

- **`VITE_API_BASE_URL`**：后端基地址，本地联调一般为 `http://localhost:8080`。
- **`VITE_API_PROXY_TARGET`**（可选）：Vite 开发代理目标地址，默认 `http://localhost:8080`。

`start.sh` 在无 `.env` 时会从 `.env.example` 复制或写入默认 `http://localhost:8080`。
即使未配置 `VITE_API_BASE_URL`，开发环境也可通过 `vite.config.ts` 的 `/api` 代理兜底访问后端。

## 6. 本地运行方式

### 6.1 只启动后端

```bash
cd aigc-server
export ARK_API_KEY=你的密钥   # 以及按需 export DB_* 等
mvn spring-boot:run
```

健康检查：`GET http://localhost:8080/api/v1/health`（与 `start.sh` 中一致）。

### 6.2 只启动前端

```bash
cd aigc-site-react
cp .env.example .env   # 按需修改 VITE_API_BASE_URL
npm install
npm run dev
```

Vite 开发端口在 `vite.config.ts` 的 `server.port`，当前仓库为 **5174**（浏览器以终端打印的 Local 为准）。

### 6.3 一键启动（推荐）

在项目根目录：

```bash
./start.sh
# 或显式指定 React：
./start.sh --frontend react
# 或：./start-react.sh
```

行为概要：

1. 检查 `mvn` / `npm` / `curl` / `lsof`。
2. 后台启动后端，日志在 `.logs/backend.log`，PID 在 `.logs/backend.pid`。
3. 等待 `/api/v1/health` 通过。
4. 准备前端 `.env`、必要时 `npm install`，再执行 `npm run dev`。
5. 默认 `Ctrl+C` 退出前端后会保留后端进程（`KEEP_BACKEND_ON_EXIT=1`）；如需退出时自动关闭后端，设置 `KEEP_BACKEND_ON_EXIT=0`。

**注意**：若 **8080** 已被非本脚本进程占用，脚本会报错退出，需先释放端口。

环境变量 **`BACKEND_PORT`** 可改后端端口（需与前端 `VITE_API_BASE_URL` 一致）。
环境变量 **`KEEP_BACKEND_ON_EXIT`** 控制退出脚本时是否关闭后端（`1` 保留，`0` 清理）。

### 6.4 生产构建前端

```bash
cd aigc-site-react
npm run build
```

产物在 `dist/`；静态资源托管方式由部署环境决定（可由 Spring 反代或 Nginx 等）。

## 7. 数据库与迁移

- 使用 **Flyway**，启动时自动执行 `src/main/resources/db/migration/` 下脚本。
- JPA `ddl-auto: validate`，表结构以迁移脚本为准。
- 存在自 JSON 向 MySQL 迁移的启动逻辑（`JsonToMysqlMigrationRunner`），首次迁库时注意数据目录与标记文件说明。

## 8. API 与功能清单

完整 REST 列表、路由控制台、剧本工程流水线等说明见 **根目录 [README.md](./README.md) 第 3～4 节**。

## 9. 常见问题

| 现象 | 可能原因 |
|------|----------|
| 前端请求跨域失败 | 检查 `aigc.cors` 是否包含前端来源；开发机可用 `http://localhost:*` 类 pattern |
| 后端启动失败 | MySQL 不可达、账号密码错误、Flyway 与库版本不一致；查看控制台与 `.logs/backend.log` |
| 前端报 `ERR_CONNECTION_REFUSED` | 先访问 `http://localhost:8080/api/v1/health`；若不通先查 `.logs/backend.log`，并确认是否误将 `KEEP_BACKEND_ON_EXIT` 设为 `0` 导致退出前端时后端被清理 |
| 一键启动报端口占用 | 释放 8080 或调整 `BACKEND_PORT` 并同步前端 API 地址 |
| 图文/视频不走真实模型 | 未配置连接与模型，或 `ARK_API_KEY` 未设置，会按 README「当前实现说明」回退 |

## 10. 安全提示

- 勿将 **API Key、数据库密码、加密密钥** 提交到 Git。
- 生产环境务必修改默认 `ENCRYPTION_KEY`、`AIGC_ACCESS_TOKEN` 等占位配置。

---

*文档版本与仓库结构同步；若与代码不一致，以代码与 `application.yml` 为准。*
