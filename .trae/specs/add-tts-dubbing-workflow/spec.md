# TTS 配音工作流 Spec

## Why
当前仓库已经具备剧本工程、镜头拆分、素材生成、视频片段生成、统一媒体资源管理等基础能力，但生产闭环仍停在“无声音成片”的阶段。缺少 TTS / 配音工作流后，教师和学生无法把镜头脚本进一步推进为可审核、可导出的完整视听内容。

## What Changes
- 为剧本工程新增 TTS / 配音工作流，支持按镜头或按项目生成配音任务
- 新增配音任务数据模型，记录文本、音色、状态、结果音频文件与失败原因
- 复用现有模型配置体系，为 TTS 能力补充可选模型与音色元数据
- 在生产编排中新增“配音已就绪”状态检查，供后续口型同步、成片编排复用
- 在前端新增最小可用的配音管理页，可发起生成、查看状态、试听与重试
- 本次不实现口型同步、背景音乐混音、时间轴精编、导出包合成，但要求保留可扩展字段与流程衔接点

## Impact
- Affected specs: 剧本工程、媒体资源管理、模型配置、生产编排、导出页
- Affected code: `aigc-server/src/main/java/com/example/aigc/service/ScriptProductionOrchestrator.java`、`aigc-server/src/main/java/com/example/aigc/controller/VideoPipelineController.java`、`aigc-server/src/main/java/com/example/aigc/service/ScriptProjectService.java`、`aigc-site-react/src/pages/ScriptProjectVideoPage.tsx`、`aigc-site-react/src/pages/ScriptProjectExportPage.tsx`、`aigc-site-react/src/stores/scriptProjectStore.ts`、`aigc-site-react/src/api/index.ts`

## ADDED Requirements
### Requirement: 剧本工程必须支持配音任务编排
系统 SHALL 为剧本工程提供独立的 TTS / 配音任务编排能力，使用户可以将镜头文本转换为可试听、可追踪的音频资源。

#### Scenario: 用户为项目发起配音生成
- **WHEN** 用户在剧本工程中选择生成配音
- **AND** 当前项目已经存在可用镜头文本
- **THEN** 系统为项目创建一组配音任务
- **AND** 每个任务至少关联 `projectId`、`shotId`、配音文本、状态、音频文件 ID

### Requirement: 配音任务必须支持状态追踪与失败重试
系统 SHALL 为配音任务提供与视频片段任务一致的最小状态管理能力，便于用户判断当前进度并对失败任务重试。

#### Scenario: 配音任务执行失败
- **WHEN** 某个配音任务调用 TTS 模型失败
- **THEN** 系统记录失败状态与错误原因
- **AND** 用户可以在界面上看到失败信息
- **AND** 用户可以单独重试失败任务

### Requirement: 配音结果必须纳入统一媒体资源管理
系统 SHALL 将配音音频文件写入现有统一媒体资源抽象，而不是使用旁路文件系统写法。

#### Scenario: 配音成功生成音频
- **WHEN** 配音任务成功返回音频结果
- **THEN** 系统将音频保存为统一媒体资源记录
- **AND** 结果包含 `storageProvider`、`bucketName`、`objectKey`、可访问链接等元数据
- **AND** 音频文件可以通过现有文件访问机制试听或下载

### Requirement: 用户必须可以选择 TTS 模型与音色参数
系统 SHALL 复用现有模型配置与工作流模型面板，为配音工作流提供独立的模型选择与基础音色参数。

#### Scenario: 用户为项目指定配音模型
- **WHEN** 用户在剧本工程中配置配音模型
- **THEN** 系统保存项目级工作流模型选择
- **AND** 至少支持保存音色名称、语言、语速等基础参数
- **AND** 这些参数在重新进入页面后仍可读取

### Requirement: 配音页面必须提供最小可用操作闭环
系统 SHALL 在前端提供单独的配音管理入口，使用户可以发起生成、查看状态、试听结果并进行重试。

#### Scenario: 用户查看项目配音页
- **WHEN** 用户打开剧本工程的配音页面
- **THEN** 页面展示镜头列表、配音任务状态、音频结果入口与失败信息
- **AND** 用户可以发起整项目生成
- **AND** 用户可以对单条失败任务执行重试

## MODIFIED Requirements
### Requirement: 生产闭环必须从“仅视频片段”升级为“视频 + 配音可衔接”
系统 SHALL 在不破坏现有视频生成能力的前提下，将生产闭环从“仅视频片段生成”扩展为“视频片段与配音任务并存”，为后续口型同步和成片编排提供输入。

#### Scenario: 项目进入生产阶段
- **WHEN** 用户已完成镜头拆分、素材与视频片段生成
- **THEN** 系统允许继续生成配音任务
- **AND** 导出页或项目状态可以区分“视频已生成但配音未完成”和“视频、配音均已就绪”

### Requirement: 导出页必须展示配音准备状态
系统 SHALL 在现有导出页中增加配音准备状态提示，使用户明确当前项目离“可交付成片”还有哪些缺失。

#### Scenario: 用户进入导出页
- **WHEN** 项目尚未完成全部配音任务
- **THEN** 页面提示配音未完成
- **AND** 引导用户进入配音页面继续处理

## REMOVED Requirements
### Requirement: 无
**Reason**: 本次为新增生产能力，不移除现有能力。
**Migration**: 无需迁移。
