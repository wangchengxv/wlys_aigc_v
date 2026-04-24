# Java 后端新增 Kling 图片模型计划

## Summary
- 目标：在 `aigc-server`（Java）后端新增 `kling` 提供商下的图片模型 `image-kling-v3`、`image-kling-v3-omni`，并实现“全链路可用”（目录可见、能力识别正确、图片生成路由可调用、测试覆盖）。
- 成功标准：
  - Provider 目录返回中，`kling` 的 `staticModels` 包含两个新模型；
  - 这两个模型在能力判定中仅归类为 `image`；
  - 图片生成流程在 `provider=kling` + 新模型时，走 Kling 图片接口而不是通用 OpenAI 图片接口；
  - 相关单测通过，且关键接口检查点可验证新增能力生效。

## Current State Analysis
- `ProviderCatalog` 中已有 `kling` 提供商，但 `staticModels` 仅包含视频模型：`video-kling-v3-6`、`video-kling-v3`、`video-kling-v3`、`kling-v1-6`、`kling-v1`；未包含新图片模型。
- `ModelCapabilityService` 的现有规则可通过模型名包含 `image` 推断图片能力；对 `image-kling-v3*` 能推断出 `image`，且不会命中 `video` 规则（当前 `isKlingVideoModel` 对 `provider=kling` 仅识别 `kling-` 前缀）。
- `GenerationServiceImpl` 中，Kling 图片生成分支当前仅在 `provider=onelinkai && isKlingModel(modelName)` 时触发；`provider=kling` + `image-kling-v3*` 不会进入该分支，最终会因 `imageGenerationPath` 为空而报错。
- 测试现状：
  - `ModelCapabilityServiceViduTest` 已覆盖部分 Kling 能力识别（如 `kling-v1`、`video-kling-v3`），但未覆盖 `image-kling-v3*`；
  - `GenerationServiceImplViduTest` 主要覆盖 Vidu/Kling 高级能力与视频分支，缺少 `provider=kling` 图片模型路由覆盖。

## Proposed Changes
- 文件：`aigc-server/src/main/java/com/example/aigc/service/ProviderCatalog.java`
  - 变更：在 `kling` 提供商 `staticModels` 追加 `image-kling-v3`、`image-kling-v3-omni`。
  - 原因：保证后端提供商目录与模型候选列表可发现新模型。
  - 实现方式：仅扩充静态模型列表，不调整现有鉴权与接口路径配置。

- 文件：`aigc-server/src/main/java/com/example/aigc/service/impl/GenerationServiceImpl.java`
  - 变更 1：新增 Kling 图片模型识别方法（如 `isKlingImageModel`），明确识别 `image-kling-` 前缀。
  - 变更 2：调整图片生成分支判断，使 `provider=kling` 且命中 Kling 图片模型时，复用 `callKlingImageApi` 路径。
  - 变更 3：保持视频路由逻辑不变（避免把 `image-kling-*` 误当视频模型处理）。
  - 原因：实现“全链路”中的运行时可调用，避免当前进入通用图片分支后因无 `imageGenerationPath` 报错。
  - 实现方式：在不影响现有 `kling-v*` 视频模型判断的前提下，最小改动扩展图片路由条件。

- 文件：`aigc-server/src/test/java/com/example/aigc/service/ModelCapabilityServiceViduTest.java`
  - 变更：新增针对 `provider=kling` + `image-kling-v3` / `image-kling-v3-omni` 的能力断言用例，校验结果仅包含 `image`。
  - 原因：锁定能力推断规则，防止后续回归把图片模型误判为视频或文本。

- 文件：`aigc-server/src/test/java/com/example/aigc/service/impl/GenerationServiceImplViduTest.java`
  - 变更：补充 `provider=kling` + `image-kling-v3*` 的图片路由测试（优先验证进入 Kling 图片 API 分支，而非通用图片路径）。
  - 原因：锁定新增运行时路由行为，确保“可配置但不可调用”的问题不会回归。
  - 实现方式：沿用现有反射与私有方法测试风格，最小增量加测。

## Assumptions & Decisions
- 决策：本次仅修改 Java 后端 `aigc-server`，不改 `aigc-server-go`。
- 决策：模型挂载在 `kling` 提供商下，能力标记目标为“仅 image”。
- 假设：`image-kling-v3` 与 `image-kling-v3-omni` 使用现有 Kling 图片接口 `/kling/v1/images/generations`（由 `callKlingImageApi` 复用），无需新增后端 endpoint 常量。
- 假设：前端与其他服务的同步（如 `aigc-site-react`、Go 目录）不在本次执行范围。

## Verification
- 单元测试：
  - 运行 `ModelCapabilityServiceViduTest`，确认新增模型能力为仅 `image`；
  - 运行 `GenerationServiceImplViduTest`，确认新增模型图片路由命中 Kling 图片分支。
- 关键接口检查（手工/联调）：
  - `GET /api/v1/provider-catalog`：检查 `kling.staticModels` 包含 `image-kling-v3`、`image-kling-v3-omni`；
  - 通过模型配置创建 `provider=kling` + 新模型后发起图片生成请求，确认不再报“未配置图片生成接口”，并返回 Kling 图片任务/图片结果。
- 回归关注点：
  - `kling-v1/v1-6/v2/v2-1/v2-6` 视频路径不受影响；
  - `onelinkai` 下现有 Kling 图片与高级能力（multi-reference/outpaint/omni）行为不变。
