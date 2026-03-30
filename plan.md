# AIGCmanju 剧本能力扩展实施计划

## 1. 目标概述

本次新增功能的目标，不是简单在现有“提示词生成”能力上叠加几个按钮，而是为 `AIGCmanju` 新增一条完整的 **剧本驱动视频生产流水线**：

1. 用户上传自己的剧本文本；
2. 点击“完善剧本”后，后台调用 AI 将原始剧本整理为适合视频生成的结构化视频脚本；
3. 在“预览脚本”页面，用户可分别抽取并生成：
  - 视频道具
  - 视频背景
  - 人物形象
4. 抽取结果需要存储，并进一步调用 AI 生图生成关键帧；
5. 关键帧确认后，将“关键帧 + 视频脚本”提交给视频模型进行视频生成；
6. 对已拆解的多个脚本片段进行 **并发多任务视频生成**；
7. 用户上传的文本、AI 生成的脚本、抽取结果、关键帧、视频等数字资产都需要 **本地持久化存储**；
8. 所有新功能默认优先调用用户在系统中配置完成的 API/模型路由，而不是写死到某个固定厂商。

---

## 2. 现状梳理

### 2.1 当前 `AIGCmanju` 能力现状

当前项目已经具备以下基础，但仍主要围绕“单次提示词生成”展开：

- 后端已有 Spring Boot 基础接口和本地文件仓库能力；
- 后端已有文本、图片、视频的简单生成接口；
- 后端已有连接配置、模型配置、路由配置等能力；
- 前端已有生成工作台、模型配置、历史记录、路由控制台；
- 存储层已支持基于本地 JSON 的持久化；
- 当前能力更偏“Prompt -> 文本/图片/视频结果”，尚未形成“剧本项目 -> 资产抽取 -> 关键帧 -> 视频流水线”。

### 2.2 当前关键边界

当前代码中的典型边界如下：

- 后端生成入口：`aigc-server/src/main/java/com/example/aigc/controller/GenerationController.java`
- 后端生成服务：`aigc-server/src/main/java/com/example/aigc/service/impl/GenerationServiceImpl.java`
- 当前任务实体：`aigc-server/src/main/java/com/example/aigc/entity/GenerationTask.java`
- 当前前端工作台：`aigc-site/src/views/WorkspaceView.vue`
- 当前前端输入面板：`aigc-site/src/components/workspace/PromptPanel.vue`

### 2.3 参考工程与可复用方向

提示词策略和产品流程参考以下工程，不直接照搬前端代码，而是提炼能力模型和提示词设计思路：

- `BigBanana-AI-Director-feature_prompttemplate/docs/AI剧本编辑功能说明.md`
- `BigBanana-AI-Director-feature_prompttemplate/docs/AI关键帧优化功能说明.md`
- `BigBanana-AI-Director-feature_prompttemplate/docs/镜头拆分功能实现说明.md`
- `BigBanana-AI-Director-feature_prompttemplate/docs/镜头提示词.md`
- `BigBanana-AI-Director-feature_prompttemplate/services/ai/scriptService.ts`
- `BigBanana-AI-Director-feature_prompttemplate/services/ai/shotService.ts`
- `BigBanana-AI-Director-feature_prompttemplate/services/ai/visualService.ts`
- `BigBanana-AI-Director-feature_prompttemplate/services/modelService.ts`

参考后可提炼出四类核心提示词能力：

1. 剧本完善 / 改写 / 结构化；
2. 道具 / 背景 / 人物信息抽取；
3. 关键帧描述优化与生图提示词生成；
4. 镜头拆分 / 子脚本拆解 / 视频生成输入组装。

---

## 3. 本次功能范围定义

### 3.1 必做范围

本轮实施建议覆盖以下完整闭环：

1. **剧本项目创建**
  - 支持文本粘贴；
  - 支持上传 `.txt` / `.md` / `.docx`（如本阶段不做 `.docx` 解析，可先落为二阶段）；
  - 创建一个新的“剧本项目”。
2. **完善剧本**
  - 用户点击“完善剧本”；
  - 后端调用文本大模型；
  - 输出适合视频生成的结构化视频脚本；
  - 同时保存原始剧本和 AI 完善结果。
3. **预览脚本**
  - 展示完善后的脚本；
  - 展示结构化镜头/片段/场景信息；
  - 支持后续从此页面发起抽取和生成功能。
4. **抽取数字资产**
  - 从视频脚本中分别抽取：
    - 视频道具
    - 视频背景
    - 人物形象
  - 每类抽取结果均可独立保存、编辑、重新生成。
5. **关键帧生成**
  - 针对人物/背景/道具抽取结果生成关键帧；
  - 支持保存关键帧提示词、生成记录、图片文件；
  - 支持用户确认选定最终关键帧。
6. **脚本拆解与并发视频生成**
  - 基于完善后的剧本和已确认关键帧；
  - 将脚本拆解为多个可并发的子脚本/子镜头任务；
  - 多任务并发提交给视频模型；
  - 聚合展示任务进度和结果。
7. **本地存储**
  - 原始剧本；
  - 完善后的视频脚本；
  - 抽取信息；
  - 关键帧提示词与图片；
  - 视频生成请求、任务状态、结果文件。

### 3.2 暂不作为首轮强依赖的范围

以下可放在第二阶段或第三阶段：

- 自动视频片段拼接为一个完整成片；
- 时间轴拖拽式分镜编辑器；
- 关键帧多版本 A/B 对比；
- `.docx` / `.pdf` 的复杂解析兼容；
- 云端对象存储同步；
- 项目协作与多人编辑。

---

## 4. 核心产品流程设计

建议新增一条独立于现有 `/workspace` 的“剧本工作流”，而不是直接改坏现有生成工作台。

### 4.1 用户主流程

#### 阶段 A：创建项目

1. 用户进入“剧本项目”页面；
2. 上传剧本或粘贴文本；
3. 填写项目名、风格、期望视频时长、视频比例等基础信息；
4. 创建项目并保存原文。

#### 阶段 B：完善剧本

1. 用户点击“完善剧本”；
2. 后端基于参考提示词，将原始文本转成更适合视频生成的脚本；
3. 输出建议包含：
  - 标题
  - 故事摘要
  - 场景列表
  - 人物列表
  - 道具列表
  - 分段视频脚本
  - 每段的情绪、动作、镜头建议
4. 保存结构化结果与 Markdown/纯文本结果。

#### 阶段 C：预览与抽取

1. 用户在预览脚本页面查看脚本；
2. 点击“生成人物形象”；
3. 点击“生成视频背景”；
4. 点击“生成视频道具”；
5. 系统分别抽取对应结构化信息；
6. 抽取结果存储到本地，并可手动修订。

#### 阶段 D：关键帧生成与确认

1. 系统根据每个资产项生成关键帧提示词；
2. 调用图片模型生成关键帧；
3. 用户选择/确认每项资产的最终关键帧；
4. 标记项目已具备视频生成前置条件。

#### 阶段 E：脚本拆解与并发视频生成

1. 系统将完善后的视频脚本拆为多个子任务；
2. 每个子任务绑定对应的关键帧、角色、背景、道具；
3. 并发提交给视频模型；
4. 后端持续轮询并更新状态；
5. 前端实时看到每个分段的生成进度；
6. 完成后可查看分段视频结果。

---

## 5. 总体架构设计

## 5.1 架构原则

- **保留现有工作台**：不破坏当前图文/视频单次生成能力；
- **新增剧本项目域模型**：把“任务”升级为“项目 + 资产 + 流水线”；
- **复用现有路由与模型配置系统**：新功能默认走用户配置的 API；
- **本地文件存储优先**：先跑通单机可用性；
- **异步任务化**：关键帧生成、视频生成都不能阻塞单请求；
- **状态可恢复**：服务重启后能从本地文件恢复项目和任务状态。

## 5.2 后端新增模块建议

建议在 `aigc-server` 内新增以下业务模块：

- `script-project`：剧本项目基础管理；
- `script-ai`：剧本完善、改写、结构化；
- `asset-extract`：人物/背景/道具抽取；
- `keyframe`：关键帧提示词生成、图片生成、确认；
- `storyboard`：脚本拆分、子镜头生成；
- `video-pipeline`：并发视频生成编排；
- `asset-storage`：文件存储、路径管理、元数据索引；
- `prompt-template`：提示词模板管理；
- `ai-routing`：文本/图片/视频能力统一路由解析。

## 5.3 前端新增模块建议

建议在 `aigc-site` 中新增一套剧本项目视图：

- 剧本项目列表页；
- 新建项目/上传剧本页；
- 剧本预览与完善页；
- 资产抽取与关键帧页；
- 视频生成进度页；
- 项目详情页。

同时新增独立 Pinia Store，而不是把所有状态塞进现有 `generation` store。

---

## 6. 数据模型设计

建议新增一组“项目化”实体，而不是继续复用单一 `GenerationTask`。

### 6.1 核心实体

#### 1）ScriptProject（剧本项目）

建议字段：

- `projectId`
- `name`
- `status`
- `sourceType`（text/upload）
- `originalScriptFileId`
- `refinedScriptFileId`
- `scriptSummary`
- `visualStyle`
- `aspectRatio`
- `targetDuration`
- `language`
- `createdAt`
- `updatedAt`

#### 2）ScriptDocumentVersion（脚本文档版本）

用于保存不同阶段文本：

- `documentId`
- `projectId`
- `versionType`（original/refined/preview）
- `format`（txt/md/json）
- `fileId`
- `contentDigest`
- `createdAt`

#### 3）ExtractedAsset（抽取资产）

统一存储人物/背景/道具：

- `assetId`
- `projectId`
- `assetType`（character/background/prop）
- `name`
- `description`
- `sourceShotId`
- `tags`
- `promptDraft`
- `status`
- `metadata`
- `createdAt`

#### 4）KeyframeRecord（关键帧记录）

- `keyframeId`
- `projectId`
- `assetId`
- `shotId`
- `promptText`
- `negativePrompt`
- `imageFileId`
- `selected`
- `status`
- `providerTaskId`
- `modelName`
- `createdAt`

#### 5）StoryboardShot（镜头/子脚本）

- `shotId`
- `projectId`
- `parentShotId`
- `sequenceNo`
- `title`
- `scriptText`
- `actionSummary`
- `cameraMovement`
- `characterRefs`
- `backgroundRefs`
- `propRefs`
- `keyframeRefs`
- `status`

#### 6）VideoSegmentTask（视频分段任务）

- `segmentTaskId`
- `projectId`
- `shotId`
- `requestPayloadFileId`
- `resultVideoFileId`
- `providerTaskId`
- `status`
- `retryCount`
- `modelName`
- `startedAt`
- `finishedAt`

#### 7）PipelineRun（流水线执行记录）

- `pipelineRunId`
- `projectId`
- `pipelineType`
- `status`
- `currentStage`
- `totalCount`
- `successCount`
- `failedCount`
- `errorMessage`
- `createdAt`
- `updatedAt`

### 6.2 推荐状态枚举

建议增加枚举而不是用字符串散落：

- `ProjectStatus`
  - `DRAFT`
  - `SCRIPT_REFINING`
  - `SCRIPT_READY`
  - `ASSET_EXTRACTING`
  - `ASSET_READY`
  - `KEYFRAME_GENERATING`
  - `KEYFRAME_READY`
  - `VIDEO_GENERATING`
  - `COMPLETED`
  - `FAILED`
- `AssetStatus`
  - `PENDING`
  - `EXTRACTED`
  - `KEYFRAME_GENERATING`
  - `KEYFRAME_READY`
  - `CONFIRMED`
  - `FAILED`
- `SegmentTaskStatus`
  - `PENDING`
  - `QUEUED`
  - `RUNNING`
  - `SUCCESS`
  - `FAILED`

---

## 7. 本地存储方案

当前项目已经有 `aigc.storage.data-dir`，建议在其下新增项目化目录结构。

### 7.1 目录结构建议

```text
${aigc.storage.data-dir}/
  script-projects/
    index.json
    {projectId}/
      project.json
      documents/
        original-script.txt
        refined-script.md
        refined-script.json
        preview-script.json
      assets/
        characters.json
        backgrounds.json
        props.json
      keyframes/
        {assetId}/
          prompt.json
          keyframe-01.png
          keyframe-02.png
          selected.json
      shots/
        shots.json
      video/
        segment-tasks.json
        {shotId}/
          request.json
          response.json
          result.mp4
      exports/
        timeline.json
```

### 7.2 存储要求

- **原始剧本必须落盘**；
- **AI 完善后的文本版和 JSON 版都要落盘**；
- **道具/背景/人物抽取结果分别存储 JSON**；
- **关键帧图片文件和提示词文件都要存储**；
- **视频任务请求与结果文件都要存储**；
- **路径元数据需可追溯到项目**；
- **删除项目时支持一并清理目录**。

### 7.3 文件访问建议

为了便于前端预览本地图片/视频，后端建议新增：

- 静态文件代理接口；
- 文件下载接口；
- 媒体预览接口。

---

## 8. 模型路由与“默认调用用户配置 API”方案

这是本次实施的关键约束：**新能力不能默认写死某个外部厂商，而要优先使用用户已经在系统里配置完成的 API 与模型。**

### 8.1 路由原则

按照能力类型自动解析：

- 剧本完善 / 资产抽取 / 镜头拆分：走 **文本大模型**
- 关键帧生成：走 **图片模型**
- 视频生成：走 **视频模型**

### 8.2 推荐解析顺序

1. 项目级显式指定模型；
2. 系统中已启用的对应能力模型；
3. 按当前路由顺序选中可用连接；
4. 若用户未配置任何连接，再退回现有 `ark` 默认配置（仅兼容兜底）。

### 8.3 后端建议新增统一路由服务

建议新增类似：

- `AiCapabilityRoutingService`
- `ResolvedAiModel`
- `AiInvokeContext`

统一负责：

- 根据能力选择连接；
- 返回连接信息、模型信息、供应商类型；
- 记录本次任务使用了哪个连接和模型；
- 保证新能力与现有路由系统一致。

### 8.4 落地要求

所有新增服务不能直接自己拼固定模型名，而是必须通过统一路由解析获取：

- `text/chat` 模型；
- `image` 模型；
- `video` 模型。

---

## 9. 提示词模板接入方案

参考 `BigBanana-AI-Director-feature_prompttemplate`，建议在 `aigc-server` 中建立自己的提示词模板层。

### 9.1 提示词模板目录建议

建议存放于：

```text
aigc-server/src/main/resources/prompts/
  script/
    refine-system.md
    refine-user.md
    rewrite-system.md
  extract/
    character.md
    background.md
    prop.md
  keyframe/
    character-keyframe.md
    background-keyframe.md
    prop-keyframe.md
    shot-keyframe-optimize.md
  storyboard/
    split-shot.md
    build-video-segment.md
```

### 9.2 各类提示词职责

#### 1）剧本完善提示词

目标：

- 把用户上传的原始剧本改造成更适合视频生成的脚本；
- 输出结构化 JSON；
- 同时可生成适合页面展示的 Markdown 文本。

#### 2）抽取提示词

人物、背景、道具分别使用独立模板，避免混在一个提示词中导致结构不稳定。

要求：

- 输出严格 JSON；
- 每一项有唯一 ID；
- 每一项包含名称、描述、场景归属、视觉要点、风格提示。

#### 3）关键帧提示词

参考 BigBanana 的关键帧优化思路，要求提示词综合：

- 场景信息；
- 叙事动作；
- 镜头运动；
- 角色状态；
- 光影和色彩；
- 风格统一要求。

#### 4）镜头拆分提示词

参考 BigBanana 的镜头拆分方案，要求：

- 输出多个子镜头；
- 每个子镜头包含动作摘要、镜头景别、运镜、主体对象；
- 每个子镜头可直接成为视频任务输入。

### 9.3 模板使用方式

建议做一个 `PromptTemplateService`，职责如下：

- 模板加载；
- 变量渲染；
- JSON 输出约束；
- 版本管理；
- 后续支持运营调整模板内容而不改 Java 代码。

---

## 10. 后端接口设计

建议采用项目化 API，而不是把所有操作继续塞进 `/generate`。

### 10.1 项目管理接口

- `POST /api/v1/script-projects`
  - 创建剧本项目
- `POST /api/v1/script-projects/upload`
  - 上传剧本文本文件
- `GET /api/v1/script-projects`
  - 项目列表
- `GET /api/v1/script-projects/{projectId}`
  - 项目详情
- `DELETE /api/v1/script-projects/{projectId}`
  - 删除项目及本地文件

### 10.2 剧本处理接口

- `POST /api/v1/script-projects/{projectId}/refine`
  - 完善剧本
- `POST /api/v1/script-projects/{projectId}/rewrite`
  - 可选：重写剧本
- `GET /api/v1/script-projects/{projectId}/script`
  - 获取剧本详情与结构化结果
- `PUT /api/v1/script-projects/{projectId}/script`
  - 用户手动修改后保存

### 10.3 资产抽取接口

- `POST /api/v1/script-projects/{projectId}/assets/extract/characters`
- `POST /api/v1/script-projects/{projectId}/assets/extract/backgrounds`
- `POST /api/v1/script-projects/{projectId}/assets/extract/props`
- `GET /api/v1/script-projects/{projectId}/assets`
- `PUT /api/v1/script-projects/{projectId}/assets/{assetId}`

### 10.4 关键帧接口

- `POST /api/v1/script-projects/{projectId}/assets/{assetId}/keyframes/generate`
- `GET /api/v1/script-projects/{projectId}/keyframes`
- `POST /api/v1/script-projects/{projectId}/keyframes/{keyframeId}/confirm`
- `POST /api/v1/script-projects/{projectId}/keyframes/{keyframeId}/regenerate`

### 10.5 镜头与视频接口

- `POST /api/v1/script-projects/{projectId}/shots/split`
- `GET /api/v1/script-projects/{projectId}/shots`
- `POST /api/v1/script-projects/{projectId}/video/generate`
- `GET /api/v1/script-projects/{projectId}/video/tasks`
- `POST /api/v1/script-projects/{projectId}/video/tasks/{segmentTaskId}/retry`

### 10.6 文件接口

- `GET /api/v1/files/{fileId}`
- `GET /api/v1/files/{fileId}/download`

### 10.7 进度推送接口

建议二选一：

1. 首版先用轮询：
  - `GET /api/v1/script-projects/{projectId}/pipeline-status`
2. 第二版升级为 SSE：
  - `GET /api/v1/script-projects/{projectId}/events`

首轮建议优先轮询，降低实现复杂度。

---

## 11. 后端实现拆分计划

### 11.1 第一层：领域与仓库层

新增：

- 新实体类；
- 新枚举；
- 新 DTO；
- 新仓库接口；
- 文件仓库实现。

建议参考现有 `FileGenerationTaskRepository` 的实现模式，增加：

- `ScriptProjectRepository`
- `ScriptDocumentRepository`
- `ExtractedAssetRepository`
- `KeyframeRecordRepository`
- `VideoSegmentTaskRepository`

如果想减少仓库数量，也可以采用“项目聚合根 + 单项目 JSON”方式：

- 一个项目目录下一个 `project.json`
- 其余资产以局部 JSON 存储

首轮更建议“项目聚合 + 文件分目录”方案，便于定位问题和人工排查。

### 11.2 第二层：AI 服务层

建议新增以下服务：

- `ScriptProjectService`
- `ScriptRefineService`
- `AssetExtractionService`
- `KeyframeGenerationService`
- `StoryboardSplitService`
- `VideoPipelineService`
- `LocalAssetFileService`
- `PromptTemplateService`
- `AiCapabilityRoutingService`

### 11.3 第三层：编排层

新增一个面向流水线的总编排服务：

- `ScriptProductionOrchestrator`

职责：

- 控制“完善剧本 -> 抽取 -> 关键帧 -> 拆分 -> 视频生成”的阶段推进；
- 做状态变更；
- 汇总失败原因；
- 提供可重试入口；
- 保证并发任务的一致性。

### 11.4 第四层：控制器层

建议新增控制器：

- `ScriptProjectController`
- `ScriptAssetController`
- `KeyframeController`
- `VideoPipelineController`
- `FileAssetController`

---

## 12. 并发视频生成设计

用户明确提出：**关键帧确定后，多任务并发生成已拆解的多个脚本，配合关键帧 -> 生成视频。**

这是本次方案的技术重点。

### 12.1 并发触发条件

只有满足以下条件时允许启动：

- 项目已存在完善后的视频脚本；
- 所需关键帧均已确认；
- 拆分后的子脚本列表已生成；
- 视频模型能力可用。

### 12.2 并发执行方案

建议在 Spring Boot 中新增：

- `ThreadPoolTaskExecutor`
- `@Async` 或 `CompletableFuture`

推荐参数做成配置项：

- `aigc.pipeline.video.max-parallel=3`
- `aigc.pipeline.video.poll-interval-ms=3000`
- `aigc.pipeline.video.max-retries=2`

### 12.3 并发流程

1. 根据视频脚本生成多个 `StoryboardShot`；
2. 为每个 `shot` 组装视频生成请求；
3. 生成多个 `VideoSegmentTask`；
4. 提交到线程池并发调用视频模型；
5. 若外部视频模型是异步任务型接口，则保存 `providerTaskId`；
6. 统一轮询外部状态；
7. 每个子任务独立更新成功/失败；
8. 所有子任务结束后，汇总项目状态。

### 12.4 失败处理策略

建议采用“分段失败不拖垮全项目”的策略：

- 某个子任务失败，仅标记该片段失败；
- 用户可单独重试失败片段；
- 全部任务成功后项目才标为完成；
- 若部分失败，项目标为 `PARTIAL_FAILED` 或保留 `VIDEO_GENERATING` + 明确失败数。

### 12.5 幂等性要求

避免用户连续点击造成重复任务：

- 同一项目同一阶段如已有运行中的 `PipelineRun`，不重复提交；
- 支持显式“重新生成”时才创建新一轮任务；
- 每一轮生成带 `pipelineRunId`。

---

## 13. 前端页面与交互设计

## 13.1 路由建议

建议新增路由：

- `/script-projects`
- `/script-projects/new`
- `/script-projects/:projectId`
- `/script-projects/:projectId/preview`
- `/script-projects/:projectId/assets`
- `/script-projects/:projectId/video`

### 13.2 页面拆分建议

#### 1）项目列表页

展示：

- 项目名称
- 当前阶段
- 更新时间
- 缩略图/状态摘要

#### 2）新建项目页

功能：

- 文本输入
- 文件上传
- 基础配置
- 创建项目
- “完善剧本”按钮

#### 3）剧本预览页

功能：

- 原始剧本 / 完善后剧本切换查看
- 显示结构化片段
- 触发抽取：
  - 生成人物形象
  - 生成视频背景
  - 生成视频道具

#### 4）资产页

功能：

- 资产分类 tab：人物 / 背景 / 道具
- 显示抽取结果卡片
- 查看提示词
- 生成关键帧
- 重新生成
- 确认选中

#### 5）视频生成页

功能：

- 展示已拆分脚本片段
- 展示每个片段绑定的关键帧
- 启动并发生成
- 查看每个片段状态
- 视频预览
- 失败重试

### 13.3 前端组件建议

建议新增：

- `ScriptProjectForm.vue`
- `ScriptEditorPanel.vue`
- `ScriptPreviewPanel.vue`
- `AssetExtractionPanel.vue`
- `AssetCard.vue`
- `KeyframeCard.vue`
- `VideoSegmentCard.vue`
- `PipelineProgressBar.vue`

### 13.4 状态管理建议

建议新增 `useScriptProjectStore`，负责：

- 当前项目详情；
- 剧本版本；
- 资产列表；
- 关键帧列表；
- 视频任务列表；
- 各阶段 loading 状态；
- 轮询 pipeline 状态。

不要继续把这套复杂工作流放到现有 `generation.ts` 中。

---

## 14. 关键实现细节建议

### 14.1 剧本完善结果建议采用“双输出”

为了兼顾可读性和程序处理，建议 AI 输出两份内容：

1. **JSON 结构化结果**
  - 供后端继续抽取、拆分、生成；
2. **Markdown/纯文本剧本**
  - 供前端预览和用户微调。

### 14.2 抽取建议“分能力调用”，不要一次性全抽

因为用户界面上需要分别点：

- 人物形象
- 视频背景
- 视频道具

因此后端也应提供分开的接口、分开的提示词、分开的存储文件。

### 14.3 关键帧建议分两类

首轮建议先支持资产级关键帧：

- 人物关键帧
- 背景关键帧
- 道具关键帧

后续可扩展镜头级关键帧：

- 起始帧
- 结束帧

如果首轮资源允许，也可以在视频片段层追加“镜头级关键帧组装”。

### 14.4 视频生成请求组装建议

每个视频子任务的请求应至少包含：

- 对应脚本片段文本；
- 角色信息；
- 背景信息；
- 道具信息；
- 关键帧图片引用；
- 运镜描述；
- 视频风格参数；
- 视频比例和时长。

### 14.5 JSON 校验必须加上

AI 输出非常容易结构漂移，因此每个 AI 步骤都建议：

- 明确 JSON Schema；
- 后端解析校验；
- 校验失败时重试 1~2 次；
- 仍失败则将原始响应落盘，便于排障。

---

## 15. 推荐实施阶段

建议按 6 个阶段实施，避免一次性改动过大。

### 阶段 1：项目骨架与存储

目标：

- 新增项目域模型；
- 新增项目接口；
- 打通本地目录存储；
- 前端新增项目入口和基础页面。

交付物：

- 可创建剧本项目；
- 可上传/保存原始剧本；
- 可查看项目详情。

### 阶段 2：完善剧本

目标：

- 接入参考提示词；
- 新增剧本完善接口；
- 保存完善后的脚本；
- 前端支持点击“完善剧本”和预览结果。

交付物：

- 原始剧本 -> 完善剧本 可跑通；
- 结构化 JSON 可存储。

### 阶段 3：资产抽取

目标：

- 人物/背景/道具三类独立抽取；
- 抽取结果本地存储；
- 前端资产页面展示。

交付物：

- 可分别点击“视频道具”“视频背景”“人物形象”；
- 抽取结果可查看和保存。

### 阶段 4：关键帧生成

目标：

- 生成资产关键帧；
- 存储图片和提示词；
- 支持用户确认。

交付物：

- 每个资产项都可生成关键帧；
- 关键帧文件可预览；
- 用户可确认选中结果。

### 阶段 5：镜头拆分与并发视频生成

目标：

- 将脚本拆分为多个视频子任务；
- 并发提交视频模型；
- 轮询和状态聚合。

交付物：

- 可启动并发视频生成；
- 每个子任务状态可见；
- 视频结果本地落盘。

### 阶段 6：联调、优化与验收

目标：

- 完整串联剧本到视频流程；
- 加上失败重试、轮询优化、页面提示；
- 完成基础测试与验收。

---

## 16. 验收标准

当满足以下条件时，认为该功能完成首轮交付：

### 16.1 剧本能力

- 用户可以上传或粘贴剧本；
- 点击“完善剧本”后能生成结构化视频脚本；
- 原始剧本和完善剧本都已本地存储。

### 16.2 抽取能力

- 在“预览脚本页面”能分别点击：
  - 生成人物形象
  - 生成视频背景
  - 生成视频道具
- 三类抽取结果都能被存储和重新读取。

### 16.3 关键帧能力

- 抽取结果可生成关键帧；
- 关键帧提示词、图片文件、关联关系都能存储；
- 用户可确认最终关键帧。

### 16.4 视频能力

- 关键帧确认后，系统能拆解多个子脚本；
- 多个子任务能并发调用视频模型；
- 每个任务状态可跟踪；
- 视频结果可预览或下载。

### 16.5 路由能力

- 默认优先调用用户已配置的 API 和模型；
- 文本、图片、视频能力分别走对应模型能力路由；
- 未配置时才允许使用系统默认兜底。

### 16.6 存储能力

- 用户上传文本本地可追溯；
- AI 生成的脚本本地可追溯；
- 抽取 JSON 本地可追溯；
- 关键帧图片本地可追溯；
- 视频文件本地可追溯。

---

## 17. 风险与应对

### 风险 1：AI 返回 JSON 不稳定

应对：

- 使用严格模板；
- 要求固定字段；
- 后端增加 JSON 校验与重试；
- 保存原始响应便于排障。

### 风险 2：视频模型接口差异大

应对：

- 封装统一视频调用适配层；
- 在路由解析后按 provider 分别组装请求；
- 优先兼容现有已经接入的 provider。

### 风险 3：并发任务过多导致外部限流

应对：

- 设置最大并发数；
- 增加重试与退避；
- 在页面提示用户当前任务数。

### 风险 4：本地文件快速膨胀

应对：

- 目录结构清晰；
- 支持删除项目时递归清理；
- 后续再考虑归档策略。

### 风险 5：一次性改造面过大

应对：

- 严格按阶段实施；
- 新增剧本工作流，不直接侵入现有 `/workspace`；
- 每阶段可独立验证。

---

## 18. 推荐落地结论

综合当前 `AIGCmanju` 的代码基础和参考工程能力，最可行的路径是：

1. **保留现有生成工作台不动**；
2. **新增“剧本项目”工作流**；
3. **后端先扩项目模型、文件存储、统一路由解析**；
4. **提示词模板参考 BigBanana，但在 Spring Boot 内部重建模板体系**；
5. **先打通“剧本完善 -> 抽取 -> 关键帧 -> 并发视频生成”闭环**；
6. **保证所有新功能默认优先调用用户配置完成的 API/模型能力**。

这个方案既能满足你提出的完整业务目标，也能最大程度复用当前项目已经有的连接配置、模型配置、路由与本地存储能力，风险可控、可分阶段落地。