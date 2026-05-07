# 快捷模型配置模式 Spec

## Why
当前模型配置流程要求用户手动填写 Base URL、提供商、模型标识等信息，对普通用户而言门槛较高。亟需提供一种预置模型库 + API Key 极简接入的快捷模式，同时保留高级用户的手动配置能力。

## What Changes
- 后端新增**预置模型库**（PresetModelRegistry），包含主流模型（OpenAI、DeepSeek、Anthropic、通义千问等）的 Base URL、模型标识、默认参数。
- 后端新增 `GET /api/v1/preset-models` 接口，供前端获取预置模型列表。
- 后端新增 `POST /api/v1/connections/quick` 快捷创建连接接口：传入 provider + apiKey + modelName，后端自动匹配预置 Base URL 并创建 Connection + Model 记录。
- 前端 ModelConfigView 新增**快捷模式**（默认）与**高级模式**切换 Tab。
- 快捷模式 UI：Provider 下拉 → Model 下拉（联动）→ API Key 输入 → 一键创建连接与模型。
- 高级模式保留现有 ConnectionForm / ModelForm 完整功能。
- 快捷模式若用户选中的模型不在预置库中，引导用户切换至高级模式手动配置。

## Impact
- Affected specs: 模型配置管理、连接管理
- Affected code:
  - `aigc-server`: 新增 PresetModelRegistry、PresetModel、QuickConnectionRequest DTO、对应 Controller/Service
  - `aigc-site`: ModelConfigView 新增模式切换、快捷表单组件、API 层新增接口

## ADDED Requirements
### Requirement: 预置模型库
系统 SHALL 提供预置模型注册表，包含主流模型提供商的 Base URL、模型名称、默认元数据。

#### Scenario: 获取预置模型列表
- **WHEN** 前端请求 `GET /api/v1/preset-models`
- **THEN** 返回所有预置模型项，每项含 provider、modelName、baseUrl、displayName、icon/emoji

#### Scenario: 预置模型不存在
- **WHEN** 用户选择的模型不在预置库中
- **THEN** 引导用户切换至高级模式手动配置 Base URL

### Requirement: 快捷创建连接与模型
系统 SHALL 提供一键创建接口，用户只需提供 Provider、Model、API Key，后端自动匹配 Base URL 并同时创建 Connection 与 Model 记录。

#### Scenario: 快捷创建成功
- **WHEN** 用户在快捷模式下提交 provider + modelName + apiKey
- **THEN** 后端根据预置库匹配 baseUrl，保存 Connection 与 Model，返回创建成功

#### Scenario: 未知模型快捷创建
- **WHEN** 用户传入的 modelName 不在预置库中
- **THEN** 后端返回 400 错误，提示"该模型不在预置库中，请使用高级模式配置"

### Requirement: 前端快捷模式 UI
系统 SHALL 在 ModelConfigView 提供快捷模式 UI，作为默认首选模式。

#### Scenario: 快捷模式默认展示
- **WHEN** 用户打开模型配置页面
- **THEN** 默认展示快捷模式界面（Provider 下拉 → Model 下拉 → API Key → 创建按钮）

#### Scenario: Provider 联动 Model 下拉
- **WHEN** 用户选择 Provider
- **THEN** Model 下拉仅显示该 Provider 下的可用预置模型

#### Scenario: 快捷创建成功
- **WHEN** 用户填写完表单并点击创建
- **THEN** 调用快捷创建接口，成功后自动切换到模型列表 Tab 并刷新

### Requirement: 模式切换
系统 SHALL 支持快捷模式与高级模式之间的切换。

#### Scenario: 切换到高级模式
- **WHEN** 用户点击"高级模式"
- **THEN** 展示现有 Connection + Model 配置界面（完全兼容现有逻辑）

#### Scenario: 从高级模式切回快捷模式
- **WHEN** 用户点击"快捷模式"
- **THEN** 恢复快捷模式 UI，现有已创建的连接/模型不受影响

## MODIFIED Requirements
### Requirement: 现有连接配置流程
**MODIFIED**: 高级模式保持现有 ConnectionForm + ModelForm 完整功能不变，快捷模式不依赖现有手动配置流程独立运作。

## REMOVED Requirements
（N/A 本次不删除任何现有功能）
