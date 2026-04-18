# Tasks

- [x] Task 1: 统一高级媒体请求结构
  - [x] 盘点现有工作台 `GenerateRequest` 的顶层字段与已接入的 Vidu/Kling 图片视频路径
  - [x] 设计可同时承载图片与视频高级字段的结构化请求体，并定义前后端字段命名与兼容策略
  - [x] 明确 `videoReferenceImageUrl`、`videoViduOptions` 迁移到统一结构后的归一化规则

- [x] Task 2: 补齐工作台高级图片/视频表单
  - [x] 在工作台按模型能力显示对应高级表单，而不是只支持当前 Vidu 视频高级项
  - [x] 支持 Vidu reference2image、Kling 多图参考生图、扩图、Omni 所需的额外输入
  - [x] 在前端提交前增加必填与基本格式校验，避免明显无效请求进入后端

- [x] Task 3: 扩展前端请求类型与提交链路
  - [x] 更新 `aigc-site-react` 的类型定义与生成请求序列化逻辑
  - [x] 让工作台将高级表单值映射到统一请求结构
  - [x] 保持现有普通图片、普通视频与历史 Vidu 视频路径可继续提交

- [x] Task 4: 扩展 Java 后端请求体与归一化逻辑
  - [x] 更新 Java `GenerateRequest` 以接收结构化高级媒体参数
  - [x] 在 `GenerationServiceImpl` 中补充兼容归一化逻辑，将旧字段与新字段统一到内部模型
  - [x] 对缺少必填参考图、多图数量不合法、扩图参数冲突、Omni 字段不匹配等场景返回明确 4xx 错误

- [x] Task 5: 补齐供应商请求体映射
  - [x] 在图片链路补齐 Kling 多图参考生图、扩图、Omni 的请求体构建
  - [x] 在视频链路补齐 Vidu reference2image 与相关高级字段的统一映射
  - [x] 确保普通 Ark/Moark/Vidu/Kling 已有路径不被新结构破坏

- [x] Task 6: 验证与回归
  - [x] 为前后端新增最小自动化验证，覆盖结构化请求、兼容旧字段、关键异常场景
  - [x] 验证工作台表单展示与提交载荷符合能力约束
  - [x] 对照 `checklist.md` 勾选验收项，并补齐失败项对应修复任务

- [x] Task 7: 补齐高级媒体请求自动化验证闭环
  - [x] 为 `aigc-site-react` 增加工作台高级能力表单与 `advancedMedia` 序列化的最小自动化验证
  - [x] 为 `aigc-server` 增补 Omni、扩图、Kling 多图等关键 4xx 异常场景测试

# Task Dependencies
- Task 2 depends on Task 1
- Task 3 depends on Task 1 and Task 2
- Task 4 depends on Task 1
- Task 5 depends on Task 4
- Task 6 depends on Task 2, Task 3, Task 4, and Task 5
