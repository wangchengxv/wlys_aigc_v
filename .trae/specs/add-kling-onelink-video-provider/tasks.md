# Tasks

- [ ] Task 1: 定义 Kling 接入契约
  - [ ] 确认 `OneLinkAI -> Kling` 的默认 Base URL、文生视频路径、图生视频路径与异步查询方式
  - [ ] 设计可复用的模型元数据字段，用于表达 `文生视频`、`图生视频` 或自动判断模式
  - [ ] 明确本次范围外能力的保留扩展点，避免后续再改数据结构

- [ ] Task 2: 扩展后端服务商与模型配置
  - [ ] 在服务商目录中新增 `Kling` 或等价的 `OneLinkAI Kling` 类型
  - [ ] 为该类型配置默认 Base URL、认证方式与视频能力标记
  - [ ] 在预置模型或快捷配置中加入至少一组 `Kling` 示例模型
  - [ ] 确保连接配置、模型配置的创建、更新、读取流程能保存并返回 Kling 元数据

- [ ] Task 3: 实现 Kling 视频生成路由
  - [ ] 在视频生成链路中识别 `Kling` 模型
  - [ ] 无参考图时提交到 `/v1/videos/text2video`
  - [ ] 有参考图时提交到 `/v1/videos/image2video`
  - [ ] 当模型模式与输入冲突时返回明确业务错误

- [ ] Task 4: 实现 Kling 任务轮询与结果解析
  - [ ] 按 `Kling` 返回结构提取任务 ID、状态、失败原因与结果视频 URL
  - [ ] 将 `Kling` 结果映射到现有统一任务结构
  - [ ] 保证不会破坏 `Ark`、`Vidu`、`Moark` 的既有解析逻辑

- [ ] Task 5: 验证与回归
  - [ ] 验证服务商目录、连接配置、模型配置接口的返回结构
  - [ ] 验证 `Kling` 文生视频与图生视频的路由选择
  - [ ] 验证任务成功、失败、缺少参考图等关键场景
  - [ ] 运行与视频生成相关的最小回归检查，确认现有供应商无回归

# Task Dependencies
- Task 2 depends on Task 1
- Task 3 depends on Task 1 and Task 2
- Task 4 depends on Task 3
- Task 5 depends on Task 2, Task 3, and Task 4
