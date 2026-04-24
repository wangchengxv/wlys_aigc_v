# Java后端 Vidu 图片模型重配计划（OneLink reference2image）

## Summary
- 目标：在 `aigc-server` 里将 Vidu 图片能力重配为仅支持两个模型：`image-vidu-q2`、`image-viduq3-pro`，并将 Vidu reference2image 请求固定走 OneLink 路径 `https://api.onelinkai.cloud` + `/vidu/ent/v2/reference2image`。
- 鉴权目标：reference2image 请求头使用 `Content-Type: application/json` 与 `Authorization: Bearer {apiKey}`（不再使用 `Token {apiKey}`）。
- 约束：仅调整 Vidu reference2image 图片链路；`vidu` 的视频（img2video）链路保持现有行为，不做协议切换。
- 范围：包含 `vidu`、`onelinkai` 的相关模型清单与 reference2image 运行时严格白名单校验；并补充后端文档说明。

## Current State Analysis
- `ProviderCatalog` 中：
  - `vidu` 当前 `AuthMode.TOKEN`，静态模型包含多种 Vidu 模型（含 `image-vidu-q2-fast` 等）。
  - `onelinkai` 静态模型也包含多种 Vidu/Kling/通用模型。
  - `vidu_onelink` 为 Bearer，但 staticModels 为空，且主要用于 Vidu 视频代理路径。
- `ProviderHttpGateway.applyHeaders()` 里 `AuthMode.TOKEN` 会统一下发 `Authorization: Token ...`；`AuthMode.BEARER` 才是 `Authorization: Bearer ...`。
- `GenerationServiceImpl` 中 Vidu 图片高级能力（`vidu_reference2image`）当前：
  - 通过 `resolveViduImageProvider()` 只允许 `vidu` 或 `onelinkai`；
  - `callViduReference2ImageApi()` 对 `vidu_onelink` 走 `/vidu/ent/v2/reference2image`，其余走 `/ent/v2/reference2image`；
  - `buildViduReference2ImagePayload()` 未做模型白名单限制，当前会透传所选模型名。
- 现有测试：
  - `ProviderHttpGatewayTokenAuthTest` 断言 TOKEN 头；
  - `GenerationServiceImplViduTest` 覆盖 Vidu reference2image 基础行为，但未覆盖“仅允许两个模型”的强校验；
  - `ModelCapabilityServiceViduTest` 含部分 Vidu/Kling能力用例，可扩充新模型能力断言。

## Proposed Changes

- 文件：`aigc-server/src/main/java/com/example/aigc/service/ProviderCatalog.java`
  - 变更：
    - 收敛 `vidu` 的图片模型可见清单，仅保留 `image-vidu-q2`、`image-viduq3-pro`（按你的“仅支持”要求）；
    - 在 `onelinkai` 的静态模型中同步仅保留这两个 Vidu 图片模型（其余 Vidu 图片项移除，如 `image-vidu-q2-fast`）。
  - 原因：目录层面先体现“仅支持这两个模型”，避免误选。
  - 说明：仅调整与 Vidu 图片能力相关的模型项，不扩展未请求的其它模型治理。

- 文件：`aigc-server/src/main/java/com/example/aigc/service/impl/GenerationServiceImpl.java`
  - 变更 1：在 Vidu reference2image 构造或调用前新增严格白名单校验，仅允许 `image-vidu-q2`、`image-viduq3-pro`。
  - 变更 2：将 Vidu reference2image 统一走 OneLink 路径：`baseUrl=https://api.onelinkai.cloud` + `path=/vidu/ent/v2/reference2image`。
  - 变更 3：仅在 reference2image 路径使用 Bearer 语义（通过选择 Bearer provider 定义进行调用），不改 `vidu` 视频链路鉴权。
  - 原因：满足“仅 reference2image 切到 Bearer + OneLink endpoint”的范围约束，避免影响现有 Vidu 视频调用。
  - 实现方式：
    - 优先复用 `providerCatalog.require("vidu_onelink")` 作为 reference2image 调用定义；
    - 如需保留 `resolveViduImageProvider()` 兼容入口，则在该流程内部强制映射到 `vidu_onelink` 调用 reference2image；
    - 补充清晰异常文案：当模型不在白名单时直接 400。

- 文件：`aigc-server/src/main/java/com/example/aigc/service/ModelCapabilityService.java`
  - 变更：补充/校正 Vidu 图片模型识别逻辑，确保 `image-viduq3-pro` 被识别为 `image`（必要时纳入 `isViduReference2ImageFamilyModel` 规则）。
  - 原因：模型改名后仍需在能力筛选中稳定显示为图片模型。

- 文件：`aigc-server/src/test/java/com/example/aigc/service/impl/GenerationServiceImplViduTest.java`
  - 变更：
    - 新增 `image-vidu-q2` 与 `image-viduq3-pro` 的 reference2image 正向构造/路由用例；
    - 新增非白名单模型（例如 `image-vidu-q2-fast`）触发 400 的反向用例；
    - 断言 reference2image 调用走 `/vidu/ent/v2/reference2image` 路径。
  - 原因：锁定“严格白名单 + OneLink path”行为，防止回归。

- 文件：`aigc-server/src/test/java/com/example/aigc/service/ProviderHttpGatewayTokenAuthTest.java`
  - 变更：补充（或重构）鉴权测试，覆盖 reference2image 场景最终使用 `Authorization: Bearer ...`，并保留/明确 TOKEN 仅用于未变更路径。
  - 原因：用户要求将该场景从 Token 切换到 Bearer，需要测试层可追踪。

- 文件：`aigc-server/src/test/java/com/example/aigc/service/ModelCapabilityServiceViduTest.java`
  - 变更：新增 `image-viduq3-pro` 的能力断言（仅 `image`）。
  - 原因：确保新支持模型在后端能力判定一致。

- 文件：`docs/图生视频的文件`（或新增同目录专门文档）
  - 变更：补充“Vidu reference2image 请求头字段/值/描述”文档，明确：
    - `Content-Type: application/json`（数据交换格式）
    - `Authorization: Bearer {apiKey}`（将 `{apiKey}` 替换为实际 API Key）
  - 原因：你明确要求补文档并将 Token 示例替换为 Bearer 示例。

## Assumptions & Decisions
- 决策：仅改 Java 后端 `aigc-server`，不改 `aigc-server-go`。
- 决策：严格白名单仅作用于 `Vidu reference2image` 流程，不波及 onelinkai 其它图片能力（如 Kling）。
- 决策：`vidu` 视频链路保持现状，不做 provider 级 Token->Bearer 全量切换。
- 决策：OneLink endpoint 采用 `base+path` 拆分落地，不使用“完整 URL 直接当 baseUrl”。
- 约定：`Content-Type` 统一采用标准值 `application/json`（HTTP 头大小写不敏感，但文档按标准写法输出）。

## Verification Steps
- 单元测试：
  - 运行 `ModelCapabilityServiceViduTest`，确认 `image-viduq3-pro` 与 `image-vidu-q2` 识别为 `image`；
  - 运行 `GenerationServiceImplViduTest`，确认 reference2image：
    - 白名单模型可通过；
    - 非白名单模型返回 400；
    - 调用路径为 `/vidu/ent/v2/reference2image`；
  - 运行 `ProviderHttpGatewayTokenAuthTest`，确认该场景使用 Bearer。
- 接口回归：
  - `GET /api/v1/provider-catalog` 检查 `vidu`/`onelinkai` 可见的 Vidu 图片模型仅剩两项；
  - 发起 reference2image 请求验证请求头为：
    - `Content-Type: application/json`
    - `Authorization: Bearer <apiKey>`。
- 风险回归：
  - 回归 `vidu` 图生视频（`/ent/v2/img2video`）流程，确认未被本次 reference2image 重配误伤；
  - 回归 onelinkai 的 Kling 图片高级能力（multi-reference/outpaint/omni）保持不变。
