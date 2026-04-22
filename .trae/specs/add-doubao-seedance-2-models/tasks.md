# Tasks

- [x] Task 1: 在后端 Go 预置模型列表中添加 Doubao Seedance 2.0 模型
  - [x] SubTask 1.1: 编辑 `aigc-server-go/internal/service/preset_models.go`
  - [x] SubTask 1.2: 添加 doubao-seedance-2-0-260128（标准版）
  - [x] SubTask 1.3: 添加 doubao-seedance-2-0-fast-260128（极速版）

- [x] Task 2: 在前端 Mock 预置模型列表中同步添加
  - [x] SubTask 2.1: 编辑 `aigc-site-react/src/api/index.ts`
  - [x] SubTask 2.2: 在 MOCK_PRESET_MODELS 中添加 ark 提供商的两个 Seedance 2.0 模型
  - [x] SubTask 2.3: 确保 providers 数组中包含 "ark"

- [x] Task 3: 验证配置正确性
  - [x] SubTask 3.1: 确认新增模型的 provider 为 "ark"
  - [x] SubTask 3.2: 确认 capabilities 为 ["video"]
  - [x] SubTask 3.3: 确认 Base URL 为 https://ark.cn-beijing.volces.com

# Task Dependencies
- Task 2 可以在 Task 1 之后执行，也可以并行执行，因为两者独立
- Task 3 依赖 Task 1 和 Task 2 完成