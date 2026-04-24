# OneLink豆包视频返回解析与轮询超时修复计划

## Summary
- 目标：修复 `onelinkai` 下 `doubao-seedance-*` 视频任务“已创建且可能已成功但后端仍返回 504”的问题，确保能稳定拿到视频 URL。
- 成功标准：
  - 创建接口仅返回 `{"id":"cgt-..."}` 时，系统可继续轮询并完成取回视频。
  - 查询接口返回 `status=succeeded` 且存在 `content.videoUrl` 时，系统应返回视频地址而非 504。
  - 厂商返回明确失败码/失败状态时，系统应尽早失败并给出可定位错误信息，不进入无意义超时。

## Current State Analysis
- 轮询入口在 `aigc-server/src/main/java/com/example/aigc/service/impl/GenerationServiceImpl.java`：
  - `pollArkVideoTask(...)` 与 `pollVideoTaskByPath(...)` 已支持循环轮询，当前默认窗口约 300 秒（`100 * 3000ms`）。
- 任务 ID 提取逻辑：
  - `parseArkVideoTaskId(...)` 已支持从 `task_id/taskId/id` 等字段提取，已覆盖官网创建返回 `id` 场景。
- 状态与 URL 解析逻辑：
  - `parseArkTaskStatus(...)`、`isSuccessStatus(...)`、`isFailedStatus(...)` 存在通用匹配。
  - `parseArkVideoUrl(...)` + `collectCandidateUrls(...)` 使用递归“包含 url 键”的泛化提取。
- 已知风险点（由对话与现状推断）：
  - 厂商返回结构可能存在包裹层/命名差异（例如 `content.videoUrl`、`data.content.videoUrl`、驼峰/下划线混用）。
  - 响应中可能存在字符串包裹符（反引号/多余引号/空白），当前 URL 判定对这类值容错不足。
  - 当前“错误判断”主要依赖 `error` 字段，若厂商以 `code/message` 表示失败，可能被延迟到超时。
- 现有测试覆盖：
  - `GenerationServiceImplViduTest` 已覆盖 OneLink 提交与基础轮询路径，但尚未针对 `id + status=succeeded + content.videoUrl` 官方风格返回建立强约束测试。

## Proposed Changes

### 1) 强化 OneLink 视频结果提取（优先命中官方结构）
- 文件：`aigc-server/src/main/java/com/example/aigc/service/impl/GenerationServiceImpl.java`
- 变更点：
  - 在 `parseArkVideoUrl(...)` 前置“定点提取”优先级，优先尝试：
    - `content.videoUrl`、`content.video_url`
    - `data.content.videoUrl`、`data.content.video_url`
    - `result.content.videoUrl`、`result.content.video_url`
  - 若定点提取失败，再走现有递归 `collectCandidateUrls(...)` 兜底。
- 原因：直接对齐你提供的官网成功响应结构，减少仅靠泛化递归导致的漏判概率。

### 2) 增强 URL 规范化与判定容错
- 文件：`aigc-server/src/main/java/com/example/aigc/service/impl/GenerationServiceImpl.java`
- 变更点：
  - 新增 URL 规范化函数（供 `isValidMediaUrl(...)` 与 URL 收集复用）：
    - 去除前后空白。
    - 去除包裹引号（`'`/`"`）与反引号（`` ` ``）后再判定 `http(s)://`。
  - 保持原有“只接受 http(s) URL”约束不变。
- 原因：你给出的示例中 URL 文本存在包裹符风险，容错后可避免“看起来有 URL 但判断失败”。

### 3) 轮询阶段补齐失败判定（避免无效等待到 504）
- 文件：`aigc-server/src/main/java/com/example/aigc/service/impl/GenerationServiceImpl.java`
- 变更点：
  - 在 `pollArkVideoTask(...)`、`pollVideoTaskByPath(...)` 中，除 `error` 外增加 `code/message` 失败判定：
    - 当响应明确为失败语义（如 `code` 非成功码且伴随错误消息）时直接抛 `BizException(502, ...)`。
  - 保留现有 `status` 失败集合判定作为并行兜底。
- 原因：对齐厂商常见错误返回形态，避免“已失败但状态未命中”被误判为处理中直至超时。

### 4) 增加最小必要日志（用于定位“扣费但未回传”）
- 文件：`aigc-server/src/main/java/com/example/aigc/service/impl/GenerationServiceImpl.java`
- 变更点：
  - 轮询中增加低噪音日志（建议 `debug` 级别）：
    - `taskId`、`attempt/maxAttempts`、解析到的 `status`、是否提取到 URL、关键错误码摘要。
  - 在超时/异常消息中保留 `summarizeBody(...)` 摘要（现有逻辑已部分具备，继续统一）。
- 原因：线上复盘时可快速判断卡在“状态识别”还是“URL提取”。

### 5) 回归测试补齐（围绕官方返回范式）
- 文件：`aigc-server/src/test/java/com/example/aigc/service/impl/GenerationServiceImplViduTest.java`
- 新增/调整测试：
  - 用例A：创建返回仅 `id`，查询返回 `status=succeeded + content.videoUrl`，应成功拿到视频。
  - 用例B：查询返回 `status=succeeded` 但 URL 含包裹符（反引号/引号），应清洗后成功返回。
  - 用例C：查询返回 `code/message` 明确失败（即使 `error` 字段缺失），应立即抛 502，不等待超时。
- 原因：把本次线上故障模式固化为自动化防回归。

## Assumptions & Decisions
- 决策1：本次以“优先拿到视频并减少误超时”为第一目标，兼顾定位能力；不改动外部 API 协议与前端入参格式。
- 决策2：默认仅新增 `debug` 级轮询日志，避免生产日志噪音；必要时可临时提至 `info` 排障。
- 决策3：错误语义保持现有风格：
  - 任务失败 -> 502；
  - 超时 -> 504；
  - 已成功但无 URL -> 502（含摘要）。
- 假设：当前 `onelinkai` 视频任务查询路径仍为 `/volc/api/v3/contents/generations/tasks/{taskId}`，无需改路由。

## Verification Steps
- 单元测试：
  - 执行 `mvn -Dtest=GenerationServiceImplViduTest test`。
  - 确认新增 3 个用例全部通过，且历史用例无回归。
- 手工验证（联调）：
  - 发起 `doubao-seedance-2.0` 任务，记录创建返回 `id`。
  - 观察轮询日志应出现：`running -> succeeded` 或直接命中 `content.videoUrl`。
  - 最终响应应返回视频 URL，不出现 504。
- 失败链路验证：
  - 构造/模拟厂商 `code/message` 失败响应，应快速返回 502，耗时显著低于轮询上限。

