# 导出包与成片交付 Spec

## Why
当前仓库已经具备镜头视频、配音、口型同步、项目级成片编排与导出页预览能力，但交付仍停留在“在线预览和复制直链”的阶段。缺少导出包与成片交付后，教师和学生无法拿到带成片、关键素材和说明清单的统一交付物，平台也无法形成完整的生产闭环。

## What Changes
- 为剧本工程新增项目级导出包工作流，支持生成统一交付包
- 新增导出包任务数据模型，记录输入成片、附带素材、状态、结果归档文件与失败原因
- 导出包至少包含项目级成片、基础元数据清单、镜头与素材摘要信息
- 前端新增最小可用的导出包交付页或在现有导出页中补充导出包操作区
- 在生产编排中增加“导出包已就绪”状态聚合，供后续审核流与交付台账复用
- 本次不实现外部对象存储分享链接、审批流签收、复杂压缩策略与批量导出，但要求保留后续扩展点

## Impact
- Affected specs: 剧本工程、统一媒体资源、项目级成片、导出页、生产编排
- Affected code: `aigc-server/src/main/java/com/example/aigc/service/ScriptProductionOrchestrator.java`、`aigc-server/src/main/java/com/example/aigc/controller/VideoPipelineController.java`、`aigc-server/src/main/java/com/example/aigc/service/ScriptProjectService.java`、`aigc-site-react/src/pages/ScriptProjectExportPage.tsx`、`aigc-site-react/src/stores/scriptProjectStore.ts`、`aigc-site-react/src/api/index.ts`

## ADDED Requirements
### Requirement: 剧本工程必须支持项目级导出包任务
系统 SHALL 为剧本工程提供项目级导出包能力，使用户可以生成单一可下载交付物。

#### Scenario: 用户为项目发起导出包生成
- **WHEN** 用户在项目导出页选择生成导出包
- **AND** 当前项目已经存在可交付的项目级成片
- **THEN** 系统创建一条导出包任务
- **AND** 任务至少关联 `projectId`、输入成片文件、导出清单、状态、结果归档文件

### Requirement: 导出包必须包含最小交付内容
系统 SHALL 为每个导出包生成最小交付内容集合，确保下载后即可用于回看和留档。

#### Scenario: 导出包生成成功
- **WHEN** 系统完成导出包生成
- **THEN** 导出包至少包含项目级成片文件
- **AND** 包含项目元数据清单，如项目名称、镜头数量、生成时间、关键文件列表
- **AND** 包含镜头摘要信息，便于后续查阅和归档

### Requirement: 导出包任务必须支持状态追踪与失败重试
系统 SHALL 为导出包任务提供最小状态管理能力，使用户可以判断当前进度并重新生成失败任务。

#### Scenario: 导出包任务失败
- **WHEN** 导出包生成失败
- **THEN** 系统记录失败状态与错误原因
- **AND** 用户可以在界面上看到失败信息
- **AND** 用户可以重新发起导出包生成

### Requirement: 导出包结果必须纳入统一媒体资源管理
系统 SHALL 将导出包归档文件保存到统一媒体资源抽象，而不是单独旁路落盘。

#### Scenario: 导出包生成完成
- **WHEN** 系统成功生成导出包
- **THEN** 结果归档文件以统一媒体资源记录保存
- **AND** 返回 `storageProvider`、`bucketName`、`objectKey`、可访问链接等元数据
- **AND** 用户可以在前端直接下载该归档文件

### Requirement: 导出包工作流必须校验成片前置条件
系统 SHALL 在启动导出包前校验项目级成片是否存在，避免生成空包。

#### Scenario: 项目尚未生成最终成片
- **WHEN** 用户发起导出包生成
- **AND** 当前项目没有成功的项目级成片文件
- **THEN** 系统拒绝创建导出包任务
- **AND** 返回明确的缺失依赖提示

### Requirement: 前端必须提供最小可用的交付操作区
系统 SHALL 在现有导出页中提供导出包状态、生成入口、下载入口与失败提示，使用户可以完成最小交付闭环。

#### Scenario: 用户进入导出页
- **WHEN** 用户打开项目导出页
- **THEN** 页面展示项目级成片状态与导出包状态
- **AND** 用户可以发起生成导出包
- **AND** 在导出包成功后可以直接下载交付文件

## MODIFIED Requirements
### Requirement: 生产闭环必须从“项目级成片可衔接”升级为“项目级交付物可衔接”
系统 SHALL 在不破坏现有镜头级和项目级成片工作流的前提下，将生产闭环从“可生成项目级成片”扩展为“可生成统一交付包”，为后续审核流和交付留档提供直接输入。

#### Scenario: 项目进入最终交付阶段
- **WHEN** 用户已完成项目级成片编排
- **THEN** 系统允许继续生成导出包
- **AND** 项目状态可以区分“成片已就绪但交付包未完成”和“交付包已就绪”

### Requirement: 导出页必须展示交付包状态
系统 SHALL 在现有导出页中增加导出包准备状态提示和下载区域，使用户明确当前项目是否已经具备最终交付物。

#### Scenario: 用户进入导出页
- **WHEN** 项目尚未完成导出包生成
- **THEN** 页面提示交付包未完成
- **AND** 引导用户先生成项目级成片或直接启动导出包

## REMOVED Requirements
### Requirement: 无
**Reason**: 本次为新增生产能力，不移除现有能力。
**Migration**: 无需迁移。
