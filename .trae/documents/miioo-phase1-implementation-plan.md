# Miioo 一期闭环落地计划（Plan）

## 1. Summary
- 目标：基于 `Miioo动画视频创作平台-详细开发文档.md` 启动“一期闭环”工程落地，实现 `登录 -> 项目 -> 剧本 -> 主体 -> 图片生成入库` 的可运行最小版本。
- 范围：前后端同仓（`frontend` + `backend`）建设，严格遵守 `AGENTS.md`、`RULES.md`、`TESTS.md` 与 `SPECS/` 约束。
- 策略：AI 能力采用“统一适配层 + Mock优先 + 可切换真实模型”实现，先打通异步任务链路与数据闭环。
- 交付原则：每个模块均包含规格对齐、实现、最小测试、自检记录与 `PROGRESS.md` 更新。

## 2. Current State Analysis
- 仓库当前状态：仅有文档体系，无前后端代码与工程配置。
- 已存在治理资产：
  - 需求与事实源：`Miioo动画视频创作平台-详细开发文档.md`
  - 规格：`SPECS/product-scope.md`、`SPECS/functional-modules.md`、`SPECS/api-contracts.md`、`SPECS/nonfunctional-security.md`
  - 架构：`ARCHITECTURE.md`
  - 红线与测试门禁：`RULES.md`、`TESTS.md`
  - 进度基线：`PROGRESS.md`
- 关键缺口：
  - 缺少工程脚手架（React/Spring Boot）、数据库迁移、统一响应与鉴权、对象存储接入、任务执行框架。
  - 缺少一期能力接口实现与端到端联调路径。
  - 缺少自动化测试与发布前最小用例执行记录。

## 3. Proposed Changes

### 3.1 仓库与工程骨架
- 新增 `frontend/`：
  - 目标：建立 React 18 + TypeScript + Ant Design + TanStack Query + Zustand 工程。
  - 关键文件（新建）：
    - `frontend/package.json`：依赖与脚本（dev/build/test/lint）。
    - `frontend/src/main.tsx`、`frontend/src/App.tsx`：应用入口。
    - `frontend/src/router/index.tsx`：路由骨架（首页、项目列表、项目工作台）。
    - `frontend/src/api/http.ts`：Axios 实例 + 统一响应拦截。
    - `frontend/src/stores/userStore.ts`、`frontend/src/stores/projectStore.ts`：全局状态。
    - `frontend/src/pages/*`：一期页面占位与核心交互页。
- 新增 `backend/`：
  - 目标：建立 Spring Boot 3.x + Spring Security JWT + MyBatis Plus + Redis + Spring AI 的后端工程。
  - 关键文件（新建）：
    - `backend/pom.xml`：依赖管理。
    - `backend/src/main/java/.../MiiooApplication.java`：启动类。
    - `backend/src/main/resources/application.yml`：基础配置（环境变量占位，不落敏感信息）。
    - `backend/src/main/java/.../common/*`：统一响应、异常、traceId、分页模型。
    - `backend/src/main/java/.../config/*`：security/redis/async/storage 配置。
    - `backend/src/main/java/.../auth|project|script|subject|asset|aitask|model/*`：一期必要包结构。
- 根目录补充（新建）：
  - `.gitignore`、`README.md`（运行指引）、`docker-compose.yml`（MySQL/Redis/MinIO 本地依赖）。

### 3.2 数据库与数据模型（一期最小集）
- 新增 `backend/src/main/resources/db/migration/`（Flyway）：
  - `V1__init_core_tables.sql`：`user`、`project`、`script`、`script_episode`、`subject`、`asset`、`ai_task`、`ai_model`。
  - `V2__add_indexes_and_constraints.sql`：唯一索引、外键/约束、常用查询索引。
- 与规格对齐：
  - 按主文档第6章与 `DOCS/data-model.md` 对齐字段。
  - 显式加入 `user_id`、`project_id` 归属字段与逻辑删除策略，满足 `RULES.md` 数据红线。
- 文档更新：
  - 若字段或约束有偏差，同步更新 `DOCS/data-model.md` 与 `SPECS/api-contracts.md`（仅在发生契约变化时）。

### 3.3 一期后端接口落地（按模块）
- `auth`：
  - 实现：`POST /api/auth/login`、`GET /api/auth/me`。
  - 要点：JWT 签发与校验、密码加密、统一错误码、401/403 区分。
- `project`：
  - 实现：项目创建/列表/详情/更新/删除（含删除前任务状态检查）。
  - 要点：同用户项目名唯一、归属校验、二次确认语义与后端保护。
- `script`：
  - 实现：AI 生成任务创建、上传剧本、分集 CRUD、从剧本提取主体（任务化）。
  - 要点：生成类接口返回 `taskId`，后台更新 `ai_task` 与业务表。
- `subject`：
  - 实现：主体 CRUD、批量生成任务、定稿接口。
  - 要点：删除前引用检查（预留 `subject_storyboard_rel` 兼容位，即使一期先不完整启用分镜）。
- `asset`：
  - 实现：上传、列表、详情、删除、星标（一期最小可用）。
  - 要点：文件类型/大小校验、对象存储落盘、业务与文件一致性。
- `aitask`：
  - 实现：任务详情、进度查询、失败原因、重试、取消（一期必要子集）。
  - 要点：状态机严格 `PENDING -> RUNNING -> SUCCESS|FAILED|CANCELLED`，幂等更新防回退。

### 3.4 AI 适配与异步任务（Mock优先）
- 新增后端能力层（新建）：
  - `integration/ai/`：`AiProvider` 接口 + `MockAiProvider` + `ProviderRouter`。
  - `service/aitask/`：任务创建、投递、执行、状态更新模板。
  - `service/orchestration/`：剧本生成、主体提取、图片生成的编排服务。
- 行为规范：
  - Controller 只返回 `taskId`，不阻塞等待长耗时任务。
  - Mock 输出结构与真实模型期望结构一致，减少后续切换成本。
  - 真实模型通过配置开关启用，不改业务层。

### 3.5 前端一期页面与交互
- 页面范围：
  - 登录页、项目列表页、项目工作台（一期子页：全局设置、剧本、主体、资产）。
- 核心能力：
  - 表单校验（必填、长度、重复名提示）。
  - 任务轮询 Hook（2-5 秒）统一处理 RUNNING/SUCCESS/FAILED。
  - 空态、错误态、失败重试入口。
- API 对齐：
  - 严格对齐 `SPECS/api-contracts.md` 的统一响应与分页格式。
  - 请求层注入 `Authorization`，并统一处理 401 跳转登录。

### 3.6 文档与治理同步
- 按任务推进实时更新：
  - `PROGRESS.md`：完成项、风险、下一步、测试结论。
  - `SPECS/api-contracts.md`：仅在接口契约变更时更新。
  - `DOCS/data-model.md`：仅在表结构/字段语义变化时更新。
  - `ARCHITECTURE.md`：若边界或分层方案发生调整则更新。

### 3.7 迭代分解（建议执行顺序）
- 批次 A（基础设施，约 2-3 天）：
  - 前后端脚手架、基础配置、统一响应、鉴权中间件、数据库迁移、容器依赖。
- 批次 B（核心业务，约 3-5 天）：
  - `auth/project/script/subject` 后端接口 + 前端对应页面。
- 批次 C（AI 与资产闭环，约 2-4 天）：
  - `ai_task`、Mock 适配层、图片生成入库、任务中心最小能力。
- 批次 D（联调与稳定性，约 1-2 天）：
  - 最小用例回归、错误处理补齐、文档与进度收尾。

## 4. Assumptions & Decisions
- 已确认决策：
  - 第一轮范围为“一期闭环”。
  - 代码仓采用“前后端同仓”。
  - AI 能力采用“Mock优先”。
- 实施假设：
  - 本地具备 Java 17+、Node 20+、Docker 环境。
  - 一期不强制接入真实模型厂商凭证，采用配置开关后续切换。
  - 一期先保证图片资产链路，视频渲染/分镜视频能力放在二期扩展。
- 红线约束（执行中必须持续满足）：
  - 严格 `user_id/project_id` 归属校验。
  - 不在前端暴露模型 API Key。
  - 长耗时任务必须走异步机制。
  - 变更必须同步规格与进度文档。

## 5. Verification Steps
- 环境与启动验证：
  - `docker-compose up -d` 后 MySQL/Redis/MinIO 可用。
  - 前后端工程可分别启动并通过健康检查。
- 后端接口验证（最小）：
  - 登录成功拿到 token；未登录访问受保护接口返回 401。
  - 项目 CRUD 满足归属和唯一性约束。
  - 剧本生成/主体提取/图片生成接口返回 `taskId` 且可轮询状态。
  - 任务失败可见 `error_message` 且可重试。
- 前端链路验证（最小）：
  - 登录后完成“创建项目 -> 剧本任务 -> 主体任务 -> 图片入库”路径。
  - 页面可展示任务状态与失败重试入口。
- 测试门禁：
  - 按 `TESTS.md` 发布前最小用例执行并记录结果。
  - 每个模块提交时填写“模块自检模板”并同步 `PROGRESS.md`。
