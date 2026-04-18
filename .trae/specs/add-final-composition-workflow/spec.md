# 成片编排与镜头拼接 Spec

## Why
当前仓库已经具备镜头视频生成、TTS / 配音、口型同步与导出页逐镜预览，但所有结果仍停留在“按镜头分散存在”的阶段。缺少成片编排与镜头拼接后，项目无法从“镜头级素材齐备”推进到“项目级可回看、可交付的完整成片”。

## What Changes
- 为剧本工程新增成片编排工作流，支持按镜头顺序汇总项目级时间线
- 新增成片编排任务数据模型，记录镜头顺序、输入片段、状态、结果成片文件与失败原因
- 在生产编排中加上“成片已就绪”状态聚合，供后续导出包 / 成片交付复用
- 前端新增最小可用的成片编排页，可发起整项目编排、查看状态、预览成片与重试
- 导出页新增项目级成片预览与准备状态提示
- 本次不实现多轨混音、字幕烧录、复杂时间轴编辑与导出包打包，但要求保留后续对接点

## Impact
- Affected specs: 剧本工程、视频生成、配音、口型同步、统一媒体资源、导出页、生产编排
- Affected code: `aigc-server/src/main/java/com/example/aigc/service/ScriptProductionOrchestrator.java`、`aigc-server/src/main/java/com/example/aigc/controller/VideoPipelineController.java`、`aigc-server/src/main/java/com/example/aigc/service/ScriptProjectService.java`、`aigc-site-react/src/pages/ScriptProjectExportPage.tsx`、`aigc-site-react/src/stores/scriptProjectStore.ts`、`aigc-site-react/src/api/index.ts`

## ADDED Requirements
### Requirement: 剧本工程必须支持项目级成片编排任务
系统 SHALL 为剧本工程提供项目级成片编排能力，使用户可以将镜头级视频结果按顺序拼接为单一成片文件。

#### Scenario: 用户为项目发起成片编排
- **WHEN** 用户在剧本工程中选择生成成片
- **AND** 当前项目已经存在可用于导出的镜头结果视频
- **THEN** 系统创建项目级成片编排任务
- **AND** 任务至少关联 `projectId`、镜头顺序、输入片段列表、状态、结果成片文件

### Requirement: 成片编排必须优先使用更高完成度的镜头结果
系统 SHALL 在编排时优先使用口型同步结果；若某镜头没有口型同步结果，则可回退到镜头视频结果。

#### Scenario: 项目包含口型同步与普通镜头结果
- **WHEN** 系统汇总输入片段
- **THEN** 优先使用每个镜头最新成功的口型同步结果
- **AND** 若该镜头没有口型同步结果，则使用成功的视频片段结果

### Requirement: 成片编排任务必须支持状态追踪与失败重试
系统 SHALL 为成片编排任务提供最小状态管理能力，使用户可以判断当前进度并对失败任务重试。

#### Scenario: 成片编排任务失败
- **WHEN** 成片编排执行失败
- **THEN** 系统记录失败状态与错误原因
- **AND** 用户可以在界面上看到失败信息
- **AND** 用户可以重新发起编排

### Requirement: 成片结果必须纳入统一媒体资源管理
系统 SHALL 将成片结果视频保存到统一媒体资源抽象，而不是单独旁路落盘。

#### Scenario: 成片编排成功
- **WHEN** 系统成功生成项目级成片
- **THEN** 结果文件以统一媒体资源记录保存
- **AND** 返回 `storageProvider`、`bucketName`、`objectKey`、可访问链接等元数据
- **AND** 用户可以在导出页直接预览或打开该成片

### Requirement: 成片编排必须校验镜头输入依赖
系统 SHALL 在发起成片编排前校验项目中至少存在一条可用镜头结果，且镜头顺序可确定。

#### Scenario: 项目尚未生成可用镜头结果
- **WHEN** 用户发起成片编排
- **AND** 全部镜头都没有成功的视频或口型同步结果
- **THEN** 系统拒绝创建成片任务
- **AND** 返回明确的缺失依赖提示

### Requirement: 前端必须提供最小可用的成片编排页
系统 SHALL 在前端提供单独的成片编排入口，使用户可以发起编排、查看状态、预览成片并对失败结果重试。

#### Scenario: 用户查看成片编排页
- **WHEN** 用户打开剧本工程的成片编排页面
- **THEN** 页面展示项目级状态、可用镜头数量、当前成片结果与失败原因
- **AND** 用户可以发起整项目编排
- **AND** 用户可以在结果生成后直接预览成片

## MODIFIED Requirements
### Requirement: 生产闭环必须从“镜头级结果可衔接”升级为“项目级成片可衔接”
系统 SHALL 在不破坏现有视频生成、配音和口型同步工作流的前提下，将生产闭环从“镜头级资产齐备”扩展为“项目级成片可生成”，为后续导出包 / 成片交付提供直接输入。

#### Scenario: 项目进入导出前阶段
- **WHEN** 用户已完成镜头视频、配音、口型同步中的任意可用组合
- **THEN** 系统允许继续生成项目级成片
- **AND** 项目状态可以区分“镜头结果已齐备但成片未完成”和“成片已就绪”

### Requirement: 导出页必须展示项目级成片状态
系统 SHALL 在现有导出页中增加项目级成片准备状态提示与成片预览区域，使用户明确当前项目是否已经具备最终可回看结果。

#### Scenario: 用户进入导出页
- **WHEN** 项目尚未完成成片编排
- **THEN** 页面提示成片未完成
- **AND** 引导用户进入成片编排页面继续处理

## REMOVED Requirements
### Requirement: 无
**Reason**: 本次为新增生产能力，不移除现有能力。
**Migration**: 无需迁移。
