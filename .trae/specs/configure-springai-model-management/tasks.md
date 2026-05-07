# Tasks
- [x] Task 1: 搭建SpringAI后端配置管理骨架
  - [x] SubTask 1.1: 定义模型配置实体与存储结构
  - [x] SubTask 1.2: 实现配置管理服务与校验逻辑
  - [x] SubTask 1.3: 提供配置增删改查REST接口

- [x] Task 2: 实现敏感信息安全与错误处理
  - [x] SubTask 2.1: 增加API Key加密存储与脱敏返回
  - [x] SubTask 2.2: 统一异常结构与错误码映射
  - [x] SubTask 2.3: 添加关键安全与参数校验测试

- [x] Task 3: 改造前端模型配置管理页面
  - [x] SubTask 3.1: 更新前端API层对接新后端接口
  - [x] SubTask 3.2: 改造管理页面的加载、保存、删除流程
  - [x] SubTask 3.3: 完善失败提示与保存成功反馈

- [x] Task 4: 打通模型选择联动并完成验收
  - [x] SubTask 4.1: 连接管理与模型选择读取新配置源
  - [x] SubTask 4.2: 验证配置变更后前端可见且可用
  - [x] SubTask 4.3: 完成端到端验证并更新文档说明

# Task Dependencies
- Task 2 depends on Task 1
- Task 3 depends on Task 1
- Task 4 depends on Task 2 and Task 3
