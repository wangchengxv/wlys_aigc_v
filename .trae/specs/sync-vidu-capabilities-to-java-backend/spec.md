# Vidu 能力同步到 Java 后端 Spec

## Why
当前 Vidu 图生视频能力已在 `aigc-server-go` 侧完成，但平台主线交付与文档范围以 `aigc-server`（Spring Boot）为主。若不把同等能力同步到 Java 后端，前后端主线会出现能力不一致，影响生产可用性与维护成本。

## What Changes
- 在 `aigc-server`（Java）补齐 Vidu 图生视频接入，提交接口为 `POST /ent/v2/img2video`
- 按 Vidu 官方协议实现 `Authorization: Token {apiKey}`、`application/json` 请求与字段映射
- 同步模型族约束（q1/q2/q3/2.0）下的时长、分辨率、音频相关参数校验
- 同步任务查询与统一结果映射，支持主视频 URL 与水印 URL 同时返回
- 增加 Java 侧最小回归测试，覆盖核心与异常场景

## Impact
- Affected specs: 服务商目录、连接配置、模型配置、视频生成、任务轮询、结果回显
- Affected code: `aigc-server/src/main/java/com/example/aigc/service/**`、`aigc-server/src/main/java/com/example/aigc/model/**`、`aigc-server/src/main/java/com/example/aigc/controller/**`、`aigc-server/src/test/java/com/example/aigc/**`

## ADDED Requirements
### Requirement: Java 后端支持 Vidu 图生视频提交
系统 SHALL 在 Java 后端将 Vidu 图生视频请求路由到 `POST /ent/v2/img2video`，并按照官方鉴权要求发送请求。

#### Scenario: 使用 Vidu 模型提交图生视频
- **WHEN** 用户选择 Vidu 模型并提交带参考图的视频任务
- **THEN** Java 后端调用 Vidu 图生视频接口
- **AND** 请求头包含 `Authorization: Token {apiKey}`

### Requirement: Java 后端执行 Vidu 参数约束校验
系统 SHALL 在 Java 后端实现与 Go 侧一致的关键参数校验，避免非法参数进入供应商接口。

#### Scenario: 参数与模型能力冲突
- **WHEN** 用户提交不支持的 `duration` 或 `resolution`
- **THEN** Java 后端返回明确的 4xx 业务错误
- **AND** 错误中提示可选范围或修正方向

### Requirement: Java 后端统一映射 Vidu 成功结果
系统 SHALL 将 Vidu 成功结果映射为平台统一结构，并同时保留主视频 URL 与可选水印 URL。

#### Scenario: Vidu 返回主视频与水印视频
- **WHEN** 查询结果同时包含 `url` 与 `watermark_video_url`
- **THEN** Java 后端统一结果中保留两个 URL（去重且顺序稳定）

### Requirement: Java 后端统一映射 Vidu 失败结果
系统 SHALL 将 Vidu 任务失败信息映射为平台可消费的统一失败结构。

#### Scenario: Vidu 任务失败
- **WHEN** Vidu 查询结果为失败并带失败原因
- **THEN** Java 后端返回统一失败状态与失败原因文本

## MODIFIED Requirements
### Requirement: 主线后端能力一致性
系统 SHALL 保证平台主线后端（Java）与已落地能力（Go）在 Vidu 图生视频核心行为上保持一致。

#### Scenario: 同一请求在主线后端可复现
- **WHEN** 前端以相同参数请求 Vidu 图生视频
- **THEN** Java 后端与 Go 后端在关键行为（路由、校验、结果映射）一致

## REMOVED Requirements
### Requirement: 无
**Reason**: 本次为 Java 侧补齐，不移除现有能力。  
**Migration**: 无。
