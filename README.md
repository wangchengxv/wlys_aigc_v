# AIGC 图文生成平台（V1）

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
cd /Users/xingyi/Desktop/枣庄
chmod +x start.command
./start.command
```

### Windows PowerShell 启动

```powershell
cd D:\path\to\枣庄
.\start.ps1
```

## 3. 接口说明（V1）

- `POST /api/v1/generate` 提交生成任务
- `GET /api/v1/history?page=1&pageSize=20&mode=all` 查询历史记录
- `GET /api/v1/tasks/{taskId}` 查询任务详情
- `GET /api/v1/models/image` 获取图片模型列表（含默认模型）
- `GET /api/v1/models/video` 获取视频模型列表（含默认模型）
- `GET /api/v1/health` 健康检查

## 4. 当前实现说明

- 前端支持：首页、工作台、历史、设置页
- 工作台支持：仅文本/仅图片/图文一起、风格标签、长度和尺寸参数、模型可选/自定义、复制下载、重生成、收藏
- 后端支持：参数校验、敏感词拦截、统一返回结构、历史分页（内存仓储）
- 文本仍为 Mock 生成；图片已接入火山方舟 `images/generations` 接口
- 视频已接入"提交任务 + 轮询查询"流程，兼容即梦视频类异步接口
- 默认图片模型：`doubao-seedream-5-0-260128`

## 5. 模型与密钥配置

后端配置文件：`aigc-server/src/main/resources/application.yml`

已支持：
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

注意：请勿把真实 API Key 提交到仓库。

视频任务等待窗口说明：默认按 `40 × 3000ms ≈ 120s` 轮询后判定超时。