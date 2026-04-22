# Kling 视频服务接入 Spec

## Why
当前仓库已经支持用户通过 OneLinkAI 接入 Vidu 视频服务，但尚未支持 Kling（可灵）视频模型。用户需要能够使用 OneLinkAI 的 API Key 来配置并使用 Kling 的文生视频和图生视频能力。

## What Changes
- 在 ProviderCatalog 中新增 `kling` 服务商定义，默认 Base URL 为 `https://api.onelinkai.cloud`
- 为 Kling 添加文生视频路径 `/v1/videos/text2video` 和图生视频路径 `/v1/videos/image2video`
- 在 GenerationServiceImpl 中扩展路由逻辑，根据模型配置和是否有参考图选择正确的接口
- 在预置模型注册表/快捷配置中加入 Kling 模型示例
- 本次不覆盖多图参考生视频、动作控制、Omni-Video、视频延长、对口型、数字人等高级能力

## Impact
- Affected specs: 服务商目录、连接配置、模型配置、视频生成
- Affected code:
  - [ProviderCatalog.java](file:///Users/xingyi/Downloads/AIGC_小云雀/AIGC_university_副本/aigc-server/src/main/java/com/example/aigc/service/ProviderCatalog.java) - 新增 Kling 服务商定义
  - [GenerationServiceImpl.java](file:///Users/xingyi/Downloads/AIGC_小云雀/AIGC_university_副本/aigc-server/src/main/java/com/example/aigc/service/impl/GenerationServiceImpl.java) - 新增 Kling 路由分支
  - [PresetModelRegistry.java](file:///Users/xingyi/Downloads/AIGC_小云雀/AIGC_university_副本/aigc-server/src/main/java/com/example/aigc/model/PresetModelRegistry.java) - 添加 Kling 预置模型
  - [ModelCapabilityService.java](file:///Users/xingyi/Downloads/AIGC_小云雀/AIGC_university_副本/aigc-server/src/main/java/com/example/aigc/service/ModelCapabilityService.java) - Kling 模型能力解析

## ADDED Requirements
### Requirement: Kling 服务商可被用户配置
系统 SHALL 在后端服务商目录中暴露一个可配置的 `Kling` 视频服务类型，使用户可以使用 OneLinkAI 域名与自己的 API Key 建立连接。

#### Scenario: 用户创建 Kling 连接
- **WHEN** 用户选择 `Kling` 类型并填写 Base URL、API Key
- **THEN** 后端返回可保存的连接配置
- **AND** 默认 Base URL 为 `https://api.onelinkai.cloud`
- **AND** 服务商目录应标记该类型支持视频能力

### Requirement: Kling 模型必须声明视频输入模式
系统 SHALL 允许 `Kling` 模型在模型配置中声明其视频输入模式（文生视频/图生视频），以便后端决定应该调用哪个接口。

#### Scenario: 用户保存 Kling 模型配置
- **WHEN** 用户为 `Kling` 连接新增或编辑模型
- **THEN** 模型配置中可以保存 `text_to_video`、`image_to_video` 或等价的自动判断模式
- **AND** 该模式在读取、更新、导入模型时不会丢失

### Requirement: Kling 文生视频路由
系统 SHALL 在用户选择 `Kling` 模型且请求不包含参考图时，调用 Kling 的文生视频接口。

#### Scenario: 无参考图生成视频
- **WHEN** 用户发起视频生成请求，所选模型属于 `Kling`
- **AND** 请求未提供参考图
- **THEN** 后端提交到 `/v1/videos/text2video`
- **AND** 使用用户连接中的 Base URL 与 API Key 认证

### Requirement: Kling 图生视频路由
系统 SHALL 在用户选择 `Kling` 模型且请求包含参考图时，调用 Kling 的图生视频接口。

#### Scenario: 带参考图生成视频
- **WHEN** 用户发起视频生成请求，所选模型属于 `Kling`
- **AND** 请求提供了参考图
- **THEN** 后端提交到 `/v1/videos/image2video`
- **AND** 图片字段映射符合 Kling 接口要求

### Requirement: Kling 输入冲突必须返回明确错误
系统 SHALL 在 `Kling` 模型配置与用户输入不匹配时，返回可理解且可操作的错误。

#### Scenario: 模型被限定为图生视频但请求未提供参考图
- **WHEN** 用户选择仅支持图生视频的 `Kling` 模型
- **AND** 请求中没有参考图
- **THEN** 后端返回 4xx 业务错误
- **AND** 错误消息明确提示需要提供参考图或切换模型

### Requirement: Kling 异步任务结果可被统一消费
系统 SHALL 支持 Kling 视频任务的异步提交与轮询，并将最终结果转换为现有工作台可消费的视频 URL 列表。

#### Scenario: Kling 任务成功返回视频
- **WHEN** Kling 任务提交成功并进入轮询
- **THEN** 后端能够从查询结果中提取任务状态、失败信息与视频 URL
- **AND** 成功时写入统一的视频结果数组

## MODIFIED Requirements
### Requirement: 现有视频生成路由必须支持按模型语义选择接口
系统 SHALL 在保持现有 `Ark`、`Vidu`、`Moark` 行为不变的前提下，将视频路由扩展为"按服务商 + 模型元数据 + 输入形态"综合决策。

#### Scenario: 非 Kling 模型继续按原逻辑工作
- **WHEN** 用户选择的不是 `Kling` 模型
- **THEN** 现有 `Ark`、`Vidu`、`Moark` 提交与查询逻辑保持不变

### Requirement: 现有模型配置能力必须可承载新视频供应商
系统 SHALL 复用当前连接配置与模型配置体系承载 `Kling`，而不是引入独立的临时配置入口。

#### Scenario: Kling 通过现有模型配置体系接入
- **WHEN** 用户查看连接列表、模型列表或服务商目录
- **THEN** `Kling` 与其他供应商以一致的数据结构返回