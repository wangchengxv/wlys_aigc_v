# Tasks

- [x] Task 1: 在 Java 后端 PresetModelRegistry 中添加 Doubao Seedance 2.0 模型
  - [x] SubTask 1.1: 编辑 `aigc-server/src/main/java/com/example/aigc/model/PresetModelRegistry.java`
  - [x] SubTask 1.2: 添加 doubao-seedance-2-0-260128（标准版）
  - [x] SubTask 1.3: 添加 doubao-seedance-2-0-fast-260128（极速版）
  - [x] SubTask 1.4: 移除旧的 doubao-seedance-1-5-pro-251215

- [x] Task 2: 验证配置正确性
  - [x] SubTask 2.1: 确认新增模型的 provider 为 "ark"
  - [x] SubTask 2.2: 确认 capabilities 为 ["video"]
  - [x] SubTask 2.3: 确认 Base URL 为 https://ark.cn-beijing.volces.com

# Task Dependencies
- Task 2 依赖 Task 1 完成