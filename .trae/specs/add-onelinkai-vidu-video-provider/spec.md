# OneLinkAI Vidu 视频能力接入 Spec

## Why
当前后端已支持直接连接 Vidu（`https://api.onelinkai.cloud`）图生视频，也已支持通过 OneLinkAI 连接调用 Vidu 模型（`vidu_onelink` 向后兼容路径）。但尚未把 **OneLinkAI Vidu 视频** 作为正式的第一方能力接入：用户通过 OneLinkAI 连接配置 API Key 后，平台应能识别 Vidu 模型并路由到 `/vidu//vidu/vidu/ent/v2/img2video`，与直接 Vidu 连接行为一致。

## What Changes
- 在 `catalog.go` 中新增 `vidu_onelink` Provider，支持 OneLinkAI 域名下的 Vidu 图生视频
- 扩展 `genVideos` 路由逻辑，识别 `onelinkai` Provider 中的 `viduq*` / `vidu*` 模型并走 OneLinkAI Vidu 路径
- 复用现有 Vidu 图生视频 payload 构建、参数校验、图片验证与任务轮询逻辑
- 扩展 `preset_models.go` 预置模型列表，补充 OneLinkAI Vidu 预置模型条目
- 为后续扩展文生视频、图生视频多能力预留路由扩展点

## Impact
- Affected specs: 服务商目录、连接配置、模型配置、视频生成、异步任务轮询
- Affected code: `aigc-server-go/internal/catalog/catalog.go`、`aigc-server-go/internal/service/preset_models.go`、`aigc-server-go/internal/service/generation.go`

## ADDED Requirements
### Requirement: OneLinkAI Vidu 服务商可被用户配置
系统 SHALL 在后端服务商目录中暴露 `vidu_onelink` 可配置的 Vidu 视频服务类型，使用户可通过 OneLinkAI 域名与自己的 API Key 建立 Vidu 图生视频连接。

#### Scenario: 用户创建 OneLinkAI Vidu 连接
- **WHEN** 用户在服务商中心选择 `vidu_onelink` 并填写 Base URL（`https://api.onelinkai.cloud`）、API Key
- **THEN** 后端可保存连接配置并返回连接信息
- **AND** 默认 Base URL 为 `https://api.onelinkai.cloud`
- **AND** `VideoSubmitPath` 为 `/vidu//vidu/vidu/ent/v2/img2video`
- **AND** `VideoResultPath` 为 `/vidu/ent/v2/tasks/{taskId}/creations`
- **AND** 鉴权方式为 `Bearer`（与 OneLinkAI 通用格式一致）

### Requirement: OneLinkAI Vidu 模型可被识别并路由
系统 SHALL 在用户选择 `onelinkai` Provider 中的 Vidu 模型（如 `viduq3-turbo`、`image-vidu-q2` 等）时，自动路由到 OneLinkAI Vidu 图生视频接口。

#### Scenario: OneLinkAI Vidu 图生视频调用
- **WHEN** 用户配置了 `onelinkai` 连接，其中模型名为 `video-viduq3-pro`
- **AND** 用户提交图生视频任务并提供参考图
- **THEN** 后端提交到 `/vidu//vidu/vidu/ent/v2/img2video`（通过 OneLinkAI 域名）
- **AND** 使用用户 OneLinkAI 连接中的 Base URL 与 API Key 认证
- **AND** 复用现有 Vidu payload 构建与参数校验逻辑

### Requirement: OneLinkAI Vidu 输入验证与错误返回
系统 SHALL 在 OneLinkAI Vidu 路径中复用现有 Vidu 图片验证与参数校验逻辑，并在冲突时返回明确 4xx 业务错误。

#### Scenario: 未提供参考图
- **WHEN** 用户选择 OneLinkAI Vidu 模型但未提供参考图
- **THEN** 后端返回 4xx 业务错误，提示需要提供参考图

## MODIFIED Requirements
### Requirement: 现有 Vidu 路由必须保持向后兼容
系统 SHALL 在添加 OneLinkAI Vidu 路由时，保持现有 `vidu` 直连路径与 `vidu_onelink` 向后兼容路径的行为不变。

#### Scenario: 非 OneLinkAI Vidu 模型继续按原逻辑工作
- **WHEN** 用户选择直接 Vidu 连接或其他非 OneLinkAI Provider
- **THEN** 现有 Vidu 直连、Ark、Moark 路由保持不变

## REMOVED Requirements
### Requirement: 无
**Reason**: 本次为增量接入，不移除现有能力。
**Migration**: 无。
