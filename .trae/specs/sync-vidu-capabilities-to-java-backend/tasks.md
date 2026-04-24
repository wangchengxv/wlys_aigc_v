# Tasks

- [x] Task 1: 盘点 Java 现状与 Go 对齐差异
  - [x] 梳理 Java 后端当前视频生成路由、供应商鉴权与任务查询路径
  - [x] 对照 Go 侧 Vidu 实现列出字段映射、参数校验、结果映射差异清单
  - [x] 输出最小改造范围，避免牵动非 Vidu 供应商逻辑

- [x] Task 2: 同步 Java 侧 Vidu 提交与鉴权
  - [x] 在 Java 后端补齐 Vidu 图生视频提交路由到 `//vidu/vidu/ent/v2/img2video`
  - [x] 实现 `Authorization: Token {apiKey}` 与 `Content-Type: application/json`
  - [x] 完成必要请求体字段映射（`model`、`images`、`prompt` 等）

- [x] Task 3: 同步 Java 侧参数约束与错误语义
  - [x] 按模型族补齐 `duration`、`resolution`、`audio` 相关校验
  - [x] 补齐 `is_rec`、`audio_type`、`voice_id`、`off_peak`、`watermark` 等条件字段逻辑
  - [x] 参数冲突时返回清晰 4xx 业务错误

- [x] Task 4: 同步 Java 侧任务查询与统一结果映射
  - [x] 接入或完善 Vidu 任务查询逻辑
  - [x] 成功结果同时映射主视频 URL 与可选水印 URL（去重）
  - [x] 失败结果映射统一失败结构并保留供应商原因

- [x] Task 5: 补齐 Java 侧回归验证
  - [x] 增加最小自动化测试（核心场景 + 异常场景）
  - [x] 执行 Java 后端测试并确认通过
  - [x] 对照 `checklist.md` 完成逐项验收勾选

# Task Dependencies
- Task 2 depends on Task 1
- Task 3 depends on Task 1 and Task 2
- Task 4 depends on Task 2 and Task 3
- Task 5 depends on Task 2 and Task 3 and Task 4
