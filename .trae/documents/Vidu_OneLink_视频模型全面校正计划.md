# Vidu OneLink 视频模型全面校正计划

## Summary
- 目标：将 Vidu（通过 OneLink 代理）的视频模型能力矩阵从当前实现的“旧约束”校正为你确认的“官方规则”，确保你提供的 `video-viduq3-pro` 用例（`duration=5`、`resolution=1080p`、`audio=true`、`voice_id=professional_host`、`movement_amplitude=auto`、`off_peak=false`）可在系统中稳定通过。
- 范围：聚焦 Java 后端 `aigc-server` 的 Vidu 能力推断、参数校验与回归测试；不改动数据库结构、不改动前端字段模型、不新增接口。
- 成功标准：
  - `video-viduq3-pro` + OneLink 路径下参数校验通过；
  - Vidu Q3/Q2/Q1 族能力规则与约束一致；
  - 相关单测覆盖并通过，避免旧矩阵回归。

## Current State Analysis
- Provider 与链路现状：
  - `ProviderCatalog` 已包含 `vidu_onelink`（`https://api.onelinkai.cloud` + `/vidu/ent/v2/img2video`），满足 OneLink 代理提交路径。
  - `GenerationServiceImpl` 中 `generateVideosWithViduOneLinkConnection` 会调用 `callViduVideoApi`，路径路由逻辑已可用。
- 关键问题（与目标不一致）：
  - `ModelCapabilityService.defaultViduMatrix` 仍是旧矩阵：
    - Q3: duration `4/8`，resolution `540/720/1080`；
    - Q2: duration `4/8`，resolution `360/540/720`，audio `true`；
    - Q1: duration `4/8`，resolution `360/540`。
  - `GenerationServiceImpl.defaultViduMatrix` 同样是旧矩阵，且 `validateAndNormalizeViduOptions` 强依赖此矩阵进行 `duration/resolution/audio` 校验；当前会拒绝 `duration=5`。
  - 单测 `ModelCapabilityServiceViduTest` 与 `GenerationServiceImplViduTest` 目前断言基于旧矩阵（例如 `List.of(4, 8)`），与本次目标冲突。

## Proposed Changes
### 1) 校正 Vidu 能力矩阵（核心逻辑）
- 文件：`aigc-server/src/main/java/com/example/aigc/service/ModelCapabilityService.java`
- 修改内容：
  - 更新 `defaultViduMatrix(String family)`：
    - Q3：duration `1..16`，resolution `540p/720p/1080p`，audio `true`；
    - Q2：duration `1..10`，resolution `540p/720p/1080p`，audio `false`；
    - Q1：duration `5`，resolution `1080p`，audio `false`；
    - 其余 family 默认值对齐到保守可用集（建议沿 Q2 或显式说明）。
  - 保持 `normalizeViduModelMetadata` 注入逻辑不变，仅替换矩阵来源。
- 原因：
  - 前端可选模型和后端能力展示依赖 metadata 规范化结果，必须先在 capability 层对齐官方能力。

### 2) 校正 Vidu 参数校验矩阵（请求校验）
- 文件：`aigc-server/src/main/java/com/example/aigc/service/impl/GenerationServiceImpl.java`
- 修改内容：
  - 更新同名 `defaultViduMatrix(String family)`，与 `ModelCapabilityService` 保持完全一致（同一组 duration/resolution/audio 规则）。
  - 复核 `validateAndNormalizeViduOptions(...)` 的联动行为：
    - `duration=5` 在 Q3 下允许；
    - Q2/Q1 下 `audio=true` 需拒绝；
    - `voice_id` 仅在 `audio=true` 下允许（现逻辑已覆盖，保持）。
- 原因：
  - 实际请求路径由 `GenerationServiceImpl` 校验把关；仅改 capability 不足以放通你的已测用例。

### 3) 更新并新增回归测试
- 文件：`aigc-server/src/test/java/com/example/aigc/service/ModelCapabilityServiceViduTest.java`
- 修改内容：
  - 调整旧断言：Q2/Q1/Q3 的 `viduDurations`、`viduResolutions`、`viduAudioSupported` 与新矩阵一致。
  - 新增断言：Q1 固定 5 秒、Q2 音频不支持、Q3 支持 1-16 秒覆盖（可用 contains/size 辅助验证）。
- 文件：`aigc-server/src/test/java/com/example/aigc/service/impl/GenerationServiceImplViduTest.java`
- 修改内容：
  - 新增或改造测试用例验证：
    - Q3 `duration=5` + `resolution=1080p` + `audio=true` 可通过校验；
    - Q2/Q1 传 `audio=true` 抛出 400；
    - Q1 传 `duration != 5` 抛出 400。
- 原因：
  - 本次变更是“规则矩阵替换”，最易回归，必须用测试锁死边界。

### 4) 保持 OneLink 代理路径为当前主链路（不改接口形态）
- 文件：`aigc-server/src/main/java/com/example/aigc/service/ProviderCatalog.java`（仅复核，不预计改动）
- 处理方式：
  - 保持 `vidu_onelink` 的 submit/result path 现状；
  - 不新增新 provider key，避免引入路由歧义。
- 原因：
  - 你明确要求固定 OneLink 代理；现有链路已满足，重点是参数校验与能力规则。

## Assumptions & Decisions
- 已确认决策：
  - 范围采用“全面校正”而非“仅修一个模型”；
  - 调用链路固定 OneLink 代理。
- 假设：
  - 现有 `video-viduq3-pro` 模型配置仍通过 `vidu`/`onelinkai` 连接解析并路由到 `vidu_onelink`，无需新增模型 ID 映射。
  - 前端高级参数 UI 继续透传，不在本次变更中对下拉选项做产品化收敛（如 540p 选项显隐等）。
- 非目标：
  - 不处理 Go 后端（`aigc-server-go`）同步；
  - 不调整数据库中的历史模型数据。

## Verification Steps
1. 后端单测（优先）：
   - 运行 `ModelCapabilityServiceViduTest`，确认 metadata 注入矩阵已切换；
   - 运行 `GenerationServiceImplViduTest`，确认 Q3 放通与 Q2/Q1 拒绝行为正确。
2. 可选集成验证（本地）：
   - 用视频生成请求构造：
     - `videoModel=video-viduq3-pro`
     - `videoReferenceImageUrl` 为可访问图片
     - `videoViduOptions` 包含 `duration=5,resolution=1080p,audio=true,voice_id=professional_host,movement_amplitude=auto,off_peak=false`
   - 期望：后端不再因矩阵校验拒绝，请求进入 OneLink 提交流程。
3. 回归检查：
   - Q2/Q1 模型请求 `audio=true` 返回 400；
   - Q1 `duration` 非 5 返回 400；
   - 其他非 Vidu 视频模型行为不变。

