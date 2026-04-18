# Vidu 图生视频能力接入 Spec

## Why
平台当前虽有视频生成链路，但缺少按 Vidu 官方图生视频协议完整建模与参数透传能力，导致模型能力无法在平台中稳定、可配置地使用。需要以官方接口为准接入 Vidu 图生视频，覆盖可用模型、关键参数、异步任务与结果映射。

## What Changes
- 新增 `Vidu` 图生视频供应商能力，默认提交接口为 `POST https://api.vidu.cn/ent/v2/img2video`
- 统一 `Authorization: Token {api key}` 鉴权与 `application/json` 请求协议
- 增加 `images`（单图）、`model`、`prompt`、`duration`、`resolution`、`seed`、`audio`、`audio_type`、`voice_id`、`is_rec`、`bgm`、`off_peak`、`watermark`、`wm_position`、`payload` 等参数映射与校验
- 按模型族（q1/q2/q3/2.0）约束可选时长、分辨率与音频能力，避免非法组合提交
- 补充 Vidu 任务查询、状态同步、失败原因与结果视频 URL（含带水印地址）统一映射
- 为后续扩展能力（如更多模型参数、音视频策略）保留字段与网关扩展点

## Impact
- Affected specs: 服务商目录、连接配置、模型配置、视频提交、异步任务查询、结果映射
- Affected code: `aigc-server-go/internal/catalog/catalog.go`、`aigc-server-go/internal/service/model_config.go`、`aigc-server-go/internal/service/generation.go`、`aigc-server-go/internal/gateway/http.go`、`aigc-server-go/internal/server/crud.go`

## ADDED Requirements
### Requirement: 平台可配置 Vidu 图生视频连接
系统 SHALL 在服务商目录提供 `Vidu` 类型，支持用户通过现有连接配置流程保存 Base URL 与 API Key，并按 Vidu 鉴权规范发起请求。

#### Scenario: 创建 Vidu 连接
- **WHEN** 用户在服务商中心选择 `Vidu` 并填写连接信息
- **THEN** 平台可保存并返回连接配置
- **AND** 默认 Base URL 为 `https://api.vidu.cn`
- **AND** 请求鉴权头按 `Authorization: Token {api key}` 发送

### Requirement: 图生视频参数按官方协议校验与映射
系统 SHALL 将平台视频生成参数映射到 Vidu 图生视频协议，并在提交前执行关键输入校验。

#### Scenario: 提交合法图生视频请求
- **WHEN** 用户选择 Vidu 模型并提交图生视频任务
- **THEN** 请求发往 `/ent/v2/img2video`
- **AND** `images` 仅允许 1 张图片（URL 或 Base64）
- **AND** 图片格式、比例与体积约束在提交前校验
- **AND** 有效参数映射到对应字段且不丢失

### Requirement: 模型能力约束驱动参数组合
系统 SHALL 按所选模型族约束 `duration`、`resolution`、`audio` 及相关字段的可用组合，防止越界参数进入供应商接口。

#### Scenario: 参数与模型能力冲突
- **WHEN** 用户为某模型提交不支持的时长或分辨率
- **THEN** 平台返回明确 4xx 业务错误
- **AND** 错误消息指示可选范围或建议替代值

### Requirement: Vidu 异步任务可被统一消费
系统 SHALL 支持 Vidu 任务提交后查询状态，并将成功与失败结果映射为平台统一任务结构。

#### Scenario: 任务状态同步与结果回写
- **WHEN** Vidu 任务进入轮询
- **THEN** 平台可同步处理中、成功、失败状态
- **AND** 成功时写入视频 URL（含可选带水印 URL）
- **AND** 失败时保留供应商失败原因并映射为平台错误结构

### Requirement: 推荐提示词与音频相关参数行为一致
系统 SHALL 在 `is_rec`、`audio`、`audio_type`、`voice_id`、`bgm` 等参数场景中遵循官方行为约束，避免无效参数误导用户。

#### Scenario: 推荐提示词覆盖 prompt
- **WHEN** 用户启用 `is_rec=true`
- **THEN** 平台按供应商行为忽略手动 `prompt`
- **AND** 在任务元数据中记录推荐模式已启用

## MODIFIED Requirements
### Requirement: 现有视频网关支持“供应商协议模板化”
系统 SHALL 将视频提交与查询逻辑从供应商硬编码分支，扩展为基于供应商协议模板与参数规则的可扩展实现。

#### Scenario: 其他供应商无回归
- **WHEN** 用户使用非 Vidu 模型（如 Ark、Moark、Kling）
- **THEN** 既有提交、查询与结果解析行为保持不变

## REMOVED Requirements
### Requirement: 无
**Reason**: 本次为增量接入，不移除现有能力。  
**Migration**: 无。
