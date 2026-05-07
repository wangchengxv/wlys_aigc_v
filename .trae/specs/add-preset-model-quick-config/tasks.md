# Tasks
- [x] Task 1: 后端新增预置模型库与快捷创建接口
  - [x] SubTask 1.1: 创建 PresetModel 实体与 PresetModelRegistry 预置模型注册表
  - [x] SubTask 1.2: 新增 GET /api/v1/preset-models 接口返回预置模型列表
  - [x] SubTask 1.3: 新增 QuickConnectionRequest DTO 及 POST /api/v1/connections/quick 快捷创建接口
  - [x] SubTask 1.4: ConnectionConfigService 新增快捷创建逻辑（自动匹配 baseUrl）
  - [x] SubTask 1.5: 预置主流模型（OpenAI、DeepSeek、Anthropic、通义千问）

- [x] Task 2: 前端新增快捷模式 API 与类型
  - [x] SubTask 2.1: types/index.ts 新增 PresetModel、QuickConnectionRequest 类型
  - [x] SubTask 2.2: services/api.ts 新增 getPresetModels() 和 quickCreateConnection() 接口
  - [x] SubTask 2.3: 新增 components/model/QuickModelForm.vue 快捷模式表单组件

- [x] Task 3: 前端 ModelConfigView 集成快捷模式
  - [x] SubTask 3.1: 新增快捷模式/高级模式 Tab 切换 UI（快捷模式默认）
  - [x] SubTask 3.2: 快捷模式下实现 Provider → Model 联动下拉
  - [x] SubTask 3.3: 快捷模式调用 quickCreateConnection，完成后自动刷新并切换 Tab
  - [x] SubTask 3.4: 未知模型场景下引导用户切换高级模式

- [x] Task 4: 验证与文档
  - [x] SubTask 4.1: 端到端测试快捷模式创建流程
  - [x] SubTask 4.2: 确认高级模式完全不受影响

# Task Dependencies
- Task 2 depends on Task 1
- Task 3 depends on Task 1 and Task 2
- Task 4 depends on Task 3
