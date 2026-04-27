# Tasks
- [x] Task 1: 明确 SpringAI 可承接功能范围与迁移边界（用户可见成果：可迁移功能清单）
  - [x] SubTask 1.1: 基于现有服务调用点梳理文本/图片/视频能力分类
  - [x] SubTask 1.2: 输出“优先迁移、暂缓迁移、保留现状”的功能映射表
  - [x] SubTask 1.3: 记录每类功能的技术依据与风险说明

- [x] Task 2: 设计统一文本调用架构（用户可见成果：统一调用规范）
  - [x] SubTask 2.1: 定义 `TextGenerationFacade` 的输入输出契约与异常语义
  - [x] SubTask 2.2: 定义 `SpringAiClientFactory` 的动态构建规则（连接、模型、鉴权、元数据）
  - [x] SubTask 2.3: 定义 SpringAI 优先、现有网关回退的执行流程

- [x] Task 3: 制定关键链路迁移方案（用户可见成果：模块级迁移顺序）
  - [x] SubTask 3.1: 路由代理链路迁移方案（`RouterProxyService`）
  - [x] SubTask 3.2: 剧本工作流文本链路迁移方案（`ScriptWorkflowService`）
  - [x] SubTask 3.3: 反推提示词与通用生成文本分支迁移方案（`ReversePromptServiceImpl`、`GenerationServiceImpl`）

- [x] Task 4: 定义验证与回归标准（用户可见成果：可执行验收标准）
  - [x] SubTask 4.1: 明确接口兼容性验证项（响应结构、错误码、流式行为）
  - [x] SubTask 4.2: 明确测试范围（单元、集成、关键回归用例）
  - [x] SubTask 4.3: 明确可观测性指标（成功率、耗时、回退率）

# Task Dependencies
- Task 2 depends on Task 1
- Task 3 depends on Task 2
- Task 4 depends on Task 3
