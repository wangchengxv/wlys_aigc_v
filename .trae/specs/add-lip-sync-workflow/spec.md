# 口型同步工作流 Spec

## Why
当前仓库已经具备视频片段生成、TTS / 配音任务、统一媒体资源管理与导出页配音准备提示，但“视频片段 + 配音音频”仍是分离状态。缺少口型同步工作流后，项目只能停留在“有画面、有声音、不同步”的中间阶段，无法进一步推进到可交付成片。

## What Changes
- 为剧本工程新增口型同步工作流，支持基于镜头视频与配音音频生成口型同步结果
- 新增口型同步任务数据模型，记录来源视频、来源音频、状态、结果视频与失败原因
- 在生产编排中加入口型同步状态聚合，供后续成片编排与导出包复用
- 前端新增最小可用的口型同步页，可发起整项目同步、查看状态、预览结果与重试
- 导出页新增口型同步准备状态提示
- 本次不实现时间轴精编、背景音乐混音、整项目成片拼接，但要求保留后续对接点

## Impact
- Affected specs: 剧本工程、视频生成、TTS 配音、统一媒体资源、导出页、生产编排
- Affected code: `aigc-server/src/main/java/com/example/aigc/service/ScriptProductionOrchestrator.java`、`aigc-server/src/main/java/com/example/aigc/controller/VideoPipelineController.java`、`aigc-server/src/main/java/com/example/aigc/service/ScriptProjectService.java`、`aigc-site-react/src/pages/ScriptProjectDubbingPage.tsx`、`aigc-site-react/src/pages/ScriptProjectVideoPage.tsx`、`aigc-site-react/src/pages/ScriptProjectExportPage.tsx`、`aigc-site-react/src/stores/scriptProjectStore.ts`、`aigc-site-react/src/api/index.ts`

## ADDED Requirements
### Requirement: 剧本工程必须支持口型同步任务编排
系统 SHALL 为剧本工程提供独立的口型同步任务编排能力，使用户可以将镜头视频与对应配音音频合成为同步结果视频。

#### Scenario: 用户为项目发起口型同步
- **WHEN** 用户在剧本工程中选择生成口型同步
- **AND** 当前项目已经存在镜头视频结果与对应配音音频
- **THEN** 系统为项目创建一组口型同步任务
- **AND** 每个任务至少关联 `projectId`、`shotId`、来源视频文件、来源音频文件、状态、结果视频文件

### Requirement: 口型同步任务必须支持状态追踪与失败重试
系统 SHALL 为口型同步任务提供和视频片段任务、配音任务一致的最小状态管理能力，便于用户掌握进度并对失败任务重试。

#### Scenario: 口型同步任务执行失败
- **WHEN** 某个口型同步任务调用模型失败
- **THEN** 系统记录失败状态与错误原因
- **AND** 用户可以在界面上看到失败信息
- **AND** 用户可以单独重试失败任务

### Requirement: 口型同步结果必须纳入统一媒体资源管理
系统 SHALL 将口型同步结果视频写入统一媒体资源抽象，而不是新增旁路文件存储。

#### Scenario: 口型同步成功生成结果视频
- **WHEN** 口型同步任务成功返回结果视频
- **THEN** 系统将结果视频保存为统一媒体资源记录
- **AND** 结果包含 `storageProvider`、`bucketName`、`objectKey`、可访问链接等元数据
- **AND** 用户可以在项目页或导出页预览结果视频

### Requirement: 口型同步工作流必须校验输入依赖
系统 SHALL 在发起口型同步前校验镜头视频和配音音频是否齐备，避免无效任务进入执行队列。

#### Scenario: 缺少视频或音频时启动口型同步
- **WHEN** 用户发起口型同步
- **AND** 某个镜头缺少视频结果或配音结果
- **THEN** 系统拒绝创建对应任务
- **AND** 返回明确的缺失依赖提示

### Requirement: 前端必须提供最小可用的口型同步页
系统 SHALL 在前端提供单独的口型同步管理入口，使用户可以发起整项目同步、查看状态、预览结果并重试失败任务。

#### Scenario: 用户查看项目口型同步页
- **WHEN** 用户打开剧本工程的口型同步页面
- **THEN** 页面展示镜头列表、来源视频、来源配音、口型同步任务状态、结果视频入口与失败原因
- **AND** 用户可以发起整项目同步
- **AND** 用户可以对单条失败任务执行重试

## MODIFIED Requirements
### Requirement: 生产闭环必须从“视频 + 配音可衔接”升级为“视频 + 配音 + 口型同步可衔接”
系统 SHALL 在不破坏现有视频生成和配音工作流的前提下，将生产闭环扩展为“镜头视频、配音音频、口型同步结果”三类资产并存，为后续成片编排提供输入。

#### Scenario: 项目进入生产阶段
- **WHEN** 用户已完成镜头视频生成与配音生成
- **THEN** 系统允许继续生成口型同步任务
- **AND** 项目状态可以区分“视频 / 配音已就绪但口型同步未完成”和“口型同步已就绪”

### Requirement: 导出页必须展示口型同步准备状态
系统 SHALL 在现有导出页中增加口型同步准备状态提示，使用户明确当前项目离“可交付成片”还缺哪一步。

#### Scenario: 用户进入导出页
- **WHEN** 项目尚未完成全部口型同步任务
- **THEN** 页面提示口型同步未完成
- **AND** 引导用户进入口型同步页面继续处理

## REMOVED Requirements
### Requirement: 无
**Reason**: 本次为新增生产能力，不移除现有能力。
**Migration**: 无需迁移。
