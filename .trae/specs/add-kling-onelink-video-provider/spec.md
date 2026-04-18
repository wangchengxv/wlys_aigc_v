# Kling 视频服务接入 Spec

## Why
当前仓库已经支持用户自定义连接、绑定模型并调用部分视频服务，但尚未把 `OneLinkAI -> Kling` 这条链路作为可落地的后端能力接入。缺少 `Kling` 专属配置与视频路由后，用户即使手动填写 API 地址和模型 ID，也无法稳定完成文生视频、图生视频调用。

## What Changes
- 新增 `Kling (via OneLinkAI)` 的后端服务商配置能力，默认走 `https://api.onelinkai.cloud/kling`
- 为 `Kling` 模型补充视频输入模式语义，支持区分 `文生视频` 与 `图生视频`
- 扩展视频生成路由，按模型配置与是否提供参考图，分别调用 `/v1/videos/text2video` 与 `/v1/videos/image2video`
- 补充 `Kling` 异步任务提交、轮询查询、结果 URL 提取与错误映射
- 增加 `Kling` 预置示例模型或等价快捷配置入口，降低配置成本
- 本次不覆盖多图参考生视频、动作控制、Omni-Video、视频延长、对口型、数字人、多模态视频编辑等高级能力，但要求为后续扩展保留元数据与路由扩展点

## Impact
- Affected specs: 服务商目录、连接配置、模型配置、视频生成、异步任务轮询
- Affected code: `aigc-server-go/internal/catalog/catalog.go`、`aigc-server-go/internal/service/preset_models.go`、`aigc-server-go/internal/service/model_config.go`、`aigc-server-go/internal/service/generation.go`、`aigc-server-go/internal/gateway/http.go`、`aigc-server-go/internal/server/crud.go`

## ADDED Requirements
### Requirement: Kling 服务商可被用户配置
系统 SHALL 在后端服务商目录中暴露一个可配置的 `Kling` 视频服务类型，使用户可以使用 OneLinkAI 域名与自己的 API Key 建立连接。

#### Scenario: 用户创建 Kling 连接
- **WHEN** 用户在服务商中心选择 `Kling` 或等价的 `OneLinkAI Kling` 类型并填写 Base URL、API Key
- **THEN** 后端返回可保存的连接配置
- **AND** 默认 Base URL 为 `https://api.onelinkai.cloud/kling`
- **AND** 服务商目录应标记该类型支持视频能力

### Requirement: Kling 模型必须声明视频输入模式
系统 SHALL 允许 `Kling` 模型在模型配置中声明其视频输入模式，以便后端决定应该调用文生视频还是图生视频接口。

#### Scenario: 用户保存 Kling 模型配置
- **WHEN** 用户为 `Kling` 连接新增或编辑模型
- **THEN** 模型配置中可以保存 `text_to_video`、`image_to_video` 或等价的自动判断模式
- **AND** 该模式在读取、更新、导入模型时不会丢失

### Requirement: Kling 文生视频路由
系统 SHALL 在用户选择 `Kling` 模型且请求不包含参考图时，调用 `Kling` 的文生视频接口。

#### Scenario: 无参考图生成视频
- **WHEN** 用户发起视频生成请求，所选模型属于 `Kling`
- **AND** 请求未提供参考图
- **THEN** 后端提交到 `/v1/videos/text2video`
- **AND** 使用用户连接中的 Base URL 与 API Key 认证

### Requirement: Kling 图生视频路由
系统 SHALL 在用户选择 `Kling` 模型且请求包含参考图时，调用 `Kling` 的图生视频接口。

#### Scenario: 带参考图生成视频
- **WHEN** 用户发起视频生成请求，所选模型属于 `Kling`
- **AND** 请求提供了参考图
- **THEN** 后端提交到 `/v1/videos/image2video`
- **AND** 图片字段映射符合 `Kling` 接口要求

### Requirement: Kling 输入冲突必须返回明确错误
系统 SHALL 在 `Kling` 模型配置与用户输入不匹配时，返回可理解且可操作的错误，而不是落到错误接口或模糊失败。

#### Scenario: 模型被限定为图生视频但请求未提供参考图
- **WHEN** 用户选择仅支持图生视频的 `Kling` 模型
- **AND** 请求中没有参考图
- **THEN** 后端返回 4xx 业务错误
- **AND** 错误消息明确提示需要提供参考图或切换模型

### Requirement: Kling 异步任务结果可被统一消费
系统 SHALL 支持 `Kling` 视频任务的异步提交与轮询，并将最终结果转换为现有工作台可消费的视频 URL 列表。

#### Scenario: Kling 任务成功返回视频
- **WHEN** `Kling` 任务提交成功并进入轮询
- **THEN** 后端能够从查询结果中提取任务状态、失败信息与视频 URL
- **AND** 成功时写入统一的视频结果数组
- **AND** 失败时映射为现有任务失败结构

## MODIFIED Requirements
### Requirement: 现有视频生成路由必须支持按模型语义选择接口
系统 SHALL 在保持现有 `Ark`、`Vidu`、`Moark` 行为不变的前提下，将视频路由从“仅按服务商硬编码”扩展为“按服务商 + 模型元数据 + 输入形态”综合决策。

#### Scenario: 非 Kling 模型继续按原逻辑工作
- **WHEN** 用户选择的不是 `Kling` 模型
- **THEN** 现有 `Ark`、`Vidu`、`Moark` 提交与查询逻辑保持不变
- **AND** 本次接入不会改变其请求路径与结果解析

### Requirement: 现有模型配置能力必须可承载新视频供应商
系统 SHALL 复用当前连接配置与模型配置体系承载 `Kling`，而不是引入独立的临时配置入口。

#### Scenario: Kling 通过现有模型配置体系接入
- **WHEN** 用户查看连接列表、模型列表或服务商目录
- **THEN** `Kling` 与其他供应商以一致的数据结构返回
- **AND** 前端无需依赖硬编码私有接口才能展示基础配置能力
