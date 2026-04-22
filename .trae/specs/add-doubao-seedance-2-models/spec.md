# 新增 Doubao Seedance 2.0 模型配置 Spec

## Why
火山引擎豆包于 2026 年 4 月发布了 Seedance 2.0 视频生成模型，包含标准版和极速版两个变体。为支持用户在模型配置界面快速添加该模型，需要在预置模型列表中添加对应配置。

## What Changes
- 在 aigc-server-go 的预置模型列表中新增 2 个 Doubao Seedance 2.0 模型
- 在 aigc-site-react 前端的 Mock 预置数据中同步添加这 2 个模型
- 前端快捷配置表单在选择 ark 提供商时可看到这两个模型选项

## Impact
- Affected specs: 模型配置、快捷添加模型、预置模型列表
- Affected code:
  - `aigc-server-go/internal/service/preset_models.go`
  - `aigc-site-react/src/api/index.ts` (MOCK_PRESET_MODELS)

## MODIFIED Requirements

### Requirement: 预置模型列表
系统 SHALL 在预置模型列表中包含以下 Doubao Seedance 2.0 模型：

#### Scenario: 标准版
- **WHEN** 用户选择提供商 "ark" 并查看模型列表
- **THEN** 应看到 "豆包视频 Seedance 2.0（标准版）" (modelName: doubao-seedance-2-0-260128, capabilities: video)

#### Scenario: 极速版
- **WHEN** 用户选择提供商 "ark" 并查看模型列表
- **THEN** 应看到 "豆包视频 Seedance 2.0（极速版）" (modelName: doubao-seedance-2-0-fast-260128, capabilities: video)

### Requirement: 快捷模型配置
当用户选择 "ark" 提供商并选择 Doubao Seedance 2.0 模型后，只需输入 API Key 即可完成配置，Base URL 使用默认值 `https://ark.cn-beijing.volces.com`。

#### Scenario: 用户快捷配置
- **WHEN** 用户在模型配置页面选择 ark 提供商、Doubao Seedance 2.0 模型，输入 API Key 并提交
- **THEN** 系统自动创建连接（Base URL 预填充）与模型配置
- **AND** 模型能力标记为 video

## Model Details

| 模型标识 | 显示名称 | Base URL | 能力 | 说明 |
|---------|---------|----------|------|------|
| doubao-seedance-2-0-260128 | 豆包视频 Seedance 2.0（标准版） | https://ark.cn-beijing.volces.com | video | 画质最高，支持 T2V/I2V/多模态参考 |
| doubao-seedance-2-0-fast-260128 | 豆包视频 Seedance 2.0（极速版） | https://ark.cn-beijing.volces.com | video | 画质略低，生成速度更快 |
