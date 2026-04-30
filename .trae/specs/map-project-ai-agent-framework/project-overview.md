# 项目概览（基于代码事实）

## 1. 目标

从代码可见，该仓库是一个“高校 AIGC 实训平台”的前后端一体项目，目标集中在以下能力：

- AIGC 创作工作台：文本/图片/视频生成、反推提示词、历史记录（前端 `WorkspacePage`、`ReversePromptPage`，后端 `GenerationController`、`ReversePromptController`）。
- 剧本工程流水线：项目创建、剧本完善、资产抽取、关键帧、镜头、视频生成、配音、口型同步、剪辑与导出（路由 `/script-projects/*` 与 `scriptProjectWorkflowSteps`，后端 `ScriptProjectController`、`VideoPipelineController`、`KeyframeController`、`ScriptAssetController`）。
- 教学管理：课程、作业、提交、评分与统计（前端 `TeachingCoursesPage`/详情页，后端 `TeachingCourseController`、`TeachingAssignmentController`、`TeachingSubmissionController`）。
- 平台治理：组织用户、审计日志、运营看板、内容审核、路由控制台与 API 代理（对应后台多个 `controller` 与前端管理页）。

## 2. 技术栈

### 前端（`aigc-site-react`）

- 运行时：React 19 + TypeScript + Vite（`package.json`）。
- 路由：`react-router-dom`，集中定义在 `src/router.tsx`。
- 状态管理：`zustand`（`src/stores/*`）。
- 网络请求：`axios`（`src/api/index.ts`）。
- 图形相关：`@comfyorg/litegraph`（`ComfyLikeCanvas` 与 `main.tsx` polyfill 注释可见）。
- 构建与质量：`vite`、`typescript`、`eslint`。

### 后端（`aigc-server`）

- 运行时：Java 17，Spring Boot 3.5.6（`pom.xml`）。
- 核心依赖：`spring-boot-starter-web`、`validation`、`data-jpa`、`actuator`。
- 数据层：MySQL 驱动 + JPA + Flyway（`pom.xml` 与 `application.yml`）。
- 认证相关：`spring-security-crypto`、`jjwt-*`。
- 其他依赖：`poi-ooxml`（导入导出相关）、AWS Bedrock SDK、Google OAuth HTTP 库。
- 测试依赖：`spring-boot-starter-test`、`h2`（test scope）。

## 3. 目录职责

### 仓库根目录

- `.trae/specs/map-project-ai-agent-framework/`：当前规格文档目录（`checklist.md`、`spec.md`、`tasks.md`）。
- `aigc-site-react/`：React 前端。
- `aigc-server/`：Spring Boot 后端。
- `start.sh`：一键启动脚本（自动启动后端并拉起前端 dev）。

### 前端目录（`aigc-site-react/src`）

- `pages/`：页面级路由组件（工作台、剧本工程、教学、管理等）。
- `router.tsx`：全站路由表与页面元信息。
- `api/`：前端 API 客户端与请求封装。
- `components/`：可复用 UI 组件与业务组件（如 `script/`、`workspace/`）。
- `stores/`：状态仓库（认证、剧本工程、主题、全局设置等）。
- `lib/`：业务辅助逻辑与测试（如视频剪辑契约、反推逻辑）。
- `layouts/`：壳层布局（`AppLayout`）。
- `context/`：上下文（如 `ToastContext`）。
- `styles/`：全局与组件样式。

### 后端目录（`aigc-server/src/main/java/com/example/aigc`）

- `controller/`：HTTP API 入口层。
- `service/`：业务服务层（生成、路由、鉴权、工作流等）。
- `repository/`：仓储抽象与实现（JPA / 文件 / 内存）。
- `entity/`：领域实体（项目、任务、资产、课程、用户等）。
- `dto/`：接口请求与响应模型。
- `model/`：配置模型与路由模型。
- `config/`：配置属性、Web 配置、拦截器等。
- `enums/`：状态与类型枚举。
- `exception/`：统一异常模型与处理。

## 4. 运行方式

### 一键启动（根目录）

`start.sh` 的逻辑为：

1. 检查 `mvn`、`npm`、`curl`、`lsof`。
2. 在 `aigc-server` 执行 `mvn spring-boot:run`（后台运行并写日志）。
3. 用 `http://localhost:8080/api/v1/health` 做健康检查。
4. 解析前端目录（优先 `aigc-site-react`），必要时生成 `.env`、安装依赖。
5. 在前端执行 `npm run dev`。

### 手动启动

- 后端：进入 `aigc-server`，运行 `mvn spring-boot:run`。
- 前端：进入 `aigc-site-react`，运行 `npm install`、`npm run dev`。

### 关键配置（代码中可见）

- 前端 API 地址：`VITE_API_BASE_URL`（`.env.example` 默认 `http://localhost:8080`）。
- 后端端口：`server.port=8080`（`application.yml`）。
- 数据库：`spring.datasource.*`（MySQL）。
- 存储：`aigc.storage.*`（默认本地目录）。
- 模型/平台：`aigc.ark.*`、`aigc.onelinkai.*`、`aigc.comfy.*`。
- 认证：`aigc.auth.*`（JWT、社交登录、种子用户等）。

## 5. 核心页面流程

### 全局访问与登录

- `main.tsx` 启动时初始化主题和全局设置，挂载 `RouterProvider`。
- `AppLayout` 先执行 `initAuth()`；无用户时渲染 `WelcomePage`，有用户才进入应用壳。
- 侧边导航 `AppShellNav` 按角色过滤菜单（`ADMIN`/`TEACHER`/`STUDENT`）。

### 创作工作台流程

- 首页 `/` 或导航进入 `/workspace`。
- 在工作台触发生成（前端 `generateContent` 调用 `/api/v1/generate`）。
- 结果进入历史与详情（`/history`、`/api/v1/history`、`/api/v1/tasks/{taskId}`）。

### 剧本工程主流程

从 `scriptProjectWorkflowSteps` 可见标准流程：

1. `/script-projects` 项目列表与筛选。
2. `/script-projects/new` 新建项目（文本或上传）。
3. `/script-projects/:projectId/global-settings` 全局设定。
4. `/script-projects/:projectId` 总览。
5. `/script-projects/:projectId/preview` 剧本预览与完善。
6. `/script-projects/:projectId/assets` 资产与关键帧。
7. `/script-projects/:projectId/video` 视频生成。
8. `/script-projects/:projectId/dubbing` 配音。
9. `/script-projects/:projectId/lip-sync` 口型同步。
10. `/script-projects/:projectId/final-composition` 剪辑。
11. `/script-projects/:projectId/export` 导出。

### 教学流程

1. `/courses` 课程列表。
2. `/courses/:courseId` 课程详情与作业。
3. `/courses/:courseId/assignments/:assignmentId` 提交、评分、统计。

### 轻量闭环流程（Storyboard Lite）

- 页面入口 `/tools/storyboard-lite`。
- API 流程：创建会话 → 保存剧本 → 生成关键帧 → 确认关键帧 → 生成视频（`/api/v1/storyboard-lite/sessions/*`）。

## 6. 外部依赖

### 基础设施依赖

- MySQL（运行时数据库，`application.yml` + `mysql-connector-j`）。
- Flyway（数据库迁移）。
- 本地文件存储（默认），并保留存储提供商配置项。

### 第三方模型/网关依赖（代码内置目录）

`ProviderCatalog` 内置了多服务商定义与网关元数据，包括：

- OpenAI、Anthropic、DeepSeek、Qwen、MiniMax、GLM、Kimi、Ollama、LM Studio。
- Azure OpenAI、AWS Bedrock、Vertex AI。
- Ark（火山方舟）、OneLinkAI、Vidu、Moark、Kling 及部分 OneLink 代理路径。

### 认证与平台依赖

- 社交登录端点配置：OneLinkAI OAuth、微信 OAuth（`aigc.auth.social.*`）。
- JWT 令牌能力（`jjwt-*`）。
- 加密与密码处理（`spring-security-crypto`、自定义加密配置）。

### 可配置外部地址（配置文件事实）

- `aigc.ark.base-url`（默认 `https://ark.cn-beijing.volces.com`）。
- `aigc.onelinkai.base-url`（默认 `https://api.onelinkai.cloud`）。
- `aigc.comfy.base-url`（默认 `http://127.0.0.1:8188`）。
