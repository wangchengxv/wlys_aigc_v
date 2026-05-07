# SpringAI大模型配置管理 Spec

## Why
当前项目已有模型配置相关前后端结构，但后端以现有实现为主，缺少基于 SpringAI 的统一模型接入与配置管理能力。需要支持管理员在前端直接维护模型配置与 API 信息，并让聊天与模型选择链路可使用这些配置。

## What Changes
- 新增基于 Spring Boot + SpringAI 的后端服务层，提供模型配置的增删改查接口。
- 新增模型提供商配置能力，支持为模型填写 API Key、Base URL、模型名、是否启用等字段。
- 新增配置安全策略：API Key 入库存储前加密，接口返回时脱敏，日志禁止输出明文密钥。
- 改造前端模型管理页面的数据来源与保存逻辑，改为调用 SpringAI 后端接口。
- 改造前端连接配置与模型选择相关页面，使其可读取并应用新的模型配置。
- 补充后端参数校验、错误码与前端错误提示，确保配置失败可定位、可恢复。

## Impact
- Affected specs: 模型配置管理、连接管理、模型选择与运行时路由能力。
- Affected code: `backend/models`、`backend/routers`、`backend/utils`、`frontend/apis/models`、`frontend/admin-settings/Connections`、`frontend/admin-settings/Models`、`frontend/chat/ModelSelector`、`frontend/workspace/Models`。

## ADDED Requirements
### Requirement: SpringAI模型配置管理接口
系统 SHALL 提供统一的模型配置管理接口，支持管理员创建、更新、删除、查询模型配置。

#### Scenario: 创建模型配置成功
- **WHEN** 管理员提交包含提供商类型、模型名、Base URL 与 API Key 的配置
- **THEN** 系统保存配置并返回可用于前端展示的模型记录（API Key 脱敏）

#### Scenario: 更新模型配置成功
- **WHEN** 管理员修改已存在模型配置并提交
- **THEN** 系统完成更新并在后续查询中返回最新配置

#### Scenario: 删除模型配置成功
- **WHEN** 管理员删除某条模型配置
- **THEN** 系统移除该配置并确保前端列表不再显示

### Requirement: 配置安全与校验
系统 SHALL 对敏感配置执行安全处理，并在无效输入时返回可读错误信息。

#### Scenario: API Key安全处理
- **WHEN** 管理员提交 API Key
- **THEN** 系统加密存储并在任何查询接口中仅返回脱敏值

#### Scenario: 参数校验失败
- **WHEN** 管理员提交缺失必要字段或格式错误的配置
- **THEN** 系统返回结构化错误，前端展示可操作的提示信息

### Requirement: 前端配置与模型选择联动
系统 SHALL 让前端模型管理、连接管理与聊天模型选择统一使用新配置数据源。

#### Scenario: 前端读取模型列表
- **WHEN** 管理员或用户打开模型相关页面
- **THEN** 前端通过新接口获取模型配置并展示启用状态与必要元数据

#### Scenario: 前端修改后即时生效
- **WHEN** 管理员保存模型配置变更
- **THEN** 前端在刷新或重新拉取后可看到变更并用于模型选择

## MODIFIED Requirements
### Requirement: 现有模型配置数据流
系统 SHALL 将模型配置的写入与读取链路从现有后端实现迁移为 SpringAI 后端服务统一管理，并保持前端页面交互入口不变。

## REMOVED Requirements
### Requirement: 前端直接依赖旧配置接口
**Reason**: 旧接口无法满足 SpringAI 统一接入与敏感信息安全治理要求。  
**Migration**: 前端 API 层切换到新后端接口，保留字段兼容映射，完成后移除旧调用路径。
