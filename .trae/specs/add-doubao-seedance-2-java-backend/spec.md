# Java 后端添加 Doubao Seedance 2.0 模型配置 Spec

## Why
火山引擎豆包于 2026 年 4 月发布了 Seedance 2.0 视频生成模型，包含标准版和极速版两个变体。需要在 Java 后端的预置模型注册表中添加对应配置，与 Go 后端和前端保持一致。

## What Changes
- 在 Java 后端 PresetModelRegistry 中新增 2 个 Doubao Seedance 2.0 模型
- 替换旧的 Seedance 1.5 模型

## Impact
- Affected specs: 模型配置、预置模型列表
- Affected code: `aigc-server/src/main/java/com/example/aigc/model/PresetModelRegistry.java`

## MODIFIED Requirements

### Requirement: 预置模型列表
系统 SHALL 在预置模型列表中包含以下 Doubao Seedance 2.0 模型：

#### Scenario: 标准版
- **WHEN** 获取 ark 提供商的预置模型列表
- **THEN** 应返回 "豆包视频 Seedance 2.0（标准版）" (modelName: doubao-seedance-2-0-260128, capabilities: video)

#### Scenario: 极速版
- **WHEN** 获取 ark 提供商的预置模型列表
- **THEN** 应返回 "豆包视频 Seedance 2.0（极速版）" (modelName: doubao-seedance-2-0-fast-260128, capabilities: video)

## Model Details

| 模型标识 | 显示名称 | Base URL | 能力 |
|---------|---------|----------|------|
| doubao-seedance-2-0-260128 | 豆包视频 Seedance 2.0（标准版） | https://ark.cn-beijing.volces.com | video |
| doubao-seedance-2-0-fast-260128 | 豆包视频 Seedance 2.0（极速版） | https://ark.cn-beijing.volces.com | video |
