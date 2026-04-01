# AIGC 图文生成平台（V2）

本目录包含前后端两个子项目：

- `aigc-site`：Vue 3 前端
- `aigc-server`：Spring Boot 后端

## 1. 启动后端

```bash
cd aigc-server
# 先配置火山方舟密钥（示例）
export ARK_API_KEY=你的密钥
mvn spring-boot:run
```

默认端口：`8080`

## 2. 启动前端

```bash
cd aigc-site
cp .env.example .env
npm install
npm run dev
```

默认端口：`5173`

## 一键启动（推荐）

```bash
cd /Users/xingyi/Desktop/枣庄
./start.sh
```

说明：

- 脚本会自动启动后端（后台运行）并启动前端开发服务
- 若不存在 `.env`，会自动从 `.env.example` 生成
- 若前端未安装依赖，会自动执行 `npm install`
- 按 `Ctrl + C` 结束时，会自动关闭本次脚本启动的后端进程

### macOS 双击启动（Finder）

```bash
chmod +x start.command
./start.command
```

### Windows PowerShell 启动

```powershell
cd D:\path\to\枣庄
.\start.ps1
```

## 3. 接口说明（V2）

- `POST /api/v1/generate` 提交生成任务
- `GET /api/v1/history?page=1&pageSize=20&mode=all` 查询历史记录
- `GET /api/v1/tasks/{taskId}` 查询任务详情
- `DELETE /api/v1/tasks/{taskId}` 删除任务记录
- `GET /api/v1/models/image` 获取图片模型列表（含默认模型）
- `GET /api/v1/models/video` 获取视频模型列表（含默认模型）
- `GET /api/v1/health` 健康检查
- `POST /api/v1/connections/{id}/test` 测试连接

### 新增：LLM Router 兼容接口

- `POST /v1/chat/completions` OpenAI 兼容聊天代理
- `POST /v1/messages` Anthropic 兼容聊天代理
- `GET /v1/models` 聚合模型列表

### 新增：路由控制台接口

- `GET /api/v1/router/keys` 路由 API Key 列表
- `POST /api/v1/router/keys` 创建路由 API Key
- `PATCH /api/v1/router/keys/{id}` 启用/禁用路由 API Key
- `DELETE /api/v1/router/keys/{id}` 删除路由 API Key
- `GET /api/v1/router/routing` 获取路由策略
- `PUT /api/v1/router/routing` 更新路由策略
- `GET /api/v1/router/logs` 查询代理日志
- `GET /api/v1/router/stats` 查询代理统计
- `GET /api/v1/router/config/export` 导出路由配置
- `POST /api/v1/router/config/import` 导入路由配置

### 新增：剧本工程工作流接口

- `POST /api/v1/script-projects` 文本创建剧本工程
- `POST /api/v1/script-projects/upload` 上传 `.txt` / `.md` / `.docx` 创建项目
- `GET /api/v1/script-projects` 获取剧本工程列表
- `GET /api/v1/script-projects/{projectId}` 获取项目聚合详情
- `DELETE /api/v1/script-projects/{projectId}` 删除项目及其本地文件
- `POST /api/v1/script-projects/{projectId}/refine` 完善剧本并生成结构化脚本
- `GET /api/v1/script-projects/{projectId}/script` 获取原始/完善脚本
- `PUT /api/v1/script-projects/{projectId}/script` 手动保存完善剧本与结构化 JSON
- `POST /api/v1/script-projects/{projectId}/assets/extract/characters|backgrounds|props` 抽取三类资产
- `GET /api/v1/script-projects/{projectId}/assets` 获取资产列表
- `PUT /api/v1/script-projects/{projectId}/assets/{assetId}` 更新资产
- `POST /api/v1/script-projects/{projectId}/assets/{assetId}/keyframes/generate` 生成资产关键帧
- `GET /api/v1/script-projects/{projectId}/keyframes` 获取关键帧列表
- `POST /api/v1/script-projects/{projectId}/keyframes/{keyframeId}/confirm` 确认关键帧
- `POST /api/v1/script-projects/{projectId}/keyframes/{keyframeId}/regenerate` 重生成关键帧
- `POST /api/v1/script-projects/{projectId}/shots/split` 拆分镜头
- `GET /api/v1/script-projects/{projectId}/shots` 获取镜头列表
- `POST /api/v1/script-projects/{projectId}/video/generate` 启动并发视频生成
- `GET /api/v1/script-projects/{projectId}/video/tasks` 获取视频任务列表
- `POST /api/v1/script-projects/{projectId}/video/tasks/{segmentTaskId}/retry` 重试单个视频片段
- `GET /api/v1/script-projects/{projectId}/pipeline-status` 轮询流水线状态
- `GET /api/v1/files/{fileId}` 预览本地图片/视频/文档
- `GET /api/v1/files/{fileId}/download` 下载本地文件

## 4. 当前实现说明

- 前端支持：首页、工作台、历史、设置页
- 前端新增：`/script-projects` 剧本工程工作流，覆盖项目创建、剧本完善、资产抽取、关键帧确认、镜头拆分、并发视频生成
- 前端新增：路由控制台，可管理客户端 API Key、优先级路由、时间计划、日志统计、配置导入导出
- 工作台支持：仅文本/仅图片/图文一起、风格标签、长度和尺寸参数、模型可选/自定义、复制下载、重生成、收藏
- 模型配置支持：连接测试、模型能力标签（text/image/video）
- 后端支持：参数校验、敏感词拦截、统一返回结构、历史分页、本地 JSON 持久化
- 文本生成优先使用用户配置的文本模型；未配置时回退到本地 mock 文案
- 图片/视频生成优先使用用户配置的图片/视频模型；未配置时回退到火山方舟默认配置
- 新增 OpenAI / Anthropic 兼容代理能力，支持固定优先级、时间计划、故障转移、请求日志和统计
- 默认图片模型：`doubao-seedream-5-0-260128`

## 5. 模型与密钥配置

后端配置文件：`aigc-server/src/main/resources/application.yml`

已支持：

- `aigc.storage.data-dir`：本地持久化目录，默认 `${user.home}/.aigcmanju`
- `aigc.ark.base-url`：方舟接口基础地址
- `aigc.ark.api-key`：从环境变量 `ARK_API_KEY` 注入
- `aigc.ark.default-image-model`：默认模型（豆包）
- `aigc.ark.default-video-model`：默认视频模型
- `aigc.ark.video-model-options`：前端视频模型下拉可选项
- `aigc.ark.video-api-path`：视频任务提交接口
- `aigc.ark.video-result-api-path`：视频任务查询接口（支持 `{taskId}` 占位符）
- `aigc.ark.video-poll-max-attempts`：视频任务最大轮询次数（默认 40）
- `aigc.ark.video-poll-interval-ms`：视频任务轮询间隔（毫秒，默认 3000）
- `aigc.ark.image-model-options`：前端下拉可选模型
- `aigc.ark.response-format/size/stream/watermark/sequential-image-generation`：图像生成默认参数
- `aigc.pipeline.video.max-parallel`：剧本流水线视频并发数
- `aigc.pipeline.video.poll-interval-ms`：剧本流水线轮询间隔
- `aigc.pipeline.video.max-retries`：剧本流水线单片段最大重试次数

注意：请勿把真实 API Key 提交到仓库。

视频任务等待窗口说明：默认按 `40 × 3000ms ≈ 120s` 轮询后判定超时。

## 6. 使用建议

- 如果你希望工作台文本/图像/视频都优先走你自己的接口，请先在“模型配置”中创建连接和模型，并给模型打上对应能力标签。
- 如果你希望把本项目当成统一的 LLM 代理层使用，可在“路由控制台”里生成客户端 Key，再把外部应用的 `base_url` 指向本项目的 `/v1`。
- 所有配置、生成历史、路由日志都会持久化到 `aigc.storage.data-dir` 指定目录。

