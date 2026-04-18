# Tasks

- [x] Task 1: 明确 Vidu 协议映射与参数矩阵
  - [x] 梳理 `img2video` 必填字段、可选字段与默认行为
  - [x] 输出按模型族划分的 `duration/resolution/audio` 约束矩阵
  - [x] 定义平台内部统一字段到 Vidu 字段的映射规则（含 `images`、`is_rec`、`payload`）

- [x] Task 2: 扩展服务商目录与连接配置
  - [x] 在服务商目录中补齐 `Vidu` 图生视频类型与默认 Base URL
  - [x] 复用现有连接配置流程保存 API Key 与鉴权方式
  - [x] 确保连接创建、更新、读取接口完整返回 Vidu 元数据

- [x] Task 3: 扩展模型配置与能力声明
  - [x] 在模型配置中声明 Vidu 模型 ID 与模型族能力标签
  - [x] 支持对参数范围进行可配置或可计算校验
  - [x] 保证模型配置导入、更新、读取时能力字段不丢失

- [x] Task 4: 实现 Vidu 图生视频提交链路
  - [x] 在视频生成流程识别 Vidu 模型并路由到 `/ent/v2/img2video`
  - [x] 构建请求体并完成单图、格式、比例、体积等前置校验
  - [x] 实现 `audio/audio_type/voice_id/is_rec/bgm/off_peak/watermark/wm_position` 条件参数处理
  - [x] 对参数冲突返回明确 4xx 业务错误

- [x] Task 5: 实现 Vidu 任务查询与结果映射
  - [x] 接入 Vidu 任务查询接口并同步任务状态
  - [x] 解析成功结果视频 URL、带水印 URL（如返回）与失败原因
  - [x] 映射到平台统一任务结果结构，不影响其他供应商

- [x] Task 6: 验证与回归
  - [x] 覆盖核心场景：最小参数提交、推荐提示词、音频开关、错峰/水印（已补自动化用例）
  - [x] 覆盖异常场景：非法图片、参数越界、模型与参数不匹配（已补自动化用例）
  - [x] 执行视频链路最小回归，确认 Ark/Moark/Vidu 其他路径无回归；Kling 链路迁移至独立规格跟踪（`add-kling-onelink-video-provider`）
  - [x] 对照 `checklist.md` 完成验收勾选

- [x] Task 7: 修复验收失败项并补齐回归证据
  - [x] 修复 Vidu 成功结果统一结构映射，支持同时保留主视频 URL 与水印 URL
  - [x] 补充 Vidu 核心/异常场景自动化测试（含 `is_rec`、`audio` 条件参数、越界校验）
  - [x] 补充 Ark/Moark/Vidu 最小回归用例，并明确 Kling 链路处理策略（迁移至独立规格跟踪）

# Task Dependencies
- Task 2 depends on Task 1
- Task 3 depends on Task 1 and Task 2
- Task 4 depends on Task 1 and Task 3
- Task 5 depends on Task 4
- Task 6 depends on Task 2 and Task 3 and Task 4 and Task 5
- Task 7 depends on Task 6
