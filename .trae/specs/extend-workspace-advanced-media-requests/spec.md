# 工作台高级媒体请求扩容 Spec

## Why
现有工作台生成请求只预留了 `videoReferenceImageUrl` 与 `videoViduOptions` 两个零散扩展位，无法继续承载 Vidu reference2image、Kling 多图参考生图、扩图、Omni 等能力所需的额外字段。

模型和后端能力已部分存在，但工作台表单与 Java 请求体结构没有统一承载层，导致高级图片/视频能力无法在现有生成入口完整提交。

## What Changes
- 在工作台生成请求中引入按媒体类型与能力分组的高级参数结构，避免继续新增零散顶层字段
- 工作台根据模型能力动态展示高级图片/视频表单项，覆盖 Vidu reference2image、Kling 多图参考生图、扩图、Omni 等场景
- Java 后端扩展 `GenerateRequest`、参数归一化、校验与供应商请求体构建逻辑
- 保留现有 `videoReferenceImageUrl` 与 `videoViduOptions` 的兼容读法，避免已有 Vidu 视频链路立即失效
- 为图片与视频链路补齐针对高级字段的最小自动化验证与回归检查

## Impact
- Affected specs: 工作台生成请求、Vidu 图生视频能力、Kling OneLink 接入、OneLink 图片高级能力
- Affected code: `aigc-site-react/src/components/workspace/PromptPanel.tsx`、`aigc-site-react/src/types/index.ts`、`aigc-site-react/src/stores/generationStore.ts`、`aigc-server/src/main/java/com/example/aigc/dto/GenerateRequest.java`、`aigc-server/src/main/java/com/example/aigc/service/impl/GenerationServiceImpl.java`、`aigc-server/src/main/java/com/example/aigc/service/ProviderHttpGateway.java`

## ADDED Requirements
### Requirement: 工作台生成请求可承载高级媒体参数
系统 SHALL 为工作台生成请求提供可扩展的高级媒体参数结构，至少区分图片与视频两个方向，并允许按能力分组承载额外字段，而不是继续为每个模型新增零散顶层字段。

#### Scenario: 高级能力请求可被结构化提交
- **WHEN** 用户在工作台选择需要额外字段的图片或视频模型
- **THEN** 前端可以将参考图、多图输入、扩图参数、Omni 参数及供应商专属选项以结构化方式提交到后端

### Requirement: 工作台按模型能力渲染高级表单
系统 SHALL 基于当前所选模型能力展示对应的高级表单项，并在必填条件不满足时阻止提交。

#### Scenario: Vidu reference2image 展示必填输入
- **WHEN** 用户选择需要参考图的 Vidu reference2image 能力
- **THEN** 工作台展示参考图及相关高级字段，并在缺少必填参考图时给出明确提示

#### Scenario: Kling 多图参考生图展示多参考输入
- **WHEN** 用户选择 Kling 多图参考生图模型
- **THEN** 工作台展示多张参考图输入区域，并将其序列化为后端可识别的结构

#### Scenario: 扩图与 Omni 展示能力专属字段
- **WHEN** 用户选择扩图或 Omni 类模型
- **THEN** 工作台仅展示该能力需要的额外字段，不暴露无关参数

### Requirement: 后端可归一化并映射高级媒体参数
系统 SHALL 在 Java 后端接收高级媒体参数后完成归一化、必填校验、条件字段校验与供应商请求体映射，并保持现有普通图片/视频生成路径可用。

#### Scenario: 支持能力差异化请求体构建
- **WHEN** 后端收到受支持模型的高级媒体请求
- **THEN** 后端按模型和供应商将结构化字段映射到正确请求体，不遗漏 reference2image、多图参考、扩图或 Omni 所需字段

#### Scenario: 不支持的字段组合被拒绝
- **WHEN** 用户提交了与所选模型能力不匹配的字段组合
- **THEN** 后端返回明确的 4xx 业务错误，而不是静默忽略或错误透传

### Requirement: 兼容现有 Vidu 视频扩展字段
系统 SHALL 在引入新结构后继续兼容现有 `videoReferenceImageUrl` 与 `videoViduOptions` 请求格式，并在内部归一化到统一结构。

#### Scenario: 旧版请求仍可工作
- **WHEN** 旧客户端仍然提交 `videoReferenceImageUrl` 与 `videoViduOptions`
- **THEN** 后端继续接受并完成归一化，现有 Vidu 视频生成功能不回归

## MODIFIED Requirements
### Requirement: 工作台生成接口
工作台生成接口 SHALL 同时支持基础生成参数与结构化高级媒体参数。图片与视频请求在保持统一入口的前提下，允许针对具体模型能力携带额外字段，并在前后端两侧执行一致的必填约束与能力约束。

## REMOVED Requirements
- 无
