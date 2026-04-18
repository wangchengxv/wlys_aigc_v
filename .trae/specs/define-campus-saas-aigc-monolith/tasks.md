# Tasks

- [ ] Task 1: 明确单体SaaS边界与信息架构
  - [ ] 梳理平台管理端、教师端、学生端的核心导航与入口分区
  - [ ] 定义“方便找、方便用”的页面结构与关键操作路径（含统一搜索与常用入口）
  - [ ] 输出角色-能力矩阵（平台管理员、租户管理员、教师、学生）

- [ ] Task 2: 设计并落地本地数据库基线
  - [ ] 设计租户、用户、角色权限、AIGC任务、作业与评分核心数据表
  - [ ] 提供初始化脚本与基础种子数据，确保本地环境可一键启动
  - [ ] 补充多租户数据隔离约束与索引策略

- [ ] Task 3: 完成租户与用户管理能力
  - [ ] 实现租户创建、启停、配额配置与租户管理员绑定
  - [ ] 实现用户全生命周期管理（创建、启停、角色分配、重置凭据）
  - [ ] 完成双层权限校验（平台级与租户级）

- [ ] Task 4: 打通OneLinkAI账号系统对接
  - [ ] 定义OneLinkAI账号绑定与凭据管理流程
  - [ ] 实现平台账号/租户账号与OneLinkAI账号映射
  - [ ] 完成鉴权、调用审计与失败重试策略

- [ ] Task 5: 落地AIGC核心工作流
  - [ ] 实现文生图、文生视频、图生视频统一任务提交流程
  - [ ] 实现任务状态查询、结果回显、失败原因展示
  - [ ] 保障工作流参数校验与供应商调用稳定性

- [ ] Task 6: 完成教师端作业分发与评分闭环
  - [ ] 实现作业创建、按班级/学生分发、提交管理
  - [ ] 实现评分、评语、评分统计与结果回看
  - [ ] 支持作业关联AIGC生成内容作为提交或参考材料

- [ ] Task 7: 提升稳定性与可观测性
  - [ ] 增加结构化日志、关键业务指标与告警阈值
  - [ ] 建立关键链路的异常分类与诊断信息标准
  - [ ] 补充操作审计（租户治理、账号治理、任务执行、评分操作）

- [ ] Task 8: 验证与验收
  - [ ] 执行核心流程联调（租户、账号、OneLinkAI、AIGC、作业评分）
  - [ ] 执行回归测试并修复阻断性问题
  - [ ] 对照 `checklist.md` 完成逐项勾选

# Task Dependencies
- Task 2 depends on Task 1
- Task 3 depends on Task 2
- Task 4 depends on Task 2 and Task 3
- Task 5 depends on Task 2 and Task 4
- Task 6 depends on Task 2 and Task 3 and Task 5
- Task 7 depends on Task 3 and Task 4 and Task 5 and Task 6
- Task 8 depends on Task 3 and Task 4 and Task 5 and Task 6 and Task 7
