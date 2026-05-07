# Checklist

## Task 1: 后端新增预置模型库与快捷创建接口
- [x] PresetModel 实体类与 PresetModelRegistry 注册表已实现
- [x] GET /api/v1/preset-models 接口返回正确结构
- [x] QuickConnectionRequest DTO 已定义
- [x] POST /api/v1/connections/quick 快捷创建接口已实现
- [x] 预置模型覆盖主流厂商（OpenAI、DeepSeek、Anthropic、通义千问）

## Task 2: 前端新增快捷模式 API 与类型
- [x] PresetModel 类型已添加到 types/index.ts
- [x] QuickConnectionRequest 类型已添加
- [x] getPresetModels() API 方法已实现
- [x] quickCreateConnection() API 方法已实现
- [x] QuickModelForm.vue 组件已创建

## Task 3: 前端 ModelConfigView 集成快捷模式
- [x] 快捷/高级模式 Tab 切换已实现，默认快捷模式
- [x] Provider 下拉可选
- [x] 选择 Provider 后 Model 下拉仅显示该 Provider 的模型
- [x] API Key 输入框存在
- [x] 快捷创建成功后自动刷新列表并切换 Tab
- [x] 未知模型场景有引导提示

## Task 4: 验证
- [x] 快捷模式创建流程端到端测试通过
- [x] 高级模式功能完全正常，不受快捷模式影响
